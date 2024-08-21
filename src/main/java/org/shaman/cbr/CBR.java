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
package org.shaman.cbr;

import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.Classifier;
import org.shaman.learning.ClassifierTransformation;
import org.shaman.learning.Presenter;

import cern.colt.list.IntArrayList;
import cern.colt.map.OpenIntIntHashMap;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;
import cern.colt.matrix.doublealgo.Statistic;


/**
 * <h2>Case Based Reasoning</h2>
 * Instance Based Learning Methods. Integrates with the
 * data-modeling package to handle symbolic data.
 * Can act as a classifier (using K-Nearest neighbor) and
 * estimator (using locally weighted regression).
 */

// *********************************************************\
// *                  Instance Based Learning              *
// *********************************************************/
public class CBR extends ClassifierTransformation implements Classifier
{
    private int    kNearest;                // How many instances to consider
    
    // The CBR Model
    private int          []view;            // The indices in the instances to use.
    private DistanceMatrix dm;              // Distance Calculation between instances
    private int            numClass;        // Number of goal classes
    
    private OpenIntIntHashMap indExclude;   // Exclude the instances in the keyset of this Map.
    
    // *********************************************************\
    // *           Case Base Reasoning Classification          *
    // *********************************************************/
    private void maintainKNearest(int c, double d, double w, int []kcl, double []kdist, double []kwei)
    {
        // Check if the new distance is smaller than the k-nearest onces up until now.
        int i, pos;
        
        if (!this.indExclude.containsKey(c)) // Stop if the current instance should be excluded.
        {
            // Check if the given distance is smaller than the current k-nearest distances.
            pos = -1;
            for (i=0; (i<kdist.length) && (pos == -1); i++)
            {
                if (d < kdist[i]) pos = i;
            }
            // If smaller, merge in the nearest neighbor list at the right spot.
            if (pos != -1)
            {
                for (i=pos+1; i<kdist.length; i++)
                { 
                    kcl[i]   = kcl[i-1];
                    kdist[i] = kdist[i-1];
                    kwei[i]  = kwei[i-1];
                }
                kdist[pos] = d;
                kcl[pos]   = c;
                kwei[pos]  = w;
            }
        }
    }
    
