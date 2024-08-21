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


import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelPropertyLearning;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;

import cern.jet.random.Uniform;


/**
 * <h2>Machine Learning Algorithm Validation</h2>
 *
 * Testing algorithms for machine-learning flows.
 * Supports simple train/test set split-up, n-fold cross validation
 * and leave-one-out testing. Contains links to classes that have
 * a lot of reporting methods to statistically analyse the performance
 * of the machine-learning under interrogation.
 *
 * <br>
 * <i>Tom M. Mitchell (1997), Machine Learning, Chapter 5</i><br>
 * <i>Witten I., Frank E. (2000), Data Mining, Chapter 5</i>
 */

// **********************************************************\
// *       Advanced Testing and Test Result Reporting       *
// **********************************************************/
public class Validation implements Tester
{
    // Type of Instance Set split-up
    /** Split the instance set up in a Train and a Test set. */
    public static final int SPLIT_TRAIN_TEST       = 0;
    /** 'n'-Fold cross validation. Split up the instance set in a number of 'folds' and train/test on each fold. */
    public static final int SPLIT_CROSS_VALIDATION = 1;
    /** Leave one out validation. Train/Test on all subsets of the instance set with 1 instances left out. */
    public static final int SPLIT_LEAVE_ONE_OUT    = 2;
    
    private int       splitType;           // How should the instance set be split up?
    private double  []par;                 // Parameters (e.g. 'n' in SPLIT_CROSS_VALIDATION)
    private boolean   skipTrain;           // Skip the training of the model in the cross-validation.
    
    private Presenter instances;           // The set of instances to work on.
    private Learner   learner;             // Trains the model on the set(s) created by this class.
    private int       []split;             // The fold-number in which the instance is assigned to the test-set
    
    // The Validation Algorithms and Reporting Classes
    private ValidationClassifier valclas;      // Supervised Classifier
    private ValidationClusterer  valclus;      // Unsupervised Classifier
    private ValidationEstimator  valest;       // (Supervised) Estimator
    
    // Internally Used Buffer
    private int              learnerType = -1;       // Type of learner. Depends on implemented interface(s) and datamodel goal type.
    private int              numFolds;               // Number of folds (1 if train/test, n in n-fold cross validation, number of instances if leave one out)
    private Presenter        trainSet;
    private Presenter        testSet;
    
    // Progress indicator
    private int              currentFold;            // Fold currently being processed. (-1 when not started)
    
    // **********************************************************\
    // *                Simple Reporting Methods                *
    // **********************************************************/
    /**
     * Get the number of instances that are tested by this validation.
     * @return The number of instances that are tested.
     */
    public int getNumberOfTests()
    {
        int i;
        int count;
        
        count = 0;
        for (i=0; i<this.split.length; i++) if (this.split[i] > 0) count++;
        
        return(count);
    }
    
    /**
     * Get the number of instances that are tested in the given fold.
     * @param fold The fold number (starts at 1)
     * @return The number of instances in the fold
     */
    public int getNumberOfTests(int fold)
    {
        int i, count;
        
        count = 0;
        for (i=0; i<this.split.length; i++) if (this.split[i] == fold) count++;
        
        return(count);
    }
    
    // **********************************************************\
    // *   Common Validation Logic for all types of Learners    *
    // **********************************************************/
    /**
     * Perform the testing procedures on the Learner
     * @throws LearnerException If something went wrong while testing.
     */
    public void test() throws LearnerException
    {
        int            i;
        Presenter      []tt;
        Presenter      train, test;
        
        // Make the right kind of test result buffers
        initTest();
        
        // Train and test the model a number of times. Remember the results.
        for (i=1; i<=this.numFolds; i++)
        {
            // Create the next train/test set combination.
            tt    = nextTrainTest(i);
            train = tt[0];
            test  = tt[1];
            
            // Train the learner on this fold if it is necessary.
            if (!skipTrain)
            {
                this.learner.setTrainSet(train);
                this.learner.initializeTraining();
                this.learner.train();
            }
            
            if      (this.learnerType == Learner.LEARNER_CLASSIFIER) valclas.testClassifier(i, test);
            else if (this.learnerType == Learner.LEARNER_CLUSTERER)  valclus.testClusterer(i, test);
            else if (this.learnerType == Learner.LEARNER_ESTIMATOR)  valest.testEstimator(i, test);
            
            this.currentFold = i;
        }
        
        // Finish compiling to test data
        endTest();
    }
    
