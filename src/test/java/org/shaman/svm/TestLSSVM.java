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

import java.util.LinkedList;
import java.util.List;

import org.shaman.TestUtils;
import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.exceptions.ShamanException;
import org.shaman.learning.Classifier;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;
import org.shaman.learning.Validation;
import org.shaman.learning.ValidationClassifier;
import org.shaman.svm.Kernel;
import org.shaman.svm.LSSVM;

import junit.framework.TestCase;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.jet.random.Uniform;


/**
 * <h2>Test LSSVM</h2>
 * Unit Tests for Least-Squares Support Vector Machine
 */

// **********************************************************\
// *   Unit Tests for Least-Squares Support Vector Machine  *
// **********************************************************/
public class TestLSSVM extends TestCase
{
    // **********************************************************\
    // *                 Classifier Test-Case                   *
    // **********************************************************/
    public void testLSSVM() throws ShamanException
    {
        // Load a Test Set.
        MemorySupplier    ms = new MemorySupplier();
        LSSVM            svm = new LSSVM();
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
        
        svm.setKernel(kertype, sigsq);
        svm.setGamma(gam);
        svm.setClassifierOutput(Classifier.OUT_CLASS_AND_CONFIDENCE);
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
    
    public void doNot_testCore() throws ShamanException
    {
        int   i;
        final int nump = 40;
        int    x_dim = 2;
        
        // Make a 2D datamodel with 1 goal attribute
        DataModelDouble   dm2d = (DataModelDouble)TestUtils.makeNumberDataModel(3, true);
        AttributeDouble atgoal = new AttributeDouble();
        atgoal.initAsSymbolCategorical(new double[]{0,1});
        atgoal.setIsActive(false);
        atgoal.setValuesAsGoal();
        dm2d.setAttribute(2, atgoal);
        dm2d.getLearningProperty().setGoal(2);
        
        // Make in InstanceSet for the Training Data
        InstanceSetMemory traindata = new InstanceSetMemory();
        List              instrain  = new LinkedList();
        traindata.create(instrain, dm2d);
        
        // Make some training data
        for (i=0; i<nump; i++)
        {
            DoubleMatrix1D innow = (DoubleMatrix1D)dm2d.createDefaultVector();
            double []xtemp = new double[x_dim];
            double   ytemp;
            
            xtemp[0] = 2*Uniform.staticNextDouble()-1;
            xtemp[1] = 2*Uniform.staticNextDouble()-1;
            if (Math.sin(xtemp[0]+xtemp[1]) > 0) ytemp =  0.0;
            else                                 ytemp =  1.0;
            
            innow.set(0, xtemp[0]);
            innow.set(1, xtemp[1]);
            innow.set(2, ytemp);
            
            instrain.add(innow);
        }
        traindata.create(instrain, dm2d);
        
        double   gam     = 10;
        int      kertype = Kernel.KERNEL_GAUSSIAN;
        double []sigsq   = new double[]{0.15};
        
        // Train the support vector machine
        LSSVM svm = new LSSVM();
        svm.setTrainSet(traindata);
        svm.setKernel(kertype, sigsq);
        svm.setGamma(gam);
        svm.train();
        
        // Evalute some points
        int   numtest = 50;
        DoubleMatrix1D Xt = DoubleFactory1D.dense.make(x_dim);
        double         Yt;
        for (i=0; i<numtest; i++)
        {
            double []xttemp = new double[x_dim];
            xttemp[0] = 2*Uniform.staticNextDouble()-1;
            xttemp[1] = 2*Uniform.staticNextDouble()-1;
            Xt.assign(xttemp);
            
            Yt = svm.classify(Xt);
            
            System.out.println(xttemp[0]+" "+xttemp[1]+"  ---> "+Yt);
        }
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
    
    public TestLSSVM(String name)
    {
        super(name);  
    }
}
