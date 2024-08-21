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

import cern.jet.random.AbstractDistribution;
import org.shaman.exceptions.ConfigException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * <h2>Real Number Genotype</h2>
 */
public class NumberGenotype implements Genotype
{
    private AbstractDistribution []pdf;       // [i] = Probability Distribution Function of gene i
    private double               []gene;      // [i] = Real value of gene i
    
    // *********************************************************\
    // *                  Genetic Operators                    *
    // *********************************************************/
    public Genotype crossover(Genotype genotype2, int crossOverPosition)
    {
        NumberGenotype numberGenotype2;
        NumberGenotype crossOverGenotype;
        double       []vector1;
        double       []vector2;
        double       []crossOverVector;
        
        // Return number genotype with first part of the gene-string from this genotype and after the cross-over position from the second genotype
        numberGenotype2 = (NumberGenotype) genotype2;
        vector1  = getGenes();
        vector2  = numberGenotype2.getGenes();
        crossOverVector  = new double[vector1.length];
        for (int i=0; i<vector1.length; i++)
        {
            if (i<crossOverPosition) crossOverVector[i] = vector1[i];
            else                     crossOverVector[i] = vector2[i];
        }
        crossOverGenotype = new NumberGenotype(this.pdf);
        crossOverGenotype.setGenes(crossOverVector);
        
        return(crossOverGenotype);
    }
    
    public void mutate(int position)
    {
        // Replace gene at given position by a new number drawn at random from the PDF at this position.
        this.gene[position] = this.pdf[position].nextDouble();
    }
    
    // *********************************************************\
    // *                  Parameter Access                     *
    // *********************************************************/
    public void     setGenes(double []gene) { this.gene = gene; }
    public double []getGenes()   { return(this.gene); }
    public int      getLength()  { return(this.pdf.length); }
    
    // *********************************************************\
    // *                      Persistence                      *
    // *********************************************************/
    public void loadState(ObjectInputStream ois) throws ConfigException
    {
        double []gin;
        
        try
        {
            gin = (double [])ois.readObject();
            this.gene = gin;
        }
        catch(IOException ex)            { throw new ConfigException(ex); }
        catch(ClassNotFoundException ex) { throw new ConfigException(ex); }
    }
    
    public void saveState(ObjectOutputStream oos) throws ConfigException
    {
        try
        {
            oos.writeObject(this.gene);
        }
        catch(IOException ex) { throw new ConfigException(ex); }
    }
    
    // *********************************************************\
    // *                   Initialization                      *
    // *********************************************************/
    public void initRandom(int length)
    {
        int    i;
        
        // Create a new random genotype as real-vector
        this.gene = new double[this.pdf.length];
        for (i=0; i<this.pdf.length; i++)
        {
            this.gene[i] = this.pdf[i].nextDouble();
        }
    }
    
    public NumberGenotype(AbstractDistribution []pdf)
    {
        // The real-number based genotype whose genes are linked to a continuous probability distribution function.
        this.pdf = pdf;
    }
}