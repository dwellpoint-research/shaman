/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                     Technologies                      *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  June 2002                                            *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2002 Shaman Research                   *
\*********************************************************/
package org.shaman.rule;


import org.shaman.dataflow.Transformation;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelObject;
import org.shaman.datamodel.DataModelPropertyVectorType;
import org.shaman.exceptions.ShamanException;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Expression Evaluator</h2>
 * Evaluate arbitrary mathematical expressions using
 * parameters, input values as variables and other
 * transformations as functions.
 *
 * <br>
 * @author Johan Kaers
 * @version 2.0
 */
// **********************************************************\
// *           Mathematical Expressions Evaluating          *
// **********************************************************/
public class ExpressionEvaluator extends Transformation
{
    /** Evaluate the expressions in parallel, using the same variable assignments */ 
    public static final int ASSIGN_PARALLEL   = 0;
    /** Evaluate the expressions one after the other. Update the variable assignments after every expression evaluation. */
    public static final int ASSIGN_SEQUENTIAL = 1;
    
    private int    assign;            // In what order to evaluate the expressions.
    private String []expression;      // The list of Expression and their destination attributes
    private String []destination;
    
    private ExpressionParser parserTemplate;  // Use this parser as a template for the one's used to evaluate the expressions
    private DataModel datamodelOut;    // The explicitly defined output datamodel. If 'null' the input datamodel is used.
    
    // Internal Stuff. Created by init()
    private DataModel            dataModel;     // The Input's DataModel.
    private boolean              primitive;     // Handling double or Object vectors?
    private ExpressionParser   []parser;        // The Expression Parsers.
    private int                []destind;       // The indices in the vector of the destination attributes
    
    // **********************************************************\
    // *                Evaluate the Expressions                *
    // **********************************************************/
    private DoubleMatrix1D evaluateExpressionsDouble(DoubleMatrix1D din) throws ShamanException
    {
        int            i;
        DoubleMatrix1D dout;
        
        // Start with the input vector. Or when the outputmodel is defined with it's default vector. 
        if (this.datamodelOut == null) dout = din.copy();
        else                           dout = ((DataModelDouble)this.datamodelOut).createDefaultVector();
        
        if      (this.assign == ASSIGN_PARALLEL)
        {
            // Evaluate all expression with same input values.
            for (i=0; i<parser.length; i++)
            {
                dout.setQuick(destind[i], parser[i].getValue(din));
            }
        }
        else if (this.assign == ASSIGN_SEQUENTIAL)
        {
            // Evaluate one after another.
            for (i=0; i<parser.length; i++)
            {
                dout.setQuick(destind[i], parser[i].getValue(dout));
            }
        }
        
        return(dout);
    }
    
    private ObjectMatrix1D evaluateExpressionsObject(ObjectMatrix1D oin) throws ShamanException
    {
        int            i;
        ObjectMatrix1D oout;
        Object         value;
        
        // Create the default output vector when the output datamodel is explicitly defines.
        // Else start with a copy of the input vector.
        if (this.datamodelOut == null) oout = oin.copy();
        else                           oout = ((DataModelObject)this.datamodelOut).createDefaultVector();
        if      (this.assign == ASSIGN_PARALLEL)
        {
            // Evaluate all expression with the same input vector
            for (i=0; i<parser.length; i++)
            {
                value = parser[i].getValueAsObject(oin);
                if (this.datamodelOut == null) ExpressionParser.setObjectDestination((DataModelObject)dataModel,    destind[i], value, oout);
                else                           ExpressionParser.setObjectDestination((DataModelObject)this.datamodelOut, destind[i], value, oout);
            }
        }
        else if (this.assign == ASSIGN_SEQUENTIAL)
        {
            // Evaluate the expressions one after another.
            for (i=0; i<parser.length; i++)
            {
                value = parser[i].getValueAsObject(oout);
                if (this.datamodelOut == null) ExpressionParser.setObjectDestination((DataModelObject)dataModel, destind[i], value, oout);
                else                           ExpressionParser.setObjectDestination((DataModelObject)this.datamodelOut, destind[i], value, oout);
            }
        }
        
        return(oout);
    }
    
    
    // **********************************************************\
    // *             Set Up the Expression Parsers              *
    // **********************************************************/
    private void parserSetup(DataModel dmin) throws ConfigException
    {
        int       i;
        
        if (parser == null) throw new ConfigException("Set the expressions first.");
        
        try
        {
            if (parserTemplate == null)  // If no Template Then use default parsers. Else clone Template.
                for (i=0; i<parser.length; i++) parser[i] = new ExpressionParser();
            else for (i=0; i<parser.length; i++) parser[i] = (ExpressionParser)parserTemplate.clone();
            
            // Set up the parsers. Add datamodel vatiables. Try to parse the expressions.
            for (i=0; i<expression.length; i++)
            {
                parser[i].checkDataModelFit(dmin);
                parser[i].setDataModel(dmin);
                parser[i].parseExp(expression[i]);
            }
        }
        catch (CloneNotSupportedException ex) { throw new ConfigException(ex); }
    }
    
