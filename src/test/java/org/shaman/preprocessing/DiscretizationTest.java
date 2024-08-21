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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.shaman.TestUtils;
import org.shaman.dataflow.VectorSource;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.exceptions.ShamanException;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;
import org.shaman.learning.Validation;
import org.shaman.learning.ValidationEstimator;
import org.shaman.preprocessing.Discretization;

import junit.framework.TestCase;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;


public class DiscretizationTest extends TestCase
{
    public void testInstanceWeighting() throws ShamanException
    {
        DataModelDouble   dmdo;
        VectorSource      vecsrc;
        Discretization    disc;
        InstanceSetMemory im;
        
        // Setup a Discretization for 1 continuous number.
        dmdo = (DataModelDouble)TestUtils.makeNumberDataModel(1, true);
        vecsrc = new VectorSource();
        disc   = new Discretization();
        vecsrc.registerConsumer(0, disc, 0);
        disc.registerSupplier(0, vecsrc, 0);
        vecsrc.setDataModel(dmdo);
        disc.setParameters(Discretization.TYPE_HISTOGRAM_EQUALIZATION, 4);
        vecsrc.init();
        disc.init();
        
        // Test Discretization on a few data-sets
        im = new InstanceSetMemory();
        im.setDataModel(dmdo);
        
        DoubleMatrix1D []ins = new DoubleMatrix1D[4];
        double         []wei = new double[4];
        
        // Unweighted dataset. Just check correct output values.
        ins[0] = DoubleFactory1D.dense.make(new double[]{1.0}); wei[0] = 1.0;
        ins[1] = DoubleFactory1D.dense.make(new double[]{2.0}); wei[1] = 1.0;
        ins[2] = DoubleFactory1D.dense.make(new double[]{3.0}); wei[2] = 1.0;
        ins[3] = DoubleFactory1D.dense.make(new double[]{4.0}); wei[3] = 1.0;
        im.setInstances(ins);
        im.setWeights(wei);
        disc.trainTransformation(im);
        
        double []min, max, mid;
        min = disc.getIntervalMin()[0];
        max = disc.getIntervalMax()[0];
        mid = disc.getIntervalMid()[0];
        assertEquals(Double.NEGATIVE_INFINITY, min[0], 0); assertEquals(1.5, mid[0], 0.0); assertEquals(2.0, max[0], 0.0); 
        assertEquals(                     2.0, min[1], 0); assertEquals(2.5, mid[1], 0.0); assertEquals(3.0, max[1], 0.0); 
        assertEquals(                     3.0, min[2], 0); assertEquals(3.5, mid[2], 0.0); assertEquals(4.0, max[2], 0.0); 
        assertEquals(                     4.0, min[3], 0); assertEquals(4.0, mid[3], 0.0); assertEquals(Double.POSITIVE_INFINITY, max[3], 0.0); 

        // Add some half-weighted instances. Should produce the same output as the test above.
        ins = new DoubleMatrix1D[6];
        wei = new double[6];
        ins[0] = DoubleFactory1D.dense.make(new double[]{1.0}); wei[0] = 1.0;
        ins[1] = DoubleFactory1D.dense.make(new double[]{2.0}); wei[1] = 0.5;
        ins[2] = DoubleFactory1D.dense.make(new double[]{2.0}); wei[2] = 0.5;
        ins[3] = DoubleFactory1D.dense.make(new double[]{3.0}); wei[3] = 0.5;
        ins[4] = DoubleFactory1D.dense.make(new double[]{3.0}); wei[4] = 0.5;
        ins[5] = DoubleFactory1D.dense.make(new double[]{4.0}); wei[5] = 1.0;
        im.setInstances(ins);
        im.setWeights(wei);
        disc.trainTransformation(im);
        
        min = disc.getIntervalMin()[0];
        max = disc.getIntervalMax()[0];
        mid = disc.getIntervalMid()[0];
        assertEquals(Double.NEGATIVE_INFINITY, min[0], 0); assertEquals(1.5, mid[0], 0.0); assertEquals(2.0, max[0], 0.0); 
        assertEquals(                     2.0, min[1], 0); assertEquals(2.5, mid[1], 0.0); assertEquals(3.0, max[1], 0.0); 
        assertEquals(                     3.0, min[2], 0); assertEquals(3.5, mid[2], 0.0); assertEquals(4.0, max[2], 0.0); 
        assertEquals(                     4.0, min[3], 0); assertEquals(4.0, mid[3], 0.0); assertEquals(Double.POSITIVE_INFINITY, max[3], 0.0);   
    }
    
