/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *               Evolutionary Algorithms                 *
 *                                                       *
 *  May 2006                                             *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2006 Shaman Research                   *
\*********************************************************/
package org.shaman.evolution;

import org.shaman.exceptions.LearnerException;

/**
 * Runnable wrapper for Fitness evaluation
 */
public class FitnessCommand implements Runnable
{
    private Evolution       evolution;        // The Evolutionary algorithm using this command for fitness evaluation
    
    private FitnessFunction fitnessFunction;  // The fitness function used to evaluate the given genotype
    private Genotype        genotype;         // The genotype to evaluate
    private int             index;            // The index of the genotype
    // --------
    private double          fitness;         // The fitness of the genotype;
    
    // Parameter specification
    public void setFitnessFunction(FitnessFunction fitfunc) { this.fitnessFunction = fitfunc; }
    public void setGenotype(Genotype genotype)              { this.genotype = genotype; }
    public void setIndex(int index)                         { this.index    = index;}
    
    // Result access
    public double getFitness() { return(this.fitness); }
    
    /**
     * Evaluate the fitness of the given genotype.
     */
    public void run()
    {
        try
        {
            // Evaluate the fitness of the given Genotype
            this.fitness = this.fitnessFunction.fitness(this.genotype);
        }
        catch(LearnerException ex)
        {
            ex.printStackTrace();
            this.fitness = -1;
        }
        finally
        {
            // Report back fitness. Decrease countdown barrier.
            this.evolution.setFitnessResult(this.index, this.fitness, this.fitnessFunction);
        }
    }
    
    public FitnessCommand(Evolution evolution)
    {
        this.evolution = evolution;
    }
}