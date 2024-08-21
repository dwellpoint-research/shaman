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
 * <h2>Data Presentation in Batches</h2>
 * Presents data by recycling it's initial data or re-reading its datasource
 * every time a new batch is requested. Can balance the instances according
 * to their goal class membership or goal value. Can take a sub-sample of
 * the available instances. <p>
 * Used by batch-training algorithms e.g. MLP, KMER neural networks.
 **/
public interface BatchPresenter extends Presenter
{
    // Goal Balancing
    /** Don't do any goal balancing.  */
    public static final int GOAL_BALANCE_NONE        = 0;  // Do not balance the amount of goal/non-goal instances
    /** Balance (stratify) the number of instances of all goal classes. */
    public static final int GOAL_BALANCE_CLASS       = 2;  // Balance goal/non-goal according to the goal classes
    /**
     *  Balance (stratify) the number of instance of all goal classes.
     *  Also balance the number of instances per goal value in all goal classes.
     **/
    public static final int GOAL_BALANCE_CLASS_VALUE = 3;  // First balance on goal class and also balance internally in the goal class
    
    /** All goal balance methods as integer constants. */
    static final int    []GOAL_BALANCE_VALUES = new int[]  { GOAL_BALANCE_NONE, GOAL_BALANCE_CLASS, GOAL_BALANCE_CLASS_VALUE };
    /** All goal balance methods as human readable strings. */
    static final String []GOAL_BALANCE_NAMES = new String[]{  "none",            "class",            "class and value" };
    
    // Batch Refresh
    /** Reorder the instances from the source every batch. */
    public static final int BATCH_REORDER = 0;  // Reorder the instances every batch get.
    /** Refresh the data (read new data) every batch. */
    public static final int BATCH_REFRESH = 1;  // Read a new set of instances every batch get.
    
    /** All batch types as integer constants. */
    static final int    []BATCH_TYPE_VALUES = new int[]  { BATCH_REORDER, BATCH_REFRESH };
    /** All batch types as human readable strings. */
    static final String []BATCH_TYPE_NAMES = new String[]{ "reorder",     "refresh"};
    
    /**
     * Get the next batch of instances.
     * @throws LearnerException If something goes wrong while getting the new instances.
     */
    public void   nextBatch() throws LearnerException; // Create the next batch of instances
    
    /**
     * Set the type of balancing to do.
     * @param _balance Type of balance.
     * @see #GOAL_BALANCE_NONE
     * @see #GOAL_BALANCE_CLASS
     * @see #GOAL_BALANCE_CLASS_VALUE
     */
    public void   setBalance(int _balance);          // Batch parameters access
    /**
     * Get the kind of balancing done.
     * @return The kind of balancing.
     */
    public int    getBalance();
    /**
     * Set the type of batch type to implements.
     * @param _batchType The type of batch type.
     * @see #BATCH_REFRESH
     * @see #BATCH_REORDER
     */
    public void   setBatchType(int _batchType);
    /**
     * Get the type of batch type used.
     * @return The type of batch type.
     */
    public int    getBatchType();
    /**
     * Set the fraction of the total-instance to subsample.
     * @param _sampleFraction sub-sample fraction.
     */
    public void   setSampleFraction(double _sampleFraction);
    /**
     * Get the sub-sample fraction.
     * @return The sub-sample fraction.
     */
    public double getSampleFraction();
}
