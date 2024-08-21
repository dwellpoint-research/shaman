package org.shaman.rule.weka;

import org.shaman.rule.RandomForest;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Johan Kaers
 */
public class CoreRandomForestTest
{
    private Instances dataset;
    private Instances testset;
    private CoreRandomForest forest;

    public static final String DATA_DIR = "/Users/johan/SoftDev/Papers/Spatial Ensemble Learning/datasets/";
    public static final String EXTASY_DIR = "/Users/johan/Dev/eXtasy/verify/2014/";
    public static final String BRUTUSS_DIR = "/Users/johan/Dev/Splice/brutus/";

    /* eXtasy results:
    Out-of-bag error estimate: 0.05480783486812843
    === Summary ===

    Correctly Classified Instances       28736               94.6758 %
    Incorrectly Classified Instances      1616                5.3242 %
    Kappa statistic                          0.8935
    K&B Relative Info Score            2482679.6181 %
    K&B Information Score                24822.9206 bits      0.8178 bits/instance
    Class complexity | order 0           30347.2724 bits      0.9998 bits/instance
    Class complexity | scheme            13788.5168 bits      0.4543 bits/instance
    Complexity improvement     (Sf)      16558.7556 bits      0.5456 bits/instance
    Mean absolute error                      0.1001
    Root mean squared error                  0.2031
    Relative absolute error                 20.0244 %
    Root relative squared error             40.6212 %
    Coverage of cases (0.95 level)          99.7694 %
    Mean rel. region size (0.95 level)      67.8802 %
    Total Number of Instances            30352

    Area under ROC curve 0.9885627150277702
    Area under PRC curve 0.9883711371587764
    Confusion matrix:
    	14636.0	763.0
    	853.0	14100.0
     */

    /* BRUTUSS results: crap
     */

    public void run() throws Exception
    {
        // eXtasy model training / testing
        loadData();
        //trainForest();
        //saveForest(EXTASY_DIR+"extasy_forest.obj");
        loadForest(EXTASY_DIR+"extasy_forest.obj");
        testForest();

        // BRUTUSS model training / testing
        //loadDataBrutuss();
        //trainForestBrutuss();
        //testForest();
    }

    private void trainForestBrutuss() throws Exception
    {
        CoreRandomForest forest;
        List<String> options;

        // Configure the Random Forest
        options = new ArrayList<String>();
        options.add("-I"); options.add("50");
        options.add("-M"); options.add("2");
        options.add("-K"); options.add("1");
        options.add("-F"); options.add("0.66");
        options.add("-S"); options.add(""+Math.abs((int)System.currentTimeMillis()));
        forest = new CoreRandomForest();
        forest.setOptions(options.toArray(new String[options.size()]));

        // Train on the dataset
        forest.buildClassifier(this.dataset);

        System.out.println("Out-of-bag error estimate: "+forest.getMeasure("measureOutOfBagError"));
        this.forest = forest;
    }

    private void trainForest() throws Exception
    {
        CoreRandomForest forest;
        List<String> options;

        // Configure the Random Forest
        options = new ArrayList<String>();
        options.add("-I"); options.add("100");
        options.add("-M"); options.add("2");
        options.add("-K"); options.add("1");
        options.add("-F"); options.add("0.66");
        options.add("-S"); options.add(""+Math.abs((int)System.currentTimeMillis()));
        forest = new CoreRandomForest();
        forest.setOptions(options.toArray(new String[options.size()]));

        // Train on the dataset
        forest.buildClassifier(this.dataset);

        System.out.println("Out-of-bag error estimate: "+forest.getMeasure("measureOutOfBagError"));
        this.forest = forest;
    }

