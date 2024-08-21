package org.shaman.spatial;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.aparapi.Kernel;
import com.aparapi.Range;

import cern.jet.random.Uniform;

import weka.core.Instance;
import weka.core.Instances;

public class CoreNaiveBayesGPU
{
    // Enable OpenCL?
    public static final boolean GPU = true;
    //public static final boolean GPU = false;

    private CoreDataSet dataSet;

    // DataSets pre-processed for simplified train/test methods 
    private int []trainData;
    private int []testData;
    private int []numDistinct;
    private int []sumDistinct;
    private int sumDistinctAll;
    private int numAttributes, numGoal, numTrainInstances, numTestInstances;
    private int classIdx;

    // The Naive Bayes Classifier Model
    private float []paijvk;            // p(aij|vk) indexed with [k][i][j]
    private float []pvk;               // p(vk)
    private int   []caijvk;
    private int   []cvk;
    // Test instances classification output
    private int   []testClass;         

    // The OpenCL Kernels
    private NaiveBayesTrainKernel trainKernel;
    private NaiveBayesTestKernel  testKernel;

    public static class NaiveBayesTestKernel extends Kernel
    {
        final private int   []testData;
        final private float []paijvk;
        final private float []pvk;
        final private int   []testClass;
        final private int     numAttributes;
        final private int     numGoal;
        final private int   []sumDistinct;
        final private int     sumDistinctAll;

        public NaiveBayesTestKernel(int []testData, float []paijvk, float []pvk, int []testClass, int numAttributes, int numGoal, int []sumDistinct, int sumDistinctAll)
        {
            this.testData = testData;
            this.paijvk   = paijvk;
            this.pvk      = pvk;
            this.testClass = testClass;
            this.numAttributes = numAttributes;
            this.numGoal = numGoal;
            this.sumDistinct = sumDistinct;
            this.sumDistinctAll = sumDistinctAll;
        }

        public void run()
        {
            int   idx, i,j,pidx,cl;
            float max;
            float pcl;

            // Location of test-instance in testData
            idx = getGlobalId() * this.numAttributes;

            // Calculate the probability per class according to the Naive Bayesian model. Output is class with highest probability.
            max  = -1;
            cl   = -1;
            for (i=0; i<this.numGoal; i++)
            {
                pcl = 1.0f;
                for (j=0; j<this.numAttributes; j++)
                {
                    pidx = i*(this.numAttributes*this.sumDistinctAll) + j*(this.sumDistinct[j]) + this.testData[idx+j];
                    pcl *= this.paijvk[pidx];
                }
                pcl *= this.pvk[i];

                if (pcl > max)
                { 
                    cl = i;
                    max = pcl;
                }
            }
            
            // Set output class
            this.testClass[getGlobalId()] = cl;
        }
    }

    public static class NaiveBayesTrainKernel extends Kernel
    {
        final private int []trainData;
        final private int []caijvk;
        final private int []cvk;
        final private int numAttributes;
        final private int []sumDistinct;
        final private int sumDistinctAll;
        final private int classIdx;

        public void run() 
        {
            int goalIdx, countIdx;
            int i,j;

            i = getGlobalId();

            goalIdx = this.trainData[i*this.numAttributes+this.classIdx];
            //cvk[goalIdx]++;             // DOES NOT WORK. The ++ needs to be atomic, otherwise parallel Kernel executions will overwrite eachother
            atomicAdd(cvk, goalIdx, 1);   // Works, but makes things very slow...
            for (j=0; j<this.numAttributes; j++)
            {
                countIdx = goalIdx*(this.numAttributes*this.sumDistinctAll) + j*(this.sumDistinct[j]) + this.trainData[i*numAttributes+j];
                atomicAdd(caijvk, countIdx, 1);
                //caijvk[countIdx]++; // DOES NOT WORK.
            }
        }

