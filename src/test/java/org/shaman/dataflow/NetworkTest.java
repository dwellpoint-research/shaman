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
import org.shaman.dataflow.IdleSource;
import org.shaman.dataflow.Join;
import org.shaman.dataflow.NetworkConnection;
import org.shaman.dataflow.NetworkNode;
import org.shaman.dataflow.PortMapping;
import org.shaman.dataflow.Transformation;
import org.shaman.dataflow.TransformationNetwork;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ShamanException;

import junit.framework.TestCase;


/**
 * Test of the DataFlow network package
 */
public class NetworkTest extends TestCase
{
    // **********************************************************\
    // *           Transformation Network Building              *
    // **********************************************************/
    public void testGrow() throws ShamanException
    {
       TransformationNetwork net;
       
       net = new TransformationNetwork();
       
       // Test Isolated Network Growing
       net.grow(0,0);
       assertEquals(0, net.getNumberOfInputs());
       assertEquals(0, net.getNumberOfOutputs());
       
       // Test Normal sized Network Growing
       net.grow(2,3);
       assertEquals(2, net.getNumberOfInputs());
       assertEquals(3, net.getNumberOfOutputs());
    }
    
    public void testPopulate() throws ShamanException
    {
        String              []traclass;
        int               [][]tracon;
        int               [][]tragrow;
        TransformationNetwork tranet;
        
        traclass = new String[]
        {
           "org.shaman.dataflow.IdleSource",
           "org.shaman.dataflow.IdleSource",
           "org.shaman.dataflow.Join",
           "org.shaman.dataflow.Block"
        };
        tracon   = new int[][]
        {
            new int[]{0,0,2,0},
            new int[]{1,0,2,1},
            new int[]{2,0,3,0}
        };
        tragrow = new int[][]
        {
            null,
            null,
            new int[]{2},
            null
        };
        
        tranet = new TransformationNetwork();
        tranet.grow(0,0);
        tranet.populate(traclass, tragrow, tracon);
        
        // Check general presence.
        Transformation []tra;
        PortMapping    portmap;
        int            i;
        
        portmap = tranet.getInputPortMapping();
        assertNotNull(portmap);
        portmap = tranet.getOutputPortMapping();
        assertNotNull(portmap);
        tra     = tranet.getTransformations();
        assertNotNull(tra);
        assertEquals(4, tra.length);
        for (i=0; i<tra.length; i++)
        {
            assertEquals(tra[i].getClass().getName(), traclass[i]);
        }
        
        // Check connections
        IdleSource src;
        Join       join;
        Block      block;
        List       conlist;
        
        src     = (IdleSource)tra[0];
        conlist = src.getConsumers(0);
        assertEquals(1, conlist.size());
        assertEquals(tra[2], conlist.get(0));
        src     = (IdleSource)tra[1];
        conlist = src.getConsumers(0);
        assertEquals(1, conlist.size());
        assertEquals(tra[2], conlist.get(0));
        join    = (Join)tra[2];
        assertEquals(tra[0], join.getSupplier(0));
        assertEquals(tra[1], join.getSupplier(1));
        
        conlist = join.getConsumers(0);
        assertEquals(1, conlist.size());
        assertEquals(tra[3], conlist.get(0));
        block   = (Block)tra[3];
        assertEquals(tra[2], block.getSupplier(0));
    }
    
    public void testPopulateNamed() throws ShamanException
    {
        NetworkNode []netnod = new NetworkNode[]
        {
            new NetworkNode("Source1", "org.shaman.dataflow.IdleSource",               "First Source",  0),
            new NetworkNode("Source2", "org.shaman.dataflow.IdleSource",               "Second Source", 1),
            new NetworkNode("Merge",   "org.shaman.dataflow.Merge",      new int[]{2}, "Source Merge",  2)
        };
        
        NetworkConnection []netcon = new NetworkConnection[]
        {
            new NetworkConnection("Source1", 0, "Merge", 0),
            new NetworkConnection("Source2", 0, "Merge", 1),
            new NetworkConnection("Merge",   0, null,    0)
        };
        
        TransformationNetwork net;
        Transformation      []tra;
        int                   i;
        
        net = new TransformationNetwork();
        net.grow(0, 1);
        net.populate(netnod, netcon);
        tra = net.getTransformations();
        assertEquals(3, tra.length);
        for (i=0; i<netnod.length; i++)
        {
            assertTrue(net.containsTransformation(tra[i]));
            assertEquals(i, net.getTransformationIndex(netnod[i].getName()));
            assertEquals(tra[i], net.getTransformation(i));
            assertEquals(tra[i], net.getTransformation(netnod[i].getName()));
        }
    }
    
    // **********************************************************\
    // *           Test Connecting to Other Transformations     *
    // **********************************************************/
    public void testConnectExternal() throws ShamanException
    {
        NetworkNode []netnod = new NetworkNode[]
        {
            new NetworkNode("Source"  , "org.shaman.dataflow.IdleSource",               "Source", 0),
            new NetworkNode("Identity", "org.shaman.dataflow.Identity",   new int[]{1}, "Dummy",  1),
        };
        
        NetworkConnection []netcon = new NetworkConnection[]
        {
            new NetworkConnection("Source",   0, "Identity", 0),
            new NetworkConnection("Identity", 0, null,       0)
        };
        
        TransformationNetwork net1, net2;
        Join                  join;
        Block                 block;
        IdleSource            src;
        DataModel             dm;
        List                  lcon;
        
        // Build the network. And connect them to some external Transformations.
        net1  = new TransformationNetwork();
        net2  = new TransformationNetwork();
        join  = new Join();
        block = new Block();
        net1.grow(0, 1);
        net2.grow(0, 1);
        join.grow(2);
        net1.populate(netnod, netcon);
        net2.populate(netnod, netcon);
        join.registerSupplier(0, net1, 0);
        net1.registerConsumer(0, join, 0);
        join.registerSupplier(1, net2, 0);
        net2.registerConsumer(0, join, 1);
        join.registerConsumer(0, block, 0);
        block.registerSupplier(0, join, 0);
        
        // Check connects
        lcon = net1.getConsumers(0);
        assertEquals(1, lcon.size());
        assertEquals(join, lcon.get(0));
        lcon = net2.getConsumers(0);
        assertEquals(1, lcon.size());
        assertEquals(join, lcon.get(0));
        
        assertEquals(net1.getOutputPortMapping(), join.getSupplier(0));
        assertEquals(net2.getOutputPortMapping(), join.getSupplier(1));
        lcon = join.getConsumers(0);
        assertEquals(1, lcon.size());
        assertEquals(block, lcon.get(0));
        
        assertEquals(join, block.getSupplier(0));
        
        // Configure the internal source in the networks.
        dm  = TestUtils.makeNumberDataModel(2, false);
        src = (IdleSource)net1.getTransformation("Source");
        src.setOutputModel(dm);
        src = (IdleSource)net2.getTransformation("Source");
        src.setOutputModel(dm);
        
        // Initialize the how thing.
        net1.init();
        net2.init();
        join.init();
        block.init();
    }

	// **********************************************************\
    // *                JUnit Setup and Teardown                *
    // **********************************************************/
	public NetworkTest(String name)
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
