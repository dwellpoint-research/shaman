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
 * <h2>Interface for an estimation algorithm</h2>
 * Interface for an (un)supervised density/function estimation algorithm. <br>
 * e.g. supervised function regression using MLP. <br>
 *      unsupervised density estimation using KMER. <br>
 **/
public interface Estimator extends Learner
{
    /**
     *  Estimate the function value for the given instance. <br>
     *  @param instance The instance
     *  @return The function estimate of the instance. <code>Double.NaN</code> if unable to estimate.
     *  @throws LearnerException if something goes wrong while estimating the instance's function value.
     */
    public DoubleMatrix1D estimate(DoubleMatrix1D instance) throws LearnerException;
    
    /** Estimate the given function/density for the given instance.
     *  @param instance The instance
     *  @param conf Buffer that is filled with the confidence of the function estimate.
     *  @return The function estimation of the instance. <code>Double.NaN</code> if unable to estimate.
     *  @throws LearnerException if something goes wrong while estimating the instance's function value.
     */
    public DoubleMatrix1D estimate(DoubleMatrix1D instance, double []conf) throws LearnerException;
    
    /**
     * Estimate the error of a self-supervised estimation.
     * Only used in unsupervised estimators. e.g. Discretization, KMER
     * @param instance The instance for which to calculate the error.
     * @return The distance between the estimated and the input vector.
     * @throws LearnerException if something goes wrong
     */
    public double estimateError(DoubleMatrix1D instance) throws LearnerException;
    
    /**
     *  Estimate the function value for the given Object instance. <br>
     *  @param instance The Object instance
     *  @return The function estimate of the instance. <code>null</code> if unable to estimate.
     *  @throws LearnerException if something goes wrong while estimating the instance's function value.
     */
    public ObjectMatrix1D estimate(ObjectMatrix1D instance) throws LearnerException;
    
    /** Estimate the given function/density for the given Object instance.
     *  @param instance The Object instance
     *  @param conf Buffer that is filled with the confidence of the function estimate.
     *  @return The function estimation of the instance. <code>null</code> if unable to estimate.
     *  @throws LearnerException if something goes wrong while estimating the instance's function value.
     */
    public ObjectMatrix1D estimate(ObjectMatrix1D instance, double []conf) throws LearnerException;
    
    /**
     * Estimate the error of a self-supervised estimation.
     * Only used in unsupervised estimators. e.g. Keyword extraction
     * @param instance The Object instance for which to calculate the error.
     * @return The distance between the estimated and the input vector.
     * @throws LearnerException if something goes wrong
     */
    public double estimateError(ObjectMatrix1D instance) throws LearnerException;
}
