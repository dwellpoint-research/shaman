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


import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;


/**
 * <h2>Port Router</h2>
 * Routes data from input port 0 to output port (n-1)
 * when data becomes available at input port n.
 */

// *********************************************************\
// *           Input Port Based Flow Routing               *
// *********************************************************/
public class PortRouter extends Transformation
{
    // The current data-object that arrived at port 0.
    private Object obin;
    
    // Number of Output ports. Number of inputs ports - 1.
    private int    numOut;

    // **********************************************************\
    // *                   DataFlow Interface                   *
    // **********************************************************/
    public void transform() throws DataFlowException
    {
        int i;
        
        // If data input is available at port 0, get it and remember it.
        if (areInputsAvailable(0, 1))
        {
            this.obin = getInput(0);
        }
        
        // If a data object is available for output
        if (this.obin != null)
        {
            boolean out;
            
            // If input is avaible on the select ports, then output the current data object at the corresponding output port.
            out = false;
            for (i=0; i<this.numOut; i++)
            {
                while(areInputsAvailable(i+1, 1))
                {
                    getInput(i+1);
                    setOutput(i, this.obin);
                    out = true;
                }
            }
            
            // If the data has been output, then forget about it.
            if (out) this.obin = null;
        }
    }
    
    public void grow(int _numOut) throws ConfigException
    {
        this.numOut      = _numOut;
        this.inputModel  = new DataModel[_numOut+1];
        this.outputModel = new DataModel[_numOut];
        super.grow(_numOut+1, _numOut);
    }

    public void init() throws ConfigException
    {
        int        i;
        DataModel  dmin;
        
        // DataModel of the data-object to be passed through on one of the output ports.
        dmin = getSupplierDataModel(0);
        setInputDataModel(0, dmin);
        for (i=0; i<this.numOut; i++) setOutputDataModel(i, dmin);
        
        // The DataModels of the other input ports.
        for (i=1; i<this.numOut+1; i++)
        {
            dmin = getSupplierDataModel(i);
            setInputDataModel(i, dmin);
        }
    }
    
    public void cleanUp() throws DataFlowException
    {
      
    }

     public String getOutputName(int port)
     {
         if (port == 0) return("Data Output");
         else return(null);
     }

     public String getInputName(int port)
     {
              if (port == 0)            return("Data Input");
         else if (port < this.numOut+1) return("Data Port Select "+(port-1));
         else return(null);
     }

     public int getNumberOfInputs() { return(this.numOut+1); }
     public int getNumberOfOutputs()
     {
        return(this.numOut);
     }

  public void checkDataModelFit(int port, DataModel dataModel) throws ConfigException
  {
      // No restrictions in datamodels.
  }

  public PortRouter()
  {
      super();
      name        = "PortRouter";
      description = "Flow Network Routing Logic based in port of incomming data.";
  }
}