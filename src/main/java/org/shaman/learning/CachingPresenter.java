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

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Presentation of Data Cached in Memory</h2>
 * All available instance data is cached in memory and is immediately available. <p>
 * Used by training algorithm that need very random access to all instances
 * e.g. ID3 decision tree training.
 **/
public interface CachingPresenter extends Presenter, Cloneable
{
    // Allow immediate and complete access to all cached data
    /**
     * Get all cached instances.
     * @return The cached instances.
     * @throws LearnerException If something goes wrong.
     */
    public DoubleMatrix1D[] getInstances() throws LearnerException;        // Get all  instances
    
    /**
     * Get all cached Object instances.
     * @return The cached Object instances.
     * @throws LearnerException If something goes wrong.
     */
    public ObjectMatrix1D[] getObjectInstances() throws LearnerException;
    
    /**
     * Get the goals of all cached instances.
     * @return The goal values of the instances.
     * @throws LearnerException If something goes wrong.
     */
    public double[]         getGoals() throws LearnerException;            // Get all instance goals
    
    
    /**
     * Get the Object goals of all cached Object instances.
     * @return The Object goal values of the instances.
     * @throws LearnerException If something goes wrong.
     */
    public Object []        getObjectGoals() throws LearnerException;
    
    /**
     * Get the weights of all cached instances.
     * @return The weights of the instances.
     */
    public double[]         getWeights();                              // Get all instance weights
    
    /**
     * Replace the cached instances with the given ones.
     * @param _instance The new set of instances.
     */
    public void             setInstances(DoubleMatrix1D []_instance);  // Set the list of instances
    
    /**
     * Replace the cached Object instances with the given ones.
     * @param _instance The new set of Object Instances.
     */
    public void             setObjectInstances(ObjectMatrix1D []_instance);
    
    /**
     * Replace the goal values of the cached instances with the given set of goal values.
     * @param _goal The new set of goal values.
     */
    public void             setGoals(double []_goal);                  // Set the list of goals
    
    /**
     * Replace the Object goal values of the cahced instances with the given set of Object goal values.
     * @param _goal The new set of Object goal values.
     */
    public void             setObjectGoals(Object []_goal);
    
    /**
     * Replace the weights of the cached instances with the given weights.
     * @param _weight The new set of weights.
     */
    public void             setWeights(double []_weight);              // Set the list of weights
    
    /**
     * Make the specified amount of instance/goal value/weight combinations.
     * @param size The number of instances to create.
     */
    public void             makeInstances(int size);
    
    /**
     * Set the weight of all instances having the specified goalValue to the given weight.
     * @param goalValue The goal value of the instances.
     * @param w Their new weight.
     */
    public void setWeightWhereGoalIs(double goalValue, double w);
    
    
    /**
     * Set the weight of all Object instance having the specified Object goalValue to the given weight.
     * @param goalValue The Object goal value of the instances.
     * @param w Their new weight.
     */
    public void setWeightWhereGoalIs(Object goalValue, double w);
    
    /**
     * Clone this set of cached instances.
     * Used in e.g. making training/test sets.
     * Does not copy the actual data instances. Just copier the list/array containing them.
     * @return The cloned set of cached instances
     * @throws CloneNotSupportedException If this set could not be cloned.
     */
    public Object clone() throws CloneNotSupportedException;
}
