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


import org.shaman.dataflow.Transformation;
import org.shaman.dataflow.TransformationNetwork;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.LearnerException;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Transformation Network that Classifies</h2>
 * A network of transformation that contains pre-processing
 * transformations and a classifier as output.
 * Assumes there is 1 input port and 1 output port.
 *
 * <br>
 * Can be used to encapsulat often used combinations like
 * e.g.
 *       PCA -) Normalization -) MLP Classifier
 */
// **********************************************************\
// *              Classifier Network Base Class             *
// **********************************************************/
public class ClassifierNetwork extends TransformationNetwork implements Classifier
{
    // Train and Datamodel used during training
    private Presenter  trainData;
    private DataModel  dataModel;
    
    // State Persistence
    public String []getVersion() { return(new String[]{"classifier network", "1.0"}); }
    
    // **********************************************************\
    // *             Individual Transformation Access           *
    // **********************************************************/
    public Transformation getClassifier()
    {
        return(trans[trans.length-1]);
    }
    
    // **********************************************************\
    // *           Transformation Network Classification        *
    // **********************************************************/
    public int classify(DoubleMatrix1D din, double []confidence) throws LearnerException
    {
        boolean        done;
        Transformation trnow;
        DoubleMatrix1D innow;
        int            i;
        int            cl;
        
        // Cycle through pre-processing components until the classifier is reached.
        innow = din; done = false; cl = -1;
        for (i=0; (i<trans.length) && (!done); i++)
        {
            trnow = trans[i];
            if (trnow instanceof Classifier)
            {
                cl = ((Classifier)trnow).classify(innow, confidence);
                done = true;
            }
            else if (trnow instanceof Estimator) innow = ((Estimator)trnow).estimate(innow);
        }
        
        return(cl);
    }
    
    public int classify(DoubleMatrix1D din) throws LearnerException
    {
        return(classify(din, null));
    }
    
    
    public int classify(ObjectMatrix1D oin, double []confidence) throws LearnerException
    {
        return(-1);
    }
    
    public int classify(ObjectMatrix1D oin) throws LearnerException
    {
        return(classify(oin, null));
    }
    
    public boolean isSupervised() { return(true); }
    
    // **********************************************************\
    // *         Automatic Classifier Network Training          *
    // **********************************************************/
    public void train() throws LearnerException
    {
        int              i,j;
        Learner          learnnow;
        CachingPresenter prestrain;
        DoubleMatrix1D []intrain;
        
        try
        {
            // Train the pre-processing transformations and finally the classifier.
            //intrain   = new DoubleMatrix1D[trainData.getNumberOfInstances()];
            prestrain = (CachingPresenter)trainData.clone();
            for (i=0; i<trans.length; i++)
            {
                if (trans[i] instanceof Learner)
                {
                    learnnow = (Learner)trans[i];
                    learnnow.setTrainSet(prestrain);
                    learnnow.initializeTraining();
                    learnnow.train();
                    
                    intrain = prestrain.getInstances();
                    if      (learnnow instanceof Classifier)
                    {
                        // Just trained the final transformation. All done.
                    }
                    else if (learnnow instanceof Estimator)
                    {
                        try
                        {
                            // Train a pre-processing components. e.g. PCA, Normalization, Discretization.
                            for (j=0; j<intrain.length; j++) intrain[j] = ((Estimator)learnnow).estimate(intrain[j]);
                            prestrain.setDataModel(trans[i].getOutputDataModel(0));
                        }
                        catch(ConfigException ex) { throw new LearnerException(ex); }
                    }
                }
                else throw new LearnerException("Cannot train a Transformation that is not a learner : "+trans[i].getName());
            }
        }
        catch(CloneNotSupportedException ex) { throw new LearnerException("Cannot make a work copy of the training data presenter."); }
    }
    
    public void initializeTraining() throws LearnerException
    {
        // Nothing special here.
    }
    
    public void setTrainSet(Presenter _trainData) throws LearnerException
    {
        trainData = _trainData;
        dataModel = trainData.getDataModel();
        
        if (!(trainData instanceof CachingPresenter))
            throw new LearnerException("Cannot trans a Classifier Network on non-cached instances.");
    }
    
    public Presenter getTrainSet()
    {
        return(trainData);
    }
    
    
    // **********************************************************\
    // *                          Constructor                   *
    // **********************************************************/
    public ClassifierNetwork()
    {
        super();
        name        = "Classifier Network";
        description = "Network and pre-processing and one classifier transformation";
    }
}
