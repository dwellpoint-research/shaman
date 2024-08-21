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
import org.shaman.datamodel.DataModelPropertyLearning;
import org.shaman.datamodel.DataModelPropertyVectorType;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.Estimator;
import org.shaman.learning.EstimatorTransformation;
import org.shaman.learning.Presenter;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.ObjectMatrix1D;
import cern.colt.matrix.doublealgo.Statistic;
import cern.colt.matrix.linalg.Blas;
import cern.colt.matrix.linalg.SeqBlas;
import cern.colt.matrix.linalg.SingularValueDecomposition;


/**
 * <h2>Principal Component Analysis</h2>
 * Performs PCA on all it's a input attributes.
 */
// **********************************************************\
// *        Principal Component Analysis Transformation     *
// **********************************************************/
public class PCA extends EstimatorTransformation implements Estimator, Persister
{
    /** Linear PCA type. */
    public final static int TYPE_LINEAR  = 0;  // Linear PCA
    /** Pricipal Curvers. */
    //public final static int TYPE_CURVES  = 1;  // Principal Curves
    /** Kernel PCA */
    //public final static int TYPE_KERNEL  = 2;  // Kernel PCA
    
    private int type;                       // Type of Principal Components
    private int numpc;                      // Number of principal components
    
    // Principal Components
    private double         []mean;          // The average of the attribute values
    private double         []stdev;         // Standard deviation
    private DoubleMatrix1D []pc;            // Principal Component Vectors
    private double         [][]pcraw;       // Principal Component Vectors as Java arrays. (for persistence)
    private double         []sv;            // Singular Values
    
    // Internal buffers. Created by init()
    private double  []allbuf;
    private double  []inbuf;
    private double  []outbuf;
    private int     []outind;
    
    // **********************************************************\
    // *                   Parameter Configuration              *
    // **********************************************************/
    public void setType(int type)        { this.type = type; }
    public int  getType()                { return(this.type); }   
    public void setNumberOfPC(int numpc) { this.numpc = numpc; }
    public int  getNumberOfPC()          { return(this.numpc); }
    
    // **********************************************************\
    // *                Training Implementation                 *
    // **********************************************************/
    public void initializeTraining() throws LearnerException
    {
        // Not really necessary in this case....
    }
    
    public void train() throws LearnerException
    {
        int              i, j, k, numin;
        int              veclen;
        double           instanceweight, dif;
        double         []instance;
        double         []count;
        double         []mean;
        double         []stdev;
        DoubleMatrix2D   mcov;
        
        numin    = this.trainData.getNumberOfInstances();
        veclen   = this.trainData.getInstance(0).size();
        instance = new double[veclen];
        mean     = new double[veclen];
        stdev    = new double[veclen];
        count    = new double[veclen];
        
        // Standardize the data. Take into account the instance weighting.
        for (i=0; i<numin; i++)
        {
            this.trainData.getInstance(i).toArray(instance);
            instanceweight = this.trainData.getWeight(i);
            for(j=0; j<veclen; j++)
            {
                mean[j]  += instance[j] * instanceweight;
                count[j] += instanceweight;
            }
        }
        for (j=0; j<veclen; j++) if (count[j] > 0) mean[j] /= count[j];
                                 else              mean[j]  = 0;
        
        for (i=0; i<numin; i++)
        {
            this.trainData.getInstance(i).toArray(instance);
            instanceweight = this.trainData.getWeight(i);
            for(j=0; j<veclen; j++)
            {
                dif       =  instance[j] - mean[j];
                stdev[j] +=  (dif * dif) * instanceweight;
            }
        }
        for (j=0; j<veclen; j++)
        { 
            if (count[j] > 0) { stdev[j] /= count[j]; stdev[j] = Math.sqrt(stdev[j]); }
            else                stdev[j] = 0;
        }
        
        // Remember mean and standard deviation
        this.mean  = mean;
        this.stdev = stdev;
        
        // Make the weighted covariance matrix
        double [][]dcov = new double[veclen][veclen];
        double     wnow, sumwei;
        sumwei = 0;
        for (i=0; i<numin; i++)
        {
            // Standardize the instance (taking into account the instance weighting)
            this.trainData.getInstance(i).toArray(instance);
            for (j=0; j<veclen; j++) if (stdev[j] != 0) instance[j] = (instance[j] - mean[j]) / stdev[j];
                                     else               instance[j] =  0;
            
            // Make contribution to the covariance matrix. Take into account instance weighting
            wnow    = this.trainData.getWeight(i);
            sumwei += wnow;
            for (j=0; j<veclen; j++)
            {
                for (k=0; k<veclen; k++) dcov[j][k] += instance[j]*instance[k]*wnow;
            }
        }
        for (i=0; i<veclen; i++)
            for (j=0; j<veclen; j++) dcov[i][j] /= sumwei;
        mcov = DoubleFactory2D.dense.make(dcov);
        
        // Do Singular Value Decomposition
        SingularValueDecomposition svd = new SingularValueDecomposition(mcov);
        DoubleMatrix2D u               = svd.getU();
        
        // Get the largest eigenvectors
        this.pc    = new DoubleMatrix1D[this.numpc];
        this.pcraw = new double[this.numpc][];
        for (i=0; i<this.numpc; i++)
        {
            this.pc[i]    = u.viewColumn(i);
            this.pcraw[i] = this.pc[i].toArray();
        }
        
        // Report the singular values
        this.sv = svd.getSingularValues();
    }
    
