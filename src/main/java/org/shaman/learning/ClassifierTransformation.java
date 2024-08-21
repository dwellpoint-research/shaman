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


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

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

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Classifier Transformation</h2>
 * Base-class for machine learning Transformations
 * that implement some kind of Classifier.
 */

// *********************************************************\
// *          Classifier Transformation Base-Class         *
// *********************************************************/
public abstract class ClassifierTransformation extends Transformation implements Classifier
{
    // General Classifier Parameter
    protected int              classifierOutput;       // Type of classifier output model
    
    // Machine Learning : Traning data and description
    protected Presenter        trainData;              // Training set
    protected DataModel        dataModel;              // Training data description
    protected DataModelDouble  dmdo;                   //     DataModel if data is double-based
    protected DataModelObject  dmob;                   //     DataModel if data is object-based

    // Typical Classifier data-structures
    protected boolean                   primitive;     // Data is double-based?
    protected DataModelPropertyLearning learn;         // Machine Learning bridge of the DataModel
    protected int                     []actind;        // Indices of the instance attributes
    protected Attribute                 attgoal;       // The goal attributes

    // *********************************************************\
    // *          Typical Classifier Implementation            *
    // *********************************************************/
    public abstract int classify(ObjectMatrix1D instance, double []confidence) throws LearnerException;

    public abstract int classify(DoubleMatrix1D instance, double []confidence) throws LearnerException;
  
    public int classify(DoubleMatrix1D instance) throws LearnerException
    {
        return(classify(instance, null));
    }

    public int classify(ObjectMatrix1D instance) throws LearnerException
    {
        return(classify(instance, null));
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
        catch(LearnerException ex)       { throw new DataFlowException(ex); }
        catch(ConfigException ex) { throw new DataFlowException(ex); }
        
        return(transOut);
    }   

    public Object []transformDouble(DoubleMatrix1D vecin) throws LearnerException, DataModelException
    {
        int             clout;
        DoubleMatrix1D  inin;
        DoubleMatrix1D  out;
        double        []conf;

        // Make a copy of all attributes
        inin  = this.learn.getInstanceVector(vecin);  // Discard the non-active attributes
        conf  = new double[this.attgoal.getNumberOfGoalClasses()];

        // Classify the data
        clout = classify(inin, conf);
        out   = makeClassifierOutput(clout, conf);

        if (out == null) return(null);
        else             return(new Object[]{out});
    }
    
    public Object []transformObject(ObjectMatrix1D vecin) throws LearnerException, DataModelException
    {
        int             clout;
        ObjectMatrix1D  inin;
        DoubleMatrix1D  out;
        double        []conf;

        // Make a copy of all attributes
        inin  = this.learn.getInstanceVector(vecin);  // Discard the non-active attributes
        conf  = new double[this.attgoal.getNumberOfGoalClasses()];

        // Classify the data
        clout = classify(inin, conf);
        out   = makeClassifierOutput(clout, conf);

        if (out == null) return(null);
        else             return(new Object[]{out});
    }
    
    protected DoubleMatrix1D makeClassifierOutput(int clout, double []conf)
    {
        DoubleMatrix1D out;
        int            i;
        
        // If classification succeeded...
        out = null;
        if (clout != -1) 
        {
            // Make the right kind of output
            if      (this.classifierOutput == Classifier.OUT_CLASS)                       // Class
                out = DoubleFactory1D.dense.make(new double[]{clout});
            else if (this.classifierOutput == Classifier.OUT_CLASS_AND_CONFIDENCE)        // Class + Class Confidence
                out = DoubleFactory1D.dense.make(new double[]{clout, conf[clout]});
            else if (this.classifierOutput == Classifier.OUT_CLASS_AND_CONFIDENCE_VECTOR) // Class + Confidence Vector
            {
                out = DoubleFactory1D.dense.make(1+conf.length);
                out.setQuick(0, clout);
                for (i=0; i<conf.length; i++) out.setQuick(i+1, conf[i]);
            }
        }
        
        return(out);
    }

    // **********************************************************\
    // *               Classifier Configuration                 *
    // **********************************************************/
    public void setClassifierOutput(int classifierOutput)
    {
        this.classifierOutput = classifierOutput;
    }
    
