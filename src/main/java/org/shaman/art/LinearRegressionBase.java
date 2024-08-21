/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2007 Shaman Research                   *
\*********************************************************/
package org.shaman.art;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.shaman.dataflow.Persister;
import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.CachingPresenter;
import org.shaman.learning.Presenter;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.matrix.linalg.LUDecomposition;
import cern.colt.matrix.linalg.QRDecomposition;


/**
 * <h2>Linear Regression</h2>
 * Uses the Akaike criterion for model selection.
 * Is able to deal with weighted instances.
 */
public class LinearRegressionBase implements Persister
{
    private static Log log = LogFactory.getLog(LinearRegressionBase.class);

    // Attribute Selection methods
    public static final int SELECTION_M5     = 0;
    public static final int SELECTION_NONE   = 1;
    public static final int SELECTION_GREEDY = 2;

    // Method to solve system of linear equations
    public static final int SOLVE_MATRIX             = 0;
    public static final int SOLVE_CONJUGATE_GRADIENT = 1;

    private int     attributeSelection          = SELECTION_M5;     // Method to select relevant attributes
    private boolean eliminateColinearAttributes = true;             // Find and deselect colinear attributes?       
    private double  ridge                       = 1.0e-8;           // Tikhonov regularization parameter in Ridge Regression
    private int     solveMethod                 = SOLVE_MATRIX;     // Solve with Conjugate Gradient method?

    // ---------- Model ------------
    private double  []m_Coefficients;               // Array for storing coefficients of linear regression.
    private boolean []m_SelectedAttributes;         // Which attributes are relevant?
    private double  []m_Means;                      // The attributes means
    private double  []m_StdDevs;                    // The attribute standard deviations
    private double    m_ClassStdDev;                // The standard deviations of the class attribute
    private double    m_ClassMean;                  // The mean of the class attribute

    // ---------- Learner ----------
    protected Presenter        trainData;           // Training set
    protected DataModel        dataModel;           // Training data description
    protected int            []actind;              // Indices of the instance attributes
    protected Attribute        attgoal;             // The goal attributes

    // **********************************************************\
    // *             State Persistence Implementation           *
    // **********************************************************/
    public void loadState(ObjectInputStream oin) throws ConfigException
    {
        try
        {
            // Parameters
            this.attributeSelection          = oin.readInt();
            this.solveMethod                 = oin.readInt();
            this.eliminateColinearAttributes = oin.readBoolean();
            this.ridge                       = oin.readDouble();
            // Trained Model
            this.m_Coefficients       = (double [])oin.readObject();
            this.m_SelectedAttributes = (boolean [])oin.readObject();
            this.m_Means              = (double [])oin.readObject();
            this.m_StdDevs            = (double [])oin.readObject();
            this.m_ClassMean          = oin.readDouble();
            this.m_ClassStdDev        = oin.readDouble();
        }
        catch(IOException ex)            { throw new ConfigException(ex); }
        catch(ClassNotFoundException ex) { throw new ConfigException(ex); }
    }

    public void saveState(ObjectOutputStream oout) throws ConfigException
    {
        try
        {
            // Parameters
            oout.writeInt(this.attributeSelection);
            oout.writeInt(this.solveMethod);
            oout.writeBoolean(this.eliminateColinearAttributes);
            oout.writeDouble(this.ridge);
            // Trained Model
            oout.writeObject(this.m_Coefficients);
            oout.writeObject(this.m_SelectedAttributes);
            oout.writeObject(this.m_Means);
            oout.writeObject(this.m_StdDevs);
            oout.writeDouble(this.m_ClassMean);
            oout.writeDouble(this.m_ClassStdDev);
        }
        catch(IOException ex) { throw new ConfigException(ex); }
    }

