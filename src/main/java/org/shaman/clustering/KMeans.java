/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2006 Shaman Research                   *
\*********************************************************/
package org.shaman.clustering;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.shaman.cbr.DistanceMatrix;
import org.shaman.dataflow.Persister;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.Classifier;
import org.shaman.learning.ClassifierTransformation;
import org.shaman.learning.Presenter;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;
import cern.colt.matrix.doublealgo.Statistic;
import cern.colt.matrix.linalg.Blas;
import cern.colt.matrix.linalg.SeqBlas;
import cern.jet.random.Uniform;


/**
 * <h2>K-Means Clustering</h2>
 * 
 */

// *********************************************************\
// *                  Instance Based Learning              *
// *********************************************************/
public class KMeans extends ClassifierTransformation implements Classifier, Persister
{
    private int    k;                // Number of clusters?
    
    // --- Cluster Calculation ----
    private DistanceMatrix   dm;              // Distance Calculation between instances
    private DoubleMatrix1D []centroid;        // Centroids of the clusters
    
    // *********************************************************\
    // *           Case Base Reasoning Classification          *
    // *********************************************************/
    public int classify(ObjectMatrix1D instance, double []confidence) throws LearnerException
    {
        return(-1);
    }
    
    public int classify(DoubleMatrix1D instance, double []confidence) throws LearnerException
    {
        int    i, clmin;
        double dnow, dmin;
        
        // Classify the input instance into the cluster whose centroid is closest.
        clmin = -1;
        dmin  = Double.POSITIVE_INFINITY;
        for (i=0; i<this.centroid.length; i++)
        {
            dnow = this.dm.distance(instance, this.centroid[i]);
            if (dnow < dmin)
            {
                clmin = i;
                dmin  = dnow;
            }
        }
        
        return(clmin);
    }
    
    // **********************************************************\
    // *                       Lazy Learning                    *
    // **********************************************************/
    public void train() throws LearnerException
    {
        int            []cpos;
        DoubleMatrix1D []centroid;
        boolean          already;
        int              i, j, pos, numin;
        
        // Data sanity checks
        if (this.trainData.getNumberOfInstances() < this.k)
            throw new LearnerException("Not enough data to cluster in "+k+" clusters");
        
        // Select 'k' distinct random training points as initial cluster centers
        cpos   = new int[this.k];
        i      = 0;
        numin  = this.trainData.getNumberOfInstances();
        while(i<this.k)
        {
            pos = Uniform.staticNextIntFromTo(0, numin-1);
            already = false;
            for (j=0; j<i; j++) if (cpos[j] == pos) already = true;
            if (!already)
            {
                cpos[i] = pos;
                i++;
            }
        }
        centroid   = new DoubleMatrix1D[this.k];
        for (i=0; i<cpos.length; i++) centroid[i] = this.trainData.getInstance(cpos[i]).copy();
        
        // Iterator until stop criterium is reached
        boolean          stop;
        int            []member1;
        int            []member2, memberswap;
        DoubleMatrix1D   point;
        int            []ccount;
        int              inlen, clmin, itcount;
        int              movecount;
        double           d, dmin;
        Blas             blas;
        
        blas      = SeqBlas.seqBlas;
        inlen     = this.trainData.getInstance(0).size();
        member1   = new int[numin];
        member2   = new int[numin];
        for (i=0; i<member1.length; i++) member1[i] = -1;
        ccount    = new int[k];
        stop      = false;
        itcount   = 0;
        while(!stop)
        {
            // Assign all training points to their nearest centroid
            for (i=0; i<numin; i++)
            {
                point = this.trainData.getInstance(i);
                clmin = -1;
                dmin  = Double.POSITIVE_INFINITY;
                for (j=0; j<centroid.length; j++)
                {
                    d = this.dm.distance(centroid[j], point);
                    if (d < dmin)
                    {
                        dmin  = d;
                        clmin = j;
                    }
                }
                member2[i] = clmin;
            }
            
            // Check if the cluster assignments have stabilized enough to stop
            movecount = 0;
            for (i=0; i<numin; i++)
            {
                if (member1[i] != member2[i]) movecount++;
            }
            
            if (movecount > 0)
            {
                itcount++;
                
                // Calculate the centroid of the new cluster assignments
                for (i=0; i<this.k; i++)
                {
                    centroid[i] = DoubleFactory1D.dense.make(inlen);
                    ccount[i]   = 0;
                }
                for (i=0; i<member2.length; i++)
                {
                    ccount[member2[i]]++;
                    blas.daxpy(1.0, this.trainData.getInstance(i), centroid[member2[i]]);
                }
                for (i=0; i<this.k; i++) if (ccount[i] > 0) blas.dscal(1.0/ccount[i], centroid[i]);
                
                // Swap membership assignment buffers
                memberswap = member2;
                member2    = member1;
                member1    = memberswap;
            }
            else stop = true;
        }
        this.centroid = centroid;
        
        //System.err.println("-- itcount "+itcount);
        //for (i=0; i<this.centroid.length; i++) System.err.println("centroid "+this.centroid[i]);
    }
    
    // **********************************************************\
    // *                 Parameter Definition                   *
    // **********************************************************/
    public void setK(int k)
    {
        this.k = k;
    }
    
    // **********************************************************\
    // *            Transformation/Flow Interface               *
    // **********************************************************/
    public void init() throws ConfigException
    {
        // Initialize default classifier in/output
        super.init();
        
        // Check if number of goal classes corresponds with parameter
        if (this.k != this.attgoal.getNumberOfGoalClasses())
            throw new ConfigException("Number of goal classes does not agree with specified number of clusters. "+this.k+" != "+this.attgoal.getNumberOfGoalClasses());
        
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
        return(false);
    }
    
    // **********************************************************\
    // *             State Persistence Implementation           *
    // **********************************************************/
    public void loadState(ObjectInputStream oin) throws ConfigException
    {
        try
        {
            int i;
            
            super.loadState(oin);
            this.k        = oin.readInt();
            this.centroid = new DoubleMatrix1D[this.k];
            for (i=0; i<this.k; i++)
            {
                this.centroid[i] = DoubleFactory1D.dense.make((double [])oin.readObject());
            }
        }
         catch(ClassNotFoundException ex) { throw new ConfigException(ex); }
        catch(IOException ex)             { throw new ConfigException(ex); }
    }
    
    public void saveState(ObjectOutputStream oout) throws ConfigException
    {
        try
        {
            int i;
            
            super.saveState(oout);
            oout.writeInt(this.k);
            for (i=0; i<this.k; i++)
            {
                oout.writeObject(this.centroid[i].toArray());
            }
        }
        catch(IOException ex) { throw new ConfigException(ex); }
    }
    
    // *********************************************************\
    // *                       Creation                        *
    // *********************************************************/
    public KMeans()
    {
        super();
        this.name        = "K-Means Clustering";
        this.description = "K-Means Clustering";
    }
}