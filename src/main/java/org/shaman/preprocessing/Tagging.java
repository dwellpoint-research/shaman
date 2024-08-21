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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.shaman.dataflow.Transformation;
import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.AttributeObject;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelPropertyVectorType;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Tagging of Data with Results</h2>
 * Copies a number of attributes of the 2th input into
 * the data vector at the 1st input.
 * Can be used to <b>tag</b> an input data-vector with
 * results from a machine-learning algorithm that processed
 * this vector in another flow-path.
 */

// **********************************************************\
// *  Tagging of data vectors with machine-learning results *
// **********************************************************/
public class Tagging extends Transformation
{
    /** The (tag source attribute name -> tag destination attribute name) */
    private Map           tags;
    
    // Buffers (made by init())
    private DataModel     dmDat;
    private DataModel     dmTag;
    private Attribute [][]tagatt;
    private int       [][]tagind;
    
    public String []getVersion() { return(new String[]{"tagging", "1.0"}); }
    
    // **********************************************************\
    // *   Copy the source's tag-field data into the Desination *
    // **********************************************************/
    private Object tag(Object obdat, Object obtag) throws LearnerException, DataModelException
    {
        int              i;
        ObjectMatrix1D  ovecdat;
        DoubleMatrix1D   vecdat;
        ObjectMatrix1D  ovectag;
        DoubleMatrix1D   vectag;
        ObjectMatrix1D  oout;
        DoubleMatrix1D  dout;
        Object           out;
        double           val;
        Object          oval;
        
        vecdat = null; ovecdat = null; vectag = null; ovectag = null;
        if (isDataPrimitive())  vecdat = (DoubleMatrix1D)obdat;
        else                   ovecdat = (ObjectMatrix1D)obdat;
        if (isTagPrimitive())   vectag = (DoubleMatrix1D)obtag;
        else                   ovectag = (ObjectMatrix1D)obtag;
        
        // Output is input vector with tags.
        if (isDataPrimitive())
        {
            // Output is Primitive
            dout = vecdat;
            if (isTagPrimitive()) // Tag is also primitive
            {
                for (i=0; i<tagind.length; i++) dout.setQuick(tagind[i][0], vectag.getQuick(tagind[i][1]));
            }
            else
            { // Tag is Object based
                for (i=0; i<tagind.length; i++)
                {
                    oval = ovectag.getQuick(tagind[i][1]);
                    val  = ((AttributeObject)tagatt[i][1]).getDoubleValue(oval, (AttributeDouble)tagatt[i][0]);
                    dout.setQuick(tagind[i][0], val);
                }
            }
            out = dout;
        }
        else
        {
            // Data is Object Based
            oout = ovecdat;
            if (isTagPrimitive()) // Tag is primitive
            {
                for (i=0; i<tagind.length; i++)
                {
                    val  = vectag.getQuick(tagind[i][1]);
                    oval = ((AttributeDouble)tagatt[i][1]).getObjectValue(val, (AttributeObject)tagatt[i][0]);
                    oout.setQuick(tagind[i][0], oval);
                }
                System.out.println(oout.viewPart(0, 3));
            }
            else
            {
                // Tag is also object based
                for (i=0; i<tagind.length; i++) oout.setQuick(tagind[i][0], ovectag.getQuick(tagind[i][1]));
            }
            out = oout;
        }
        
        if (out == null) return(null);
        else             return(out);
    }
    
    public String getOutputName(int port)
    {
        if (port == 0) return("Tagged Output");
        else return(null);
    }
    
    public String getInputName(int port)
    {
        if      (port == 0) return("Data Vector Input");
        else if (port == 1) return("Tag Vector Input");
        else return(null);
    }
    
    // **********************************************************\
    // *                      Data-flow                         *
    // **********************************************************/
    /**
     * Copy the 'tagging' fields from the 2'th input vector into the 1st one.
     * @param in Object [] containing the 2 input vectors
     * @return Object [] containing the tagged 1st input vector.
     * @throws DataFlowException If the tagging failed.
     */
    public Object []transform(Object []in) throws DataFlowException
    {
        Object obin1, obin2;
        Object obout;
        Object []fout;
        
        // The inputs : tag source and destination vectors
        obin1 = in[0];
        obin2 = in[1];
        
        try
        {
            // Transform the input to the output.
            if ((obin1 != null) && (obin2 != null)) obout = tag(obin1, obin2);
            else                                    obout = null;
            
            // Add the transformation output to the output queue.
            if (obout != null) fout = new Object[]{obout};
            else               fout = null;
        }
        catch(DataModelException ex) { throw new DataFlowException(ex); }
        catch(LearnerException ex)   { throw new DataFlowException(ex); }
        
        return(fout);
    }
    
