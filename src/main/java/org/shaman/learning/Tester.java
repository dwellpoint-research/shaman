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
package org.shaman.learning;

import org.shaman.exceptions.LearnerException;

/**
 * <h2>Testing a Machine Learning Algorithm</h2>
 * Perform some kind of testing method on a Learner using a set of instances.
 */
public interface Tester
{
    /**
     * Perform test(s) on a Learner's machine-learning components.
     * @throws LearnerException If something went wrong during training the Learner or evaluating to results.
     */
    public void      test() throws LearnerException;
    
    /**
     * Set the instances to use during testing.
     * @param _instances The instances to use.
     */
    public void      setSet(Presenter _instances);
    
    /**
     * Get the instances used during testing.
     * @return The instances used during testing.
     */
    public Presenter getSet();
}
