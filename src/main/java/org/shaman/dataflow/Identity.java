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
import org.shaman.exceptions.ShamanException;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Identity Transformation</h2>
 * Copies any input data that arrives to the corresponding output port.
 */

// *********************************************************\
// *                Identity Transformation                *
// *********************************************************/
public class Identity extends Transformation
{
  private int     numPort;      // The Number of identity ports.
  private boolean dump;         // Dump the output.

  // **********************************************************\
  // *            Transformation/Flow Interface               *
  // **********************************************************/
  /**
   * Route all incomming data on port i to the output port i.
   * @throws ShamanException If there's a problem using the Flow base-class.
   */
  public void transform() throws DataFlowException  // Override the base-class transform() method.
  {
    int            i,j;
    Object         obin;

    // Check all input ports for new data.   
    for (i=0; i<getNumberOfInputs(); i++)
    {
        // Route all data on Input port i to Output port i
        while(areInputsAvailable(i,1))
        {
            obin = getInput(i);
            
            // Log input?
            if (dump)
            {
                if (obin instanceof DoubleMatrix1D) System.out.println(obin);
                else
                {
                   StringBuffer bufdump = new StringBuffer();
                   ObjectMatrix1D ovec = (ObjectMatrix1D)obin;
                   Object         onow;
                   for (j=0; j<ovec.size(); j++)
                   {
                       onow = ovec.getQuick(j);
                       if (onow != null) bufdump.append(onow.toString()+"  ");
                       else              bufdump.append("null  ");
                   }
                   System.out.println(bufdump.toString());
               }   
            }
            
            // Output input on the same output port
            setOutput(i, obin);
        }
    }
  }
  
  public void setDump(boolean _dump)
  {
      this.dump = _dump;
  }
  

  /**
   * Create an Identity transformation with the given amount of input (and output) ports.
   * Should be called right after the constructor.
   * @param _numPort The number of input and output ports to install.
   */
  public void grow(int _numPort)
  {
      this.numPort = _numPort;
      super.grow(numPort, numPort);
  }

  public void init() throws ConfigException
  {
       int       i;
       DataModel dmport;

       // Use the Supplier's DataModel as input and output datamodels.
       for (i=0; i<numPort; i++)
       {
           dmport = setSupplierAsInputDataModel(i);
           setOutputDataModel(i, dmport);
       }
  }
  
  public void cleanUp() throws DataFlowException
  {
  }

  public String getOutputName(int port)
  {
     if (port < numPort) return("Identical Output "+port);
     else return(null);
  }

  public String getInputName(int port)
  {
     if (port < numPort) return("Input "+port);
     else return(null);
  }

  public int getNumberOfInputs()  { return(numPort); }
  public int getNumberOfOutputs() { return(numPort); }

  public void checkDataModelFit(int port, DataModel dataModel) throws ConfigException
  {
      // All is fine.
  }

  public Identity()
  {
      super();
      name        = "Identity";
      description = "Identity Transformation";
  }
}