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
 * <h2>Machine Learning Base Interface</h2>
 * A class implementing this interface contains some kind of
 * machine-learning or data-mining algorithm that can extract
 * useful knowledge from the given train set of data.
 * <p>
 */

// **********************************************************\
// *       Base Interface for machine-learning Flows        *
// **********************************************************/
public interface Learner
{
    /** Learner type of a supervised classification learner */
    public static final int LEARNER_CLASSIFIER = 0;
    /** Learner type of an unsupervised classification learner */
    public static final int LEARNER_CLUSTERER  = 1;
    /** Learner type for a supervised function estimation learner */
    public static final int LEARNER_ESTIMATOR  = 2;
    /** Learner type for a 'different' type of learner. */
    public static final int LEARNER_OTHER      = 3;
    
    /** Get the current set of training data used by the learner */
    public Presenter getTrainSet();
    
    /** Set the set of training data used by the learner */
    public void      setTrainSet(Presenter _instances) throws LearnerException;
    
    /** Initialize the training algorithm. */
    public void      initializeTraining() throws LearnerException;
    
    /** Train the internal model of the learner using a machine-learning or data-mining algorithm. */
    public void      train() throws LearnerException;
    
    /** Is this learner supervised? Does it use labeled training data? */
    public boolean   isSupervised();
}
