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
package org.shaman.learning;


import org.shaman.datamodel.AttributeObject;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelObject;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Test and Reporting Algorithms for Estimator</h2>
 *
 * Statistically founded test procedures and reporting for
 * estimator algorithms.
 *
 * <br>
 * <i>Tom M. Mitchell (1997), Machine Learning, Chapter 5</i><br>
 * <i>Witten I., Frank E. (2000), Data Mining, Chapter 5</i>
 */

// **********************************************************\
// *        Estimator Testing and Test Result Reporting     *
// **********************************************************/
public class ValidationEstimator
{
    // **********************************************************\
    // *      Statistically Founded Test for Estimators        *
    // **********************************************************/
    // General Data about the data to Test
    private Validation       val;           // The Validation to which this test belongs
    private Presenter        instances;     // The complete set of instances (if possible)
    private DataModel        dataModel;     // DataModel of the Data
    private AttributeObject  atobgoal;      // The goal attribute when using supervised object estimation
    private int       numTest;              // Number of instances tested
    private int     []split;                // How to Split up the total instances set
    private int       numFolds;             // Number of Folds
    private Estimator es;                   // The estimator used in the tests
    
    // Estimator Test Buffers
    private boolean onlyDifference;         // Only know the difference between goal and estimation...
    private double  [][]estTest;            // Output of the estimation. If unsupervised, the estimation error.
    private double  []estOut;               // The output of the estimation.
    private double  []estGoal;              // If supervised, the goal function's value. If unsupervised 0.
    private boolean []estError;             // If true, the estimator failed.
    
    // Kind of Error Measure
    /** Mean Squared Error Measure Type */
    public static final int ERROR_MEAN_SQUARED            = 0;
    /** Root Mean Squared Error Measure Type */
    public static final int ERROR_ROOT_MEAN_SQUARED       = 1;
    /** Mean Absolute Error Measure Type */
    public static final int ERROR_MEAN_ABSOLUTE           = 2;
    /** Relative Squared Error Measure Type */
    public static final int ERROR_RELATIVE_SQUARED        = 3;
    /** Root Relative Squared Error Measure Type */
    public static final int ERROR_ROOT_RELATIVE_SQUARED   = 4;
    /** Relative Absolute Error Measure Type */
    public static final int ERROR_RELATIVE_ABSOLUTE       = 5;
    /** Correlation Coefficient Error Measure Type */
    public static final int ERROR_CORRELATION_COEFFICIENT = 6;
    
    // **********************************************************\
    // *      (Unsupervised) Estimation Testing / Reporting     *
    // **********************************************************/
    /**
     * Get some kind of error measure of the estimator tests.
     * @param type The type of error measure, as defined by the constants ERROR_x.
     * @return The error measure.
     */
    public double getError(int type) throws LearnerException
    {
        return(getError(type, 1, numFolds)); // Calculate error for all folds.
    }
    
    /**
     * Get some kind of error measure of the estimator performance on the given fold.
     * @param fold The fold number (starts at 1)
     * @param type The type of error measure, as defined by the constants ERROR_x.
     * @return The error measure for the given fold.
     */
    public double getError(int fold, int type) throws LearnerException
    {
        return(getError(type, fold, fold)); // Calculate error only for the given fold.
    }
    
