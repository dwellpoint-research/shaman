/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                     Technologies                      *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  March 2002 & January 2005                            *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2002-5 Shaman Research                 *
\*********************************************************/
package org.shaman.dataflow;

import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.AttributeObject;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelObject;
import org.shaman.datamodel.DataModelPropertyVectorType;
import org.shaman.exceptions.ShamanException;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectFactory1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Converting Between DataModels</h2>
 * Converts the incomming data between 2 DataModels.
 * Can re-order, convert from double to Object or back again.
 */

// **********************************************************\
// *          DataModel Converter Transformation            *
// **********************************************************/
public class DataModelConverter extends Transformation
{
    private DataModel dataModel;          // The Input DataModel
    private DataModel outModel;           // The Desired Output DataModel
    private     int []inputInd;           // Internal Data created by init()
    
    // **********************************************************\
    // *            Specify the Output DataModel                *
    // **********************************************************/
    /**
     * Set the output datamodel.
     * @param _outModel The output datamodel
     */
    public void setOutputModel(DataModel _outModel)
    {
        this.outModel = _outModel;
    }
    
    // **********************************************************\
    // *           Convert between the DataModels               *
    // **********************************************************/
    private DoubleMatrix1D convertPrimitivePrimitive(DoubleMatrix1D vin) throws ShamanException
    {
        int            i;
        DoubleMatrix1D vout;
        
        vout = DoubleFactory1D.dense.make(inputInd.length);
        for (i=0; i<inputInd.length; i++)
        {
            if (inputInd[i] != -1) vout.setQuick(i, vin.getQuick(inputInd[i]));
        }
        
        return(vout);
    }
    
    private ObjectMatrix1D convertObjectObject(ObjectMatrix1D vin) throws DataModelException
    {
        int            i;
        ObjectMatrix1D vout;
        
        vout = ObjectFactory1D.dense.make(this.inputInd.length);
        for (i=0; i<this.inputInd.length; i++)
        {
            // Copy the object from the input vector if possible.
            if (this.inputInd[i] != -1) vout.setQuick(i, vin.getQuick(this.inputInd[i]));
            else
            {
                // Else create a default object for this position
                vout.setQuick(i, ((DataModelObject)this.outModel).getAttributeObject(i).getDefaultObject());
            }
        }
        
        return(vout);
    }
    
    private ObjectMatrix1D convertPrimitiveObject(DoubleMatrix1D vin) throws ShamanException
    {
        int             i;
        ObjectMatrix1D  vout;
        AttributeDouble atd;
        AttributeObject ato;
        double          dnow;
        Object          onow;
        
        vout = ((DataModelObject)this.outModel).createDefaultVector();
        for (i=0; i<inputInd.length; i++)
        {
            if (inputInd[i] != -1) 
            {
                dnow = vin.getQuick(inputInd[i]);
                atd  = ((DataModelDouble)this.dataModel).getAttributeDouble(inputInd[i]);
                ato  = ((DataModelObject)this.outModel).getAttributeObject(i);
                onow = atd.getObjectValue(dnow, ato);
                vout.setQuick(i, onow);
            }
        }
        
        return(vout);
    }
    
    private DoubleMatrix1D convertObjectPrimitive(ObjectMatrix1D vin) throws ShamanException
    {
        int             i;
        DoubleMatrix1D  vout;
        AttributeDouble atd;
        AttributeObject ato;
        double          dnow;
        Object          onow;
        
        vout = DoubleFactory1D.dense.make(inputInd.length);
        for (i=0; i<inputInd.length; i++)
        { 
            if (inputInd[i] != -1) 
            {
                onow = vin.getQuick(inputInd[i]);
                atd  = ((DataModelDouble)this.outModel).getAttributeDouble(inputInd[i]);
                ato  = ((DataModelObject)this.dataModel).getAttributeObject(i);
                dnow = ato.getDoubleValue(onow, atd);
                vout.setQuick(i, dnow);
            }
        }
        
        return(vout);
    }
    
