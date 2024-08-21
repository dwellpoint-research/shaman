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

// **********************************************************\
// *                    Natural Java Order                  *
// **********************************************************/

/**
 * <h2>Default Order Implementation</h2>
 * Uses the natural Java order for Object based data, the '>' operator for primitive data.
 *
 * @author Johan Kaers
 * @version 2.0
 */

public class OrderNatural implements Order
{
  /**
    * The natural order of the objects if they implement <code>Comparable</code>.
    * @param at The attribute describing the objects' structure
    * @param o1 Object1
    * @param o2 Object2
    * @return Their natural order.
    * @throws DataModelException If the object are not <code>Comparable</code>
    */
   public int order(AttributeObject at, Object o1, Object o2) throws DataModelException
   {
      int o;
      Comparable c1, c2;

      o = 0;
      if ((o1 instanceof Comparable) && (o2 instanceof Comparable))
      {
        c1 = (Comparable)o1;
        c2 = (Comparable)o2;
        o = c1.compareTo(c2);
      }
      else throw new DataModelException("Objects do not implement java.lang.Comparable");

      return(o);
   }

   /**
    * The natural order between two numbers.
    * @param at The attribute describing the values' structure
    * @param d1 Value1
    * @param d2 Value2
    * @return 0 if d1==d2, -1 if d1 smaller d2, 1 if d1 greater d2
    */
   public int order(AttributeDouble at, double d1, double d2)
   {
     if      (d1  < d2) return(-1);
     else if (d1 == d2) return(0);
     else return(1);
   }

   // **********************************************************\
   // *       Cloning and Construction Implementation          *
   // **********************************************************/
   public Object clone() throws CloneNotSupportedException
   {
     return(new OrderNatural());
   }

   public OrderNatural()
   {
   }
}