    public void testEqualIntervals() throws ShamanException
    {
        MemorySupplier     ms  = new MemorySupplier();
        InstanceSetMemory  im  = new InstanceSetMemory();
        Discretization   disc  = new Discretization();
        
        // Train the Discretization on the Wine classification data-set
        ms.registerConsumer(0, disc, 0);
        ms.registerConsumer(0, im, 0);
        disc.registerSupplier(0, ms, 0);
        TestSets.loadWine(ms);
        
        // Train for 10 equally spaced intervals
        disc.setParameters(Discretization.TYPE_EQUAL_INTERVALS, 10);
        disc.init();
        im.create(ms);
        disc.trainTransformation(im);
        
        
        double []intmin, intmax;
        
        // Check output first continuous attribute.
        intmin = disc.getIntervalMin()[0];
        intmax = disc.getIntervalMax()[0];
        
        // Interval  0 from continous range minimum bound 
        //       ... n equally spaced over data-set values
        //          10 to continuous range maximum bound
        assertEquals(0,     intmin[0], 0.0001); assertEquals(   11.41, intmax[0], 0.0001);
        assertEquals(11.41, intmin[1], 0.0001); assertEquals(   11.79, intmax[1], 0.0001);
        assertEquals(11.79, intmin[2], 0.0001); assertEquals(   12.17, intmax[2], 0.0001); 
        assertEquals(12.17, intmin[3], 0.0001); assertEquals(   12.55, intmax[3], 0.0001); 
        assertEquals(12.55, intmin[4], 0.0001); assertEquals(   12.93, intmax[4], 0.0001); 
        assertEquals(12.93, intmin[5], 0.0001); assertEquals(   13.31, intmax[5], 0.0001); 
        assertEquals(13.31, intmin[6], 0.0001); assertEquals(   13.69, intmax[6], 0.0001); 
        assertEquals(13.69, intmin[7], 0.0001); assertEquals(   14.07, intmax[7], 0.0001); 
        assertEquals(14.07, intmin[8], 0.0001); assertEquals(   14.45, intmax[8], 0.0001); 
        assertEquals(14.45, intmin[9], 0.0001); assertEquals(10000.00, intmax[9], 0.0001); 
    }
    
    public void testHistogramEqualization() throws ShamanException
    {
        MemorySupplier     ms  = new MemorySupplier();
        InstanceSetMemory  im  = new InstanceSetMemory();
        Discretization   disc  = new Discretization();
        
        // Train the Discretization on the Wine classification data-set
        ms.registerConsumer(0, disc, 0);
        ms.registerConsumer(0, im, 0);
        disc.registerSupplier(0, ms, 0);
        TestSets.loadWine(ms);
        
        // Train for 10 intervals containing an equal amount of data-points
        disc.setParameters(Discretization.TYPE_HISTOGRAM_EQUALIZATION, 10);
        disc.init();
        im.create(ms);
        disc.trainTransformation(im);
        
        double []intmin, intmax;
        
        // Check output first continuous attribute.
        intmin = disc.getIntervalMin()[0];
        intmax = disc.getIntervalMax()[0];
        
        // Interval  0 from continous range minimum bound 
        //       ... n containing some number of points
        //          10 to continuous range maximum bound
        assertEquals(0,     intmin[0], 0.0001); assertEquals(   11.87, intmax[0], 0.0001);
        assertEquals(11.87, intmin[1], 0.0001); assertEquals(   12.25, intmax[1], 0.0001);
        assertEquals(12.25, intmin[2], 0.0001); assertEquals(   12.42, intmax[2], 0.0001); 
        assertEquals(12.42, intmin[3], 0.0001); assertEquals(   12.72, intmax[3], 0.0001); 
        assertEquals(12.72, intmin[4], 0.0001); assertEquals(   13.05, intmax[4], 0.0001); 
        assertEquals(13.05, intmin[5], 0.0001); assertEquals(   13.28, intmax[5], 0.0001); 
        assertEquals(13.28, intmin[6], 0.0001); assertEquals(   13.51, intmax[6], 0.0001); 
        assertEquals(13.51, intmin[7], 0.0001); assertEquals(   13.75, intmax[7], 0.0001); 
        assertEquals(13.75, intmin[8], 0.0001); assertEquals(   14.10, intmax[8], 0.0001);
        assertEquals(14.10, intmin[9], 0.0001); assertEquals(10000.00, intmax[9], 0.0001); 
    }
    
