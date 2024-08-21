package org.shaman.rule;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;
import org.shaman.learning.Classifier;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;
import org.shaman.learning.Validation;
import org.shaman.preprocessing.Discretization;
import org.shaman.preprocessing.Normalization;
import org.shaman.preprocessing.PCA;

import java.io.FileInputStream;
import java.io.ObjectInputStream;

/**
 * @author Johan Kaers
 */
public class Experiments
{
    private void digitForest() throws Exception
    {
     /*
       PCA (20) -> RandomForest(50,0.75,2,SQRT)
        955    0   2   3   1   6   8   3   1   1
        0 1121   3   2   0   1   5   0   3   0
        15    0 940  26   6   1   5  11  27   1
        0    0  14 912   0  21   2  18  37   6
        2    3   5   2 908   2  12   6   4  38
        5    0   2  27   5 804  16   4  21   8
        8    4   5   0   5   9 925   0   2   0
        0    9  16   3   7   0   1 938   9  45
        8    0  14  40  12  17   6  11 860   6
        2    5   3  20  37   6   1  22  12 901
        Error: 0.0736

        PCA (20) -> RandomForest(50,0.75,2) just Bagging no RandomForest
        Confusion Matrix10 x 10 matrix
        936    0   7   7   0  12  13   1   3   1
        0 1117   5   4   0   0   5   0   3   1
        21    1 888  44   9   4   9  13  40   3
        2    1  21 896   1  20   2  22  34  11
        6    1  10   5 877   4  12   7  10  50
        13    2  12  34  10 767  11   5  30   8
        11    4  10   3  11  10 906   0   3   0
        3   11  16  16   9   4   3 900   9  57
        12    1  20  50  22  27   8  12 816   6
        6    6   3  17  50   5   1  29  14 878
        Error: 0.1019
        */

        // Classifier Flow for Handwritten Digit Recognition
        MemorySupplier msTrain = new MemorySupplier();
        MemorySupplier msTest  = new MemorySupplier();
        RandomForestClassifier forest = new RandomForestClassifier();
        PCA pca = new PCA();

        msTrain.registerConsumer(0, pca, 0);
        pca.registerSupplier(0, msTrain, 0);
        forest.registerSupplier(0, pca, 0);

        InstanceSetMemory imTrain = new InstanceSetMemory();
        InstanceSetMemory imTest  = new InstanceSetMemory();
        InstanceSetMemory imprep = new InstanceSetMemory();

        // Load the Data-Sets
        TestSets.loadMNIST(msTrain, new String[]{"./src/main/resources/data/mnist/train-images-idx3-ubyte", "./src/main/resources/data/mnist/train-labels-idx1-ubyte"});
        //TestSets.loadMNIST(msTrain, new String[]{"./src/main/resources/data/mnist/t10k-images-idx3-ubyte",  "./src/main/resources/data/mnist/t10k-labels-idx1-ubyte"});
        TestSets.loadMNIST(msTest,  new String[]{"./src/main/resources/data/mnist/t10k-images-idx3-ubyte",  "./src/main/resources/data/mnist/t10k-labels-idx1-ubyte"});

        imTrain.create(msTrain);
        imTest.create(msTest);
        System.out.println("Read MNIST handwritten digit train/test data-sets of "+imTrain.getNumberOfInstances()+"/"+imTest.getNumberOfInstances()+" instances.");


        //String pcaFile = "/Users/johan/SoftDev/temp/mnist_digit_t10k_pca20.obj";
        String pcaFile = "/Users/johan/SoftDev/temp/mnist_digit_train_pca20.obj";

        long tbeg, tend;
    /*
            System.out.println("Calculating Principal Components for training data-set.");
            // Find the 20 most principal components. Apply transformation on the data.
            tbeg = System.currentTimeMillis();
            pca.setType(PCA.TYPE_LINEAR);
            pca.setNumberOfPC(5);
            //pca.setNumberOfPC(20);
            pca.init();
            pca.trainTransformation(imTrain);

            tend = System.currentTimeMillis();
            System.out.println("Calculated Principal Components in "+(tend-tbeg)+" ms");
            ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream(pcaFile));
            pca.saveState(oout);
            oout.close();
            */
        System.out.println("Loading PCAs from "+pcaFile);
        ObjectInputStream oin = new ObjectInputStream(new FileInputStream(pcaFile));
        pca.loadState(oin);
        oin.close();
        pca.init();

        System.out.println("Applying PCA and standardization on train and test data-set.");
        imTrain = InstanceSetMemory.estimateAll(imTrain, pca);
        // Do the same pre-processing on the test dataset
        imTest = InstanceSetMemory.estimateAll(imTest, pca);

        // Configure and initialize the Random Forest
        forest.setClassifierOutput(Classifier.OUT_CLASS);
        forest.setNumberOfTrees(50);
        forest.setTrainFraction(0.75);
        forest.setMinObjects(2);
        //forest.setMaxDepth(4);
        forest.setNumberOfVariables(RandomForestTree.NUMBER_OF_VARIABLES_SQRT);
        forest.init();

        // Run a Cross-Validation for the classifier.
        System.out.println("Training and testing Cross-Validation Decision Tree");
        double       [][]cmraw;
        DoubleMatrix2D cm;
        Validation val = new Validation(imTrain, forest);
        //val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{3.0});
        val.create(Validation.SPLIT_TRAIN_TEST, new double[]{0.0});
        val.setTrainTestSet(imTrain, imTest);
        val.test();
        cmraw = val.getValidationClassifier().getConfusionMatrix();
        cm = DoubleFactory2D.dense.make(cmraw);
        System.out.println("Confusion Matrix"+cm);
        System.out.println("Classification Error "+val.getValidationClassifier().getClassificationError()+"\nError: "+errorFromConfusionMatrix(cmraw));
        System.out.println("Error classifying "+val.getValidationClassifier().getErrorOfClassification());
    }

