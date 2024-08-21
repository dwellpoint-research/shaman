/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *               Evolutionary Algorithms                 *
 *                                                       *
 *  April 2005 & January 2015                            *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2015 Shaman Research                   *
 \*********************************************************/

package org.shaman.evolution;

import cern.jet.random.AbstractDistribution;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.LearnerException;
import org.shaman.neural.NeuralNet;

public class PhysicsEnvironment implements Environment
{
    private Evolution evolution;
    private PhysicsFigure figureTemplate;
    private AbstractDistribution  []genotypePDFs;   // PDF's for the neuron weights and body shape that make up the genotype

    // *********************************************************\
    // *                Create Brain Genotype                  *
    // *********************************************************/
    public Genotype makeRandomGenotype() throws LearnerException
    {
        NumberGenotype gen;

        gen = new NumberGenotype(this.genotypePDFs);
        gen.initRandom(this.genotypePDFs.length);

        return(gen);
    }

    public FitnessFunction makeFitnessFunction() throws LearnerException
    {
        PhysicsFitnessFunction fitness;

        try
        {
            // Prepare phyiscal world fitness function
            fitness = new PhysicsFitnessFunction();
            fitness.initialize(this);
        }
        catch(ConfigException ex) { throw new LearnerException(ex); }

        return fitness;
    }

    // *********************************************************\
    // *                   Initialization                      *
    // *********************************************************/
    public void initialize() throws ConfigException
    {
        PhysicsFitnessFunction fitnessFunction;

        // Prepare phyiscal world fitness function
        fitnessFunction = new PhysicsFitnessFunction();
        fitnessFunction.initialize(this);

        NeuralNet nnet;
        int       i, numberOfNeuronWeights;

        // Count the total of neuron weights. Each weight has a PDF in the figure genotype.
        nnet   = fitnessFunction.getNeuralNet();
        numberOfNeuronWeights = 0;
        for (i=0; i<nnet.getNumberOfNeurons(); i++) numberOfNeuronWeights += nnet.getNeuron(i).getWeights().length;
        
        // Ask the figure to make the PDFs for the neuron weights AND the figure's shape and behaviour parameters as well.
        this.genotypePDFs = this.figureTemplate.makeGenotypeDistributions(numberOfNeuronWeights);
    }
    
    public void setEvolution(Evolution evolution)
    {
        this.evolution = evolution;
    }
    
    public Evolution getEvolution()
    {
        return this.evolution;
    }
    
    public void setFigureTemplate(PhysicsFigure physicsFigure)
    {
        this.figureTemplate = physicsFigure;
    }
    
    public PhysicsFigure getFigureTemplate()
    {
        return this.figureTemplate;
    }

    public PhysicsEnvironment()
    {
    }
}
