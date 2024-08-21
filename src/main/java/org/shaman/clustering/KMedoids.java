/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  Based on K-Medoids clustering                        *
 *                                                       *
 *  Copyright (c) 2002-5 Shaman Research                 *
\*********************************************************/
package org.shaman.clustering;

import org.shaman.cbr.DistanceMatrix;
import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.Classifier;
import org.shaman.learning.ClassifierTransformation;
import org.shaman.learning.Presenter;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;
import cern.colt.matrix.doublealgo.Statistic;


/**
 * <h2>K-Medoids Clustering</h2>
 * This algorithm minimizes the total sum of distances of
 * the training vectors to the nearest 'medoid'.
 * The number of clusters has to be specified beforehand.
 * A vector is assigned to the cluster of the nearest medoid.<br>
 * <p>
 * <i> L. Kaufman and P.J. Rousseeuw. (1990) Finding Groups in Data : An Introduction to Cluster Analysis, John Wiley & Sons
 *
 * <br>
 *
 * @author Isabel Van Dooren
 * @author Johan Kaers
 * @version 2.0
 */

// **********************************************************\
// *                  K-Medoids Clustering                  *
// **********************************************************/
public class KMedoids extends ClassifierTransformation implements Classifier
{
    // The types of distances supported
    /** Standard Euclidean Distance. */
    public static final int DISTANCE_EUCLID      = 0;
    /** Canberra Distance Function */
    public static final int DISTANCE_CANBERRA    = 1;
    /** Manhattan Distance */
    public static final int DISTANCE_MANHATTAN   = 2;
    /** Bray-Curtis Distance Function */
    public static final int DISTANCE_BRAY_CURTIS = 3;
    /** Maximum Distance Function */
    public static final int DISTANCE_MAXIMUM     = 4;
    
    // Clustering Parameters
    private int     nr;                        // Number of clusters to find
    private int     distanceType;              // Type of distance.
    private boolean ignoreMissingValues;       // Never use data containing missing values.
    private boolean attributeDistance;         // Use the distance function of the Attributes
    
    // Output
    private double         [][]medoidData;      // The medoids as raw double vector data
    private Object         [][]omedoidData;     // The medoids as raw Object vector data
    private DoubleMatrix1D []medoidInstance;    // The medoids as primtive instance vectors
    private ObjectMatrix1D []omedoidInstance;   // The medoids as Object instance vectors
    
    // K-Medoid Training
    private Statistic.VectorVectorFunction distance;   // The actual distance function used
    private int            []clusvector;       // Cluster assignment for all instances
    private int            []medoids;          // Medoid indices in instance list
    private DistanceMatrix dm;                 // The point-to-point distance cache
    private int            nrobjects;
    private int            nrfeatures;
    
    // *********************************************************\
    // *      K-Medoid Clustering as Classifier Interface      *
    // *********************************************************/
    public int classify(DoubleMatrix1D instance, double []confidence) throws LearnerException
    {
        int    i, cl;
        double max;
        double []dcl = new double[nr];
        
        // Calculate the distance to each of the medoids
        for (i=0; i<nr; i++) dcl[i] = dm.distance(medoidInstance[i], instance);
        
        // Take the closest one
        cl = -1; max = Double.NEGATIVE_INFINITY;
        for (i=0; i<nr; i++)
        {
            if (dcl[i] > max) { cl = i; max = dcl[i]; }
            if (confidence != null) confidence[i] = dcl[i];
        }
        
        return(cl);
    }
    
    public int classify(ObjectMatrix1D instance, double []confidence) throws LearnerException
    {
        int    i, cl;
        double max;
        double []dcl = new double[nr];
        
        // Calculate the distance to each of the medoids
        for (i=0; i<nr; i++) dcl[i] = dm.distance(omedoidInstance[i], instance);
        
        // Take the closest one
        cl = -1; max = Double.NEGATIVE_INFINITY;
        for (i=0; i<nr; i++)
        {
            if (dcl[i] > max) { cl = i; max = dcl[i]; }
            if (confidence != null) confidence[i] = dcl[i];
        }
        
        return(cl);
    }
    
