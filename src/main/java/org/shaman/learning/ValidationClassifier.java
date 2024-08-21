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

import java.io.FileWriter;
import java.util.Arrays;

import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelPropertyLearning;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;
import cern.jet.stat.Probability;


/**
 * <h2>Test and Reporting Algorithms for Classifier</h2>
 *
 * Statistically founded test procedures and reporting for
 * classification algorithms.
 *
 * <br>
 * <i>Tom M. Mitchell (1997), Machine Learning, Chapter 5</i><br>
 * <i>Witten I., Frank E. (2000), Data Mining, Chapter 5</i>
 */

// **********************************************************\
// *       Classifier Testing and Test Result Reporting     *
// **********************************************************/
public class ValidationClassifier
{
    // **********************************************************\
    // *      Statistically Founded Test for Classifiers        *
    // **********************************************************/
    // General Data about the association Validation
    private Validation  val;                // The Validation to which this test belongs
    private Presenter   instances;          // The complete set of instances (if possible)
    private Classifier  cl;                 // The Classifier used in the tests
    private DataModel   dataModel;          // DataModel of the Data
    private int    numTest;                 // Number of instances tested
    private int  []split;                   // How to Split up the total instances set
    private int    numFolds;                // Number of Folds
    
    // Cost-Sensitive Reporting
    private double [][]costMatrix;          // Cost Matrix
    
    // Classification Test Buffers
    private int    numClass;                // Number of classes
    private int    [][]classTest;           // Results of classification tests folds (should match with corresponding goal classes)
    private int    []classOut;              // Test result as classification output.
    private int    []classGoal;             // Expected test result. (if classGoal[i] == classOut[i] then classError[i] = 0)
    private byte   []classError;            // Test correctness (0 = incorrect, 1 = correct, -1 = couldn't classify)
    private double [][]classConfusion;      // Classification confusion matrix
    private int    []classCount;            // Number of instances of the various classes
    private ClassConfidence []confidence;   // Positive class confidence table. For lift charts/ROC curves, etc...
    
