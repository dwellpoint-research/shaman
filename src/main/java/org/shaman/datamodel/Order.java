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


/**********************************************************\
 *   Interface for complete order between 2 data-values   *
\**********************************************************/

/**
 * <h2>Complete Order Relation</h2>
 * Interface for a complete order relation on an attribute. <br>
 * e.g. Medals : Bronze < Silver < Gold, Time Order : 20 sep 1999 < 20 dec 2001
 *
 * @author Johan Kaers
 * @version 2.0
 */

public interface Order extends Cloneable
{
   /**
    * A complete order relation between non-primitive attributes.
    * @param at The attribute describing the objects' structure
    * @param o1 Object 1
    * @param o2 Object 2
    * @return 0 if o1==o2, -1 if o1 is smaller then o2, 1 if o2 is greather then o1 according to the order relation
    * @throws DataModelException If the comparison is not possible
    */
   public int order(AttributeObject at, Object o1, Object o2) throws DataModelException;

   /**
    * A complete order relation between primitive attributes.
    * @param at The attribute describing the objects' structure
    * @param d1 Value 1
    * @param d2 Value 2
    * @return 0 if d1==d2, -1 if d1 is smaller then d2, 1 if d2 is greather then d1 according to the order relation
    * @throws DataModelException If the comparison is not possible
    */
   public int order(AttributeDouble at, double d1, double d2) throws DataModelException;

   // Make a clone of the order
   public Object clone() throws CloneNotSupportedException;
}
