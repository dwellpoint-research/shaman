package org.shaman.spatial;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.experiment.Stats;
import weka.filters.Filter;
import weka.filters.supervised.attribute.Discretize;

public class CoreDataSet
{
    // WEKA logic to compare against
    private Classifier classifier;
    private Instances dataSet, trainSet, testSet;
    
    private void initWekaClassifier() throws Exception
    {
        NaiveBayes classifier = new NaiveBayes();
        List<String> options = new ArrayList<String>();
        //options.add("-D");
        //options.add("-K");
        classifier.setOptions(options.toArray(new String[0]));

        this.classifier = classifier;
    }
    
    public void readWekaDataSet(String dataSetName) throws Exception
    {
        DataSource source;
        Attribute attribute;
        Instances dataSet;
        String dataDir = "/Users/johan/SoftDev/Papers/Spatial Ensemble Learning/datasets/";

        // Read the dataSet
        source = new DataSource(dataDir+"/"+dataSetName);
        dataSet = source.getDataSet();
        dataSet.setClass(dataSet.attribute("class"));
        int classIndex = dataSet.classIndex();
        System.out.println("DataSet "+dataSetName+" containing "+dataSet.numInstances()+" Instances and "+dataSet.numAttributes()+" Attributes.");
        for(int i=0; i<dataSet.numAttributes(); i++)
        {
            attribute = dataSet.attribute(i);
            if (attribute.isNominal())
            {
                System.out.println("\t"+attribute.name()+" "+dataSet.attributeStats(i).distinctCount+" "+(i==classIndex?"*":""));
            }
            else if (attribute.isNumeric())
            {
                Stats stats = dataSet.attributeStats(i).numericStats;
                System.out.println("\t"+attribute.name()+" in ["+stats.min+", "+stats.max+"]");
            }
        }

        Instances trainSet, testSet;
        
        // Split up into train/test-set
        dataSet.randomize(new Random(System.currentTimeMillis()));
        dataSet.stratify(3);
        trainSet = dataSet.trainCV(3, 0);
        testSet  = dataSet.testCV(3, 0);

        
        int i;
        int classIdx;

        // Combine Train- and TestSets
        classIdx = dataSet.classIndex();

        boolean doDiscretize;
        List<Integer> lNumeric;

        // Discretize continuous attributes?
        doDiscretize = false;
        lNumeric = new LinkedList<Integer>();
        for(i=0; i<dataSet.numAttributes(); i++)
        {
            if (i != classIdx)
            {
                if (dataSet.attribute(i).isNumeric())
                {
                    lNumeric.add(i);
                    doDiscretize = true;
                }
            }
        }
        if (doDiscretize)
        {
            Discretize discretize;

            i= 0;
            int []numericIdx = new int[lNumeric.size()];
            for(Integer num: lNumeric) { numericIdx[i] = num; i++; }

            System.out.println("Discretizing "+lNumeric.size()+" Attribtues for CoreNaiveBayes dataset");
            discretize = new Discretize();
            discretize.setAttributeIndicesArray(numericIdx);
            discretize.setInputFormat(dataSet);
            discretize.setUseBetterEncoding(true);
            System.out.println("\t... Total DataSet");
            dataSet = Filter.useFilter(dataSet, discretize);
            System.out.println("\t... TrainSet");
            trainSet = Filter.useFilter(trainSet, discretize);
            System.out.println("\t... TestSet");
            testSet  = Filter.useFilter(testSet, discretize);
        }
        
        double errtotaltest;
        double [][]errtotalconf;
        long tbeg, tend;

        tbeg = System.currentTimeMillis();
        // Train/Test and print the confusion matrix / classification error.
        initWekaClassifier();
        this.classifier.buildClassifier(trainSet);
        errtotalconf = testConfusion(testSet);
        tend = System.currentTimeMillis();
        errtotaltest = printPerformance(errtotalconf);
        System.out.println("Classification error "+errtotaltest+". Run time "+(tend-tbeg)+" ms.");
        
        this.trainSet = trainSet;
        this.testSet  = testSet;
        this.dataSet  = dataSet;
    }
    
    public double printPerformance(double [][]conf)
    {
        StringBuffer sconf = new StringBuffer();
        double cntcorrect, cntall, err;

        sconf.append("Confusion Matrix:\n");
        cntcorrect = 0;
        cntall     = 0;
        for(int i=0; i<conf.length; i++)
        {
            for(int j=0; j<conf[i].length; j++)
            {
                cntall += conf[i][j];
                if (i==j) cntcorrect += conf[i][j];

                sconf.append("\t"+conf[i][j]);
            }
            sconf.append("\n");
        }
        err = 1.0 - (cntcorrect / cntall);

        System.out.println(sconf.toString());

        return(err);
    }
    
    public double [][]testConfusion(Instances instances) throws Exception
    {
        Attribute  atclass;
        double [][]cmatrix;
        double   []conf;
        double     maxconf;
        int        numclass, instanceclass, maxidx;

        // Create confusion matrix for the given test-set of instances
        atclass  = instances.classAttribute();
        numclass = atclass.numValues();
        cmatrix  = new double[numclass][numclass];
        for(Instance instance: instances)
        {
            instanceclass = (int)instance.classValue();
            conf          = this.classifier.distributionForInstance(instance);
            maxconf       = Double.NEGATIVE_INFINITY;
            maxidx        = -1;
            for(int i=0; i<conf.length; i++)
                if (conf[i] > maxconf) { maxconf = conf[i]; maxidx = i; }
            cmatrix[instanceclass][maxidx]++;
        }

        return(cmatrix);
    }
    
    public Instances getTrainSet() { return this.trainSet; }
    public Instances getTestSet() { return this.testSet; }
    public Instances getDataSet() { return this.dataSet; }

}
