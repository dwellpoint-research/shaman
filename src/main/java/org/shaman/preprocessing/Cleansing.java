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
import org.shaman.datamodel.AttributeObject;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelPropertyVectorType;
import org.shaman.datamodel.Order;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.Estimator;
import org.shaman.learning.EstimatorTransformation;
import org.shaman.learning.Presenter;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectFactory1D;
import cern.colt.matrix.ObjectMatrix1D;
import cern.jet.random.Uniform;


/**
 * <h2>Automatic Data Cleansing</h2>
 * Various Data Cleaning algorithms.
 */

// **********************************************************\
// *            Data Cleansing Pre-processing Flow          *
// **********************************************************/
public class Cleansing extends EstimatorTransformation implements Estimator
{
    /** Fill in any missing values. */
    public static final int OPERATION_MISSING_VALUES  = 0;
    /** Remove outliers. */
    public static final int OPERATION_REMOVE_OUTLIERS = 1;
    /** Limit the value to the specified range */
    public static final int OPERATION_LIMIT_VALUES    = 2;
    
    /** Fields on which to operate. A field can occure multiple times for different operations. */
    private String []field;
    /** Operation types on the corresponding fields. */
    private int    []oper;
    /** Parameters of the corresponding operation on the corresponding field */
    private double [][]par;
    
    // Data generate during trained that is used by the operations. */
    private double [][]dat;
    
    // Internal Data created by init()
    private int     []actind;    // The indices of the active attributes
    private boolean []opind;     // if [i] = 'true', attribute[i] is involved in 1 or more operations.
    
    // **********************************************************\
    // *           Estimator Interface Implementation           *
    // **********************************************************/
    public DoubleMatrix1D estimate(DoubleMatrix1D instance, double []conf) throws LearnerException
    {
        DoubleMatrix1D out;
        
        try
        {
            out = (DoubleMatrix1D)estimateObject(instance, conf);
        }
        catch(DataModelException     ex) { throw new LearnerException(ex); }
        catch(ConfigException ex) { throw new LearnerException(ex); }
        
        return(out);
    }
    
    public ObjectMatrix1D estimate(ObjectMatrix1D instance, double []conf) throws LearnerException
    {
        ObjectMatrix1D out;
        
        try
        {
           out = (ObjectMatrix1D)estimateObject(instance, conf);
        }
        catch(DataModelException     ex) { throw new LearnerException(ex); }
        catch(ConfigException ex) { throw new LearnerException(ex); }
        
        return(out);
    }
    
