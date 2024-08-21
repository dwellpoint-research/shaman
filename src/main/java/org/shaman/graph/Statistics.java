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
import java.util.Set;

import cern.jet.random.Uniform;

/**
 * <h2>Statistics</h2>
 * Reports about graph- and complex-network theoretical properties of a graph.
 */
public class Statistics
{
    private Graph       graph;            // Graph to investigate
    
    // *********************************************************\
    // *     Fraction of Nodes visited in Random Walk          *
    // *********************************************************/
    public double getVisitedFractionAfterRandomWalk(int walklen, int repeat) throws GraphException
    {
        double []vf;
        
        vf = getVisitedFractionsAfterRandomWalk(walklen, repeat);
        
        return(vf[walklen-1]);
    }
    
    public double []getVisitedFractionsAfterRandomWalk(int walklen, int repeat) throws GraphException
    {
        int       i,j;
        double    []vf;
        Set       nodeset;
        GraphNode nodenow;
        GraphNode []nei;
        GraphNode []node;
        int       rind;
        boolean   deadend;
        
        vf   = new double[walklen];
        node = this.graph.getNodes();
        for (i=0; i<repeat; i++)
        {
            rind    = Uniform.staticNextIntFromTo(0, node.length-1);
            nodeset = new HashSet();
            nodenow = node[rind];
            deadend = false;
            for (j=1; (j<walklen) && (!deadend); j++)
            {
                nodeset.add(new Long(nodenow.getID()));
                vf[j] += (((double)nodeset.size()) / node.length);
                
                nei     = nodenow.getNeighbors();
                if ((nei != null) && (nei.length > 0))
                {
                    rind    = Uniform.staticNextIntFromTo(0, nei.length-1);
                    nodenow = nei[rind];
                }
                else deadend = true;
            }
        }
        for (i=0; i<vf.length; i++) vf[i] /= repeat;
        
        return(vf);
    }
    
    // *********************************************************\
    // *            Neighbor Distance Distribution             *
    // *********************************************************/
    public double []getNeighborDistanceDistribution() throws GraphException
    {
        // Calculate the probability distribution that a random node
        // is 'd' steps away from another random node.
        int       i,j,d;
        int       dsize;
        GraphNode []node;
        GraphNode nodenow;
        GraphNode []neinow;
        double    []pd;
        double    [][]cd;
        int       maxd;
        Iterator  neiit;
        HashSet   neiall;
        HashSet   neiprev;
        HashSet   neinew;
        
        node  = this.graph.getNodes();
        cd    = new double[node.length][];
        dsize = (int)Math.sqrt(node.length);      // Should be fine for small world networks since > log(nodes.length)
        for (i=0; i<node.length; i++) cd[i] = new double[dsize];
        
        // Count the number of 'n'-th degree neighbors for all nodes.
        maxd = 0;
        for (i=0; i<node.length; i++)
        {
            // Start the set of all encountered nodes.
            neiall = new HashSet();
            neiall.add(node[i]);
            
            // Start with the '0'-th degree neighbor : The node itself.
            d       = 0;
            neiprev = new HashSet();
            neiprev.add(node[i]);
            do
            {
                // Find the 'd'-th degree neighbors
                neiit  = neiprev.iterator();
                neinew = new HashSet();
                while(neiit.hasNext())
                {
                    nodenow = (GraphNode)neiit.next();
                    neinow  = nodenow.getNeighbors();
                    for (j=0; j<neinow.length; j++)
                    {
                        if (!neiall.contains(neinow[j]))
                        {
                            neinew.add(neinow[j]);
                            neiall.add(neinow[j]);
                        }
                    }
                }
                neiprev = (HashSet)neinew.clone();
                
                // Remember the number of 'd'-th degree neighbors
                d++;
                if (d > maxd) maxd = d;
                if (d < dsize) cd[i][d] = neinew.size();
                else throw new GraphException("Graph Diameter is to large. Can't determine neighbor distribution.");
            }
            while (neinew.size() > 0);  // Continue while at least one new node was discovered last iteration
        }
        
        // Average the results of all nodes to get the neighbor distribution
        pd = new double[maxd];
        for (i=0; i<node.length; i++)
        {
            for (j=0; j<maxd; j++) pd[j] += cd[i][j];
        }
        for (i=0; i<maxd; i++) pd[i] /= (node.length * node.length);
        
        return(pd);
    }
    
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
    
    // *********************************************************\
    // *                  Degree Distribution                  *
    // *********************************************************/
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
    
    public double getAverageDegree() throws GraphException
    {
        int       i;
        double    deg;
        GraphNode []node;
        
        node = this.graph.getNodes();
        deg  = 0;
        for (i=0; i<node.length; i++) deg += node[i].getNeighbors().length;
        deg /= node.length;
        
        return(deg);
    }
    
    // *********************************************************\
    // *            Constructor and initialization             *
    // *********************************************************/
    public Statistics(Graph graph)
    {
        this.graph = graph;
    }
}