    private int classifyObject(Object instance, double []confidence) throws LearnerException
    {
        DoubleMatrix1D  dins;
        ObjectMatrix1D  oins;
        int             i,cl;
        double          max, wei, sumwei;
        int             cnow;
        double          dnow;
        double        []kdist;
        double        []kwei;
        int           []kcl;
        double        []pcl;
        
        // Make the correct instance the one to use
        dins = null; oins = null;
        if (this.primitive) dins = (DoubleMatrix1D)instance;
        else                oins = (ObjectMatrix1D)instance;
        
        // Clear class probability tables
        pcl      = new double[this.numClass];
        for (i=0; i<pcl.length; i++) pcl[i] = 0;
        
        // Make k-Nearest distance and class buffers
        kcl   = new int[this.kNearest];
        kdist = new double[this.kNearest];
        kwei  = new double[this.kNearest];
        for (i=0; i<kdist.length; i++) kdist[i] = Double.POSITIVE_INFINITY;
        
        // Find K-Nearest Instances with the correct distance handling. Use DistanceMatrix for this.
        if (this.primitive)
        {
            DoubleMatrix1D dinv = dins.viewSelection(this.view);
            DoubleMatrix1D dtr;
            for (i=0; i<this.trainData.getNumberOfInstances(); i++)
            {
                dtr  = this.trainData.getInstance(i).viewSelection(this.view);
                cnow = this.trainData.getGoalClass(i);
                wei  = this.trainData.getWeight(i);
                dnow = this.dm.distance(dtr, dinv);
                maintainKNearest(cnow, dnow, wei, kcl, kdist, kwei);
            }
        }
        else
        {
            ObjectMatrix1D oinv = oins.viewSelection(this.view);
            ObjectMatrix1D otr;
            for (i=0; i<this.trainData.getNumberOfInstances(); i++)
            {
                otr  = this.trainData.getObjectInstance(i).viewSelection(this.view);
                cnow = this.trainData.getGoalClass(i);
                wei  = this.trainData.getWeight(i);
                dnow = this.dm.distance(otr, oinv);
                maintainKNearest(cnow, dnow, wei, kcl, kdist, kwei);
            }
        }
        
        // For all class, calculate the contributions of the k-nearest points.
        sumwei = 0;
        for (i=0; i<kcl.length; i++)
        {
            pcl[kcl[i]] += kwei[i];      // Take into account instance weighting.
            sumwei      += kwei[i];
        }
        if (sumwei > 0)
             for (i=0; i<pcl.length; i++) pcl[i] /= sumwei;
        else for (i=0; i<pcl.length; i++) pcl[i]  = -1;
        
        // Take the class with the highest occurence in the k-neighbors
        cl = -1; max = -1;
        for (i=0; i<pcl.length; i++) if (pcl[i] > max) { cl = i; max = pcl[i]; }
        
        // Fill in confidence
        if (confidence != null) for (i=0; i<pcl.length; i++) confidence[i] = pcl[i];
        
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
    // *                       Lazy Learning                    *
    // **********************************************************/
    public void train() throws LearnerException
    {
        int i;
        int []actind;
        
        // Include all active attributes in view on instances.
        actind    = this.dataModel.getActiveIndices();
        this.view = new int[actind.length];
        for (i=0; i<this.view.length; i++) this.view[i] = i;
        
        // Experimental algorithm that tries to find irrelevant attributes.
        if(false) sensitivityPolling();
    }
    
    private void sensitivityPolling() throws LearnerException
    {
        int               i, j, k;
        int               numins;
        //int               numgoal;
        int               []viewold;
        int               []viewnow;
        int               []actind;
        int               []split;
        double            foldsize;
        OpenIntIntHashMap []indexfold;
        IntArrayList      []indfold;
        IntArrayList      ifnow;
        double            []errwith;
        double            []errwo;
        //int               []actindnow;
        final int numfold;
        
        numfold  = 10;
        
        numins    = trainData.getNumberOfInstances();
        //numgoal   = attgoal.getNumberOfGoalClasses();
        split     = new int[numins];
        foldsize  = numins / (double)numfold;
        indexfold = new OpenIntIntHashMap[numfold];
        indfold   = new IntArrayList[numfold];
        errwith   = new double[numfold];
        errwo     = new double[numfold];
        
        // Field Sensitivity Polling.
        actind = dataModel.getActiveIndices(); // Remember original active attributes.
        
        // Make the random fold-assignment table.
        for (i=0; i<numins; i++) split[i] = (int)(i/foldsize);
        /*
         int               pos1, pos2, sbuf;
         
         for (i=0; i<numins; i++)
         {
         pos1        = Uniform.staticNextIntFromTo(0, numins-1);
         pos2        = Uniform.staticNextIntFromTo(0, numins-1);
         sbuf        = split[pos1];
         split[pos1] = split[pos2];
         split[pos1] = sbuf;
         }*/
        
        // Make the index exclusion maps for all folds
        for (i=0; i<numfold; i++) indexfold[i] = new OpenIntIntHashMap();
        for (i=0; i<numins;  i++) indexfold[split[i]].put(i, 0);
        for (i=0; i<numfold; i++) indfold[i] = indexfold[i].keys();
        
        //while (change)
        //actindnow = dataModel.getActiveIndices();
        viewnow = new int[view.length-1];
        for (i=0; i<view.length; i++)
        {
            viewold = view;
            
            // Compare an n-fold cross validation with/without the current attribute left out
            for (j=0; j<viewnow.length; j++)
            {
                if      (j <i) viewnow[j] = viewold[j];
                else if (j>=i) viewnow[j] = viewold[j+1];
            }
            
            // N-Fold Cross Validation without the current attribute.
            dataModel.getAttribute(actind[viewold[i]]).setIsActive(false);
            view = viewnow;
            for (j=0; j<numfold; j++)
            {
                // Exlude the current fold from possible nearest neighbors.
                indExclude = indexfold[j];
                ifnow      = indfold[j];
                errwo[j]   = 0;
                for (k=0; k<ifnow.size(); k++)  // Classify the instance in the fold. Calculate the error.
                {
                    if (         trainData.getGoalClass(ifnow.get(k)) !=
                        classify(trainData.getInstance(ifnow.get(k)))) errwo[j]++;
                }
                errwo[j] /= ifnow.size();
            }
            
            // N-Fold Cross Validation with the current attribute.
            view = viewold;
            dataModel.getAttribute(actind[viewold[i]]).setIsActive(true);
            for (j=0; j<numfold; j++)
            {
                // Exlude the current fold from possible nearest neighbors.
                indExclude = indexfold[j];
                ifnow      = indfold[j];
                errwith[j]   = 0;
                for (k=0; k<ifnow.size(); k++)  // Classify the instance in the fold. Calculate the error.
                {
                    if (trainData.getGoalClass(ifnow.get(k)) !=
                        classify(trainData.getInstance(ifnow.get(k)))) errwith[j]++;
                }
                errwith[j] /= ifnow.size();
            }
            
            System.out.println("Left out attribute "+dataModel.getAttribute(actind[viewold[i]]).getName());
            for (j=0; j<numfold; j++) System.out.println("fold "+j+" size "+indfold[j].size()+" Error "+errwith[j]+"  "+errwo[j]);
        }
        
    }
    
    // **********************************************************\
    // *                 Parameter Definition                   *
    // **********************************************************/
    public void setKNearest(int _kNearest)
    {
        this.kNearest = _kNearest;
    }
    
    // **********************************************************\
    // *            Transformation/Flow Interface               *
    // **********************************************************/
    public void init() throws ConfigException
    {
        // Initialize default classifier in/output
        super.init();
        
        // The number of goal classes
        this.numClass = this.attgoal.getNumberOfGoalClasses();
        
        try
        {
            // Create the Distance Matrix
            this.dm = new DistanceMatrix();
            this.dm.create(Statistic.EUCLID, false, false, this.dataModel);
        }
        catch(LearnerException ex) { throw new ConfigException(ex); }
    }
    
    public void cleanUp() throws DataFlowException
    {
    }
    
    public void checkDataModelFit(int port,DataModel dataModel) throws DataModelException
    {
        checkClassifierDataModelFit(dataModel, false, true, true);
    }
    
    // **********************************************************\
    // *                    Learner Interface                   *
    // **********************************************************/
    public void initializeTraining() throws LearnerException
    {
        // Exclude the instances with these indices in classification.
        this.indExclude = new OpenIntIntHashMap();
    }
    
    public Presenter getTrainSet()
    {
        return(this.trainData);
    }
    
    public void setTrainSet(Presenter _trainData)
    {
        this.trainData = _trainData;
        this.dataModel = this.trainData.getDataModel();
    }
    
    public boolean isSupervised()
    {
        return(true);
    }
    
    // *********************************************************\
    // *                 CBR Classifier Creation               *
    // *********************************************************/
    public CBR()
    {
        super();
        this.name        = "Case Based Reasoning";
        this.description = "Instance Based Classification and Regression";
    }
}