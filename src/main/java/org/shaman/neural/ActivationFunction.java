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

/**
 * <h3>Activation Function</h3>
 */
public interface ActivationFunction
{
    /**
     * Execute the activation function.
     * @return The activation of the neuron on the current input.
     */
    public double act();             // The activation function
    
    /**
     * Calculate the derivative of the activation function at the given value.
     * @param x The value to calculate the derivative for.
     * @return Derivative of the activation function at x
     */
    public double actDer(double x);  // Derivative of the activation function at x
}
