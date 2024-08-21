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
 * <h2>Boolean Condition Based If-Then-Else</h2>
 * Uses very simple boolean conditions to route
 * the data to the THEN (0) or ELSE (1) output ports.
 */

// **********************************************************\
// *            Expression Based Rule Evaluation            *
// **********************************************************/
public class IfThenElse extends Transformation
{
    // Input DataModel
    private DataModelObject  dataModel;        // The Incomming Data-Vectors DataModel.
    
    // The Boolean Condition
    private BooleanCondition cond;             // Evaluate's condition on data in the model.
    
    // **********************************************************\
    // *                 Rule Configuration                     *
    // **********************************************************/
    public void setCondition(BooleanCondition cond)
    {
        this.cond = cond;
    }
    
    public BooleanCondition getCondition()
    {
        return(this.cond);
    }
    
    // **********************************************************\
    // *           Route according to the Expression             *
    // **********************************************************/
    public void transform() throws DataFlowException
    {
        ObjectMatrix1D oin;
        boolean        fire;
        
        // If there's data present...
        if (areInputsAvailable(0, 1))
        {
            // Get data from input port
            oin = (ObjectMatrix1D)getInput(0);
            
            try
            {
                // Evaluate Condition
                fire = cond.apply(oin);
            }
            catch(LearnerException ex) { throw new DataFlowException(ex); }
            
            // Send to the correct output port. 0 for TRUE. 1 for FALSE.
            if   (fire) setOutput(0, oin);
            else        setOutput(1, oin);
        }
    }
    
    /**
     *  One input.
     *  @return '1'
     */
    public int getNumberOfInputs()  { return(1); }
    
    /**
     *  Two outputs. The TRUE and FALSE ports.
     *  @return '2'
     */
    public int getNumberOfOutputs() { return(2); }
    
    public String getOutputName(int port)
    {
        if      (port == 0) return("TRUE output");
        else if (port == 1) return("FALSE output");
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
        DataModel        dmsup;
        DataModelObject  dmin;
        
        // Make sure the input is compatible with this transformation's data requirements
        dmsup = getSupplierDataModel(0);
        checkDataModelFit(0, dmsup);
        dmin  = (DataModelObject)dmsup;
        
        // Set and create the DataModels
        setInputDataModel(0,  dmin);
        setOutputDataModel(0, dmin);
        setOutputDataModel(1, dmin);
        this.dataModel = dmin;
        
        // Set the datamodel in the Condition
        cond.setDataModel(dmin);
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    public void checkDataModelFit(int port, DataModel dm) throws DataModelException
    {
        // Allow only Object based data
        if (dm.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector))
            throw new DataModelException("Can only use Object Vectors");
    }
    
    // **********************************************************\
    // *                     Constructor                        *
    // **********************************************************/
    public IfThenElse()
    {
        super();
        name        = "IfThenElse";
        description = "Route to THEN or ELSE output depending on a BooleanCondition";
    }
}