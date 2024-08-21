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


import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.AttributeObject;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelObject;
import org.shaman.exceptions.ShamanException;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.TransformationException;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Vector Source</h2>
 * A source of data-vectors.
 * Ouputs vectors of the specified datamodel.
 * Can be made to enforce or ignore the datamodel before outputting the vectors. 
 */
public class VectorSource extends Transformation
{ 
    // DataModel enforcement strategy
    /** Ignore the DataModel */
    public static final int FIT_NONE   = 0;
    /** Enforce the DataModel constraints */
    public static final int FIT_NORMAL = 1;
    /** Enforce the DataModel constraints. If something does not fit, try to fix it with heuristics. Very dangerous. */
    public static final int FIT_FORCE  = 2;
    
    // The DataModel of the Vector. One of two is defined.
    protected DataModelObject dmob;
    protected DataModelDouble dmdo;
    protected DataModel       dm;
    
    // Data Format enforcement
    protected int       datamodelfit;
    
    // **********************************************************\
    // *                 Enforce data format                    *
    // **********************************************************/
    protected ObjectMatrix1D checkFit(ObjectMatrix1D out) throws TransformationException
    {
        ObjectMatrix1D outfit;
        
        outfit = null;
        try
        {
            if      (this.datamodelfit == FIT_NORMAL) { this.dmob.checkFit(out); outfit = out; }
            else if (this.datamodelfit == FIT_FORCE)  { forceFit(dmob, out); outfit = out; }
            else outfit = out;
        }
        catch(DataModelException ex) { throw new TransformationException(ex); }
        
        return(outfit);
    }
    
    protected DoubleMatrix1D checkFit(DoubleMatrix1D out) throws TransformationException
    {
        DoubleMatrix1D outfit;
        
        outfit = null;
        try
        {
            if      (this.datamodelfit == FIT_NORMAL) { this.dmdo.checkFit(out); outfit = out; }
            else if (this.datamodelfit == FIT_FORCE)  { this.dmdo.checkFit(out); outfit = out; } // Same for double vectors.
            else outfit = out;
        }
        catch(DataModelException ex) { throw new TransformationException(ex); }
        
        return(outfit);
    }
    
  /**
    * Try to modify the given vector so it fits this datamodel.
    */ 
   public void forceFit(DataModelObject dm, ObjectMatrix1D vec) throws DataModelException
   {
       int             i;
       String          typenow;
       Object          obnow;
       AttributeObject attnow;
       Attribute     []attribute;
      
       attribute = dm.getAttributes();
       for (i=0; i<attribute.length; i++)
       {
          attnow  = (AttributeObject)attribute[i];
            
          typenow = attnow.getRawType();
          obnow   = vec.getQuick(i);
            
          // If it's a missing value. Use the unique missing as object...
          if (attnow.isMissingValue(obnow)) vec.setQuick(i, attnow.getMissingAsObject());
          else
          {
              // These are special cases... I'd like to think.
              // Look at rule.ExpressionParser for more motivation...
              if (typenow.equals("java.lang.Double"))
              {
                  if (obnow instanceof java.math.BigDecimal)
                  {
                      vec.setQuick(i, new Double(((java.math.BigDecimal)obnow).doubleValue()));
                  }
                  else if (obnow instanceof java.lang.Integer)
                  {
                      vec.setQuick(i, new Double(((java.lang.Integer)obnow).doubleValue()));
                  }
              }
              else if (typenow.equals("java.util.Date"))
              {
                  if (obnow instanceof java.sql.Timestamp)
                  {
                      vec.setQuick(i, new java.util.Date(((java.sql.Timestamp)obnow).getTime()));
                  }
              }
          }
       }
    }
    
	
    // **********************************************************\
    // *                 Output a Data Vector                   *
    // **********************************************************/
    /**
     * Ouputs the given object-based vector on the only output port.
     * Depending on the datamodel-fit parameter, the datamodel format is enforced or not.
     * @param out The object-vector to output
     * @throws ShamanException If the vector does not comply with it's datamodel.
     */
    public void outputVector(ObjectMatrix1D out) throws DataFlowException
    {
        ObjectMatrix1D outfit;
        
        outfit = checkFit(out);
        this.setOutput(0, outfit);
    }
    
