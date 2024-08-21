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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.shaman.dataflow.Persister;
import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelPropertyLearning;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;
import org.shaman.graph.GraphException;
import org.shaman.learning.BatchPresenter;
import org.shaman.learning.InstanceBatch;
import org.shaman.learning.Presenter;


/**
 * <h2>Multi-Layer Perceptron</h2>
 * A Neural Network containing 1,2 or 3 layers of fully connected neurons.
 * Can be used for classification and function estimation tasks.
 * Support a number of output encoding schemes for classification
 * (one-of-n, error-correcting-codes) and regression (estimation).
 * <br>
 */
// **********************************************************\
// *        Multi-Layer Perceptron Neural Network           *
// **********************************************************/
public class MLP implements Persister
{
    private static Log log = LogFactory.getLog(MLP.class);
    
    // Type of output encoding
    /** Use 1 output neuron's value as output. Used in supervised function estimation (regression) problems.*/
    public static final int OUTPUT_REGRESSION            = 0;  // Approximate a function f: R^n -> R
    /** Use 1-of-n output encoding to determine the output of a classification problem */
    public static final int OUTPUT_ONE_OF_N              = 1;  // Classify using 1-of-n output encoding
    /** Use an error-correcting-code output encoding to find the output of a classification problem */
    public static final int OUTPUT_ERROR_CORRECTING_CODE = 2;  // Classify using an error correcting code output encoding
    
    // MLP Structure Parameters
    private int      neuronActivation;        // Activation Function
    private double []neuronParameters;        // and it's parameters
    private int      layer1Size;              // Number of neurons in Hidden Layer 1
    private int      layer2Size;              // Number of neurons in Hidden Layer 2
    private int      outputType;              // Type of output.
    
    // Backpropagation training parameters
    private double   eta;                     // Learning Rate
    private double   alpha;                   // Momentum Parameters
    private boolean  momentum;                // Use momentum term in weight update
    private int      numEpochs;               // Numbers of epochs to train.
    
    // Batch Presenter Parameters
    private int    batchType;
    private int    balance;
    private double sampleFraction;
    
    // Training Data and it's DataModel
    private BatchPresenter   batch;
    private DataModelDouble  dataModel;
    
    // ------------
    private NeuralNet net;                   // The Neural Network data-structures
    private int     []layerSize;             // Number of neurons in the layers
    private int        inSize, outSize;      // Number of inputs, number of outputs
    
    // **********************************************************\
    // *               Training Set Configuration               *
    // **********************************************************/
    public void setTrainSet(Presenter instances) throws LearnerException
    {
        // And the presenter supports batches
        if (!(instances instanceof BatchPresenter))
        {
            this.batch = makeBatchTrainSet(instances);
        }
        
        // Set the datamodel to the given data's
        this.dataModel = (DataModelDouble)this.batch.getDataModel();
    }
    
    public void setDataModel(DataModelDouble dataModel)
    {
        this.dataModel = dataModel;
    }
    
    public Presenter getTrainSet() { return(this.batch); }
    
    private BatchPresenter makeBatchTrainSet(Presenter im) throws LearnerException
    {
        // Make a batch TrainSet based on the supplied presenter with the specified parameters
        InstanceBatch     ib;
        
        ib = new InstanceBatch();
        ib.create(im, this.batchType, this.balance, this.sampleFraction);
        
        return(ib);
    }
    
    // **********************************************************\
    // *                Parameter Configuration                 *
    // **********************************************************/
    /**
     * Set the parameters used to create a BatchPresenter if a non-batch presenter
     * is given as trainset.
     * @param _batchType Type (reread/recycle) of batch presenter to make.
     * @param _balance Kind of goal balancing to do.
     * @param _sampleFraction Amount of sub-sampling to do.
     */
    public void setBatchParameters(int _batchType, int _balance, double _sampleFraction)
    {
        batchType      = _batchType;
        balance        = _balance;
        sampleFraction = _sampleFraction;
    }
    
