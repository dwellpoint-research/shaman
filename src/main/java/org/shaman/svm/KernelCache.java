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
package org.shaman.svm;

import java.util.HashMap;
import java.util.Map;

import org.shaman.exceptions.LearnerException;
import org.shaman.learning.Presenter;

import cern.colt.matrix.DoubleMatrix1D;



/**
 * <h2>Kernel Matrix Cache</h2>
 * Stores entries of the Kernel Matrix that have
 * been evaluated to avoid duplicate calculations.
 */

// **********************************************************\
// *                    Kernel Matrix Cache                 *
// **********************************************************/
public class KernelCache
{
    private Kernel    kernel;
    private Presenter x;
    private Map       cacheMap;   
    
    public void set(int i, int j, double m)
    {
        String key;
        Double value;
        
        key   = i+"@"+j;
        value = new Double(m);
        this.cacheMap.put(key, value);
    }
    
    public double get(int i, int j) throws LearnerException
    {
        Double value;
        double hit;
        
        // Check if the value is there
        value = (Double)this.cacheMap.get(i+"@"+j);
        if (value != null) hit = value.doubleValue();
        else
        {
            // If not there, calculate it
            DoubleMatrix1D a = x.getInstance(i);
            DoubleMatrix1D b = x.getInstance(j);
            hit   = this.kernel.apply(a,b);
            
            // And insert in cache
            value = new Double(hit);
            this.cacheMap.put(i+"@"+j, value);
        }
        
        return(hit); 
    }
    
    public KernelCache(Kernel kernel, Presenter x)
    {
        this.x        = x;
        this.kernel   = kernel;
        this.cacheMap = new HashMap();
    }
}