    private void wineForest() throws Exception
    {
        // Classifier Flow for Handwritten Digit Recognition
        MemorySupplier msTrain = new MemorySupplier();
        MemorySupplier msTest  = new MemorySupplier();
        RandomForestClassifier forest = new RandomForestClassifier();
        PCA pca = new PCA();

        msTrain.registerConsumer(0, pca, 0);
        pca.registerSupplier(0, msTrain, 0);
        forest.registerSupplier(0, pca, 0);

        InstanceSetMemory imTrain = new InstanceSetMemory();
        InstanceSetMemory imTest  = new InstanceSetMemory();
        InstanceSetMemory imprep = new InstanceSetMemory();
        TestSets.loadWine(msTrain);
        TestSets.loadWine(msTest);

        imTrain.create(msTrain);
        imTest.create(msTest);
        System.out.println("Read MNIST handwritten digit train/test data-sets of "+imTrain.getNumberOfInstances()+"/"+imTest.getNumberOfInstances()+" instances.");


        System.out.println("Calculating Principal Components for training data-set.");
        pca.setType(PCA.TYPE_LINEAR);
        pca.setNumberOfPC(5);
        pca.init();
        pca.trainTransformation(imTrain);

        System.out.println("Applying PCA and standardization on train and test data-set.");
        imTrain = InstanceSetMemory.estimateAll(imTrain, pca);
        // Do the same pre-processing on the test dataset
        imTest = InstanceSetMemory.estimateAll(imTest, pca);

        // Configure and initialize the Random Forest
        forest.setClassifierOutput(Classifier.OUT_CLASS);
        forest.setNumberOfTrees(50);
        forest.setTrainFraction(0.66);
        forest.setMinObjects(2);
        //forest.setMaxDepth(4);
        forest.setNumberOfVariables(RandomForestTree.NUMBER_OF_VARIABLES_SQRT);
        forest.init();

        // Run a Cross-Validation for the classifier.
        System.out.println("Training and testing Cross-Validation Decision Tree");
        double       [][]cmraw;
        DoubleMatrix2D cm;
        Validation val = new Validation(imTrain, forest);
        //val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{3.0});
        val.create(Validation.SPLIT_TRAIN_TEST, new double[]{0.0});
        val.setTrainTestSet(imTrain, imTest);
        val.test();
        cmraw = val.getValidationClassifier().getConfusionMatrix();
        cm = DoubleFactory2D.dense.make(cmraw);
        System.out.println("Confusion Matrix"+cm);
        System.out.println("Classification Error "+val.getValidationClassifier().getClassificationError()+"\nError: "+errorFromConfusionMatrix(cmraw));
        System.out.println("Error classifying "+val.getValidationClassifier().getErrorOfClassification());

        System.out.println("Out-of-bag error estimate: "+forest.getOutOfBagError());
    }

    private double errorFromConfusionMatrix(double [][]conf)
    {
        int cntAll, cntRight;
        double error;

        // Count the total number of instances and the ones on the diagonal.
        error = 0;
        cntAll = cntRight = 0;
        for(int i=0; i<conf.length; i++)
            for(int j=0; j<conf[i].length; j++)
            {
                if (i==j) cntRight += conf[i][j];
                cntAll += conf[i][j];
            }

        // Error is 1.0 - percentage right
        error = cntAll - cntRight;
        error = error / cntAll;

        return error;
    }

    public static void main(String []args)
    {
        Experiments app;

        app = new Experiments();
        try
        {
            app.wineForest();
            //app.digitForest();
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
}