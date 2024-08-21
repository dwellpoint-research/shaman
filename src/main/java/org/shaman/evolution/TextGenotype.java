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

import cern.jet.random.Empirical;
import cern.jet.random.EmpiricalWalker;
import cern.jet.random.engine.MersenneTwister;
import org.shaman.exceptions.ConfigException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * <h2>Text String Genotype</h2>
 * English text String genotype. Contains a String of characters
 * from [a...z] + [ ]. Probability of a letter occuring is derived
 * from empirically observed frequency found in standard english texts.
 */
public class TextGenotype implements Genotype
{
    private String textString;
    
    // Make English letter frequency empirical distribution and random generator
    public static final char[]LETTERS =
    { 'e', 't', 'i', 'a', 'o', 'n', 's', 'r', 'h', 'c', 'l', 'd', 'p',
      'y', 'u', 'm', 'f', 'b', 'g', 'w', 'v', 'k', 'x', 'q', 'z', 'j', ' ' };
    
    public static final double[]LETTER_FREQ =
    {
        0.127, 0.097, 0.075, 0.073, 0.068, 0.067, 0.067, 0.064, 0.049, 0.045, 0.040, 0.031, 0.030, 
        0.027, 0.024, 0.024, 0.021, 0.017, 0.016, 0.013, 0.008, 0.008, 0.005, 0.002, 0.001, 0.001, 0.22 };
   
    public static final EmpiricalWalker randomLetter = new EmpiricalWalker(LETTER_FREQ, Empirical.NO_INTERPOLATION, new MersenneTwister());

    // *********************************************************\
    // *                   Genetic Operators                   *
    // *********************************************************/
    public void mutate(int position)
    {
        // Replace the letter at the given position by a random one
        char []chars = this.textString.toCharArray();
        chars[position] = LETTERS[randomLetter.nextInt()];
        this.textString = new String(chars);
    }
    
    public Genotype crossover(Genotype genotype2, int crossOverPosition)
    {
        TextGenotype crossOverGenotype;
        char       []chars1, chars2;
        char       []crossOverChars;
        int          length, i;
        
        // Return genotype with string composed of the first part (up until the given position) of 
        // this string and the rest with the string of the other genotype
        chars1 = getText().toCharArray();
        chars2 = ((TextGenotype)genotype2).getText().toCharArray();
        length = getLength();
        crossOverChars = new char[length];
        for(i=0; i<length; i++)
        {
            if (i<crossOverPosition) crossOverChars[i] = chars1[i];
            else                     crossOverChars[i] = chars2[i];
        }
        crossOverGenotype = new TextGenotype();
        crossOverGenotype.setText(new String(crossOverChars));
        
        return(crossOverGenotype);
    }
    
    public String toString() { return(this.textString); }
    
    // *********************************************************\
    // *                      Persistence                      *
    // *********************************************************/
    public void loadState(ObjectInputStream ois) throws ConfigException
    {
        String sin;
        
        try
        {
            sin = (String)ois.readObject();
            this.textString = sin;
        }
        catch(IOException ex)            { throw new ConfigException(ex); }
        catch(ClassNotFoundException ex) { throw new ConfigException(ex); }
    }
    
    public void saveState(ObjectOutputStream oos) throws ConfigException
    {
        try
        {
            oos.writeObject(this.textString);
        }
        catch(IOException ex) { throw new ConfigException(ex); }
    }
    
    // *********************************************************\
    // *                   Parameter Access                    *
    // *********************************************************/
    public void   setText(String text)  { this.textString = text; }
    public String getText()             { return(this.textString); }
    public int getLength()              { return(this.textString.length()); }
    
    // *********************************************************\
    // *               Initialization / Cleanup                *
    // *********************************************************/
    public void initRandom(int length)
    {
        int    i;
        char []ran;
        String rantxt;
        
        // Make a String of random letters
        ran = new char[length];
        for (i=0; i<ran.length; i++) ran[i] = LETTERS[randomLetter.nextInt()];
        rantxt = new String(ran);
        this.textString = rantxt;
    }
    
    public TextGenotype()
    {
        this.textString = null;
    }
}