    public ExpressionParser getParser(int ind) throws ShamanException
    {
        if ((parser != null) && (parser.length > ind)) return(parser[ind]);
        else throw new LearnerException("Expressions not set or parser index out of bound.");
    }
    
    
    // **********************************************************\
    // *                   Set the Expressions                  *
    // **********************************************************/
    /**
     * Set the Expressions that should be evaluated and their destination attributes.
     * @param _expression   The expressions to Evaluator
     * @param _destination  Their destination attributes.
     */
    public void setExpressions(String []_expression, String []_destination)
    {
        expression  = _expression;
        destination = _destination;
        
        parser = new ExpressionParser[expression.length];
    }
    
    /**
     * Set the order in which to evaluate the expressions.
     * One after another, or all at the same time.
     * @see #ASSIGN_PARALLEL
     * @see #ASSIGN_SEQUENTIAL
     * @param _assign The assignment order.
     */ 
    public void setAssignOrder(int _assign)
    {
        this.assign = _assign; 
    }
    
    /**
     * Set the ExpressionParser to use as a template for the parsers used during evaluation.
     * @param _parserTemplate The parser to use as a template for the ones used here. 
     */
    public void setParserTemplate(ExpressionParser _parserTemplate)
    {
        this.parserTemplate = _parserTemplate;
    }
    
    /**
     * Set datamodel of the output.
     * @param _datamodelOut The explicitly defined output datamodel
     */
    public void setDataModelOutput(DataModel _datamodelOut)
    {
        this.datamodelOut = _datamodelOut;
    }
    
    
    // **********************************************************\
    // *         Calculate the PCA of the incomming Vector      *
    // **********************************************************/
    public Object []transform(Object obin) throws DataFlowException
    {
        DoubleMatrix1D   din;
        ObjectMatrix1D   oin;
        Object           out;
        
        // Get the Object vector in the right data-format.
        out = null; din = null; oin = null;
        if (this.primitive) { din = (DoubleMatrix1D)obin; }
        else                { oin = (ObjectMatrix1D)obin; }
        
        if ((din != null) || (oin !=  null))
        {
            try
            {
                // Evaluate the Expressions for the incomming data vector.
                if (this.primitive) out = evaluateExpressionsDouble(din);
                else                out = evaluateExpressionsObject(oin);
            }
            catch(ShamanException ex) { throw new DataFlowException(ex); }
        }
        
        if (out == null) return(null);
        else             return(new Object[]{out});
    }
    
    public String getOutputName(int port)
    {
        if (port == 0) return("Expression Output");
        else return(null);
    }
    
    public String getInputName(int port)
    {
        if (port == 0) return("Input");
        else return(null);
    }
    
    public int getNumberOfInputs()  { return(1); }
    public int getNumberOfOutputs() { return(1); }
    
    // **********************************************************\
    // *                       Construction                     *
    // **********************************************************/
    public void init() throws ConfigException
    {
        int        i;
        DataModel  dmsup;
        DataModel  dmin;
        DataModel  dmout;
        
        // Make sure the input is compatible with this transformation's data requirements
        dmsup = getSupplierDataModel(0);
        checkDataModelFit(0, dmsup);
        dmin  = dmsup;
        
        // Make the corresponding output data model
        dmout = makeOutputDataModel(dmin);
        
        // Set the Input and Output datamodels
        setInputDataModel(0,dmin);
        setOutputDataModel(0,dmout);
        this.dataModel = dmin;
        if (dmin.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector)) this.primitive = true;
        else                                                                               this.primitive = false;
        
        // Initialize the Parsers
        parserSetup(dmin);
        
        // Make the index table of the destination attributes
        destind = new int[destination.length];
        for (i=0; i<destination.length; i++)
        {
            destind[i] = dmout.getAttributeIndex(destination[i]);
            if (destind[i] == -1) throw new DataModelException("Cannot evaluate to unknown output attribute '"+destination[i]+"'");
        }
        
        // Check if the assignment order and the outputdatamodel don't clash
        if ((this.assign == ASSIGN_SEQUENTIAL) && (this.datamodelOut != null))
        {
            throw new DataModelException("Cannot assign sequentially and have an explicitly defined output datmodel.");
        }
    }
    
    public void cleanUp() throws DataFlowException
    {
    }
    
    private DataModel makeOutputDataModel(DataModel dmin) throws ConfigException
    {
        DataModel dmout;
        
        if (this.datamodelOut == null)
        {
            // Copy the input datamodel
            try { dmout    = (DataModel)dmin.clone(); } catch(CloneNotSupportedException ex) { throw new ConfigException(ex); }
        }
        else
        {
            // Use the explicitly defined output datamodel
            dmout = this.datamodelOut;
        }
        
        return(dmout);
    }
    
    public void checkDataModelFit(int port, DataModel dm) throws DataModelException
    {
        // Happens when the Parsers are setup.
    }
    
    public ExpressionEvaluator()
    {
        super();
        name        = "Expression Evaluator";
        description = "Evaluate Arbitrary Mathematical Expressions.";
    }
}
