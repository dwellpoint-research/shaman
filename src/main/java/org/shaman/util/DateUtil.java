/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                   Utility Methods                     *
 *                                                       *
 *  January 2005                                         *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2005 Shaman Research                   *
\*********************************************************/
package org.shaman.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * A util class for transforming dates to strings and strings to date.
 */
public class DateUtil
{    
    /**
     * Formats a date following the given format.
     *
     * @param date                the date to format
     * @param format              the format
     * @return                    the string representation
     * @exception ParseException
     */
    public static String formatDate(Date date, String format)
    {
        SimpleDateFormat df = new SimpleDateFormat(format);
        return df.format(date);
    }
    
    /**
     * Translates a string to a date object, following the given format.
     *
     * @param text                          the string to translate
     * @param format              the format
     * @return                              the transformed date
     * @exception java.text.ParseException
     */
    public static Date parseDate(String text, String format) throws java.text.ParseException
    {
        if (text == null) return null;
        SimpleDateFormat df = new SimpleDateFormat(format);
        Date date = df.parse(text);
        return date;
    }
    
    public static Date getPreviousDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_YEAR, -1);
        return cal.getTime();
    }
    
    public static Date getNextDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_YEAR, 1);
        return cal.getTime();
    }
}