    private void initTest() throws LearnerException
    {
        try
        {
            if      (learnerType == Learner.LEARNER_CLASSIFIER)
            {
                // Confusion, ROC, etc...
                this.valclas = new ValidationClassifier();
                this.valclas.initTest(this, instances, split, numFolds, (Classifier)learner);
            }
            else if (learnerType == Learner.LEARNER_CLUSTERER)
            {
                // Cluster Membership, etc...
                this.valclus = new ValidationClusterer();
                this.valclus.initTest(this, instances, split, numFolds, (Classifier)learner);
            }
            else if (learnerType == Learner.LEARNER_ESTIMATOR)
            {
                // Function Estimator Test Results...
                this.valest = new ValidationEstimator();
                this.valest.initTest(this, instances, split, numFolds, (Estimator)learner);
            }
            
            this.currentFold = -1;
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
    }
    
    private void endTest()
    {
        if      (learnerType == Learner.LEARNER_CLASSIFIER)
        {
            this.valclas.endTest();
        }
        else if (learnerType == Learner.LEARNER_CLUSTERER)
        {
            this.valclus.endTest();
        }
        else if (learnerType == Learner.LEARNER_ESTIMATOR)
        {
            this.valest.endTest();
        }
    }
    
    /**
     * Give the supervised classification validation
     * result reporting class.
     */
    public ValidationClassifier getValidationClassifier()
    {
        return(this.valclas);
    }
    
    /**
     * Give the unsupervised classification validation
     * result reporting class.
     */
    public ValidationClusterer getValidationClusterer()
    {
        return(this.valclus);
    }
    
    /**
     * Give the supervised function estimator validation
     * result reporting class
     */
    public ValidationEstimator getValidationEstimator()
    {
        return(this.valest);
    }
    
    // **********************************************************\
    // *              Test and Train Set Generation             *
    // **********************************************************/
    private Presenter []nextTrainTest(int foldPos) throws LearnerException
    {
        Presenter []tt;
        
        tt = null;
        if     (this.splitType == SPLIT_TRAIN_TEST)
        {
            if (foldPos == 1)
            {
                tt    = new Presenter[2];
                if ((this.trainSet == null) && (this.testSet == null)) tt = makeTrainTest(1);
                else
                {
                    tt = new Presenter[] { this.trainSet, this.testSet };
                }
            }
        }
        else if ((this.splitType == SPLIT_CROSS_VALIDATION) || (this.splitType == SPLIT_LEAVE_ONE_OUT))
        {
            if (foldPos <= (int)this.par[0]) tt = makeTrainTest(foldPos);
            foldPos++;
        }
        
        return(tt);
    }
    
    private Presenter []makeTrainTest(int fold) throws LearnerException
    {
        int       i;
        Presenter []tt;
        int       numtrain, numtest, postrain, postest;
        int       []itrain;
        int       []itest;
        
        try
        {
            tt    = new Presenter[2];
            tt[0] = (Presenter)this.instances.clone();
            tt[1] = (Presenter)this.instances.clone();
            
            // Count # train and # test instances
            numtrain = 0; numtest = 0;
            for (i=0; i<this.split.length; i++)
            {
                if (this.split[i] == fold) numtest++;
                else                       numtrain++;
            }
            
            // Make index arrays of the test and train instances
            itrain = new int[numtrain]; postrain = 0;
            itest  = new int[numtest];  postest  = 0;
            for (i=0; i<this.split.length; i++)
            {
                if (this.split[i] == fold) itest [postest++]  = i;
                else                       itrain[postrain++] = i;
            }
            
            // Shrink the original set into the disjunct train and test set
            tt[0].reorder(itrain);
            tt[1].reorder(itest);
        }
        catch(CloneNotSupportedException ex) { throw new LearnerException(ex); }
        
        return(tt);
    }
    
    // **********************************************************\
    // *    Split the set up the set according to the type      *
    // **********************************************************/
    /**
     * Prepare a test procedure of the given type and parameters.
     * Split the instance set up. Prepare testing buffers.
     * @param _splitType The type of validation procedure to follow
     * @throws LearnerException If something went wrong.
     * @see #SPLIT_CROSS_VALIDATION
     * @see #SPLIT_LEAVE_ONE_OUT
     * @see #SPLIT_TRAIN_TEST
     */
    public void create(int _splitType, double []_par) throws LearnerException
    {
        int   i,j;
        int   numtest, tpos;
        
        this.splitType = _splitType;
        this.par       = _par;
        
        this.split = new int[this.instances.getNumberOfInstances()];
        
        // Split the set up in train/test sets.
        if (this.splitType == SPLIT_TRAIN_TEST)
        {
            // par[0] = % of the set to use for testing.
            for (i=0; i<this.split.length; i++) this.split[i] = 0;
            
            // Select the correct amount of instances for testing
            numtest = (int)(this.split.length*this.par[0]);
            for (i=0; i<numtest; i++)
            {
                tpos = Uniform.staticNextIntFromTo(0, this.split.length-1);
                while (this.split[tpos] == 1)
                {
                    tpos++;
                    if (tpos == this.split.length) tpos = 0;
                }
                this.split[tpos] = 1;
            }
            this.numFolds = 1;
        }
        else if (this.splitType == SPLIT_CROSS_VALIDATION)
        {
            // par[0] = 'n' of n-fold cross validation
            int fold;
            int foldsize;
            
            for (i=0; i<this.split.length; i++) this.split[i] = -1;
            
            // Divide the set of in 'n' folds
            fold     = (int)this.par[0];
            foldsize = (int)(this.split.length / this.par[0]);
            for (i=1; i<=fold; i++)
            {
                for (j=0; j<foldsize; j++)
                {
                    tpos = Uniform.staticNextIntFromTo(0, this.split.length-1);
                    while (this.split[tpos] >= 0)
                    {
                        tpos++;
                        if (tpos == this.split.length) tpos = 0;
                    }
                    this.split[tpos] = i;
                }
            }
            for (i=0; i<this.split.length; i++) if (this.split[i] == -1) this.split[i] = fold; // Adjust for foldsize discretization.
            this.numFolds = fold;
        }
        else if (this.splitType == SPLIT_LEAVE_ONE_OUT)
        {
            for (i=1; i<=this.split.length; i++) this.split[i] = i;
            this.numFolds = this.split.length;
        }
        
        this.trainSet = null;
        this.testSet  = null;
    }
    
    /**
     * Expicitly set the Train and Test set when doing a Train/Test splitup.
     * @param _trainSet Use this train set.
     * @param _testSet Use this test set.
     * @throws LearnerException If the validation type is not SPLIT_TRAIN_TEST.
     */
    public void setTrainTestSet(Presenter _trainSet, Presenter _testSet) throws LearnerException
    {
        if (this.splitType == SPLIT_TRAIN_TEST)
        {
            this.trainSet = _trainSet;
            this.testSet  = _testSet;
        }
        else throw new LearnerException("Cannot set train and test set for a non-SPLIT_TRAIN_TEST validation.");
    }
    
    /**
     * Modify the number of folds.
     * @param _numFolds The new number of folds.
     */
    public void setNumberOfFolds(int _numFolds)
    {
        this.numFolds = _numFolds;
        this.par      = new double[]{this.numFolds};
    }
    
    /**
     * Set if the cross-validation should skip the training of the model.
     * Just test the stability of the given model on various parts of the instance set.
     * @param _skipTrain If <code>true</code> the cross-validation will not (re)train the model in the fols.
     */
    public void setSkipTrain(boolean _skipTrain) { this.skipTrain = _skipTrain; }
    
    /**
     * Get if the cross-validation should skip the (re)training of the model.
     * @return <code>true</code> if the cross-validation will skip the training.
     */
    public boolean getSkipTrain() { return(this.skipTrain); }
    
    /**
     * Get the number of folds in the current validation configuration.
     * @return The number of folds. (1 for train/test split, 'n' for cross-validation, size of instance set in leave one out.)
     */
    public int getNumberOfFolds() { return(this.numFolds); }
    
    /**
     * Modify the type of validation to use.
     * @param _splitType The type of validation to use.
     */
    public void setSplitType(int _splitType) { this.splitType = _splitType; }
    /**
     * Get the type of validation used.
     * @return The type of validation used.
     * @see #SPLIT_CROSS_VALIDATION
     * @see #SPLIT_LEAVE_ONE_OUT
     * @see #SPLIT_TRAIN_TEST
     */
    public int  getSplitType()               { return(this.splitType); }
    /**
     * Explicitly set the way how to split up the instances in train and test sets.
     * @param _split The value at 'n' is the fold in which instance 'n' belongs to a test set.
     */
    public void setSplit(int []_split)       { this.split = _split; }
    /**
     * Get the exact way of how the validation will split up the instances in train and test sets.
     * @return split The array containing the test fold numbers.
     */
    public int  []getSplit()                 { return(this.split); }
    
    // **********************************************************\
    // *                  Progress Tracking                     *
    // **********************************************************/
    public int getProgress() { return(this.currentFold); }
    
    // **********************************************************\
    // *                  Tester Interface                      *
    // **********************************************************/
    /**
     * Modify the set of instances to use during validation.
     * @param _instances The new set of instances.
     */
    public void      setSet(Presenter _instances) { this.instances = _instances; }
    /**
     * Get the set of instances used during validation.
     * @return The set of instances used during validation.
     */
    public Presenter getSet()                     { return(this.instances); }
    
    // **********************************************************\
    // *            Construction of a Validation                *
    // **********************************************************/
    /**
     * Find out which type of machine-learning the given Learner/Presenter combination is.
     * @param learner The learner that will train on the given set of instances.
     * @param instances The set of instances the learner will train on.
     * @return The type of learner.
     * @see #LEARNER_CLASSIFIER
     * @see #LEARNER_CLUSTERER
     * @see #LEARNER_ESTIMATOR
     * @see #LEARNER_OTHER
     */
    static int findLearnerType(Learner learner, Presenter instances)
    {
        int learnerType;
        
        if ((learner instanceof Classifier) && (learner instanceof Estimator))
        {
            // Break tie by looking at the datmodel of the instances set
            DataModel dm;
            DataModelPropertyLearning learn;
            
            dm    = instances.getDataModel();
            learn = dm.getLearningProperty();
            if (learner.isSupervised() && learn.getHasGoal())
            {
                if (dm.getAttribute(learn.getGoalIndex()).getGoalType() == Attribute.GOAL_CLASS)
                    learnerType = Learner.LEARNER_CLASSIFIER;
                else learnerType = Learner.LEARNER_ESTIMATOR;
            }
            else learnerType = Learner.LEARNER_OTHER; // Dunno.
        }
        else if (learner instanceof Classifier)
        {
            if   (learner.isSupervised()) learnerType = Learner.LEARNER_CLASSIFIER;
            else                          learnerType = Learner.LEARNER_CLUSTERER;
        }
        else if (learner instanceof Estimator) learnerType = Learner.LEARNER_ESTIMATOR;
        else learnerType = Learner.LEARNER_OTHER;  // Possible because there are other learners as well... e.g. Normalization
        
        return(learnerType);
    }
    
    /**
     * Make a validation for the given set of instances and the machine learning object.
     * @param _instances The set of instances
     * @param _trainer The machine learner to validate.
     */
    public Validation(Presenter _instances, Learner _trainer)
    {
        super();
        this.instances   = _instances;
        this.learner     = _trainer;
        this.trainSet    = null;
        this.testSet     = null;
        this.learnerType = findLearnerType(learner, instances);
    }
}
