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

import java.text.SimpleDateFormat;

import org.shaman.exceptions.DataModelException;


// **********************************************************\
// *                     Date Order                         *
// **********************************************************/
/**
 * <h2>Date Order</h2>
 * The order between dates. Use the Data Property's settings to
 * correctly order the dates.
 *
 * @author Johan Kaers
 * @version 2.0
 */

public class OrderDate implements Order
{
    /**
     * The order between 2 dates.
     * @param at The attribute describing the objects' structure
     * @param o1 Object1
     * @param o2 Object2
     * @return Their order.
     * @throws DataModelException If the objects cannot be compared.
     */
    public int order(AttributeObject at, Object o1, Object o2) throws DataModelException
    {
        int                   o;
        String                datcl;
        SimpleDateFormat      dateformat;
        long                  t1, t2;
        AttributePropertyDate apdate;
        
        t1 = 0; t2 = 0;
        if (at.hasProperty(AttributePropertyDate.PROPERTY_DATE))
        {
            apdate     = (AttributePropertyDate)at.getProperty(AttributePropertyDate.PROPERTY_DATE);
            datcl      = at.getRawType();
            dateformat = apdate.getDateFormat();
            
            o = 0;
            if (datcl.equals("java.lang.String"))
            {
                try
                {
                    String s1 = (String)o1;
                    String s2 = (String)o2;
                    t1 = dateformat.parse(s1).getTime();
                    t2 = dateformat.parse(s2).getTime();
                    if      (t1 <  t2) o = -1;
                    else if (t1 == t2) o =  0;
                    else               o =  1;
                }
                catch(java.text.ParseException ex) { throw new DataModelException("Cannot parse as date one of these Strings : '"+o1+"' and '"+o2+"'"); }
            }
            else
            {
                // Just assume these Objects hold some kind of Date and implement comparable.
                if ((o1 instanceof Comparable) && (o2 instanceof Comparable))
                {
                    Comparable c1 = (Comparable)o1;
                    Comparable c2 = (Comparable)o2;
                    o = c1.compareTo(c2);
                }
                else throw new DataModelException("Date Objects do not implement java.lang.Comparable");
            }
        }
        else throw new DataModelException("The Attribute of the given objects does not have the AttributeProperty of dates.");
        
        return(o);
    }
    
    /**
     * The order between two dates.
     * @param at The attribute describing the values' structure
     * @param d1 Value1
     * @param d2 Value2
     * @return 0 if d1==d2, -1 if d1 smaller d2, 1 if d1 greater d2
     */
    public int order(AttributeDouble at, double d1, double d2) throws DataModelException
    {
        if (at.hasProperty(AttributePropertyDate.PROPERTY_DATE))
        {
            if      (d1  < d2) return(-1);
            else if (d1 == d2) return(0);
            else return(1);
        }
        else throw new DataModelException("Date values do not have the AttributeProperty of Dates.");
    }
    
    // **********************************************************\
    // *       Cloning and Construction Implementation          *
    // **********************************************************/
    public Object clone() throws CloneNotSupportedException
    {
        return(new OrderDate());
    }
    
    public OrderDate()
    {
    }
}
