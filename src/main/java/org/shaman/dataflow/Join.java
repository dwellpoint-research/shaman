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

import java.util.HashSet;
import java.util.Set;

import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelProperty;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;


/**
 * <h2>Data Flow Join</h2>
 * Joins a number of data-flows with the same datamodel into
 * one stream.
 */

// *********************************************************\
// *     Datamodel compatible data-streams joiner          *
// *********************************************************/
public class Join extends Transformation
{
    private int       numIn;      // Number of Input Ports.
    
    // **********************************************************\
    // *            Transformation/Flow Interface               *
    // **********************************************************/
    /**
     * Route all incomming data to the output port.
     * @throws DataFlowException If there's a problem using the Flow base-class.
     */
    public void transform() throws DataFlowException
    {
        int            i;
        Object         obin;
        
        // Check all input ports for new data. If found get the data and send to the output port.
        for (i=0; i<getNumberOfInputs(); i++)
        {
            while (areInputsAvailable(i, 1))
            {
                obin = getInput(i);
                setOutput(0, obin);
            }
        }
    }
    
    /**
     * Create a Join with the given amount of input ports.
     * Should be called right after the constructor.
     * @param _numIn The number of input ports to install on this Join.
     */
    public void grow(int _numIn)
    {
        numIn       = _numIn;
        inputModel  = new DataModel[numIn];
        outputModel = new DataModel[1];
        super.grow(_numIn, 1);
    }
    
    public void init() throws ConfigException
    {
        int        i;
        DataModel  dmin, dmout;
        DataModel  dminother;
        String     dmconflict;
        
        // Check if the DataModels are compatible with eachother
        dmin      = getSupplierDataModel(0);
        
        dmconflict = "";
        for (i=1; i<numIn; i++)
        {
            try
            {
                checkDataModelFit(i, dmin);
            }
            catch(ConfigException ex)
            {
                dminother   = getSupplierDataModel(i);
                dmconflict += "'"+dminother.getName()+"' ";
            }
        }
        
        // If not compatible then give up.
        if (dmconflict.length() > 0) throw new DataModelException("First DataModel '"+dmin.getName()+"' is not compatible with datamodel(s) "+dmconflict);
        else
        {
            // Else use the first datamodel as output datamodel.
            dmout = dmin;
            setOutputDataModel(0, dmout);
        }
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    public String getOutputName(int port)
    {
        if (port == 0) return("Join Output");
        else return(null);
    }
    
    public String getInputName(int port)
    {
        if (port < numIn) return("Join Input "+port);
        else return(null);
    }
    
    public int getNumberOfInputs()  { return(numIn); }
    public int getNumberOfOutputs() { return(1); }
    
    public void checkDataModelFit(int port, DataModel dataModel) throws ConfigException
    {
        int         i;
        DataModel   dmin;
        Set         nameset1;
        Set         nameset2;
        Attribute []atts;
        DataModelProperty prop;
        
        // Get the DataModel of the supplier of input port 'port'
        dmin = getSupplierDataModel(port);
        
        // Check if the 2 datamodel work on the same kind of data vector.
        prop = dmin.getProperty(DataModel.PROPERTY_VECTORTYPE);
        if (!prop.equals(dataModel.getProperty(DataModel.PROPERTY_VECTORTYPE)))
            throw new DataModelException("Can't join two datamodels with different data formats.");
        
        // Check if the attribute name agree
        atts     = dataModel.getAttributes();
        nameset1 = new HashSet();
        for (i=0; i<atts.length; i++) nameset1.add(atts[i].getName());
        atts     = dmin.getAttributes();
        nameset2 = new HashSet();
        for (i=0; i<atts.length; i++) nameset2.add(atts[i].getName());
        
        nameset1.retainAll(nameset2);
        if (nameset1.size() != nameset2.size())
            throw new DataModelException("Can't join data with different datamodels. Following attributes do not agree "+nameset1);
    }
    
    public Join()
    {
        super();
        name        = "Join";
        description = "Joins two or more data flows with the same datamodel into one data flow.";
    }
}