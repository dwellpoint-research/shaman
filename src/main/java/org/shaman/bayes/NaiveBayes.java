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
package org.shaman.bayes;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.shaman.dataflow.Persister;
import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.AttributeObject;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.CachingPresenter;
import org.shaman.learning.Classifier;
import org.shaman.learning.ClassifierTransformation;
import org.shaman.learning.Presenter;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Naive Bayes Classifier</h2>
 * Classifier that is based on Bayes' Theorem and the naive assumption that
 * all attributes are statistically independent. All attributes should be
 * categorical, as well as the goal attribute.
 * <p>
 * <i>Tom M. Mitchell (1997), Machine Learning, p177</i>
 */

// *********************************************************\
// *    Naive Base Classifier for Categorical Attributes   *
// *********************************************************/
public class NaiveBayes extends ClassifierTransformation implements Classifier, Persister
{
    // The Naive Bayes Classifier Model
    private double [][][]paijvk;            // p(aij|vk) indexed with [k][i][j]
    private double []pvk;                   // p(vk)
    private double [][][]caijvk;
    private double []cvk;
    private int    ctot;
    
    // *********************************************************\
    // *             Naive Bayes Classification                *
    // *********************************************************/
    private int classifyObject(Object instance, double []confidence) throws LearnerException
    {
        DoubleMatrix1D  dins;
        ObjectMatrix1D  oins;
        AttributeDouble attdonow;
        AttributeObject attobnow;
        int             i,j,cl;
        int             numclass;
        double          val, max;
        Object          oval;
        int             clnow;
        int             numval;
        double        []pcl;
        
        // Make the correct instance
        dins = null;
        oins = null;
        if (this.primitive) dins = (DoubleMatrix1D)instance;
        else                oins = (ObjectMatrix1D)instance;
        
        numclass = 0; pcl = null;
        try
        {
            // Clear class probability tables
            numclass = this.attgoal.getNumberOfGoalClasses();
            pcl      = new double[numclass];
            for (i=0; i<pcl.length; i++) pcl[i] = 1;
            
            // Calculate the probability per class according to the Naive Bayesian Method.
            for (i=0; i<numclass; i++)
            {
                numval = 0;
                for (j=0; j<this.actind.length; j++)
                {
                    // Multiply by the probability that the current value occurs.
                    if (this.primitive) // Primitive Data.
                    {
                        attdonow = this.dmdo.getAttributeDouble(actind[j]);
                        val      = dins.getQuick(j);
                        if ((attdonow.isMissingAsDouble(val)) &&
                                (attdonow.getMissingIs() == Attribute.MISSING_IS_UNKNOWN)) ; // Skip unknown values
                        else
                        {
                            // Treat missing values as a special category.
                            clnow   = attdonow.getCategory(val);
                            pcl[i] *= this.paijvk[i][j][clnow];
                            numval++;
                        }
                    }
                    else             // Object Based Data.
                    {
                        attobnow = this.dmob.getAttributeObject(actind[j]);
                        oval     = oins.getQuick(j);
                        if ( attobnow.isMissingAsObject(oval) &&
                                (attobnow.getMissingIs() == Attribute.MISSING_IS_UNKNOWN)); // Skip unknown values.
                        else
                        {
                            // Treat missing values as a special category.
                            clnow   = attobnow.getCategory(oval);
                            pcl[i] *= this.paijvk[i][j][clnow];
                            numval++;
                        }
                    }
                }
                
                if (numval > 0) pcl[i] *= this.pvk[i];
                else            pcl[i] = -1;
            }
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
        
        // Find the most probable class
        max = -1; cl = -1;
        for (i=0; i<numclass; i++)
        {
            if (pcl[i] > max) { cl = i; max = pcl[i]; }
        }
        if (confidence != null)
        {
            for (i=0; i<pcl.length; i++) confidence[i] = pcl[i];
        }
        
        // Return classification result.
        if (max > -1) return(cl);
        else          return(-1);
    }
    
    public int classify(ObjectMatrix1D instance, double []confidence) throws LearnerException
    {
        return(classifyObject(instance, confidence));
    }
    
    public int classify(DoubleMatrix1D instance, double []confidence) throws LearnerException
    {
        return(classifyObject(instance, confidence));
    }
    
    // **********************************************************\
    // *                 Naive Bayes Learning                   *
    // **********************************************************/
    public void train() throws LearnerException
    {
        Attribute       attnow;
        AttributeDouble attdonow;
        AttributeObject attobnow;
        int             i, j, k, numins, numgoal, numclass;
        int             gc, cn;
        double          val, iweight;
        Object          oval;
        
        // Naive Bayes model. Counters and probabilities
        double [][][]paijvk;
        double []pvk;
        double [][][]caijvk;
        double []cvk;
        int    ctot;
        
        try
        {
            // Number of instances in the training set
            numins = this.trainData.getNumberOfInstances();
            
            // Make the probability and counter buffers.
            numgoal = this.attgoal.getNumberOfGoalClasses();
            paijvk  = new double[numgoal][][];
            pvk     = new double[numgoal];
            caijvk  = new double[numgoal][][];
            cvk     = new double[numgoal];
            
            // Arrays of [number of goal classes][number of attributes][number of classes in attribute]
            for (i=0; i<numgoal; i++)
            {
                paijvk[i] = new double[actind.length][];
                caijvk[i] = new double[actind.length][];
                for (j=0; j<actind.length; j++)
                {
                    attnow       = this.dataModel.getAttribute(actind[j]);
                    numclass     = attnow.getNumberOfCategories();
                    paijvk[i][j] = new double[numclass];
                    caijvk[i][j] = new double[numclass];
                    for (k=0; k<paijvk[i][j].length; k++) { paijvk[i][j][k] = 0; caijvk[i][j][k] = 0; }
                }
                pvk[i] = 0; cvk[i] = 0;
            }
            
            // Determine the probability of the goal classes. Take into account instance weights.
            ctot = 0;
            for (i=0; i<numins; i++)
            {
                iweight  = trainData.getWeight(i);
                gc       = trainData.getGoalClass(i);
                cvk[gc] += iweight;
                ctot    += iweight;
            }
            
            // And the probability of a specific attribute category occuring together with a goal class.
            for (i=0; i<cvk.length; i++) pvk[i] = ((double)cvk[i]) / ((double)ctot);
            
            // For all the goal classes
            for (i=0; (i<numgoal); i++)
            {
                // And all the active attributes
                for (j=0; (j<this.actind.length); j++)
                {
                    // Loop over all the instance in the training set
                    attnow = this.dataModel.getAttribute(this.actind[j]);
                    for (k=0; (k<numins); k++)
                    {
                        // Class i occuring in instance k...
                        if (this.trainData.getGoalClass(k) == i)
                        {
                            // Lookup the weight of the current instance
                            iweight = this.trainData.getWeight(k);
                            
                            // Attribute cn occuring...
                            if (this.primitive)
                            {
                                attdonow = (AttributeDouble)attnow;
                                val      = this.trainData.getInstance(k).getQuick(j);
                                if ((!attdonow.isMissingAsDouble(val)) || (attdonow.getMissingIs() == Attribute.MISSING_IS_VALUE))
                                {
                                    // Adjust the counter of the occuring category. Take into account instance weight.
                                    cn = attdonow.getCategory(val);
                                    caijvk[i][j][cn] += iweight;
                                }
                            }
                            else
                            {
                                attobnow = (AttributeObject)attnow;
                                oval     = this.trainData.getObjectInstance(k).getQuick(j);
                                if ((!attobnow.isMissingAsObject(oval)) || (attobnow.getMissingIs() == Attribute.MISSING_IS_VALUE))
                                {
                                    // Adjust the counter of the occuring category. Take into account instance weight.
                                    cn = attobnow.getCategory(oval);
                                    caijvk[i][j][cn] += iweight;
                                }
                            }
                        }
                    }
                }
            }
            
            // Convert the counters to probabilities
            for (i=0; i<numgoal; i++)
            {
                for (j=0; j<this.actind.length; j++)
                {
                    for (k=0; k<caijvk[i][j].length; k++)
                        if (caijvk[i][j][k] != 0) paijvk[i][j][k] = ((double)caijvk[i][j][k]) / ((double)cvk[i]);
                        else                      paijvk[i][j][k] = 0;
                }
            }
            
            // Trained if not canceled
            // Commit trained model
            this.paijvk = paijvk;
            this.pvk    = pvk;
            this.caijvk = caijvk;
            this.cvk    = cvk;
            this.ctot   = ctot;
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
    }

    // **********************************************************\
    // *            Transformation/Flow Interface               *
    // **********************************************************/
    public void init() throws ConfigException
    {
        // Initialize classifier. Check input datamodel, install classifier output model.
        super.init();
        
        try
        {
            // Make some helper variables
            initializeTraining();
        }
        catch(LearnerException ex) { throw new ConfigException(ex); }
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }

    // **********************************************************\
    // *                    Learner Interface                   *
    // **********************************************************/
    public void initializeTraining() throws LearnerException
    {
        // Find the active attribute and the index of the goal attribute
        this.actind  = this.dataModel.getActiveIndices();
        this.attgoal = this.dataModel.getAttribute(this.dataModel.getLearningProperty().getGoalIndex());
    }
    
    public Presenter getTrainSet()
    {
        return(trainData);
    }
    
    public void setTrainSet(Presenter trainData) throws LearnerException
    {
        DataModel dmin;
        
        try
        {
            // Install new Training instances, initialize classifier data-structures
            dmin           = trainData.getDataModel();
            this.trainData = (CachingPresenter)trainData;
            setDataModel(dmin);
            initClassifier(dmin);
        }
        catch(ConfigException ex) { throw new LearnerException(ex); }
    }
    
    public boolean   isSupervised() { return(true); }

    public void checkDataModelFit(int port, DataModel dmin) throws DataModelException
    {
        // Make sure the input contains object or double based categorical data
        checkClassifierDataModelFit(dmin, true, true, true);
    }
    
    // **********************************************************\
    // *             State Persistence Implementation           *
    // **********************************************************/
    public void loadState(ObjectInputStream oin) throws ConfigException
    {
        try
        {
            super.loadState(oin);
            this.paijvk = (double [][][])oin.readObject();
            this.pvk    = (double [])oin.readObject();
            this.caijvk = (double [][][])oin.readObject();
            this.cvk    = (double [])oin.readObject();
            this.ctot  = oin.readInt();
        }
        catch(IOException ex)            { throw new ConfigException(ex); }
        catch(ClassNotFoundException ex) { throw new ConfigException(ex); }
    }
    
    public void saveState(ObjectOutputStream oout) throws ConfigException
    {
        try
        {
            super.saveState(oout);
            oout.writeObject(this.paijvk);
            oout.writeObject(this.pvk);
            oout.writeObject(this.caijvk);
            oout.writeObject(this.cvk);
            oout.writeInt(this.ctot);
        }
        catch(IOException ex) { throw new ConfigException(ex); }
    }
    
    // *********************************************************\
    // *              Naive Bayes Classifier Creation          *
    // *********************************************************/
    public NaiveBayes()
    {
        super();
        name        = "Naive Bayes";
        description = "Naive Bayes Classifier.";
    }
}