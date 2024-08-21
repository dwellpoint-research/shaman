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
 * <h2>Default Distance Between 2 Values</h2>
 * Simple difference between 2 real numbers. <br>
 *
 * @author Johan Kaers
 * @version 2.0
 */
public class DistanceNumber implements Distance
{
  /**
    * Tries to parse the objects as Strings containing Doubles, returns the absolute value of the difference.
    * @param at The Attribute describing the Objects internal structure
    * @param o1 Object 1
    * @param o2 Object 2
    * @return The distance between the two objects.
    * @throws DataModelException if the Objects cannot be parsed as Strings containing Doubles.
    */
   public double distance(AttributeObject at, Object o1, Object o2) throws DataModelException
   {
      double d1, d2;

      try
      {
        d1 = Double.parseDouble(o1.toString());
        d2 = Double.parseDouble(o2.toString());
      }
      catch(Exception ex) { throw new DataModelException("Cannot convert the input objects to primitive numbers."); }

      return(distance(null,d1,d2));
   }

   /**
    * Calculates the absolute value of the difference of the 2 values.
    * @param at The Attribute describing the structure of the values
    * @param d1 Value 1
    * @param d2 Value 2
    * @return The absolute value of the difference of the 2 values
    */
   public double distance(AttributeDouble at, double d1, double d2) throws DataModelException
   {
     return(Math.abs(d1-d2));
   }

   public Object clone() throws CloneNotSupportedException
   {
     return(new DistanceNumber());
   }

   public DistanceNumber() {}
}