        public NaiveBayesTrainKernel(int []trainData, int []caijvk, int []cvk, int numAttributes, int []sumDistinct, int sumDistinctAll, int classIdx)
        {
            this.trainData = trainData;
            this.caijvk = caijvk;
            this.cvk = cvk;
            this.numAttributes = numAttributes;
            this.sumDistinct = sumDistinct;
            this.sumDistinctAll = sumDistinctAll;
            this.classIdx = classIdx;
        }
    }

    public void initExperiment()
    {
        float []paijvk;
        float []pvk;
        int []caijvk;
        int []cvk;

        // Naive Bayes model. Counters and probabilities
        paijvk  = new float[this.numGoal*this.numAttributes*this.sumDistinctAll];
        pvk     = new float[this.numGoal];
        caijvk  = new int[this.numGoal*this.numAttributes*this.sumDistinctAll];
        cvk     = new int[this.numGoal];

        this.paijvk = paijvk;
        this.pvk    = pvk;
        this.caijvk = caijvk;
        this.cvk    = cvk;

        // The Kernel for training
        this.trainKernel = new NaiveBayesTrainKernel(this.trainData, this.caijvk, this.cvk, this.numAttributes, this.sumDistinct, this.sumDistinctAll, this.classIdx);

        // The test Instances classification output
        this.testClass  = new int[this.numTestInstances];
        this.testKernel = new NaiveBayesTestKernel(this.testData, this.paijvk, this.pvk, this.testClass, this.numAttributes, this.numGoal, this.sumDistinct, this.sumDistinctAll);
    }

    private void train()
    {
        int     i, j, k, numins, numgoal, numatt;

        // Dimensions of the training dataset
        numins  = this.numTrainInstances;
        numatt  = this.numAttributes;
        numgoal = this.numGoal;

        // Clear Arrays of [number of goal classes][number of attributes][number of classes in attribute]
        for(i=0; i<this.caijvk.length; i++)
        {
            this.caijvk[i] = 0;
            this.paijvk[i] = 1.0f;
        }
        for (i=0; i<this.cvk.length; i++)
        {
            this.cvk[i] = 0;
            this.pvk[i] = 0;
        }

        int []caijvk;
        int []cvk;

        if (!GPU)
        {
            int goalIdx, countIdx;

            caijvk = this.caijvk;
            cvk    = this.cvk;

            // Count co-occurences of all Attribute values and goal classes.
            for (i=0; i<numins; i++)
            {
                goalIdx = this.trainData[i*numAttributes+this.classIdx];
                cvk[goalIdx]++;
                for (j=0; j<numatt; j++)
                {
                    countIdx = countIdx(goalIdx, j, this.trainData[i*numAttributes+j]);
                    caijvk[countIdx]++;
                }
            }
        }
        else
        {
            // Core Naive Bayes Training
            this.trainKernel.execute(Range.create(this.numTrainInstances));

            caijvk = this.caijvk;
            cvk = this.cvk;
        }        

        int cidx;
        float []paijvk;
        float []pvk;

        // Convert calculated co-occurences into probabilities
        paijvk = this.paijvk;
        pvk    = this.pvk;
        for (i=0; i<cvk.length; i++) pvk[i] = ((float)cvk[i]) / ((float)numins);
        for (i=0; i<numgoal; i++)
        {
            for (j=0; j<numatt; j++)
            {
                if (j != this.classIdx)
                {
                    // Divide the number of co-occurences by the number of occurences of the goal class.
                    for (k=0; k<this.numDistinct[j]; k++)
                    {
                        cidx = countIdx(i, j, k);
                        if (caijvk[cidx] != 0) paijvk[cidx] = ((float)caijvk[cidx]) / ((float)cvk[i]);
                        else                   paijvk[cidx] = 0;
                    }
                }
                // Or set all goal classes equally probably for the goal attribute.
                else
                {
                    cidx = countIdx(i, j, 0);
                    for(k=0; k<this.numDistinct[j]; k++)
                    {
                        paijvk[cidx+k] = 1.0f;
                    }
                }
            }
        }
    }

    private int countIdx(int goalClass, int attribute, int valueIdx)
    {
        return goalClass*(this.numAttributes*this.sumDistinctAll) + attribute*(this.sumDistinct[attribute]) + valueIdx;
    }