    private Object estimateObject(Object instance, double []conf) throws LearnerException, DataModelException, ConfigException
    {
        int              i, cat, inind;
        double           val;
        Object          oval;
        DoubleMatrix1D   in;
        ObjectMatrix1D  oin;
        Object           out;
        DataModel        dmin;
        Attribute        at;
        AttributeDouble  atdo;
        AttributeObject  atob;
        
        // Cast to correct vector. Make output vector.
        dmin = getInputDataModel(0); in = null; oin = null;
        if (this.primitive)
        {
            in  = DoubleFactory1D.dense.make(((DoubleMatrix1D)instance).toArray());
            out = in;
        }
        else
        {
            oin = ObjectFactory1D.dense.make(((ObjectMatrix1D)instance).toArray());
            out = oin;
        }
        
        // Loop over all operations.
        for (i=0; i<field.length; i++)
        {
            at    = dmin.getAttribute(field[i]);
            inind = findInActiveIndices(dmin.getAttributeIndex(field[i]));
            if (oper[i] == OPERATION_MISSING_VALUES)  // Replace Missing Values.
            {
                if (at.hasProperty(Attribute.PROPERTY_CATEGORICAL))
                {
                    if (this.primitive)
                    {
                        atdo = (AttributeDouble)at;
                        val  = in.getQuick(inind);
                        if (atdo.isMissingAsDouble(val))
                        {
                            // Create a random category according to the PDF of non-missing value categories.
                            cat = randomFrom(dat[i]);
                            val  = atdo.getCategoryDouble(cat);
                            in.setQuick(inind, val);
                        }
                    }
                    else
                    {
                        atob = (AttributeObject)at;
                        oval = oin.getQuick(inind);
                        if (atob.isMissingAsObject(oval))
                        {
                            cat  = randomFrom(dat[i]);
                            oval = atob.getCategoryObject(cat);
                            oin.setQuick(inind, oval);
                        }
                    }
                }
                else if (at.hasProperty(Attribute.PROPERTY_CONTINUOUS))
                {
                    // Replace missing values with median.
                    if (this.primitive)
                    {
                        atdo = (AttributeDouble)at;
                        if (atdo.isMissingAsDouble(in.getQuick(inind))) in.setQuick(inind, dat[i][0]);
                    }
                    else ; // Never happens. Checked in DataModel fit.
                }
            }
            else if (oper[i] == OPERATION_LIMIT_VALUES)
            {
                Order or = at.getOrder();
                
                // Limit continuous value
                if (at.hasProperty(Attribute.PROPERTY_CONTINUOUS))
                {
                    if (this.primitive)
                    {
                        atdo = (AttributeDouble)at;
                        val  = in.getQuick(inind);
                        if (!atdo.isMissingAsDouble(val)) // Limit the values to the specified interval.
                        {
                            // Check if the value is outside it interval. If so, adjuyt to internval limit.
                            if (or.order(atdo, val, par[i][0]) == -1) in.setQuick(inind, par[i][0]);
                            if (or.order(atdo, val, par[i][1]) ==  1) in.setQuick(inind, par[i][1]);
                        }
                    }
                    else
                    {
                        ; // Dunno.
                    }
                }
            }
            else if (oper[i] == OPERATION_REMOVE_OUTLIERS)
            {
                // Remove outliers.
            }
        }
        
        return(out);
    }
    
    private int randomFrom(double []pdf)
    {
        int    pos;
        double roz, acc;
        
        // Make random from 0...1
        roz = Uniform.staticNextDouble();
        // Pick the index corresponding to the random value.
        acc = pdf[0]; pos = 0; while ((acc < roz) && (pos < pdf.length-1))  acc += pdf[++pos];
        
        return(pos);
    }
    
    public double estimateError(DoubleMatrix1D instance) throws LearnerException
    {
        return(0);
    }
    
    // **********************************************************\
    // *               Cleansing Operation Specfication         *
    // **********************************************************/
    /**
     * Set the cleansing operations and their parameters.
     * @param _field The names of the fields to operate on.
     * @param _oper The operation identifiers.
     * @param _par The parameters of the operations.
     */
    public void setOperations(String []_field, int []_oper, double [][]_par)
    {
        field = _field;
        oper  = _oper;
        par   = _par;
    }
    
