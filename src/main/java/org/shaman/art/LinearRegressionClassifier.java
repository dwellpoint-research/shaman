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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.shaman.dataflow.Persister;
import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.CachingPresenter;
import org.shaman.learning.Classifier;
import org.shaman.learning.ClassifierTransformation;
import org.shaman.learning.Presenter;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Linear Regression Classifier</h2>
 */
public class LinearRegressionClassifier extends ClassifierTransformation implements Classifier, Persister
{
    private static Log log = LogFactory.getLog(LinearRegressionClassifier.class);

    private LinearRegressionBase regression;
   
    // **********************************************************\
    // *             Classify using Linear Regression           *
    // **********************************************************/
    private int classifyObject(Object instance, double []confidence) throws LearnerException
    {
        int cind;

        cind = -1;
        try
        {
            int             i;
            double          reg, dif, difmin;
            AttributeDouble atdgoal;

            // Predict value using the Regression
            reg     = this.regression.regressionPrediction((DoubleMatrix1D)instance);

            // Find the goal class value closest to the predicted value
            cind    = -1;
            difmin  = Double.MAX_VALUE;
            atdgoal = (AttributeDouble)this.attgoal;
            for (i=0; i<atdgoal.getNumberOfCategories(); i++)
            {
                dif = Math.abs(reg - atdgoal.getCategoryDouble(i));
                if (dif < difmin) { cind = i; difmin = dif; }
            }
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }

        return (cind);
    }

    public int classify(ObjectMatrix1D instance, double []confidence) throws LearnerException
    {
        throw new LearnerException("Object-based Instances not supported");
    }

    public int classify(DoubleMatrix1D instance, double []confidence) throws LearnerException
    {
        return(classifyObject(instance, confidence));
    }

    // **********************************************************\
    // *                    Learner Interface                   *
    // **********************************************************/
    public void initializeTraining() throws LearnerException
    {
        // Initialize the Regression for Learning
        this.regression.initializeTraining();
    }

    public Presenter getTrainSet()
    {
        return(trainData);
    }

    public void setTrainSet(Presenter trainData) throws LearnerException
    {
        DataModel dmin;

        try
        {
            // Install new Training instances, initialize classifier data-structures
            dmin           = trainData.getDataModel();
            this.trainData = (CachingPresenter)trainData;
            setDataModel(dmin);
            initClassifier(dmin);
            
            // Install the Training instances in the Regression component
            this.regression.setTrainSet(trainData);
        }
        catch(ConfigException ex) { throw new LearnerException(ex); }
    }
    
    public void train() throws LearnerException
    {
        // Call the Linear Regression Training
        this.regression.train();
    }

    public boolean   isSupervised() { return(true); }

    public void checkDataModelFit(int port, DataModel dmin) throws DataModelException
    {
        // Make sure the input contains double based data
        checkClassifierDataModelFit(dmin, false, true, false);
        // And the Regression component has enough data-typing information
        this.regression.checkDataModelFit(dmin);
    }

    public void init() throws ConfigException
    {
        // Initialize classifier.
        super.init();
    }

    public void cleanUp() throws DataFlowException
    {
        // Cleanup regression model.
        this.regression.cleanUp();
    }
    
    // **********************************************************\
    // *                Parameter Configuration                 *
    // **********************************************************/
    public double  getRidge()                { return this.regression.getRidge(); }
    public void    setRidge(double newRidge) { this.regression.setRidge(newRidge); }
    public void    setAttributeSelectionMethod(int method) { this.regression.setAttributeSelectionMethod(method); }
    public int     getAttributeSelectionMethod()           { return(this.regression.getAttributeSelectionMethod());  }
    public boolean getEliminateColinearAttributes()                                       { return this.regression.getEliminateColinearAttributes(); }
    public void    setEliminateColinearAttributes(boolean newEliminateColinearAttributes) { this.regression.setEliminateColinearAttributes(newEliminateColinearAttributes); }
    public void    setSolveMethod(int solveMethod) { this.regression.setSolveMethod(solveMethod); }
    public int     getSolveMethod()                { return(this.regression.getSolveMethod()); }
     
    // **********************************************************\
    // *             State Persistence Implementation           *
    // **********************************************************/
    public void loadState(ObjectInputStream oin) throws ConfigException
    {
        this.regression.loadState(oin);
        super.loadState(oin);
    }

    public void saveState(ObjectOutputStream oout) throws ConfigException
    {
        this.regression.saveState(oout);
        super.saveState(oout);
    }
    
    // **********************************************************\
    // *                      Construction                      *
    // **********************************************************/
    public LinearRegressionClassifier()
    {
        super();
        name        = "LinearRegressionClassifier";
        description = "Classifier using Linear Regression";
        
        this.regression = new LinearRegressionBase();
    }
}