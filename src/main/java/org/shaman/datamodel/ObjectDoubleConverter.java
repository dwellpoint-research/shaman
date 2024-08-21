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
 * <h2>Data Format Converter</h2>
 * Interface for converting between primitive and non-primitive attribute values. <br>
 * e.g. Convert Date String to '# seconds ago'
 *
 * @author Johan Kaers
 * @version 2.0
 */

// **********************************************************\
// *  Interface for converting Objects to Double and back   *
// **********************************************************/
public interface ObjectDoubleConverter 
{
  /**
    * Convert the given Object of the given AttributeObject
    * to a primitive value of the given AttributeDouble.
    * @param o  The Object
    * @param ao The AttributeObject describing the structure of the Object
    * @param ad The AttributeDouble descriving the structure of the output value.
    * @return The Object in it's primitive representation
    * @throws DataModelException If converting is not possible.
    */
   public double toDouble(Object o, AttributeObject ao, AttributeDouble ad) throws DataModelException;

   /**
    * Convert a primitive attribute value to a non-primitive one.
    * @param d The primitive value.
    * @param ad The structure of the primitive value.
    * @param ao The structure of the non-primitive value.
    * @return The corresponding non-primitive value
    * @throws DataModelException If the converting is not possible
    */
   public Object toObject(double d, AttributeDouble ad, AttributeObject ao) throws DataModelException;

   // Make a clone of the converter
   public Object clone() throws CloneNotSupportedException;
}
