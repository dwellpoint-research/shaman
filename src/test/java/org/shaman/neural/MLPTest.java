/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2002-5 Shaman Research                 *
\*********************************************************/
package org.shaman.neural;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.shaman.learning.Classifier;
import org.shaman.learning.InstanceBatch;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;
import org.shaman.learning.Validation;
import org.shaman.learning.ValidationClassifier;
import org.shaman.learning.ValidationEstimator;
import org.shaman.neural.MLP;
import org.shaman.neural.MLPClassifier;
import org.shaman.neural.MLPEstimator;
import org.shaman.neural.Neuron;
import org.shaman.preprocessing.Normalization;

import junit.framework.TestCase;


/**
 * Multi-Layer Feed-Forward Neural Network Test Case
 */
public class MLPTest extends TestCase
{
    // **********************************************************\
    // *                   Test MLP Estimation                  *
    // **********************************************************/
    public void testSine() throws Exception
    {
        MemorySupplier    ms = new MemorySupplier();
        InstanceSetMemory im = new InstanceSetMemory();
        MLP              mlp = new MLP();
        MLPEstimator     mes = new MLPEstimator();
        
        ms.registerConsumer(0, mes, 0);
        ms.registerConsumer(0, im, 0);
        mes.registerSupplier(0, ms, 0);
        
        // Load a sine wave
        TestSets.loadSine(ms, 200, 1.37);
        
        // Create the Estimator for the Sine wave data
        mlp.setNeuronType(Neuron.ACTIVATION_SIGMOID_TANH, new double[]{1.0});
        mlp.setNetworkParameters(7, 0, MLP.OUTPUT_REGRESSION);
        mlp.setBatchParameters(InstanceBatch.BATCH_REORDER, InstanceBatch.GOAL_BALANCE_NONE, 1.0);
        mlp.setBackPropagationParameters(0.08, true, 0.9, 200);
        mes.setMLP(mlp);
        
        // Create the DataSet from the loaded data
        im.create(ms);
        
        // Train using Cross Validation
        Validation          val;
        ValidationEstimator valest;
        double              err;
        
        val = new Validation(im, mes);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{10.0});
        val.test();
        valest = val.getValidationEstimator();
        err = valest.getError(ValidationEstimator.ERROR_MEAN_SQUARED);
        assertTrue(err < 0.5);
    }
    
    // **********************************************************\
    // *                Test MLP Classification                 *
    // **********************************************************/
    public void testCancer() throws Exception
    {
        MemorySupplier    ms = new MemorySupplier();
        InstanceSetMemory im = new InstanceSetMemory();
        Normalization   norm = new Normalization();
        MLP              mlp = new MLP();
        MLPClassifier    mcl = new MLPClassifier();
        
        norm.registerSupplier(0, ms, 0);
        ms.registerConsumer(0, norm, 0);
        norm.registerConsumer(0, mcl, 0);
        mcl.registerSupplier(0, norm, 0);
        
        // Load Wisconsin Breast Cancer data-set
        TestSets.loadCancer(ms, true, true);
        
        // Standardize the data.
        norm.setType(Normalization.TYPE_STANDARDIZE);
        norm.init();
        
        // Set-up feed-forward neural net, using backpropagation for training.
        mlp.setNeuronType(Neuron.ACTIVATION_SIGMOID_EXP, new double[]{.5});
        mlp.setBackPropagationParameters(0.001, true, 0.9, 200);
        mlp.setBatchParameters(InstanceBatch.BATCH_REORDER, InstanceBatch.GOAL_BALANCE_NONE, 1.0);
        mlp.setNetworkParameters(10, 5, MLP.OUTPUT_ONE_OF_N);
        mcl.setMLP(mlp);
        mcl.setClassifierOutput(Classifier.OUT_CLASS_AND_CONFIDENCE);
        mcl.init();
        
        // Create the DataSet from the loaded data
        im.create(ms);
        
        // Standardize the data
        norm.trainTransformation(im);
        im = InstanceSetMemory.estimateAll(im, norm);
        
        // Train using Cross Validation
        Validation           val;
        ValidationClassifier valclas;
        
        val = new Validation(im, mcl);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{5.0});
        val.test();
        valclas = val.getValidationClassifier();
        
        //       double [][]cmraw = valclas.getConfusionMatrix();
        //       DoubleMatrix2D cm    = DoubleFactory2D.dense.make(cmraw);
        //       System.out.println(cm);
        //       System.out.println("Error : "+valclas.getClassificationError()+"\n");
        
        // Check if the Classification Error is around the expected value...
        assertEquals(0.03, valclas.getClassificationError(), 0.25);
    }
    
    // **********************************************************\
    // *                  Test MLP Persistence                  *
    // **********************************************************/
    public void testClassifierPersistence() throws Exception
    {
        MemorySupplier    ms = new MemorySupplier();
        InstanceSetMemory im = new InstanceSetMemory();
        Normalization   norm = new Normalization();
        MLP              mlp = new MLP();
        MLPClassifier    mcl = new MLPClassifier();
        
        norm.registerSupplier(0, ms, 0);
        ms.registerConsumer(0, norm, 0);
        norm.registerConsumer(0, mcl, 0);
        mcl.registerSupplier(0, norm, 0);
        
        // Load Wisconsin Breast Cancer data-set
        TestSets.loadCancer(ms, true, true);
        
        // Standardize the data.
        norm.setType(Normalization.TYPE_STANDARDIZE);
        norm.init();
        
        // Set-up feed-forward neural net, using backpropagation for training.
        mlp.setNeuronType(Neuron.ACTIVATION_SIGMOID_EXP, new double[]{.5});
        mlp.setBackPropagationParameters(0.001, true, 0.9, 200);
        mlp.setBatchParameters(InstanceBatch.BATCH_REORDER, InstanceBatch.GOAL_BALANCE_NONE, 1.0);
        mlp.setNetworkParameters(10, 5, MLP.OUTPUT_ONE_OF_N);
        mcl.setMLP(mlp);
        mcl.setClassifierOutput(Classifier.OUT_CLASS_AND_CONFIDENCE);
        mcl.init();
        
        // Create the DataSet from the loaded data
        im.create(ms);
        
        // Standardize the data
        norm.trainTransformation(im);
        im = InstanceSetMemory.estimateAll(im, norm);
           
        // Train the MLP on the standardized data
        mcl.trainTransformation(im);
        
        // Write the training model of the MLP to a file.
        String mlpoutname = System.getProperty("java.io.tmpdir")+System.getProperty("file.separator")+"mlp_test.obj";
        ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream(mlpoutname));
        mcl.saveState(oout);
        oout.close();
        
        // **CRASH** AND **BURN**
        mcl = null;
        // ++++++++++++++++++++++
        
        // Make new MLPClassifier and Connect to Normalization
        mcl = new MLPClassifier();
        ObjectInputStream oin = new ObjectInputStream(new FileInputStream(mlpoutname));
        mcl.loadState(oin);
        oin.close();
        
        // Test the performance of the loaded MLP
        Validation           val;
        ValidationClassifier valclas;
        
        val = new Validation(im, mcl);
        val.create(Validation.SPLIT_TRAIN_TEST, new double[]{1.0});
        val.setSkipTrain(true);
        val.test();
        valclas = val.getValidationClassifier();
        
        // Check if the loaded MLP has the expected performance
        assertEquals(0.03, valclas.getClassificationError(), 0.25);
        
