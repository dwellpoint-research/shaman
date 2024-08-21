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
 * <h2>Making Discrete Data Continuous</h2>
 * Makes all it's input's discrete attributes continuous.
 * Can add same noise to discrete data.
 */

// **********************************************************\
// *         Continuity Maker Pre-processing Component      *
// **********************************************************/
public class Continuity extends EstimatorTransformation implements Estimator
{
    /**
     *  Size of the Guassian Blurring around the category membership values
     *  Distance between 2 classes is 1. e.g. 0.5 seems reasonable.
     **/
    private double blurSize;
    
    // Internal Data created by init()
    private boolean []attdis;       // [i] = true if Attribute[i] is discrete
    private double  []inbuf;
    private int       numdis;       // Number of continuous variables
    
    // **********************************************************\
    // *           Estimator Interface Implementation           *
    // **********************************************************/
    public DoubleMatrix1D estimate(DoubleMatrix1D instance, double []conf) throws LearnerException
    {
        int              i;
        DoubleMatrix1D   out;
        AttributeDouble  attnow;
        double           val;
        
        // Make the data continuous.
        instance.toArray(inbuf);
        try
        {
            for (i=0; i<inbuf.length; i++)
            {
                if (attdis[i])
                {
                    // Category Membership Index + Random Guassian Noise of specified spread.
                    attnow = this.dmdo.getAttributeDouble(actind[i]);
                    val    = attnow.getCategory(inbuf[i]);
                    if (val != -1)
                    {
                        val   += cern.jet.random.Normal.staticNextDouble(0, blurSize);
                        inbuf[i] = val;
                    }
                    else inbuf[i] = Double.NaN;
                }
            }
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
        out = DoubleFactory1D.dense.make(inbuf);
        
        return(out);
    }
    
    public ObjectMatrix1D estimate(ObjectMatrix1D instance, double []conf) throws LearnerException
    {
        throw new LearnerException("Cannot handle Object based data");
    }
    
    public double estimateError(DoubleMatrix1D instance) throws LearnerException
    {
        int              i, pos;
        AttributeDouble  attnow;
        DoubleMatrix1D   out;
        DoubleMatrix1D   inv;
        DoubleMatrix1D   incon;
        int              cat;
        double           d;
        
        try
        {
            // Make continuous and Invert result by rounding to closest integer.
            inv   = DoubleFactory1D.dense.make(numdis);
            incon = DoubleFactory1D.dense.make(numdis);
            out   = estimate(instance);
            pos   = 0;
            for (i=0; i<instance.size(); i++)
            {
                if (attdis[i])
                {
                    attnow = this.dmdo.getAttributeDouble(actind[i]);
                    if (!Double.isNaN(out.getQuick(i)))
                    {
                        cat = (int)Math.round(out.getQuick(i));
                        incon.setQuick(pos, instance.getQuick(i));
                        inv.setQuick(pos, cat);
                    }
                    else
                    {
                        incon.setQuick(pos, 0);
                        inv.setQuick(pos, 0);
                    }
                    pos++;
                }
            }
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
        
        // Calculate distance between the inverted estimate and the input.
        d = Statistic.EUCLID.apply(incon, inv);
        
        return(d);
    }
    
    public Object []transformDouble(DoubleMatrix1D vecin) throws LearnerException, DataModelException
    {
        int             i;
        DoubleMatrix1D  esout;
        DoubleMatrix1D  inin;
        double        []conf;

        // Convert input vector to instance format
        vecin.toArray(this.dallbuf);
        inin  = this.learn.getInstanceVector(vecin);  // Discard the non-active attributes
        conf  = new double[1];

        // Estimate output value(s)
        esout = estimate(inin, conf);
        
        // Make an output with the discrete attributes replaced by their continuous counterparts
        for (i=0; i<this.actind.length; i++) this.dallbuf[this.actind[i]] = esout.getQuick(i);
        esout = DoubleFactory1D.dense.make(this.dallbuf);

        if (esout == null) return(null);
        else               return(new Object[]{esout});
    }
    
    // **********************************************************\
    // *                  Continuity Training                   *
    // **********************************************************/
    /**
     * Set the discretization parameters.
     * @param _blurSize Variance of Guassian noise that is added to the discrete values.
     */
    public void setParameters(double _blurSize)
    {
        blurSize = _blurSize;
    }    
    
    // **********************************************************\
    // *                 Train the Continuity                   *
    // **********************************************************/
    public void      initializeTraining() throws LearnerException
    {
        // Not really necessary in this case....
    }
    
    public void      train() throws LearnerException
    {
        // Nothing to do. Doesn't need training.
    }
    
    public boolean isSupervised()
    {
        return(false);
    }
    
    public void      setTrainSet(Presenter _instances)
    {
        trainData = _instances;
        dataModel = trainData.getDataModel();
    }
    
    public Presenter getTrainSet()
    {
        return(trainData);
    }
    
    public String getInputName(int port)
    {
        if (port == 0) return("Discrete Input");
        else return(null);
    }
    
    public String getOutputName(int port)
    {
        if (port == 0) return("Continuous Output");
        else return(null);
    }
        
    // **********************************************************\
    // *                       Construction                     *
    // **********************************************************/
    public void init() throws ConfigException
    {
        int             i;
        DataModelDouble dmin;
        AttributeDouble attnow;
        
        // Make sure the input is compatible with this transformation's data requirements
        super.init();
        dmin = (DataModelDouble)this.dmdo;
        
        // Make some buffers
        this.inbuf  = new double[this.actind.length];
        this.attdis = new boolean[dmin.getNumberOfActiveAttributes()];
        this.numdis = 0;
        for (i=0; i<this.actind.length; i++)
        {
            attnow = dmin.getAttributeDouble(this.actind[i]);
            if (attnow.hasProperty(Attribute.PROPERTY_CATEGORICAL)) { this.attdis[i] = true; this.numdis++; }
            else                                                      this.attdis[i] = false;
        }
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    protected DataModel makeOutputDataModel(DataModel dmin) throws DataModelException
    {
        int             i;
        DataModelDouble dmout;
        AttributeDouble attnow;
        AttributeDouble attdo;
        int           []actind;
        
        try
        { 
            // Start with a clone of this DataModel
            dmout = (DataModelDouble)dmin.clone(); 
            
            // Find the attributes that need changing and re-initialize them as continuous.
            dmout.setName("Continuous "+dmout.getName());
            actind = dmin.getActiveIndices();
            for (i=0; i<actind.length; i++)
            {
                attnow = dmout.getAttributeDouble(actind[i]);
                if (attnow.hasProperty(Attribute.PROPERTY_CATEGORICAL))
                {
                    // Make a continuous clone of the categorical attribute
                    attdo = (AttributeDouble)attnow.clone();
                    attdo.initAsNumberContinuous();
                    dmout.setAttribute(actind[i], attdo);
                }
            }
        }
        catch(CloneNotSupportedException ex) { throw new DataModelException(ex); }
        
        return(dmout);
    }
    
    public void checkDataModelFit(int port, DataModel dm) throws DataModelException
    {
        int             i;
        int           []actind;
        AttributeDouble attnow;
        DataModelDouble dmin;
        boolean         founddis;
        
        // Check if the input is primitive
        if (!dm.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector))
            throw new DataModelException("Primitive input data required.");
        dmin = (DataModelDouble)dm;
        
        // Check attribute properties
        actind   = dmin.getActiveIndices();
        founddis = false;
        for (i=0; i<actind.length; i++)
        {
            attnow = dmin.getAttributeDouble(actind[i]);
            if (attnow.hasProperty(Attribute.PROPERTY_CATEGORICAL)) founddis = true;
        }
        
        // A bit paranoid :
        if (!founddis) throw new DataModelException("No categorical attributes found at input.");
    }
    
    // **********************************************************\
    // *                     Constructor                        *
    // **********************************************************/
    public Continuity()
    {
        super();
        name        = "continuity";
        description = "Making Categorical Attibutes Continuous.";
    }
}
