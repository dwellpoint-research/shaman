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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 *  <h2>Graph Node</h2>
 * Node of a Graph
 */
public class Node implements GraphNode, Comparable
{
    private static long nextID = 0;      // Unique ID counter
    private long        id;              // The Unique ID of this Node
    private GraphNode []neighbor;        // End point of this Node's Edges in the Network Graph
    private Map         localNode;       // Map of nodes in the 'neighborhood' of this node.
    
    // *********************************************************\
    // *         Graph Node Interface Implementation           *
    // *********************************************************/
    public void addNeighbor(GraphNode newneighbor) throws GraphException
    {
        int       i, ind;
        long      newid;
        GraphNode []newnei;
        
        // Check if the new neighbor is already connected to this node.
        newid = newneighbor.getID();
        ind   = -1;
        for (i=0; (i<this.neighbor.length) && (ind == -1); i++) 
            if (this.neighbor[i].getID() == newid) ind = i;
        
        if (ind == -1)
        {
            // Connect the new neighbor.
            newnei = new GraphNode[this.neighbor.length+1];
            for (i=0; i<this.neighbor.length; i++) newnei[i] = this.neighbor[i];
            newnei[i] = newneighbor;
            this.neighbor = newnei;
        }
    }
    
    public void removeNeighbor(long id) throws GraphException
    {
        int       i, ind;
        GraphNode []newnei;
        
        // Check if the node with the given ID is really a neighbor.
        ind = -1;
        for (i=0; i<this.neighbor.length; i++) 
            if (this.neighbor[i].getID() == id) ind = i;
        
        if (ind != -1)
        {
            newnei = new GraphNode[this.neighbor.length-1];
            for (i=0; i<newnei.length; i++)
            {
                if   (i<ind) newnei[i] = this.neighbor[i];
                else         newnei[i] = this.neighbor[i+1];
            }
            this.neighbor = newnei;
        }
        else throw new GraphException("Cannot find neighbor to remove '"+id+"'");
    }
    
    public boolean hasNeighbor(GraphNode nei) throws GraphException
    {
        int     i;
        long    neiid;
        boolean found;
        
        neiid = nei.getID();
        found = false;
        for (i=0; (i<this.neighbor.length) && (!found); i++)
        {
            if (this.neighbor[i].getID() == neiid) found = true;
        }
        
        return(found);
    }
    
    public GraphNode []getNeighbors() throws GraphException
    {
        return(this.neighbor);
    }
    
    public void setNeighbors(GraphNode []nei) throws GraphException
    {
        this.neighbor = nei;
    }
    
    public int getDegree()
    {
        // Count number of neighbors. The degree of this node in Graph Theory talk.
        if (this.neighbor == null) return(0);
        else                       return(this.neighbor.length);
    }
    
    public long getID() { return(id); }
    
    public void setID(long id) { this.id = id; }
    
    // *********************************************************\
    // *   Dynamic Complex Network Construction / Maintenance  *
    // *********************************************************/
    public int []getNumberOfFirstAndSecondNeighbors() throws GraphException
    {
        int       i;
        GraphNode []neinow;
        GraphNode nodenow;
        HashMap   neifir;
        HashMap   neisec;
        Iterator  neiit;
        int       []nfs;
        
        neifir = new HashMap();
        neisec = new HashMap();
        nfs    = new int[2];
        
        // Make a unique list of first degree neighbors
        neinow = getNeighbors();
        for (i=0; i<neinow.length; i++)
        {
            neifir.put(new Long(neinow[i].getID()), neinow[i]);
        }
        
        // Make a unique list of second degree neighbors
        neiit = neifir.values().iterator();
        while (neiit.hasNext())
        {
            nodenow = (GraphNode)neiit.next();
            neinow  = nodenow.getNeighbors();
            for (i=0; i<neinow.length; i++) neisec.put(new Long(neinow[i].getID()), neinow[i]);
        }
        
        // Return the number of unique first and second degree neighbors.
        nfs[0] = neifir.size();
        nfs[1] = neisec.size();
        
        return(nfs);
    }
    
    
    public GraphNode []getLocalNodes()
    {
        GraphNode []node = (GraphNode [])this.localNode.values().toArray(new GraphNode[]{});
        
        return(node);
    }
    
    public double []getLocalDegreeDistribution() throws GraphException
    {
        int         i;
        GraphNode []node;
        double    []Pk;
        int         maxDegree;
        int       []nodeDegree;
        
        // Make a array of the nodes in the local Sub Graph if not there yet.
        if (this.localNode == null) exploreLocalSubGraph();
        node = (GraphNode [])this.localNode.values().toArray(new GraphNode[]{});
        
        // Get the degree of all nodes
        nodeDegree = new int[node.length];
        maxDegree  = 0;
        for (i=0; i<nodeDegree.length; i++)
        {
            nodeDegree[i] = node[i].getDegree();
            if (nodeDegree[i] > maxDegree) maxDegree = nodeDegree[i];
        }
        
        // Calculate the Distribution
        Pk = new double[maxDegree+1];
        for (i=0; i<maxDegree+1; i++) Pk[i] = 0;
        for (i=0; i<node.length; i++) Pk[nodeDegree[i]]++;
        for (i=0; i<maxDegree+1; i++) Pk[i] /= node.length;
        
        return(Pk);
    }
    
    protected void  exploreLocalSubGraph() throws GraphException
    {
        int         NEI = 3;
        int         i,j;
        HashSet     nodes;
        HashSet     newnodes;
        Iterator    nodeit;
        GraphNode   gnnow;
        GraphNode []neinow;
        Long        idnow;
        boolean     stop;
        
        // Start the Local Node map with this node itself.
        this.localNode = new HashMap();
        this.localNode.put(new Long(getID()), this);
        
        // Get the nodes of the 'local' sub-graph
        nodes = new HashSet();
        nodes.add(this);
        stop  = false;
        j     = 0;
        while (!stop)
        {
            // Remember the new nodes in the neighbors of last iteration's new nodes.
            newnodes = new HashSet();
            nodeit   = nodes.iterator();
            while (nodeit.hasNext())
            {
                gnnow  = (GraphNode)nodeit.next();
                neinow = gnnow.getNeighbors();
                for (i=0; i<neinow.length; i++)
                {
                    idnow = new Long(neinow[i].getID());
                    if (!this.localNode.containsKey(idnow))
                    {
                        this.localNode.put(idnow, neinow[i]);
                        newnodes.add(neinow[i]);
                    }
                }
            }
            
            // Check if we should stop exploring...
            if (newnodes.size() > 0)
            {
                nodes    = (HashSet)newnodes.clone();
                newnodes = new HashSet();
                j++;
                
                if (j>NEI) stop = true;
            }
            else stop = true;
        }
    }
    
    
    // *********************************************************\
    // *          Constructor and ID Initialization            *
    // *********************************************************/
    public int compareTo(Object o2)
    {
        if (o2 instanceof GraphNode)
        {
            GraphNode n2 = (GraphNode)o2;
                
            if      (this.id  < n2.getID())  return(-1);
            else if (this.id == n2.getID()) return(0);
            else                            return(1);
        }
        else return(0);
    }
    
    public String toString() { return(this.neighbor.length+"\t"+this.id); }
    
    private void makeID()
    {
        this.id = nextID++;
    }
    
    public Node()
    {
        this.neighbor = new GraphNode[0];
        makeID();
    }
}
