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
import org.shaman.datamodel.DataModelPropertyVectorType;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;



/**
 * <h2>Expression Based Rule</h2>
 * Evaluate a Logical Expression using arbitrary mathematical symbols,
 * the input attributes as variables and external Transformations
 * as functions.
 *
 * <br>
 * The Input vector is sent to the first output port if it is TRUE
 * and to the second output port if it is FALSE. If the expression
 * could not be evaulated, it is not copied to any output.
 *
 * <br>
 * Uses the Java Expression Parser.
 *     http://jep.sourceforge.net/
 */

// **********************************************************\
// *            Expression Based Rule Evaluation            *
// **********************************************************/
public class ExpressionRule extends Transformation
{
    // Input DataModel
    private DataModel dataModel;               // The Incomming Data-Vectors DataModel.
    private String    expression;              // The Rule Expression.
    
    private ExpressionParser   parserTemplate; // The parser template used to create the parser.
    
    // *** Run-time Data ****
    private boolean           primitive;       // Is the data double- or Object-based
    private ExpressionParser  parser;          // The Parser used for condition Evaluation.
    
    // **********************************************************\
    // *             Set Up the Expression Parser               *
    // **********************************************************/
    private void parserSetup(DataModel dmin) throws ConfigException
    {
        try
        {
            ExpressionParser parser;
                
            // Create the parser in the image of the template or as a default parser.
            if (parserTemplate != null)
            {
                parser = (ExpressionParser)parserTemplate.clone();
            }
            else parser = new ExpressionParser();
            
            // Check if the parser (and it's Transformation Functions) can handle the input.
            parser.checkDataModelFit(dmin);
            
            // Set-up the Parser <-> DataModel bridge
            parser.setDataModel(dmin);
            
            // Parse the Expressions. Report any errors in the expressions.
            parser.parseExp(expression);
            
            // Commit parser
            this.parser = parser;
        }
        catch(CloneNotSupportedException ex) { throw new ConfigException(ex); }
    }
    
    // **********************************************************\
    // *                   Expression Access                    *
    // **********************************************************/
    /**
     * Set the boolean Rule condition to evaluate. The IF confition in the IF THEN ELSE template.
     * @param _expression The condition to evaluate.
     */
    public void setExpression(String _expression)
    {
        expression  = _expression;
    }
    
    /**
     * Return the boolean Rule condition to evaluate.
     * @return The expression that is evaluated.
     */ 
    public String getExpression()
    {
        return(expression);
    }
    
    /**
     * Set the ExpressionParser to use as a template for the parsers used during evaluation.
     * @param _parserTemplate The parser to use as a template for the ones used here. 
     */
    public void setParserTemplate(ExpressionParser _parserTemplate)
    {
        this.parserTemplate = _parserTemplate;
    }
    
    // **********************************************************\
    // *           Route according to the Expression             *
    // **********************************************************/
    public void transform() throws DataFlowException
    {
        Object         obin;
        DoubleMatrix1D din;
        ObjectMatrix1D oin;
        double         dout;
        Object         oout;
        int            rout;
        
        // If there's data present...
        if (areInputsAvailable(0, 1))
        {
            // Get data from 1 port input data
            obin = getInput(0);
            
            try
            {
                // Evaluate Primitive or Object Based Rule.
                if (this.primitive)
                {
                    din  = (DoubleMatrix1D)obin;
                    dout = this.parser.getValue(din);
                    if (dout == 1) rout = 1;
                    else           rout = 0;
                }
                else
                {
                    oin  = (ObjectMatrix1D)obin;
                    oout = this.parser.getValueAsObject(oin);
                    if (oout instanceof Double)
                    {
                        if (((Double)oout).intValue() == 1) rout = 1;
                        else                                rout = 0;
                    }
                    else throw new DataFlowException("The Expression '"+expression+"' does not evaluate to a Boolean value but to : "+oout);
                }
            }
            catch(LearnerException ex) { throw new DataFlowException(ex); }
            
            // Send to the correct output port
            if      (rout == 1) setOutput(0, obin);
            else if (rout == 0) setOutput(1, obin);
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
        DataModel dmin;
        
        // Make sure the input is compatible with this transformation's data requirements
        dmin = getSupplierDataModel(0);
        checkDataModelFit(0, dmin);
        if (dmin.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector)) this.primitive = true;
        else                                                                               this.primitive = false;
        
        // Initialize the Parsers
        parserSetup(dmin);
        
        // Set and create the DataModels
        setInputDataModel(0,  dmin);
        setOutputDataModel(0, dmin);
        setOutputDataModel(1, dmin);
        this.dataModel = dmin;
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    public void create() throws LearnerException
    {
        // Do smart stuff...
    }
    
    public void checkDataModelFit(int port, DataModel dm) throws DataModelException
    {
    }
    
    // **********************************************************\
    // *                      Constructor                       *
    // **********************************************************/
    public ExpressionRule()
    {
        super();
        name        = "ExpressionRule";
        description = "Evaluate a Logical Expression.";
    }
}