    // **********************************************************\
    // *              Train the Cleansing Operations            *
    // **********************************************************/
    private double []trainMissingValue(int atind, int inind, double []par) throws LearnerException, DataModelException
    {
        int             i;
        double        []dat;
        Attribute       at;
        AttributeDouble atdo;
        AttributeObject atob;
        
        at = dataModel.getAttribute(atind); atdo = null; atob = null;
        if (at.hasProperty(Attribute.PROPERTY_CATEGORICAL))     // Do the categorical missing value thing.
        {
            int     count;
            double  []cpdf;
            double  val;
            Object oval;
            
            if (this.primitive) atdo = (AttributeDouble)at;
            else                atob = (AttributeObject)at;
            
            // Make the PDF of category occurence as data
            cpdf  = new double[at.getNumberOfCategories()];
            for (i=0; i<cpdf.length; i++) cpdf[i] = 0;
            count = 0;
            for (i=0; i<trainData.getNumberOfInstances(); i++)
            {
                if (this.primitive)
                {
                    val = trainData.getInstance(i).getQuick(inind);
                    if (!atdo.isMissingAsDouble(val))  { cpdf[atdo.getCategory(val)]++;  count++; }
                }
                else
                {
                    oval = trainData.getObjectInstance(i).getQuick(inind);
                    if (!atob.isMissingAsObject(oval)) { cpdf[atob.getCategory(oval)]++; count++; }
                }
            }
            if (count > 0) for (i=0; i<cpdf.length; i++) cpdf[i] /= count;
            
            //for (i=0; i<cpdf.length; i++) System.out.print(cpdf[i]+" ");
            //System.out.println(" = PDF of "+at.getName());
            
            dat = cpdf;
        }
        else if (at.hasProperty(Attribute.PROPERTY_CONTINUOUS))   // Or the continuous missing value operation.
        {
            if (this.primitive)
            {
                int    numnm, pos;
                double val;
                double med;
                double []nonmis;
                
                // Find the Median of the non-missing values.
                atdo = this.dmdo.getAttributeDouble(atind);
                
                // Count the non-missing values
                numnm = 0;
                for (i=0; i<trainData.getNumberOfInstances(); i++)
                {
                    val = trainData.getInstance(i).getQuick(inind);
                    if (!atdo.isMissingAsDouble(val)) numnm++;
                }
                
                // Make a list of them.
                nonmis = new double[numnm];
                pos    = 0;
                for (i=0; i<trainData.getNumberOfInstances(); i++)
                {
                    val = trainData.getInstance(i).getQuick(inind);
                    if (!atdo.isMissingAsDouble(val)) nonmis[pos++] = val;
                }
                
                // Find Median
                Arrays.sort(nonmis);
                med = nonmis[nonmis.length/2];
                dat = new double[]{med};
                
                //System.out.println("Median value for missing replacement "+med+" in "+numnm+" values "+atdo.getName());
            }
            else dat = null; // Never occurs. Check in DataModel fit.
        }
        else
        {
            throw new LearnerException("Can't handle missing values of attributes that aren't CONTINUOUS or CATEGORICAL.");
        }
        
        return(dat);
    }
    
    private int findInActiveIndices(int ind)
    {
        int pos = -1;
        for (int i=0; i<actind.length; i++)
        {
            if (actind[i] == ind) pos = i;
        }
        
        return(pos);
    }
    
    public void train() throws LearnerException
    {
        int i;
        int atind;
        int inind;
        
        try
        {
            // Make the Operation Data.
            dat = new double[oper.length][];
            for (i=0; i<field.length; i++)
            {
                if (oper[i] == OPERATION_MISSING_VALUES)
                {
                    atind  = dataModel.getAttributeIndex(field[i]);
                    inind  = findInActiveIndices(atind);
                    dat[i] = trainMissingValue(atind, inind, par[i]);
                }
                else if (oper[i] == OPERATION_REMOVE_OUTLIERS)
                {
                    // Yet To Do (tm)
                }
                else if (oper[i] == OPERATION_LIMIT_VALUES)
                {
                    // No training necessary...
                }
            }
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
    }
    
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
        trainData = _instances;
        dataModel = trainData.getDataModel();
    }
    
    public Presenter getTrainSet()
    {
        return(trainData);
    }
    
    // **********************************************************\
    // *              Clean-up the incomming data               *
    // **********************************************************/
    public Object []transform(Object ob) throws DataFlowException
    {
        int              i;
        ObjectMatrix1D   obin, obout;
        DoubleMatrix1D   doin, doout;
        Object           out;
        Object        []oinbuf;
        double        []inbuf;
        
        out = null;
        if (ob != null)
        {
            try
            {
                // Make the instance and apply the cleaning operations. Make output vector.
                if (!this.primitive)
                {
                    oinbuf = ((ObjectMatrix1D)ob).toArray();
                    obin   = this.learn.getInstanceVector((ObjectMatrix1D)ob);
                    obout  = estimate(obin);
                    for (i=0; i<actind.length; i++) oinbuf[actind[i]] = obout.getQuick(i);
                    out    = ObjectFactory1D.dense.make(oinbuf);
                }
                else
                {
                    inbuf = ((DoubleMatrix1D)ob).toArray();
                    doin  = this.learn.getInstanceVector((DoubleMatrix1D)ob);
                    doout = estimate(doin);
                    for (i=0; i<actind.length; i++) inbuf[actind[i]] = doout.getQuick(i);
                    out   = DoubleFactory1D.dense.make(inbuf);
                }
            }
            catch(LearnerException ex) { throw new DataFlowException(ex); }
            catch(DataModelException ex) { throw new DataFlowException(ex); }
        }
        
        if (out == null) return(null);
        else             return(new Object[]{out});
    }
    