    // **********************************************************\
    // *                    Learner Support                     *
    // **********************************************************/
    public void initializeTraining() throws LearnerException
    {
        DataModel dmin;

        // Install new Training instances, initialize data-structures
        dmin           = this.trainData.getDataModel();
        this.trainData = (CachingPresenter)trainData;
        this.dataModel = dmin;
        this.actind    = dmin.getActiveIndices();
        this.attgoal   = dmin.getAttribute(dmin.getLearningProperty().getGoalIndex());

        // Clear model
        this.m_Coefficients       = null;
        this.m_SelectedAttributes = null;
    }

    public void setTrainSet(Presenter trainData) throws LearnerException
    {
        this.trainData = trainData;
    }

    public void cleanUp() throws DataFlowException
    {
        // Clear trained model.
        this.m_Coefficients       = null;
        this.m_SelectedAttributes = null;
    }

    public void checkDataModelFit(DataModel dmin) throws DataModelException
    {
        // Verify that each Attribute has a proper data-description
    }

    // **********************************************************\
    // *                Parameter Configuration                 *
    // **********************************************************/
    public double  getRidge()                { return this.ridge; }
    public void    setRidge(double newRidge) { this.ridge = newRidge; }
    public void    setSolveMethod(int solveMethod) { this.solveMethod = solveMethod; }
    public int     getSolveMethod()                { return(this.solveMethod); }
    public void    setAttributeSelectionMethod(int method) { attributeSelection = method; }
    public int     getAttributeSelectionMethod()           { return(attributeSelection);  }
    public boolean getEliminateColinearAttributes()                                       { return eliminateColinearAttributes; }
    public void    setEliminateColinearAttributes(boolean newEliminateColinearAttributes) { eliminateColinearAttributes = newEliminateColinearAttributes; }

    // **********************************************************\
    // *            Train Linear Regression Model               *
    // **********************************************************/
    public void train() throws LearnerException
    {
        // Start with all Attributes selected
        m_SelectedAttributes = new boolean[this.actind.length];
        for (int i = 0; i < this.actind.length; i++) m_SelectedAttributes[i] = true;
        m_Coefficients = null;

        // Compute mean and standard deviations of all Attributes
        m_Means   = new double[this.actind.length];
        m_StdDevs = new double[this.actind.length];
        for (int j = 0; j < this.actind.length; j++)
        {
            m_Means[j]   = meanOrMode(this.trainData, j);
            m_StdDevs[j] = Math.sqrt(variance(this.trainData, j));

            // Deselect Attributes with constant values
            if (m_StdDevs[j] == 0) { m_SelectedAttributes[j] = false; }
        }

        // Also calculate Mean and Standard Deviation for the Class Attribute
        m_ClassStdDev = Math.sqrt(variance(this.trainData, -1));
        m_ClassMean   = meanOrMode(this.trainData, -1);

        // Perform the regression
        findBestModel();
    }

    private double meanOrMode(Presenter instances, int attIndex) throws LearnerException
    {
        double result, found, val;
        int    i, numins;

        // From: Instances.meanOrMode()
        // Continuous: return mean
        // Nominal   : return most frequent

        // Calculate the Mean or Most Frequent value of the given Attribute
        numins = instances.getNumberOfInstances();
        result = 0;
        found  = 0;
        for (i=0; i<numins; i++)
        {
            found  += instances.getWeight(i);
            if (attIndex != -1) val = instances.getInstance(i).getQuick(attIndex);
            else                val = instances.getGoal(i);
            result += instances.getWeight(i)*val;
        }
        result = result / found;

        return(result);
    }

    private double variance(Presenter instances, int attIndex) throws LearnerException
    {
        // From: Instances.variance()
        double sum = 0, sumSquared = 0, sumOfWeights = 0;
        double val;

        for (int i = 0; i < instances.getNumberOfInstances(); i++)
        {
            if (attIndex != -1) val = instances.getInstance(i).getQuick(attIndex);
            else                val = instances.getGoal(i);
            sum          += instances.getWeight(i) * val;
            sumSquared   += instances.getWeight(i) * val * val;
            sumOfWeights += instances.getWeight(i);
        }
        double result = (sumSquared - (sum * sum / sumOfWeights)) / (sumOfWeights - 1);

        if (result < 0) return 0;
        else            return result;
    }

