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
import org.shaman.dataflow.IdleSource;
import org.shaman.dataflow.Join;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ShamanException;
import org.shaman.exceptions.DataModelException;

import junit.framework.TestCase;


/**
 * Test of the Join Transformation
 */
public class JoinTest extends TestCase
{
    // **********************************************************\
    // *                 Large Scale Join Operations            *
    // **********************************************************/
    public void testJoinAllSizes()  throws ShamanException
    {
       Join       join;
       Object   []obin;
       Object [][]obout;
       int        k, j, i, numin;
       
       for (k=1; k<100; k++)
       {
           numin = k;
           join  = new Join();
           join.grow(numin);
       
           obin  = new Object[numin];
           for (i=0; i<numin; i++)
           {
               for (j=0; j<numin; j++) if (i==j) obin[j] = "JoinData"+i; else obin[j] = null;
               obout   = TestUtils.transformNToM(join, obin);
               assertEquals(1, obout.length);
               assertEquals(1, obout[0].length);
               assertEquals("JoinData"+i, obout[0][0]);
           }
       }
    }
    
    // **********************************************************\
    // *                   DataFlow Join Operation              *
    // **********************************************************/
    public void testZeroToZero() throws ShamanException
    {
       Join     join;
       
       join = new Join();
       join.grow(0);
    }
    
    public void testOneToOne() throws ShamanException
    {
       Join     join;
       Object   []obout;
       
       join = new Join();
       join.grow(1);
       obout = TestUtils.transform1To1(join, "Join1");
       assertEquals(1, obout.length);
       assertEquals("Join1", obout[0]);
    }
    
    public void testTwoToOne() throws ShamanException
    {
       Join     join;
       Object   []obout;
       
       join = new Join();
       join.grow(2);
       
       obout = TestUtils.transform2To1(join, "Join1", null);
       assertEquals(1, obout.length);
       assertEquals("Join1", obout[0]);
       
       obout = TestUtils.transform2To1(join, null, "Join2");
       assertEquals(1, obout.length);
       assertEquals("Join2", obout[0]);
       
       obout = TestUtils.transform2To1(join, "Join1", "Join2");
       assertEquals(2, obout.length);
       assertEquals("Join1", obout[0]);
       assertEquals("Join2", obout[1]);
    }
    
    public void testDataModelFit() throws ShamanException
    {
       IdleSource  idsrc1, idsrc2;
       Join        join;
       DataModel   dm1, dm2, dmnum;
       
       // Make 2 single String attribute datamodels. And a primitive one.
       dm1   = TestUtils.makeStringDataModel(1);
       dm2   = TestUtils.makeStringDataModel(1);
       dmnum = TestUtils.makeNumberDataModel(1, true);
       
       join   = new Join(); join.grow(2);
       idsrc1 = new IdleSource();
       idsrc2 = new IdleSource();
       idsrc1.registerConsumer(0, join, 0);
       idsrc2.registerConsumer(0, join, 1);
       join.registerSupplier(0, idsrc1, 0);
       join.registerSupplier(1, idsrc2, 0);
       idsrc1.setOutputModel(dm1);
       idsrc2.setOutputModel(dm2); 
       idsrc1.init();
       idsrc2.init();
       
       try
       {
           // Test name conflict in attributes detection.
           dm2.getAttribute(0).setName("attribute1");
           join.init();
           fail("Same attribute name check failed.");
       }
       catch(DataModelException ex) {}
       
       try
       {
           // Test data-format collision detection
           idsrc2.setOutputModel(dmnum);
           idsrc2.init();
           join.init();
           fail("Data-format collision detection failed.");
       }
       catch(DataModelException ex) {}
       
       // Set Joinable DataModel
       try
       {
         dm2.getAttribute(0).setName("attribute0");
         idsrc2.setOutputModel(dm2);
         idsrc2.init();
         join.init();
       }
       catch(DataModelException ex)
       {
           fail("Data-model fit failed.");
       }
    }
    
	// **********************************************************\
    // *                JUnit Setup and Teardown                *
    // **********************************************************/
	public JoinTest(String name)
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
