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
// *                      Number Order                      *
// **********************************************************/
/**
 * <h2>Number Order</h2>
 * The order between 2 numbers. Uses natural Java order for all Java Objects that
 * hold numbers. It parses Strings as Doubles.
 *
 * @author Johan Kaers
 * @version 2.0
 */

public class OrderNumber implements Order
{
  /**
    * The order between 2 numbers in a Java number holdind Object or String.
    * @param at The attribute describing the objects' structure
    * @param o1 Object1
    * @param o2 Object2
    * @return Their order.
    * @throws DataModelException If the objects cannot be compared.
    */
   public int order(AttributeObject at, Object o1, Object o2) throws DataModelException
   {
      int o;

      o = 0;
      if ((o1 instanceof String) && (o2 instanceof String))
      {
        try
        {
          String s1 = (String)o1;
          String s2 = (String)o2;
          double d1 = Double.parseDouble(s1);
          double d2 = Double.parseDouble(s2);
          if      (d1 <  d2) o = -1;
          else if (d1 == d2) o =  0;
          else               o =  1;
        }
        catch(NumberFormatException ex) { throw new DataModelException("Cannot parse as double one of these Strings : '"+o1+"' and '"+o2+"'"); }
      }
      else if (o1 instanceof String && o2 instanceof Number)
      {
          double d1 = Double.parseDouble((String) o1);
          double d2 = ((Number) o2).doubleValue();
          if      (d1 <  d2) o = -1;
          else if (d1 == d2) o =  0;
          else               o =  1;
      }
      else if (o1 instanceof Number && o2 instanceof String)
      {
          double d1 = ((Number) o1).doubleValue();
          double d2 = Double.parseDouble((String) o2);
          if      (d1 <  d2) o = -1;
          else if (d1 == d2) o =  0;
          else               o =  1;
      }
      else
      {
        // Just assume these Objects really hold numbers and implement comparable.
        if ((o1 instanceof Comparable) && (o2 instanceof Comparable))
        {
          Comparable c1 = (Comparable)o1;
          Comparable c2 = (Comparable)o2;
          o = c1.compareTo(c2);
        }
        else throw new DataModelException("Objects do not implement java.lang.Comparable");
      }

      return(o);
   }

   /**
    * The order between two numbers.
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
     return(new OrderNumber());
   }

   public OrderNumber()
   {
   }
}