    private boolean deselectColinearAttributes(boolean [] selectedAttributes, double [] coefficients)
    {
        // Removes the attribute with the highest standardised coefficient greater than 1.5 from the selected attributes.
        double maxSC   = 1.5;
        int    maxAttr =  -1;
        int    coeff   = 0;
        for (int i = 0; i < selectedAttributes.length; i++)
        {
            if (selectedAttributes[i])
            {
                double SC = Math.abs(coefficients[coeff] * m_StdDevs[i] / m_ClassStdDev);
                if (SC > maxSC)
                {
                    maxSC   = SC;
                    maxAttr = i;
                }
                coeff++;
            }
        }
        if (maxAttr >= 0)
        {
            selectedAttributes[maxAttr] = false;
            log.debug("Deselected colinear attribute:" + (maxAttr + 1)+ " with standardised coefficient: " + maxSC);
            return true;
        }
        return false;
    }

    private void findBestModel() throws LearnerException
    {
        // For the weighted case we still use numInstances in the calculation of the Akaike criterion. 
        int numInstances = this.trainData.getNumberOfInstances();

        // Do regression until no more colinear Attributes are found.
        do
        {
            m_Coefficients = doRegression(m_SelectedAttributes);
        }
        while (eliminateColinearAttributes && deselectColinearAttributes(m_SelectedAttributes, m_Coefficients));

        // Count the number of remaining selected Attributes
        int numAttributes = 1;
        for (int i = 0; i < m_SelectedAttributes.length; i++)
            if (m_SelectedAttributes[i]) numAttributes++;

        // Calculate the squared error of the model on the training data and initial Akaike value
        double fullMSE = calculateSE(m_SelectedAttributes, m_Coefficients);
        double akaike = (numInstances - numAttributes) + 2 * numAttributes;

        boolean improved;
        int     currentNumAttributes = numAttributes;
        if (this.attributeSelection == SELECTION_GREEDY)
        {
            do
            {
                boolean [] currentSelected = (boolean []) m_SelectedAttributes.clone();
                improved = false;
                currentNumAttributes--;

                // Loop over the Select Attributes
                for (int i = 0; i < m_SelectedAttributes.length; i++)
                {
                    if (currentSelected[i])
                    {
                        // Calculate the Akaike rating without this attribute
                        currentSelected[i] = false;
                        double [] currentCoeffs = doRegression(currentSelected);
                        double currentMSE = calculateSE(currentSelected, currentCoeffs);
                        double currentAkaike = currentMSE / fullMSE  * (numInstances - numAttributes) + 2 * currentNumAttributes;

                        // If it is better than the current best
                        if (currentAkaike < akaike)
                        {
                            log.debug("Removing attribute " + (i + 1) + " improved Akaike: " + currentAkaike);

                            // Deselect this Attribute and remember Coefficients
                            improved = true;
                            akaike   = currentAkaike;
                            System.arraycopy(currentSelected, 0, m_SelectedAttributes, 0, m_SelectedAttributes.length);
                            m_Coefficients = currentCoeffs;
                        }
                        else currentSelected[i] = true;
                    }
                }
            }
            while (improved);
        }
        else if (this.attributeSelection == SELECTION_M5)
        {
            // Step through the attributes removing the one with the smallest standardised coefficient until no improvement in Akaike
            do
            {
                improved = false;
                currentNumAttributes--;

                // Find attribute with smallest SC
                double minSC   = 0;
                int    minAttr = -1;
                int    coeff   = 0;
                for (int i = 0; i < m_SelectedAttributes.length; i++)
                {
                    if (m_SelectedAttributes[i])
                    {
                        double SC = Math.abs(m_Coefficients[coeff] * m_StdDevs[i] / m_ClassStdDev);
                        if ((coeff == 0) || (SC < minSC))
                        {
                            minSC   = SC;
                            minAttr = i;
                        }
                        coeff++;
                    }
                }

                // See whether removing it improves the Akaike score
                if (minAttr >= 0)
                {
                    m_SelectedAttributes[minAttr] = false;
                    double []currentCoeffs = doRegression(m_SelectedAttributes);
                    double   currentMSE    = calculateSE(m_SelectedAttributes, currentCoeffs);
                    double   currentAkaike = currentMSE / fullMSE * (numInstances - numAttributes) + 2 * currentNumAttributes;

                    // If it is better than the current best
                    if (currentAkaike < akaike)
                    {
                        log.debug("Removing attribute " + (minAttr + 1)+ " improved Akaike: " + currentAkaike);

                        // Deselect this Attribute and remember Coefficients
                        improved = true;
                        akaike   = currentAkaike;
                        m_Coefficients = currentCoeffs;
                    }
                    else
                    {
                        m_SelectedAttributes[minAttr] = true;
                    }
                }
            }
            while (improved);
        }
    }

