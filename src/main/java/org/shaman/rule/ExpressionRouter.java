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
 * <h2>Expression Based Data Flow Router</h2>
 * Copies it's input to one or more output ports. The output
 * ports are selected by checking if their associated
 * boolean expressions evaluates to 'true' on the input vector.
 * This Flow can perform expression based data routing in a flow network.
 */

// *********************************************************\
// *               Expression Flow Routing                 *
// *********************************************************/
public class ExpressionRouter extends Transformation
{
    private DataModel        dataModel;     // Input (and output) datamodel
    private ExpressionParser parseTemp;     // Template of the parsers to use for the expressions
    private String         []expression;    // The Boolean expressions of the output ports.
    private int              numOut;        // The number of output ports. One for each expression.
    
    // *** Run-time Data ***
    private boolean            primitive;   // Is the data double- or Object-based
    private ExpressionParser []parser;      // The Expression Parsers.
    
    // **********************************************************\
    // *            Setting up the Expression Parsers           *
    // **********************************************************/
    private void parserSetup(DataModel dmin) throws ConfigException
    {
        int       i;
        
        try
        {
            // Make sure there's a parser template
            if (this.parseTemp == null) this.parseTemp = new ExpressionParser();
            
            // Make the Parsers
            this.parser = new ExpressionParser[this.numOut];
            for (i=0; i<this.numOut; i++)
            {
                this.parser[i] = (ExpressionParser)this.parseTemp.clone();
            }
        }
        catch(CloneNotSupportedException ex) { throw new ConfigException(ex); }
        
        // Set up the parsers. Add datamodel vatiables. Try to parse the expressions.
        for (i=0; i<expression.length; i++)
        {
            parser[i].checkDataModelFit(dmin);
            parser[i].setDataModel(dmin);
            parser[i].parseExp(expression[i]);
        }
    }
    
    // **********************************************************\
    // *                   Access to the Parsers                 *
    // **********************************************************/
    public ExpressionParser getParser(int ind) throws LearnerException
    {
        if ((parser != null) && (parser.length > ind)) return(parser[ind]);
        else throw new LearnerException("Expressions not set or parser index out of bound.");
    }
    
    // **********************************************************\
    // *                Expression Router Parameters            *
    // **********************************************************/
    /**
     * Set the boolean expressions that select the output port(s).
     * @param _expression The boolean expressions that are used to select to output port(s).
     */
    public void setExpressions(String []_expression) { expression = _expression;  }
    
    /**
     * Get the rules that select the output port(s) in case of TYPE_RULE routing.
     * @return The rules that select the output port(s)
     */
    public String []getExpressions() { return(expression); }
    
    public void setParserTemplate(ExpressionParser parseTemp)
    {
        this.parseTemp = parseTemp;
    }
    
    // **********************************************************\
    // *            Transformation/Flow Interface               *
    // **********************************************************/
    /**
     * Route data to the appropriate output port(s).
     * Send it to all output ports whose expressions evaluate to 'true'.
     * @throws DataFlowException If there's a problem using the Flow base-class or
     *                           if the expression cannot be evaluated.
     */
    public void transform() throws DataFlowException
    {
        int            i;
        Object         obin;
        DoubleMatrix1D dovec;
        ObjectMatrix1D obvec;
        double         doout;
        Object         obout;
        
        // Check input availability on port 0
        if (areInputsAvailable(0, 1))
        {
            // Get the data vector in the right format. Make an instance.
            dovec = null;
            obvec = null;
            obin  = getInput(0);
            try
            {
                if (this.primitive)
                {
                    // Parse the expression with primitive data.
                    dovec = (DoubleMatrix1D)obin;
                    for (i=0; i<parser.length; i++)
                    {
                        // Route to the current output port if the expression evaluates to '1'
                        doout = parser[i].getValue(dovec);
                        if (doout == 1.0) setOutput(i, dovec);
                    }
                }
                else
                {
                    // Parse the expression with object data.
                    obvec   = (ObjectMatrix1D)obin;
                    for (i=0; i<parser.length; i++)
                    {
                        // Route to the current output port if the expression evaluates to '1'
                        obout = parser[i].getValueAsObject(obvec);
                        if (obout instanceof Double)
                        {
                            if (((Double)obout).intValue() == 1.0) setOutput(i, obvec);
                        }
                    }
                }
            }
            catch(LearnerException ex) { throw new DataFlowException(ex); }
        }
    }
    
    /**
     * Create a Expression Router with the given amount of output ports.
     * Should be called right after the constructor.
     * @param _numOut The number of output ports to install on this Expression Router.
     */
    public void grow(int _numOut) throws ConfigException
    {
        this.numOut = _numOut;
        super.grow(1, _numOut);
    }
    
    public void init() throws ConfigException
    {
        int        i;
        DataModel  dmin;
        
        // Check if the DataModel is compatible with the Conditions
        dmin = getSupplierDataModel(0);
        checkDataModelFit(0, dmin);
        if (dmin.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector)) this.primitive = true;
        else                                                                               this.primitive = false;
        
        if ((expression != null) && (expression.length == numOut))
        {
            // Try to initialize the Parsers
            parserSetup(dmin);
            
            // Set the Output Datamodels
            for (i=0; i<numOut; i++) setOutputDataModel(i, dmin);
            
            // Remember the datamodel
            this.dataModel = dmin;
        }
        else throw new DataModelException("Expression not there are mismatch with the number of outputs.");
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    public String getOutputName(int port)
    {
        if (port == 0) return("Expression Routed Output");
        else return(null);
    }
    
    public String getInputName(int port)
    {
        if (port == 0) return("Input");
        else return(null);
    }
    
    public int getNumberOfInputs() { return(1); }
    public int getNumberOfOutputs()
    {
        return(this.numOut);
    }
    
    public void checkDataModelFit(int port, DataModel dataModel) throws DataModelException
    {
        // All fine.
    }
    
    // **********************************************************\
    // *                      Construction                      *
    // **********************************************************/
    public ExpressionRouter()
    {
        super();
        this.name        = "Expression Router";
        this.description = "Flow Network Routing Logic based on boolean expressions.";
    }
}