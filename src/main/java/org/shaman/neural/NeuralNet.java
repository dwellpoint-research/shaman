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

import org.shaman.dataflow.Persister;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.LearnerException;
import org.shaman.graph.GraphNode;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.jet.random.Uniform;


/**
 * <h2>Neural Network Base Class</h2>
 * Basic Structure for all kinds of neural networks.
 * Supports asynchronous update, layered synchronous update.
 * Can be used as a base-class for <br>
 * e.g. Multi-Layer Perceptron networks, Hopfield Nets, Self-Organizing Maps, etc... <br>
 * Has an array of neurons that can be grouped in layers and a state vector
 * that contains the input layer and the output of the neurons in the
 * hidden and output layers.
 * <br>
 * 
 * @author Johan Kaers
 * @version 2.0
 */

// **********************************************************\
// *                Neural Network Base Class               *
// **********************************************************/
public class NeuralNet implements Persister
{
    // Neuron type
    private int      neuronActivation;        // Activation Function
    private double []neuronParameters;        // and it's parameters
    
    // Network Graph
    private NetworkGraph graph;               // The graph describing neuron to neuron connections
    
    // The state of the neural network
    protected double  []state;        // All input, hidden and output values of this network
    protected double  []statesync;    // Buffer used for temporary storing output values in synchronous update.
    private   Neuron  []neuron;       // All neurons of this network.
    
    // Network layer positions and size
    private   int       inputPos;     // Input Layer begin position
    private   int       inputLen;     //             length
    private   int       outputPos;    // Output Layer begin position
    private   int       outputLen;    //             length
    private   int     []layPos;       // [i] = Layer i begin position
    private   int     []layLen;       // [i] = Layer i length
    
     // **********************************************************\
    // *             State Persistence Implementation           *
    // **********************************************************/
    public void loadState(ObjectInputStream oin) throws ConfigException
    {
        try
        {
            int i;
            
            this.neuronActivation = oin.readInt();
            this.neuronParameters = (double [])oin.readObject();
            this.state            = (double [])oin.readObject();
            this.statesync        = (double [])oin.readObject();
            for (i=0; i<this.neuron.length; i++)
                this.neuron[i].setWeights((double [])oin.readObject());
            this.inputPos  = oin.readInt();
            this.inputLen  = oin.readInt();
            this.outputPos = oin.readInt();
            this.outputLen = oin.readInt();
            this.layPos    = (int [])oin.readObject();
            this.layLen    = (int [])oin.readObject();
        }
        catch(IOException ex)            { throw new ConfigException(ex); }
        catch(ClassNotFoundException ex) { throw new ConfigException(ex); }
    }
    
    public void saveState(ObjectOutputStream oout) throws ConfigException
    {
        try
        {
            int  i;
            
            oout.writeInt(this.neuronActivation);
            oout.writeObject(this.neuronParameters);
            oout.writeObject(this.state);
            oout.writeObject(this.statesync);
            for (i=0; i<this.neuron.length; i++)
                oout.writeObject(this.neuron[i].getWeights());
            oout.writeInt(this.inputPos);
            oout.writeInt(this.inputLen);
            oout.writeInt(this.outputPos);
            oout.writeInt(this.outputLen);
            oout.writeObject(this.layPos);
            oout.writeObject(this.layLen);
        }
        catch(IOException ex) { throw new ConfigException(ex); }
    }
    
    // **********************************************************\
    // *           Network Construction using Graph             *
    // **********************************************************/
    public void create(NetworkGraph graph) throws LearnerException
    {
        int         i;
        Neuron    []neuron;
        GraphNode []node;
        
        // Create the neurons
        node       = graph.getNodes();
        neuron     = new Neuron[node.length];
        for(i=0; i<neuron.length; i++) 
            neuron[i] = new Neuron(this, this.neuronActivation, this.neuronParameters);
        
        int [][][]neucon;
        int       j, pos;
        
        // Convert Graph structure to neuron indices and connect the neurons accordingly
        pos         = 0;
        neucon      = graph.getNeuronInputs();
        this.layLen = new int[neucon.length];
        this.layPos = new int[neucon.length];
        for (i=0; i<neucon.length; i++)
        {
            // Remember position and length of the input and output layer (can be the same...)
            if (i == 0)
            {
                this.inputPos = pos;
                this.inputLen = neucon[i].length;
            }
            if (i == neucon.length-1)
            {
                this.outputPos = pos;
                this.outputLen = neucon[i].length;
            }
            
            // Remember position and length of the layers
            this.layPos[i] = pos;
            this.layLen[i] = neucon[i].length;
            
            // For all the neurons in the current layer
            for (j=0; j<neucon[i].length; j++)
            {
                // Connect them to their input neurons 
                neuron[pos].setConnections(neucon[i][j], pos);
                pos++;
            }
        }
        
        
        int    maxind;
        Neuron neunow;
        
        // Find the maximum index in the state array of any neuron.
        maxind = 0;
        for (i=0; i<neuron.length; i++)
        {
            neunow = neuron[i];
            for (j=0; j<neunow.input.length; j++) if (neunow.input[j] > maxind) maxind = neunow.input[j];
            if (neunow.output > maxind) maxind = neunow.output;
        }
        
        // Make a state array that is just long enough.
        this.state     = new double[maxind+1];
        this.statesync = new double[maxind+1];
        
        // Remember neurons and the graph structure
        this.neuron = neuron;
        this.graph  = graph;
    }
    
