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

import hep.aida.ref.Histogram1D;

import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.AttributeObject;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelObject;
import org.shaman.datamodel.DataModelPropertyVectorType;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.Presenter;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;
import cern.colt.matrix.doublealgo.Statistic;


/**
 * Memory-Cache for the distances between all pairs of instances from a given set.
 * Can store the raw distances (as doubles) or store them as histogram bin indices (bytes).
 * Alternatively, it defers all calculations until they are specifically requested.
 * Lets you define the type of distance function to use, wheter it should use the distance
 * functions of the attributes, if it should ignore missing values and how to weight to
 * different attributes.
 */

// **********************************************************\
// *          Distance between instance cache               *
// **********************************************************/
public class DistanceMatrix
{
    // Type of Distance Matrix
    /** Fall through matrix. Do not cache any distances, just always calculate when asked. */
    public static final int TYPE_NONE      = 0;   // Fall through. Always calculate distance.
    /** Cache the raw distances (as doubles) */
    public static final int TYPE_RAW       = 1;   // Raw distance matrix.
    /** Cache the distances as bin indices (as bytes) in the distance histogram */
    public static final int TYPE_HISTOGRAM = 2;   // Use histogram bin indices (0-255) as distance cache.
    
    // Type of Distance Cache to use
    private int      type;
    // Use the Attributes' distance function first...
    private boolean  attributeDistance;
    // Ignore Missing Values when calculating distances...
    private boolean  ignoreMissingValues;
    // Type of distance used.
    private Statistic.VectorVectorFunction distance;
    
    // The distance matrix
    // -------------------
    private double [][]dmd;           // Raw double distance values (type == TYPE_RAW)
    private byte   [][]dmb;           // Indices of the Histogram's bins (type == TYPE_HISTOGRAM)
    
    private Histogram1D   histdist;   // The histogram of the distances.
    private double      []binmid;     // Midpoints of the bins of the distance histrogram
    
    // The data whose distance is cached here and it's data model
    private Presenter       instance;
    private DataModel       dataModel;
    private DataModelDouble dmdo;
    private DataModelObject dmob;
    private boolean         primitive;
    private int           []actind;
    
    // **********************************************************\
    // *          Distance Between 2 Primitive Vectors          *
    // **********************************************************/
    /**
     * Explicity calculate the distance between the 2 primitive vectors
     * @param v1 Primitive Instance Vector 1
     * @param v2 Primitive Instance Vector 2
     * @throws LearnerException If the distance could not be calculated.
     * @return The distance between the vectors.
     */
    public double distance(DoubleMatrix1D v1, DoubleMatrix1D v2) throws LearnerException
    {
        double d;
        
        d = calculateDistance(v1, v2, this.actind);
        
        return(d);
    }
    
    /**
     * Explicitly calculate the distance between 2 Object vectors
     * @param ov1 Object Instance Vector 1
     * @param ov2 Object Instance Vector 2
     * @throws LearnerException If the distance cannot be calculated.
     * @return The distance between the vectors.
     */
    public double distance(ObjectMatrix1D ov1, ObjectMatrix1D ov2) throws LearnerException
    {
        double d;
        
        d = calculateDistance(ov1, ov2, this.actind);
        
        return(d);
    }
    
    // **********************************************************\
    // *                    Distance Lookup                     *
    // **********************************************************/
    /**
     * Get the distance between instance i and j of the instance set.
     * @param i Index 1 in the set
     * @param j Index 2 in the set
     * @return The distance betweem the 2 vectors.
     * @throws LearnerException If the distance could not be retrieved from the cache or calculated.
     */
    public double distance(int i, int j) throws LearnerException
    {
        DoubleMatrix1D v1, v2;
        ObjectMatrix1D ov1, ov2;
        double         d;
        
        d = 0;
        if      (this.type == TYPE_RAW)
        {
            d = this.dmd[i][j];
        }
        else if (this.type == TYPE_HISTOGRAM)
        {
            int bi = this.dmb[i][j];
            if ((bi >= this.binmid.length) || (bi < 0)) d = 0;
            else                                        d = this.binmid[bi];
        }
        else if (this.type == TYPE_NONE)
        {
            if (this.primitive)
            {
                v1 = this.instance.getInstance(i);
                v2 = this.instance.getInstance(j);
                d  = calculateDistance(v1, v2, this.actind);
            }
            else
            {
                ov1 = this.instance.getObjectInstance(i);
                ov2 = this.instance.getObjectInstance(j);
                d   = calculateDistance(ov1, ov2, this.actind);
            }
        }
        
        return(d);
    }
    
