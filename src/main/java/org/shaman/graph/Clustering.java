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
import java.util.Iterator;
import java.util.Map;

import cern.colt.bitvector.BitVector;

/**
 * <h2>Clustering</h2>
 * Cluster structure of a graph.
 */
public class Clustering
{
    private Graph       graph;            // Graph to investigate
    
    private GraphNode []lcNode;           // Cluster discovery data
    private Map         lcIDToIndex;
    private BitVector   lcVector;
    private int         lcSize;
    
    // *********************************************************\
    // *            Cluster Structure of the Graph             *
    // *********************************************************/
    public double getLargestClusterSize()
    {
        return(lcSize);
    }
    
    public GraphNode []getLargestClusterNodes()
    {
        int       i,j;
        GraphNode []cnod;
        
        // Return the nodes that are marked as belonging to the largest clutzer.
        cnod = new GraphNode[lcVector.cardinality()];
        j    = 0;
        for (i=0; i<lcNode.length; i++) if (lcVector.get(i)) { cnod[j++] = lcNode[i]; }
        
        return(cnod);
    }
    
    public double getAveragePathLengthInLargestCluster() throws GraphException
    {
        int       i;
        GraphNode []cnod;
        int       []z1z2;
        double    z1,z2;
        double    N;
        double    l;
        
        // Get the nodes belonging to the largest cluster
        cnod = getLargestClusterNodes();
        N    = cnod.length;
        
        if (N > 1)
        {
            // Calculate average number of first and second degree neighbors
            z1 = 0; z2 = 0;
            for (i=0; i<cnod.length; i++)
            {
                z1z2 = getNumberOfFirstAndSecondNeighbors(cnod[i]);
                z1  += z1z2[0];
                z2  += z1z2[1];
            }
            z1 /= N; z2 /= N;
            
            if ((z1 > 0) && (z2 != z1))
            {
                // Approximate Average Path Length.
                l = 1+(Math.log(N/z1) / Math.log(z2/z1));
            }
            else l = 0;
        }
        else l = 0;
        
        return(l);
    }
    
    public void findLargestCluster() throws GraphException
    {
        int       i;
        HashMap   idToIndex;
        GraphNode []node;
        GraphNode nodenow;
        Iterator  nodeit;
        boolean   done;
        int       numnodes;
        BitVector visitall;
        BitVector visitnow;
        BitVector visitnew;
        BitVector vecneiall;
        int       indnow;
        boolean   newnodes;
        
        node     = this.graph.getNodes();
        numnodes = this.graph.getNodeMap().size();
        lcNode   = node;
        lcSize   = 0;
        
        // Make a ID to Index Translation Table
        idToIndex   = new HashMap();
        lcIDToIndex = idToIndex;
        lcSize      = 0;
        nodeit      = this.graph.getNodeMap().values().iterator();
        i = 0; while (nodeit.hasNext())
        {
            nodenow = (GraphNode)nodeit.next();
            idToIndex.put(new Long(nodenow.getID()), new Integer(i));
            i++;
        }
        
        // Find Largest Cluster
        visitall  = new BitVector(numnodes);
        visitall.clear();
        done      = false;
        visitall  = new BitVector(numnodes);
        visitnow  = new BitVector(numnodes);
        visitnew  = new BitVector(numnodes);
        vecneiall = new BitVector(numnodes);
        indnow   = 0;
        while(!done)
        {
            // Find all n-th degree neighbors of node[indnow]
            visitnow.set(indnow);
            visitnew.clear();
            visitnew.set(indnow);
            newnodes = true;
            while(newnodes)
            {
                vecneiall.clear();
                for (i=0; i<numnodes; i++)
                {
                    if (visitnew.get(i)) vecneiall.or(getNeighborVector(i));
                }
                visitnew = vecneiall.copy();
                visitnew.andNot(visitnow);
                
                if (visitnew.cardinality() > 0) visitnow.or(visitnew);
                else                            newnodes = false;
            }
            
            // Check if this is the largest cluster
            if (visitnow.cardinality() > lcSize)
            {
                lcSize   = visitnow.cardinality();
                lcVector = visitnow.copy();
            }
            
            // Mark the nodes of the new cluster
            visitall.or(visitnow);
            
            visitnow.clear();
            
            // Still nodes left in other clusters?
            if (visitall.cardinality() < numnodes)
            {
                // Find the first not visited node. And continue from there on.
                indnow = 0; while(visitall.get(indnow)) indnow++;
            }
            else done = true;
        }
    }
    
    private BitVector getNeighborVector(int ind) throws GraphException
    {
        int       i;
        GraphNode node;
        GraphNode []nei;
        int       neiind;
        BitVector vecnei;
        
        // Set the bits of the neighbors of node[ind]
        node   = lcNode[ind];
        nei    = node.getNeighbors();
        vecnei = new BitVector(lcNode.length);
        for (i=0; i<nei.length; i++)
        {
            neiind = getNodeIndex(nei[i].getID());
            vecnei.set(neiind);
        }
        
        return(vecnei);
    }
    
    private int getNodeIndex(long id)
    {
        Integer intid;
        
        intid = (Integer)lcIDToIndex.get(new Long(id));
        
        return(intid.intValue());
    }
    
    // *********************************************************\
    // *                  Degree Distribution                  *
    // *********************************************************/
    public double getAverageNumberOfSecondNeighbors() throws GraphException
    {
        int    i;
        double ks;
        GraphNode []node;
        
        node = this.graph.getNodes();
        ks   = 0;
        for (i=0; i<node.length; i++) ks += getNumberOfFirstAndSecondNeighbors(node[i])[1];
        ks /= node.length;
        
        return(ks);
    }
    
    public int []getNumberOfFirstAndSecondNeighbors(GraphNode node) throws GraphException
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
        neinow = node.getNeighbors();
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
    
    public double []getDegreeDistribution() throws GraphException
    {
        // Calculate the Degree Distribution of the Graph
        int       i;
        GraphNode []node;
        double    []Pk;
        int       maxDegree;
        int       []nodeDegree;
        
        // Make an array containing all the nodes
        node = this.graph.getNodes();
        
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
    
    
    // *********************************************************\
    // *            Constructor and initialization             *
    // *********************************************************/
    public Clustering(Graph graph)
    {
       this.graph = graph;
    }
}
