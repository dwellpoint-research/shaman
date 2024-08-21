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

import java.util.Arrays;

import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.AttributePropertyFuzzy;
import org.shaman.datamodel.AttributePropertyFuzzyContinuous;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelPropertyVectorType;
import org.shaman.datamodel.FMF;
import org.shaman.datamodel.FMFContinuous;
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
import cern.jet.random.Uniform;


/**
 * <h2>Fuzzyfication of Continuous Data</h2>
 * <br>
 * @author Johan Kaers
 * @version 2.0
 */

// **********************************************************\
// *         Fuzzyfication Pre-processing Component         *
// **********************************************************/
public class Fuzzyfication extends EstimatorTransformation implements Estimator
{
    /** Divide in equal intervals between the minimum and maximum. */
    public final static int TYPE_EQUAL_INTERVALS        = 0;
    /** Divide in intervals containing the same number of values. */
    public final static int TYPE_HISTOGRAM_EQUALIZATION = 1;
    private int type;
    
    // Discretization Data
    private int    numberOfIntervals;      // Number of intervals to discretize into
    private double [][]intMin;             // Begin, End and Middle of the discretization intervals
    private double [][]intMax;
    private double [][]intMid;
    
    // Fuzzy Attributes 
    private AttributeDouble []attFuz;
    
    // Internal Data created by init()
    private int     []actind;       // Active attribute indices
    private boolean []attcon;       // [i] = true if Attribute[i] is continuous
    private double  []inbuf;
    private double  []allbuf;
    private int     numcon;         // Number of continuous variables
    
    // **********************************************************\
    // *                DataModel Fuzzyfication                 *
    // **********************************************************/
    private void setFMF (int j, FMFContinuous fmfcon)
    {
        double min, max;
        double fuzmin, fuzmax;
        double width;
        
        min    = intMin[j][0];
        max    = intMax[j][intMax[j].length-1];
        width  = (max-min)/this.numberOfIntervals;
        fuzmin = Uniform.staticNextDoubleFromTo(min, max-width);
        fuzmax = fuzmin+width;
        fmfcon.setBounds(fuzmin, fuzmax); 
    }
    
    private void installFuzzyFunctions() throws DataModelException
    {
        int             i,j;
        FMF           []fmfcon;
        FMFContinuous   fmfnow;
        AttributeDouble attnow;
        AttributePropertyFuzzyContinuous propcon;
        
        for (i=0; i<actind.length; i++)
        {
            if (this.attcon[i])
            {
                attnow  = this.attFuz[i];
                propcon = (AttributePropertyFuzzyContinuous)attnow.getProperty(AttributePropertyFuzzy.PROPERTY_FUZZY);
                
                // Make the continuous fuzzy memebership functions for this attribute.
                fmfcon = new FMF[this.numberOfIntervals];
                for (j=0; j<this.numberOfIntervals; j++)
                {
                    fmfnow    = new FMFContinuous(intMin[i][j], intMax[i][j]);
                    fmfnow.setIntervals(this.intMin[i], this.intMax[i], this.intMid[i]);
                    fmfnow.setIntervalIndex(j);
                    fmfnow.setThreshold(AttributePropertyFuzzyContinuous.getFuzzyThreshold());
                    fmfnow.init();
                    fmfcon[j] = fmfnow;
                    
                    //                   fmfnow    = new FMFContinuous();
                    //                   fmfcon[j] = fmfnow;
                    //                   setFMF(i, fmfnow);
                }
                
                // Install the FMFs in the attribute property
                propcon.setFMF(fmfcon);
                
                //             System.out.println("Attribute "+i);
                //             for (j=0; j<this.numberOfIntervals; j++)
                //             {
                //                System.out.print("   "+intMin[i][j]+" - "+intMax[i][j]+" / ");
                //             }
                //             System.out.println("");
            }
        }
    }
    
