/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2006 Shaman Research                   *
\*********************************************************/
package org.shaman.clustering;

import org.shaman.clustering.KMeans;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;
import org.shaman.learning.Validation;
import org.shaman.learning.ValidationClusterer;

import junit.framework.TestCase;


/**
 * K-Means Clustering Test
 */
public class KMeansTest extends TestCase
{
    // **********************************************************\
    // *                    Test Clustering                     *
    // **********************************************************/
    public void testKMeans() throws Exception
    {
        MemorySupplier    ms = new MemorySupplier();
        InstanceSetMemory im = new InstanceSetMemory();
        KMeans            km = new KMeans();
        
        ms.registerConsumer(0, km, 0);
        km.registerSupplier(0, ms, 0);
        
        // Load Iris Dataset
        TestSets.loadIris(ms, 2);
        
        // Standardize the data.
        km.setK(3);
        km.init();
        
        // Create the DataSet from the loaded data
        im.create(ms);
        
        // Train using Cross Validation
        Validation          val;
        ValidationClusterer valcl;
        
        val = new Validation(im, km);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{3.0});
        val.test();
        valcl = val.getValidationClusterer();
        
        double []clmem = valcl.getClusterMembership();
        System.err.println(clmem[0]+" "+clmem[1]+" "+clmem[2]);
    }
    
    // **********************************************************\
    // *                JUnit Setup and Teardown                *
    // **********************************************************/
    public KMeansTest(String name)
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