    /**
     * Ouputs the given double-based vector on the only output port.
     * Depending on the datamodel-fit parameter, the datamodel format is enforced or not.
     * @param out The double vector to output
     * @throws ShamanException If the vector does not comply with it's datamodel.
     */
    public void outputVector(DoubleMatrix1D out) throws DataFlowException
    {
        DoubleMatrix1D outfit;
        
        outfit = checkFit(out);
        this.setOutput(0, outfit);
    }
    
    /**
     * Outputs an object-based or double vector on the only output port.
     * @param An object-based or double vector.
     * @throws ShamanException If the input object is not a vector. Or the given vector does not comply with it's datamodel
     */ 
    public void outputVector(Object out) throws DataFlowException
    {
        if ((out instanceof DoubleMatrix1D) || (out instanceof ObjectMatrix1D))
          throw new DataFlowException("Output object is not a vector. Use ObjectMatrix1D or DoubleMatrix1D.");
        else
        {
            if      (out instanceof DoubleMatrix1D) outputVector((DoubleMatrix1D)out);
            else if (out instanceof ObjectMatrix1D) outputVector((ObjectMatrix1D)out);
        }
    }
    
    /**
     * Forces the given object to be output. Use with care.
     * Only works when DataModel fit is disabled.
     * Handy for use in unit-tests. 
     * @param out The Object to be output
     * @throws ShamanException 
     */
    public void forceOutput(Object out) throws DataFlowException
    {
        if (this.datamodelfit == FIT_NONE)
        {
             this.setOutput(0, out); 
        }
        else throw new  DataFlowException("Cannot just output on Object if DataModel constraints have to be enforced.");
    }
    
    // **********************************************************\
    // *           Define the DataModel of the Vector           *
    // **********************************************************/
    /**
     * Specify the datamodel of the vectors that are output.
     * @param datamodel The datamodel if the vectors that are output.
     */ 
    public void setDataModel(DataModel datamodel)
    {
        if      (datamodel instanceof DataModelDouble) this.dmdo = (DataModelDouble)datamodel;
        else if (datamodel instanceof DataModelObject) this.dmob = (DataModelObject)datamodel;
        this.dm = datamodel;
    }
    
    /**
     * Get the datamodel of the vectors.
     * @return The vector's datamodel.
     */
    public DataModel getDataModel()
    {
        return(this.dm);
    }
    
    /**
     * Specify if the datamodel should be enforced before outputting a vector.
     * @param datamodelfit The datamodel enforcement strategy to follow
     * @see #FIT_NONE
     * @see #FIT_NORMAL
     * @see #FIT_FORCE
     */
    public void setFit(int datamodelfit)
    {
        this.datamodelfit = datamodelfit;
    }
    
    /**
     * Get the type of datamodel enforcement strategy to enforce.
     * @return The datamodel enforcement strategy
     */
    public int getFit()
    {
        return(this.datamodelfit);
    }
    
    // **********************************************************\
    // *                Transformation Implementation           *
    // **********************************************************/
    public void init() throws ConfigException
    {
        // Set the DataSource's datamodel at output.
        setOutputDataModel(0, this.dm);
    }
    
    public void cleanUp() throws DataFlowException
   {
      
   }
    
    public void checkDataModelFit(int port, DataModel dmin) throws ConfigException
    {
      ; // All fine. No inputs anyway
    }
    
    public int getNumberOfInputs()  { return(0); }
    public int getNumberOfOutputs() { return(1); }
    public String getInputName(int port) {  return(null); }
    public String getOutputName(int port)
    {
        if   (port == 0) return("Vector Source");
        else return(null);
    }
    // **********************************************************\
    // *              Constructor/State Persistence             *
    // **********************************************************/
    public VectorSource()
    {
       super();
       name        = "Vector Source";
       description = "Transformation that outputs well-formatted data vectors";
    }
}
