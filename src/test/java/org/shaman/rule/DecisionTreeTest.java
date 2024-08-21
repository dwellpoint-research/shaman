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
package org.shaman.rule;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.shaman.exceptions.ShamanException;
import org.shaman.learning.Classifier;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;
import org.shaman.learning.Validation;
import org.shaman.learning.ValidationClassifier;
import org.shaman.preprocessing.Discretization;
import org.shaman.rule.DecisionTree;

import junit.framework.TestCase;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;


/**
 * Decision Tree Test
 */
public class DecisionTreeTest extends TestCase
{
    public static void testDecisionTreePrimitive() throws ShamanException
    {
        MemorySupplier ms = new MemorySupplier();
        DecisionTree dt = new DecisionTree();
        InstanceSetMemory im = new InstanceSetMemory();
        dt.registerSupplier(0, ms, 0);
        ms.registerConsumer(0, dt, 0);
        ms.registerConsumer(0, im, 0);
        
        // Validate performance of decision tree on primitive data
        TestSets.loadCancer(ms, false);
        dt.init();
        im.create(ms);
        
        DoubleMatrix2D cm;
        double         [][]cmraw;
        ValidationClassifier valclas;
        Validation val = new Validation(im, dt);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{10.0});
        val.test();
        valclas = val.getValidationClassifier();
        
        cmraw = valclas.getConfusionMatrix();
        cm    = DoubleFactory2D.dense.make(cmraw);
        System.out.println(cm);
        System.out.println("Primitive Error : "+valclas.getClassificationError()+"\n");
        assertEquals(0.05, valclas.getClassificationError(), 0.03);
    }
    
    public static void testPersistence() throws ShamanException, IOException
    {
        MemorySupplier ms = new MemorySupplier();
        DecisionTree dt = new DecisionTree();
        InstanceSetMemory im = new InstanceSetMemory();
        dt.registerSupplier(0, ms, 0);
        ms.registerConsumer(0, dt, 0);
        ms.registerConsumer(0, im, 0);
        
        // Validate performance of decision tree on primitive data
        TestSets.loadCancer(ms, false);
        dt.init();
        im.create(ms);
        
        dt.trainTransformation(im);
        
        // Write the training model of the MLP to a file.
        String outname = System.getProperty("java.io.tmpdir")+System.getProperty("file.separator")+"tree_test.obj";
        ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream(outname));
        dt.saveState(oout);
        oout.close();
        
        // **CRASH** AND **BURN**
        dt = null;
        // ++++++++++++++++++++++
        
        // Make new MLPClassifier and Connect to Normalization
        dt = new DecisionTree();
        ObjectInputStream oin = new ObjectInputStream(new FileInputStream(outname));
        dt.loadState(oin);
        oin.close();
        dt.registerSupplier(0, ms, 0);
        dt.init();
        
        // Test the performance of the loaded Tree
        Validation           val;
        ValidationClassifier valclas;
        
        val = new Validation(im, dt);
        val.create(Validation.SPLIT_TRAIN_TEST, new double[]{1.0});
        val.setSkipTrain(true);
        val.test();
        valclas = val.getValidationClassifier();
        
        double [][] cmraw = valclas.getConfusionMatrix();
        DoubleMatrix2D cm    = DoubleFactory2D.dense.make(cmraw);
        System.out.println(cm);
        System.out.println("Primitive Error : "+valclas.getClassificationError()+"\n");
        
        // Check if the loaded MLP has the expected performance
        assertEquals(0.05, valclas.getClassificationError(), 0.25);
    }
    
    public static void testDecisionTreeObject() throws ShamanException
    {
        MemorySupplier    ms = new MemorySupplier();
        DecisionTree      dt = new DecisionTree();
        InstanceSetMemory im = new InstanceSetMemory();
        dt.registerSupplier(0, ms, 0);
        ms.registerConsumer(0, dt, 0);
        ms.registerConsumer(0, im, 0);
        
        // Validate performance of decision tree on Object data
        TestSets.loadCancer(ms, false);
        dt.setClassifierOutput(Classifier.OUT_CLASS);
        dt.init();
        im.create(ms);
        
        DoubleMatrix2D cm;
        double         [][]cmraw;
        ValidationClassifier valclas;
        Validation val = new Validation(im, dt);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{10.0});
        val.test();
        valclas = val.getValidationClassifier();
        
        cmraw = valclas.getConfusionMatrix();
        cm    = DoubleFactory2D.dense.make(cmraw);
        System.out.println(cm);
        System.out.println("Error : "+valclas.getClassificationError()+"\n");
        assertEquals(0.05, valclas.getClassificationError(), 0.03);
    }
    
    public static void testDecisionTreeMultiClass() throws ShamanException
    {
        MemorySupplier     ms  = new MemorySupplier();
        InstanceSetMemory  im  = new InstanceSetMemory();
        Discretization   disc  = new Discretization();
        DecisionTree       dt  = new DecisionTree();
        InstanceSetMemory im2;
        
        // Connect the Flow Network
        ms.registerConsumer(0, disc, 0);
        disc.registerSupplier(0, ms, 0);
        disc.registerConsumer(0, dt, 0);
        disc.registerConsumer(0, im, 0);
        dt.registerSupplier(0, disc, 0);
        
        // Make and Train the Flows up until the DecisionTree
        TestSets.loadWine(ms);
        disc.setParameters(Discretization.TYPE_HISTOGRAM_EQUALIZATION, 10);
        disc.init();
        im.create(ms);
        disc.trainTransformation(im);
        
        // Make the instance set for the Tree training.
        im2 = InstanceSetMemory.estimateAll(im, disc);
        
        // Cross validate Tree.
        dt.init();
        
        System.out.println("Cross-Validation Decision Tree for Multi-Class problem");
        double       [][]cmraw;
        DoubleMatrix2D cm;
        Validation val = new Validation(im2, dt);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{10.0});
        val.test();
        cmraw = val.getValidationClassifier().getConfusionMatrix();
        cm = DoubleFactory2D.dense.make(cmraw);
        System.out.println("Confusion Matrix of Decision Tree "+cm);
        System.out.println("Classification Error "+val.getValidationClassifier().getClassificationError());
        System.out.println("Error classifying "+val.getValidationClassifier().getErrorOfClassification());
        assertEquals(0.11, val.getValidationClassifier().getClassificationError(), 0.1);
    }
    
    // **********************************************************\
    // *                     Test-Case Setup                    *
    // **********************************************************/
    protected void setUp() throws Exception
    {
    }
    
    protected void tearDown() throws Exception
    {
    }
    
    public DecisionTreeTest(String name)
    {
        super(name);
    }
}
