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
 * <h2>Property of an Attribute</h2>
 * Enforces and describes a property of the associated Attribute's data.
 * e.g. continuous data from a number of intervals.
 *      data belonging to a number of predefined categories.
 *      dates of some pre-defined format.
 */
public interface AttributeProperty
{
   /**
    *  Check if the given Object conforms with this Property's data requirements.
    *  @param at The Attribute's structure
    *  @param o The Object
    *  @return <code>true</code> if the Object is legal. <code>false</code> if not.
    *  @throws DataModelException If something is wrong with the Attribute's structure w.r.t this property.
    */
   public boolean isLegal(AttributeObject at, Object o) throws DataModelException;

   /**
    * Check if the given value conforms with this Property's data requirements.
    * @param at The Attribute describing the structure of the values
    * @param d The value
    * @return <code>true</code> if the value is legal. <code>false</code> if not.
    * @throws DataModelException If something is wrong with the Attribute's structure w.r.t this property.
    */
   public boolean isLegal(AttributeDouble at, double d) throws DataModelException;
}
