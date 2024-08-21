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


/**
 * <h2>Distance in Time</h2>
 * The distance between 2 dates. <br>
 *
 * @author Johan Kaers
 * @version 2.0
 */
public class DistanceDate implements Distance
{
    /** Number of milliseconds in 1 second */
    public static final long SECOND = 1000;
    /** Number of milliseconds in 1 minute  */
    public static final long MINUTE = SECOND*60;
    /** Number of milliseconds in 1 hour */
    public static final long HOUR   = MINUTE*60;
    /** Number of milliseconds in 1 day */
    public static final long DAY    = HOUR*24;
    /** Number of milliseconds in 1 week */
    public static final long WEEK   = DAY*7;   
    /** Number of milliseconds in 1 month. Actually in 1 period of 4 weeks. Not really a month... */
    public static final long MONTH  = DAY*28;    // Actually 4 weeks...
    /** Number of milliseconds in 1 year */
    public static final long YEAR      = DAY*365;
    /** Number of milliseconds in 1 leap year */
    public static final long LEAP_YEAR = DAY*366;
    
    
    /**
     *  The distance between 2 Dates expressed in milliseconds.
     *  Works for attributes with the <code>PROPERTY_DATE</code>
     *  @param at The Attribute's structure
     *  @param o1 Time Object 1
     *  @param o2 Time Object 2
     *  @return The time distance in milliseconds.
     */
    public double distance(AttributeObject at, Object o1, Object o2) throws DataModelException
    {
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
            if (datcl.equals("java.util.Date"))
            {
                t1 = ((java.util.Date)o1).getTime();
                t2 = ((java.util.Date)o2).getTime();
            }
            else if (datcl.equals("java.sql.Date"))
            {
                t1 = ((java.sql.Date)o1).getTime();
                t2 = ((java.sql.Date)o2).getTime();
            }
            else if (datcl.equals("java.sql.Timestamp"))
            {
                t1 = ((java.sql.Timestamp)o1).getTime();
                t2 = ((java.sql.Timestamp)o2).getTime();
            }
            else if (datcl.equals("java.util.Calendar"))
            {
                t1 = ((java.util.Calendar)o1).getTime().getTime();
                t2 = ((java.util.Calendar)o2).getTime().getTime();
            }
            else if (datcl.equals("java.lang.Long"))
            {
                t1 = ((java.lang.Long)o1).longValue();
                t2 = ((java.lang.Long)o2).longValue();
            }
            else if (datcl.equals("java.lang.String"))
            {
                try
                {
                    t1 = dateformat.parse((String)o1).getTime();
                    t2 = dateformat.parse((String)o2).getTime();
                }
                catch(java.text.ParseException ex) { throw new DataModelException(ex); }
            }
            else throw new DataModelException("Cannot calculate the distance in time between 2 object of the class "+datcl);
        }
        
        return(Math.abs(t2-t1));
    }
    
    /**
     * The distance between 2 Dates expressed in milliseconds.
     * @param at The Attribute describing the structure of the values
     * @param d1 Date 1 in milliseconds
     * @param d2 Date 2 in milliseconfs
     * @return The absolute value of the difference of the 2 values
     */
    public double distance(AttributeDouble at, double d1, double d2) throws DataModelException
    {
        if (at.hasProperty(AttributePropertyDate.PROPERTY_DATE)) return(Math.abs(d1-d2));
        else throw new DataModelException("Input attribute is not time data. Propery '"+AttributePropertyDate.PROPERTY_DATE+"' not found.");
    }
    
    public Object clone() throws CloneNotSupportedException
    {
        return(new DistanceDate());
    }
}
