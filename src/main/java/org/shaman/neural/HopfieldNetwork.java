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
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.LearnerException;
import org.shaman.graph.Graph;
import org.shaman.graph.GraphException;
import org.shaman.learning.Classifier;
import org.shaman.learning.ClassifierTransformation;
import org.shaman.learning.Presenter;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Hopfield Network/h2>
 * Associative memory style classification neural network.
 */

// **********************************************************\
// *                   Hopfield Network                     *
// **********************************************************/
public class HopfieldNetwork extends ClassifierTransformation implements Classifier, Persister
{
    private Graph     graph;            // The graph describing neuron connections. If 'null', a fully connected topology is assumed
    private int       numneu;           // The number of neurons when graph is 'null'
    private NeuralNet net;              // Neural network logic
    private DoubleMatrix1D []pattern;   // Patterns to recognize
    private int            []goalClass; // Patterns goal class membership (normally 0...#patterns-1)
    
    // **********************************************************\
    // *                          Training                      *
    // **********************************************************/
    private void trainHopfield() throws LearnerException
    {
        int    neucount, traincount;
        int    i,j,k,n;
        double wij;
        Neuron         neunow;
        int          []neucon;
        DoubleMatrix1D trainvec;
        double       []wi;
        
        // First derive patterns to recognize from trainset
        this.pattern   = new DoubleMatrix1D[this.trainData.getNumberOfInstances()];
        this.goalClass = new int[this.trainData.getNumberOfInstances()];
        for (i=0; i<this.pattern.length; i++)
        {
            this.pattern[i]   = this.trainData.getInstance(i);
            this.goalClass[i] = this.trainData.getGoalClass(i); 
        }
        
        // Determine the weights vectors of the neurons using the Hebbian imprinting rule
        neucount   = this.net.getNumberOfNeurons();
        traincount = this.pattern.length;
        for (i=0; i<neucount; i++)
        {
            neunow = this.net.getNeuron(i);
            neucon = neunow.getInputConnections();
            n      = neucon.length;
            wi     = new double[n];
            for (j=0; j<n; j++)
            {
                wij    = 0;
                for (k=0; k<traincount; k++)
                {
                    trainvec = this.pattern[k];
                    wij     += trainvec.getQuick(i) * trainvec.getQuick(neucon[j]);
                }
                wij  /= n;
                wi[j] = wij;
            }
            neunow.setWeights(wi);
        }
    }
    
    // **********************************************************\
    // *            Classification of given input               *
    // **********************************************************/
    public int classify(DoubleMatrix1D instance, double[]confidence) throws LearnerException
    {
        int            i, j, step, timeout;
        double       []inbuf;
        int            opos, mpos, statesize;
        double         dot, maxdot;
        DoubleMatrix1D statevec;
        DoubleMatrix1D trainins;
        
        inbuf  = new double[this.numneu];

        // Set input data in network
        instance.toArray(inbuf);
        this.net.setInput(inbuf);
        
        // Evolve state until convergence or time-out
        statevec  = null;
        timeout   = this.numneu*10;   // Heuristics! Heuristics!
        step      = timeout/20;       // -----------------------
        opos      = -1;
        mpos      = -1;
        maxdot    = Integer.MIN_VALUE;
        statesize = instance.size();
        for (i=0; (i<timeout) && (opos == -1); i+=step)
        {
            // Update state a number of times
            this.net.updateAsynchronous(step);
            statevec = this.net.getStateVector();
            
            // Check match with train instances
            maxdot = Integer.MIN_VALUE;
            for (j=0; (j<this.pattern.length) && (opos == -1); j++)
            {
                // Calculate distance between train pattern and current network state
                trainins = this.pattern[j];
                dot      = trainins.zDotProduct(statevec);
                if (dot == statesize)
                {
                    // Perfect match
                    opos = this.goalClass[j];
                    if (confidence != null) confidence[opos] = 1.0;
                }
                else if (dot > maxdot)
                {
                    // Best match up until now
                    mpos   = this.goalClass[j];
                    maxdot = dot;
                }
            }
        }
        
        // Take the best guess as output
        if (opos == -1)
        {
            if ((maxdot > 0) && (mpos != -1))
            {
                opos = mpos;
                if (confidence != null) confidence[opos] = maxdot / statesize;
            }
            else
            {
                opos = 0;
                if (confidence != null) confidence[0] = 0.0;
            }
        }
        
        return(opos);
    }
    
    public int classify(ObjectMatrix1D instance, double[]confidence) throws LearnerException
    {
        throw new LearnerException("Cannot classify Object based data");
    }
    
