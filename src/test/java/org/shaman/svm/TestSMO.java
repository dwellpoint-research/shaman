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
package org.shaman.svm;

import org.shaman.exceptions.ShamanException;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;
import org.shaman.learning.Validation;
import org.shaman.learning.ValidationClassifier;
import org.shaman.svm.Kernel;
import org.shaman.svm.SMO;

import junit.framework.TestCase;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;


/**
 * <h2>Test SMO</h2>
 * Unit Tests for Sequential Mimimal Optimization Support Vector Machine
 *
 * @author Johan Kaers
 * @version 2.0
 */

// **********************************************************\
// *    Unit Tests for Sequential Mimimal Optimization      *
// **********************************************************/
public class TestSMO extends TestCase
{
    // **********************************************************\
    // *                 Classifier Test-Case                   *
    // **********************************************************/
    public void testSMO() throws ShamanException
    {
        // Load a Test Set.
        MemorySupplier    ms = new MemorySupplier();
        SMO              svm = new SMO();
        InstanceSetMemory im = new InstanceSetMemory();
        ms.registerConsumer(0, svm, 0);
        ms.registerConsumer(0, im, 0);
        svm.registerSupplier(0, ms, 0);

        //TestSets.loadCancer(ms, false, true);
        TestSets.loadIris(ms, true);

        // Configure and Train the Support Vector Machine
        double   gam     = 10;
        int      kertype = Kernel.KERNEL_GAUSSIAN;
        double []sigsq   = new double[]{1.2};
        
        svm.setKernel(new Kernel(kertype, sigsq));
        svm.setSMOParameters(0, 2.0);
        svm.init();
       
        im.create(ms);       

        DoubleMatrix2D       conf;
        Validation           val;
        ValidationClassifier valclas;

        im.create(ms);
        val = new Validation(im, svm);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{3.0});
        val.test();
        valclas = val.getValidationClassifier();
        System.out.println("Error : "+valclas.getClassificationError());
        conf = DoubleFactory2D.dense.make(valclas.getConfusionMatrix());
        System.out.println("Confusion Matrix"+conf);
    }
    
    // **********************************************************\
    // *               Unit Test Setup/Teardown                 *
    // **********************************************************/
    protected void setUp() throws Exception
    {
        super.setUp();
    }
    
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
    
    public TestSMO(String name)
    {
        super(name);  
    }
}