    private double testConfusionCore()
    {
        int    []testClass;
        int idx, realClass;

        testClass = this.testClass;

        if (GPU)
        {
            // Test instances using the Kernel implementing the Naive Bayes classification
            this.testKernel.execute(Range.create(this.numTestInstances));
        }
        else
        {
            // Classify test instances in plain Java.
            idx       = 0;
            for(int i=0; i<this.numTestInstances; i++)
            {
                testClass[i] = classify(idx);
                idx += this.numAttributes;
            }
        }


        double [][]conf;
        double error;
        int numGoal;

        // Derive confusion matrix and classification error
        numGoal   = this.numGoal;
        error     = 0;
        conf      = new double[numGoal][numGoal];
        idx       = 0;
        for(int i=0; i<this.numTestInstances; i++)
        {
            realClass = this.testData[idx+this.classIdx];
            conf[realClass][testClass[i]]++;
            if (testClass[i] != realClass) error++;
            idx += this.numAttributes;
        }

        error /= this.numTestInstances;

        //System.out.println("Core Naive Bayes\n****************");
        //this.dataSet.printPerformance(conf);
        //System.out.println("Classification error: "+error);

        return error;
    }

    private int classify(int idx)
    {
        int   i,j,pidx,cl;
        int   numclass, numatt, sumdistinctall;
        float max;
        float pcl;

        numatt   = this.numAttributes;
        numclass = this.numGoal;
        sumdistinctall = this.sumDistinctAll;

        // Calculate the probability per class according to the Naive Bayesian Method.
        max  = -1;
        cl   = -1;
        for (i=0; i<numclass; i++)
        {
            pcl = 1.0f;
            for (j=0; j<numatt; j++)
            {
                pidx = i*(numatt*sumdistinctall) + j*(this.sumDistinct[j]) + this.testData[idx+j];
                pcl *= this.paijvk[pidx];
            }
            pcl *= this.pvk[i];

            if (pcl > max)
            { 
                cl = i;
                max = pcl;
            }
        }

        // Return classification result.
        return(cl);
    }


    private void convertDataSet() throws Exception
    {
        Instances dataSet;
        int i,j;
        int []numDistinct;
        int []sumDistinct;
        int  sumDistinctAll;
        int []trainData;
        int []testData;
        Set<Double> []distinctValues;
        Map<Double, Integer> []valueIndex;

        // Determine possible distinct values for all Attributes. Make lookup maps to translate the attribute values to indices in [0, #values-1]
        dataSet = this.dataSet.getDataSet();
        distinctValues = new Set[dataSet.numAttributes()];
        valueIndex     = new Map[dataSet.numAttributes()];
        numDistinct    = new int[dataSet.numAttributes()];
        sumDistinct    = new int[dataSet.numAttributes()];
        for(i=0; i<dataSet.numAttributes(); i++)
        {
            distinctValues[i] = new HashSet<Double>();
            valueIndex[i] = new HashMap<Double, Integer>();
        }

        for(Instance instance: dataSet)
        {
            for(j=0; j<instance.numAttributes(); j++)
                distinctValues[j].add(instance.value(j));
        }
        for(i=0; i<dataSet.numAttributes(); i++)
        {
            j = 0;
            for(Double distinctValue: distinctValues[i])
                valueIndex[i].put(distinctValue, j++);

            numDistinct[i] = valueIndex[i].size();
            if (i==0) sumDistinct[i] = 0;
            else      sumDistinct[i] = sumDistinct[i-1]+valueIndex[i-1].size();

            System.out.println(dataSet.attribute(i).name()+": [0..."+(valueIndex[i].size()-1)+"] -> "+distinctValues[i]);
        }
        sumDistinctAll = sumDistinct[i-1]+valueIndex[i-1].size();

        // Translate train- and test-datasets to 2D array of integers containing attribute value indices
        this.numTrainInstances = this.dataSet.getTrainSet().numInstances();
        this.numTestInstances = this.dataSet.getTestSet().numInstances();
        trainData = translateDataSet(this.dataSet.getTrainSet(), valueIndex);
        testData  = translateDataSet(this.dataSet.getTestSet(), valueIndex);

        this.trainData   = trainData;
        this.testData    = testData;
        this.classIdx    = dataSet.classIndex();
        this.numDistinct = numDistinct;
        this.sumDistinct = sumDistinct;
        this.sumDistinctAll = sumDistinctAll;
        this.numGoal     = numDistinct[this.classIdx];
        this.numAttributes = dataSet.numAttributes();
    }

