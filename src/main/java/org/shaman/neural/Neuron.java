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

import org.shaman.exceptions.LearnerException;
import org.shaman.util.MathUtil;

import cern.jet.random.Uniform;


/**
 * <h2>A Neuron</h2>
 * Various kinds of neurons for various types of neural networks.
 * A neuron is always part of a neural network. It gets it's input
 * data from the net's state vector and puts it's single output value
 * in the state vector.
 */

// **********************************************************\
// *     A Neuron. For MLPs, SOMs, Hopfield Nets, etc...    *
// **********************************************************/
public class Neuron
{
    /** Linear activation function. f(x) = x */
    public static int ACTIVATION_LINEAR             = 1;
    /** Threshold activation function. f(x) = -1 if x-th < 0, 1 if x-th >= 0 */
    public static int ACTIVATION_THRESHOLD          = 2;
    /** Exponential Sigmoidal activation function. f(x) = 1 / 1+exp(-2*ap0*x) */
    public static int ACTIVATION_SIGMOID_EXP        = 3;
    /** TanH Sigmoidal activaton function. f(x) = tanh(ap0*x) */
    public static int ACTIVATION_SIGMOID_TANH       = 4;
    /** Activation is Euclidian distance between weights and input */
    public static int ACTIVATION_EUCLIDEAN_DISTANCE = 10;
    /** Activation is dot product between weights and input */
    public static int ACTIVATION_DOT_PRODUCT        = 11;
    /** Gaussian kernel activation function. f(x) =  exp( -(x-w) / ap0) */
    public static int ACTIVATION_KERNEL_GAUSSIAN    = 20;
    /** Sign activation function. f(x) = -1 if x < 0 else f(x) = 1 */
    public static int ACTIVATION_SIGN               = 30;
    
    /** Initialize the weights with Uniform random values from [-par1,par1]; */
    public static int WEIGHT_INIT_RANDOM       = 1;
    /** Initialize the weights with Uniform random values from [-par1,par2]; */
    public static int WEIGHT_INIT_RANDOM_RANGE = 2;
    /** Initialize the weights with 0 */
    public static int WEIGHT_INIT_ZERO         = 0;
    
    /** This neuron's weight vector */
    protected double             []weight;
    /** The indices in the neural net's state vector of the input vector of this  neuron */
    protected int                []input;
    /** The index of this neuron's output in the neural net's state vector */
    protected int                output;
    /** The threshold of this neuron */
    private double             threshold;
    /** Type of activation function. */
    private int                activation;
    /** The activation function's parameters */
    private double             []ap;
    /** The neural network this neuron is part of */
    private NeuralNet          net;
    /** The inner class implementing the activation function of this neuron. */
    private ActivationFunction actfunc;   
    
    // **********************************************************\
    // *                   Neuron Activation                    *
    // **********************************************************/
    /**
     * Evaluate the activation function. Store result in the synchronous state vector.
     */
    public void activateSynchronous()
    {
        // Calculate the neuron's output. Store in synchronous output buffer.
        this.net.statesync[this.output] = this.actfunc.act();
    }
    
    public void activateAsynchronous()
    {
        // Calculate the neuron's output. Update state directly.
        this.net.state[this.output] = this.actfunc.act();
    }
    
    // **********************************************************\
    // *                   Neuron construction                  *
    // **********************************************************/
    /**
     * Initialize the neurons weights according to the given initialized type and parameters.
     * @param witype The type of neuron initialization to do
     * @param par1 Parameter 1.
     * @param par2 Parameter 2.
     * @see #WEIGHT_INIT_RANDOM
     * @see #WEIGHT_INIT_RANDOM_RANGE
     * @see #WEIGHT_INIT_ZERO
     */
    public void initWeights(int witype, double par1, double par2)
    {
        int i;
        
        if (witype == WEIGHT_INIT_RANDOM) // Uniform random values from [-par1,par1];
        {
            for (i=0; i<this.weight.length; i++) this.weight[i] = Uniform.staticNextDoubleFromTo(-par1, par1);
        }
        if (witype == WEIGHT_INIT_RANDOM_RANGE) // Uniform random values from [par1, par2];
        {
            for (i=0; i<this.weight.length; i++) this.weight[i] = Uniform.staticNextDoubleFromTo(par1, par2);
        }
        else if (witype == WEIGHT_INIT_ZERO) // Zero weights.
        {
            for (i=0; i<this.weight.length; i++) this.weight[i] = 0;
        }
    }
    public void initWeights(int witype, double par1) { initWeights(witype, par1, 0); }
    