    /**
     * Gives an array of bin indices of the distances between instance i and all the other instances.
     * Make sure type is TYPE_HISTORGRAM.
     * @param i The index of the instance
     * @return Array of bin indices
     */
    public byte []distanceTo(int i)
    {
        return(this.dmb[i]);
    }
    
    /**
     * Give the distance between instances at index i and j as an index in the histogram bins.
     * Make sure type is TYPE_HISTOGRAM.
     * @param i Index of the first instance
     * @param j Index of the second instance.
     * @return Distance between the instances as an index in the histogram bins.
     */
    public byte distanceAsBin(int i, int j) { return(this.dmb[i][j]); }
    
    /**
     * Give the distance corresponding to the histogram bin with the given index.
     * @param i The histogram bin index.
     * @return The distance corresponding to histogram bin.
     */
    public double distanceBin(int i)
    {
        if ((i>=this.binmid.length) || (i<0)) return(Double.POSITIVE_INFINITY);
        else                                  return(this.binmid[i]);
    }
    
    // **********************************************************\
    // *               Make the distance matrix                 *
    // **********************************************************/
    private double calculateDistance(ObjectMatrix1D a, ObjectMatrix1D b, int []ind) throws LearnerException
    {
        // Distance Calculation taking into account all parameters.
        int             i,j;
        AttributeObject attnow;
        ObjectMatrix1D  ap;
        ObjectMatrix1D  bp;
        DoubleMatrix1D  ad;
        DoubleMatrix1D  bd;
        double          d, dnow;
        int           []view;
        int           []indp;
        int             numleg;
        
        try
        {
            // Filter out any dimensions containing missing values.
            if (this.ignoreMissingValues)
            {
                // Count the number of dimensions with a missing value in one or both vectors.
                numleg = 0;
                for (i=0; i<a.size(); i++)
                {
                    attnow = this.dmob.getAttributeObject(ind[i]);
                    if (!attnow.isMissingAsObject(a.getQuick(i)) &&
                        !attnow.isMissingAsObject(b.getQuick(i))) numleg++;
                }
                if (numleg != a.size()) // If missing values present.
                {
                    // Make a list of dimensions containing no missing values.
                    view = new int[numleg];
                    indp = new int[numleg];
                    j = 0;
                    for (i=0; i<a.size(); i++)
                    {
                        attnow = this.dmob.getAttributeObject(ind[i]);
                        if (!attnow.isMissingAsObject(a.getQuick(i)) &&
                            !attnow.isMissingAsObject(b.getQuick(i))) { view[j] = i; indp[j] = ind[i]; j++; }
                    }
                    
                    // Make a view on the vectors containing only legal values.
                    ap = a.viewSelection(view);
                    bp = b.viewSelection(view);
                }
                else { ap = a; bp = b; numleg = a.size(); indp = ind; }
            }
            else { ap = a; bp = b; numleg = a.size(); indp = ind; }
            
            if (this.attributeDistance)
            {
                // Use the attributes' distance functions for all dimensions.
                ad = DoubleFactory1D.dense.make(numleg);
                bd = DoubleFactory1D.dense.make(numleg); // Keep b as zero. Make sure the distance function is OK with this.
                for (i=0; i<numleg; i++)
                {
                    attnow = this.dmob.getAttributeObject(indp[i]);
                    dnow   = attnow.distance(ap.getQuick(i), bp.getQuick(i));
                    ad.setQuick(i, dnow);
                }
                
                // Apply the distance function.
                d = this.distance.apply(ad, bd);
                if (numleg > 0) d /= numleg;
            }
            else
            {
                d = Double.NaN; // Not possible.
            }
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
        
        return(d);
    }
    
    private double calculateDistance(DoubleMatrix1D a, DoubleMatrix1D b, int []ind) throws LearnerException
    {
        // Distance Calculation taking into account all parameters.
        int             i,j;
        AttributeDouble attnow;
        DoubleMatrix1D  ap;
        DoubleMatrix1D  bp;
        DoubleMatrix1D  ad;
        DoubleMatrix1D  bd;
        double          d, dnow;
        int           []view;
        int           []indp;
        int             numleg;
        
        try
        {
            // Filter out any dimensions containing missing values.
            if (this.ignoreMissingValues)
            {
                // Count the number of dimensions with a missing value in one or both vectors.
                numleg = 0;
                for (i=0; i<a.size(); i++)
                {
                    attnow = this.dmdo.getAttributeDouble(ind[i]);
                    if (!attnow.isMissingAsDouble(a.getQuick(i)) &&
                        !attnow.isMissingAsDouble(b.getQuick(i))) numleg++;
                }
                if (numleg != a.size()) // If missing values present.
                {
                    // Make a list of dimensions containing no missing values.
                    view = new int[numleg];
                    indp = new int[numleg];
                    j = 0;
                    for (i=0; i<a.size(); i++)
                    {
                        attnow = this.dmdo.getAttributeDouble(ind[i]);
                        if (!attnow.isMissingAsDouble(a.getQuick(i)) &&
                            !attnow.isMissingAsDouble(b.getQuick(i))) { view[j] = i; indp[j] = ind[i]; j++; }
                    }
                    
                    // Make a view on the vectors containing only legal values.
                    ap = a.viewSelection(view);
                    bp = b.viewSelection(view);
                }
                else { ap = a; bp = b; numleg = a.size(); indp = ind; }
            }
            else { ap = a; bp = b; numleg = a.size(); indp = ind; }
            
            if (this.attributeDistance)
            {
                // Use the attributes' distance functions for all dimensions.
                ad = DoubleFactory1D.dense.make(numleg);
                bd = DoubleFactory1D.dense.make(numleg); // Keep b as zero. Make sure the distance function is OK with this.
                for (i=0; i<numleg; i++)
                {
                    attnow = this.dmdo.getAttributeDouble(indp[i]);
                    dnow   = attnow.distance(ap.getQuick(i), bp.getQuick(i));
                    ad.setQuick(i, dnow);
                }
            }
            else { ad = ap; bd = bp; }
            
            // Apply the distance function.
            d = this.distance.apply(ad, bd);
            if (numleg > 0) d /= numleg;
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
        
        return(d);
    }
    
    /**
     * Make the distance cache of the already specified type.
     * @throws LearnerException If the distance cache could not be made.
     */
    private void make() throws LearnerException
    {
        if      (this.type == TYPE_NONE)      { }
        else if (this.type == TYPE_RAW)       { makeRaw(); }
        else if (this.type == TYPE_HISTOGRAM) { makeHistogram(126); }
    }
    
    private void makeRaw() throws LearnerException
    {
        int             i,j;
        int             numin;
        ObjectMatrix1D ov1, ov2;
        DoubleMatrix1D  v1, v2;
        double          d;
        
        numin  = this.instance.getNumberOfInstances();
        dmd    = new double[numin][numin];
        if (this.primitive)
        {
            // Primitive Data.
            for (i=0; i<numin; i++)
            {
                v1 = this.instance.getInstance(i);
                for (j=0; (j<numin) && (j<=i); j++)
                {
                    v2 = this.instance.getInstance(j);
                    d  = calculateDistance(v1, v2, this.actind);
                    this.dmd[i][j] = d;
                    this.dmd[j][i] = d;
                }
            }
        }
        else
        {
            // Object Based Data.
            for (i=0; i<numin; i++)
            {
                ov1 = this.instance.getObjectInstance(i);
                for (j=0; (j<numin) && (j<=i); j++)
                {
                    ov2 = this.instance.getObjectInstance(j);
                    d  = calculateDistance(ov1, ov2, this.actind);
                    this.dmd[i][j] = d;
                    this.dmd[j][i] = d;
                }
            }
        }
    }
    
    private void makeHistogram(int bins) throws LearnerException
    {
        int            i,j;
        int            numin;
        ObjectMatrix1D ov1, ov2;
        DoubleMatrix1D v1, v2;
        double         d, dmin, dmax;
        
        numin  = this.instance.getNumberOfInstances();
        
        // Find the minimum and maximam distance between 2 points
        dmin = Double.POSITIVE_INFINITY; dmax = Double.NEGATIVE_INFINITY;
        if (this.primitive)
        {
            // For primitive Data.
            for (i=0; i<numin; i++)
            {
                v1 = this.instance.getInstance(i);
                for (j=0; (j<numin) && (j<=i); j++)
                {
                    v2 = this.instance.getInstance(j);
                    d  = calculateDistance(v1, v2, this.actind);
                    if      (d < dmin) dmin = d;
                    else if (d > dmax) dmax = d;
                }
                if (i%100 == 0) System.out.println("Making primitive data histogram distances "+i+" min/max "+dmin+" "+dmax);
            }
            System.out.println("number of instances : "+numin+" min/max "+dmin+" "+dmax);
        }
        else
        {
            // For Object Based Data.
            for (i=0; i<numin; i++)
            {
                ov1 = this.instance.getObjectInstance(i);
                for (j=0; (j<numin) && (j<=i); j++)
                {
                    ov2 = this.instance.getObjectInstance(j);
                    d   = calculateDistance(ov1, ov2, this.actind);
                    if      (d < dmin) dmin = d;
                    else if (d > dmax) dmax = d;
                }
                if (i%100 == 0) System.out.println("Making object data histogram distances "+i);
            }
            System.out.println("number of instances : "+numin+" min/max "+dmin+" "+dmax);
        }
        
        // Make a histogram of the distances. Fill the bin index distance.
        this.histdist = new Histogram1D("Distance Histogram", bins, dmin, dmax);
        this.dmb      = new byte[numin][numin];
        if (this.primitive)
        {
            // For primitive data.
            for (i=0; i<numin; i++)
            {
                v1 = this.instance.getInstance(i);
                for (j=0; (j<numin) && (j<=i); j++)
                {
                    v2 = this.instance.getInstance(j);
                    d  = calculateDistance(v1, v2, this.actind);
                    this.histdist.fill(d);
                    this.dmb[i][j] = (byte)this.histdist.xAxis().coordToIndex(d);
                    this.dmb[j][i] = this.dmb[i][j];
                }
            }
        }
        else
        {
            // Object Based Data
            for (i=0; i<numin; i++)
            {
                ov1 = this.instance.getObjectInstance(i);
                for (j=0; (j<numin) && (j<=i); j++)
                {
                    ov2 = this.instance.getObjectInstance(j);
                    d   = calculateDistance(ov1, ov2, this.actind);
                    this.histdist.fill(d);
                    this.dmb[i][j] = (byte)this.histdist.xAxis().coordToIndex(d);
                    this.dmb[j][i] = this.dmb[i][j];
                }
            }
        }
        
        // Make the bin middle value vector
        this.binmid = new double[bins];
        for (i=0; i<bins; i++) this.binmid[i] = this.histdist.xAxis().binCentre(i);
    }
    
    
    // **********************************************************\
    // *                      Construction                      *
    // **********************************************************/
    /**
     * Create a fall-through matrix. Calculate distances using the given distance function.
     * @param _distance The distance function. (vector x vector -> number)
     * @param _attributeDistance Use the attributes' distance relations to calculate the distance.
     *                           Only works with distance functions using (a_i - b_i)
     * @param _ignoreMissingValues Never use missing value data while calculating distances.
     * @param _dataModel The dataModel of the instance that will be used.
     * @throws LearnerException never
     */
    public void create(Statistic.VectorVectorFunction _distance, boolean _attributeDistance, boolean _ignoreMissingValues, DataModel _dataModel) throws LearnerException
    {
        this.type                = TYPE_NONE;
        this.distance            = _distance;
        this.attributeDistance   = _attributeDistance;
        this.ignoreMissingValues = _ignoreMissingValues;
        setDataModel(_dataModel);
        make();
     }
    
    /**
     * Create a distance cache of the given type.
     * Use the specified distance function to calculate the distances
     * between the instances in the given set of instances.
     * @param _type The type of distance matrix to make.
     * @param _distance The distance function
     * @param _attributeDistance Use the attributes' distance relations to calculate the distance.
     *                           Only works with distance functions using (a_i - b_i)
     * @param _ignoreMissingValues Never use missing value data while calculating distances.
     * @param _trainset The set of instances.
     * @throws LearnerException If something goes wrong in creating the distance cache.
     */
    public void create(int _type, Statistic.VectorVectorFunction _distance, boolean _attributeDistance, boolean _ignoreMissingValues, Presenter _instance) throws LearnerException
    {
        this.type                = _type;
        this.distance            = _distance;
        this.attributeDistance   = _attributeDistance;
        this.ignoreMissingValues = _ignoreMissingValues;
        this.instance            = _instance;
        setDataModel(this.instance.getDataModel());
        make();
    }
    
    public void setDataModel(DataModel _dataModel)
    {
        this.dataModel = _dataModel;
        if (_dataModel.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector))
        {
            this.dmdo      = (DataModelDouble)_dataModel;
            this.primitive = true;
        }
        else
        {
            this.dmob      = (DataModelObject)_dataModel;
            this.primitive = false;
        }
        this.actind = _dataModel.getActiveIndices();
    }
    
    public DataModel getDataModel() { return(this.dataModel); }
    
    /**
     * Make a DistanceMatrix of TYPE_RAW with the Euclidean Distance function.
     */
    public DistanceMatrix()
    {
        this.type              = TYPE_RAW;
        this.distance          = Statistic.EUCLID;
        this.attributeDistance = false;
    }
}
