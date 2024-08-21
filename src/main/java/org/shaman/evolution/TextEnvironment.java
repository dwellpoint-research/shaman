/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *               Evolutionary Algorithms                 *
 *                                                       *
 *  April 2005                                           *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2005 Shaman Research                   *
\*********************************************************/
package org.shaman.evolution;

import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.LearnerException;


/**
 * <h2>Text String Environment</h2>
 * Evolutionary environment for text Strings. Goal text is defined before-hand. GA tries to find this text.
 * No pratical use, but nice to have around of algorithm testing and tuning.
 */
public class TextEnvironment implements Environment
{
    private String goalText;       // The text string to search for
    
    // *********************************************************\
    // *          Environment Interface Implementation         *
    // *********************************************************/
    public Genotype makeRandomGenotype() throws LearnerException
    {
        TextGenotype genotype;
        
        genotype = new TextGenotype();
        genotype.initRandom(this.goalText.length());
        
        return(genotype);
    }
    
    public FitnessFunction makeFitnessFunction() throws LearnerException
    {
        TextFitnessFunction fit;
        
        fit = new TextFitnessFunction();
        fit.setEnvironment(this);
        
        return(fit);
    }
    
    // *********************************************************\
    // *                Parameter Configuration                *
    // *********************************************************/
    public String getText() { return(this.goalText); }
    
    public void setText(String text) { this.goalText = text; }
    
    // *********************************************************\
    // *               Initialization / Cleanup                *
    // *********************************************************/
    public void initialize()
    {
    }
    
    public void cleanUp() throws DataFlowException
    {
    }
    
    // *********************************************************\
    // *                    Construction                       *
    // *********************************************************/
    public TextEnvironment()
    {
    }
}