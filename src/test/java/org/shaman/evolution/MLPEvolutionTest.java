/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *               Evolutionary Algorithms                 *
 *                                                       *
 *  January 2006                                         *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2006 Shaman Research                   *
\*********************************************************/
package org.shaman.evolution;

import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;
import org.shaman.neural.MLP;
import org.shaman.neural.Neuron;
import org.shaman.preprocessing.Normalization;

import junit.framework.TestCase;
import cern.jet.random.Normal;
import cern.jet.random.engine.MersenneTwister;


/**
 * Test evolution of MLP estimators and classifiers
 */
public class MLPEvolutionTest extends TestCase
{
    // *********************************************************\
    // *   Test MLP Estimator Evolution on 1D Sine Function    *
    // *********************************************************/
    public void testMLPEstimatorEvolution() throws Exception
    {
        MemorySupplier    ms;
        InstanceSetMemory im;
        
        // Load sine-wave data-set of 200 points in [-Pi, Pi]
        ms = new MemorySupplier();
        TestSets.loadSine(ms, 200, 1.37);
        im = new InstanceSetMemory();
        im.create(ms);
        
        MLPEnvironment mlpenv;
        MLP            mlp;
        
        // Create MLP right for approximating the sine funtion
        mlp = new MLP();
        mlp.setNeuronType(Neuron.ACTIVATION_SIGMOID_TANH, new double[]{1.0});
        mlp.setNetworkParameters(7, 0, MLP.OUTPUT_REGRESSION);
        mlp.setTrainSet(im);
        mlp.create();
        
        // PDF of neuron-weights
        Normal pdftemp = new Normal(0.0, 2, new MersenneTwister());
        
        // Configuration MLP Evolution environment with previous
        mlpenv = new MLPEnvironment();
        mlpenv.setMLPTemplate(mlp);
        mlpenv.setPDFTemplate(pdftemp);
        mlpenv.setDataSet(im);
        mlpenv.initialize();
        
        Evolution      evo;
        
        // Setup evolution and parameters
        evo = new Evolution();
        evo.setEnvironment(mlpenv);
        evo.setPopulationSize(250);
        evo.setPCrossover(0.8);
        evo.setPMutation(0.1);
        evo.setFitnessScale(Evolution.FITNESS_SCALE_RANK);
        evo.setSurvivalFraction(0.1);
        evo.initialize();
        
        int     i;
        boolean found;
        MLPFitnessFunction fitfunc;
        
        fitfunc = (MLPFitnessFunction)mlpenv.makeFitnessFunction();
        found   = false;
        for(i=0; (i<50) && (!found); i++)
        {
            evo.generation();
            //evo.logFitness();
            System.err.print("."); System.err.flush();
            if (i%10==0)System.err.print(i);
            //if (i%10 == 0) fitfunc.fitness(evo.getFittest(), true);
        }
        double fit = fitfunc.fitness(evo.getFittest(), false);
        System.err.println("FITNESS "+fit);
        assertEquals(10, fit, 0.02);
    }
    
    // *********************************************************\
    // *  Test MLP Classifier Evolution on Wine Classification *
    // *********************************************************/
    public void testMLPClassifierEvolution() throws Exception
    {
        MemorySupplier    ms   = new MemorySupplier();
        InstanceSetMemory im   = new InstanceSetMemory();
        Normalization     norm = new Normalization();
        
        // Load 3-class wine classification data-set. 14 continuous variables and 1 class.
        TestSets.loadWine(ms, false);
        im.create(ms);
        
        // Standardize the data.
        norm.registerSupplier(0, ms, 0);
        ms.registerConsumer(0, norm, 0);
        norm.setType(Normalization.TYPE_STANDARDIZE);
        norm.init();
        im.create(ms);
        norm.trainTransformation(im);
        im = InstanceSetMemory.estimateAll(im, norm);
        
        MLPEnvironment mlpenv;
        MLP            mlp;
        
        // Create MLP right for classifying the wine
        mlp = new MLP();
        mlp.setNeuronType(Neuron.ACTIVATION_SIGMOID_EXP, new double[]{.5});
        mlp.setNetworkParameters(10, 5, MLP.OUTPUT_ONE_OF_N);
        //mlp.setNetworkParameters(20, 10, MLP.OUTPUT_ONE_OF_N);
        mlp.setTrainSet(im);
        mlp.create();        
         
        // = new Normal(0.0, 0.1, new MersenneTwister());
        Normal pdftemp = new Normal(0.0, 1.0, new MersenneTwister());
        
        // Configuration MLP Evolution environment with previous
        mlpenv = new MLPEnvironment();
        mlpenv.setDataSet(im);
        mlpenv.setPDFTemplate(pdftemp);
        mlpenv.setMLPTemplate(mlp);
        mlpenv.initialize();
        
        Evolution      evo;
        
        // Setup evolution and parameters
        evo = new Evolution();
        evo.setEnvironment(mlpenv);
        evo.setPopulationSize(250);
        evo.setPCrossover(0.8);
        evo.setPMutation(0.1);
        evo.setFitnessScale(Evolution.FITNESS_SCALE_RANK);
        evo.setSurvivalFraction(0.1);
        evo.initialize();
        
        int     i;
        boolean found;
        MLPFitnessFunction fitfunc;
        
        fitfunc = (MLPFitnessFunction)mlpenv.makeFitnessFunction();
        found   = false;
        for(i=0; (i<50) && (!found); i++)
        {
            evo.generation();
            //evo.logFitness();
            if (i%10 == 0) fitfunc.fitness(evo.getFittest(), true);
        }
        double fit = fitfunc.fitness(evo.getFittest(), true);
        assertEquals(1.0 - 0.10, fit, 0.5);
    }
    
    // **********************************************************\
    // *                     Test-Case Setup                    *
    // **********************************************************/
    protected void setUp() throws Exception
    {
    }
    
    protected void tearDown() throws Exception
    {
    }
    
    public MLPEvolutionTest()
    {
    }
}