    /**
     * Set the topology parameters of the MLP.
     * @param _layer1Size Number of neurons in hidden layer 1
     * @param _layer2Size Number of neurons in hidden layer 2
     * @param _outputType Kind of output encoding to use in the output layer.
     * @see #OUTPUT_REGRESSION
     * @see #OUTPUT_ONE_OF_N
     * @see #OUTPUT_ERROR_CORRECTING_CODE
     */
    public void setNetworkParameters(int _layer1Size, int _layer2Size, int _outputType)
    {
        layer1Size = _layer1Size;
        layer2Size = _layer2Size;
        outputType = _outputType;
    }
    
    public void setNetworkLayer1Size(int _layer1Size)
    {
        layer1Size = _layer1Size;
    }
    
    public void setNetworkLayer2Size(int _layer2Size)
    {
        layer2Size = _layer2Size;
    }
    
    public int getNetworkLayer1Size() { return(layer1Size); }
    public int getNetworkLayer2Size() { return(layer2Size); }
    
    /**
     * Set the parameter of the Backpropagation training algorithms.
     * @param _eta Learning rate
     * @param _momentum If <code>true</code> a momentum term is used in weight updates.
     * @param _alpha Momentum factor
     * @param _numEpochs Number of epochs to train
     */
    public void setBackPropagationParameters(double _eta, boolean _momentum, double _alpha, int _numEpochs)
    {
        eta       = _eta;        // Learning rate
        momentum  = _momentum;   // Use a momentum term in weight update
        alpha     = _alpha;      // Momentum parameter
        numEpochs = _numEpochs;
    }
    
    public void setBackPropagationNumberOfEpochs(int _numEpochs)
    {
        numEpochs = _numEpochs;
    }
    
    public int getBackPropagationNumberOfEpochs()
    {
        return(numEpochs);
    }
    
    /**
     * Set the kind of neuron and it's parameters.
     * @param _neuronActivation Type of activation function to use in the neurons of this network.
     * @param _neuronParameters Parameters of the activation function used in the neurons of this network.
     */
    public void setNeuronType(int _neuronActivation, double []_neuronParameters)
    {
        neuronActivation = _neuronActivation;
        neuronParameters = _neuronParameters;
    }
    
    public int getNeuronTypeActivation()
    {
        return(neuronActivation);
    }
    
    // **********************************************************\
    // *                 Backpropagation Learning               *
    // **********************************************************/
    private ActivationFunction fact;        // Activation function of the neurons
    private double  [][]layout;             // The output of the (hidden and output) layers
    private double  [][]delta;              // The back-propagated error
    private double  [][][]weight;           // The weights of the neurons
    private double  [][][]weightMomentum;   // The momentum term of the weights
    private double  []weibuf;               // Buffers for the algoritm
    private double  []inbuf;
    private double  []outbuf;
    private double  []goalbuf;
    private double  [][]conftrain;          // Confusion matrix of the training
    private int     []confcount;
    
    protected void initBackPropagation() throws LearnerException
    {
        int i,j,k,laypos;
        
        // Initialize Various Buffers and Parameters for Error Back Propagation Learning
        fact           = this.net.getNeuron(0).getActivationFunction();
        inbuf          = new double[getInputSize()];
        outbuf         = new double[getOutputSize()];
        goalbuf        = new double[getOutputSize()];
        layout         = new double[layerSize.length][];
        delta          = new double[layerSize.length][];
        weight         = new double[layerSize.length][][];
        if (momentum) weightMomentum = new double[layerSize.length][][];
        for (i=0; i<layerSize.length; i++)
        {
            layout[i]         = new double[layerSize[i]];
            delta[i]          = new double[layerSize[i]];
            weight[i]         = new double[layerSize[i]][];
            if (momentum) weightMomentum[i] = new double[layerSize[i]][];
            if (i > 0)
            {
                laypos = this.net.getLayerBegin(i);
                for (j=0; j<layerSize[i]; j++)
                {
                    weibuf               = this.net.getNeuron(laypos+j).getWeights();
                    weight[i][j]         = weibuf;
                    if (momentum)
                    {
                        weightMomentum[i][j] = new double[weibuf.length];
                        for (k=0; k<weightMomentum[i][j].length; k++) weightMomentum[i][j][k] = 0;
                    }
                }
            }
        }
        
        try
        {
            // Make a confusion matrix for classification goals
            Attribute attgoal = dataModel.getAttribute(dataModel.getLearningProperty().getGoalIndex());
            
            if (attgoal.getGoalType() == Attribute.GOAL_CLASS)
            {
                confcount = new int[attgoal.getNumberOfGoalClasses()];
                conftrain = new double[attgoal.getNumberOfGoalClasses()][];
                for (i=0; i<conftrain.length; i++) conftrain[i] = new double[attgoal.getNumberOfGoalClasses()];
            }
        }
        catch(DataModelException ex) { throw new LearnerException("Cannot create confusion matrix for training."); }
    }
    
