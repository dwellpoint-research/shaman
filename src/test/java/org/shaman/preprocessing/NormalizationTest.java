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
import org.shaman.preprocessing.Normalization;

import junit.framework.TestCase;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;


public class NormalizationTest extends TestCase
{
    public void testInstanceWeighting() throws ShamanException
    {
        DataModelDouble   dmdo;
        VectorSource      vecsrc;
        Normalization     norm;
        InstanceSetMemory im;
        
        // Setup a Standardization for 1 continuous number.
        dmdo = (DataModelDouble)TestUtils.makeNumberDataModel(1, true);
        vecsrc = new VectorSource();
        norm   = new Normalization();
        vecsrc.registerConsumer(0, norm, 0);
        norm.registerSupplier(0, vecsrc, 0);
        vecsrc.setDataModel(dmdo);
        norm.setType(Normalization.TYPE_STANDARDIZE);
        vecsrc.init();
        norm.init();
        
        // Test Standardization on a few data-sets
        im = new InstanceSetMemory();
        im.setDataModel(dmdo);
        
        DoubleMatrix1D []ins = new DoubleMatrix1D[2];
        double         []wei = new double[2];
        double         []mean, stdev;
        
        // Unweighted dataset. Just check correct output values.
        ins[0] = DoubleFactory1D.dense.make(new double[]{1.0}); wei[0] = 1.0;
        ins[1] = DoubleFactory1D.dense.make(new double[]{2.0}); wei[1] = 1.0;
        im.setInstances(ins);
        im.setWeights(wei);
        norm.trainTransformation(im);
        
        // Mean   = (1.0 + 2.0) / 2
        // Stddev = 
        //     Sqrt((1.0 - 1.5) * (1.0 - 1.5) + (2.0 - 1.5) * (2.0 - 1.5)) / 2)
        //     Sqrt(                     0.25 +                      0.25) / 2)
        //     Sqrt(0.25) = 0.5
        mean  = norm.getMean();
        stdev = norm.getStdDev();
        assertEquals(1.5,  mean[0], 0);
        assertEquals(0.5, stdev[0], 0);
        
        // Weighted DataSet. Check if weighting works as expected.
        double mwei, swei;
        
        // 2 instances with second one carrying a double weight.
        ins[0] = DoubleFactory1D.dense.make(new double[]{1.0}); wei[0] = 1.0;
        ins[1] = DoubleFactory1D.dense.make(new double[]{2.0}); wei[1] = 2.0;
        im.setInstances(ins);
        im.setWeights(wei);
        norm.trainTransformation(im);
        mwei = norm.getMean()[0];
        swei = norm.getStdDev()[0];
        
        // is the same as 3 instances with single weight.
        ins = new DoubleMatrix1D[3];
        wei = new double[3];
        ins[0] = DoubleFactory1D.dense.make(new double[]{1.0}); wei[0] = 1.0;
        ins[1] = DoubleFactory1D.dense.make(new double[]{2.0}); wei[1] = 1.0;
        ins[2] = DoubleFactory1D.dense.make(new double[]{2.0}); wei[2] = 1.0;
        im.setInstances(ins);
        im.setWeights(wei);
        norm.trainTransformation(im);
        mean  = norm.getMean();
        stdev = norm.getStdDev();
        
        assertEquals( mean[0], mwei, 0);
        assertEquals(stdev[0], swei, 0);
    }

    public void testNormalization() throws ShamanException
    {
        MemorySupplier     ms = new MemorySupplier();
        InstanceSetMemory  im = new InstanceSetMemory();
        Normalization    norm = new Normalization();
        
        // Train the Normalization on the Wine classification data-set
        ms.registerConsumer(0, norm, 0);
        ms.registerConsumer(0, im, 0);
        norm.registerSupplier(0, ms, 0);
        TestSets.loadWine(ms);
        
        norm.setType(Normalization.TYPE_NORMALIZE);
        norm.init();
        
        im.create(ms);
        norm.trainTransformation(im);
        
        double []min, max;
        min = norm.getMin();
        max = norm.getMax();
        assertEquals(11.03, min[0],  0.0001); assertEquals(  14.83, max[0],  0.0001);   
        assertEquals( 0.74, min[1],  0.0001); assertEquals(   5.80, max[1],  0.0001);
        assertEquals( 1.36, min[2],  0.0001); assertEquals(   3.23, max[2],  0.0001);
        assertEquals(10.60, min[3],  0.0001); assertEquals(  30.00, max[3],  0.0001);
        assertEquals(70.00, min[4],  0.0001); assertEquals( 162.00, max[4],  0.0001);
        assertEquals( 0.98, min[5],  0.0001); assertEquals(   3.88, max[5],  0.0001);
        assertEquals( 0.34, min[6],  0.0001); assertEquals(   5.08, max[6],  0.0001);
        assertEquals( 0.13, min[7],  0.0001); assertEquals(   0.66, max[7],  0.0001);
        assertEquals( 0.41, min[8],  0.0001); assertEquals(   3.58, max[8],  0.0001);
        assertEquals( 1.28, min[9],  0.0001); assertEquals(  13.00, max[9],  0.0001);
        assertEquals( 0.48, min[10], 0.0001); assertEquals(   1.71, max[10], 0.0001);
        assertEquals( 1.27, min[11], 0.0001); assertEquals(   4.00, max[11], 0.0001);
        assertEquals(278.0, min[12], 0.0001); assertEquals(1680.00, max[12], 0.0001);
    }
    
