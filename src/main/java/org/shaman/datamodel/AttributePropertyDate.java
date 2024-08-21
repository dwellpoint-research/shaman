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
import java.util.Date;
import java.util.Locale;

import org.shaman.exceptions.DataModelException;


/**
 * <h2>Date Data Property</h2>
 * Requires that all data represents some kind of date.
 * The Attribute that has this property contains time information.
 * As <code>java.util.Date, java.sql.Date, java.util.Calendar, java.lang.Long</code>
 * or as a date String parsable by <code>DateFormat</code> for Object based data.
 * As <code>long</code> containing time information for double based data.
 */
public class AttributePropertyDate implements AttributeProperty
{
    /** The name (key) of this property in an Attribute's property map. */
    public static final String PROPERTY_DATE = "date";
    
    // Locale and Data Format specification
    private String dateFormatPattern;   // SimpleDateFormat pattern String.
    private String localeCountry;       // ISO3166 Country Code
    private String localeLanguage;      // ISO639 Language Code
    
    // The Locale and Data Format Objects (made by constructor)
    private Locale           locale;
    private SimpleDateFormat dateformat;
    
    /**
     *  Check if the given Object is a date conforming to the specified structure.
     *  For Objects other than Strings, no checking is done.
     *  For Strings, the given date pattern, and locale (country, language) should fit.
     *  @param at The Attribute's structure
     *  @param o The Object
     *  @return <code>true</code> if the Object is legal. <code>false</code> if not.
     *  @throws DataModelException If something is wrong with the Attribute's structure w.r.t this property.
     */
    public boolean isLegal(AttributeObject at, Object o) throws DataModelException
    {
        boolean legal;
        
        // Assume all is fine for Java objects that contain time information.
        legal = true;
        if (o instanceof String)
        {
            // Only check Strings by trying to parse them with the SimpleDataFormat.
            if (parseDate((String)o) == null) { legal = false; }
        }
        
        return(legal);
    }
    
    /**
     * Parse the given String with the customized date-format
     * @param o The date String to parse.
     * @return A Date object parsed according to the date-format.
     */
    public Date parseDate(String o)
    {
        Date dato = null;
        try
        {
            dato = this.dateformat.parse(o);
        }
        catch(java.text.ParseException ex) { dato = null; }
        
        return(dato);
    }
    
    /**
     * Check if the given date value is legal.
     * @param at The Attribute describing the structure of the values
     * @param d The value
     * @return <code>true</code>. Because can't really tell...
     * @throws DataModelException Never.
     */
    public boolean isLegal(AttributeDouble at, double d) throws DataModelException
    {
        // Can't really tell. Assume all is fine.
        
        return(true);
    }
    
    /**
     * Return the date format used to parse String dates.
     * Used by the distance and order relations for dates.
     * @return The SimpleDateFormat used for parsing data Strings.
     */
    public SimpleDateFormat getDateFormat()    { return(dateformat); }
    
    // **********************************************************\
    // *               Date Property Construction               *
    // **********************************************************/
    /**
     * Create a default date property. The default Locale and DateFormat Pattern will be used.
     */
    public AttributePropertyDate()
    {
        locale     = Locale.getDefault();
        dateformat = new SimpleDateFormat();
    }
    
    /**
     * Create date property that uses (parses and outputs) Strings with the default Locale
     * and the given Date Format Pattern.
     * @param _dateFormatPattern The pattern describing the date format.
     * @see java.text.SimpleDateFormat
     */
    public AttributePropertyDate(String _dateFormatPattern)
    {
        dateFormatPattern = _dateFormatPattern;
        locale            = Locale.getDefault();
        if (dateFormatPattern == null) dateformat = new SimpleDateFormat();
        else                           dateformat = new SimpleDateFormat(dateFormatPattern, locale);
    }
    
    /**
     * Create date property that uses the Locale of the given country/language and
     * the given Data Format Pattern to parse date Strings.
     * @param _dateFormatPattern The pattern describing the date format.
     * @param _localeCountry ISO3166 Country Code
     * @param _localeLanguage  ISO 639 Language Code
     * @see java.text.SimpleDateFormat
     * @see java.util.Locale
     */
    public AttributePropertyDate(String _dateFormatPattern, String _localeCountry, String _localeLanguage)
    {
        dateFormatPattern = _dateFormatPattern;
        localeCountry     = _localeCountry;
        localeLanguage    = _localeLanguage;
        
        // Make the wanted Locale
        if ((localeCountry == null) || (localeLanguage == null)) locale = Locale.getDefault();
        else
        {
            locale = new Locale(localeLanguage, localeCountry);
        }
        // Make the SimpleDateFormat
        if (dateFormatPattern == null) dateformat = new SimpleDateFormat();
        else                           dateformat = new SimpleDateFormat(dateFormatPattern, locale);
    }
}