    public void backPropagation() throws LearnerException
    {
        int     e;
        double  error;
        
        try
        {
            error  = 0;
            for (e=0; e<this.numEpochs; e++)
            {
                // Train 1 BackPropagation Epoch
                error    = backPropagationEpoch();
                
                // Report the current error once in a while
                if (e%20== 0)  log.debug("\tEpoch "+e+"/"+this.numEpochs+". Error = "+error);
            }
            log.debug("Error after training "+error);
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
    }
    
    public double backPropagationEpoch() throws LearnerException, DataModelException
    {
        int     i, j, k, ic;
        int     numIns;
        int     outind;
        double  lo, sdw;
        double  iweight;
        double  error, errnow;
        double  match;
        double  dij, dwk;
        
        // Start new batch
        batch.nextBatch();
        numIns = batch.getNumberOfInstances();
        
        // Incremental Error Back Propagation Learning Rule
        error = 0; match = 0;
        
        // Reset confusion during training of classification network
        if ((outputType == OUTPUT_ONE_OF_N) || (outputType == OUTPUT_ERROR_CORRECTING_CODE))
        {
            for (i=0; i<conftrain.length; i++) for (j=0; j<conftrain[i].length; j++) conftrain[i][j] = 0;
            for (i=0; i<confcount.length; i++) confcount[i] = 0;
        }
        
        for (ic = 0; ic<numIns; ic++)
        {
            // Feed forward the instance through the network
            batch.getInstance(ic).toArray(inbuf);
            toDesiredOutput(batch.getGoal(ic), goalbuf);
            this.net.setInput(inbuf);
            this.net.updateSynchronous();
            this.net.getOutput(outbuf);
            if (checkMatch(outbuf, goalbuf)) match++;
            
            // Take into account this instance's weight
            iweight = batch.getWeight(ic);
            
            // Get all the layer's output
            for (i=0; i<inbuf.length; i++)     layout[0][i] = inbuf[i];         // The Input 'layer'
            for (i=1; i<layerSize.length; i++)
            {
                this.net.getOutput(i, layout[i]); // The hidden and output ones
            }
            
            // Propagate the error back through the connections. BACK PROPAGATION.
            outind = layerSize.length-1;
            for (j=0; j<layerSize[outind]; j++)
            {
                delta[outind][j] = fact.actDer(layout[outind][j]) * (goalbuf[j]-outbuf[j]);
            }
            for (i=layerSize.length-2; i>0; i--)
            {
                for (j=0; j<layerSize[i]; j++)
                {
                    lo  =  fact.actDer(layout[i][j]);   // Derivative of the layer's output
                    sdw = 0; for (k=0; k<layerSize[i+1]; k++) sdw += weight[i+1][k][j]*delta[i+1][k];
                    delta[i][j] = lo*sdw;
                }
            }
            
            // Adjust the weights
            for (i=1; i<layerSize.length; i++)
            {
                for (j=0; j<layerSize[i]; j++)
                {
                    dij = delta[i][j];
                    for (k=0; k<layerSize[i-1]; k++)
                    {
                        // Calculate delta. Take into account instance weight by muliplying the learning rate with it.
                        dwk = (eta*iweight) * dij * layout[i-1][k];
                        
                        // Add momentum term if enabled
                        if (momentum)
                        {
                            dwk                    += alpha*weightMomentum[i][j][k];
                            weightMomentum[i][j][k] = dwk;
                        }
                        // Adjust weights
                        weight[i][j][k] += dwk;
                    }
                }
            }
            
            // Update the error.
            errnow = 0;
            for (i=0; i<outbuf.length; i++) errnow += (goalbuf[i]-outbuf[i])*(goalbuf[i]-outbuf[i]);
            errnow /= (outbuf.length*2);
            error += errnow;
            
            if ((ic > 0) && (ic%100 == 0)) match = 0;
        }
        
        // Make confusion during training
        if ((outputType == OUTPUT_ONE_OF_N) || (outputType == OUTPUT_ERROR_CORRECTING_CODE))
        {
            for(i=0; i<conftrain.length; i++)
            {
                if (confcount[i] != 0)
                    for(j=0; j<conftrain[i].length; j++) conftrain[i][j] /= confcount[i];
            }
        }
        
        return(error);
    }
    
    public double[][] getConfusionOfLastEpoch()
    {
        return(conftrain);
    }
    
    private void toDesiredOutput(double goal, double []out) throws LearnerException, DataModelException
    {
        int    i;
        int    igoal;
        double negAct, posAct;
        
        if      (outputType == OUTPUT_REGRESSION) out[0] = goal;
        else
        {
            posAct = 0; negAct = 0;
            if      (neuronActivation == Neuron.ACTIVATION_SIGMOID_EXP)  { negAct =  0; posAct = 1; }
            else if (neuronActivation == Neuron.ACTIVATION_SIGMOID_TANH) { negAct = -1; posAct = 1; }
            
            igoal = this.dataModel.getAttributeDouble(this.dataModel.getLearningProperty().getGoalIndex()).getGoalClass(goal);
            if (outputType == OUTPUT_ONE_OF_N)
            {
                for (i=0; i<out.length; i++)
                {
                    if (i == igoal) out[i] = posAct;
                    else            out[i] = negAct;
                }
            }
            else if (outputType == OUTPUT_ERROR_CORRECTING_CODE)
            {
                // ADD ADD
            }
        }
    }
    
    private boolean checkMatch(double []out, double []goal)
    {
        int     i;
        boolean match;
        int     gpos, opos;
        double  omax;
        
        match = false;
        if (outputType == OUTPUT_ONE_OF_N)
        {
            // Match if maximum of outputs agrees with the one in the goal vector
            gpos = -1; opos = -1;
            for (i=0; i<goal.length; i++) if (goal[i] == 1.00) gpos = i;
            omax = Double.NEGATIVE_INFINITY;
            for (i=0; i<out.length; i++)
            {
                if (out[i] > omax) { omax = out[i]; opos = i; }
            }
            if (gpos == opos) match = true;
            
            if (opos != -1)
            {
                // Update confusion
                conftrain[gpos][opos]++;
                confcount[gpos]++;
            }
        }
        else if (outputType == OUTPUT_ERROR_CORRECTING_CODE)
        {
            // ADD ADD
        }
        else match = true;
        
        return(match);
    }
    
    // **********************************************************\
    // *                MLP Specific Data Access                *
    // **********************************************************/
    /**
     * Get the size of all layers.
     * @return An array with the size of the input, hidden and output layers.
     */
    public int []getLayerSizes() { return(layerSize); }
    
    /**
     * Get the size of the input layer.
     * @return The size of the input layer.
     */
    public int getInputSize()    { return(layerSize[0]); }
    
    /**
     * Get the size of the output layer.
     * @return The size of the output layer.
     */
    public int getOutputSize()   { return(layerSize[layerSize.length-1]); }
    
    /**
     * Give the Neural Network of this MLP
     */
    public NeuralNet getNeuralNet() { return(this.net); }
    
    // **********************************************************\
    // *                   MLP Construction                     *
    // **********************************************************/
    public void create() throws LearnerException
    {
        NetworkGraph mlpgraph;
        NeuralNet    net;
        
        try
        {
            net      = new NeuralNet();
            
            // Make a fully connected multi-layer neural net graph with the right number of neurons in the layers
            if (this.inSize  == -1) this.inSize  = net.numberOfInputs(this.dataModel);
            if (this.outSize == -1) this.outSize = numberOfOutputs(this.dataModel);
            mlpgraph     = NetworkGraphFactory.makeMLP(this.inSize, this.layer1Size, this.layer2Size, this.outSize);
            
            // Convert graph to real network of neurons
            net.setNeuronType(this.neuronActivation, this.neuronParameters);
            net.create(mlpgraph);
            
            // Remember layer-sizes
            this.layerSize = net.getLayerSizes();
            
            int    i, numneu;
            Neuron neunow;
            
            // Initialize (non-input) neuron weights
            numneu = net.getNumberOfNeurons();
            for (i=inSize; i<numneu; i++)
            {
                neunow = net.getNeuron(i);
                neunow.setActivation(this.neuronActivation);
                neunow.setActivationParameters(this.neuronParameters);
                neunow.initWeights(Neuron.WEIGHT_INIT_RANDOM, 0.1);
            }
            
            // All done.
            this.net = net;
        }
        catch(GraphException ex) { throw new LearnerException(ex); }
    }
    
    private int numberOfOutputs(DataModel dm) throws LearnerException
    {
        Attribute attgoal;
        int       no;
        DataModelPropertyLearning learn;
        
        try
        {
            learn = dm.getLearningProperty();
            if (!learn.getHasGoal()) throw new LearnerException("The MLP's DataModel has no goal attribute.");
            
            attgoal = dm.getAttribute(learn.getGoalIndex());
            no      = 1;
            if ((outputType == OUTPUT_REGRESSION) && (attgoal.getGoalType() == Attribute.GOAL_VALUE))
            {
                // Use a single neuron output layer for regression problems.
                no = 1;
            }
            else if (outputType == OUTPUT_ONE_OF_N)
            {
                // Classification MLP. Check if the DataModel agress with this.
                if (attgoal.getGoalType() == Attribute.GOAL_VALUE) throw new LearnerException("Cannot build one-of-n classification MLP on a regression goal attribute.");
                
                no = attgoal.getNumberOfGoalClasses();
            }
            else if (outputType == OUTPUT_ERROR_CORRECTING_CODE)
            {
                if (attgoal.getGoalType() == Attribute.GOAL_VALUE) throw new LearnerException("Cannot build error-correcting-code classification MLP on a regression goal attribute.");
                
                //no = attgoal.getNumberOfGoalClasses();
            }
            else throw new LearnerException("Cannot determine number of outputs for unknown output type");
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
        
        return(no);
    }
    
    // **********************************************************\
    // *             State Persistence Implementation           *
    // **********************************************************/
    public void loadState(ObjectInputStream oin) throws ConfigException
    {
        try
        {
            // MLP Topology and Training parameters
            this.neuronActivation = oin.readInt();
            this.neuronParameters = (double [])oin.readObject();
            this.inSize           = oin.readInt();
            this.layer1Size       = oin.readInt();
            this.layer2Size       = oin.readInt();
            this.outSize          = oin.readInt();
            this.outputType       = oin.readInt();
            this.eta              = oin.readDouble();
            this.alpha            = oin.readDouble();
            this.momentum         = oin.readBoolean();
            this.numEpochs        = oin.readInt();
            this.batchType        = oin.readInt();
            this.balance          = oin.readInt();
            this.sampleFraction   = oin.readDouble();
            create();
            this.net.loadState(oin);
        }
        catch(LearnerException ex)       { throw new ConfigException(ex); }
        catch(IOException ex)            { throw new ConfigException(ex); }
        catch(ClassNotFoundException ex) { throw new ConfigException(ex); }
    }
    
    public void saveState(ObjectOutputStream oout) throws ConfigException
    {
        try
        {
            oout.writeInt(this.neuronActivation);
            oout.writeObject(this.neuronParameters);
            oout.writeInt(this.inSize);
            oout.writeInt(this.layer1Size);
            oout.writeInt(this.layer2Size);
            oout.writeInt(this.outSize);
            oout.writeInt(this.outputType);
            oout.writeDouble(this.eta);
            oout.writeDouble(this.alpha);
            oout.writeBoolean(this.momentum);
            oout.writeInt(this.numEpochs);
            oout.writeInt(this.batchType);
            oout.writeInt(this.balance);
            oout.writeDouble(this.sampleFraction);
            this.net.saveState(oout);
        }
        catch(IOException ex) { throw new ConfigException(ex); }
    }
    
    public MLP()
    {
        this.inSize  = -1;
        this.outSize = -1;
    }
}