    public String getOutputName(int port)
    {
        if (port == 0) return("Clean Output");
        else return(null);
    }
    
    public String getInputName(int port)
    {
        if (port == 0) return("Dirty Input");
        else return(null);
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
        int        i;
        
        // Instal data-models make some common buffers
        super.init();
        
        // Make some buffers. Mark the fields that are operated on.
        actind = this.dataModel.getActiveIndices();
        opind  = new boolean[dataModel.getAttributeCount()];
        for (i=0; i<opind.length; i++) opind[i] = false;
        for (i=0; i<field.length; i++) opind[dataModel.getAttributeIndex(field[i])] = true;
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    protected DataModel makeOutputDataModel(DataModel dmin) throws DataModelException
    {
        DataModel dmout;
        
        // Make a clone of this DataModel
        try { dmout = (DataModel)dmin.clone(); } catch(CloneNotSupportedException ex) { throw new DataModelException(ex); }
        
        // That's about it. Cleansing doesn't change the DataModel. Just change the name.
        dmout.setName("Cleansed "+dmout.getName());
        
        return(dmout);
    }
    
    public void checkDataModelFit(int port, DataModel dm) throws DataModelException
    {
        int       i;
        Attribute attnow;
        DataModel dmin;
        int       find;
        
        dmin = dm;
        
        // Check if the fields are there and the operations on them compatible with their attributes.
        for (i=0; i<field.length; i++)
        {
            // Find field, check if it's active.
            find = dmin.getAttributeIndex(field[i]);
            if (find != -1)
            {
                attnow   = dmin.getAttribute(find);
                if (attnow.getIsActive())
                {
                    // Check if operation agree with attribute structure.
                    if      (oper[i] == OPERATION_MISSING_VALUES)
                    {
                        if ((dmin.getVectorTypeProperty().equals(DataModelPropertyVectorType.objectVector)) &&
                                (!attnow.hasProperty(Attribute.PROPERTY_CATEGORICAL)))
                            throw new DataModelException("Cannot handle missing values in non-categorical object based data.");
                    }
                    else if (oper[i] == OPERATION_LIMIT_VALUES)
                    {
                        // Limiting means continuous attribute.
                        if (!attnow.hasProperty(Attribute.PROPERTY_CONTINUOUS))
                            throw new DataModelException("Limiting Cleansing Operation needs a continuous attribute. '"+field[i]+"' is not continuous");
                        else
                        {
                            // Continuous with order even.
                            if (attnow.getOrder() == null)
                                throw new DataModelException("Limiting Cleansing Operation needs a continuous attribute with an order. Can't find order in '"+field[i]+"'");
                            if (attnow instanceof AttributeObject)
                                throw new DataModelException("Limiting Cleansing Operation can't handle Object based data.");
                        }
                    }
                    else if (oper[i] == OPERATION_REMOVE_OUTLIERS)
                    {
                        // Outlier Removal needs continuous attribute.
                        if (!attnow.hasProperty(Attribute.PROPERTY_CONTINUOUS))
                            throw new DataModelException("Outlier Removal Cleansing Operation needs a continuous attribute. '"+field[i]+"' is not continuous");
                        
                    }
                    else throw new DataModelException("Unknown operation type specified on Field '"+field[i]+"'");
                }
                else throw new DataModelException("Field '"+field[i]+"' involved in a Cleansing Operation is not active");
            }
            else throw new DataModelException("Cannot find Field '"+field[i]+"' in DataModel");
        }
    }
    
    // **********************************************************\
    // *                   State Persistence                    *
    // **********************************************************/
    public Cleansing()
    {
        super();
        name        = "cleansing";
        description = "Clean-up the data. e.g Fill in missing values, remove outliers.";
    }
}