    private double getError(int type, int fmin, int fmax) throws LearnerException
    {
        // Calculate error for the tests in the fold that have fmin <= foldnumber <= fmax.
        int    i;
        int    count;
        double err;
        
        // Error types that can be used in all cases. Only the difference between goal and estimation is used.
        if      (type == ERROR_MEAN_SQUARED)
        {
            err = 0; count = 0;
            for (i=0; i<estOut.length; i++)
            {
                if (inFold(i, fmin, fmax) && (!estError[i])) { count++; err += (estGoal[i] - estOut[i])*(estGoal[i] - estOut[i]); }
            }
            if (count > 0) err /= count;
            else           err = 0;
        }
        else if (type == ERROR_ROOT_MEAN_SQUARED)
        {
            err = 0; count = 0;
            for (i=0; i<estOut.length; i++)
            {
                if (inFold(i, fmin, fmax) && (!estError[i])) { count++; err += (estGoal[i] - estOut[i])*(estGoal[i] - estOut[i]); }
            }
            if (count > 0) err /= count;
            else           err = 0;
            err = Math.sqrt(err);
        }
        else if (type == ERROR_MEAN_ABSOLUTE)
        {
            err = 0; count = 0;
            for (i=0; i<estOut.length; i++)
            {
                if (inFold(i, fmin, fmax) && (!estError[i])) { count++; err += Math.abs(estGoal[i] - estOut[i]); }
            }
            if (count > 0) err /= count;
            else           err = 0;
        }
        else if (!onlyDifference)
        {
            // Error type that can only be used for supervised primitive estimation.
            double mgoal, mest;
            double den;
            
            count = 0; mgoal = 0; mest = 0;
            for (i=0; i<estOut.length; i++)
            {
                if (inFold(i, fmin, fmax) && (!estError[i])) { count++; mgoal += estGoal[i]; mest += estOut[i]; }
            }
            if (count > 0) { mgoal /= count; mest /= count;  }
            
            if (type == ERROR_RELATIVE_SQUARED)
            {
                err = 0; den = 0;
                for (i=0; i<estOut.length; i++)
                {
                    if (inFold(i, fmin, fmax) && (!estError[i]))
                    {
                        err  += (estOut[i]-estGoal[i])*(estOut[i]-estGoal[i]);
                        den += (estGoal[i]-mgoal)    *(estGoal[i]-mgoal);
                    }
                }
                if (den > 0) err /= den;
            }
            else if (type == ERROR_ROOT_RELATIVE_SQUARED)
            {
                err = 0; den = 0;
                for (i=0; i<estOut.length; i++)
                {
                    if (inFold(i, fmin, fmax) && (!estError[i]))
                    {
                        err  += (estOut[i]-estGoal[i])*(estOut[i]-estGoal[i]);
                        den += (estGoal[i]-mgoal)    *(estGoal[i]-mgoal);
                    }
                }
                if (den > 0) err /= den;
                err = Math.sqrt(err);
            }
            else if (type == ERROR_RELATIVE_ABSOLUTE)
            {
                err = 0; den = 0;
                for (i=0; i<estOut.length; i++)
                {
                    if (inFold(i, fmin, fmax) && (!estError[i]))
                    {
                        err += Math.abs(estOut[i]-estGoal[i]);
                        den += Math.abs(estGoal[i]-mgoal);
                    }
                }
                if (den > 0) err /= den;
            }
            else if (type == ERROR_CORRELATION_COEFFICIENT)
            {
                double spa, sp, sa;
                spa = 0; sp = 0; sa = 0;
                for (i=0; i<estOut.length; i++)
                {
                    if (inFold(i, fmin, fmax) && (!estError[i]))
                    {
                        spa += (estOut[i]-mest)   * (estGoal[i]-mgoal);
                        sp  += (estOut[i]-mest)   * (estOut[i]-mest);
                        sa  += (estGoal[i]-mgoal) * (estGoal[i]-mgoal);
                    }
                }
                if (count > 1) { spa /= count-1; sp /= count-1; sa /= count-1; }
                if ((sp > 0) && (sa > 0)) err = spa/(sp*sa);
                else                      err = 0;
            }
            else throw new LearnerException("Unknown error measure type requested.");
        }
        else throw new LearnerException("Can't only use the specified error type on supervised primitive estimation problems.");
        
        return(err);
    }
    
    private boolean inFold(int i, int fmin, int fmax)
    {
        boolean in;
        int     s;
        
        s = split[i];
        if ((s >= fmin) && (s <= fmax)) in = true;
        else                            in = false;
        
        return(in);
    }
    
