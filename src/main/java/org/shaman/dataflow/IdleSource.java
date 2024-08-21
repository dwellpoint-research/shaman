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


/**
 * <h2>Idle Flow Source</h2>
 * Transformation that never outputs anything.
 * Does have an output DataModel.
 */

// *********************************************************\
// *            Idle Flow Source Transformation            *
// *********************************************************/
public class IdleSource extends Transformation
{
  private DataModel outmodel;      // The DataModel of the Output Port.

  // **********************************************************\
  // *            Specify the Output DataModel                *
  // **********************************************************/
  /**
   * Specify the datamodel if the idle output port.
   * @param outmodel The datamodel of the data that is never output.
   */ 
  public void setOutputModel(DataModel outmodel)
  {
      this.outmodel = outmodel;
  }
  
  /**
   * Get the datamodel of the idel output port.
   * @return The datamodel of the data that is never output.
   */ 
  public DataModel getOutputModel()
  {
      return(outmodel);
  }

  // **********************************************************\
  // *            Transformation/Flow Interface               *
  // **********************************************************/
  /**
   * Don't do anyhting. Even when asked. Nothing at all. Repeat.
   * @throws ShamanException never.
   */
  public void transform() throws DataFlowException
  {
  }
  
  public void init() throws ConfigException
  {
     // Just install the given output datamodel at output port 0.
     setOutputDataModel(0, outmodel);
  }
  
  public void cleanUp() throws DataFlowException
  {
      
  }

  public String getOutputName(int port)
  {
    if (port == 0) return("Idle Output");
    else           return(null);
  }

  public String getInputName(int port)
  {
     return(null);
  }

  public int getNumberOfInputs()  { return(0); }
  public int getNumberOfOutputs() { return(1); }

  public void checkDataModelFit(int port, DataModel dataModel) throws ConfigException
  {
      // No inputs. So all is fine.
  }

  public IdleSource()
  {
    super();
    name        = "IdleSource";
    description = "Idle Flow Source Transformation. Never does a thing.";
  }
}