/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                    Data Modeling                      *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2005 Shaman Research                   *
\*********************************************************/
package org.shaman.datamodel;

import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.AttributeObject;
import org.shaman.datamodel.AttributePropertyDate;
import org.shaman.datamodel.DistanceDate;
import org.shaman.datamodel.DistanceTrigram;
import org.shaman.datamodel.OrderDate;
import org.shaman.exceptions.DataModelException;

import junit.framework.TestCase;


/**
 * <h2>Symbol Attributes Test</h2>
 */
public class SymbolicAttributesTest extends TestCase
{
    // **********************************************************\
    // *             Test Symbolic Date Attribute               *
    // **********************************************************/
    public void testDateProperty() throws DataModelException
    {
        String datename       = "java.lang.String";
        String pattern        = "dd MMM yyyy";
        String localeCountry  = "be";
        String localeLanguage = "nl";
        
        AttributeObject       atobdate;
        AttributeDouble       atdodate;
        DistanceDate          disdate;
        OrderDate             orddate;
        AttributePropertyDate apdate;
        double                d;
        int                   o;
        
        // Install a Dutch Belgian Date Parser.... Leuven Vlaams!
        apdate   = new AttributePropertyDate(pattern, localeCountry, localeLanguage);
        disdate  = new DistanceDate();
        orddate  = new OrderDate();
        atobdate = new AttributeObject();
        atdodate = new AttributeDouble();
        atobdate.initAsFreeObject(datename);
        atdodate.initAsFreeNumber();
        atobdate.addProperty(AttributePropertyDate.PROPERTY_DATE, apdate);
        atdodate.addProperty(AttributePropertyDate.PROPERTY_DATE, apdate);
        atobdate.setDistance(disdate);
        atdodate.setDistance(disdate);
        atobdate.setOrder(orddate);
        atdodate.setOrder(orddate);
        
        // Test time distance calculation on Dutch Time Strings
        String date1 = "30 januari 2001";
        String date2 = "2 februari 2001";       
        d = atobdate.distance(date1, date2);
        assertEquals(259200.0, (d/1000), 0);     // 3 Days in Seconds.
        d = atdodate.distance(System.currentTimeMillis(), System.currentTimeMillis()+100);
        assertEquals(100, d, 0);                 // 100 ms difference...
        
        // Test time order on Dutch Time Strings
        o = atobdate.order("20 januari 2003", "10 december 2001");
        assertEquals(1, o);
        o = atobdate.order("10 december 2001", "20 januari 2003");
        assertEquals(-1, o);
    }
    
    // **********************************************************\
    // *           Some light trigram distance testing          *
    // **********************************************************/
    public void testTrigramDistance() throws DataModelException
    {
        // Test The Trigram Matching.
        DistanceTrigram distri;
        String          tris1 = "Ambachtenlaen";
        String          tris2 = "Ambachtenlaan";
        double          d;
        
        distri = new DistanceTrigram();
        d      = distri.distance(null, tris1, tris2);
        assertEquals(0.6666666, d, 0.04);
    }
    
    // **********************************************************\
    // *                JUnit Setup and Teardown                *
    // **********************************************************/
    public SymbolicAttributesTest(String name)
    {
        super(name);
    }
    
    protected void setUp() throws Exception
    {
        super.setUp();
    }
    
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
}
