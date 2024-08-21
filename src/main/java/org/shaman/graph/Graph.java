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
package org.shaman.graph;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.shaman.dataflow.Persister;
import org.shaman.exceptions.ConfigException;


/**
 * <h2>Graph</h2>
 * Contains the structure and reports about graph- and complex-network
 * theoretical properties of a graph.
 */
public class Graph implements Persister
{
    private Map        nodeMap;         // Map of (ID -> GraphNode) of all nodes in the Graph.
    
    // *********************************************************\
    // *             Network Graph (De)Construction            *
    // *********************************************************/
    public void add(GraphNode node) throws GraphException
    {
        if (!this.nodeMap.containsKey(new Long(node.getID())))
        {
            // Add the Node to the graph if not already there
            this.nodeMap.put(new Long(node.getID()), node);
        }
        else throw new GraphException("The specified node '"+node.getID()+"' is already present in the Graph");
    }
    
    public void remove(GraphNode oldnode) throws GraphException
    {
        int       i;
        GraphNode []nei;
        Long        oldid;
        
        // Check if the node is present in the Graph
        oldid = new Long(oldnode.getID());
        if (nodeMap.get(oldid) == null)
            throw new GraphException("The specified node "+oldnode.getID()+" is not present");
        
        // Remove the edges from the neighbors of the node to the node itself.
        nei = oldnode.getNeighbors();
        for (i=0; i<nei.length; i++) nei[i].removeNeighbor(oldid.longValue());
        
        nodeMap.remove(oldid);
    }
    
    // *********************************************************\
    // *                             API                       *
    // *********************************************************/
    public int getNumberOfNodes() { return(this.nodeMap.size()); }
    public GraphNode []getNodes() { return((GraphNode[])this.nodeMap.values().toArray(new GraphNode[]{})); }
    public Map  getNodeMap()            { return(this.nodeMap); }
    public void setNodeMap(Map nodeMap) { this.nodeMap = nodeMap; }
    
    // *********************************************************\
    // *                             Output                    *
    // *********************************************************/
    public void printTopology() throws GraphException
    {
        GraphNode nodenow;
        Iterator  nodeit;
        
        nodeit = nodeMap.values().iterator();
        while (nodeit.hasNext())
        {
            nodenow = (GraphNode)nodeit.next();
            System.out.println(nodenow.toString());
        }
    }
    
    // **********************************************************\
    // *             State Persistence Implementation           *
    // **********************************************************/
    public void loadState(ObjectInputStream oin) throws ConfigException
    {
        try
        {
            long  []ids;
            int [][]neimat;
            GraphNode []nodes;
            GraphNode []neinode;
            int         i,j;
            
            // Load ids and adjency matrix
            ids    = (long [])oin.readObject();
            neimat = (int [][])oin.readObject();
            
            // Assemble graph with given ids and topology
            this.nodeMap = new TreeMap();
            nodes = new GraphNode[ids.length];
            for (i=0; i<nodes.length; i++)
            {
                nodes[i] = new Node();
                nodes[i].setID(ids[i]);
            }            
            for (i=0; i<nodes.length; i++)
            {
                neinode = new GraphNode[neimat[i].length];
                for (j=0; j<neimat[i].length; j++) neinode[j] = nodes[neimat[i][j]];
                nodes[i].setNeighbors(neinode);
                
                add(nodes[i]);
            }
        }
        catch(GraphException ex)         { throw new ConfigException(ex); }
        catch(IOException ex)            { throw new ConfigException(ex); }
        catch(ClassNotFoundException ex) { throw new ConfigException(ex); }
    }
    
    public void saveState(ObjectOutputStream oout) throws ConfigException
    {
        try
        {
            long      []ids;
            Iterator    itid, itnod;
            HashMap     idind;
            long        idnow;
            int         i,j;
            GraphNode []nodnei;
            int     [][]neimat;
            
            // Make array of the Node ids
            idind = new HashMap();
            ids   = new long[this.nodeMap.size()];
            itid  = this.nodeMap.keySet().iterator();
            for (i=0; i<ids.length; i++)
            {
                idnow  = ((Long)itid.next()).longValue();
                ids[i] = idnow;
                idind.put(new Long(idnow), new Integer(i));
            }
            
            // Make sparse adjecency matrix
            itnod  = this.nodeMap.values().iterator();
            neimat = new int[this.nodeMap.size()][];
            for (i=0; i<neimat.length; i++)
            {
                nodnei    = ((GraphNode)itnod.next()).getNeighbors();
                neimat[i] = new int[nodnei.length];
                for (j=0; j<neimat[i].length; j++)
                    neimat[i][j] = ((Integer)idind.get(new Long(nodnei[j].getID()))).intValue();
            }
            
            // Write IDs and adjecency matrix as state
            oout.writeObject(ids);
            oout.writeObject(neimat);
        }
        catch(GraphException ex) { throw new ConfigException(ex); }
        catch(IOException ex)    { throw new ConfigException(ex); }
    }
    
    // *********************************************************\
    // *            Constructor and initialization             *
    // *********************************************************/
    public Graph()
    {
        // Start with an empty graph.
        nodeMap = new TreeMap();
    }
}
