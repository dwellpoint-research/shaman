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

/**
 * <h2>Categorical Fuzzy Membership Function</h2>
 */
public class FMFCategorical implements FMF
{
    // **********************************************************\
    // *         Categorical Fuzzy Membership Function          *
    // **********************************************************/
    private int    cat;
    private double []val;
    private Object []oval;
    private double []fmf;
    
    public FMFCategorical(Attribute at, int _cat) throws DataModelException
    {
        int             i, numcat;
        
        cat    = _cat;
        numcat = at.getNumberOfCategories();
        val    = new double[numcat];
        fmf    = new double[numcat];
        
        if (at instanceof AttributeDouble)
        {
            AttributeDouble atdo = (AttributeDouble)at;
            for (i=0; i<numcat; i++)
            {
                val[i] = atdo.getCategoryDouble(i);
                if (i == cat) fmf[i] = 1.0;
                else          fmf[i] = 0.0;
            }
        }
        else
        {
            AttributeObject atob = (AttributeObject)at;
            for (i=0; i<numcat; i++)
            {
                oval[i] = atob.getCategoryObject(i);
                if (i == cat) fmf[i] = 1.0;
                else          fmf[i] = 0.0;
            }
        }
    }
    
    public double apply(double v) throws DataModelException
    {
        int     i;
        boolean found;
        double  fv;
        
        fv    = 0.0;
        found = false;
        for (i=0; (i<val.length) && (!found); i++)
        {
            if (val[i] == v) { fv = fmf[i]; found = true; }
        }
        
        return(fv);
    }
    
    public double apply(Object o) throws DataModelException
    {
        int     i;
        boolean found;
        double  fv;
        
        fv    = 0.0;
        found = false;
        for (i=0; (i<val.length) && (!found); i++)
        {
            if (oval[i].equals(o)) { fv = fmf[i]; found = true; }
        }
        
        return(fv);
    }
}