    public int getNumberOfInputs()  { return(2); }
    public int getNumberOfOutputs() { return(1); }
    
    /**
     * Set the tags translation table.
     * The keys are the names of the tag-Attributes in the source's (2th data vector) datamodel
     * the value are the corresponding names in the destination (1th data vector)'s datamodel.
     * @param _tags The tag-translation table.
     */
    public void setTags(Map _tags)
    {
        tags = _tags;
    }
    
    // **********************************************************\
    // *                       Construction                     *
    // **********************************************************/
    public void init() throws ConfigException
    {
        int        i;
        DataModel  dmsup;
        DataModel  dmin;
        DataModel  dmout;
        Iterator   itatt;
        String     tagname, datname;
        
        // Make sure the input is compatible with this transformation's data requirements
        dmsup = getSupplierDataModel(0);
        checkDataModelFit(0, dmsup);
        dmin  = dmsup;
        dmDat = dmin;
        setInputDataModel(0,dmDat);
        
        dmsup = getSupplierDataModel(1);
        checkDataModelFit(1, dmsup);
        dmin  = dmsup;
        dmTag = dmin;
        setInputDataModel(1, dmTag);
        
        // Make the corresponding output data model
        dmout = makeOutputDataModel(dmDat);
        setOutputDataModel(0,dmout);
        
        // Make the tag indices table
        itatt  = tags.keySet().iterator();
        tagind = new int[tags.size()][];
        tagatt = new Attribute[tags.size()][];
        for (i=0; i<tags.size(); i++)
        {
            tagname      = (String)itatt.next();
            datname      = (String)tags.get(tagname);
            tagind[i]    = new int[2];
            tagatt[i]    = new Attribute[2];
            tagind[i][0] = dmDat.getAttributeIndex(datname);
            tagind[i][1] = dmTag.getAttributeIndex(tagname);
            tagatt[i][0] = dmDat.getAttribute(datname);
            tagatt[i][1] = dmTag.getAttribute(tagname);
        }
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    private boolean isDataPrimitive() { return(dmDat.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector)); }
    private boolean isTagPrimitive()  { return(dmTag.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector)); }    
    
    private DataModel makeOutputDataModel(DataModel dmin) throws DataModelException
    {
        DataModel dmout;
        
        try
        {
            // Just a copy if input model at port 0.
            dmout = (DataModel)dmin.clone();
        }
        catch(CloneNotSupportedException ex) { throw new DataModelException(ex); }
        
        return(dmout);
    }
    
    public void checkDataModelFit(int port, DataModel dm) throws DataModelException
    {
        boolean   found;
        int       attind;
        String    attname;
        DataModel dmin;
        Iterator  itatt;
        
        dmin = dm;
        
        // Check if the required fields are present
        found = true;
        if      (port == 0)
        {
            itatt = tags.values().iterator();
            while (itatt.hasNext() && found)
            {
                attname = (String)itatt.next();
                attind  = dmin.getAttributeIndex(attname);
                if (attind == -1)
                {
                    found = false;
                    throw new DataModelException("Cannot find tag-destination attribute with name "+attname+" in datamodel "+dmin.getName());
                }
            }
        }
        else if (port == 1)
        {
            itatt = tags.keySet().iterator();
            while (itatt.hasNext() && found)
            {
                attname = (String)itatt.next();
                attind  = dmin.getAttributeIndex(attname);
                if (attind == -1)
                {
                    found = false;
                    throw new DataModelException("Cannot find tag-source attribute with name "+attname+" in datamodel "+dmin.getName());
                }
            }
        }
    }
    
    // **********************************************************\
    // *                      Constructor                       *
    // **********************************************************/
    public Tagging()
    {
        super();
        tags        = new HashMap();
        name        = "tagging";
        description = "Tagging of a data vector with results from a machine-learning flow.";
    }
}
