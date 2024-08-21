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
package org.shaman.cbr;

import org.shaman.cbr.CBR;
import org.shaman.exceptions.ShamanException;
import org.shaman.learning.Classifier;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;
import org.shaman.learning.Validation;
import org.shaman.learning.ValidationClassifier;

import junit.framework.TestCase;


/**
 * <h2>Case Based Reasoning Test Case</h2>
 */
public class CBRTest extends TestCase
{
   // **********************************************************\
   // *               Case Based Reasoning Test                *
   // **********************************************************/
   public void testCancerDouble() throws ShamanException
   {
       cancerCore(true);
   }
   
   public void testCancerObject() throws ShamanException
   {
       cancerCore(false);
   }
    
   private void cancerCore(boolean prim) throws ShamanException
   {
       MemorySupplier    ms = new MemorySupplier();
       CBR              cbr = new CBR();
       InstanceSetMemory im = new InstanceSetMemory();
       cbr.registerSupplier(0, ms, 0);
       ms.registerConsumer(0,cbr, 0);
       ms.registerConsumer(0, im, 0);

       // Load Wisconsin Breast Cancer data-set
       TestSets.loadCancer(ms, false, prim);
       
       // Set Parameters
       cbr.setClassifierOutput(Classifier.OUT_CLASS);
       cbr.setKNearest(5);
       cbr.init();
       
       // Initialize
       cbr.init();
       im.create(ms);

       // Train using 3-fold Cross Validatin
       Validation           val;
       ValidationClassifier valclas;
       
       val = new Validation(im, cbr);
       val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{3.0});
       val.test();
       valclas = val.getValidationClassifier();
       
       // Check if the Classification Error is around the expected value...
       assertEquals(0.03, valclas.getClassificationError(), 0.1);
   }
   
	// **********************************************************\
    // *                JUnit Setup and Teardown                *
    // **********************************************************/
	public CBRTest(String name)
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
