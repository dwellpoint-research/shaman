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


/**
 * <h2>Normalization of Continuous Data</h2>
 * Normalizes or Standardizes all it's continuous attributes.
 */

// **********************************************************\
// *          Normalizing Pre-processing Component          *
// **********************************************************/
public class Normalization extends EstimatorTransformation implements Estimator, Persister
{
    /** Type of operation : scale from [max,min] to [-1,1] */
    public final static int TYPE_NORMALIZE   = 0;
    /** Type of operation : Make mean=0, stdev = 1 */
    public final static int TYPE_STANDARDIZE = 1;
    private int type;
    
    // Normalization Data
    private double []mean;         // The means of the attributes
    private double []stdev;        // The standard deviation of the attributes
    private double []max;          // The maxima
    private double []min;          // The minima of the attributes
    
    // Internal buffers
    private double  []allbuf;
    private double  []inbuf;
    private boolean []attcon;
    private int       numcon;
    
    // **********************************************************\
    // *                 Train the Normalization                *
    // **********************************************************/
    public void      initializeTraining() throws LearnerException
    {
        create();
    }
    
    private void trainNormalize(double []mean, double []stdev, double []max, double []min) throws LearnerException, DataModelException
    {
        int              i, j, numin, veclen;
        double         []ibuf;
        AttributeDouble  attnow;
        
        numin     = this.trainData.getNumberOfInstances();
        veclen    = this.trainData.getInstance(0).size();
        ibuf      = new double[veclen];
        
        // Calculate the maximum and minimum of all attributes
        for (i=0; i<numin; i++)
        {
            this.trainData.getInstance(i).toArray(ibuf);
            for(j=0; j<veclen; j++)
            {
                if (this.attcon[j])
                {
                    attnow = this.dmdo.getAttributeDouble(this.actind[j]);
                    if (!attnow.isMissingAsDouble(ibuf[j]))
                    {
                        if (ibuf[j] > max[j]) max[j] = ibuf[j];
                        if (ibuf[j] < min[j]) min[j] = ibuf[j];
                    }
                }
            }
        }
    }
    
    private void trainStandardize(double []mean, double []stdev, double []max, double []min) throws LearnerException, DataModelException
    {
        int              i, j, numin, veclen;
        double         []instance;
        double         []count;
        double           instanceweight, dif;
        AttributeDouble  attnow;
        
        numin    = this.trainData.getNumberOfInstances();
        veclen   = this.trainData.getInstance(0).size();
        instance = new double[veclen];
        count    = new double[veclen];
        
        // Calculate means. Take into account instance weighting.
        for (i=0; i<veclen; i++) { count[i] = 0; }
        for (i=0; i<numin; i++)
        {
            this.trainData.getInstance(i).toArray(instance);
            instanceweight = this.trainData.getWeight(i);
            for(j=0; j<veclen; j++)
            {
                if (this.attcon[j])
                {
                    attnow = this.dmdo.getAttributeDouble(this.actind[j]);
                    if (!attnow.isMissingAsDouble(instance[j]))
                    {
                        mean[j]  += instance[j] * instanceweight;
                        count[j] += instanceweight;
                    }
                }
            }
        }
        for (j=0; j<veclen; j++)
        {
            if (this.attcon[j])
            {
                if (count[j] > 0) mean[j] /= count[j];
                else              mean[j]  = this.dmdo.getAttributeDouble(this.actind[j]).getMissingAsDouble();
            }
            else mean[j] = 0;
        }
        
        // Calculate standard deviations. Take into account instance weights.
        for (i=0; i<numin; i++)
        {
            this.trainData.getInstance(i).toArray(instance);
            instanceweight = this.trainData.getWeight(i);
            for(j=0; j<veclen; j++)
            {
                if (this.attcon[j])
                {
                    attnow = this.dmdo.getAttributeDouble(this.actind[j]);
                    if (!attnow.isMissingAsDouble(instance[j]))
                    {
                        dif       =  instance[j] - mean[j];
                        stdev[j] +=  (dif * dif) * instanceweight;
                    }
                }
            }
        }
        for (j=0; j<veclen; j++)
        {
            if (this.attcon[j])
            {
                if (count[j] > 0) { stdev[j] /= count[j]; stdev[j] = Math.sqrt(stdev[j]); }
                else                stdev[j]  = this.dmdo.getAttributeDouble(this.actind[j]).getMissingAsDouble();
            }
            else stdev[j] = 0;
        }
    }
    
