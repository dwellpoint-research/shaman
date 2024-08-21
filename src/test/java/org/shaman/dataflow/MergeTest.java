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
import org.shaman.dataflow.Merge;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ShamanException;
import org.shaman.exceptions.DataModelException;

import junit.framework.TestCase;
import cern.colt.matrix.ObjectFactory1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * Test of the Merge Transformation
 */
public class MergeTest extends TestCase
{
    // **********************************************************\
    // *         Transformation Network Component Tests         *
    // **********************************************************/
    public void testZeroToZero() throws ShamanException
    {
       Merge     merge;
       
       merge = new Merge();
       merge.grow(0);
    }
    
    public void testOneToOne() throws ShamanException
    {
       Merge      merge;
       Object   []obout;
       
       merge = new Merge();
       merge.grow(1);
       obout = TestUtils.transform1To1(merge, "Merge1");
       assertEquals(1, obout.length);
       assertEquals("Merge1", obout[0]);
    }
    
    public void testTwoToOne() throws ShamanException
    {
       IdleSource       idsrc1, idsrc2;
       Merge            merge;
       Object         []obout;
       ObjectMatrix1D   vec1, vec2, vecout;
       DataModel        dm1, dm2, dmnum;
       
       // Make 2 single String attribute datamodels. And a primitive one.
       dm1   = TestUtils.makeStringDataModel(1);
       dm2   = TestUtils.makeStringDataModel(1);
       dmnum = TestUtils.makeNumberDataModel(1, true);
       
       merge  = new Merge(); merge.grow(2);
       idsrc1 = new IdleSource();
       idsrc2 = new IdleSource();
       idsrc1.registerConsumer(0, merge, 0);
       idsrc2.registerConsumer(0, merge, 1);
       merge.registerSupplier(0, idsrc1, 0);
       merge.registerSupplier(1, idsrc2, 0);
       idsrc1.setOutputModel(dm1);
       idsrc2.setOutputModel(dm2); 
       idsrc1.init();
       idsrc2.init();
       
       try
       {
           // Test duplicate attribute name detection.
           merge.init();
           fail("Duplicate attribute name detection failed.");
       }
       catch(DataModelException ex) {}
       
       try
       {
           // Test data-format collision detection
           idsrc2.setOutputModel(dmnum);
           idsrc2.init();
           merge.init();
           fail("Data-format collision detection failed.");
       }
       catch(DataModelException ex) {}
       
       // Set mergable datamodel
       dm2.getAttribute(0).setName("attribute1");
       idsrc2.setOutputModel(dm2);
       idsrc2.init();
       merge.init();
       
       // Merge 2 vectors
       vec1 = ObjectFactory1D.dense.make(1);
       vec2 = ObjectFactory1D.dense.make(1);
       vec1.setQuick(0, "Merge1");
       vec2.setQuick(0, "Merge2");
       
       obout = TestUtils.transform2To1(merge, vec1, null);
       assertNull(obout);
       
       obout = TestUtils.transform2To1(merge, null, vec2);
       assertNull(obout);
       
       obout = TestUtils.transform2To1(merge, vec1, vec2);
       assertEquals(1, obout.length);
       vecout = (ObjectMatrix1D)obout[0];
       assertEquals(2, vecout.size());
       assertEquals("Merge1", vecout.getQuick(0));
       assertEquals("Merge2", vecout.getQuick(1));
    }
    
	// **********************************************************\
    // *                JUnit Setup and Teardown                *
    // **********************************************************/
	public MergeTest(String name)
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
