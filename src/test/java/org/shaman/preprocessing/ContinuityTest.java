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

import org.shaman.exceptions.ShamanException;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;
import org.shaman.learning.Validation;
import org.shaman.learning.ValidationEstimator;
import org.shaman.preprocessing.Continuity;

import junit.framework.TestCase;


public class ContinuityTest extends TestCase
{
    public static void testContinuity() throws ShamanException
    {
        MemorySupplier     ms   = new MemorySupplier();
        InstanceSetMemory  im   = new InstanceSetMemory();
        Continuity         cont = new Continuity();
        
        // Train the Normalization
        ms.registerConsumer(0, cont, 0);
        ms.registerConsumer(0, im, 0);
        cont.registerSupplier(0, ms, 0);
        
        TestSets.loadCancer(ms, false, true);
        
        cont.setParameters(0.4);
        cont.init();
        im.create(ms);
        
        Validation          val;
        ValidationEstimator valest;
        double              err;
        
        val = new Validation(im, cont);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{10.0});
        val.test();
        valest = val.getValidationEstimator();
        
        // Make sure self-estimation error is smaller than an empirical bound
        err = valest.getError(ValidationEstimator.ERROR_ROOT_MEAN_SQUARED);
        System.out.println("Average continuity error "+err);
        assertTrue(err < 4.5);
    }
    
    // **********************************************************\
    // *                JUnit Setup and Teardown                *
    // **********************************************************/
    public ContinuityTest(String name)
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