    // **********************************************************\
    // *           Estimator Interface Implementation           *
    // **********************************************************/
    public DoubleMatrix1D estimate(DoubleMatrix1D instance, double []conf) throws LearnerException
    {
        DoubleMatrix1D   out;
        
        instance.toArray(inbuf);
        out = DoubleFactory1D.dense.make(inbuf);
        
        return(out);
    }
    
    public ObjectMatrix1D estimate(ObjectMatrix1D instance, double []conf) throws LearnerException
    {
        throw new LearnerException("Do not support Object based data.");
    }
    
    public double estimateError(DoubleMatrix1D instance) throws LearnerException
    {
        return(0.0);
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
        type              = _type;
        numberOfIntervals = _numberOfIntervals;
    }
    
    /**
     * Get the number of discretization intervals
     * @return The number of intervals
     */
    public int getNumberOfIntervals()
    {
        return(numberOfIntervals);
    }
    
    public void      train() throws LearnerException
    {
        try
        {
            if      (type == TYPE_EQUAL_INTERVALS)        trainEqualIntervals();
            else if (type == TYPE_HISTOGRAM_EQUALIZATION) trainHistEq();
            installFuzzyFunctions();
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
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
        
        numins = trainData.getNumberOfInstances();
        numatt = actind.length;
        min = new double[numatt]; max = new double[numatt]; ibuf = new double[numatt];
        for (i=0; i<min.length; i++) min[i] = Double.POSITIVE_INFINITY;
        for (i=0; i<max.length; i++) max[i] = Double.NEGATIVE_INFINITY;
        
        // Find the minimum and maximum of the discretization attributes.
        for (i=0; i<numins; i++)
        {
            trainData.getInstance(i).toArray(ibuf);
            
            for (j=0; j<attcon.length; j++)
            {
                if (attcon[j])
                {
                    attdi = this.dmdo.getAttributeDouble(actind[j]);
                    if ((!attdi.isMissingAsDouble(ibuf[j])) && (min[j] > ibuf[j])) min[j] = ibuf[j];
                    if ((!attdi.isMissingAsDouble(ibuf[j])) && (max[j] < ibuf[j])) max[j] = ibuf[j];
                }
            }
        }
        
        // Calculate the intervals. Take into account the legal interval bounds of the continuous attributes.
        intMin = new double[numatt][];
        intMax = new double[numatt][];
        intMid = new double[numatt][];
        for (i=0; i<attcon.length; i++)
        {
            if (attcon[i])
            {
                attdi     = this.dmdo.getAttributeDouble(actind[i]);
                doLegal   = attdi.getLegalValues();
                intMin[i] = new double[numberOfIntervals];
                intMax[i] = new double[numberOfIntervals];
                intMid[i] = new double[numberOfIntervals];
                
                intlen = (max[i] - min[i]) / numberOfIntervals;
                minnow = min[i];
                maxnow = min[i]+intlen;
                for (j=0; j<numberOfIntervals; j++)
                {
                    intMin[i][j] = minnow;
                    intMax[i][j] = maxnow;
                    intMid[i][j] = (maxnow+minnow)/2;
                    minnow = maxnow;
                    maxnow = maxnow+intlen;
                }
                
                if (doLegal[0] > min[i]) throw new LearnerException("Data out of continuous range bounds. "+min[i]+" < "+doLegal[0]);
                if (doLegal[1] < max[i]) throw new LearnerException("Data out of continuous range bounds. "+max[j]+" > "+doLegal[1]);
                intMin[i][0]                   = doLegal[0];
                intMax[i][numberOfIntervals-1] = doLegal[1];
            }
            else { intMin[i] = null; intMax[i] = null; intMid[i] = null; }
        }
    }
    
    private void trainHistEq() throws LearnerException, DataModelException
    {
        int             i,j;
        int             numins, numatt;
        AttributeDouble attnow;
        double          []attdat;
        DoubleMatrix1D  innow;
        double          dat;
        int             pos, minpos, maxpos;
        double          realpos, posstep;
        double          []doLegal;
        
        numins = trainData.getNumberOfInstances();
        numatt = actind.length;
        attdat = new double[numins];
        intMin = new double[numatt][];
        intMax = new double[numatt][];
        intMid = new double[numatt][];
        
        for (i=0; i<numatt; i++)
        {
            // Notify the Listeners of the progress
            if (attcon[i])
            {
                // Make an array of all non-missing data of this attribute.
                attnow    = this.dmdo.getAttributeDouble(actind[i]);
                doLegal   = attnow.getLegalValues();
                intMin[i] = new double[numberOfIntervals];
                intMax[i] = new double[numberOfIntervals];
                intMid[i] = new double[numberOfIntervals];
                
                if (numins > 0) // If there's data
                {
                    pos = 0;
                    for (j=0; j<numins; j++)
                    {
                        innow = trainData.getInstance(j);
                        dat   = innow.getQuick(i);
                        if (!attnow.isMissingAsDouble(dat)) { attdat[pos++] = dat; }
                    }
                    
                    // Sort the data
                    Arrays.sort(attdat, 0, pos);
                    
                    // Find the numbers on the interval bound
                    posstep = ((double)pos-1) / numberOfIntervals;
                    realpos = posstep; minpos = 0; maxpos = (int)posstep;
                    for (j=0; j<numberOfIntervals; j++)
                    {
                        intMin[i][j] = attdat[minpos];
                        intMax[i][j] = attdat[maxpos];
                        intMid[i][j] = (intMin[i][j] + intMax[i][j])/2;
                        minpos  = maxpos;
                        realpos = realpos+posstep;
                        maxpos  = (int)(realpos);
                    }
                    
                    // Check continuous range bounds. Adjust bins accordingly.
                    if (doLegal[0] > intMin[i][0])                   throw new DataModelException("Data out of continuous range bounds. "+intMin[i][0]+" < "+doLegal[0]);
                    if (doLegal[1] < intMax[i][numberOfIntervals-1]) throw new DataModelException("Data out of continuous range bounds. "+intMax[i][numberOfIntervals-1]+" > "+doLegal[1]);
                    intMin[i][0]                   = doLegal[0];
                    intMax[i][numberOfIntervals-1] = doLegal[1];
                }
                else
                {
                    // No data... All fine.
                    for (j=0; j<numberOfIntervals; j++) { intMin[i][j] = doLegal[0]; intMax[i][j] = doLegal[1]; }
                }
            }
            else { intMin[i] = null; intMax[i] = null; intMid[i] = null; }
        }
    }
    
    // **********************************************************\
    // *    Train the Discretization on the data at input 0     *
    // **********************************************************/
    public void      initializeTraining() throws LearnerException
    {
        // Not really necessary in this case....
    }
    
    public boolean isSupervised()
    {
        return(false);
    }
    
    public void      setTrainSet(Presenter _instances)
    {
        this.trainData = _instances;
        this.dataModel = trainData.getDataModel();
        this.dmdo      = (DataModelDouble)this.dataModel;
    }
    
    public Presenter getTrainSet()
    {
        return(trainData);
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
        
        in   = (DoubleMatrix1D)obin;
        if (in == null) out = null;
        else
        {
            try
            {
                // Make the instance
                in.toArray(allbuf);
                dmin = (DataModel)getInputDataModel(0);
                inin = dmin.getLearningProperty().getInstanceVector(in);  // Discard the non-active attributes
                
                // Discretize the data
                out = estimate(inin, null);
                
                // Make an output with the continuous attributes replaced by their discretized counterparts
                for (i=0; i<actind.length; i++) allbuf[actind[i]] = out.getQuick(i);
                out = DoubleFactory1D.dense.make(allbuf);
            }
            catch(DataModelException ex)     { throw new DataFlowException(ex); }
            catch(LearnerException   ex)     { throw new DataFlowException(ex); }
            catch(ConfigException ex) { throw new DataFlowException(ex); }
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
        int             i;
        DataModel       dmsup;
        DataModelDouble dmin;
        DataModelDouble dmout;
        AttributeDouble attnow;
        
        // Make sure the input is compatible with this transformation's data requirements
        dmsup = getSupplierDataModel(0);
        checkDataModelFit(0, dmsup);
        dmin = (DataModelDouble)dmsup;
        
        // Make the corresponding output data model
        dmout = makeOutputDataModel(dmin);
        
        // Set and create the DataModels
        setInputDataModel(0,dmin);
        setOutputDataModel(0,dmout);
        dataModel = dmin;
        
        // Make some buffers
        actind   = dmin.getActiveIndices();
        attcon   = new boolean[actind.length];
        numcon   = 0;
        for (i=0; i<actind.length; i++)
        {
            attnow = dmin.getAttributeDouble(actind[i]);
            if (attnow.hasProperty(Attribute.PROPERTY_CONTINUOUS)) { attcon[i] = true; numcon++; }
            else                                                     attcon[i] = false;
        }
        allbuf = new double[dmin.getAttributeCount()];
        inbuf  = new double[actind.length];
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    private DataModelDouble makeOutputDataModel(DataModelDouble dmin) throws DataModelException
    {
        int              i;
        DataModelDouble  dmout;
        AttributeDouble  attnow;
        int              []actind;
        boolean          []attcon;
        
        // Start with a clone of this DataModel
        try { dmout = (DataModelDouble)dmin.clone(); } catch(CloneNotSupportedException ex) { throw new DataModelException(ex); }
        
        // Find the attributes that need changing.
        dmout.setName("Fuzzyfied "+dmout.getName());
        actind = dmin.getActiveIndices();
        attcon = new boolean[actind.length];
        for (i=0; i<actind.length; i++)
        {
            attnow = dmin.getAttributeDouble(actind[i]);
            if (attnow.hasProperty(Attribute.PROPERTY_CONTINUOUS)) attcon[i] = true;
            else                                                   attcon[i] = false;
        }
        
        // Put Fuzzy Attributes there.
        this.attFuz = new AttributeDouble[actind.length];
        for (i=0; i<actind.length; i++)
        {
            if (attcon[i])
            {
                try
                {
                    AttributePropertyFuzzyContinuous propFuz = new AttributePropertyFuzzyContinuous();
                    attnow = (AttributeDouble)dmout.getAttributeDouble(actind[i]).clone();
                    attnow.addProperty(AttributePropertyFuzzy.PROPERTY_FUZZY, propFuz);
                    
                    dmout.setAttribute(actind[i], attnow);
                    this.attFuz[i] = attnow;
                }
                catch(CloneNotSupportedException ex) { throw new DataModelException(ex); }
            }
        }
        
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
        this.primitive = true;
        dmin           = (DataModelDouble)dm;
        
        // Check attribute properties
        actind   = dmin.getActiveIndices();
        foundcon = false;
        for (i=0; i<actind.length; i++)
        {
            attnow = dmin.getAttributeDouble(actind[i]);
            if (attnow.hasProperty(Attribute.PROPERTY_CONTINUOUS)) foundcon = true;
        }
        
        // A bit paranoid :
        if (!foundcon) throw new DataModelException("No continuous attributes found at input.");
        
        // Check if the # of intervals is large enough.
        if (numberOfIntervals < 3) throw new DataModelException("Cannot discretize in fewer than 3 intervals.");
    }
    
    // **********************************************************\
    // *                      Constructor                       *
    // **********************************************************/
    public Fuzzyfication()
    {
        super();
        type        = TYPE_HISTOGRAM_EQUALIZATION;
        name        = "fuzzyfication";
        description = "Fuzzyfication of data";
    }
}