    // **********************************************************\
    // *                  Network Construction                  *
    // **********************************************************/
    public void create() throws LearnerException
    {
        NetworkGraph hopgraph;
        NeuralNet    net;
        
        try
        {
            net      = new NeuralNet();
            
            // Make a fully connected multi-layer neural net graph with the right number of neurons in the layers
            if (this.graph == null) hopgraph = NetworkGraphFactory.makeHopfield(this.numneu);
            else                    hopgraph = NetworkGraphFactory.makeHopfield(this.graph);
            
            // Convert graph to real network of neurons
            net.setNeuronType(Neuron.ACTIVATION_SIGN, null);
            net.create(hopgraph);
            this.numneu = net.getNumberOfNeurons();
            
            // All done.
            this.net = net;
        }
        catch(GraphException ex) { throw new LearnerException(ex); }
    }
    
    public void init() throws ConfigException
    {
        // Default classifier
        super.init();
    }
    
    public void cleanUp() throws DataFlowException
    {
        this.net = null;
    }
    
    public void checkDataModelFit(int port, DataModel dm) throws ConfigException
    {
        // Expect double based categorical (-1,1) input
        checkClassifierDataModelFit(dm, true, true, false);
    }
    
    // **********************************************************\
    // *                Parameter Specification                 *
    // **********************************************************/
    public void setGraph(Graph graph) { this.graph = graph; }
    
    public void setNumberOfNeurons(int numneu) { this.numneu = numneu; }
    
    public NeuralNet getNeuralNet() { return(this.net); }
    
    // **********************************************************\
    // *                    Learner Interface                   *
    // **********************************************************/
    public Presenter getTrainSet()
    {
        return(this.trainData);
    }
    
    public boolean isSupervised()
    {
        return(true);
    }
    
    public void setTrainSet(Presenter _instances) throws LearnerException
    {
        try
        {
            checkDataModelFit(0, _instances.getDataModel());
            this.trainData = _instances;
            this.dataModel = _instances.getDataModel();
        }
        catch(ConfigException ex) { throw new LearnerException(ex); }
    }
    
    public void initializeTraining() throws LearnerException
    {
        // Create a network according to the parameters, datamodels, etc...
        create();
    }
    
    public void train() throws LearnerException
    {
        // Train using Hebbian imprinting
        trainHopfield();
    }
    
    // **********************************************************\
    // *             State Persistence Implementation           *
    // **********************************************************/
    public void loadState(ObjectInputStream oin) throws ConfigException
    {
        try
        {
            // Load graph if explicitly specified
            super.loadState(oin);
            if (oin.readBoolean())
            {
                this.graph = new Graph();
                this.graph.loadState(oin);
            }
            else
            {
                this.graph  = null;
                this.numneu = oin.readInt();
            }
            
            // Create and load Neural Network logic
            create();
            this.net.loadState(oin);
            
            
            int i, numpat;
            
            // Load goal patterns
            numpat       = oin.readInt();
            this.pattern = new DoubleMatrix1D[numpat];
            for (i=0; i<this.pattern.length; i++)
                this.pattern[i] = DoubleFactory1D.dense.make((double [])oin.readObject());
            this.goalClass = (int [])oin.readObject();
        }
        catch(ClassNotFoundException ex) { throw new ConfigException(ex); }
        catch(LearnerException ex)       { throw new ConfigException(ex); }
        catch(IOException ex)            { throw new ConfigException(ex); }
    }
    
    public void saveState(ObjectOutputStream oout) throws ConfigException
    {
        try
        {
            // Save Graph or just number of fully connected neurons
            super.saveState(oout);
            if (this.graph != null)
            {
                oout.writeBoolean(true);
                this.graph.saveState(oout);
            }
            else
            {
                oout.writeBoolean(false);
                oout.writeInt(this.numneu);
            }
            
            // Save Neural Network logic
            this.net.saveState(oout);
            
            int i;
            
            // Save goal patterns
            oout.writeInt(this.pattern.length);
            for (i=0; i<this.pattern.length; i++)
                oout.writeObject(this.pattern[i].toArray());
            oout.writeObject(this.goalClass);
        }
        catch(IOException ex) { throw new ConfigException(ex); }
    }
    
    // **********************************************************\
    // *                    Construction                        *
    // **********************************************************/
    public HopfieldNetwork()
    {
        super();
        name        = "HopfieldNetwork";
        description = "Hopfield associative memory neural network";
    }
}