    private double calculateSE(boolean [] selectedAttributes, double []coefficients) throws LearnerException
    {
        // Calculate the squared error of a regression model on the training data
        double se = 0;
        for (int i = 0; i < this.trainData.getNumberOfInstances(); i++)
        {
            double prediction = regressionPrediction(this.trainData.getInstance(i),  selectedAttributes, coefficients);
            double error = prediction - this.trainData.getGoal(i);
            se += error * error;
        }
        return se;
    }

    protected double regressionPrediction(DoubleMatrix1D transformedInstance) throws LearnerException
    {
        return regressionPrediction(transformedInstance, this.m_SelectedAttributes, this.m_Coefficients);
    }

    private double regressionPrediction(DoubleMatrix1D transformedInstance, boolean []selectedAttributes, double []coefficients) throws LearnerException
    {
        // Predict output of the given instance using the given regression model
        double result = 0;
        int    column = 0;
        for (int j = 0; j < transformedInstance.size(); j++)
        {
            if (selectedAttributes[j])
            {
                result += coefficients[column] * transformedInstance.getQuick(j);
                column++;
            }
        }
        result += coefficients[column];

        return result;
    }

    private double []doRegression(boolean [] selectedAttributes) throws LearnerException
    {
        double[]       coefficients;
        DoubleMatrix2D independent;
        DoubleMatrix2D dependent;
        double       []weights;

        // Perform linear regression on the train set using only the selected attributes

        // Count the number of selected attributes
        int numAttributes = 0;
        for (int i = 0; i < selectedAttributes.length; i++)
            if (selectedAttributes[i]) numAttributes++;
        coefficients = new double[numAttributes + 1];

        // If there are any selected attributes left
        if (numAttributes > 0)
        {
            // Put the standardized train-data in the Matrices
            independent = DoubleFactory2D.dense.make(this.trainData.getNumberOfInstances(), numAttributes);
            dependent   = DoubleFactory2D.dense.make(this.trainData.getNumberOfInstances(), 1);
            for (int i = 0; i < this.trainData.getNumberOfInstances(); i ++)
            {
                DoubleMatrix1D inst = this.trainData.getInstance(i);
                int column = 0;
                for (int j = 0; j < this.actind.length; j++)
                {
                    if (selectedAttributes[j])
                    {
                        double value = (inst.getQuick(j) - m_Means[j]) / m_StdDevs[j];
                        independent.setQuick(i, column, value);
                        column++;
                    }
                }
                dependent.setQuick(i, 0, this.trainData.getGoal(i));
            }

            // Collect the instance weights
            weights = new double [this.trainData.getNumberOfInstances()];
            for (int i = 0; i < weights.length; i++)
                weights[i] = this.trainData.getWeight(i);

            // Compute coefficient using Ridge Regression
            double[] coeffsWithoutIntercept  = solve(independent, dependent, weights, ridge).toArray();
            System.arraycopy(coeffsWithoutIntercept, 0, coefficients, 0, numAttributes);
        }
        coefficients[numAttributes] = m_ClassMean;

        // Convert coefficients into original scale
        int column = 0;
        for(int i = 0; i < this.actind.length; i++)
        {
            if (selectedAttributes[i])
            {
                coefficients[column]                  /= m_StdDevs[i];
                coefficients[coefficients.length - 1] -= coefficients[column] * m_Means[i];
                column++;
            }
        }

        return coefficients;
    }