    // **********************************************************\
    // *            Transformation/Flow Interface               *
    // **********************************************************/
    public void init() throws ConfigException
    {
        super.init();
        
        // Object based data should use the Attributes' Distance Functions.
        if (!this.primitive && !attributeDistance)
            throw new ConfigException("Cannot use Object based data and primtive vector distance functions.");
        
        // Attribute Distance Function cannot be used with Distance Functions that include a '(ai+bi)' term.
        if (attributeDistance)
        {
            if ((distanceType == DISTANCE_BRAY_CURTIS) || (distanceType == DISTANCE_CANBERRA))
                throw new ConfigException("Cannot combine Attribute distance function and the specified distance function type.");
        }
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    public String getOutputName(int port)
    {
        if (port == 0) return("Clustered Output");
        else return(null);
    }
    
    public String getInputName(int port)
    {
        if (port == 0) return("Input");
        else return(null);
    }
    
    // **********************************************************\
    // *                    Learner Interface                   *
    // **********************************************************/
    public void initializeTraining() throws LearnerException
    {
        create();
    }
    
    public void train() throws LearnerException
    {
        buildMedoids();
        swapMedoids();
        createClusVector();
    }
    
    public Presenter getTrainSet()
    {
        return(trainData);
    }
    
    public void setTrainSet(Presenter _trainData)
    {
        trainData = _trainData;
        dataModel = trainData.getDataModel();
    }
    
    public boolean isSupervised()
    {
        return(false);
    }
    
    
    // *********************************************************\
    // *           K-Medoids Clusterer Creation                *
    // *********************************************************/
    public void setClusterParameters(int _nr)
    {
        nr = _nr;
    }
    
    public void setDistanceParameters(int _distanceType, boolean _attributeDistance, boolean _ignoreMissingValues)
    {
        distanceType        = _distanceType;
        attributeDistance   = _attributeDistance;
        ignoreMissingValues = _ignoreMissingValues;
    }
    
    public void checkDataModelFit(int port, DataModel dataModel) throws ConfigException
    {
        int      []actind;
        int       i;
        Attribute attnow;
        DataModel dmin;
        
        dmin = dataModel;
        
        // Check attribute properties
        actind = dmin.getActiveIndices();
        
        // If the distance of the attributes is used. Check if it's there.
        if (attributeDistance)
        {
            for (i=0; i<actind.length; i++)
            {
                attnow = dmin.getAttribute(actind[i]);
                if (attnow.getDistance() == null)
                    throw new ConfigException("Cannot find the Distance function for Attribute '"+attnow.getName()+"'");
            }
        }
    }
    
    public void create() throws LearnerException
    {
        // Make internal buffers.
        // Get the correct distance function
        if      (distanceType == DISTANCE_BRAY_CURTIS) distance = Statistic.BRAY_CURTIS;
        else if (distanceType == DISTANCE_CANBERRA)    distance = Statistic.CANBERRA;
        else if (distanceType == DISTANCE_EUCLID)      distance = Statistic.EUCLID;
        else if (distanceType == DISTANCE_MANHATTAN)   distance = Statistic.MANHATTAN;
        else if (distanceType == DISTANCE_MAXIMUM)     distance = Statistic.MAXIMUM;
        else throw new LearnerException("Unknown distance type found!");
        
        // Create the histogram based Distance cache.
        dm = new DistanceMatrix();
        dm.create(DistanceMatrix.TYPE_HISTOGRAM, distance, attributeDistance, ignoreMissingValues, trainData);
        
        // Make some other training and output buffers.
        nrobjects      = trainData.getNumberOfInstances();
        if (this.primitive) nrfeatures = trainData.getInstance(0).size();
        else               nrfeatures = trainData.getObjectInstance(0).size();
        clusvector     = new int[nrobjects];
        medoids        = new int[nr];
        if (this.primitive)
        {
            medoidInstance = new DoubleMatrix1D[nr];
            medoidData     = new double[nr][];
            for (int i=0; i<nr; i++) medoidData[i] = new double[nrfeatures];
        }
        else
        {
            omedoidInstance = new ObjectMatrix1D[nr];
            omedoidData     = new Object[nr][];
            for (int i=0; i<nr; i++) omedoidData[i] = new Object[nrfeatures];
        }
    }
    
    public KMedoids()
    {
        super();
        name        = "K-Medoids";
        description = "K-Medoids Clustering";
    }
    
    
    // ****************************************  BUILD  *********************************************//
    private void buildMedoids() throws LearnerException
    {
        double []sumdistmatrix;             //For each object, sum of distances to other objects
        double min;
        int    minpos = 0;
        
        sumdistmatrix = new double[nrobjects];
        
        //first medoid
        for(int i=0 ; i<nr ; i++) medoids[i] = -1; // medoids posities op -1 initializeren
        min    = Double.POSITIVE_INFINITY;
        minpos = 0; //Position of object with min sum of distances to others
        for(int i=0; i<nrobjects; i++)
        {
            sumdistmatrix[i] = 0;
            for(int j=0; j<nrobjects; j++) sumdistmatrix[i] += dm.distance(i,j);
            if (min > sumdistmatrix[i])
            {
                min    = sumdistmatrix[i];
                minpos = i;
            }
        }
        medoids[0] = minpos;
        
        //other medoids
        double[] Cj = new double[nrobjects];
        byte[]   distje;
        double   max;
        byte     Dj;
        double   dij;
        int      maxpos;
        for(int k=1; k<nr ; k++)
        {
            //System.out.println("Building Medoid of cluster " + k);
            max    = Double.NEGATIVE_INFINITY;
            maxpos = 0;
            int[] meds = VectorMath.selectElements(medoids,k);
            for(int i=0 ; i<nrobjects ; i++)
            {
                if (!VectorMath.isIn(medoids,i))
                {
                    for(int j=0 ; j<nrobjects ; j++)
                    {
                        if (!(VectorMath.isIn(medoids,j)) && (j!=i))
                        {
                            distje = VectorMath.selectElements(dm.distanceTo(j), meds);
                            Dj     = VectorMath.minElement(distje);
                            dij    = dm.distance(i,j);
                            Cj[i]  = Cj[i] + Math.max(0 , dm.distanceBin(Dj)-dij );
                        }
                    }
                    if(Cj[i] > max)
                    {
                        max = Cj[i];
                        maxpos = i;
                    }
                    Cj[i]=0;
                }
            }
            while (max == 0 && VectorMath.isIn(medoids,maxpos)) //be sure initial Medoids are all distinct
            {
                //System.out.println("Initialization of a medoid randomly");
                maxpos++;
            }
            medoids[k] = maxpos;
        }
    }
    
    // *************************** SWAP MEDOIDS ******************************************************** //
    private void swapMedoids() throws LearnerException
    {
        double  Cjih;
        double  C_minval   = -1;
        int     C_iter     = 0;
        int     minposmed  = -1; //position in m_medoids of medoid that is best swapped with
        int     minposcand = -1; //this candidate position in m_bytedistmatrix
        
        while (C_minval<0 && C_iter<20)
        {
            if(C_iter==20) throw new LearnerException("KMedoids Clustering did not converge after 20 iterations");
            
            //Date tel0 = new Date();
            C_minval  = Double.POSITIVE_INFINITY;
            for (int k=0; k<nr; k++)
            {
                int i = medoids[k];
                
                for (int h=0 ; h<nrobjects ; h++)
                {
                    if (!VectorMath.isIn(medoids,h))
                    {
                        Cjih = calculateCostDifference(i,h);
                        if (Cjih < C_minval)
                        {
                            C_minval = Cjih;
                            minposmed  = k;// of m_medoids[k];
                            minposcand = h;
                        }
                    }
                }
            }  //end for medoids loop
            if(C_minval < 0)  medoids[minposmed] = minposcand;
            C_iter++;
            //System.out.println("iteration nr: " + C_iter);
        }  //end while loop
    }
    
    private double calculateCostDifference(int i, int h) throws LearnerException
    {
        //System.out.println("h is " + h);
        
        int    []minposW = new int[1];
        double Ej, val, Dj, dij, dhj;
        double Cjih=0;
        for (int j=0; j<nrobjects; j++)
        {
            if (!VectorMath.isIn(medoids,j) || (j!=h)) //All the notselected objects IF the swap is carried out+the gain that the newly selected object has
            {
                minposW[0] = 0;
                byte[] distje = VectorMath.selectElements(dm.distanceTo(j), medoids);
                Dj  = dm.distanceBin(VectorMath.minElement(distje, minposW));
                dij = dm.distance(j,i);
                dhj = dm.distance(j,h);
                if (Dj == dij) //i is closest medoid for j
                {
                    Ej = Double.POSITIVE_INFINITY;   //distance to j of second nearest medoid for j
                    for (int m=0; m<nr; m++)
                    {
                        val = dm.distance(medoids[m], j);
                        if(m != minposW[0] && (val < Ej)) Ej = val;
                    }
                    if (dhj < Ej) Cjih += dhj - dij;   // new candidate medoid is closer to j than second nearest
                    else          Cjih += Ej - Dj;     // second nearest medoid is closer to j than new candidate medoid
                }
                else if (Dj > dhj) Cjih += dhj - Dj; // new candidate medoid is closer to j than considered medoid
            }
        }
        return Cjih;
    }
    
    // *********************************** CLUSVECTOR ******************************************************** //
    private void createClusVector() throws LearnerException
    {
        int  i;
        int []minposW = new int[1];
        
        // Find the cluster for the train data...
        for(i=0 ; i<nrobjects; i++)
        {
            minposW[0] = 0;
            if(VectorMath.isIn(medoids, i, minposW)) clusvector[i] = minposW[0];
            else
            {
                //byte[] distje = VectorMath.selectElements(dm.distanceTo(i), medoids);
                minposW[0] = 0;
                //int minval = VectorMath.minElement(distje, minposW);
                clusvector[i] = minposW[0];
            }
        }
        
        // Make the clusters
        if (this.primitive)
        {
            //System.out.println("The primtive vector medoids are:");
            for (i=0; i<medoids.length; i++)
            {
                medoidInstance[i] = trainData.getInstance(medoids[i]);
                medoidInstance[i].toArray(medoidData[i]);
                //System.out.println(medoidInstance[i]);
            }
        }
        else
        {
            //System.out.println("The object vector medoids are:");
            for (i=0; i<medoids.length; i++)
            {
                omedoidInstance[i] = trainData.getObjectInstance(medoids[i]);
                omedoidInstance[i].toArray(omedoidData[i]);
                //System.out.println(omedoidInstance[i]);
            }
        }
    }
}