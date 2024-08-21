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
package org.shaman.dataflow;

import java.util.HashMap;

import org.shaman.datamodel.Attribute;
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
 * <h2>Data Flow Merge</h2>
 * Merges two or more data flows into one data flow
 * of vectors that are the concatenation of the
 * incomming vectors.
 */

// *********************************************************\
// *                  Data Flow Merging                    *
// *********************************************************/
public class Merge extends Transformation
{
    private DataModel []dataModel;   // DataModels of the Input Ports
    private boolean     primitive;   // Double or ObjectVectors?
    private int         numIn;       // Number of Input Ports.
    
    // **********************************************************\
    // *            Transformation/Flow Interface               *
    // **********************************************************/
    /**
     * If all ports have data, concatenate the incomming vectors into one big one and output it.
     * @param in The input vectors
     * @return The concatenation of the input vectors
     * @throws ShamanException If there's a problem using the Flow base-class.
     */
    public Object []transform(Object []in) throws DataFlowException
    {
        int            i;
        DoubleMatrix1D dout;
        ObjectMatrix1D oout;
        Object         out;
        
        out = null;
        
        // Concatenate the incomming data vectors with COLT
        if (this.primitive)
        {
            DoubleMatrix1D []din = new DoubleMatrix1D[in.length];
            for (i=0; i<in.length; i++) din[i] = (DoubleMatrix1D)in[i];
            dout = DoubleFactory1D.dense.make(din);
            out  = dout;
        }
        else
        {
            ObjectMatrix1D []oin = new ObjectMatrix1D[in.length];
            for (i=0; i<in.length; i++) oin[i] = (ObjectMatrix1D)in[i];
            oout = ObjectFactory1D.dense.make(oin);
            out  = oout;
        }
        
        if (out == null) return(null);
        else             return(new Object[]{out});
    }
    
    /**
     * Only one input port. So, output is input.
     * @return An Object array with 1 element, the input.
     * @throws ShamanException. Never.
     */
    public Object []transform(Object in) throws DataFlowException
    {
        return(new Object[]{in});
    }
    
    /**
     * Create a Merger with the given amount of input ports.
     * Should be called right after the constructor.
     * @param _numIn The number of input ports to install on this Router.
     */
    public void grow(int _numIn)
    {
        numIn       = _numIn;
        super.grow(_numIn, 1);
    }
    
    public void init() throws ConfigException
    {
        int        i;
        DataModel  dmin;
        DataModel  dmout;
        
        // Check if the DataModels are compatible with eachother. If they have the same DataFormat.
        dataModel    = new DataModel[numIn];
        dmin         = getSupplierDataModel(0);
        dataModel[0] = dmin;
        for (i=1; i<numIn; i++)
        {
            dmin         = getSupplierDataModel(i);
            dataModel[i] = dmin;
            checkDataModelFit(i, dataModel[0]);
        }
        
        // Merge the DataModels. Set as outputmodel.
        dmout = makeOutputDataModel(dataModel);
        setOutputDataModel(0, dmout);
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    
    private DataModel makeOutputDataModel(DataModel []dmin) throws ConfigException
    {
        int         i,j, pos;
        DataModel   dmout;
        DataModel []dminclone;
        int         numatt;
        HashMap     attmap;
        Attribute []atts;
        boolean     prim;
        String      mergename;
        
        dmout = null;
        try
        {
            // First Clone the input DataModels
            dminclone = new DataModel[dmin.length];
            for (i=0; i<dmin.length; i++)
            {
                dminclone[i] = (DataModel)dmin[i].clone();
            }
            
            // Check for duplicate attributes
            mergename = "";
            attmap    = new HashMap();
            for (i=0; i<dminclone.length; i++)
            {
                if (i < dminclone.length-1) mergename += dminclone[i].getName()+" + ";
                else                        mergename += dminclone[i].getName();
                atts       = dminclone[i].getAttributes();
                for (j=0; j<atts.length; j++)
                {
                    if (attmap.put(atts[j].getName(), atts[j]) != null)
                        throw new DataModelException("Duplicate attribute '"+atts[j].getName()+"' found in DataModel '"+dminclone[i].getName()+"'");
                }
            }
            
            // Join the DataModels into one big DataModel
            prim           = dmin[0].getProperty(DataModel.PROPERTY_VECTORTYPE).equals(DataModelPropertyVectorType.doubleVector);
            this.primitive = prim;
            numatt         = attmap.keySet().size();
            if (prim) dmout = new DataModelDouble(mergename, numatt);
            else      dmout = new DataModelObject(mergename, numatt);
            pos = 0;
            for (i=0; i<dminclone.length; i++)
            {
                atts       = dminclone[i].getAttributes();
                for (j=0; j<atts.length; j++) dmout.setAttribute(pos++, atts[j]);
            }
        }
        catch(CloneNotSupportedException ex) { throw new DataModelException(ex); }
        
        return(dmout);
    }
    
    public void checkDataModelFit(int port, DataModel dataModel) throws ConfigException
    {
        DataModel  dmin;
        
        // Check it the DataModel at port 'port' has the same data format
        dmin = getSupplierDataModel(port);
        
        if (!dmin.getProperty(DataModel.PROPERTY_VECTORTYPE).equals(dataModel.getProperty(DataModel.PROPERTY_VECTORTYPE)))
            throw new DataModelException("Cannot Merge inputs with different data formats : '"+dmin.getName()+"' format is not equal to '"+dataModel.getName()+"' format");
    }
    
    public String getOutputName(int port)
    {
        if (port == 0) return("Merger Output");
        else return(null);
    }
    
    public String getInputName(int port)
    {
        if (port < numIn) return("Merger Input "+port);
        else return(null);
    }
    
    public int getNumberOfInputs()  { return(numIn); }
    public int getNumberOfOutputs() { return(1); }
    
    public Merge()
    {
        super();
        name        = "Merge";
        description = "Concatenates two or more data flows into one.";
    }
}