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
 * <h2>Distance Between 2 Value</h2>
 * Interface to calculate the distance between 2 attribute values. <br>
 * e.g. Trigram matching between 2 String, Travel Time between 2 addresses.
 *
 * @author Johan Kaers
 * @version 2.0
 */

public interface Distance extends Cloneable
{
   /**
    * Calculates the distance between the 2 given Objects according to some distance metric.
    * This relation is symmetric, 0 if <code>o1.equals(o2)</code>
    * @param at The Attribute describing the Objects internal structure
    * @param o1 Object 1
    * @param o2 Object 2
    * @return The distance between the two objects.
    * @throws DataModelException if something is wrong with the structure of the Objects, preventing the calculating of the distance.
    */
   public double distance(AttributeObject at, Object o1, Object o2) throws DataModelException;

   /**
    * Calculates the distance between the 2 given doubles according to some distance metric.
    * This relation is symmetric, 0 if <code>d1 == d2</code>
    * @param at The Attribute describing the double's internal structure
    * @param d1 value 1
    * @param d2 value 2
    * @return The distance between the two double values.
    * @throws DataModelException if something is wrong with the values, that prevents calculating the distance.
    */
   public double distance(AttributeDouble at, double d1, double d2) throws DataModelException;

   // Make a clone of the order
   public Object clone() throws CloneNotSupportedException;
}
