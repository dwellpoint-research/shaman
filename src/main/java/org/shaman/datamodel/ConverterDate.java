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
 * <h2>Date Converter</h2>
 * Converter for time data. Can handle all kinds of time containing classes.
 * Look at the attribute-property for dates to get an overview.
 */
public class ConverterDate implements ObjectDoubleConverter
{
    // Ways to convert a date to a double and back.
    /** Convert to doubles by calculating the absolute time (as in Date.getTime()). */
    public static final int TO_DOUBLE_ABSOLUTE = 0;  // Convert to absolute time value (ms after ...)
    /** Convert to doubles by calculating the time difference from the current moment. */
    public static final int TO_DOUBLE_AGO      = 1;  // Use difference in time from now.
    /** Convert to an absolute time double to an Object */
    public static final int TO_OBJECT_ABSOLUTE = 0;  // Convert to absolute time value.
    /** Add the current moment's timevalue to the double before converting it to an Object. */
    public static final int TO_OBJECT_AGO      = 1;  // Add 'now' to input before converting.
    
    private int toDouble;
    private int toObject;
    
    // **********************************************************\
    // *            Time Data Object to Double Converter        *
    // **********************************************************/
    /**
     * Convert the given time object to a double containing the time information in the desired format.
     * @param ob The object
     * @param ao The attribute describing the Object's structure
     * @param ad The attribute describing the returned value's structure.
     * @return The double corresponding to the given object
     * @throws DataModelException If the given Object is illegal or cannot be converted to double.
     */
    public double toDouble(Object ob, AttributeObject ao, AttributeDouble ad) throws DataModelException
    {
        double                val;
        AttributePropertyDate apdate;
        
        val = 0;
        
        if (ao.hasProperty(AttributePropertyDate.PROPERTY_DATE) && ad.hasProperty(AttributePropertyDate.PROPERTY_DATE))
        {
            if(ao.checkType(ob))
            {
                if (!ao.isMissingValue(ob))
                {
                    if (ao.isLegal(ob))
                    {
                        apdate = (AttributePropertyDate)ao.getProperty(AttributePropertyDate.PROPERTY_DATE);
                        val    = objectDateToDouble(apdate, ob);
                    }
                    else if (ao.getIllegalIsMissing()) val = ad.getMissingAsDouble();
                    else throw new DataModelException("Illegal date object "+ob+" found in Attribute '"+ao.getName()+"'. Cannot treat it as missing.");
                }
                else val = ad.getMissingAsDouble();
            }
            else throw new DataModelException("Date Object has wrong type. "+ob.getClass().getName()+". Attribute name "+ao.getName());
        }
        else throw new DataModelException("One or both attributes do not have to date property "+AttributePropertyDate.PROPERTY_DATE);
        
        return(val);
    }
    
    /**
     * Convert the given date double to a date Object.
     * @param d The double
     * @param ad The attribute describing the double's structure
     * @param ao The attribute describing the returned Object's structure.
     * @throws DataModelException If the double is illegal or if it cannot be converted to an Object.
     */
    public Object toObject(double d, AttributeDouble ad, AttributeObject ao) throws DataModelException
    {
        Object                val;
        AttributePropertyDate apdate;
        
        val = null;
        if (ao.hasProperty(AttributePropertyDate.PROPERTY_DATE) && ad.hasProperty(AttributePropertyDate.PROPERTY_DATE))
        {
            if (!ad.isMissingValue(d))
            {
                if (ad.isLegal(d))
                {
                    apdate = (AttributePropertyDate)ao.getProperty(AttributePropertyDate.PROPERTY_DATE);
                    val    = doubleDateToObject(apdate, ao, d);
                }
                else if (ad.getIllegalIsMissing()) val = ao.getMissingAsObject();
                else throw new DataModelException("Illegal date value "+d+" found in Attribute '"+ad.getName()+"'. Cannot treat it as missing.");
            }
            else val = ao.getMissingAsObject();
        }
        else throw new DataModelException("One or both attributes do not have to date property "+AttributePropertyDate.PROPERTY_DATE);
        
        return(val);
    }
    
    private Object doubleDateToObject(AttributePropertyDate apdate, AttributeObject ao, double d) throws DataModelException
    {
        Object ob;
        String obtype;
        long   t;
        
        // Make the currect raw time value to convert to Object.
        if     (toObject == TO_OBJECT_ABSOLUTE) t = (long)d;
        else if(toObject == TO_OBJECT_AGO)      t = System.currentTimeMillis() - ((long)d);
        else throw new DataModelException("Unknown TO_OBJECT convertion type");
        
        // Convert to the appropriate object.
        obtype = ao.getRawType();
        if      (obtype.equals("java.util.Date"))     ob = new java.util.Date(t);
        else if (obtype.equals("java.sql.Date"))      ob = new java.sql.Date(t);
        else if (obtype.equals("java.sql.Timestamp")) ob = new java.sql.Timestamp(t);
        else if (obtype.equals("java.util.Calendar"))
        {
            java.util.GregorianCalendar gc = new java.util.GregorianCalendar();
            gc.setTime(new java.util.Date(t));
            ob = gc;
        }
        else if (obtype.equals("java.lang.Long")) ob = new Long(t);
        else if (obtype.equals("java.lang.String"))
        {
            SimpleDateFormat df = apdate.getDateFormat();
            String ds = df.format(new java.util.Date(t));
            ob = ds;
        }
        else throw new DataModelException("Cannot convert double Date Value to class "+obtype);
        
        return(ob);
    }
    
    private double objectDateToDouble(AttributePropertyDate apdate, Object ob) throws DataModelException
    {
        SimpleDateFormat dateformat = apdate.getDateFormat();
        long    t;
        double dt;
        
        // Make the raw time value from the time containing Object
        if      (ob instanceof java.util.Date)     t = ((java.util.Date)ob).getTime();
        else if (ob instanceof java.sql.Date)      t = ((java.sql.Date)ob).getTime();
        else if (ob instanceof java.sql.Timestamp) t = ((java.sql.Timestamp)ob).getTime();
        else if (ob instanceof java.util.Calendar) t = ((java.util.Calendar)ob).getTime().getTime();
        else if (ob instanceof java.lang.Long)     t = ((java.lang.Long)ob).longValue();
        else if (ob instanceof java.lang.String)
        {
            try
            {
                t = dateformat.parse((String)ob).getTime();
            }
            catch(java.text.ParseException ex) { throw new DataModelException(ex); }
        }
        else throw new DataModelException("Cannot convert date Object of class "+ob.getClass().getName()+" to double.");
        
        // Check format
        if      (toDouble == TO_DOUBLE_AGO)     t = System.currentTimeMillis() - t; // # milliseconds ago
        else if (toDouble == TO_DOUBLE_ABSOLUTE) ; // Absolute time.
        else throw new DataModelException("Unknown TO_DOUBLE convertion type.");
        
        dt = (double)(t);
        
        return(dt);
    }
    
    // **********************************************************\
    // *                         Cloneing                       *
    // **********************************************************/
    public Object clone() throws CloneNotSupportedException
    {
        ConverterDate cdn = new ConverterDate(toDouble, toObject);
        
        return(cdn);
    }
    
    /**
     * Make a converter that handles the conversion in the given manner
     * @param _toDouble How to convert from Object to double. (TO_DOUBLE_x)
     * @param _toObject How to convert from double to Object. (TO_OBJECT_x)
     */
    public ConverterDate(int _toDouble, int _toObject)
    {
        toDouble = _toDouble;
        toObject = _toObject;
    }
}