    // **********************************************************\
    // *             Cost Sensitive Reporting                   *
    // **********************************************************/
    /**
     * Give the gain chart over all folds from the .
     * @param GAINLEN The number of points in the chart.
     * @param vbeg Begin percentage on the X-axis of the chart.
     * @param vend End percentage on the X-axis of the chart.
     * @return The ROC curve as double[] indexed by the x-value, containing the y-values.
     * @throws LearnerException If an error occuring while generating or saving the ROC curve.
     */
    public double []getGainChart(int GAINLEN, double vbeg, double vend) throws LearnerException
    {
        int             i,j,k;
        double          [][]gainfold;
        double          []gain;
        int             not, ind, pos;
        double          nt, cavg, cnow, gainnow;
        double          x;
        double          xstep;
        ClassConfidence []confbuf;
        DataModelPropertyLearning learn;
        
        learn = dataModel.getLearningProperty();
        
        try
        {
            // Check classification appropriateness.
            if (dataModel.getAttribute(learn.getGoalIndex()).getNumberOfGoalClasses() != 2)
                throw new LearnerException("Cannot generate a Gain Chart of a multi-class classification.");
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
        
        // Make buffers for the ROC curves of the different folds as well as for the combines one.
        gainfold = new double[numFolds][];
        for (i=0; i<gainfold.length; i++) gainfold[i] = new double[GAINLEN];
        gain = new double[GAINLEN];
        
        // Iterate over the folds
        for (i=1; i<=numFolds; i++)
        {
            // Count the number of succesfully tested instances in this fold and the number of positives
            not = 0; cavg = 0;
            for (j=0; j<split.length; j++) if ((split[j] == i) && (classError[j] != -1)) not++;
            confbuf = new ClassConfidence[not];
            pos = 0;
            for (j=0; j<split.length; j++)
            {
                if ((split[j] == i) && (classError[j] != -1))
                {
                    confbuf[pos++] = confidence[j];
                    if (classGoal[j] == 1) cavg++;
                }
            }
            cavg /= pos;
            
            // Order the results over decreasing confidence.
            Arrays.sort(confbuf);
            
            // The total number of classifications
            nt    = confbuf.length;
            xstep = (vend-vbeg) / GAINLEN;
            
            // Make Gain Chart.
            x = vbeg;
            for (j=0; j<GAINLEN; j++)
            {
                // Calculate gain at 'ind'
                ind  = (int)(x*nt);
                cnow = 0;
                for (k=0; k<ind; k++)
                {
                    pos = confbuf[k].ind;
                    if (classGoal[pos] == 1) cnow++;
                }
                cnow            /= ind;
                gainnow          = (cnow/cavg)-1.0;
                gainfold[i-1][j] = gainnow;
                
                // Move on the show.
                x += xstep;
            }
        }
        
        // Merge the Gain Charts of the folds
        for (i=0; i<GAINLEN; i++)
        {
            gainnow = 0;
            for (j=0; j<numFolds; j++) gainnow += gainfold[j][i];
            gainnow /= numFolds;
            gain[i] = gainnow;
        }
        
        return(gain);
    }
    
    public void setCostMatrix(double [][]_costMatrix)
    {
        costMatrix = _costMatrix;
    }
    
    public double getCost()
    {
        int    i,j;
        double [][]confmat;
        double cost;
        
        confmat = getConfusionMatrix();
        cost    = 0;
        for (i=0; i<confmat.length; i++)
        {
            for (j=0; j<confmat[i].length; j++) cost += confmat[i][j]*costMatrix[i][j];
        }
        
        return(cost);
    }
    
    // **********************************************************\
    // *           Supervised Classification Statistics         *
    // **********************************************************/
    /**
     * Get the interval of which you can be 'confidence'% certain that is contains the classification error.
     * @param confidence The required confidence level (e.g. 0.9 to be 90% certain).
     * @return Interval of the specified confidence [0] = begin, [1] = end, [2] = mean, [3] = variance
     * @throws LearnerException If the confidence interval cannot be calculated
     */
    public double []getErrorConfidenceInterval(double confidence) throws LearnerException
    {
        return(getErrorConfidenceInterval(1, numFolds, confidence));
    }
    
    /**
     * Get the interval of which you can be 'confidence'% certain that it contains the classification error of the given fold.
     * @param fold The fold number (starts at 1)
     * @param confidence The required confidence lebel (e.g. 0.9 to be 90% certain).
     * @return Interval of the specified confidence [0] = begin, [1] = end, [2] = mean, [3] = variance
     * @throws LearnerException If the confidence interval cannot be calculated
     */
    public double []getErrorConfidenceInterval(int fold, double confidence) throws LearnerException
    {
        return(getErrorConfidenceInterval(fold, fold, confidence));
    }
    
    private double []getErrorConfidenceInterval(int fmin, int fmax, double confidence) throws LearnerException
    {
        double []ci = new double[4];
        int    i;
        int    count;
        int    cor;
        double acc, var;
        
        ci[0] = 0; ci[1] = 0; ci[2] = 0; ci[3] = 0;
        
        // Find accuracy and test count
        count = 0; cor = 0;
        for (i=0; i<classError.length; i++)
        {
            if (inFold(split[i], fmin, fmax))
            {
                if (classError[i] != -1) count++;
                if (classError[i] ==  1) cor++;
            }
        }
        if (count > 0) { acc = ((double)cor)/count; var = (cor*(1-acc) + (count-cor)*acc)/ (count-1); }
        else           { acc = 0; var = 0; }
        ci[2] = acc; ci[3] = var;
        
        // Find confidence bounds
        if ((count > 0) && (confidence > 0) && (confidence < 1))
        {
            double f,z,N;
            double cc, cb, ce, cd;
            
            f = acc; N = count;
            z = -Probability.normalInverse((1.0 - confidence)/2);
            
            cc = z*Math.sqrt((f/N) - (f*f)/N + (z*z)/(4*N*N));
            cd = 1 + ((z*z)/N);
            cb = (f + (z*z)/(2*N) - cc) / cd;
            ce = (f + (z*z)/(2*N) + cc) / cd;
            
            ci[0] = cb; ci[1] = ce;
            if (ci[0] < 0) ci[0] = 0.0;
            if (ci[1] > 1) ci[1] = 1.0;
        }
        else throw new LearnerException("Cannot calculate confidence interval. No samples or bad confidence level. "+count+" "+confidence);
        
        return(ci);
    }
    
    private boolean inFold(int i, int fmin, int fmax)
    {
        boolean in;
        int     s;
        
        s = split[i];
        if ((s >= fmin) && (s <= fmax)) in = true;
        else                            in = false;
        
        return(in);
    }
    
    // **********************************************************\
    // *             Reporting over single instances            *
    // **********************************************************/
    public byte getInstanceErrorOfClassification(int inpos)
    {
        return(this.classError[inpos]);
    }
    
    // **********************************************************\
    // *             Reporting about a specific Fold            *
    // **********************************************************/
    /**
     * Give the percentage of instances that could not be classified in the given fold.
     * @param fold The number of the fold (starts at 1)
     * @return The
     */
    public double getErrorOfClassification(int fold)
    {
        int    i;
        double counterr, counttot;
        double err;
        
        counterr = 0; counttot = 0;
        for (i=0; i<classError.length; i++)
        {
            if (split[i] == fold)
            {
                if (classError[i] == -1) counterr++;
                counttot++;
            }
        }
        if (counttot != 0) err = counterr / counttot;
        else               err = 0;
        
        return(err);
    }
    
    /**
     * Give classification error of the given fold.
     * @param fold The fold number (starts at 1)
     * @return The classification error of the fold.
     */
    public double getClassificationError(int fold)
    {
        int    i;
        int    count;
        double err;
        
        count = 0; err = 0;
        for (i=0; i<classError.length; i++)
        {
            if (split[i] == fold)
            {
                if (classError[i] != -1) count++;
                if (classError[i] ==  0) err++;
            }
        }
        if (count > 0) err /= count;
        else           err = 0;
        
        return(err);
    }
    
    /**
     * Get the confusion matrix of the given fold.
     * @param fold The number of the fold (starts at 1)
     * @return The confusion matrix of this fold.
     */
    public double [][]getConfusionMatrix(int fold)
    {
        int    i,j;
        int    []gccount;
        double [][]confold;
        
        // Make space for the confusion matrix of the fold
        confold = new double[numClass][];
        gccount = new int[numClass];
        for (i=0; i<confold.length; i++) confold[i] = new double[numClass];
        for (i=0; i<confold.length; i++)
        {
            for (j=0; j<confold.length; j++) confold[i][j] = 0;
            gccount[i] = 0;
        }
        
        // Find test of this fold and update the confusion matrix with them.
        for (i=0; i<split.length; i++)
        {
            if (split[i] == fold)
            {
                if (classOut[i] != -1)
                {
                    confold[classGoal[i]][classOut[i]]++;
                    gccount[classGoal[i]]++;
                }
            }
        }
        
        // Normalize over Goal Class rows?
        ;
        
        return(confold);
    }
    
    /**
     * Give the ROC curve of the given fold of the tested 2-class classification problem.
     * @param fold The fold number (starting at 1)
     * @param ROCLEN The number of points in the curve (e.g. 100)
     * @return The ROC curve of the given fold. Array of length ROCLEN.
     * @throws LearnerException If the curve cannot be generated for some reason.
     */
    public double []getROCCurve(int fold, int ROCLEN) throws LearnerException
    {
        return(getROCCurve(fold, ROCLEN, null));
    }
    
    /**
     * Give the ROC curve of the given fold of the tested 2-class classification problem.
     * Also save it to the specified file.
     * @param fold The fold number (starting at 1)
     * @param ROCLEN The number of points in the curve (e.g. 100)
     * @param rocfile The file in which to save the curve.
     * @throws LearnerException If the curve cannot be generated or saved.
     * @return The ROC curve. Array of length ROCLEN.
     */
    public double []getROCCurve(int fold, int ROCLEN, String rocfile) throws LearnerException
    {
        int             i,j;
        double          []roc;
        int             not, np, nn, ind, pos;
        double          x, y;
        double          xstep;
        ClassConfidence []confbuf;
        
        // Check classification appropriateness.
        try
        {
            if (dataModel.getAttribute(dataModel.getLearningProperty().getGoalIndex()).getNumberOfGoalClasses() != 2)
                throw new LearnerException("Cannot generate a Lift/ROC curve of a multi-class classification.");
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
        
        // Make buffer for the ROC curve
        roc = new double[ROCLEN];
        
        // Count the number of succesfully tested instances in the fold
        not = 0;
        for (j=0; j<split.length; j++) if ((split[j] == fold) && (classError[j] != -1)) not++;
        confbuf = new ClassConfidence[not];
        pos = 0;
        for (j=0; j<split.length; j++)
        {
            if ((split[j] == fold) && (classError[j] != -1)) confbuf[pos++] = confidence[j];
        }
        
        // Order the results over decreasing confidence.
        Arrays.sort(confbuf);
        
        // Count total number of positives(1) and negatives(0)
        np  = 0; nn = 0;
        for (j=0; j<not; j++)
        {
            if (classGoal[confbuf[j].ind] == 1) np++;
            else                                nn++;
        }
        
        // Make ROC curce
        xstep = ((double)nn) / ROCLEN;
        x = 0; pos = 1; y = 0;
        for (j=0; (j<not) && (pos < ROCLEN); j++)
        {
            ind = confbuf[j].ind;
            if (classGoal[ind] == 1) y += 1;
            else                     x += 1;
            
            if (x >= xstep)
            {
                while (((x-xstep) > 0) && (pos < ROCLEN))
                {
                    x         -= xstep;
                    roc[pos++] = y/np;
                }
            }
        }
        for (; pos<ROCLEN; pos++) roc[pos] = 1.00;
        
        // Write out the merged ROC curve
        if (rocfile != null)
        {
            try
            {
                FileWriter froc = new FileWriter(rocfile);
                for (i=0; i<ROCLEN; i++) froc.write((((double)i)/ROCLEN)+"\t"+roc[i]+"\n");
                froc.close();
            }
            catch(java.io.IOException ex) { ex.printStackTrace(); }
        }
        
        return(roc);
    }
    
    // **********************************************************\
    // *                Reporting over all Folds                *
    // **********************************************************/
    /**
     * Give the average ROC curve (over the folds) of the tested 2-class classification.
     * @param ROCLEN The number of points in the ROC curve. (typical value 100)
     * @return The ROC curve as double[] indexed by the x-value, containing the y-values.
     * @throws LearnerException If an error occuring while generating the ROC curve.
     */
    public double []getROCCurve(int ROCLEN) throws LearnerException
    {
        return(getROCCurve(ROCLEN, null));
    }
    
    /**
     * Give the average ROC curve (over the folds) of the tested 2-class classification.
     * @param ROCLEN The number of points in the ROC curve. (typical value 100)
     * @param rocfile The filename of the file where to store the ROC curve data.
     *                <code>null</code> if it shouldn't be stored.
     * @return The ROC curve as double[] indexed by the x-value, containing the y-values.
     * @throws LearnerException If an error occuring while generating or saving the ROC curve.
     */
    public double []getROCCurve(int ROCLEN, String rocfile) throws LearnerException
    {
        int             i,j;
        double          [][]rocfold;
        double          []roc;
        int             not, np, nn, ind, pos;
        double          x, y;
        double          xstep, rocnow;
        ClassConfidence []confbuf;
        
        try
        {
            // Check classification appropriateness.
            if (dataModel.getAttribute(dataModel.getLearningProperty().getGoalIndex()).getNumberOfGoalClasses() != 2)
                throw new LearnerException("Cannot generate a Lift/ROC curve of a multi-class classification.");
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
        
        // Make buffers for the ROC curves of the different folds as well as for the combines one.
        rocfold = new double[numFolds][];
        for (i=0; i<rocfold.length; i++) rocfold[i] = new double[ROCLEN];
        roc = new double[ROCLEN];
        
        // Iterate over the folds
        for (i=1; i<=numFolds; i++)
        {
            // Count the number of succesfully tested instances in this fold
            not = 0;
            for (j=0; j<split.length; j++) if ((split[j] == i) && (classError[j] != -1)) not++;
            confbuf = new ClassConfidence[not];
            pos = 0;
            for (j=0; j<split.length; j++)
            {
                if ((split[j] == i) && (classError[j] != -1)) confbuf[pos++] = confidence[j];
            }
            
            // Order the results over decreasing confidence.
            Arrays.sort(confbuf);
            
            // Count total number of positives(1) and negatives(0)
            np  = 0; nn = 0;
            for (j=0; j<not; j++)
            {
                if (classGoal[confbuf[j].ind] == 1) np++;
                else                                nn++;
            }
            
            // Make ROC curce
            xstep = ((double)nn) / ROCLEN;
            x = 0; pos = 1; y = 0;
            for (j=0; (j<not) && (pos < ROCLEN); j++)
            {
                ind = confbuf[j].ind;
                if (classGoal[ind] == 1) y += 1;
                else                     x += 1;
                
                if (x >= xstep)
                {
                    while (((x-xstep) > 0) && (pos < ROCLEN))
                    {
                        x                  -= xstep;
                        rocfold[i-1][pos++] = y/np;
                    }
                }
            }
            for (; pos<ROCLEN; pos++) rocfold[i-1][pos] = 1.00;
        }
        
        // Merge the ROC curves of the folds
        for (i=0; i<ROCLEN; i++)
        {
            rocnow = 0;
            for (j=0; j<numFolds; j++) rocnow += rocfold[j][i];
            rocnow /= numFolds;
            roc[i] = rocnow;
        }
        
        // Write out the merged ROC curve
        if (rocfile != null)
        {
            try
            {
                FileWriter froc = new FileWriter(rocfile);
                for (i=0; i<ROCLEN; i++) froc.write((((double)i)/ROCLEN)+"\t"+roc[i]+"\n");
                froc.close();
            }
            catch(java.io.IOException ex) { ex.printStackTrace(); }
        }
        
        return(roc);
    }
    
    /**
     * Give the confusion matrix of the tested classification.
     * @return The confusion-matrix as a <code>double [][]</code>
     */
    public double [][]getConfusionMatrix()
    {
        return(classConfusion);
    }

    
    /**
     * Give the percentage of instances that could not be classified.
     * @return The percentage [0,1] of instances that could not be classified.
     */
    public double getErrorOfClassification()
    {
        int    i;
        double count;
        double err;
        
        count = 0;
        for (i=0; i<classError.length; i++)
        {
            if (classError[i] == -1) count++;
        }
        err = count / classError.length;
        
        return(err);
    }
    
    /**
     * Give the overall error of the tested classification
     * @return The classification error.
     */
    public double getClassificationError()
    {
        int    i;
        int    count;
        double err;
        
        count = 0; err = 0;
        for (i=0; i<classError.length; i++)
        {
            if (classError[i] != -1) count++;
            if (classError[i] == 0) err++;
        }
        if (count > 0) err /= count;
        else           err = 0;
        
        return(err);
    }
    
    // **********************************************************\
    // *                Classification Testing                  *
    // **********************************************************/
    /**
     * Test the classifier of the associated Validation on the given test-set.
     * @param fold The fold-number of this test.
     * @param testSet The set of instances that should be tested
     * @throws LearnerException If something goes wrong during the test classification operations.
     */
    void testClassifier(int fold, Presenter testSet) throws LearnerException
    {
        int             i;
        int             gcnow;
        int             clnow;
        int             numins;
        DoubleMatrix1D  insnow;
        ObjectMatrix1D  oinsnow;
        byte            []errbuf;
        int             []goalbuf;
        ClassConfidence []confbuf;
        int             epos;
        double          []conf = new double[numClass];
        
        numins            = testSet.getNumberOfInstances();
        dataModel         = testSet.getDataModel();
        errbuf            = new byte[numins];
        goalbuf           = new int[numins];
        confbuf           = new ClassConfidence[numins];
        classTest[fold-1] = new int[numins];
        
        // Update the test results, confusion matrix and error vector.
        for (i=0; i<numins; i++)
        {
            // Test the current instance
            insnow = null; oinsnow = null;
            if (isPrimitive())  insnow = testSet.getInstance(i);
            else               oinsnow = testSet.getObjectInstance(i);
            gcnow      = testSet.getGoalClass(i);
            goalbuf[i] = gcnow;
            if (isPrimitive())  clnow = cl.classify(insnow, conf);
            else                clnow = cl.classify(oinsnow, conf);
            
            if (clnow != -1)
            {
                // If the classification succeeded. Update error, confusion, confidence buffers.
                numTest++;
                this.classCount[gcnow]++;
                this.classConfusion[gcnow][clnow]++;
                
                this.classTest[fold-1][i] = clnow;
                confbuf[i]           = new ClassConfidence(conf[1]);
                if (clnow == gcnow) errbuf[i] = 1;
                else                errbuf[i] = 0;
            }
            else
            {
                // Classification didn't work...
                this.classTest[fold-1][i] = -1;
                confbuf[i]                = null;
                errbuf[i]                 = -1;
            }
        }
        
        // Merge this fold's buffers in the global buffers used by the reporting methods.
        epos = 0;
        for (i=0; i<this.split.length; i++)
        {
            if (split[i] == fold)
            {
                this.classOut[i]   = this.classTest[fold-1][epos];
                this.classGoal[i]  = goalbuf[epos];
                this.classError[i] = errbuf[epos];
                this.confidence[i] = confbuf[epos];
                if (this.confidence[i] != null) this.confidence[i].ind = i;
                epos++;
            }
        }
        //System.out.println("Finished validating fold "+fold+" of "+numFolds);
    }
    
    
    // **********************************************************\
    // *           Classification Test Initialization           *
    // **********************************************************/
    void initTest(Validation _val, Presenter _instances, int []_split, int _numFolds, Classifier _cl) throws DataModelException
    {
        int       i,j;
        int       numins;
        DataModel dm;
        DataModelPropertyLearning learn;
        
        // Remember the general information about the comming test.
        this.val       = _val;
        this.instances = _instances;
        this.dataModel = instances.getDataModel();
        this.split     = _split;
        this.numFolds  = _numFolds;
        this.cl        = _cl;
        this.numTest   = 0;
        
        // Check DataModel fit.
        learn = this.dataModel.getLearningProperty();
        if (learn.getGoalIndex() == -1)
            throw new DataModelException("Cannot comparis classifier that has no goal class... Impossible configuration for a Classifier.");
        
        // Test Results. Error vector. Confusion Matrix.
        dm         = instances.getDataModel();
        numins     = instances.getNumberOfInstances();
        this.numClass   = dm.getAttribute(learn.getGoalIndex()).getNumberOfGoalClasses();
        this.classTest  = new int[numFolds][];
        this.classOut   = new int[numins];
        this.classGoal  = new int[numins];
        this.classError = new byte[numins];
        for (i=0; i<this.classError.length; i++) { this.classOut[i] = -1; this.classError[i] = -1; }
        this.classConfusion = new double[numClass][];
        this. classCount     = new int[numClass];
        for (i=0; i<numClass; i++)
        {
            this.classConfusion[i] = new double[numClass];
            this.classCount[i]     = 0;
            for (j=0; j<numClass; j++) this.classConfusion[i][j] = 0;
        }
        this.confidence = new ClassConfidence[numins];
    }
    
    void endTest()
    {
        int i,j;
        
        // Make the confusion matrix
        for(i=0; i<classConfusion.length; i++)
        {
            for (j=0; j<classConfusion[i].length; j++)
            {
                //if (classCount[i] != 0) classConfusion[i][j] /= classCount[i];
                //else                    classConfusion[i][j]  = 0;
            }
        }
        
        // Install Default Costs.
        costMatrix = new double[numClass][numClass];
        for (i=0; i<costMatrix.length; i++)
        {
            for (j=0; j<costMatrix.length; j++) if (i == j) costMatrix[i][j] = 0.0;
            else        costMatrix[i][j] = 1.0;
        }
    }
    
    
    public ValidationClassifier()
    {
    }
    
    private boolean isPrimitive()
    {
        return(this.dataModel instanceof DataModelDouble);
    }
    
    // **********************************************************\
    // *   The confidence of classification test an instance    *
    // **********************************************************/
    private class ClassConfidence implements Comparable
    {
        int    ind;     // The index in the 'instances' set.
        double conf;    // Confidence of classification test.
        
        public int compareTo(Object o)
        {
            ClassConfidence cc = (ClassConfidence)o;
            if      (conf <  cc.conf) return(1);
            else if (conf == cc.conf) return(0);
            else                      return(-1);
        }
        
        public ClassConfidence(double _conf) { conf = _conf; }
    }
    
    // **********************************************************\
    // *                   Univariate Analysis                  *
    // **********************************************************/
    private int getbin(double min,double max,int nrofbins,double value) {
        double  x0 = min;
        double  x1 = max;
        int     b0 = 0;
        int     b1 = nrofbins-1;
        while (b1 - b0 > 1) {
            double    x = (x1 + x0)/2;
            int       b = (b0 + b1)/2;
            if (value < x) {
                b1 = b;
                x1 = x;
            } else {
                b0 = b;
                x0 = x;
            }
        };
        return b0;
    }
    
    public double UnivariateROCValue(int index,double threshold) throws LearnerException
    {
        int                 signallength = instances.getNumberOfInstances();
        DoubleMatrix1D      signal  =    DoubleFactory1D.dense.make(signallength);
        
        double    max = 0,min = 0;
        double    result = 0;
        
        //prepare signal and look for min,max
        for (int i = 0;i < signallength;i++) {
            signal.set(i,instances.getInstance(i).get(index));
            //System.out.println(signal.get(i));
            if (i == 0) {
                max = signal.get(i);
                min = max;
            } else {
                if (max < signal.get(i)) max = signal.get(i);
                if (min > signal.get(i)) min = signal.get(i);
            };
        }
        
        //System.out.println("max = " + max);
        //System.out.println("min = " + min);
        
        //initialize histograms
        int   nrofbins = 256;
        int   nrofclasses = 2;
        int   [][]histclass = new int[nrofclasses][];
        int   []nrofpoints = new int[nrofclasses];
        
        for (int j = 0;j < nrofclasses;j++) {
            nrofpoints[j] = 0;
            histclass[j] = new int[nrofbins];
            for (int i = 0;i < nrofbins;i++) histclass[j][i] = 0;
        }
        
        //normalize and check
        for (int i = 0;i < signallength;i++) {
            double value = (signal.get(i)-min)/(max - min);
            int t = getbin(0.0,1.0,nrofbins,value);
            //System.out.println("["+value + "]-"+t);
            histclass[classGoal[i]][t]++;
            nrofpoints[classGoal[i]]++;
        }
        
        //Generate coordinates  (cumulative histogram)
        double [][]x = new double[nrofclasses][];
        double []p = new double[nrofclasses];
        for (int j = 0;j < nrofclasses;j++) {
            x[j] = new double[nrofbins];
            p[j] = 0.0;
            double c = 0.0;
            for (int i = 0;i < nrofbins;i++) {
                c += (double) (((double) histclass[j][i])/(double) nrofpoints[j]);
                x[j][i] = c;
                //System.out.print( c + ",");
            }
            //System.out.println("");
            
        }
        
        //calculate integrale
        result = 0.0;
        for (int i = 0;i < nrofbins;i++ ) {
            result += (x[0][i] - p[0])*(x[1][i]+p[1])/2;
            for (int j = 0;j < nrofclasses;j++) p[j] = x[j][i];
        };
        
        
        return 1-result;
    }
    
    public double UnivariateROCValue(int index) throws LearnerException
    {
        return UnivariateROCValue(index,0.5);
    }
    
    public DoubleMatrix1D getROCCurveUnivariate(int index,double threshold,double from, double too) throws LearnerException
    {
        int                 signallength = instances.getNumberOfInstances();
        DoubleMatrix1D      signal  =    DoubleFactory1D.dense.make(signallength);
        
        double    max = 0,min = 0;
        
        //prepare signal and look for min,max
        for (int i = 0;i < signallength;i++) {
            signal.set(i,instances.getInstance(i).get(index));
            //System.out.println(signal.get(i));
            if (i == 0) {
                max = signal.get(i);
                min = max;
            } else {
                if (max < signal.get(i)) max = signal.get(i);
                if (min > signal.get(i)) min = signal.get(i);
            };
        }
        
        //System.out.println("max = " + max);
        //System.out.println("min = " + min);
        
        //initialize histograms
        int   nrofbins = 256;
        int   nrofclasses = 2;
        int   [][]histclass = new int[nrofclasses][];
        int   []nrofpoints = new int[nrofclasses];
        
        for (int j = 0;j < nrofclasses;j++) {
            nrofpoints[j] = 0;
            histclass[j] = new int[nrofbins];
            for (int i = 0;i < nrofbins;i++) histclass[j][i] = 0;
        }
        
        //normalize and check
        for (int i = 0;i < signallength;i++) {
            double value = (signal.get(i)-min)/(max - min);
            int t = getbin(0.0,1.0,nrofbins,value);
            //System.out.println("["+value + "]-"+t);
            histclass[classGoal[i]][t]++;
            nrofpoints[classGoal[i]]++;
        }
        
        //Generate coordinates  (cumulative histogram)
        double [][]x = new double[nrofclasses][];
        double []p = new double[nrofclasses];
        for (int j = 0;j < nrofclasses;j++) {
            x[j] = new double[nrofbins];
            p[j] = 0.0;
            double c = 0.0;
            for (int i = 0;i < nrofbins;i++) {
                c += (double) (((double) histclass[j][i])/(double) nrofpoints[j]);
                x[j][i] = c;
                //System.out.print( c + ",");
            }
            //System.out.println("");
            
        }
        
        DoubleMatrix1D      result  =    DoubleFactory1D.dense.make(signallength);
        
        int   nrofsamples = 256;
        int   offset = 0;
        
        for (int j = 0;j < nrofclasses;j++)  p[j] = 0.0;
        
        for (int i = 0;i < nrofsamples;i++) {
            double t = (double) i / (double) nrofsamples;
            if  (t >= x[0][offset]) {
                while (offset < nrofbins && t >= x[0][offset]) offset++;
            }
            //interpolate
            if (offset > 0) {
                if (offset == nrofbins) {
                    result.set(i,x[1][offset-1] + ((1.0-x[1][offset-1])/(1.0-x[0][offset-1])) * (t - x[0][offset-1]));
                } else {
                    if ((x[0][offset]-x[0][offset-1]) > 0)
                        result.set(i,x[1][offset-1] + ((x[1][offset]-x[1][offset-1])/(x[0][offset]-x[0][offset-1])) * (t - x[0][offset-1]));
                    else
                        result.set(i,result.get(i-1));
                }
            } else {
                result.set(i,(x[1][offset])/(x[0][offset]) * (t));
            }
            //System.out.println(t + "\t" + x[0][offset] + "\t" + result.get(i));
        }
        
        return result;
    }
    
}
