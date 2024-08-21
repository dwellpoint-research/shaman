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
package org.shaman.datamodel;

import java.io.Serializable;

import org.shaman.exceptions.DataModelException;

import cern.jet.random.Uniform;


/**
 * <h2>Fuzzy Continuous Data Property</h2>
 * Extends the contiguous data property with some fuzyness.
 */
public class AttributePropertyFuzzyContinuous extends AttributePropertyFuzzy implements Serializable
{
    public static final int TYPE_SOFT_FUZZY = 1;
    public static final int TYPE_LATTICE    = 2;
    
    private FMF    []fmf;
    
    private static double fuzzyThreshold;
    
    // **********************************************************\
    // *             Continuous Data Fuzzyfication              *
    // **********************************************************/
    public double getThreshold()
    {
        return(AttributePropertyFuzzyContinuous.fuzzyThreshold);
    }
    
    public static void setFuzzyThreshold(double fuzzyThreshold)
    {
        AttributePropertyFuzzyContinuous.fuzzyThreshold = fuzzyThreshold;
    }
    
    public static double getFuzzyThreshold()
    {
        return(AttributePropertyFuzzyContinuous.fuzzyThreshold);
    }
    
    public FMF getFMF(double v) throws DataModelException
    {
        int    i;
        double nowmember, maxmember;
        int    maxind;
        
        if (this.fmf != null)
        {
            maxmember = this.fmf[0].apply(v);;
            maxind    = 0;
            for (i=1; i<fmf.length; i++)
            {
                nowmember = this.fmf[i].apply(v);
                if (nowmember > maxmember)
                {
                    maxind    = i;
                    maxmember = nowmember;
                }
            }
            
            return(this.fmf[maxind]);
        }
        else return(null);
    }
    
    public FMF getFMF(Object o) throws DataModelException
    {
        return(null);
    }
    
    
    public FMF getRandomFMF() throws DataModelException
    {
        return(this.fmf[Uniform.staticNextIntFromTo(0, fmf.length-1)]);
    }
    
    public AttributePropertyFuzzyContinuous(FMF []fmf)
    {
        this.fmf = fmf;
    }
    
    public AttributePropertyFuzzyContinuous()
    {
    }
    
    public void setFMF(FMF []fmf)
    {
        this.fmf = fmf;
    }
    
    
    /**
     *  Check if the given Object lies within the legal continuous value intervals.
     *  @param at The Attribute's structure
     *  @param o The Object
     *  @return <code>true</code> if the Object is legal. <code>false</code> if not.
     *  @throws DataModelException If something is wrong with the Attribute's structure w.r.t this property.
     */
    public boolean isLegal(AttributeObject at, Object o) throws DataModelException
    {
        int     i;
        Order   atorder;
        boolean foundlegal;
        Object  []obLegal;
        
        foundlegal = false;
        if (o != null)
        {
            atorder    = at.getOrder();
            obLegal    = at.getLegalValues();
            if ((atorder != null) && (obLegal != null))
            {
                for (i=0; (i<obLegal.length) && (!foundlegal); i+=2)
                {
                    if ((at.order(o, obLegal[i])   >= 0) &&
                            (at.order(obLegal[i+1], o) >= 0)) foundlegal = true;
                }
            }
            else throw new DataModelException("Cannot check Continuous Attribute Property because the AttributeObject's structure is wrong.");
        }
        
        return(foundlegal);
    }
    
    /**
     * Check if the given value within the legal continuous value intervals.
     * @param at The Attribute describing the structure of the values
     * @param d The value
     * @return <code>true</code> if the value is legal. <code>false</code> if not.
     * @throws DataModelException If something is wrong with the Attribute's structure w.r.t this property.
     */
    public boolean isLegal(AttributeDouble at, double d) throws DataModelException
    {
        int     i;
        Order   atorder;
        boolean foundlegal;
        double  []doLegal;
        
        atorder    = at.getOrder();
        doLegal    = at.getLegalValues();
        foundlegal = false;
        if ((atorder != null) && (doLegal != null))
        {
            for (i=0; (i<doLegal.length) && (!foundlegal); i+=2)
            {
                if ((at.order(d, doLegal[i])   >= 0) &&
                        (at.order(doLegal[i+1], d) >= 0)) foundlegal = true;
            }
        }
        else throw new DataModelException("Cannot check Continuous Attribute Property because the AttributeDouble's structure is wrong.");
        
        return(foundlegal);
    }
}