    /**
     * Set the kind of neuron and it's parameters.
     * @param _neuronActivation Type of activation function to use in the neurons of this network.
     * @param _neuronParameters Parameters of the activation function used in the neurons of this network.
     */
    public void setNeuronType(int _neuronActivation, double []_neuronParameters)
    {
        this.neuronActivation = _neuronActivation;
        this.neuronParameters = _neuronParameters;
    }
    
    // **********************************************************\
    // *                     Network construction               *
    // **********************************************************/
    public NetworkGraph getGraph() { return(this.graph); }
    
    public int getNumberOfNeurons() { return(this.neuron.length); }
    
    public int getLayerBegin(int i) { return(this.layPos[i]); }
    
    public int []getLayerSizes() { return(this.layLen); }
    
    /**
     * Get the neuron at the specified index.
     * @param ind The index of the neurons
     * @return The neuron at the specified index.
     */
    public Neuron getNeuron(int ind)
    {
        return(this.neuron[ind]);
    }
    
    /**
     * Get the size of the input vector of this network.
     * This value is equal to the number of active attributes in the input datamodel.
     * @param dm The dataModel of the instances the network processes.
     * @return The number of active attributes in the given datamodel.
     * @throws LearnerException If there are no active attributes and hence no input instances possible.
     */
    protected int numberOfInputs(DataModel dm) throws LearnerException
    {
        int ni;
        
        ni = dm.getNumberOfActiveAttributes();
        if (ni == 0) throw new LearnerException("The MLP's DataModel has no active attributes.");
        
        return(ni);
    }
    
    // **********************************************************\
    // *              Neural Network activation                 *
    // **********************************************************/
    /**
     * Set the input values of this network.
     * Copy the value of the given vector in this network's state vector starting at the specified position.
     * @param in The vector containing the input values for this network.
     * @param indbeg The begin index in the state-vector.
     */
    public void setInput(double []in)
    {
        int i;
        
        // Set the given input values in the state array as input for the network
        for (i=this.inputPos; i<this.inputPos+this.inputLen; i++)
        {
            this.state[i]     = in[i-this.inputPos];
            this.statesync[i] = in[i-this.inputPos];
        }
    }
    
    /**
     * Get the current output values of this network.
     * These values are the outputs of the neurons in the layer(s) with highest 'order'.
     * @param out The array in which to copy the output values.
     */
    public void getOutput(double []out)
    {
        int     i;
        int     pos;
        
        // Copy the output values into the given array
        pos = 0;
        for (i=this.outputPos; i<this.outputPos+this.outputLen; i++) 
            out[pos++] = this.state[this.neuron[i].output];
    }
    
    /**
     * Copy the current output of the given neuron layer into the given array.
     * @param lay The number of the layer
     * @param out The array to copy the ouput into.
     */
    public void getOutput(int layer, double []out)
    {
        int i, beg, len, pos;
        
        pos = 0;
        beg = this.layPos[layer];
        len = this.layLen[layer];
        for (i=beg; i<beg+len; i++) out[pos++] = this.state[neuron[i].output];
    }
    
    // **********************************************************\
    // *             Asynchronous network dynamics              *
    // **********************************************************/
    public void updateAsynchronous(int count)
    {
        int   i, ranpos;
        
        // Update the state of the network a number of times by activating a random neuron
        for(i=0; i<count; i++)
        {
            ranpos = Uniform.staticNextIntFromTo(0, this.neuron.length-1);
            this.neuron[ranpos].activateAsynchronous();
        }
    }
    
    public DoubleMatrix1D getStateVector()
    {
        return(DoubleFactory1D.dense.make(this.state));
    }
    
    // **********************************************************\
    // *       Synchronous feed-forward network dynamics        *
    // **********************************************************/
    /**
     * Do a complete forward synchronous updating step of the network.
     * Implements the layered feed-forward synchronous behavior of e.g. MLPs, SOMs.
     */
    public void updateSynchronous()
    {
        int i;
        
        for (i=1; i<this.layLen.length; i++)
        {
            updateLayer(i);
            synchronize(i);
        }
    }
    
    /**
     * Synchronize the given layer of neurons.
     * Copy the current output of the neurons in the layer to the networks's state vector.
     * @param lay The layer to synchronize.
     */
    private void synchronize(int layer)
    {
        int i, oind, pos, len;
        
        // Copy the ouput of neuron of layer i to the state buffer.
        pos = this.layPos[layer];
        len = this.layLen[layer];
        for (i=pos; i<pos+len; i++)
        {
            oind             = neuron[i].output;
            this.state[oind] = this.statesync[oind];
        }
    }
    
    /**
     * Update the given layer.
     * Update (evaluate the activation function) all neurons of the given layer.
     * @param lay The layer to update.
     */
    private void updateLayer(int layer)
    {
        int i, pos, len;
        
        // Activate all neuron in the layer synchronously
        pos = this.layPos[layer];
        len = this.layLen[layer];
        for (i=pos; i<pos+len; i++) this.neuron[i].activateSynchronous();
    }
    
    // **********************************************************\
    // *                      Construction                      *
    // **********************************************************/
    public NeuralNet()
    {
        this.neuron = new Neuron[0];
        this.graph  = null;
    }
}