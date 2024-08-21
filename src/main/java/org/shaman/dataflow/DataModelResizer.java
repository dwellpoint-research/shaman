/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                     Technologies                      *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  March 2003                                           *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2002 Shaman Research                   *
\*********************************************************/
package org.shaman.dataflow;

import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelObject;
import org.shaman.datamodel.DataModelPropertyVectorType;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectFactory1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Extend or Shorten Data Vector</h2>
 * Adds or drops a number of attributes at
 * the end of a data-vector.
 *
 * <br>
 * @author Johan Kaers
 * @version 2.0
 */

// **********************************************************\
// *                 Data-Vector Resizer                    *
// **********************************************************/
public class DataModelResizer extends Transformation
{
    // The Input DataModel
    private DataModel  dataModel;
    
    // The Extended or Truncated Output DataModel
    private DataModel  outModel;
    
    // Extend or Shorten the Data Vector?
    private boolean extend;
    
    // Internal Data created by init()
    private int            outSize;           // Size of the output model when truncating
    private ObjectMatrix1D ovecextend;        // Prototype object vector to append when extending
    private DoubleMatrix1D dvecextend;        //           double
    private int            extendSize;        // Number of Attributes to add when extending
    private boolean        primitive;         // Double or Object based vectors?
    
    // **********************************************************\
    // *            Specify the Output DataModel                *
    // **********************************************************/
    /**
     * Set the output datamodel.
     * @param _outModel The output datamodel
     */
    public void setOutputModel(DataModelObject _outModel)
    {
        this.outModel = _outModel;
    }
    
    public void setExtend(boolean _extend)
    {
        this.extend = _extend;
    }
    
    // **********************************************************\
    // *         Extend or Shorten the Data Vector              *
    // **********************************************************/
    private ObjectMatrix1D sizeObject(ObjectMatrix1D vin)
    {
        ObjectMatrix1D vout;
        
        if (extend)
        {
            ObjectMatrix1D ext;
            
            if (this.extendSize > 0)
            {
                ext  = this.ovecextend.copy();
                vout = ObjectFactory1D.dense.append(vin, ext);
            }
            else vout = vin;
        }
        else
        {
            vout = vin.viewPart(0, this.outSize);
        }
        
        return(vout);
    }
    
    private DoubleMatrix1D sizePrimitive(DoubleMatrix1D vin)
    {
        DoubleMatrix1D vout;
        
        if (extend)
        {
            DoubleMatrix1D ext;
            if (this.extendSize > 0)
            {
                ext = this.dvecextend.copy();
                vout = DoubleFactory1D.dense.append(vin, ext);
            }
            else vout = vin;
        }
        else
        {
            vout = vin.viewPart(0, this.outSize);
        }
        
        return(vout);
    }
    
    // **********************************************************\
    // *           Extend or Truncate the Data Vector           *
    // **********************************************************/
    public Object []transform(Object obin) throws DataFlowException
    {
        Object         out;
        
        if (obin == null) out = null;
        else
        {
            if (this.primitive) out = sizePrimitive((DoubleMatrix1D)obin);
            else                out = sizeObject((ObjectMatrix1D)obin);
        }
        
        if (out == null) return(null);
        else             return(new Object[]{out});
    }
    
    public String getOutputName(int port)
    {
        if (port == 0) return("Resized Vector");
        else return(null);
    }
    
    public String getInputName(int port)
    {
        if (port == 0) return("Input Vector");
        else return(null);
    }
    
    public int getNumberOfInputs()  { return(1); }
    public int getNumberOfOutputs() { return(1); }
    
    // **********************************************************\
    // *                       Construction                     *
    // **********************************************************/
    public void init() throws ConfigException
    {
        DataModel  dmsup;
        DataModel  dmin;
        
        // Make sure the desired output datamodel is compatible with the input one.
        dmsup = getSupplierDataModel(0);
        checkDataModelFit(0, dmsup);
        dmin  = dmsup;
        
        // Set the DataModels
        setInputDataModel(0,dmin);
        setOutputDataModel(0, this.outModel);
        this.dataModel = dmin;
        if (dmin.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector)) this.primitive = true;
        else                                                                               this.primitive = false;
        
        // Prepare Transformation Data
        if (this.extend)
        {
            int i, extsize;
            
            // Create the vector extension templates
            extsize         = this.outModel.getAttributeCount() - dmin.getAttributeCount();
            this.extendSize = extsize;
            if (!this.primitive)
            {
                this.ovecextend = ObjectFactory1D.dense.make(extsize);
                for (i=0; i<extsize; i++)
                   this.ovecextend.setQuick(i, ((DataModelObject)this.outModel).getAttributeObject(i+dmin.getAttributeCount()).getDefaultObject());
            }
            else
            {
                this.dvecextend = DoubleFactory1D.dense.make(extsize);
                for (i=0; i<extsize; i++)
                   this.dvecextend.setQuick(i, ((DataModelDouble)this.outModel).getAttributeDouble(i+dmin.getAttributeCount()).getDefaultValue());
            }
        }
        else
        {
            // Just need the output size
            this.outSize = this.outModel.getAttributeCount();
        }
    }
    
    public void cleanUp() throws DataFlowException
    {    
    }
    
    public void checkDataModelFit(int port, DataModel dm) throws DataModelException
    {
        int       i;
        DataModel dmin;
        boolean   fine;
        
        dmin = dm;
        fine = true;
        if (this.extend)
        {
            // Check if the input model agrees with the output model at every position
            if (this.outModel.getAttributeCount() >= dmin.getAttributeCount())
            {
                for (i=0; (i<dmin.getAttributeCount()) && fine; i++)
                {
                    if (!dmin.getAttribute(i).getName().equals(this.outModel.getAttribute(i).getName())) fine = false;
                }
                if (!fine)
                    throw new DataModelException("Input and Extended Output DataModel do not contain the same attribute at input position "+(i-1)+" with name "+dmin.getAttribute(i-1).getName());
            }
            else throw new DataModelException("Cannot extend to a datamodel that is smaller than the input model. "+this.outModel.getAttributeCount()+" < "+dmin.getAttributeCount());
        }
        else
        {
            // Check if the output model agrees with the input model at every position
            if (dmin.getAttributeCount() >= this.outModel.getAttributeCount())
            {
                for (i=0; (i< this.outModel.getAttributeCount()) && fine; i++)
                {
                    if (!this.outModel.getAttribute(i).getName().equals(dmin.getAttribute(i).getName())) fine = false;
                }
                if (!fine)
                    throw new DataModelException("Input and Extended Output DataModel do not contain the same attribute at input position "+(i-1)+" with name "+this.outModel.getAttribute(i-1).getName());
            }
            else throw new DataModelException("Cannot scale to a datamodel that is larger than the input model. "+dmin.getAttributeCount()+" < "+this.outModel.getAttributeCount());
        }
    }
    
    public DataModelResizer()
    {
        super();
        name        = "datamodel resizer";
        description = "Adds or drops attributes at the end of the input datamodel";
    }
}