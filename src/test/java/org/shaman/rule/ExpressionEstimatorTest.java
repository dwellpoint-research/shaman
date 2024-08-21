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

import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.exceptions.ShamanException;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;
import org.shaman.learning.Validation;
import org.shaman.learning.ValidationEstimator;
import org.shaman.rule.ExpressionEstimator;

import junit.framework.TestCase;


/**
 * Expression based Estimator
 */
public class ExpressionEstimatorTest extends TestCase
{
    public void testExpressionEstimator() throws ShamanException
    {
        MemorySupplier      ms = new MemorySupplier();
        ExpressionEstimator ee = new ExpressionEstimator();
        InstanceSetMemory   im = new InstanceSetMemory();
        
        // Train an Estimator
        ee.registerSupplier(0, ms, 0);
        ms.registerConsumer(0, ee, 0);
        ms.registerConsumer(0, im, 0);
        
        // Load test-set and modify goal to estimator goal type
        TestSets.loadCancer(ms, false, true);
        DataModelDouble dm = (DataModelDouble)ms.getOutputDataModel(0);
        AttributeDouble ag = new AttributeDouble("class");
        ag.initAsNumberContinuous();
        ag.setAsGoal();
        dm.setAttribute(dm.getLearningProperty().getGoalIndex(), ag);
        
        // Set estimation expression.
        ee.setExpression("ifthenelse(((Field2 + Field3 + Field4 + Field5 + Field6 + Field7 + Field8 + Field9) / 8) >= 3.70, 4, 2)");
        ee.init();
        
        // Create data-set
        im.create(ms);
        
        // Train using 10-fold Cross Validatin
        Validation          val;
        ValidationEstimator valest;
        
        val = new Validation(im, ee);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{10.0});
        val.test();
        valest = val.getValidationEstimator();
        
        // Check estimation error bounds
        System.err.println(valest.getError(ValidationEstimator.ERROR_MEAN_ABSOLUTE));
        assertEquals(0.0974, valest.getError(ValidationEstimator.ERROR_MEAN_ABSOLUTE), 0.1);
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
    
    public ExpressionEstimatorTest(String name)
    {
        super(name);
    }
}
