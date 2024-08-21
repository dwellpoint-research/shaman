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


import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.LearnerException;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Data Presentation Interface</h2>
 * A presenter contains a set of instances to be used by a machine-learning algorithm.
 * An instance differs from data vectors that are communicated between Flows
 * in that instances are stripped of their inactive fields. Their goal
 * values (if present) are also available. A presenter also contains a weight
 * for every instance.
 **/
public interface Presenter extends Cloneable
{
    /**
     * Get the number of instances currently available.
     * @return The number of available instance.
     */
    public int              getNumberOfInstances();
    
    /**
     * (re)Read all available instances from data source.
     * @throws LearnerException If something went wrong reading data from the source.
     */
    public void             getNewInstances() throws LearnerException;
    
    /**
     * Get the LearnerDataModel describing the instance.
     * @return The datamodel of the instances.
     */
    public DataModel getDataModel();
    
    /**
     * Change the datamodel of the instances to the given one.
     * @param dm The new datamodel for the instances.
     */
    public void             setDataModel(DataModel dm);
    
    /**
     * Get the instance at the given index.
     * @param ind The index
     * @return The instance at the given index.
     * @throws LearnerException If the instance could not be retrieved.
     */
    public DoubleMatrix1D   getInstance(int ind) throws LearnerException;
    
    /**
     * Get the Object instance at the given index.
     * @param ind The index
     * @return The Object instance at the given index.
     * @throws LearnerException If the instance could not be retrieved.
     */
    public ObjectMatrix1D  getObjectInstance(int ind) throws LearnerException;
    
    /**
     * Get the goal-value of the instance at the given index.
     * @param ind The index
     * @return The goal-value of the instance at the given index.
     * @throws LearnerException If the goal value is not available.
     */
    public double           getGoal(int ind) throws LearnerException;
    
    /**
     * Get the Object goal-value of the instance at the given index.
     * @param ind The index
     * @return The Object goal-value of the instance at the given index.
     * @throws LearnerException If the Object goal is not available.
     */
    public Object           getObjectGoal(int ind) throws LearnerException;
    
    /**
     * Get the goal-class index of the instance at the given index.
     * @param ind The index
     * @return The goal-class index of the instance at the given index.
     * @throws LearnerException If the goal class index does not exist or is not available.
     */
    public int              getGoalClass(int ind) throws LearnerException;
    
    /**
     * Get the weight of the instance at the given index.
     * @param ind The index
     * @return The weight of the instance at the given index.
     */
    public double           getWeight(int ind);
    
    /**
     * Change the weight of the instance at the given index.
     * @param ind The index
     * @param w The new weight.
     */
    public void             setWeight(int ind, double w);
    
    /**
     * Change the goal-value of the instance at the given index.
     * @param ind The index
     * @param g The new goal-value
     */
    public void             setGoal(int ind, double g);
    
    /**
     * Change the Object goal-value of the instance at the given index.
     * @param ind The index
     * @param g The new Object goal-value.
     */
    public void             setObjectGoal(int ind, Object g);
    
    /**
     * Change the instance at the given index.
     * @param ind The index
     * @param in The new instance.
     */
    public void             setInstance(int ind, DoubleMatrix1D in);
    
    /**
     * Change the Object instance at the given index.
     * @param ind The index.
     * @param in The new Object instance.
     */
    public void             setObjectInstance(int ind, ObjectMatrix1D in);
    
    /**
     * Change the order and size of the instance set.
     * Reorder the instances according to the given array of indices.
     * @param ind The set of indices showing the new order and size of the set
     * @throws LearnerException If the re-ordering or resizing went wrong.
     */
    public void             reorder(int []ind) throws LearnerException;
    
    /**
     * Give the index of an instance with the given goal class.
     * @param gc The goal class to find.
     * @return The index of an instance with the given goal class.
     * @throws LearnerException If none can be found or an error occured during the search for one.
     */
    public int              getIndexWithGoalClass(int gc) throws LearnerException;
    
    /**
     * Give the index of an instance with the given goal value.
     * @param gv The goal value to find.
     * @return The index of an instance with the given goal value.
     * @throws LearnerException If none can be found or an error occured during the search.
     */
    public int              getIndexWithGoalValue(double gv) throws LearnerException;
    
    /**
     * Give the index of an instance with the given Object goal value.
     * @param gv The Object goal value to find.
     * @return The index of an instance with the given Object goal value.
     * @throws LearnerException If none can be found or an error occured during the search.
     */
    public int              getIndexWithObjectGoalValue(Object gv) throws LearnerException;
    
    /**
     * Clone the Presenter.
     * @return A clone of the presenter.
     * @throws CloneNotSupportedException If it could not be cloned.
     */
    public Object clone() throws CloneNotSupportedException;
}
