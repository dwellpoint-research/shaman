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

import org.shaman.learning.BatchPresenter;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;
import org.shaman.learning.Validation;
import org.shaman.learning.ValidationEstimator;
import org.shaman.neural.KMER;
import org.shaman.neural.Lattice;

import junit.framework.TestCase;


/**
 * Kernel-Based Maximum Entropy Learning Network Test
 */
public class KMERTest extends TestCase
{
    // **********************************************************\
    // *                 Test KMER Estimation                   *
    // **********************************************************/
    public void testKMER() throws Exception
    {
        // Make the KMER network
        MemorySupplier    ms = new MemorySupplier();
        KMER            kmer = new KMER();
        InstanceSetMemory im = new InstanceSetMemory();
        
        ms.registerConsumer(0, im, 0);
        ms.registerConsumer(0, kmer, 0);
        kmer.registerSupplier(0, ms, 0);
        
        // Load a 2D test set from an image.
        TestSets.loadImage(ms, "./src/main/resources/data/data4s.gif");
        im.create(ms);
        
        // Configure the KMER net to cover the train-data image
        kmer.setBatchParameters(BatchPresenter.BATCH_REORDER, BatchPresenter.GOAL_BALANCE_NONE, 0.02);
        kmer.setLatticeParameters(12, 12, 0, Lattice.NEIGHBORHOOD_GAUSSIAN, false);
        kmer.setKMERParameters(0.001, 1.0, 400);
        kmer.init();
        kmer.setTrainSet(im);
        
        // Train using Cross Validation
        Validation          val;
        ValidationEstimator valest;
        double              err;
        
        val = new Validation(im, kmer);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{3.0});
        val.test();
        valest = val.getValidationEstimator();
        
        // Make sure self-estimation error is smaller than an empirical bound
        err = valest.getError(ValidationEstimator.ERROR_MEAN_SQUARED);
        assertTrue(err < 0.01);
    }
    
    public void testPersistence() throws Exception
    {
        // Make the KMER network
        MemorySupplier    ms = new MemorySupplier();
        KMER            kmer = new KMER();
        InstanceSetMemory im = new InstanceSetMemory();
        
        ms.registerConsumer(0, im, 0);
        ms.registerConsumer(0, kmer, 0);
        kmer.registerSupplier(0, ms, 0);
        
        // Load a 2D test set from an image.
        TestSets.loadImage(ms, "./src/main/resources/data/data4s.gif");
        im.create(ms);
        
        // Configure the KMER net to cover the train-data image
        kmer.setBatchParameters(BatchPresenter.BATCH_REORDER, BatchPresenter.GOAL_BALANCE_NONE, 0.02);
        kmer.setLatticeParameters(12, 12, 0, Lattice.NEIGHBORHOOD_GAUSSIAN, false);
        kmer.setKMERParameters(0.001, 1.0, 400);
        kmer.init();
        kmer.trainTransformation(im);
        
        // Save the trained KMER net
        String outname = System.getProperty("java.io.tmpdir")+System.getProperty("file.separator")+"kmer_test.obj";
        ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream(outname));
        kmer.saveState(oout);
        oout.close();
        
        // **CRASH** AND **BURN**
        kmer = null;
        // ++++++++++++++++++++++
        
        // Make new KMER net, load persisted state, connect to data-set and initialize
        kmer = new KMER();
        ObjectInputStream oin = new ObjectInputStream(new FileInputStream(outname));
        kmer.loadState(oin);
        oin.close();
        kmer.registerSupplier(0, ms, 0);
        kmer.init();
        
        // Validate performance of the trained net
        Validation          val;
        ValidationEstimator valest;
        double              err;
        
        val = new Validation(im, kmer);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{3.0});
        val.setSkipTrain(true);
        val.test();
        valest = val.getValidationEstimator();
        
        // Make sure self-estimation error is smaller than an empirical bound
        err = valest.getError(ValidationEstimator.ERROR_MEAN_SQUARED);
        assertTrue(err < 0.01);
    }
    
    // **********************************************************\
    // *                JUnit Setup and Teardown                *
    // **********************************************************/
    public KMERTest(String name)
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