    // **********************************************************\
    // *                   Estimator Testing                    *
    // **********************************************************/
    void testEstimator(int fold, Presenter testSet) throws LearnerException
    {
        int             i;
        double          gnow, enow;
        Object          ognow, oenow;
        int             numins;
        DoubleMatrix1D  insnow;
        ObjectMatrix1D  oinsnow;
        boolean         []errbuf;
        double          []goalbuf;
        int             epos;
        boolean         sup;
        
        numins            = testSet.getNumberOfInstances();
        dataModel         = testSet.getDataModel();
        sup               = es.isSupervised();
        errbuf            = new boolean[numins];
        goalbuf           = new double[numins];
        estTest[fold-1]   = new double[numins];
        
        // Estimate the error for all instances.
        for (i=0; i<numins; i++)
        {
            if (isPrimitive())  // Primitive Data
            {
                insnow = testSet.getInstance(i);
                if (sup) // This estimator estimates the goal's value. (e.g. MLP with regression)
                {
                    gnow = testSet.getGoal(i);
                    enow = es.estimate(insnow).getQuick(0);
                }
                else   // This estimator approximates itself. (e.g. Discretization)
                {
                    gnow = 0;
                    enow = es.estimateError(insnow);
                }
            }
            else  // Object based data.
            {
                oinsnow = testSet.getObjectInstance(i);
                if (sup)
                {
                    // Supervised Object Estimation. Use distance function of Object based Goal Attribute.
                    ognow = testSet.getObjectGoal(i);
                    oenow = es.estimate(oinsnow).getQuick(0);
                    if (oenow != null)
                    {
                        try
                        {
                            gnow = 0;
                            enow = atobgoal.distance(ognow, oenow);
                        }
                        catch(DataModelException ex) { throw new LearnerException(ex); }
                    }
                    else { gnow = Double.NaN; enow = Double.NaN; }
                }
                else
                {
                    // Unsupervised Self Object Estimation is easy.
                    gnow = 0;
                    enow = es.estimateError(oinsnow);
                }
            }
            
            if (!Double.isNaN(enow))            // If Estimation worked... Update this fold's buffers.
            {
                numTest++;
                estTest[fold-1][i] = enow;
                goalbuf[i]         = gnow;
                errbuf[i]          = false;
            }
            else
            {
                estTest[fold-1][i] = Double.NaN;  // Else remember that the thing failed.
                goalbuf[i]         = Double.NaN;
                errbuf[i]          = true;
            }
        }
        // Merge the folds buffers whit the global ones on which the reporting operates.
        epos = 0;
        for (i=0; i<split.length; i++)
        {
            if (split[i] == fold)
            {
                estOut[i]     = estTest[fold-1][epos];
                estGoal[i]    = goalbuf[epos];
                estError[i]   = errbuf[epos];
                epos++;
            }
        }
    }
    
    // **********************************************************\
    // *           Test Initialization / Cleanup                *
    // **********************************************************/
    void initTest(Validation _val, Presenter _instances, int []_split, int _numFolds, Estimator _es) throws LearnerException
    {
        int numins;
        
        // Remember the general information about the comming test.
        val       = _val;
        instances = _instances;
        dataModel = instances.getDataModel();
        split     = _split;
        numFolds  = _numFolds;
        numTest   = 0;
        es        = _es;
        
        // Determine if both the goal and estimate are known, or just the difference between them.
        if (!isPrimitive())
        {
            onlyDifference = true;
            if (es.isSupervised())
            {
                try
                {
                    // The Supervised Object Estimation Case needs an Object Goal with a Distance function.
                    atobgoal = ((DataModelObject)dataModel).getAttributeObject(dataModel.getLearningProperty().getGoalIndex());
                    if (atobgoal.getDistance() == null)
                        throw new LearnerException("Can't test a supervised Object estimator without a distance defined on the goal field.");
                }
                catch(DataModelException ex) { throw new LearnerException(ex); }
            }
        }
        else
        {
            if (es.isSupervised()) onlyDifference = false;
            else                   onlyDifference = true;
        }
        
        // Regression Test Result buffers
        numins   = instances.getNumberOfInstances();
        estTest  = new double[numFolds][];
        estOut   = new double[numins];
        estGoal  = new double[numins];
        estError = new boolean[numins];
    }
    
    void endTest()
    {
        // Nothing really
    }
    
    public ValidationEstimator()
    {
    }
    
    private boolean isPrimitive()
    {
        return(this.dataModel instanceof DataModelDouble);
    }
}
