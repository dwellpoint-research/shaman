package org.shaman.spatial;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cern.jet.random.Uniform;

import weka.core.Instance;
import weka.core.Instances;

public class CoreNaiveBayes
{
    private CoreDataSet dataSet;

    // DataSets pre-processed for simplified train/test methods 
    private int [][]trainData;
    private int [][]testData;
    private int []numDistinct;
    private int classIdx;

    // The Naive Bayes Classifier Model
    private double [][][]paijvk;            // p(aij|vk) indexed with [k][i][j]
    private double []pvk;                   // p(vk)

    public void train() // doesn't throw excepion
    {
        int     i, j, k, numins, numgoal, numatt;

        // Naive Bayes model. Counters and probabilities
        double [][][]paijvk;
        double []pvk;
        double [][][]caijvk;
        double []cvk;

        // Dimensions of the training dataset
        numins  = this.trainData.length;
        numatt  = this.trainData[0].length;
        numgoal = this.numDistinct[this.classIdx];

        // Make the probability and counter buffers.
        paijvk  = new double[numgoal][][];
        pvk     = new double[numgoal];
        caijvk  = new double[numgoal][][];
        cvk     = new double[numgoal];

        // Arrays of [number of goal classes][number of attributes][number of classes in attribute]
        for (i=0; i<numgoal; i++)
        {
            paijvk[i] = new double[numatt][];
            caijvk[i] = new double[numatt][];
            for (j=0; j<numatt; j++)
            {
                paijvk[i][j] = new double[this.numDistinct[j]];
                caijvk[i][j] = new double[this.numDistinct[j]];
                for (k=0; k<paijvk[i][j].length; k++) { paijvk[i][j][k] = 1.0; caijvk[i][j][k] = 0; }
            }
            pvk[i] = 0; cvk[i] = 0;
        }

        int goalIdx;

        // Count co-occurences of all Attribute values and goal classes.
        for (i=0; i<numins; i++)
        {
            goalIdx = this.trainData[i][this.classIdx];
            cvk[goalIdx]++;
            for (j=0; (j<numatt); j++)
            {
                caijvk[goalIdx][j][this.trainData[i][j]]++;
            }
        }
        for (i=0; i<cvk.length; i++) pvk[i] = ((double)cvk[i]) / ((double)numins);

        // Convert the counters to probabilities
        for (i=0; i<numgoal; i++)
        {
            for (j=0; j<numatt; j++)
            {
                if (j != this.classIdx)
                {
                    // Divide the number of co-occurences by the number of occurences of the goal class.
                    for (k=0; k<caijvk[i][j].length; k++)
                    {
                        if (caijvk[i][j][k] != 0) paijvk[i][j][k] = ((double)caijvk[i][j][k]) / ((double)cvk[i]);
                        else                      paijvk[i][j][k] = 0;
                    }
                }
                // Or set all goal classes equally probably for the goal attribute.
                else for(k=0; k<caijvk[i][j].length; k++) paijvk[i][j][k] = 1.0;
            }
        }

        this.paijvk = paijvk;
        this.pvk    = pvk;
    }

    private int classify(int []instance)
    {
        int      i,j,cl;
        int      numclass, numatt;
        double   max;
        double []pcl;

        numatt   = instance.length;
        numclass = this.numDistinct[this.classIdx];
        pcl      = new double[numclass];

        // Calculate the probability per class according to the Naive Bayesian Method.
        max = -1;
        cl  = -1;
        for (i=0; i<numclass; i++)
        {
            pcl[i] = 1.0;
            for (j=0; j<numatt; j++)
            {
                pcl[i] *= this.paijvk[i][j][instance[j]];
            }
            pcl[i] *= this.pvk[i];

            if (pcl[i] > max)
            { 
                cl = i;
                max = pcl[i];
            }
        }

        // Return classification result.
        return(cl);
    }
    
    private void experiment()
    {
        // Create data-structures for training.
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
        int []buf;

        picked = new boolean[this.trainData.length];
        for(i=0; i<this.testData.length; i++)
        {
            idx = Uniform.staticNextIntFromTo(0, this.trainData.length-1);
            while(picked[idx]) idx = (idx+1)%this.trainData.length;
            picked[idx] = true;
            buf = this.testData[i];
            this.testData[i] = this.trainData[idx];
            this.trainData[idx] = buf;
        }
    }

