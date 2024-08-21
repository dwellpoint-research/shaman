package org.shaman.art;

import org.shaman.art.LinearRegressionBase;
import org.shaman.art.LinearRegressionClassifier;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;
import org.shaman.learning.Validation;
import org.shaman.learning.ValidationClassifier;
import org.shaman.preprocessing.Normalization;

import junit.framework.TestCase;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;


public class LinearRegressionClassifierTest extends TestCase
{

    public void testCancer() throws Exception
    {
        MemorySupplier    ms = new MemorySupplier();
        InstanceSetMemory im = new InstanceSetMemory();
        Normalization   norm = new Normalization();
        LinearRegressionClassifier reg = new LinearRegressionClassifier();

        norm.registerSupplier(0, ms, 0);
        ms.registerConsumer(0, norm, 0);
        norm.registerConsumer(0, reg, 0);
        reg.registerSupplier(0, norm, 0);

        // Load Wisconsin Breast Cancer data-set
        TestSets.loadCancer(ms, true, true);

        // Standardize the data.
        norm.setType(Normalization.TYPE_STANDARDIZE);
        norm.init();

        // Setup Linear Regression
        reg.setAttributeSelectionMethod(LinearRegressionBase.SELECTION_GREEDY);
        reg.setSolveMethod(LinearRegressionBase.SOLVE_CONJUGATE_GRADIENT);
        reg.setEliminateColinearAttributes(true);
        reg.setRidge(0.01);
        reg.init();

        // Create the DataSet from the loaded data
        im.create(ms);

        // Standardize the data
        norm.trainTransformation(im);
        //im = InstanceSetMemory.estimateAll(im, norm);

        // Train using Cross Validation
        Validation           val;
        ValidationClassifier valclas;

        val = new Validation(im, reg);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{10.0});
        val.test();
        valclas = val.getValidationClassifier();

        double [][]cmraw = valclas.getConfusionMatrix();
        DoubleMatrix2D cm    = DoubleFactory2D.dense.make(cmraw);
        System.out.println(cm);
        System.out.println("Error : "+valclas.getClassificationError()+"\n");

        // Check if the Classification Error is around the expected value...
        assertEquals(0.04, valclas.getClassificationError(), 0.05);
    }
}