    private boolean outPrimitive() { return(this.outModel.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector)); }
    private boolean inPrimitive()  { return(this.dataModel.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector)); }

    // **********************************************************\
    // *       Transform the DataType of the incomming data     *
    // **********************************************************/
    public Object []transform(Object obin) throws DataFlowException
    {
        Object         out;
        DoubleMatrix1D din;
        ObjectMatrix1D oin;
        
        if ((obin == null)) out = null;
        else
        {
            try
            {
                if (inPrimitive())
                {
                    din = (DoubleMatrix1D)obin;
                    if (outPrimitive()) out = convertPrimitivePrimitive(din);
                    else                out = convertPrimitiveObject(din);
                }
                else
                {
                    oin = (ObjectMatrix1D)obin;
                    if (outPrimitive()) out = convertObjectPrimitive(oin);
                    else                out = convertObjectObject(oin);
                }
            }
            catch(ShamanException ex) { throw new DataFlowException(ex); }
        }
        
        if (out == null) return(null);
        else             return(new Object[]{out});
    }
    
    public int getNumberOfInputs()  { return(1); }
    public int getNumberOfOutputs() { return(1); }
    
    public String getOutputName(int port)
    {
        if (port == 0) return("Output");
        else return(null);
    }
    
    public String getInputName(int port)
    {
        if (port == 0) return("Input");
        else return(null);
    }
    
    // **********************************************************\
    // *                Request/Response Usage.                 *
    // **********************************************************/
    /**
     * NOTE: Do not use. Method is slow, not thread-safe
     */ 
    public ObjectMatrix1D convertDataModel(ObjectMatrix1D vecin, DataModel dmdst, DataModel dmsrc) throws DataModelException
    {
        int            i;
        ObjectMatrix1D vecout;
        
        this.dataModel = dmsrc;
        this.outModel  = dmdst;
        
        // Make convertion tables.
        this.inputInd = new int[dmdst.getAttributeCount()];
        for (i=0; i<dmdst.getAttributeCount(); i++)
        {
            this.inputInd[i] = this.dataModel.getAttributeIndex(dmdst.getAttribute(i).getName());
        }
        
        // Convert the Vector
        vecout = (ObjectMatrix1D)convertObjectObject(vecin);
        
        return(vecout);
    }
    
    public ObjectMatrix1D convertDataModel(ObjectMatrix1D vecin, int []trans, DataModel dmdst) throws DataModelException
    {
        ObjectMatrix1D vecout;
        int            i;
        
        // Make a clean output vector
        vecout = ObjectFactory1D.dense.make(trans.length);
        for (i=0; i<trans.length; i++)
        {
            // If the current attribute is also present in the input vector, copy it's object from the input vector position.
            if (trans[i] != -1) vecout.setQuick(i, vecin.getQuick(trans[i]));
            else
            {
                // Else create a default object for this attribute
                vecout.setQuick(i, ((DataModelObject)dmdst).getAttributeObject(i).getDefaultObject());
            }
        }
        
        return(vecout);
    }
    
    // **********************************************************\
    // *                       Construction                     *
    // **********************************************************/
    public void init() throws ConfigException
    {
        int        i;
        DataModel  dmsup;
        DataModel  dmin;
        
        // Make sure the desired output datamodel is compatible with the input one.
        dmsup = getSupplierDataModel(0);
        checkDataModelFit(0, dmsup);
        dmin = dmsup;
        
        // Set the DataModels
        setInputDataModel(0,dmin);
        setOutputDataModel(0, outModel);
        this.dataModel = dmin;
        
        // Find the position of the output attributes in the input datamodel
        this.inputInd = new int[outModel.getAttributeCount()];
        for (i=0; i<outModel.getAttributeCount(); i++)
        {
            this.inputInd[i] = dataModel.getAttributeIndex(outModel.getAttribute(i).getName());
        }
    }
    
    public void cleanUp() throws DataFlowException
    {
    }    
    
    public void checkDataModelFit(int port, DataModel dm) throws DataModelException
    {
        // All fine. Since this is the whole idea of this Transformation.
    }
    
    public DataModelConverter()
    {
        super();
        name        = "datamodel converter";
        description = "Converts Between LearnerDataModels.";
    }
}
