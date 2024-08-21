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
package org.shaman.rule;


import org.shaman.dataflow.Transformation;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelObject;
import org.shaman.datamodel.DataModelPropertyVectorType;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;

import cern.colt.matrix.ObjectMatrix1D;



/**
 * <h2>Boolean Condition Based Switch/Case</h2>
 * Routed the incomming data to the output
 * ports whose BooleanCondition evaluates to
 * true.
 */

// **********************************************************\
// *            Expression Based Rule Evaluation            *
// **********************************************************/
public class Switch extends Transformation
{
    // Input DataModel
    private DataModel        dataModel;        // The Incomming Data-Vectors DataModel.
    
    // The Boolean Conditions
    private BooleanCondition []cond;           // The conditions that control to active output ports.
    private int              numOut;           // The number of output ports. One for each Condition.
    
    // **********************************************************\
    // *                 Rule Configuration                     *
    // **********************************************************/
    public void setConditions(BooleanCondition []cond)
    {
        this.cond = cond;
    }
    
    public BooleanCondition []getConditions()
    {
        return(this.cond);
    }
    
    // **********************************************************\
    // *          Route according to the Conditions             *
    // **********************************************************/
    public void transform() throws DataFlowException
    {
        int            i;
        ObjectMatrix1D invec;
        boolean      []fire;
        
        // If there's data present...
        if (areInputsAvailable(0, 1))
        {
            // Get data from input port
            invec = (ObjectMatrix1D)getInput(0);
            
            try
            {
                // Evaluate Conditions first.
                fire = new boolean[cond.length];
                for (i=0; i<cond.length; i++)
                {
                    if (cond[i].apply(invec)) fire[i] = true;
                    else                      fire[i] = false;
                }
                
                // Send to the firing output ports.
                for (i=0; i<cond.length; i++)
                {
                    if (fire[i]) setOutput(i, invec);
                }
            }
            catch(LearnerException ex) { throw new DataFlowException(ex); }
        }
    }
    
    /**
     * Create a Switch with the given amount of output ports.
     * Should be called right after the constructor.
     * @param _numOut The number of output ports to install on this Switch.
     */
    public void grow(int _numOut)
    {
        this.numOut = _numOut;
        super.grow(1, _numOut);
    }
    
    /**
     *  One input.
     *  @return '1'
     */
    public int getNumberOfInputs()  { return(1); }
    
    /**
     *  Return the number of output ports.
     *  @return The number of output ports
     */
    public int getNumberOfOutputs() { return(this.numOut); }
    
    public String getOutputName(int port)
    {
        if   (port < this.numOut) return("Output for Condition "+port);
        else return(null);
    }
    
    public String getInputName(int port)
    {
        if (port == 0) return("Input");
        else return(null);
    }
    
    // **********************************************************\
    // *                       Construction                     *
    // **********************************************************/
    public void init() throws ConfigException
    {
        int               i;
        DataModel        dmsup;
        DataModelObject  dmin;
        
        // Make sure the input is compatible with this transformation's data requirements
        dmsup = getSupplierDataModel(0);
        checkDataModelFit(0, dmsup);
        dmin  = (DataModelObject)dmsup;
        this.dataModel = dmin;
        
        // Check if the number of output ports and the number of conditions agree
        if (this.cond.length != this.numOut)
            throw new ConfigException("Number of output ports does not agree with the number of boolean conditions. "+this.numOut+" != "+this.cond.length);
        
        // Set the input/output datamodels
        setInputDataModel(0,  dmin);
        for (i=0; i<this.numOut; i++) setOutputDataModel(i, dmin);
        
        // Set the datamodel in the Condition
        for (i=0; i<this.numOut; i++) this.cond[i].setDataModel(dmin);
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    public void checkDataModelFit(int port, DataModel dm) throws DataModelException
    {
        // Allow only Object based data
        if (dm.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector));
            throw new DataModelException("Can only use Object Vectors");
    }
    
    // **********************************************************\
    // *                       Constructor                      *
    // **********************************************************/
    public Switch()
    {
        super();
        name        = "Switch";
        description = "Route to the output ports whose BooleanCondition is fired.";
    }
}