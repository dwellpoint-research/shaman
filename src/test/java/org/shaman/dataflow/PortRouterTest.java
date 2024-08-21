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
package org.shaman.dataflow;

import org.shaman.TestUtils;
import org.shaman.dataflow.PortRouter;
import org.shaman.exceptions.ShamanException;

import junit.framework.TestCase;


/**
 * Test of the PortRouter Transformation
 */
public class PortRouterTest extends TestCase
{
    public void testZero() throws ShamanException
    {
        PortRouter pr;
       
        pr = new PortRouter();
        pr.grow(0);
    }
    
    public void testOne() throws ShamanException
    {
        PortRouter pr;
        Object   []obout;
        
        pr = new PortRouter();
        pr.grow(1);
        
        obout = TestUtils.transform2To1(pr, "Data", "Select");
        assertEquals(1, obout.length);
        assertEquals("Data", obout[0]);
        
        obout = TestUtils.transform2To1(pr, null, "Select");
        assertNull(obout);
    }
    
    public void testTwo()throws ShamanException
    {
        PortRouter   pr;
        Object     []obin;
        Object   [][]obout;
        
        pr = new PortRouter();
        pr.grow(2);
        
        // Output on port 0
        obin  = new Object[]{"Data", "Select1", null};
        obout = TestUtils.transformNToM(pr, obin);
        assertEquals(1,      obout[0].length);
        assertEquals("Data", obout[0][0]);
        assertNull  (        obout[1]);
        
        // Output on port 1
        obin  = new Object[]{"Data", null , "Select2"};
        obout = TestUtils.transformNToM(pr, obin);
        assertEquals(1,      obout[1].length);
        assertEquals("Data", obout[1][0]);
        assertNull  (        obout[0]);
        
        // First port with data is taken
        obin  = new Object[]{"Data", "SelectBoth", "SelectBoth"};
        obout = TestUtils.transformNToM(pr, obin);
        assertEquals(1,      obout[0].length);
        assertEquals("Data", obout[0][0]);
        assertNull  (        obout[1]);
    }
    
	// **********************************************************\
    // *                JUnit Setup and Teardown                *
    // **********************************************************/
	public PortRouterTest(String name)
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
