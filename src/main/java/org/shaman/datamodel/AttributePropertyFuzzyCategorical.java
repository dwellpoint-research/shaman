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

import org.shaman.exceptions.DataModelException;

import cern.jet.random.Uniform;


/**
 * <h2>Fuzzy Categorical Data Property</h2>
 * Extends the Categorical data property with fuzzyness,
 * using a fuzzy membership matrix.
 */
public class AttributePropertyFuzzyCategorical extends AttributePropertyFuzzy
{
    public static final int TYPE_CRISP = 1;  // Not fuzzy at all....
    
    private int       type;      // Type of FMF
    private Attribute at;        // The Attribute this is a property of.
    
    // All possible Fuzzy Membership Functions
    private FMF    []fmf;
    
    // **********************************************************\
    // *                   Data Fuzzyfication                   *
    // **********************************************************/
    public FMF getRandomFMF() throws DataModelException
    {
        return(fmf[Uniform.staticNextIntFromTo(0, fmf.length-1)]);
    }
    
    public FMF getFMF(double v) throws DataModelException
    {
        int             cat;
        AttributeDouble atdo;
        FMFCategorical  fmf;
        
        atdo = (AttributeDouble)at;
        cat  = atdo.getCategory(v);
        if (cat != -1) fmf = new FMFCategorical(this.at, cat);
        else throw new DataModelException("Can't determine category of value "+v);
        
        return(fmf);
    }
    
    public FMF getFMF(Object o) throws DataModelException
    {
        int             cat;
        AttributeObject atob;
        FMFCategorical  fmf;
        
        atob = (AttributeObject)at;
        cat  = atob.getCategory(o);
        if (cat != -1) fmf = new FMFCategorical(this.at, cat);
        else throw new DataModelException("Can't determine category of value "+o);
        
        return(fmf);
    }
    
    public AttributePropertyFuzzyCategorical(Attribute _at, int _type) throws DataModelException
    {
        this.at   = _at;
        this.type = _type;
        if (type == TYPE_CRISP)
        {
            int             i, numc;
            
            numc = at.getNumberOfCategories();
            fmf  = new FMF[numc];
            for (i=0; i<numc; i++)
            {
                fmf[i] = new FMFCategorical(at, i);
            }
        }
        else throw new DataModelException("Don't know other than CRISP type. Try again later.");
    }
    
    
    // **********************************************************\
    // *        AttributePropery Categorical Implementation     *
    // **********************************************************/
    /**
     *  Check if the given Object is found the in the legal values list.
     *  @param at The Attribute's structure
     *  @param o The Object
     *  @return <code>true</code> if the Object is found. <code>false</code> if not.
     *  @throws DataModelException If something is wrong with the Attribute's structure w.r.t this property.
     */
    public boolean isLegal(AttributeObject at, Object o) throws DataModelException
    {
        int     i;
        boolean foundlegal;
        Object  []obLegal;
        
        obLegal    = at.getLegalValues();
        foundlegal = false;
        if (obLegal != null)
        {
            for (i=0; (i<obLegal.length) && (!foundlegal); i++)
            {
                if   (o == null) foundlegal = (obLegal[i] == null);
                else             foundlegal = o.equals(obLegal[i]);
            }
        }
        else throw new DataModelException("Cannot check Categorical Attribute Property because legal values list is missing.");
        
        return(foundlegal);
    }
    
    /**
     * Check if the given value is found in the legal values list.
     * @param at The Attribute describing the structure of the values
     * @param d The value
     * @return <code>true</code> if the value is found. <code>false</code> if not.
     * @throws DataModelException If something is wrong with the Attribute's structure w.r.t this property.
     */
    public boolean isLegal(AttributeDouble at, double d) throws DataModelException
    {
        int     i;
        boolean foundlegal;
        double  []doLegal;
        
        doLegal    = at.getLegalValues();
        foundlegal = false;
        if (doLegal != null)
        {
            for (i=0; (i<doLegal.length) && (!foundlegal); i++)
            {
                if (Double.isNaN(d)) foundlegal = Double.isNaN(doLegal[i]);
                else                 foundlegal = (d == doLegal[i]);
            }
        }
        else throw new DataModelException("Cannot check Categorical Attribute Property because legal values list is missing.");
        
        return(foundlegal);
    }
}
