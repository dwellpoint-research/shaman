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

import cern.jet.stat.Probability;


/**
 * <h2>Comparing 2 Machine Learning Algorithms</h2>
 *
 * Uses stastics to compare the results of repeated validation of
 * 2 machine learning algorithms of the same type on the same
 * instance set.
 * <br>
 * <i>Only Classifiers are supported for now<i>
 *
 * <br>
 * <i>Tom M. Mitchell (1997), Machine Learning, Chapter 5</i><br>
 * <i>Witten I., Frank E. (2000), Data Mining, Chapter 5</i>
 */

// **********************************************************\
// *       Comparing 2 Machine Learning Algorithms          *
// **********************************************************/
public class LearnerComparison
{
    // Comparison Parameters
    private Presenter instances;           // The set of instances to work on.
    private Learner   learner1;            // First Learner
    private Learner   learner2;            // Second Learner
    private int       numRepeats;          // Number of times to repeat the cross-validation using the 2 learners
    private int       numFolds;            // Number of folds in 1 cross-validation
    private boolean   skipTrain;           // Do not train during cross-validation. Just test.
    
    // Comparison Buffers
    private int        learnerType;         // Type of both learners. As in Validation.LEARNER_x
    private double     []clerr1;              // Errors of the Cross-Validation Experiments on Learner1 and 2
    private double     []clerr2;
    private double     []errdif;              // Difference Between the Paired Cross-Validation experiment
    private double     md,sd;                 // Mean and Variance of the Differences
    private Validation [][]val;               // The Cross-Validations of Learner 1 and 2
    
    // Progress indication
    private int     numRepeatsDone;
    
    // **********************************************************\
    // *            Repeated Cross Validations Results          *
    // **********************************************************/
    public double giveLearner1Error()
    {
        int    i;
        double err;
        
        err = 0;
        for (i=0; i<clerr1.length; i++) err += this.clerr1[i];
        err /= clerr1.length;
        
        return(err);
    }
    
    public double giveLearner2Error()
    {
        int    i;
        double err;
        
        err = 0;
        for (i=0; i<clerr1.length; i++) err += this.clerr2[i];
        err /= clerr2.length;
        
        return(err);
    }
    
    public double giveLearner1Variance()
    {
        int    i;
        double mean, var;
        
        var  = 0;
        mean = giveLearner1Error();
        for (i=0; i<clerr1.length; i++)
        {
            var += (clerr1[i]-mean)*(clerr1[i]-mean);
        }
        var = Math.sqrt(var);
        
        return(var);
    }
    
    public double giveLearner2Variance()
    {
        int    i;
        double mean, var;
        
        var  = 0;
        mean = giveLearner1Error();
        for (i=0; i<clerr2.length; i++)
        {
            var += (clerr2[i]-mean)*(clerr2[i]-mean);
        }
        var = Math.sqrt(var);
        
        return(var);
    }
    
    // **********************************************************\
    // *                 Comparing 2 Classifiers                *
    // **********************************************************/
    /**
     * Return the classifier that performed best (had lowest classification error)
     * on the repeated cross-validation tests.
     * @return The best classifier
     */
    public Classifier giveBestClassifier()
    {
        if (md < 0) return((Classifier)learner1);
        else        return((Classifier)learner2);
    }
    
    /**
     * Calculate and return the probability that the 2 classifier are different,
     * have different classification abilities.
     * @return The probability the classifiers are different.
     */
    public double giveDifferentProbability()
    {
        double t, tp;
        
        if ((md != 0) && (numRepeatsDone > 0))
        {
            // Calculate t statictic
            t = md / Math.sqrt(sd/numRepeatsDone);
            
            // Calculate how probable it is that the 2 are different. (area under distribution till t)
            tp = Probability.studentT(numRepeatsDone-1, Math.abs(t));
        }
        else tp = 0;
        
        return(tp);
    }
    
    /**
     * Calculate if you can be 'conf' percent sure, the 2 classifiers are different.
     * @param conf The confidence you want in [0,1]
     * @return <code>true</code> if the classifier are 'conf'% sure to be different.
     */
    public boolean areClassifiersDifferent(double conf)
    {
        boolean same;
        double  t, z;
        
        if ((md == 0) || (numRepeatsDone == 0)) same = true;
        else
        {
            // Calculate t statictic
            t = md / Math.sqrt(sd/numRepeatsDone);
            
            // Calculate required z value
            z = Probability.studentTInverse(1-conf, numRepeatsDone-1);
            
            if ((t > z) || (t < -z)) same = false;
            else                     same = true;
        }
        
        return(same);
    }
    
