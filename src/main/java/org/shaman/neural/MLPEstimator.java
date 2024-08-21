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
package org.shaman.neural;


import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.shaman.dataflow.Persister;
import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelPropertyLearning;
import org.shaman.datamodel.DataModelPropertyVectorType;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.Estimator;
import org.shaman.learning.EstimatorTransformation;
import org.shaman.learning.Presenter;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Multi-Layer Perceptron Neural Network</h2>
 * MLP used for function estimation
 **/
// **********************************************************\
// *     Multi-Layer Perceptron Neural Network Estimator    *
// **********************************************************/
public class MLPEstimator extends EstimatorTransformation implements Estimator, Persister
{
    // The Multi-Layer Neural Network
    private MLP    mlp;
    
    // **********************************************************\
    // *          Estimation using a trained Neural Net         *
    // **********************************************************/
    public DoubleMatrix1D estimate(DoubleMatrix1D instance, double []conf) throws LearnerException
    {
        double  []inbuf;
        double  []outbuf;
        NeuralNet net;
        
        inbuf  = new double[this.mlp.getInputSize()];
        outbuf = new double[this.mlp.getOutputSize()];
        
        // Feed input data through network
        net = this.mlp.getNeuralNet();
        instance.toArray(inbuf);
        net.setInput(inbuf);
        net.updateSynchronous();
        net.getOutput(outbuf);
        
        // Return output of the Neural Network
        DoubleMatrix1D vout = DoubleFactory1D.dense.make(outbuf.length);
        vout.assign(outbuf);
        
        return(vout);
    }
    
    public  ObjectMatrix1D estimate(ObjectMatrix1D instance, double []conf) throws LearnerException
    {
        throw new LearnerException("Cannot estimate Object based data");
    }
    
    // **********************************************************\
    // *                Parameter Configuration                 *
    // **********************************************************/
    public void setMLP(MLP mlp)
    {
        this.mlp = mlp;
    }
    
    // **********************************************************\
    // *             Transformation Implementation              *
    // **********************************************************/
    public void init() throws ConfigException
    {
        // Standard Classifier initialization
        super.init();
    }
    
    public void checkDataModelFit(int port, DataModel dm) throws ConfigException
    {
        Attribute []actatt;
        int         i;
        DataModel   dmin;
        
        dmin = dm;
        
        // Check if the input is primitive
        if (!dmin.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector))
            throw new ConfigException("Primitive input data required.");
        
        // Check continuous data attribute property
        actatt = dmin.getActiveAttributes();
        for (i=0; i<actatt.length; i++)
        {
            if (!actatt[i].hasProperty(Attribute.PROPERTY_CONTINUOUS))
                throw new ConfigException("Continuous input data expected. Attribute '"+actatt[i].getName()+"' is not continuous.");
        }
        
        // Check if there is a classification goal.
        DataModelPropertyLearning learn;
        
        learn = dmin.getLearningProperty();
        if (!learn.getHasGoal())
            throw new ConfigException("Input requires a goal. Can't find one.");
        if (dmin.getAttribute(learn.getGoalIndex()).getGoalType() == Attribute.GOAL_CLASS)
            throw new ConfigException("Input contains a goal, but not an estimator goal.");
    }
    
    public void cleanUp() throws DataFlowException
    {
        this.mlp = null;
    }
    
    // **********************************************************\
    // *                    Learner Interface                   *
    // **********************************************************/
    public void initializeTraining() throws LearnerException
    {
        // Create a network according to the parameters, datamodels, etc...
        this.mlp.create();
        
        // Create all buffers for Back-Propagation training
        this.mlp.initBackPropagation();
    }
    
    public void train() throws LearnerException
    {
        // Train a number of epochs.
        this.mlp.backPropagation();
    }
    
    public Presenter getTrainSet()
    {
        return(this.mlp.getTrainSet());
    }
    
    public void setTrainSet(Presenter instances) throws LearnerException
    {
        try
        {
            // First make sure the data is in the right format.
            checkDataModelFit(0, instances.getDataModel());
            
            // Give the data to the MLP
            this.mlp.setTrainSet(instances);
        }
        catch(ConfigException ex) { throw new LearnerException(ex); }
    }
    
    public boolean isSupervised()
    {
        return(true);
    }
    
    // **********************************************************\
    // *             State Persistence Implementation           *
    // **********************************************************/
    public void loadState(ObjectInputStream oin) throws ConfigException
    {
        super.loadState(oin);
        if (this.mlp == null) this.mlp = new MLP();
        this.mlp.loadState(oin);
    }
    
    public void saveState(ObjectOutputStream oout) throws ConfigException
    {
        super.saveState(oout);
        this.mlp.saveState(oout);
    }
    
    // **********************************************************\
    // *                      Construction                      *
    // **********************************************************/
    public MLPEstimator()
    {
        super();
        name        = "MLPEstimator";
        description = "Multi-Layer Neural Network with 0, 1 or 2 fully connected hidden layers used for function estimation";
    }
}
