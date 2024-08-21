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

import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.LearnerException;

public interface Environment
{
    /**
     * Create a random genotype whose fitness can be evaluated by a FitnessFunction made by this interface.
     * @return
     * @throws LearnerException
     */
    public Genotype makeRandomGenotype() throws LearnerException;

    /**
     * Create a fitness function that can evaluate a genotype of the type made by the above method.
     * @return
     * @throws LearnerException
     * @throws ConfigException
     */
    public FitnessFunction makeFitnessFunction() throws LearnerException, ConfigException;
}