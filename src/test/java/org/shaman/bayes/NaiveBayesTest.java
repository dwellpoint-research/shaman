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
package org.shaman.bayes;

import org.shaman.bayes.NaiveBayes;
import org.shaman.exceptions.ShamanException;
import org.shaman.learning.Classifier;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;
import org.shaman.learning.Validation;
import org.shaman.learning.ValidationClassifier;

import junit.framework.TestCase;


/**
 * <h2>Naive Bayes Learner Test Case</h2>
 */
public class NaiveBayesTest extends TestCase
{
    // **********************************************************\
    // *                    Naive Bayes Test                    *
    // **********************************************************/
    public void testCancerObject() throws ShamanException
    {
        MemorySupplier    ms = new MemorySupplier();
        NaiveBayes        nb = new NaiveBayes();
        InstanceSetMemory im = new InstanceSetMemory();
        nb.registerSupplier(0, ms, 0);
        ms.registerConsumer(0, nb, 0);
        ms.registerConsumer(0, im, 0);
        
        // Load Wisconsin Breast Cancer data-set
        TestSets.loadCancer(ms, false, false);
        
        nb.setClassifierOutput(Classifier.OUT_CLASS);
        
        nb.init();
        im.create(ms);
        
        // Train using 10-fold Cross Validatin
        Validation           val;
        ValidationClassifier valclas;
        
        val = new Validation(im, nb);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{10.0});
        val.test();
        valclas = val.getValidationClassifier();
        
        // Check if the Classification Error is around the expected value...
        assertEquals(0.03, valclas.getClassificationError(), 0.1);
    }
    
    public void testCancerDouble() throws ShamanException
    {
        MemorySupplier    ms = new MemorySupplier();
        NaiveBayes        nb = new NaiveBayes();
        InstanceSetMemory im = new InstanceSetMemory();
        nb.registerSupplier(0, ms, 0);
        ms.registerConsumer(0, nb, 0);
        ms.registerConsumer(0, im, 0);
        
        // Load Wisconsin Breast Cancer data-set
        TestSets.loadCancer(ms, false, true);
        
        nb.setClassifierOutput(Classifier.OUT_CLASS);
        
        nb.init();
        im.create(ms);
        
        // Train using 10-fold Cross Validatin
        Validation           val;
        ValidationClassifier valclas;
        
        val = new Validation(im, nb);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{10.0});
        val.test();
        valclas = val.getValidationClassifier();
        
        // Check if the Classification Error is around the expected value...
        assertEquals(0.03, valclas.getClassificationError(), 0.1);
    }

    // **********************************************************\
    // *                JUnit Setup and Teardown                *
    // **********************************************************/
    public NaiveBayesTest(String name)
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
