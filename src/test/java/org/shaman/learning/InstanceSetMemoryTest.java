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
package org.shaman.learning;

import java.util.LinkedList;
import java.util.List;

import org.shaman.TestUtils;
import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.InstanceSetMemory;

import junit.framework.TestCase;
import cern.colt.matrix.DoubleMatrix1D;


/**
 * Test of the CachingPresenter from Memory
 */
public class InstanceSetMemoryTest extends TestCase
{
    // **********************************************************\
    // *           Diagnostics Transformation Network           *
    // **********************************************************/
    public void testCreateFromListWithGoal() throws LearnerException, DataModelException
    {
        InstanceSetMemory im;
        DataModelDouble   dmtest;
        List              datvec;
        AttributeDouble   atdo;
        DoubleMatrix1D    vecdo;
        int               i;
        double            wei, goal;
        
        // Create test datamodel
        dmtest = (DataModelDouble)TestUtils.makeNumberDataModel(3, true);
        atdo   = dmtest.getAttributeDouble(0);
        atdo.setIsActive(false);
        atdo   = dmtest.getAttributeDouble(2);
        atdo.setName("goal");
        atdo.setIsActive(false);
        atdo.setMissingValues(new double[]{-1.0});
        atdo.setAsGoal();
        dmtest.getLearningProperty().setGoal("goal");
        
        // Create test data. One active, one inactive and one continuous goal attribute.
        datvec = new LinkedList();
        for (i=0; i<100; i++)
        {
            vecdo = (DoubleMatrix1D)dmtest.createDefaultVector();
            vecdo.setQuick(0, i);
            vecdo.setQuick(1, i+100.0);
            
            goal  = ((double)i)/100.0;
            if (i>=90) vecdo.setQuick(2, -1.0);
            else       vecdo.setQuick(2, goal);
            
            datvec.add(vecdo);
        }
        
        // Create the InstanceSet
        im = new InstanceSetMemory();
        im.create(datvec, dmtest);
        
        // Check if the instance data is correct
        assertEquals(im.getNumberOfInstances(), 90);
        for (i=0; i<90; i++)
        {
            vecdo = im.getInstance(i);
            assertEquals(1,       vecdo.size());
            assertTrue((i+100.0) == vecdo.getQuick(0));
            goal = im.getGoal(i);
            wei  = im.getWeight(i);
            assertTrue((((double)i)/100.0 ) == goal);
            assertTrue(1.00 == wei);
        }
    }

	// **********************************************************\
    // *                JUnit Setup and Teardown                *
    // **********************************************************/
	public InstanceSetMemoryTest(String name)
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
