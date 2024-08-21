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


import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.Estimator;
import org.shaman.learning.EstimatorTransformation;
import org.shaman.learning.Presenter;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectFactory1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Expression Based Estimator</h2>
 * Estimate a function using expression(s). The output
 * of the expression(s) is the estimated function value.
 */
// **********************************************************\
// *               Expression Based Classifier              *
// **********************************************************/
public class ExpressionEstimator extends EstimatorTransformation implements Estimator
{
    private String         []expression;       // The Classification Expression.
    private ExpressionParser parserTemplate;   // The parser template used to create the parser.
    
    // *** Run-time Data ****
    private ExpressionParser []parser;         // The Parser used for condition Evaluation.
    
    // **********************************************************\
    // *             Set Up the Expression Parser               *
    // **********************************************************/
    private void parserSetup(DataModel dmin) throws ConfigException
    {
        try
        {
            int              i;
            ExpressionParser parser;
            
            this.parser = new ExpressionParser[this.expression.length];
            for (i=0; i<this.expression.length; i++)
            {
                // Create the parser in the image of the template or as a default parser.
                if (parserTemplate != null) parser = (ExpressionParser)parserTemplate.clone();
                else                        parser = new ExpressionParser();
                
                // Check if the parser (and it's Transformation Functions) can handle the input.
                parser.checkDataModelFit(dmin);
                
                // Set-up the Parser <-> DataModel bridge
                parser.setDataModel(dmin);
                
                // Parse the Expressions. Report any errors in the expressions.
                parser.parseExp(expression[i]);
                
                // Commit parser
                this.parser[i] = parser;
            }
        }
        catch(CloneNotSupportedException ex) { throw new ConfigException(ex); }
    }
    
    // **********************************************************\
    // *               Nothing to do for Training               *
    // **********************************************************/
    public void train()              throws LearnerException { }
    public void initializeTraining() throws LearnerException { }
    public void setTrainSet(Presenter trainData) throws LearnerException { }
    public Presenter getTrainSet()  { return(null); }
    public boolean   isSupervised() { return(true); }
    
    // **********************************************************\
    // *              Expression Configuration                  *
    // **********************************************************/
    public void setExpression(String _expression)
    {
        this.expression = new String[]{_expression};
    }
    
    public void setExpressions(String []expression) { this.expression = expression; }
    public void setParserTemplate(ExpressionParser _parserTemplate) { this.parserTemplate = _parserTemplate; }
    
    // **********************************************************\
    // *           Route according to the Expression             *
    // **********************************************************/
    public ObjectMatrix1D estimate(ObjectMatrix1D instance, double []confidence) throws LearnerException
    {
        ObjectMatrix1D estout;
        Object         oout;
        int            i;
        
        estout = ObjectFactory1D.dense.make(this.expression.length);
        for (i=0; i<this.expression.length; i++)
        {
            oout = this.parser[i].getValueAsObject(instance);
            estout.setQuick(i, oout);
        }
        
        return(estout);
    }
    
    public DoubleMatrix1D estimate(DoubleMatrix1D instance, double []confidence) throws LearnerException
    {
        DoubleMatrix1D estout;
        int            i;
        double         dout;
        
        estout = DoubleFactory1D.dense.make(this.expression.length);
        for (i=0; i<this.expression.length; i++)
        {
            dout = this.parser[i].getValue(instance);
            estout.setQuick(i, dout);
        }
        
        return(estout);
    }
    
    // **********************************************************\
    // *                       Construction                     *
    // **********************************************************/
    public void init() throws ConfigException
    {
        super.init();
        
        // Initialize the Parsers
        parserSetup(this.dataModel);
    }
    
    public void cleanUp() throws DataFlowException
    {
    }
    
    public void checkDataModelFit(int port, DataModel dm) throws DataModelException
    {
        checkEstimatorDataModelFit(dm, true, true);
    }
    
    // **********************************************************\
    // *                      Constructor                       *
    // **********************************************************/
    public ExpressionEstimator()
    {
        super();
        name        = "ExpressionEstimator";
        description = "Estimate function based on expressions";
    }
}