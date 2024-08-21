/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *               Evolutionary Algorithms                 *
 *                                                       *
 *  April 2005 & December 2005                           *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2005 Shaman Research                   *
\*********************************************************/
package org.shaman.evolution;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.jet.random.AbstractDistribution;
import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.neural.MLP;
import org.shaman.neural.NeuralNet;
import org.shaman.neural.Neuron;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * <h2>Neural Network Fitness</h2>
 */
public class MLPFitnessFunction implements FitnessFunction
{
    private MLP                  mlpTemplate;   // The Multi-layer feed-forward neural net template
    private AbstractDistribution pdfTemplate;   // Neuron Weight PDF template
    private InstanceSetMemory    dataset;       // Training/Testing instances
    
    // **********************
    private static final int TASK_CLASSIFY = 1;
    private static final int TASK_ESTIMATE = 2;
    
    private int    [][]weightIndex;             // Index in genotype of Neuron[i] Weight[j]
    private double [][]neuronWeights;           // Buffers for neuron weights from genotype;
    private double   []outputVector;            // Output vector
    private int        taskType;                // Classifier or Estimator neural net?
    
    private MLP                    mlp;         // The MLP to operate on
    private NeuralNet              neuralNet;   // Its Neural network logic
    private AbstractDistribution []genpdf;      // PDF's for the neuron weights that define the genotype
    
    // *********************************************************\
    // *  How close is the evolved MLP to the output function? *
    // *********************************************************/
    public double fitness(Genotype genotype) throws LearnerException
    {
        return(fitness(genotype, false));
    }
    
    public double fitness(Genotype genotype, boolean dolog) throws LearnerException
    {
        double   fitness;
        double []genotypeWeights;
        Neuron   neuron;
        
        // Copy the evolved weights in the MLP's neurons
        genotypeWeights = ((NumberGenotype)genotype).getGenes();
        for (int i=0; i<this.weightIndex.length; i++)
        {
            neuron = this.neuralNet.getNeuron(i);
            for (int j=0; j<this.weightIndex[i].length; j++)
                this.neuronWeights[i][j] = genotypeWeights[this.weightIndex[i][j]];
            neuron.setWeights(this.neuronWeights[i]);
        }
        
        // Run data-set through network. Derive fitness function from estimation or classification performance.
        if (this.taskType == TASK_ESTIMATE) fitness = fitnessEstimator(dolog);
        else                                fitness = fitnessClassifier(dolog);
        
        return(fitness);
    }
    
    private double fitnessClassifier(boolean dolog) throws LearnerException
    {
        DoubleMatrix1D []instances;
        double         []instanceVector;
        int              maxClass, goalClass;
        double           maxOutput, error;
        double           fitness;
        DoubleMatrix2D   confusionMatrix;
        
        // Classify the instances in the data-set with the evolved network. Fitness is 1 - classification error
        confusionMatrix = DoubleFactory2D.dense.make(this.outputVector.length, this.outputVector.length);
        error     = 0;
        instances     = this.dataset.getInstances();
        for (int i=0; i<instances.length; i++)
        {
            // Run instance through network
            instanceVector = instances[i].toArray();
            this.neuralNet.setInput(instanceVector);
            this.neuralNet.updateSynchronous();
            this.neuralNet.getOutput(this.outputVector);
            
            // Find class with highest output value
            maxClass  = 0;
            maxOutput = this.outputVector[0];
            for (int j=1; j<this.outputVector.length; j++)
            {
                if (this.outputVector[j] > maxOutput)
                {
                    maxClass  = j;
                    maxOutput = this.outputVector[j];
                }
            }
            
            // Does this class agree with the expected one?
            goalClass = this.dataset.getGoalClass(i);
            if (maxClass != goalClass) error += 1.0;
            confusionMatrix.setQuick(goalClass, maxClass, confusionMatrix.getQuick(goalClass, maxClass)+1);
        }
        error /= instances.length;
        
        if (dolog)
        {
            System.out.println("Error: "+error+" Confusion Matrix: "+confusionMatrix);
        }
        
        // Fitness 1 - classification error.
        fitness = 1.0 - error;
        
        return(fitness);
    }
    