//        double [][]cmraw = valclas.getConfusionMatrix();
//        DoubleMatrix2D cm    = DoubleFactory2D.dense.make(cmraw);
//        System.out.println(cm);
//        System.out.println("Error : "+valclas.getClassificationError()+"\n");
    }
    
    public void testEstimatorPersistence() throws Exception
    {
        MemorySupplier    ms = new MemorySupplier();
        InstanceSetMemory im = new InstanceSetMemory();
        MLP              mlp = new MLP();
        MLPEstimator     mes = new MLPEstimator();
        
        ms.registerConsumer(0, mes, 0);
        ms.registerConsumer(0, im, 0);
        mes.registerSupplier(0, ms, 0);
        
        // Load a sine wave
        TestSets.loadSine(ms, 200, 1.37);
        
        // Create the Estimator for the Sine wave data
        mlp.setNeuronType(Neuron.ACTIVATION_SIGMOID_TANH, new double[]{1.0});
        mlp.setNetworkParameters(7, 0, MLP.OUTPUT_REGRESSION);
        mlp.setBatchParameters(InstanceBatch.BATCH_REORDER, InstanceBatch.GOAL_BALANCE_NONE, 1.0);
        mlp.setBackPropagationParameters(0.08, true, 0.9, 200);
        mes.setMLP(mlp);
        
        // Create the DataSet from the loaded data
        im.create(ms);
        
        // Train the MLP on the standardized data
        mes.trainTransformation(im);
        
        // Write the training model of the MLP to a file.
        String mlpoutname = System.getProperty("java.io.tmpdir")+System.getProperty("file.separator")+"mlp_test.obj";
        ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream(mlpoutname));
        mes.saveState(oout);
        oout.close();
        
        // **CRASH** AND **BURN**
        mes = null;
        // ++++++++++++++++++++++
        
        // Make new MLPEstimator
        mes = new MLPEstimator();
        ObjectInputStream oin = new ObjectInputStream(new FileInputStream(mlpoutname));
        mes.loadState(oin);
        oin.close();
        
        // Test the performance of the loaded MLP
        Validation          val;
        ValidationEstimator valest;
        double              err;
        
        val = new Validation(im, mes);
        val.create(Validation.SPLIT_TRAIN_TEST, new double[]{1.0});
        val.setSkipTrain(true);
        val.test();
        valest = val.getValidationEstimator();
        err = valest.getError(ValidationEstimator.ERROR_MEAN_SQUARED);
        assertTrue(err < 0.5);
    }
    
    // **********************************************************\
    // *                JUnit Setup and Teardown                *
    // **********************************************************/
    public MLPTest(String name)
    {
        super(name);
    }
    
    protected void setUp() throws Exception
    {
        super.setUp();
    }
    
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
}
