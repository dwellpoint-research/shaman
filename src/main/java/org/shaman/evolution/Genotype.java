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

import org.shaman.dataflow.Persister;

/**
 * <h2>Genotype</h2>
 * Genotype interface for evolutionary algorithms containing initialization, genetic operators and persistence.
 */
public interface Genotype extends Persister
{
    /**
     * Give number of variables in the data-string that forms the Genotype
     * @return Length of Genotype
     */
    public int      getLength();
    
    /**
     * Initialize a random Genotype of the given length.
     * @param length The length of the Genotype to initialize
     */
    public void     initRandom(int length);
    
    /**
     * Crossover operator. Return a Genotype whose data-string switches between this Genotype
     * and the given one at the given position in the string.
     * @param genotype2 The Genotype to combine with
     * @param crossOverPosition The position in the data-string to cross-over to the given Genotype
     * @return The combined Genotype.
     */
    public Genotype crossover(Genotype genotype2, int crossOverPosition);
    
    /**
     * Change the variable at the given position by a random one.
     * @param position The position in the genotype to mutate.
     */
    public void     mutate(int position);
}