    /**
     * Perform a number of repeated n-fold cross-validations on the 2 classifiers.
     */
    public void testClassifiers() throws LearnerException
    {
        int        i;
        double   []cvpar;
        Validation val1;
        Validation val2;
        
        // Make some buffers
        numRepeatsDone = 0;
        cvpar          = new double[]{numFolds};
        clerr1         = new double[numRepeats];
        clerr2         = new double[numRepeats];
        val            = new Validation[2][numRepeats];
        // Do the repeated cross-validation
        for (i=0; i<numRepeats; i++)
        {
            // Cross-Validation with the same instance set and cross-validation parameters.
            val1 = new Validation(instances, learner1);
            val2 = new Validation(instances, learner2);
            val1.create(Validation.SPLIT_CROSS_VALIDATION, cvpar);
            val1.setSkipTrain(skipTrain);
            val2.setSplitType(val1.getSplitType());
            val2.setSplit(val1.getSplit());
            val2.setNumberOfFolds(val1.getNumberOfFolds());
            val2.setSet(val1.getSet());
            val2.setSkipTrain(skipTrain);
            val[0][i] = val1;
            val[1][i] = val2;
            
            // Cross-Validate the first model.
            val1.test();
            // Cross-Validate the second model
            val2.test();
            // Remember the Classification Error results of this Cross-Validation Repeat.
            clerr1[i] = val1.getValidationClassifier().getClassificationError();
            clerr2[i] = val2.getValidationClassifier().getClassificationError();
            
            System.out.println("Repeat "+numRepeatsDone+" error 1 "+clerr1[i]+" error 2 "+clerr2[i]);
        }
        
        // Pre-calculate some things...
        prepareStatistics(this.clerr1, this.clerr2, this.numRepeatsDone);
    }
    
    
    public void prepareStatistics(double []clerr1, double []clerr2, int numRepeatsDone)
    {
        int i;
        
        this.clerr1         = clerr1;
        this.clerr2         = clerr2;
        this.numRepeats     = numRepeatsDone;
        this.numRepeatsDone = numRepeatsDone;
        this.errdif         = new double[this.numRepeats];
        
        // Make the paired classification error differences
        for (i=0; i<numRepeatsDone; i++) this.errdif[i] = clerr1[i] - clerr2[i];
        
        // Calculate mean and variance
        this.md = 0;
        this.sd = 0;
        if (this.numRepeatsDone > 0)
        {
            for (i=0; i<numRepeatsDone; i++) this.md += errdif[i];
            this.md /= numRepeatsDone;
            for (i=0; i<numRepeatsDone; i++) sd += (this.errdif[i] - this.md)*(this.errdif[i] - this.md);
        }
    }
    
    
    // **********************************************************\
    // *     Initialize the comparison between 2 learners       *
    // **********************************************************/
    /**
     * Initialize a comparison between the 2 given learners.
     * @param _instances The instances to use.
     * @param _learner1 The first learner
     * @param _learner2 The second learner
     * @param _numFolds   Number of folds in every cross-validation.
     * @param _numRepeats Number of repeated cross-validations to do.
     * @throws LearnerException If the two learner types do not match. Or they're not classifiers.
     */
    public void init(Presenter _instances, Learner _learner1, Learner _learner2, int _numFolds, int _numRepeats) throws LearnerException
    {
        int type1, type2;
        
        instances  = _instances;
        learner1   = _learner1;
        learner2   = _learner2;
        numRepeats = _numRepeats;
        numFolds   = _numFolds;
        
        type1 = Validation.findLearnerType(learner1, instances);
        type2 = Validation.findLearnerType(learner2, instances);
        
        if (type1 == type2) learnerType = type1;
        else throw new LearnerException("Can't compare 2 different types of learners. Go away.");
        if (learnerType == Learner.LEARNER_OTHER)
            throw new LearnerException("Can't compare 2 learners of unknown type.");
        if (numRepeats <= 1)
            throw new LearnerException("Can't calculate statistics on only 1 test. Take e.g. 10 repeats, please.");
        
        if (learnerType != Learner.LEARNER_CLASSIFIER)
            throw new LearnerException("Only suppor CLASSIFIERS for now... Come back later.");
    }
    
    /**
     * Get the cross-validation of the given learner and repeat.
     * @param learner '1' for the first Learner, '2' for the second Learner
     * @param repeat The repeat number [1, number of repeats].
     * @return The cross-validation. <code>null</code> if the learner or repeat number are out of range.
     */
    public Validation getValidation(int learner, int repeat)
    {
        Validation valnow;
        
        valnow = null;
        if ((learner == 1) && (repeat <= numRepeatsDone)) valnow = val[0][repeat-1];
        if ((learner == 2) && (repeat <= numRepeatsDone)) valnow = val[1][repeat-1];
        
        return(valnow);
    }
    
    /**
     * Get the number of cross-validation repeats to perform for this test.
     * @return The number of repeats of the cross-validation.
     */
    public int getNumberOfRepeats()
    {
        return(numRepeats);
    }
    
    /**
     * Give the first learner that is being compared.
     * @return The first learner
     */
    public Learner getLearner1()
    {
        return(learner1);
    }
    
    /**
     * Give the second learner that is being compared
     * @return The second learner
     */
    public Learner getLearner2()
    {
        return(learner2);
    }
    
    /**
     * Set wheter the comparison should not train during cross-validation.
     * @param _skipTrain If <code>true</code> the cross-validations will skip the (re)training of the models.
     */
    public void setSkipTrain(boolean _skipTrain)
    {
        this.skipTrain = _skipTrain;
    }
    
    public void setNumberOfRepeats(int _numRepeats)
    {
        this.numRepeats = _numRepeats;
    }
    
    /**
     * Get if the cross-validations should skip the (re)training of the models.
     * @return <code>true</code> if the models won't be trained, just tested.
     */
    public boolean getSkipTrain() { return(skipTrain); }
    
    /**
     * Get the number of folds in the repeated cross-validation.
     * @return The number of folds in the repeated cross-validation.
     */
    public int getNumberOfFolds()
    {
        return(numFolds);
    }
    
    /**
     * Default comparison creation.
     * 10 folds, 10 repeats.
     */
    public LearnerComparison()
    {
        super();             // Monitored Object constructor
        skipTrain  = false;
        numFolds   = 10;
        numRepeats = 10;
    }
}