    /**
     * Modify this neuron's weights by copying the given weight vector into this neuron.
     * @param _win The new weights of this neuron
     */
    public void setWeights(double []_win)
    {
        for (int i=0; i<this.weight.length; i++) this.weight[i] = _win[i];
    }
    
    /**
     * Get the weights vector of this neuron
     * @return The weight vector of this neuron.
     */
    public double []getWeights() { return(this.weight); }
    
    /**
     * Get the activation function of this neuron
     * @return The ActivationFunction of this neuron.
     */
    public ActivationFunction getActivationFunction() { return(this.actfunc); }
    
    /**
     * Set the kind of activation function used in this neuron.
     * @param _activation The kind of activation function to use.
     * @param par The parameters of this activation function.
     * @throws LearnerException if the activation function cannot be created.
     */
    public void setActivation(int _activation, double []par) throws LearnerException
    {
        setActivation(_activation);
        for (int i=0; i<this.ap.length; i++) this.ap[i] = par[i];
    }
    
    /**
     * Set the activation function to use in this neuron.
     * @param _activation The kind of activation function to use.
     * @throws LearnerException if the activation function cannot be created.
     */
    public void setActivation(int _activation) throws LearnerException
    {
        this.activation = _activation;
        if      (this.activation == ACTIVATION_LINEAR)
        {
            this.actfunc = new NeuronLinear();
            this.ap      = new double[0];
        }
        else if (activation == ACTIVATION_SIGMOID_EXP)
        {
            this.actfunc = new NeuronSigmoidExp();  // f(x) = 1 / 1+exp(-2*ap0*x)
            this.ap      = new double[1];
            this.ap[0]   = 0.5;
        }
        else if (activation == ACTIVATION_SIGMOID_TANH)
        {
            this.actfunc = new NeuronSigmoidTanh(); // f(x) = tanh(ap0*x)
            this.ap      = new double[1];
            this.ap[0]   = 1.0;
        }
        else if (activation == ACTIVATION_KERNEL_GAUSSIAN)
        {
            this.actfunc   = new NeuronKernelGaussian(); // f(x) = 1 if exp( -(x-w) / ap0) > threshold else 0
            this.ap        = new double[1];
            this.ap[0]     = 1;
            this.threshold = 0.5;
        }
        else if (activation == ACTIVATION_SIGN)
        {
            this.actfunc   = new NeuronSign();
            this.ap        = new double[0];
            this.threshold = 0.0;
        }
        else throw new LearnerException("Cannot initialize unknown activation function.");
    }
    
    /** Set the parameters of the current activation function.
     *  Copy the given parameter vector into this neuron.
     *  @param par The activation functions parameters.
     */
    public void setActivationParameters(double []par)
    {
        for (int i=0; i<this.ap.length; i++) this.ap[i] = par[i];
    }
    
    /**
     * Set the connections to the input neurons
     * @param input Array with the indices in the network state of the input neurons
     * @param output The index of the output neuron
     */
    public void setConnections(int []input, int output)
    {
        this.input  = input;
        this.output = output;
        this.weight = new double[input.length];
    }
    
    public int []getInputConnections() { return(this.input); }
    
    public int getOutputConnection() { return(this.output); }
    
    /**
     * Get the activation function's parameter vector
     * @return The activation function's parameter vector
     */
    public double []getActivationParameter() { return(this.ap); }
    
    /**
     * Set the threshold of this neuron.
     * @param _threshold The new threshold to use.
     */
    public void   setThreshold(double _threshold) { this.threshold = _threshold; }
    
    /**
     * Get the threshold of this neuron.
     * @return The threshold of this neuron.
     */
    public double getThreshold() { return(this.threshold); }
    
    /**
     * Make a Neuron that is part of the given network, with the specified activation function,
     * that gets it's input from the specified input indices in the net's state vector and
     * outputs to the given output index in the net's state vector.
     * @param _net The neural network this neuron is part of
     * @param _activation The activation to use in this neuron
     * @param _input The indices in the state vector containing the input.
     * @param _output The index in that state vector where to put the output.
     * @throws LearnerException If the activation function could not be made.
     */
    public Neuron(NeuralNet _net, int _activation, int []_input, int _output) throws LearnerException
    {
        this.net      = _net;
        this.input    = _input;
        this.output   = _output;
        this.weight    = new double[input.length];
        this.threshold = 0;
        setActivation(_activation);
    }
    
