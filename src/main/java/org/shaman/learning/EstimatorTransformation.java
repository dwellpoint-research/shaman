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


import org.shaman.dataflow.Transformation;
import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelObject;
import org.shaman.datamodel.DataModelPropertyLearning;
import org.shaman.datamodel.DataModelPropertyVectorType;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Estimator Transformation</h2>
 * Base-class for machine learning Transformations
 * that implement some kind of function Estimator.
 */

// *********************************************************\
// *    Naive Base Classifier for Categorical Attributes   *
// *********************************************************/
public abstract class EstimatorTransformation extends Transformation implements Estimator
{
    // Machine Learning : Traning data and description
    protected Presenter        trainData;              // Training set
    protected DataModel        dataModel;              // Training data description
    protected DataModelDouble  dmdo;                   //     DataModel if data is double-based
    protected DataModelObject  dmob;                   //     DataModel if data is object-based
    
    // Typical Classifier data-structures
    protected boolean                   primitive;     // Data is double-based?
    protected DataModelPropertyLearning learn;         // Machine Learning bridge of the DataModel
    protected double                  []dallbuf;       // Buffer for fast manipulation of double vectors
    protected Object                  []oallbuf;       //                                 Object vectors 
    protected int                     []actind;        // Indices of the instance attributes
    protected Attribute                 attgoal;       // The goal attributes
    
    // *********************************************************\
    // *          Typical Classifier Implementation            *
    // *********************************************************/
    public abstract DoubleMatrix1D estimate(DoubleMatrix1D instance, double []conf) throws LearnerException;
    
    public abstract ObjectMatrix1D estimate(ObjectMatrix1D instance, double []conf) throws LearnerException;
    
    public ObjectMatrix1D estimate(ObjectMatrix1D instance) throws LearnerException
    {
        return(estimate(instance, null));
    }
    
    public DoubleMatrix1D estimate(DoubleMatrix1D instance) throws LearnerException
    {
        return(estimate(instance, null));
    }
    
    public double estimateError(DoubleMatrix1D instance) throws LearnerException
    {
        return(0);
    }
    
    public double estimateError(ObjectMatrix1D instance) throws LearnerException
    {
        return(0);
    }
    
    public Object []transform(Object obin) throws DataFlowException
    {
        Object []transOut;
        
        transOut = null;
        try
        {
            // If there's valid input
            if (obin != null)
            {
                // Classify the vector of the right type
                if (this.primitive) transOut = transformDouble((DoubleMatrix1D)obin);
                else                transOut = transformObject((ObjectMatrix1D)obin);
            }
        }
        catch(LearnerException ex) { throw new DataFlowException(ex); }
        catch(ConfigException ex)  { throw new DataFlowException(ex); }
        
        return(transOut);
    }   
    
    public Object []transformDouble(DoubleMatrix1D vecin) throws LearnerException, DataModelException
    {
        DoubleMatrix1D  esout;
        DoubleMatrix1D  inin;
        double        []conf;
        
        // Convert input vector to instance format
        inin  = this.learn.getInstanceVector(vecin);  // Discard the non-active attributes
        conf  = new double[1];
        
        // Estimate output value(s)
        esout = estimate(inin, conf);
        
        if (esout == null) return(null);
        else               return(new Object[]{esout});
    }
    
    public Object []transformObject(ObjectMatrix1D vecin) throws LearnerException, DataModelException
    {
        ObjectMatrix1D  esout;
        ObjectMatrix1D  inin;
        double        []conf;
        
        // Convert input vector to instance format
        inin  = this.learn.getInstanceVector(vecin);  // Discard the non-active attributes
        conf  = new double[1];
        
        // Estimate output value(s)
        esout = estimate(inin, conf);
        
        if (esout == null) return(null);
        else               return(new Object[]{esout});
    }
    
    // **********************************************************\
    // *                  Estimator Training                    *
    // **********************************************************/
    /**
     * Train this Estimator on the given set of training data
     * @param trainSet The set containing the training instances
     * @throws LearnerException If something goes wrong while training.
     */
    public void trainTransformation(Presenter trainSet) throws LearnerException
    {
        Learner learner;
        
        // Get the trainer and learner (often same object)
        learner = (Estimator)this;
        
        // Set the data model and the training instances
        learner.setTrainSet(trainSet);
        
        // Initialize the training and train.
        learner.initializeTraining();
        learner.train();
    }
    
    // **********************************************************\
    // *              Estimator Initialization                  *
    // **********************************************************/
    public void init() throws ConfigException
    {
        DataModel  dmin;
        
        // Make sure the input is compatible with this transformation's data requirements
        dmin = getSupplierDataModel(0);
        checkDataModelFit(0, dmin);
        
        // Install DataModels and initialize Classifier data-structures
        setDataModel(dmin);
        
        // Initialize estimator data
        initEstimator(dmin);
    }
    
    protected DataModel makeOutputDataModel(DataModel dmin) throws DataModelException
    {
        // Override to make a custum output datamodel
        return(null);
    }
    
    protected void setDataModel(DataModel dmin) throws ConfigException
    {
        DataModel dmout;
        
        // Make the corresponding output data model
        dmout = makeOutputDataModel(dmin);
        if (dmout == null) dmout = LearnerDataModels.getEstimatorDataModel(dmin);
        
        // Install DataModels
        setInputDataModel(0, dmin);
        setOutputDataModel(0,dmout);
        this.dataModel = dmin;
        this.learn     = dmin.getLearningProperty();
        if (dmin.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector))
        {
            this.primitive = true;
            this.dmdo      = (DataModelDouble)dataModel;
        }
        else
        {
            this.primitive = false;
            this.dmob      = (DataModelObject)dataModel;
        }
    }
    
    protected void initEstimator(DataModel dmin)
    {
        // Get instance indices and goal attribute from the datamodel.
        this.actind  = dmin.getActiveIndices();
        this.attgoal = dmin.getAttribute(dmin.getLearningProperty().getGoalIndex());
        
        // Also make fast vector manipulation buffers
        if (this.primitive) this.dallbuf = new double[dmin.getAttributeCount()];
        else                this.oallbuf = new Object[dmin.getAttributeCount()];
    }
    
    public String getInputName(int port)
    {
        if (port == 0) return("Estimator Input");
        else return(null);
    }
    
    public String getOutputName(int port)
    {
        if (port == 0) return("Estimator Output");
        else return(null);
    }
    
    public int getNumberOfInputs()  { return(1); }
    public int getNumberOfOutputs() { return(1); }
    
    // *********************************************************\
    // *             Typical Estimator DataModel Fit           *
    // *********************************************************/
    public void checkEstimatorDataModelFit(DataModel dmin, boolean doubleVec, boolean objectVec) throws DataModelException
    {
        Attribute   attnow;
        DataModelPropertyLearning learn;
        
        // First make sure the input vectors are from a supported type
        if (!doubleVec && dmin.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector))
            throw new DataModelException("Cannot handle double-based vectors as input");
        if (!objectVec && dmin.getVectorTypeProperty().equals(DataModelPropertyVectorType.objectVector))
            throw new DataModelException("Cannot handle object-based vectors as input");
        
        // Check if a estimator goal is present
        learn = dmin.getLearningProperty();
        if (!learn.getHasGoal()) 
            throw new DataModelException("DataModel with goal required.");
        
        // Check the goal type
        attnow = dmin.getAttribute(learn.getGoalIndex());
        if (attnow.getGoalType() != Attribute.GOAL_VALUE) 
            throw new DataModelException("DataModel with a classification goal required.");
    }
}