    public void testStandardization() throws ShamanException
    {
        MemorySupplier     ms = new MemorySupplier();
        InstanceSetMemory  im = new InstanceSetMemory();
        Normalization    norm = new Normalization();
        
        // Train the Standardization on the Wine classification data-set
        ms.registerConsumer(0, norm, 0);
        ms.registerConsumer(0, im, 0);
        norm.registerSupplier(0, ms, 0);
        TestSets.loadWine(ms);
        
        norm.setType(Normalization.TYPE_STANDARDIZE);
        norm.init();
        
        im.create(ms);
        norm.trainTransformation(im);
        
        double []mean, std;
        mean = norm.getMean();
        std  = norm.getStdDev();
        assertEquals( 12.99, mean[0],  0.01); assertEquals(  0.80, std[0],  0.01);
        assertEquals(  2.32, mean[1],  0.01); assertEquals(  1.10, std[1],  0.01);
        assertEquals(  2.36, mean[2],  0.01); assertEquals(  0.27, std[2],  0.01);
        assertEquals( 19.46, mean[3],  0.01); assertEquals(  3.31, std[3],  0.01);
        assertEquals( 99.76, mean[4],  0.01); assertEquals( 14.27, std[4],  0.01);
        assertEquals(  2.29, mean[5],  0.01); assertEquals(  0.62, std[5],  0.01);
        assertEquals(  2.03, mean[6],  0.01); assertEquals(  0.99, std[6],  0.01);
        assertEquals(  0.36, mean[7],  0.01); assertEquals(  0.12, std[7],  0.01);
        assertEquals(  1.59, mean[8],  0.01); assertEquals(  0.57, std[8],  0.01);
        assertEquals(  5.03, mean[9],  0.01); assertEquals(  2.29, std[9],  0.01);
        assertEquals(  0.95, mean[10], 0.01); assertEquals(  0.22, std[10], 0.01);
        assertEquals(  2.61, mean[11], 0.01); assertEquals(  0.70, std[11], 0.01);
        assertEquals(747.94, mean[12], 0.01); assertEquals(314.59, std[12], 0.01);
    }
    
    public void testEstimator() throws ShamanException
    {
        MemorySupplier     ms = new MemorySupplier();
        InstanceSetMemory  im = new InstanceSetMemory();
        Normalization    norm = new Normalization();
        
        // Train the Standardization on the Wine classification data-set
        ms.registerConsumer(0, norm, 0);
        ms.registerConsumer(0, im, 0);
        norm.registerSupplier(0, ms, 0);
        TestSets.loadWine(ms);
        
        norm.setType(Normalization.TYPE_STANDARDIZE);
        norm.init();
        
        im.create(ms);
        norm.trainTransformation(im);
        
        // Transform the wine-dataset through to Standardized format.
        im = InstanceSetMemory.estimateAll(im, norm);
        
        // Assert if the transformed data-set is standardized.
        assertStandardized(im);
    }
    
    public void testPersistence() throws ShamanException, IOException
    {
        MemorySupplier     ms = new MemorySupplier();
        InstanceSetMemory  im = new InstanceSetMemory();
        Normalization    norm = new Normalization();
        
        // Train the Standardization on the Wine classification data-set
        ms.registerConsumer(0, norm, 0);
        ms.registerConsumer(0, im, 0);
        norm.registerSupplier(0, ms, 0);
        TestSets.loadWine(ms);
        
        norm.setType(Normalization.TYPE_STANDARDIZE);
        norm.init();
        
        im.create(ms);
        norm.trainTransformation(im);
        
        // Save the trained Descretization
        String outname = System.getProperty("java.io.tmpdir")+System.getProperty("file.separator")+"norm_test.obj";
        ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream(outname));
        norm.saveState(oout);
        oout.close();
        
        // **CRASH** AND **BURN**
        norm = null;
        // ++++++++++++++++++++++
        
        // Make new Normalization, load persisted state, connect to data-set and initialize
        norm = new Normalization();
        ObjectInputStream oin = new ObjectInputStream(new FileInputStream(outname));
        norm.loadState(oin);
        oin.close();
        norm.registerSupplier(0, ms, 0);
        norm.init();
        
        // Transform the wine-dataset through to Standardized format.
        im = InstanceSetMemory.estimateAll(im, norm);
        
        // Assert if the transformed data-set is standardized.
        assertStandardized(im);
    }
    
    private void assertStandardized(InstanceSetMemory im) throws ShamanException
    {
        // Check if the standardized data-set is really 0 mean, 1 stddev.
        int      i,j;
        double []sbuf = new double[im.getInstance(0).size()];
        DoubleMatrix1D din;
        
        for (i=0; i<im.getNumberOfInstances(); i++)
        {
            din = im.getInstance(i);
            for (j=0; j<din.size(); j++) sbuf[j] += din.getQuick(j) * din.getQuick(j);
        }
        for (j=0; j<sbuf.length; j++)
        { 
            sbuf[j] /= im.getNumberOfInstances();
            sbuf[j]  = Math.sqrt(sbuf[j]);
            assertEquals(1.0, sbuf[j], 0.0001);
        }
    }
    
    // **********************************************************\
    // *                JUnit Setup and Teardown                *
    // **********************************************************/
    public NormalizationTest(String name)
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