    public double []getSingularValues()
    {
        return(sv);
    }
    
    public void      setTrainSet(Presenter _instances)
    {
        this.trainData = _instances;
        this.dataModel = this.trainData.getDataModel();
    }
    
    public Presenter getTrainSet()
    {
        return(this.trainData);
    }
    
    public boolean isSupervised()
    {
        return(false);
    }
    
    // **********************************************************\
    // *           Estimator Interface Implementation           *
    // **********************************************************/
    public DoubleMatrix1D estimate(DoubleMatrix1D instance, double []conf) throws LearnerException
    {
        if (type == TYPE_LINEAR) return(estimateLinear(instance, conf));
        else throw new LearnerException("Unknown PCA type");
    }
    
    public DoubleMatrix1D estimateLinear(DoubleMatrix1D instance, double []conf) throws LearnerException
    {
        int              i;
        DoubleMatrix1D   inst;
        DoubleMatrix1D   out;
        DataModelDouble  dmin;
        AttributeDouble  attnow;
        boolean          missing;
        double           []vr;
        
        dmin = this.dmdo;
        
        // Check for any missing values. Can't have that in a linear transformation...
        missing = false;
        try
        {
            for (i=0; (i<this.actind.length) && (!missing); i++)
            {
                attnow = dmin.getAttributeDouble(this.actind[i]);
                if (attnow.isMissingAsDouble(instance.getQuick(i))) missing = true;
            }
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
        
        if (!missing)
        {
            // Standardize the instance
            instance.toArray(this.inbuf);
            for (i=0; i<this.inbuf.length; i++) if (this.stdev[i] != 0) this.inbuf[i] = (this.inbuf[i] - this.mean[i]) / this.stdev[i];
                                                else                    this.inbuf[i] =  0;
            inst = DoubleFactory1D.dense.make(this.inbuf);
            
            // Project the instance over the Principal Components
            vr = new double[this.numpc];
            for (i=0; i<this.numpc; i++)
            {
                vr[i] = this.pc[i].zDotProduct(inst);
            }
            out = DoubleFactory1D.dense.make(vr);
        }
        else
        {
            out = DoubleFactory1D.dense.make(this.numpc);
            for (i=0; i<this.numpc; i++) out.setQuick(i, Double.NaN);
        }
        
        if (conf != null) conf[0] = 1.0;
        
        return(out);
    }
    
    public ObjectMatrix1D estimate(ObjectMatrix1D instance, double []conf) throws LearnerException
    {
        throw new LearnerException("Cannot handler Object based instances");
    }
    
    public double estimateError(DoubleMatrix1D instance) throws LearnerException
    {
        int            i, veclen;
        DoubleMatrix1D out;
        DoubleMatrix1D ri;
        double         err;
        Blas           blas = SeqBlas.seqBlas;
        
        // Calculate the PCA of the input.
        err = 0; veclen = instance.size();
        out = estimate(instance);
        if (out != null)
        {
            // Calculate distance between the inverted PCA and the input.
            ri    = DoubleFactory1D.dense.make(veclen);
            for (i=0; i<this.numpc; i++) blas.daxpy(out.getQuick(i), this.pc[i], ri);
            ri.toArray(this.inbuf);
            for (i=0; i<this.inbuf.length; i++) this.inbuf[i] = (this.inbuf[i] * this.stdev[i]) + this.mean[i];
            ri.assign(this.inbuf);
            err = Statistic.EUCLID.apply(instance, ri);
        }
        
        return(err);
    }
    
    // **********************************************************\
    // *         Calculate the PCA of the incomming Vector      *
    // **********************************************************/
    public Object []transform(Object obin) throws DataFlowException
    {
        int              i;
        DoubleMatrix1D   in, inin;
        DoubleMatrix1D   out;
        DataModelDouble  dmin;
        
        in   = (DoubleMatrix1D)obin; out = null;
        if (in == null) out = null;
        else
        {
            try
            {
                // Make the instance
                in.toArray(this.allbuf);
                dmin = this.dmdo;
                inin = dmin.getLearningProperty().getInstanceVector(in);  // Discard the non-active attributes
                
                // Perform the correct type of PCA on the instance
                out = estimate(inin, null);
                
                // Make the output instance. First the PCA projection followed by the non-active input data.
                if (out != null)
                {
                    for (i=0; i<this.outind.length; i++)
                    {
                        if (this.outind[i] < 0) this.outbuf[i] = out.getQuick(-(this.outind[i]+1));
                        else                    this.outbuf[i] = this.allbuf[this.outind[i]];
                    }
                    out = DoubleFactory1D.dense.make(this.outbuf);
                }
            }
            catch(DataModelException ex) { throw new DataFlowException(ex); }
            catch(LearnerException ex)   { throw new DataFlowException(ex); }
        }
        
        if (out == null) return(null);
        else             return(new Object[]{out});
    }
    
    // **********************************************************\
    // *                       Construction                     *
    // **********************************************************/
    public void init() throws ConfigException
    {
        // Check input datamodel/create output datamodel
        super.init();
        
        // Make some buffers
        DataModel dmin;
        int       i,pos;
        
        dmin   = this.dataModel;
        this.inbuf  = new double[this.actind.length];
        this.allbuf = new double[dmin.getAttributeCount()];
        this.outbuf = new double[dmin.getAttributeCount() - this.actind.length + this.numpc];
        this.outind = new    int[dmin.getAttributeCount() - this.actind.length + this.numpc];
        pos    = 0;
        for (i=0; i<this.numpc; i++) this.outind[pos++] = -(i+1);
        for (i=0; i<dmin.getAttributeCount(); i++)
        {
            if (!dmin.getAttribute(i).getIsActive()) this.outind[pos++] = i;
        }
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    public void create() throws LearnerException
    {
        // Do smart stuff...
    }
    
    protected DataModel makeOutputDataModel(DataModel dmin) throws DataModelException
    {
        DataModelDouble  dmout;
        AttributeDouble  []attout;
        AttributeDouble  nat;
        int              numout, i, pos;
        
        try
        {
            // Copy the input datamodel
            dmout = (DataModelDouble)dmin.clone();
            
            // Make the Output Attributes.
            numout = dmin.getAttributeCount() - dmin.getNumberOfActiveAttributes() + this.numpc;
            attout = new AttributeDouble[numout];
            
            // First the PCA of the active attributes followed by the non-active attributes.
            pos = 0;
            for (i=0; i<this.numpc; i++)
            {
                nat = new AttributeDouble("pca"+i);
                nat.initAsNumberContinuous();
                nat.setIsActive(true);
                attout[pos++] = nat;
            }
            for (i=0; i<dmout.getAttributeCount(); i++)
            {
                if (!dmout.getAttribute(i).getIsActive()) attout[pos++] = dmout.getAttributeDouble(i);
            }
            dmout.setAttributes(attout);
            
            // Move the goal-index in the output model if any.
            DataModelPropertyLearning learn;
            learn = dmout.getLearningProperty();
            if (learn.getHasGoal()) dmout.getLearningProperty().setGoal(learn.getGoalName());
        }
        catch(CloneNotSupportedException ex) { throw new DataModelException(ex); }
        
        return(dmout);
    }
    
    public void checkDataModelFit(int port, DataModel dm) throws DataModelException
    {
        int              i;
        DataModelDouble dmin;
        
        // Check if the input is primitive
        if (!dm.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector))
            throw new DataModelException("Primitive input data required.");
        dmin = (DataModelDouble)dm;
        
        // Check attribute properties
        int []actatt = dmin.getActiveIndices();
        for (i=0; i<actatt.length; i++)
        {
            if (!dmin.getAttribute(actatt[i]).hasProperty(Attribute.PROPERTY_CONTINUOUS)) 
                throw new DataModelException("Continuous input data expected. Attribute '"+dmin.getAttribute(i).getName()+"' is not continuous.");
        }
        
        // Check if number of PCs exceeds range
        if (this.numpc > actatt.length) throw new DataModelException("To many principal components wanted.");
    }
    
    // **********************************************************\
    // *             State Persistence Implementation           *
    // **********************************************************/
    public void loadState(ObjectInputStream oin) throws ConfigException
    {
        try
        {
            super.loadState(oin);
            this.type   = oin.readInt();
            this.numpc  = oin.readInt();
            this.mean   = (double [])oin.readObject();
            this.stdev  = (double [])oin.readObject();
            this.pcraw  = (double [][])oin.readObject();
            this.sv     = (double [])oin.readObject();
            
            // Convert 2D double-array of PCA data to proper COLT vectors.
            this.pc = new DoubleMatrix1D[this.pcraw.length];
            for (int i=0; i<this.pc.length; i++) this.pc[i] = DoubleFactory1D.dense.make(this.pcraw[i]);
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
            oout.writeInt(this.numpc);
            oout.writeObject(this.mean);
            oout.writeObject(this.stdev);
            oout.writeObject(this.pcraw);
            oout.writeObject(this.sv);
        }
        catch(IOException ex) { throw new ConfigException(ex); }
    }
    
    // **********************************************************\
    // *                      Constructor                       *
    // **********************************************************/
    public PCA()
    {
        super();
        name        = "Principal Component Analysis";
        description = "Dimensionality Reduction using linear principal component analysis.";
    }
}