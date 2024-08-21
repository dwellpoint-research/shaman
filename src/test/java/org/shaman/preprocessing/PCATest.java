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
package org.shaman.preprocessing;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.shaman.exceptions.ShamanException;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;
import org.shaman.learning.Validation;
import org.shaman.learning.ValidationEstimator;
import org.shaman.preprocessing.PCA;

import junit.framework.TestCase;


public class PCATest extends TestCase
{
    public void testPCA() throws ShamanException
    {
        MemorySupplier     ms  = new MemorySupplier();
        InstanceSetMemory  im  = new InstanceSetMemory();
        PCA               pca  = new PCA();
        
        // Train the Normalization
        ms.registerConsumer(0, pca, 0);
        ms.registerConsumer(0, im, 0);
        pca.registerSupplier(0, ms, 0);
        
        TestSets.loadWine(ms);
        
        pca.setType(PCA.TYPE_LINEAR);
        pca.setNumberOfPC(12);
        pca.init();
        
        im.create(ms);
        
        Validation          val;
        ValidationEstimator valest;
        double              err;
        
        val = new Validation(im, pca);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{10.0});
        val.test();
        valest = val.getValidationEstimator();
        
        // Make sure self-estimation error is smaller than an empirical bound
        err = valest.getError(ValidationEstimator.ERROR_ROOT_MEAN_SQUARED);
        System.out.println("Average discretization error "+err);
        assertTrue(err < 12);
    }
    
    public void testPersistence() throws Exception
    {
        MemorySupplier     ms  = new MemorySupplier();
        InstanceSetMemory  im  = new InstanceSetMemory();
        PCA               pca  = new PCA();
        
        // Train the Normalization
        ms.registerConsumer(0, pca, 0);
        ms.registerConsumer(0, im, 0);
        pca.registerSupplier(0, ms, 0);
        
        TestSets.loadWine(ms);
        
        pca.setType(PCA.TYPE_LINEAR);
        pca.setNumberOfPC(12);
        pca.init();
        
        im.create(ms);
        
        // Train the PCA on the data-set
        pca.trainTransformation(im);
        
        // Save the trained PCA
        String outname = System.getProperty("java.io.tmpdir")+System.getProperty("file.separator")+"pca_test.obj";
        ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream(outname));
        pca.saveState(oout);
        oout.close();
        
        // **CRASH** AND **BURN**
        pca = null;
        // ++++++++++++++++++++++
        
        // Make new PCA, load persisted state, connect to data-set and initialize
        pca = new PCA();
        ObjectInputStream oin = new ObjectInputStream(new FileInputStream(outname));
        pca.loadState(oin);
        oin.close();
        pca.registerSupplier(0, ms, 0);
        pca.init();
        
         // Test the performance of the loaded PCA
        Validation          val;
        ValidationEstimator valest;
        double              err;
        
        val = new Validation(im, pca);
        val.create(Validation.SPLIT_TRAIN_TEST, new double[]{1.0});
        val.setSkipTrain(true);
        val.test();
        valest = val.getValidationEstimator();
        err = valest.getError(ValidationEstimator.ERROR_MEAN_SQUARED);
        assertTrue(err < 12);
    }
    
    // **********************************************************\
    // *                JUnit Setup and Teardown                *
    // **********************************************************/
    public PCATest(String name)
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