    private void testForest() throws Exception
    {
        System.out.print("Testing Forest");
        // Evaluate and collect statistics on the testset.
        Evaluation eval = new Evaluation(this.dataset);
        eval.evaluateModel(this.forest, this.testset);

        System.out.println(eval.toSummaryString(true));
        System.out.println("Area under ROC curve " + eval.areaUnderROC(1));
        System.out.println("Area under PRC curve " + eval.areaUnderPRC(1));
        System.out.println("Confusion matrix:");
        System.out.println("\t"+eval.numTrueNegatives(1)+"\t"+eval.numFalsePositives(1));
        System.out.println("\t"+eval.numFalseNegatives(1)+"\t"+eval.numTruePositives(1));
    }

    private void loadForest(String path) throws Exception
    {
        CoreRandomForest forest;
        RandomForest randomForest;
        ObjectInputStream objectInputStream;

        // Load the Random Forest in its trained state.
        forest = new CoreRandomForest();
        randomForest = new RandomForest();
        forest.setRandomForest(randomForest);
        objectInputStream = new ObjectInputStream(new FileInputStream(path));
        randomForest.loadState(objectInputStream);
        objectInputStream.close();

        this.forest = forest;
    }

    private void saveForest(String path) throws Exception
    {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(path));
        this.forest.getRandomForest().saveState(objectOutputStream);
        objectOutputStream.flush();
        objectOutputStream.close();
    }

    public void loadDataBrutuss() throws Exception
    {
        DataSource source;
        Instances dataSet, trainSet, testSet;

        // Read the train and test sets
        source = new DataSource(BRUTUSS_DIR+"donor_train.arff");
        dataSet = source.getDataSet();
        dataSet.setClass(dataSet.attribute("class"));
        System.out.println("Read BRUTUSS train dataset of "+dataSet.numInstances()+" instances.");

        source = new DataSource(BRUTUSS_DIR+"donor_test.arff");
        testSet = source.getDataSet();
        testSet.setClass(testSet.attribute("class"));
        System.out.println("Read BRUTUSS test dataset of "+testSet.numInstances()+" instances.");

        this.dataset = dataSet;
        this.testset = testSet;
    }

    public void loadData() throws Exception
    {
        DataSource source;

        /*
        source = new DataSource(DATA_DIR+"covtype.train.arff");
        this.dataset = source.getDataSet();
        this.dataset.setClass(dataset.attribute("class"));
        source = new DataSource(DATA_DIR+"covtype.test.arff");
        this.testset = source.getDataSet();
        this.testset.setClass(dataset.attribute("class"));
        */

        /*
         // CH dataset: http://www.hakank.org/weka/
        source = new DataSource(DATA_DIR+"ch.arff");
        this.dataset = source.getDataSet();
        this.dataset.setClass(dataset.attribute("class"));
        source = new DataSource(DATA_DIR+"ch.arff");
        this.testset = source.getDataSet();
        this.testset.setClass(dataset.attribute("class"));
        */

        /*
        source = new DataSource(DATA_DIR+"cod-rna-train.arff");
        this.dataset = source.getDataSet();
        this.dataset.setClass(dataset.attribute("class"));
        source = new DataSource(DATA_DIR+"cod-rna-test.arff");
        this.testset = source.getDataSet();
        this.testset.setClass(dataset.attribute("class"));
        */

        DataSource extasySource;
        Instances dataSet, trainSet, testSet;

        // Read the eXtasy dataset
        extasySource = new DataSource(EXTASY_DIR+"extasy.arff");
        dataSet = extasySource.getDataSet();
        dataSet.setClass(dataSet.attribute("class"));
        System.out.println("Read eXtasy dataset of "+dataSet.numInstances()+" instances.");

        // Randomize and stratify the instances. Split up in 2/3 train, 1/3 test set.
        dataSet.randomize(new Random(1));
        dataSet.stratify(3);
        trainSet = dataSet.trainCV(3, 0);
        testSet = dataSet.testCV(3, 0);

        System.out.println("Training on "+trainSet.numInstances()+" testing on "+testSet.numInstances());

        this.dataset = trainSet;
        this.testset = testSet;
    }

    public static void main(String []args)
    {
        CoreRandomForestTest test;

        test = new CoreRandomForestTest();
        try
        {
            test.run();
            System.exit(0);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            System.exit(5);
        }
    }
}
