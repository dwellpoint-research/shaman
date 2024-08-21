/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *               Evolutionary Algorithms                 *
 *                                                       *
 *  April 2005 and December 2005                         *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2005 Shaman Research                   *
\*********************************************************/
package org.shaman.evolution;

import org.shaman.exceptions.LearnerException;

/**
 * <h2>Fitness Function</h2>
 */
public interface FitnessFunction
{
    /**
     * Calculate the fitness of the given Genotype.
     * @param genotype The Genotype to calculate the fitness for.
     * @return The fitness of the Genotype
     * @throws LearnerException When something goes wrong during fitness calculation.
     */
    public double fitness(Genotype genotype) throws LearnerException;
}