    /**
     * Create a Neuron that is part of the given network, with the specified activation function.
     * @param _net The neural network this neuron is part of.
     * @param _activation The activation function to use in this neuron.
     * @throws LearnerException If the activation function could not be made.
     */
    public Neuron(NeuralNet _net, int _activation, double []_actPar) throws LearnerException
    {
        this.net       = _net;
        this.threshold = 0;
        setActivation(_activation, _actPar);
    }
    
    // **********************************************************\
    // *         Activation functions as inner classes          *
    // **********************************************************/
    protected double dotInputsWeights()
    {
        int    i;
        double dp;
        
        // Calculate dot-product of weight vector and input neuron states
        dp = 0;
        for (i=0; i<this.weight.length; i++) 
            dp += this.net.state[this.input[i]] * this.weight[i];
        
        return(dp);
    }
    
    /**
     * <h3>Sign Activation Function</h3>
     */
    class NeuronSign implements ActivationFunction
    {
        public double act()
        {
            double input;
            double sign;
            
            input = dotInputsWeights();
            if (input < 0) sign = -1;
            else           sign =  1;
            
            return(sign);
        }
        
        public double actDer(double x)
        {
            return(0);
        }
        
        public String toString() { return("SIGN"); }
    }
    
    /**
     * <h3>Gaussian Kernel Activation Function</h3>
     */
    class NeuronKernelGaussian implements ActivationFunction
    {
        // f(x) =  exp( -(x-w) / ap0)
        public double act()
        {
            int    i;
            double dsxw, xi;
            double e;
            
            dsxw = 0;
            for (i=0; i<weight.length; i++)
            {
                xi    = net.state[input[i]];
                dsxw += (xi - weight[i]) * (xi - weight[i]);
            }
            
            e = Math.exp(-dsxw / ap[0]*ap[0]);
            
            return(e);
            
            //if (e > threshold) return(1);
            //else               return(0);
        }
        
        public double actDer(double x) { return(0); }
        
        public String toString() { return("KERNELGAUSSIAN"); }
    }
    
    /**
     * <h3>Linear Activation Function</h3>
     */
    class NeuronLinear implements ActivationFunction
    {
        // Thresholded Linear Activation Function
        // x = x-threshold
        public double act()
        {
            return(dotInputsWeights() - threshold);
        }
        
        public double actDer(double x)
        {
            return(x - threshold);
        }
        
        public String toString() { return("LINEAR"); }
    }
    
    /**
     * <h3>Exponential Sigmoid Activation Function</h3>
     */
    class NeuronSigmoidExp implements ActivationFunction
    {
        // Non threshold Sigmoidal Activation Function :
        //  f(x) = 1 / 1+exp(-2*ap0*x)
        // df(x) = 2*ap0*fx*(1-fx)
        
        public double act()
        {
            double x;
            double fx;
            
            x  = dotInputsWeights();
            fx = act(x);
            
            return(fx);
        }
        
        public double act(double x)
        {
            double fx;
            fx = 1.0 / (1.0 + Math.exp(-2*ap[0]*x));
            
            return(fx);
        }
        
        public double actDer(double x)
        {
            double dfx;
            
            dfx = 2.0*ap[0]*x*(1.0 - x);
            
            return(dfx);
        }
        
        public String toString() { return("SIGMOIDEXP"); }
    }
    
    /**
     * <h3>TanH Sigmoid Activation Function</h3>
     */
    class NeuronSigmoidTanh implements ActivationFunction
    {
        // Non threshold Sigmoidal Activation Function :
        //  f(x) = tanh(ap0*x)
        // df(x) = ap0*(1-fx)
        
        public double act()
        {
            double x;
            double fx;
            
            x  = dotInputsWeights();
            fx = act(x);
            
            return(fx);
        }
        
        public double act(double x)
        {
            double fx;
            
            fx = MathUtil.tanh(ap[0]*x);
            
            return(fx);
        }
        
        public double actDer(double x)
        {
            double dfx;
            
            dfx = ap[0]*(1-(x*x));
            
            return(dfx);
        }
        
        public String toString() { return("SIGMOIDTANH"); }
    }
}