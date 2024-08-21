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

import org.shaman.exceptions.ShamanException;
import org.shaman.learning.Classifier;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;
import org.shaman.learning.Validation;
import org.shaman.learning.ValidationClassifier;
import org.shaman.rule.ExpressionClassifier;

import junit.framework.TestCase;


/**
 * Expression based Classifier
 */
public class ExpressionClassifierTest extends TestCase
{
    public void testExpressionClassifier() throws ShamanException
    {
        MemorySupplier       ms = new MemorySupplier();
        ExpressionClassifier ec = new ExpressionClassifier();
        InstanceSetMemory    im = new InstanceSetMemory();
        
        // Train a Classifier
        ec.registerSupplier(0, ms, 0);
        ms.registerConsumer(0, ec, 0);
        ms.registerConsumer(0, im, 0);
        
        TestSets.loadCancer(ms, false, true);
        
        ec.setExpression("((Field2 + Field3 + Field4 + Field5 + Field6 + Field7 + Field8 + Field9) / 8) >= 3.70");
        ec.setClassifierOutput(Classifier.OUT_CLASS_AND_CONFIDENCE);
        ec.init();
        
        im.create(ms);
        
        // Train using 10-fold Cross Validatin
        Validation           val;
        ValidationClassifier valclas;
        
        val = new Validation(im, ec);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{10.0});
        val.test();
        valclas = val.getValidationClassifier();
        
        // Check if the Classification Error is around the expected value...
        assertEquals(0.05, valclas.getClassificationError(), 0.1);
        //System.err.println(valclas.getClassificationError());
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
    
    public ExpressionClassifierTest(String name)
    {
        super(name);
    }
}