    public void testSelfEstimation() throws ShamanException
    {
        MemorySupplier     ms  = new MemorySupplier();
        InstanceSetMemory  im  = new InstanceSetMemory();
        Discretization   disc  = new Discretization();
        
        // Train the Normalization
        ms.registerConsumer(0, disc, 0);
        ms.registerConsumer(0, im, 0);
        disc.registerSupplier(0, ms, 0);
        
        TestSets.loadWine(ms);
        
        disc.setParameters(Discretization.TYPE_HISTOGRAM_EQUALIZATION, 10);
        disc.init();
        
        im.create(ms);
        
        Validation          val;
        ValidationEstimator valest;
        double              err;
        
        val = new Validation(im, disc);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{10.0});
        val.test();
        valest = val.getValidationEstimator();
        
        
        // Make sure self-estimation error is smaller than an empirical bound
        err = valest.getError(ValidationEstimator.ERROR_ROOT_MEAN_SQUARED);
        System.out.println("Average discretization error "+err);
        assertTrue(err < 70);
    }
    
    public void testPersistence() throws ShamanException, IOException
    {
        MemorySupplier     ms  = new MemorySupplier();
        InstanceSetMemory  im  = new InstanceSetMemory();
        Discretization   disc  = new Discretization();
        
        // Train the Normalization
        ms.registerConsumer(0, disc, 0);
        ms.registerConsumer(0, im, 0);
        disc.registerSupplier(0, ms, 0);
        
        TestSets.loadWine(ms);
        
        disc.setParameters(Discretization.TYPE_HISTOGRAM_EQUALIZATION, 10);
        disc.init();
        
        im.create(ms);
        
        disc.trainTransformation(im);
        
        // Save the trained Descretization
        String outname = System.getProperty("java.io.tmpdir")+System.getProperty("file.separator")+"disc_test.obj";
        ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream(outname));
        disc.saveState(oout);
        oout.close();
        
        // **CRASH** AND **BURN**
        disc = null;
        // ++++++++++++++++++++++
        
        // Make new Discretization, load persisted state, connect to data-set and initialize
        disc = new Discretization();
        ObjectInputStream oin = new ObjectInputStream(new FileInputStream(outname));
        disc.loadState(oin);
        oin.close();
        disc.registerSupplier(0, ms, 0);
        disc.init();
        
        Validation          val;
        ValidationEstimator valest;
        double              err;
        
        val = new Validation(im, disc);
        val.create(Validation.SPLIT_TRAIN_TEST, new double[]{1.0});
        val.setSkipTrain(true);
        val.test();
        valest = val.getValidationEstimator();
        
        // Make sure self-estimation error is smaller than an empirical bound
        err = valest.getError(ValidationEstimator.ERROR_ROOT_MEAN_SQUARED);
        System.out.println("Average discretization error "+err);
        assertTrue(err < 70);
    }
    
    // **********************************************************\
    // *                JUnit Setup and Teardown                *
    // **********************************************************/
    public DiscretizationTest(String name)
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
