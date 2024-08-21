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
import org.shaman.dataflow.Identity;
import org.shaman.exceptions.ShamanException;

import junit.framework.TestCase;



/**
 * Test of the Identity Transformation
 */
public class IdentityTest extends TestCase
{
    // **********************************************************\
    // *         Transformation Network Component Tests         *
    // **********************************************************/
    public void testZeroToZero() throws ShamanException
    {
       Identity   id;
       
       id = new Identity();
       id.grow(0);
    }
    
    public void testOneToOne() throws ShamanException
    {
       Identity   id;
       Object   []obout;
       
       id = new Identity();
       id.grow(1);
       obout = TestUtils.transform1To1(id, "Identity");
       assertEquals(1, obout.length);
       assertEquals("Identity", obout[0]);
    }
    
    public void testTwoToTwo() throws ShamanException
    {
       Identity     id;
       Object   [][]out;
       
       id  = new Identity();
       id.grow(2);
       
       out = TestUtils.transform2To2(id, null, null);
       assertNull(out[0]);
       assertNull(out[1]);
       
       out = TestUtils.transform2To2(id, "Identity1", null);
       assertEquals(1, out[0].length);
       assertEquals("Identity1", out[0][0]);
       assertNull(out[1]);
       
       out = TestUtils.transform2To2(id, null, "Identity2");
       assertNull(out[0]);
       assertEquals(1, out[1].length);
       assertEquals("Identity2", out[1][0]);
       
       out = TestUtils.transform2To2(id, "Identity1", "Identity2");
       assertEquals(1, out[0].length);
       assertEquals("Identity1", out[0][0]);
       assertEquals(1, out[1].length);
       assertEquals("Identity2", out[1][0]);
    }

	// **********************************************************\
    // *                JUnit Setup and Teardown                *
    // **********************************************************/
	public IdentityTest(String name)
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
