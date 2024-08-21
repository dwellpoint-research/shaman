/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *               Evolutionary Algorithms                 *
 *                                                       *
 *  April 2005                                           *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2005 Shaman Research                   *
\*********************************************************/
package org.shaman.evolution;

import org.shaman.exceptions.LearnerException;

/**
 * <h2>Text String Fitness</h2>
 */
public class TextFitnessFunction implements FitnessFunction
{
    private TextEnvironment env;
    
    public double fitness(Genotype genotype) throws LearnerException
    {
        String genotypeText, goalText;
        int    fitness;
        
        // Count number of character matching with the goal text.
        genotypeText = ((TextGenotype)genotype).getText();
        goalText     = this.env.getText();
        
        fitness = 0;
        for (int i=0; i<genotype.getLength(); i++)
        {
            if (genotypeText.charAt(i) == goalText.charAt(i)) fitness++;
        }
        
        return(fitness);
    }
    
    public void setEnvironment(TextEnvironment env)
    {
        this.env = env;
    }
}