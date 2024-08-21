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
import org.shaman.datamodel.AttributePropertyFuzzy;
import org.shaman.datamodel.AttributePropertyFuzzyCategorical;
import org.shaman.datamodel.FMF;
import org.shaman.exceptions.DataModelException;

import junit.framework.TestCase;


/**
 * <h2>Fuzzy Attributes Test</h2>
 */
public class AttributePropertyFuzzyTest extends TestCase
{
    // **********************************************************\
    // *                   Fuzzyfication Test                   *
    // **********************************************************/
    public void testFuzzyProperties() throws DataModelException
    {
        AttributeDouble                   atdo;
        AttributePropertyFuzzyCategorical atpfuz;
        
        atdo = new AttributeDouble();
        atdo.initAsSymbolCategorical(new double[]{0.0,1.0,2.0,3.0});
        atpfuz = new AttributePropertyFuzzyCategorical(atdo, AttributePropertyFuzzyCategorical.TYPE_CRISP);
        atdo.addProperty(AttributePropertyFuzzy.PROPERTY_FUZZY, atpfuz);
        
        FMF fuz = atpfuz.getFMF(2.0);
        assertEquals(0.0, fuz.apply(3.0), 0.0);
        assertEquals(1.0, fuz.apply(2.0), 0.0);
    }
    
    // **********************************************************\
    // *                JUnit Setup and Teardown                *
    // **********************************************************/
    public AttributePropertyFuzzyTest(String name)
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