    private double fitnessEstimator(boolean dolog) throws LearnerException
    {
        DoubleMatrix1D []instances;
        double         []instanceVector;
        double           out, expected, error;
        double           fitness;
        
        // Run the MLP on the target function domain. Measure difference with expected value.
        error = 0;
        instances = this.dataset.getInstances();
        for (int i=0; i<instances.length; i++)
        {
            instanceVector = instances[i].toArray();
            this.neuralNet.setInput(instanceVector);
            this.neuralNet.updateSynchronous();
            this.neuralNet.getOutput(this.outputVector);
            
            out       = this.outputVector[0];
            expected  = this.dataset.getGoal(i);
            error += (out-expected)*(out-expected);
        }
        error /= instances.length;
        
        if (dolog)
        {
            System.out.println("Error: " + error);
        }
        
        // Fitness is negative of mean error.
        fitness = 10-error;
        
        return(fitness);
    }
    
    // *********************************************************\
    // *                   Configuration                       *
    // *********************************************************/
    public void setMLPTemplate(MLP mlp)                      { this.mlpTemplate = mlp; }
    public void setPDFTemplate(AbstractDistribution pdfTemp) { this.pdfTemplate = pdfTemp; }
    public void setDataSet(InstanceSetMemory funcout)        { this.dataset     = funcout; }
    public AbstractDistribution []getGenoTypePDFs()          { return(this.genpdf); }
    
    // *********************************************************\
    // *               Initialization / Cleanup                *
    // *********************************************************/
    private void cloneMLPTemplate() throws ConfigException
    {
        try
        {
            ByteArrayOutputStream bout;
            ObjectOutputStream    oout;
            ObjectInputStream     oin;
            byte                []mlpstate;
            MLP                   mlpclone;
            
            // Clone the MLP template using the Persister interface
            bout = new ByteArrayOutputStream();
            oout = new ObjectOutputStream(bout);
            this.mlpTemplate.saveState(oout);
            oout.flush();
            mlpstate = bout.toByteArray();
            oout.close();
            bout.close();
            
            oin      = new ObjectInputStream(new ByteArrayInputStream(mlpstate));
            mlpclone = new MLP();
            mlpclone.loadState(oin);
            oin.close();
            
            this.mlp = mlpclone;
        }
        catch(IOException ex) { throw new ConfigException(ex); }
    }
    
    public void initialize() throws ConfigException
    {
        int        numberOfNeurons, genotypeLength;
        AbstractDistribution []genpdf;
        int        pos, len;
        int    [][]weightIndex;
        double [][]neuronWeights;
        NeuralNet  neuralNet;
        Neuron     neuron;
        
        // Clone the given template neural net.
        cloneMLPTemplate();
        
        // Create PDFs for neural network weights contained in the NumberGenotype
        numberOfNeurons = this.mlp.getNeuralNet().getNumberOfNeurons();
        genotypeLength = 0;
        for (int i=0; i<numberOfNeurons; i++)
        {
            neuron          = this.mlp.getNeuralNet().getNeuron(i);
            genotypeLength += neuron.getWeights().length;
        }
        genpdf = new AbstractDistribution[genotypeLength];
        for (int i=0; i<genpdf.length; i++)
        {
            genpdf[i] = (AbstractDistribution)this.pdfTemplate.clone();
        }
        this.genpdf = genpdf;
        
        // Find out which positions in the genotype correspond to which neuron's weights
        neuralNet     = this.mlp.getNeuralNet();
        weightIndex   = new int[neuralNet.getNumberOfNeurons()][];
        neuronWeights = new double[neuralNet.getNumberOfNeurons()][];
        pos         = 0;
        for (int i=0; i<neuralNet.getNumberOfNeurons(); i++)
        {
            neuron = neuralNet.getNeuron(i);
            len    = neuron.getWeights().length;
            weightIndex[i]   = new int[neuron.getWeights().length];
            neuronWeights[i] = new double[neuron.getWeights().length];
            for (int j=0; j<len; j++) weightIndex[i][j] = pos++;
        }
        this.weightIndex   = weightIndex;
        this.neuronWeights = neuronWeights;
        this.neuralNet = neuralNet;
                
        DataModel dm;
        Attribute atgoal;
        
        // What type of learning task are we dealing with
        dm     = this.dataset.getDataModel();
        atgoal = dm.getAttribute(dm.getLearningProperty().getGoalIndex());
        if (atgoal.getGoalType() == Attribute.GOAL_CLASS)
        {
            // n-class classification problem
            this.taskType = TASK_CLASSIFY;
            this.outputVector = new double[atgoal.getNumberOfGoalClasses()];
        }
        else
        {
            // 1-D function estimation
            this.taskType = TASK_ESTIMATE;
            this.outputVector = new double[1];
        }
    }
}