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
import org.shaman.learning.Classifier;
import org.shaman.learning.ClassifierTransformation;
import org.shaman.learning.Presenter;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Expression Based Classifier</h2>
 * Classify input using an expression. The output
 * of the expression is the class of the input vector.
 */
// **********************************************************\
// *               Expression Based Classifier              *
// **********************************************************/
public class ExpressionClassifier extends ClassifierTransformation implements Classifier
{
    private String           expression;       // The Classification Expression.
    private ExpressionParser parserTemplate;   // The parser template used to create the parser.
    
    // *** Run-time Data ****
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
    public void setExpression(String _expression)                   { this.expression  = _expression; }
    public void setParserTemplate(ExpressionParser _parserTemplate) { this.parserTemplate = _parserTemplate; }
    
    // **********************************************************\
    // *           Route according to the Expression             *
    // **********************************************************/
    public int classify(ObjectMatrix1D instance, double []confidence) throws LearnerException
    {
        Object oout;
        Number nout;
        int    clout;
        
        // Evaluate the classification expression on the input vector
        oout = this.parser.getValueAsObject(instance);
        if (oout instanceof Number)
        {
            nout  = (Number)oout;
            clout = nout.intValue();
        }
        else throw new LearnerException("Expecting a Number as output and not a "+oout.getClass().getName());
        
        return(clout);
    }
    
    public int classify(DoubleMatrix1D instance, double []confidence) throws LearnerException
    {
        double dout;
        int    clout;
        
        // Evaluate the classification expression on the input vector
        dout  = this.parser.getValue(instance);
        clout = (int)dout;
        
        return(clout);
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
        checkClassifierDataModelFit(dm, false, true, true);
    }
    
    // **********************************************************\
    // *                      Constructor                       *
    // **********************************************************/
    public ExpressionClassifier()
    {
        super();
        name        = "ExpressionClassifier";
        description = "Classify input based on an expression";
    }
}