    public int getClassifierOutput()
    {
        return(this.classifierOutput);
    }
    
    // **********************************************************\
    // *                  Estimator Training                    *
    // **********************************************************/
    /**
     * Train this Classifier on the given set of training data
     * @param trainSet The set containing the training instances
     * @throws LearnerException If something goes wrong while training.
     */
    public void trainTransformation(Presenter trainSet) throws LearnerException
    {
        Learner learner;
        
        // Get the trainer and learner (often same object)
        learner = (Classifier)this;
            
        // Set the data model and the training instances
        learner.setTrainSet(trainSet);
            
        // Initialize the training and train.
        learner.initializeTraining();
        learner.train();
    }

    // **********************************************************\
    // *             Classifier Initialization                  *
    // **********************************************************/
    public void init() throws ConfigException
    {
        DataModel  dmin;
     
        // Make sure the input is compatible with this transformation's data requirements
        dmin = getSupplierDataModel(0);
        checkDataModelFit(0, dmin);

        // Install DataModels and initialize Classifier data-structures
        setDataModel(dmin);
        
        // Initialize classifier data
        initClassifier(dmin);
    }
    
    protected void setDataModel(DataModel dmin) throws ConfigException
    {
        DataModel dmout;
        
        // Make the corresponding output data model
        dmout = LearnerDataModels.getClassifierDataModel(dmin, this.classifierOutput);
        
        // Install DataModels
        setInputDataModel(0,dmin);
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
    
    protected void initClassifier(DataModel dmin)
    {
        // Get instance indices and goal attribute from the datamodel.
        this.actind  = dmin.getActiveIndices();
        this.attgoal = dmin.getAttribute(dmin.getLearningProperty().getGoalIndex());
    }
    
    public String getInputName(int port)
    {
        if (port == 0) return("Classifier Input");
        else return(null);
    }

    public String getOutputName(int port)
    {
        if (port == 0) return("Classified Output");
        else return(null);
    }
   
    public int getNumberOfInputs()  { return(1); }
    public int getNumberOfOutputs() { return(1); }

    // *********************************************************\
    // *            Typical Classifier DataModel Fit           *
    // *********************************************************/
    public void checkClassifierDataModelFit(DataModel dmin, boolean categoricalInput, boolean doubleVec, boolean objectVec) throws DataModelException
    {
        Attribute []actatt;
        int         i;
        Attribute   attnow;
        DataModelPropertyLearning learn;
        
        // First make sure the input vectors are from a supported type
        if (!doubleVec && dmin.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector))
            throw new DataModelException("Cannot handle double-based vectors as input");
        if (!objectVec && dmin.getVectorTypeProperty().equals(DataModelPropertyVectorType.objectVector))
            throw new DataModelException("Cannot handle object-based vectors as input");
        
        // Check if a classification goal is present
        learn = dmin.getLearningProperty();
        if (!learn.getHasGoal()) 
            throw new DataModelException("DataModel with goal required.");

        // Check the goal type
        attnow = dmin.getAttribute(learn.getGoalIndex());
        if (attnow.getGoalType() != Attribute.GOAL_CLASS) 
             throw new DataModelException("DataModel with a classification goal required.");

        // If categorical input is needed make sure the instance attributes are categorical
        if (categoricalInput)
        {
            // Check attribute properties
            actatt = (Attribute [])dmin.getActiveAttributes();
            for (i=0; i<actatt.length; i++)
            {
                if (!actatt[i].hasProperty(Attribute.PROPERTY_CATEGORICAL)) 
                   throw new DataModelException("Categorical input data expected. Attribute '"+actatt[i].getName()+"' is not categorical.");
            }
        }
    }
    
    // *********************************************************\
    // *            Persister Methods for Sub-classes          *
    // *********************************************************/
    protected void loadState(ObjectInputStream oin) throws ConfigException
    {
        try
        {
            super.loadState(oin);
            this.classifierOutput = oin.readInt();
        }
        catch(IOException ex)            { throw new ConfigException(ex); }
    }
    
    protected void saveState(ObjectOutputStream oout) throws ConfigException
    {
        try
        {
            super.saveState(oout);
            oout.writeInt(this.classifierOutput);
        }
        catch(IOException ex) { throw new ConfigException(ex); }
    }
}