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
package org.shaman.preprocessing;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.TreeMap;

import org.shaman.dataflow.Persister;
import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelPropertyVectorType;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.Estimator;
import org.shaman.learning.EstimatorTransformation;
import org.shaman.learning.Presenter;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;
import cern.colt.matrix.doublealgo.Statistic;


/**
 * <h2>Discretization of Continuous Data</h2>
 * Discretizes all it's input's continuous attributes.
 * Supports splitting up in equal intervals or histogram equalization.
 * <br>
 * <i>Witten I., Frank E. (2000), Data Mining, Section 7.1</i>
 */

// **********************************************************\
// *         Discretizing Pre-processing Component          *
// **********************************************************/
public class Discretization extends EstimatorTransformation implements Estimator, Persister
{
    /** Divide in equal intervals between the minimum and maximum. */
    public final static int TYPE_EQUAL_INTERVALS        = 0;
    /** Divide in intervals containing the same number of values. */
    public final static int TYPE_HISTOGRAM_EQUALIZATION = 1;
    private int type;                // Type of discretization algorithm to use
    private int numberOfIntervals;   // Number of intervals to discretize into
    
    // Discretization Data
    private double [][]intMin;       // Begin, End and Middle of the discretization intervals
    private double [][]intMax;
    private double [][]intMid;
    
    // --- Working buffers ---
    private boolean []attcon;        // [i] = true if Attribute[i] is continuous
    private int       numcon;        // Number of continuous variables
    private double  []allbuf;        // Work buffers : all attributes
    private double  []inbuf;         //                instance attributes
        
