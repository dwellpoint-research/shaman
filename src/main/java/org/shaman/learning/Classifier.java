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
 *  <h2>Interface for a classification algorithm</h2>
 *  e.g. supervised classification using a MLP, unsupervised classification (clustering) with K-Mediods <br>
 **/
public interface Classifier extends Learner
{
    /** Output DataModel contains 1 field : the class */
    public static final int OUT_CLASS                       = 0;
    /** Output DataModel contains 2 fields : the class and the confidence in this class */
    public static final int OUT_CLASS_AND_CONFIDENCE        = 1;
    /** Output DataModel contains 1+number of classes fields : the class and the vector of confidences in all classes */
    public static final int OUT_CLASS_AND_CONFIDENCE_VECTOR = 2;
    
    public static final int    []OUT_CLASS_VALUES = new int[]
                                                            { OUT_CLASS, OUT_CLASS_AND_CONFIDENCE, OUT_CLASS_AND_CONFIDENCE_VECTOR };
    public static final String []OUT_CLASS_NAMES = new String[]
                                                              { "class",   "class and confidence"  , "class and confidence vector"   };
    
    /**
     *  Classify the given instance. Returns it's goal class index or -1 if it cannot classify.
     *  @param instance The instance to classify
     *  @returns The classification of the instance. -1 if cannot classify.
     *  @throws LearnerException if an error occurred during classification
     */
    public int classify(DoubleMatrix1D instance) throws LearnerException;
    
    /**
     * Classify the given instance. Returns it's goal class index or -1 if it cannot classify.
     * Also gives the confidence in all possible classifications.
     * @param instance The instance to classify
     * @param confidence Buffer of 'number of classes' doubles,
     *        that is filled with the confidence of the classification algorithm in the classes.
     * @returns The classification of the instance. This class has the highest confidence. -1 if cannot classify.
     * @throws LearnerException if an error occurred during classification
     */
    public int classify(DoubleMatrix1D instance, double []confidence) throws LearnerException;
    
    /**
     *  Classify the given Object instance. Returns it's goal class index or -1 if it cannot classify.
     *  @param instance The Object instance to classify
     *  @returns The classification of the Object instance. -1 if cannot classify.
     *  @throws LearnerException if an error occurred during classification
     */
    public int classify(ObjectMatrix1D instance) throws LearnerException;
    
    /**
     * Classify the given Object instance. Returns it's goal class index or -1 if it cannot classify.
     * Also gives the confidence in all possible classifications.
     * @param instance The Object instance to classify
     * @param confidence Buffer of 'number of classes' doubles,
     *        that is filled with the confidence of the classification algorithm in the classes.
     * @returns The classification of the Object instance. This class has the highest confidence. -1 if cannot classify.
     * @throws LearnerException if an error occurred during classification
     */
    public int classify(ObjectMatrix1D instance, double []confidence) throws LearnerException;
}