    private double testConfusionCore()
    {
        double [][]conf;
        int numClass, testClass, realClass;
        double error;

        error    = 0;
        numClass = this.numDistinct[this.classIdx];
        conf     = new double[numClass][numClass];
        for(int []instance: this.testData)
        {
            testClass = classify(instance);
            realClass = instance[this.classIdx];
            conf[realClass][testClass]++;
            if (testClass != realClass) error++;
        }
        error /= this.testData.length;

        //System.out.println("Core Naive Bayes\n****************");
        //this.dataSet.printPerformance(conf);
        //System.out.println("Classification error: "+error);
        
        return error;
    }


    private void convertDataSet() throws Exception
    {
        Instances dataSet;
        int i,j;
        int []numDistinct;
        int [][]trainData;
        int [][]testData;
        Set<Double> []distinctValues;
        Map<Double, Integer> []valueIndex;

        // Determine possible distinct values for all Attributes. Make lookup maps to translate the attribute values to indices in [0, #values-1]
        dataSet = this.dataSet.getDataSet();
        distinctValues = new Set[dataSet.numAttributes()];
        valueIndex     = new Map[dataSet.numAttributes()];
        numDistinct    = new int[dataSet.numAttributes()];
        for(i=0; i<distinctValues.length; i++)
        {
            distinctValues[i] = new HashSet<Double>();
            valueIndex[i] = new HashMap<Double, Integer>();
        }

        for(Instance instance: dataSet)
        {
            for(j=0; j<instance.numAttributes(); j++)
                distinctValues[j].add(instance.value(j));
        }
        for(i=0; i<valueIndex.length; i++)
        {
            j = 0;
            for(Double distinctValue: distinctValues[i])
                valueIndex[i].put(distinctValue, j++);

            numDistinct[i] = valueIndex[i].size();

            System.out.println(dataSet.attribute(i).name()+": [0..."+(valueIndex[i].size()-1)+"] -> "+distinctValues[i]);
        }

        // Translate train- and test-datasets to 2D array of integers containing attribute value indices
        trainData = translateDataSet(this.dataSet.getTrainSet(), valueIndex);
        testData  = translateDataSet(this.dataSet.getTestSet(), valueIndex);

        this.trainData = trainData;
        this.testData  = testData;
        this.numDistinct = numDistinct;
        this.classIdx = dataSet.classIndex();
    }

    private int [][]translateDataSet(Instances dataSet, Map<Double, Integer> []valueIndex)
    {
        int i,j;
        int [][]data;

        // Lookup attribute value indices for all instances
        data = new int[dataSet.numInstances()][dataSet.numAttributes()];
        i    = 0;
        for(Instance instance: dataSet)
        {
            for(j=0; j<instance.numAttributes(); j++)
            {
                data[i][j] = valueIndex[j].get(instance.value(j));
            }
            i++;
        }

        return data;
    }


    private void setDataSet(CoreDataSet data)
    {
        this.dataSet = data;
    }


    public static void main(String []args)
    {
        try
        {
            CoreDataSet data = new CoreDataSet();
            CoreNaiveBayes nb = new CoreNaiveBayes();
            nb.setDataSet(data);

            // CH(ess) dataset: http://www.hakank.org/weka/
            //data.readWekaDataSet("ch.arff");

            // MU(shroom classification) dataset: http://www.hakank.org/weka/
            data.readWekaDataSet("mu.arff");

            // Codon RNA dataset
            //data.readWekaDataSet("cod-rna-train.arff");

            // W8A
            //data.readWekaDataSet("w8a.train.arff");

            //data.readWekaDataSet("spambase_real.arff");

            // Tree Cover type: 581012 instances and 55 Attributes.
            // WEKA           Train/Test time: 1531 ms
            // CoreNaiveBayes Train/Test time:  137 ms
            //data.readWekaDataSet("covtype.train.arff");

            // Convert DataSet(s) to primitive format
            nb.convertDataSet();

            // Train Naive Bayes
            nb.train();

            // Test performance
            nb.experiment();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            System.exit(5);
        }
    }
}
