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

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;

/**
 * <h2>Vector Type Property</h2>
 */
public class DataModelPropertyVectorType
{
    /** Vectors are COLT double vectors */
    public static DataModelProperty doubleVector = new DoubleVectorProperty();
    /** Vectors are COLT Object vectors */ 
    public static DataModelProperty objectVector = new ObjectVectorProperty();

    /**
     * DataModel property defining the Vector associated with the DataModel
     * as 1 dimensional COLT matrices containing doubles..
     */
    public static class DoubleVectorProperty implements DataModelProperty, Comparable
    {
        public static final Class vectorClass = DoubleMatrix1D.class;
        
        public int compareTo(Object o)
        {
            if (o instanceof DoubleVectorProperty) return(0);
            else                                   return(-1);
        }
    }
    
    /**
     * DataModel property defining the Vector associated with the DataModel
     * as 1 dimensional COLT matrices containing Objects.
     */
    public static class ObjectVectorProperty implements DataModelProperty, Comparable
    {
        public static final Class vectorClass = ObjectMatrix1D.class;
        
        public int compareTo(Object o)
        {
            if (o instanceof ObjectVectorProperty) return(0);
            else                                   return(-1);
        }
    }
    
    private DataModelPropertyVectorType()
    {
        // Use static members.
    }
}
