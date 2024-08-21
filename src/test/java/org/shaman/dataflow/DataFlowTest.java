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

import java.util.List;

import org.shaman.TestUtils;
import org.shaman.dataflow.Block;
import org.shaman.dataflow.Identity;
import org.shaman.dataflow.VectorSource;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.exceptions.ShamanException;
import org.shaman.exceptions.ConfigException;

import junit.framework.TestCase;
import cern.colt.matrix.DoubleMatrix1D;
import cern.jet.random.Uniform;


/**
 * <h2>DataFlow Test Case</h2>
 */
public class DataFlowTest extends TestCase
{
    // **********************************************************\
    // *                Test Basic Data Flow                    *
    // **********************************************************/
    public void testBasicDataFlow() throws ShamanException
    {
        // First build a simple   Source -> Identity -> Block    setup
        VectorSource vsrc  = new VectorSource();
        Identity     id    = new Identity(); id.grow(1);
        Block        block = new Block();
        id.connect(0, vsrc, 0);
        block.connect(0, id, 0);
        DataModelDouble dmout = (DataModelDouble)TestUtils.makeNumberDataModel(10, true);
        block.setCollect(true);
        vsrc.setDataModel(dmout);
        vsrc.setFit(VectorSource.FIT_NORMAL);        
        vsrc.init();
        id.init();
        block.init();
        
        // Output Vectors in Vector Source. Check if they arrived in Block unmodified.
        int            i,j;
        DoubleMatrix1D vec, vecout;
        List           bdat;
        for (i=0; i<10; i++)
        {
            vec = (DoubleMatrix1D)dmout.createDefaultVector();
            for (j=0; j<vec.size(); j++) vec.setQuick(j, Uniform.staticNextDouble());
            
            vsrc.outputVector(vec);
            bdat = block.getBlockedData();
            assertNotNull(bdat);
            assertEquals(1, bdat.size());
            vecout = (DoubleMatrix1D)bdat.iterator().next();
            for (j=0; j<vec.size(); j++) assertEquals(vec.getQuick(j), vecout.getQuick(j), 0);
            
            block.clearBlockedData();
        }
    }
    
    // **********************************************************\
    // *             Test Building of Neighborhood              *
    // **********************************************************/
    public void testNeighbourhood() throws ShamanException
    {
        Identity     iddum = new Identity(); iddum.grow(1);
        VectorSource vsrc  = new VectorSource();
        Identity     id    = new Identity(); id.grow(1);
        Block        block = new Block();
        
        try
        {
            // Vector Source has 0 input ports.
            vsrc.registerSupplier(0, iddum, 0);
            fail("Registered supplier at an input port that is out of bounds.");
        }
        catch(ConfigException ex) {}
        
        // Connect vector source and the identity
        vsrc.registerConsumer(0, id, 0);
        id.registerSupplier(0, vsrc, 0);
        try
        {
            // Vector source has 1 output port. 
            vsrc.registerConsumer(1, id, 0);
            fail("Registered supplier at an output port that is out of bounds.");
        }
        catch(ConfigException ex) {}
        
        // Connect Identity and Block
        block.connect(0, id, 0);
        
        // Test out of bounds check
        try
        {
            id.connect(1, id, 0);
            fail("Connected to out of bounds input port");
        }
        catch(ConfigException ex) {}
        try
        {
            id.connect(0, id, 1);
            fail("Connected to out of bounds output port");
        }
        catch(ConfigException ex) {}
    }

	// **********************************************************\
    // *                JUnit Setup and Teardown                *
    // **********************************************************/
	public DataFlowTest(String name)
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