    public void      train() throws LearnerException
    {
        int     i, veclen;
        
        veclen    = this.trainData.getInstance(0).size();
        
        // Prepare work buffers
        double []mean  = new double[veclen];
        double []stdev = new double[veclen];
        double []max   = new double[veclen];
        double []min   = new double[veclen];
        for (i=0; i<mean.length; i++)
        { 
            mean[i]  = 0;
            stdev[i] = 0;
            max[i]   = Double.NEGATIVE_INFINITY;
            min[i]   = Double.POSITIVE_INFINITY;
        }
        
        try
        {
            // Train for Normalization or Standardization
            if      (this.type == TYPE_NORMALIZE)   trainNormalize(mean, stdev, max, min);
            else if (this.type == TYPE_STANDARDIZE) trainStandardize(mean, stdev, max, min);
            else throw new LearnerException("Cannot train. Unknown normalization type.");
            
//            DoubleMatrix1D mmean, msd;
//            mmean = DoubleFactory1D.dense.make(mean);
//            System.out.println("mean : "+mmean);
//            msd   = DoubleFactory1D.dense.make(stdev);
//            System.out.println("stdev: "+msd);
            
            // Commit trained model
            this.mean  = mean;
            this.stdev = stdev;
            this.max   = max;
            this.min   = min;
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
    }
    
    public void      setTrainSet(Presenter _instances)
    {
        this.trainData = _instances;
        this.dataModel = (DataModelDouble)this.trainData.getDataModel();
    }
    
    public Presenter getTrainSet()
    {
        return(this.trainData);
    }
    
    public boolean isSupervised()
    {
        return(true);
    }
    
    public double []getMax()    { return(this.max); }
    public double []getMin()    { return(this.min); }
    public double []getMean()   { return(this.mean); }
    public double []getStdDev() { return(this.stdev); }
    
    // **********************************************************\
    // *                 Parameter Configuration                *
    // **********************************************************/
    /**
     * Set the type of operation to do.
     * @param _type Kind of operation to do.
     * @see #TYPE_NORMALIZE
     * @see #TYPE_STANDARDIZE
     */
    public void setType(int type)
    {
        this.type = type;
    }
    
    // **********************************************************\
    // *             Normalize the incomming Vector             *
    // **********************************************************/
    public DoubleMatrix1D estimate(DoubleMatrix1D instance, double []conf) throws LearnerException
    {
        int             i;
        DoubleMatrix1D  out;
        DataModelDouble dmin;
        AttributeDouble attnow;
        
        try
        {
            instance.toArray(inbuf);
            dmin = (DataModelDouble)this.dataModel;
            if (this.type == TYPE_NORMALIZE) // Scale the selected attribute values to [-1,1]
            {
                for (i=0; i<this.inbuf.length; i++)
                {
                    if (this.attcon[i])
                    {
                        attnow = dmin.getAttributeDouble(this.actind[i]);
                        if (!attnow.isMissingAsDouble(this.inbuf[i]))
                        {
                            this.inbuf[i] = (((this.inbuf[i] - this.min[i]) / (this.max[i] - this.min[i])) * 2.0) - 1.0;
                        }
                        else this.inbuf[i] = attnow.getMissingAsDouble();
                    }
                }
            }
            else if (this.type == TYPE_STANDARDIZE) // Make mean = 0 and variance = 1 for the selected attributes
            {
                for(i=0; i<this.inbuf.length; i++)
                {
                    if (this.attcon[i])
                    {
                        attnow = dmin.getAttributeDouble(this.actind[i]);
                        if (!attnow.isMissingAsDouble(this.inbuf[i]))
                        {
                            if (this.stdev[i] != 0) this.inbuf[i] = (this.inbuf[i] - this.mean[i]) / this.stdev[i];
                            else                    this.inbuf[i] =  0;
                        }
                        else this.inbuf[i] = attnow.getMissingAsDouble();
                    }
                }
            }
            
            // Make the output vector
            out = DoubleFactory1D.dense.make(this.inbuf);
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
        
        return(out);
    }
    
    public ObjectMatrix1D estimate(ObjectMatrix1D instance, double []conf) throws LearnerException
    {
        throw new LearnerException("Cannot normalize Object based data");
    }
    
    public Object []transformDouble(DoubleMatrix1D vecin) throws LearnerException, DataModelException
    {
        int             i;
        DoubleMatrix1D  inin;
        DoubleMatrix1D  out;
        
        try
        {
            // Make the instance
            vecin.toArray(this.allbuf);
            inin = this.learn.getInstanceVector(vecin);  // Discard the non-active attributes
            
            // Normalize the instance
            out = estimate(inin);
            
            // Replace the normalized (active) attributes
            for (i=0; i<this.actind.length; i++)
                if (this.attcon[i]) this.allbuf[this.actind[i]] = out.getQuick(i);
            out = DoubleFactory1D.dense.make(this.allbuf);
        }
        catch(ConfigException ex) { throw new DataModelException(ex); }
        
        if (out == null) return(null);
        else             return(new Object[]{out});
    }
    
    public Object []transformObject(ObjectMatrix1D vecin) throws LearnerException, DataModelException
    {
       throw new DataModelException("Cannot handle Object based data");
    }
    
    public String getOutputName(int port)
    {
        if (port == 0) return("Normalized Output");
        else return(null);
    }
    
    public String getInputName(int port)
    {
        if (port == 0) return("Continuous Input");
        else return(null);
    }
    
    // **********************************************************\
    // *                       Construction                     *
    // **********************************************************/
    public void init() throws ConfigException
    {
        super.init();
        
        // Make some buffers
        int       i;
        Attribute attnow;
        
        this.inbuf  = new double[this.actind.length];
        this.allbuf = new double[this.dataModel.getAttributeCount()];
        this.attcon = new boolean[actind.length];
        this.numcon = 0;
        for (i=0; i<actind.length; i++)
        {
            attnow = this.dataModel.getAttribute(this.actind[i]);
            if (attnow.hasProperty(Attribute.PROPERTY_CONTINUOUS)) { this.attcon[i] = true; this.numcon++; }
            else                                                     this.attcon[i] = false;
        }
    }
    
    public void cleanUp() throws DataFlowException
    {
    }
    
    public void create() throws LearnerException
    {
        // Do stuff
    }
    
    protected DataModel makeOutputDataModel(DataModel dmin) throws DataModelException
    {
        DataModelDouble  dmout;
        AttributeDouble  nat;
        int              i;
        int              []actind;
        double           []normRange = new double[]{-10,10}; // Should be enough for even the thoughest outliers.
        
        try
        {
            // Start with makeing a copy of the input datamodel
            dmout = (DataModelDouble)dmin.clone();
            if      (this.type == TYPE_NORMALIZE)   dmout.setName("Normalized "+dmout.getName());
            else if (this.type == TYPE_STANDARDIZE) dmout.setName("Standardized "+dmout.getName());
            
            // Modify the input attributes that are normalized.
            actind = dmout.getActiveIndices();
            for (i=0; i<actind.length; i++)
            {
                nat = (AttributeDouble)dmout.getAttribute(actind[i]);
                if (nat.hasProperty(Attribute.PROPERTY_CONTINUOUS))
                {
                    nat = (AttributeDouble)nat.clone();
                    nat.initAsNumberContinuous(normRange);
                    dmout.setAttribute(actind[i], nat);
                }
            }
        }
        catch(CloneNotSupportedException ex) { throw new DataModelException(ex); }
        
        return(dmout);
    }
    
    public void checkDataModelFit(int port, DataModel dm) throws ConfigException
    {
        int             i;
        int           []actatt;
        DataModelDouble dmin;
        AttributeDouble attnow;
        boolean         foundcon;
        
        // Check if the input is primitive
        if (!dm.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector))
            throw new ConfigException("Primitive input data required.");
        else dmin = (DataModelDouble)dm;
        
        // Check attribute properties
        actatt   = dmin.getActiveIndices();
        foundcon = false;
        for (i=0; i<actatt.length; i++)
        {
            attnow = dmin.getAttributeDouble(actatt[i]);
            if (attnow.hasProperty(Attribute.PROPERTY_CONTINUOUS)) foundcon = true;
        }
        if (!foundcon) throw new ConfigException("No continuous attributes found at input.");
    }
    
    public Normalization()
    {
        super();
        type        = TYPE_NORMALIZE;
        name        = "normalization";
        description = "Normalization (scale to [-1,1]) or Standardization (make mean=0, stdev=1) of continuous primitive attributes.";
    }
    
    // **********************************************************\
    // *             State Persistence Implementation           *
    // **********************************************************/
    public void loadState(ObjectInputStream oin) throws ConfigException
    {
        try
        {
            super.loadState(oin);
            this.type  = oin.readInt();
            this.mean  = (double [])oin.readObject();
            this.stdev = (double [])oin.readObject();
            this.max   = (double [])oin.readObject();
            this.min   = (double [])oin.readObject();
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
            oout.writeObject(this.mean);
            oout.writeObject(this.stdev);
            oout.writeObject(this.max);
            oout.writeObject(this.min);
        }
        catch(IOException ex) { throw new ConfigException(ex); }
    }
}