    private int []translateDataSet(Instances dataSet, Map<Double, Integer> []valueIndex)
    {
        int i,j;
        int []data;
        int numAttributes;

        // Lookup attribute value indices for all instances
        numAttributes = dataSet.numAttributes();
        data = new int[dataSet.numInstances()*numAttributes];
        i    = 0;
        for(Instance instance: dataSet)
        {
            for(j=0; j<instance.numAttributes(); j++)
            {
                data[i*numAttributes+j] = valueIndex[j].get(instance.value(j));
            }
            i++;
        }

        return data;
    }


    private void setDataSet(CoreDataSet data)
    {
        this.dataSet = data;
    }

    private void experiment()
    {
        // Create data-structures for training.
        initExperiment();

        double sumerror;
        int i;
        long tbeg, tend;
        long trun;

        tbeg     = System.currentTimeMillis();
        sumerror = 0;
        for(i=0; i<100; i++)
        {
            // Train Naive Bayes
            train();

            // Test performance
            sumerror += testConfusionCore();

            // Randomize Instances between train- and test-sets
            randomizeInstances();

            if (i%10==0) System.out.println("... "+i);
        }
        tend = System.currentTimeMillis();
        sumerror /= i;
        System.out.println("Average classification error in "+i+" runs: "+sumerror);
        trun = (tend-tbeg)/i;
        System.out.println("Total time: "+(tend-tbeg)+". Average time per run: "+trun);
    }

    private void randomizeInstances()
    {
        int       i;
        boolean []picked;
        int       idx;

        picked = new boolean[this.numTrainInstances];
        for(i=0; i<this.numTestInstances; i++)
        {
            idx = Uniform.staticNextIntFromTo(0, this.numTrainInstances-1);
            while(picked[idx]) idx = (idx+1)%this.numTrainInstances;
            picked[idx] = true;
            swapTrainTest(idx, i);
        }
    }

    private void swapTrainTest(int trainIdx, int testIdx)
    {
        int buf;
        int trainPos = trainIdx*this.numAttributes;
        int testPos = testIdx*this.numAttributes;
        for(int i=0; i<this.numAttributes; i++)
        {
            buf = this.testData[testPos+i];
            this.testData[testPos+i] = this.trainData[trainPos+i];
            this.trainData[trainPos+i] = buf;
        }
    }

    public static void main(String []args)
    {
        try
        {
            CoreDataSet data = new CoreDataSet();
            CoreNaiveBayesGPU nb = new CoreNaiveBayesGPU();
            nb.setDataSet(data);

            // CH(ess) dataset: http://www.hakank.org/weka/
            //data.readWekaDataSet("ch.arff");

            // MU(shroom classification) dataset: http://www.hakank.org/weka/
            //data.readWekaDataSet("mu.arff");

            // Codon RNA dataset
            data.readWekaDataSet("cod-rna-train.arff");

            // W8A
            //data.readWekaDataSet("w8a.train.arff");

            //data.readWekaDataSet("spambase_real.arff");

            // Tree Cover type: 581012 instances and 55 Attributes.
            // Avg time train GPU,  test Java                                 : 839
            //          train Java, test Java / GPU                           : 115 / 114
            //          Train Java, test Java / GPU. No instance randomization:  74 /  72
            //data.readWekaDataSet("covtype.train.arff");

            // Convert DataSet(s) to primitive format
            nb.convertDataSet();

            nb.experiment();
        }
        catch(Throwable ex)
        {
            ex.printStackTrace();
            //System.exit(5);
        }
    }
}