    public DoubleMatrix1D solve(DoubleMatrix2D a, DoubleMatrix2D y, double[] w, double ridge) throws LearnerException
    { 
        DoubleMatrix2D wa;
        DoubleMatrix2D wy;

        // Multiply with given weight vector
        wa = DoubleFactory2D.dense.make(a.rows(), a.columns());
        wy = DoubleFactory2D.dense.make(a.rows(), 1);
        for (int i = 0; i < w.length; i++)
        {
            double sqrt_weight = Math.sqrt(w[i]);
            for (int j = 0; j < a.columns(); j++) wa.set(i, j, a.get(i, j) * sqrt_weight);
            wy.set(i, 0, y.get(i, 0) * sqrt_weight);
        }
        a = wa;
        y = wy;

        double[]       m_Coefficients;
        int            nc;
        Algebra        algebra;
        DoubleMatrix2D xt, b, ss, bb;
        DoubleMatrix1D solution;
        boolean        success;

        nc             = a.columns();
        m_Coefficients = new double[nc];
        algebra        = Algebra.DEFAULT;

        xt      = algebra.transpose(a);
        success = true;
        do
        {
            ss = algebra.mult(xt, a);

            // Set ridge regression adjustment
            for (int i = 0; i < nc; i++) ss.set(i, i, ss.get(i, i) + ridge);

            try
            {
                // Carry out the regression
                bb = algebra.mult(xt, y);
                for (int i = 0; i < nc; i++)  m_Coefficients[i] = bb.get(i, 0);
                b  = DoubleFactory2D.dense.make(m_Coefficients, m_Coefficients.length);
                solution = solve(ss, b);
                for (int i = 0; i < nc; i++) m_Coefficients[i] = solution.getQuick(i);
                success = true;
            } 
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
                ridge  *= 10;
                success = false;
            }
        }
        while (!success);

        return(DoubleFactory1D.dense.make(m_Coefficients));
    }

    private DoubleMatrix1D solve(DoubleMatrix2D A, DoubleMatrix2D B) throws LearnerException
    {
        if      (this.solveMethod == SOLVE_CONJUGATE_GRADIENT)  return solveConjugateGradient(A, B);
        else if (this.solveMethod == SOLVE_MATRIX)              return solveMatrix(A, B);
        else throw new LearnerException("Unkown method to solve system of linear equations specified.");
    }
    
    private DoubleMatrix1D solveConjugateGradient(DoubleMatrix2D A, DoubleMatrix2D B)
    {
        DoubleMatrix1D x, b;

        // Use Conjugate Gradient Ascent to solve  A*X = B.
        x = DoubleFactory1D.dense.make(B.rows());
        b = B.viewColumn(0);
        new ConjugateGradient().cg(A, x, b);

        return(x);
    }

    private DoubleMatrix1D solveMatrix(DoubleMatrix2D A, DoubleMatrix2D B)
    {
        DoubleMatrix2D X;
        DoubleMatrix1D xout;
        int            i;
        
        // Solve A*X = B. Use LU- or QR Decomposition, depending of whether A is square.
        if (A.columns() == A.rows()) X = new LUDecomposition(A).solve(B);
        else                         X = new QRDecomposition(A).solve(B);
        
        // Just return the first column
        xout = DoubleFactory1D.dense.make(X.rows());
        for (i = 0; i < xout.size(); i++) xout.setQuick(i, X.get(i, 0));
        
        return(xout);
    }
}