    // **********************************************************\
    // *           Estimator Interface Implementation           *
    // **********************************************************/
    public DoubleMatrix1D estimate(DoubleMatrix1D instance, double []conf) throws LearnerException
    {
        int              i;
        DoubleMatrix1D   out;
        double           val;
        boolean          found;
        int              pos;
        
        instance.toArray(this.inbuf);
        try
        {
            // Discretize the continuous input attributes
            for (i=0; i<this.inbuf.length; i++)
            {
                // If the attribute is continuous
                if (this.attcon[i])
                {
                    // Find the interval whose bounds contain the attribute's value
                    val = this.inbuf[i];
                    pos = 0;
                    do
                    {
                        found = ((val >= this.intMin[i][pos]) && (val < this.intMax[i][pos]));
                        if (!found) pos++;
                    }
                    while ((pos < this.numberOfIntervals) && (!found));
                    
                    // Store interval number, when out of bounds, use the missing value indicator
                    if (found) this.inbuf[i] = pos;
                    else       this.inbuf[i] = this.dmdo.getAttributeDouble(this.actind[i]).getMissingAsDouble();
                }
            }
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
        
        // Make an output vector of the discretized input vector
        out = DoubleFactory1D.dense.make(this.inbuf);
        
        return(out);
    }
    
    public double estimateError(DoubleMatrix1D instance) throws LearnerException
    {
        int              i, pos;
        AttributeDouble  attnow;
        DoubleMatrix1D   out;
        DoubleMatrix1D   inv;
        DoubleMatrix1D   incon;
        double           d;
        
        // Make vectors for input and discretized input
        inv   = DoubleFactory1D.dense.make(this.numcon);
        incon = DoubleFactory1D.dense.make(this.numcon);
        
        // First discretize the input
        out   = estimate(instance);
        
        try
        {
            // Estimate distance from input to discretized input
            pos = 0;
            for (i=0; i<instance.size(); i++)
            {
                // If the attribute is continuous
                if (this.attcon[i])
                {
                    // Replace the attribute's value by the middle of the interval in whose bound the value is located
                    attnow = this.dmdo.getAttributeDouble(this.actind[i]);
                    if (!attnow.isMissingAsDouble(instance.getQuick(i)))
                    {
                        inv.setQuick  (pos, this.intMid[i][(int)out.getQuick(i)]);
                        incon.setQuick(pos, instance.getQuick(i));
                    }
                    else
                    {
                        inv.setQuick(pos, 0.0);
                        incon.setQuick(pos, 0.0);
                    }
                    pos++;
                }
            }
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
        
        // Calculate (Euclidean) distance between the input and it's discretized counterpart.
        d = Statistic.EUCLID.apply(incon, inv);
        
        return(d);
    }
    
    public ObjectMatrix1D estimate(ObjectMatrix1D instance, double []conf) throws LearnerException
    {
        throw new LearnerException("Do not support Object based data.");
    }
    
    // **********************************************************\
    // *               Discretization Training                  *
    // **********************************************************/
    /**
     * Set the discretization parameters.
     * @param _type The type of discretization to use.
     * @param _numberOfIntervals The number of interval to divide into.
     */
    public void setParameters(int _type, int _numberOfIntervals)
    {
        this.type              = _type;
        this.numberOfIntervals = _numberOfIntervals;
    }
    
    /**
     * Get the number of discretization intervals
     * @return The number of intervals
     */
    public int getNumberOfIntervals()
    {
        return(this.numberOfIntervals);
    }
    
    public void      train() throws LearnerException
    {
        try
        {
            if      (this.type == TYPE_EQUAL_INTERVALS)        trainEqualIntervals();
            else if (this.type == TYPE_HISTOGRAM_EQUALIZATION) trainHistEq();
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
        
//        // Debug overview output
//        int i,j;
//        for (i=0; i<attcon.length; i++)
//        {
//            System.out.println(dataModel.getAttributeName(actind[i]));
//            if (attcon[i])
//            {
//                for (j=0; j<intMin[i].length; j++)
//                {
//                    System.out.print("["+intMin[i][j]+","+intMax[i][j]+"[ ");
//                }
//                System.out.println();
//            }
//        }
    }
    
    private void trainEqualIntervals() throws LearnerException, DataModelException
    {
        int             i,j;
        int             numins, numatt;
        double          []min;
        double          []max;
        double          []ibuf;
        AttributeDouble attdi;
        double          intlen;
        double          minnow, maxnow;
        double          []doLegal;
        
        numins = this.trainData.getNumberOfInstances();
        numatt = this.actind.length;
        min = new double[numatt]; max = new double[numatt]; ibuf = new double[numatt];
        for (i=0; i<min.length; i++) min[i] = Double.POSITIVE_INFINITY;
        for (i=0; i<max.length; i++) max[i] = Double.NEGATIVE_INFINITY;
        
        // Find the minimum and maximum of the discretization attributes.
        for (i=0; i<numins; i++)
        {
            this.trainData.getInstance(i).toArray(ibuf);
            
            for (j=0; j<this.attcon.length; j++)
            {
                if (this.attcon[j])
                {
                    attdi = this.dmdo.getAttributeDouble(this.actind[j]);
                    if ((!attdi.isMissingAsDouble(ibuf[j])) && (min[j] > ibuf[j])) min[j] = ibuf[j];
                    if ((!attdi.isMissingAsDouble(ibuf[j])) && (max[j] < ibuf[j])) max[j] = ibuf[j];
                }
            }
        }
        
        double [][]intMin, intMax, intMid;
        
        // Calculate the intervals. Take into account the legal interval bounds of the continuous attributes.
        intMin = new double[numatt][];
        intMax = new double[numatt][];
        intMid = new double[numatt][];
        for (i=0; i<this.attcon.length; i++)
        {
            if (this.attcon[i])
            {
                attdi     = this.dmdo.getAttributeDouble(this.actind[i]);
                doLegal   = attdi.getLegalValues();
                intMin[i] = new double[this.numberOfIntervals];
                intMax[i] = new double[this.numberOfIntervals];
                intMid[i] = new double[this.numberOfIntervals];
                
                intlen = (max[i] - min[i]) / this.numberOfIntervals;
                minnow = min[i];
                maxnow = min[i]+intlen;
                for (j=0; j<this.numberOfIntervals; j++)
                {
                    intMin[i][j] = minnow;
                    intMax[i][j] = maxnow;
                    intMid[i][j] = (maxnow+minnow)/2;
                    minnow = maxnow;
                    maxnow = maxnow+intlen;
                }
                
                if (doLegal[0] > min[i]) throw new DataModelException("Data out of continuous range bounds. "+min[i]+" < "+doLegal[0]);
                if (doLegal[1] < max[i]) throw new DataModelException("Data out of continuous range bounds. "+max[j]+" > "+doLegal[1]);
                intMin[i][0]                        = doLegal[0];
                intMax[i][this.numberOfIntervals-1] = doLegal[1];
            }
            else { intMin[i] = null; intMax[i] = null; intMid[i] = null; }
        }
        this.intMin = intMin;
        this.intMax = intMax;
        this.intMid = intMid;
    }
    
    private void trainHistEq() throws LearnerException, DataModelException
    {
        int               i,j;
        int               numins, numatt;
        double        [][]intMin, intMax, intMid;
        
        numins = this.trainData.getNumberOfInstances();
        numatt = this.actind.length;
        intMin = new double[numatt][];
        intMax = new double[numatt][];
        intMid = new double[numatt][];
        for (i=0; i<numatt; i++)
        {
            if (this.attcon[i])
            {
                // Make an array of all non-missing data of this attribute.
                intMin[i] = new double[this.numberOfIntervals];
                intMax[i] = new double[this.numberOfIntervals];
                intMid[i] = new double[this.numberOfIntervals];
                
                if (numins > 0) // If there's data
                {
                    // Do histogram equalization for this attribute
                    histogramEqualization(i, intMin[i], intMax[i], intMid[i]);
                }
                else
                {
                    double []doLegal;
                    
                    // No data... All fine. Just return legal bounds for all intervals.
                    doLegal = this.dmdo.getAttributeDouble(this.actind[i]).getLegalValues();
                    for (j=0; j<this.numberOfIntervals; j++)
                    { 
                        intMin[i][j] = doLegal[0];
                        intMax[i][j] = doLegal[1];
                        intMid[i][j] = (intMin[i][j] + intMax[i][j]) / 2.0;
                    }
                }
            }
            else
            { 
                intMin[i] = null;
                intMax[i] = null;
                intMid[i] = null;
            }
        }
        this.intMin = intMin;
        this.intMax = intMax;
        this.intMid = intMid;
    }
    
    private void histogramEqualization(int i, double []intMin, double []intMax, double []intMid) throws LearnerException, DataModelException
    {
        int             j, numins;
        AttributeDouble attnow;
        DoubleMatrix1D  innow;
        double          dat;
        TreeMap         order;
        Double          val, valwei;
        
        attnow  = this.dmdo.getAttributeDouble(this.actind[i]);
        numins  = this.trainData.getNumberOfInstances();
        order   = new TreeMap();
        
        // Make an ordered map of Value -> Weight of Value
        order = new TreeMap();
        for (j=0; j<numins; j++)
        {
            innow = this.trainData.getInstance(j);
            dat   = innow.getQuick(i);
            if (!attnow.isMissingAsDouble(dat))
            { 
                val    = new Double(dat);
                valwei = (Double)order.get(val);
                if   (valwei == null) valwei = new Double(this.trainData.getWeight(j));
                else                  valwei = new Double(this.trainData.getWeight(j) + valwei.doubleValue());
                order.put(val, valwei);
            }
        }
        
        Iterator itwei, itval;
        double   totpos, posstep;
        double   posnow;
        
        // Count sum of weights over all values.
        itwei  = order.values().iterator();
        totpos = 0;
        while(itwei.hasNext()) totpos += ((Double)itwei.next()).doubleValue();
        
        // Sum of weights divided by number of intervals is number of values per interval.
        posstep = (totpos) / this.numberOfIntervals;
        
        // Find the values on the interval boundaries
        intMin[0] = ((Double)order.keySet().iterator().next()).doubleValue();
        j         = 0;
        posnow    = 0;
        itval     = order.keySet().iterator();
        while(itval.hasNext() && (j<this.numberOfIntervals-1))
        {
            val     = (Double)itval.next();
            valwei  = (Double)order.get(val);
            posnow += valwei.doubleValue();
            while ((posnow > posstep) && (j<this.numberOfIntervals-1))
            {
                if (j > 0) intMin[j] = intMax[j-1];
                           intMax[j] = val.doubleValue();
                           intMid[j] = (intMin[j] + intMax[j]) / 2.0;
                posnow -= posstep;
                j++;
            }
        }
        // Make sure all intervals have been accounted for.
        if (j == this.numberOfIntervals-1)
        {
            intMin[j] = intMax[j-1];
            intMax[j] = ((Double)order.lastKey()).doubleValue();
            intMid[j] = (intMin[j] + intMax[j]) / 2.0;
        }
        
        double          []doLegal;
        
        // Set start of first interval and end of last interval to maximum values of continuous ranges
        doLegal                          = attnow.getLegalValues();
        intMin[0]                        = doLegal[0];
        intMax[this.numberOfIntervals-1] = doLegal[1];
    }
    
    public double [][]getIntervalMin() { return(this.intMin); }
    
    public double [][]getIntervalMax() { return(this.intMax); }
    
    public double [][]getIntervalMid() { return(this.intMid); }
    
    // **********************************************************\
    // *               Train the Discretization                 *
    // **********************************************************/
    public void      initializeTraining() throws LearnerException
    {
    }
    
    public boolean isSupervised()
    {
        return(false);
    }
    
    public void      setTrainSet(Presenter _instances) throws LearnerException
    {
        this.trainData = _instances;
        try
        {
            setDataModel(this.trainData.getDataModel());
        }
        catch(ConfigException ex) { throw new LearnerException(ex); }
    }
    
    public Presenter getTrainSet()
    {
        return(this.trainData);
    }
    
    // **********************************************************\
    // *            Discretize the incomming data               *
    // **********************************************************/
    public Object []transform(Object obin) throws DataFlowException
    {
        int            i;
        DoubleMatrix1D in;
        DoubleMatrix1D inin;
        DoubleMatrix1D out;
        DataModel      dmin;
        
        in = (DoubleMatrix1D)obin;
        if (in == null) out = null;
        else
        {
            try
            {
                // Make the instance
                in.toArray(this.allbuf);
                dmin = (DataModel)getInputDataModel(0);
                inin = dmin.getLearningProperty().getInstanceVector(in);  // Discard the non-active attributes
                
                // Discretize the data
                out = estimate(inin, null);
                
                // Make an output with the continuous attributes replaced by their discretized counterparts
                for (i=0; i<this.actind.length; i++) this.allbuf[this.actind[i]] = out.getQuick(i);
                out = DoubleFactory1D.dense.make(this.allbuf);
            }
            catch(DataModelException ex) { throw new DataFlowException(ex); }
            catch(LearnerException   ex) { throw new DataFlowException(ex); }
            catch(ConfigException ex)    { throw new DataFlowException(ex); }
        }
        
        if (out == null) return(null);
        else             return(new Object[]{out});
    }
    
    // **********************************************************\
    // *                       Construction                     *
    // **********************************************************/
    public void create() throws LearnerException
    {
        // Do stuff.
    }
    
    public void init() throws ConfigException
    {
        // Initialize DataModels and make work buffers.
        super.init();
        
        // Check if the # of intervals is large enough to make sense.
        if (this.numberOfIntervals < 3) throw new DataModelException("Cannot discretize in fewer than 3 intervals.");
        
        // Make some buffers
        int             i;
        AttributeDouble attnow;
        
        this.actind   = this.dataModel.getActiveIndices();
        this.attcon   = new boolean[this.actind.length];
        this.numcon   = 0;
        for (i=0; i<this.actind.length; i++)
        {
            attnow = this.dmdo.getAttributeDouble(this.actind[i]);
            if (attnow.hasProperty(Attribute.PROPERTY_CONTINUOUS)) { this.attcon[i] = true; this.numcon++; }
            else                                                     this.attcon[i] = false;
        }
        this.allbuf = new double[this.dmdo.getAttributeCount()];
        this.inbuf  = new double[actind.length];
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    protected DataModel makeOutputDataModel(DataModel dmin) throws DataModelException
    {
        int              i;
        DataModelDouble  dmout;
        AttributeDouble  attnow;
        double           []legal;
        int              []actind;
        boolean          []attcon;
        
        // Start with a clone of the input DataModel
        try
        { 
            dmout = (DataModelDouble)dmin.clone();
        }
        catch(CloneNotSupportedException ex) { throw new DataModelException(ex); }
        
        // Find the attributes that need to be changed from Continuous to Discrete
        dmout.setName("Discretized "+dmout.getName());
        actind = dmin.getActiveIndices();
        attcon = new boolean[actind.length];
        for (i=0; i<actind.length; i++)
        {
            attnow = ((DataModelDouble)dmin).getAttributeDouble(actind[i]);
            if (attnow.hasProperty(Attribute.PROPERTY_CONTINUOUS)) attcon[i] = true;
            else                                                   attcon[i] = false;
        }
        
        // Make the legal values of the discrete output. 0 to numberofIntervals - 1
        legal = new double[this.numberOfIntervals];
        for (i=0; i<this.numberOfIntervals; i++) legal[i] = i;
        
        // Change the Continuous attributes into Categorical ones.
        try
        {
            for (i=0; i<actind.length; i++)
            {
                if (attcon[i])
                {
                    attnow = (AttributeDouble)dmout.getAttributeDouble(actind[i]).clone();
                    attnow.initAsSymbolCategorical(legal);
                    dmout.setAttribute(actind[i], attnow);
                }
            }
        }
        catch(CloneNotSupportedException ex) { throw new DataModelException(ex); }
        
        return(dmout);
    }
    
    public void checkDataModelFit(int port, DataModel dm) throws DataModelException
    {
        int              i;
        int              []actind;
        AttributeDouble  attnow;
        DataModelDouble  dmin;
        boolean          foundcon;
        
        // Check if the input is primitive
        if (!dm.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector))
            throw new DataModelException("Primitive input data required.");
        dmin = (DataModelDouble)dm;
        
        // Check attribute properties for at least one continuous one.
        actind   = dmin.getActiveIndices();
        foundcon = false;
        for (i=0; i<actind.length; i++)
        {
            attnow = dmin.getAttributeDouble(actind[i]);
            if (attnow.hasProperty(Attribute.PROPERTY_CONTINUOUS)) foundcon = true;
        }
        if (!foundcon) throw new DataModelException("No continuous attributes found at input.");
    }
    
    // **********************************************************\
    // *             State Persistence Implementation           *
    // **********************************************************/
    public void loadState(ObjectInputStream oin) throws ConfigException
    {
        try
        {
            super.loadState(oin);
            this.type              = oin.readInt();
            this.numberOfIntervals = oin.readInt();
            this.intMin = (double [][])oin.readObject();
            this.intMax = (double [][])oin.readObject();
            this.intMid = (double [][])oin.readObject();
        }
        catch(IOException ex)            { throw new ConfigException(ex); }
        catch(ClassNotFoundException ex) { throw new ConfigException(ex); }
    }
    
    public void saveState(ObjectOutputStream oout) throws ConfigException
    {
        try
        {
            super.saveState(oout);
            oout.writeInt(this.type);
            oout.writeInt(this.numberOfIntervals);
            oout.writeObject(this.intMin);
            oout.writeObject(this.intMax);
            oout.writeObject(this.intMid);
        }
        catch(IOException ex) { throw new ConfigException(ex); }
    }
    
    // **********************************************************\
    // *                   State Persistence                    *
    // **********************************************************/
    public Discretization()
    {
        super();
        this.type        = TYPE_HISTOGRAM_EQUALIZATION;
        this.name        = "discretization";
        this.description = "Discretization of continuous attributes in Equal Intervals or using Histogram Equalization";
    }
}