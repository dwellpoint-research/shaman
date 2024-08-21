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
package org.shaman.neural;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.shaman.exceptions.LearnerException;
import org.shaman.graph.Graph;
import org.shaman.graph.GraphException;
import org.shaman.graph.GraphNode;
import org.shaman.graph.Node;


/**
 * <h2>Neural Network Graph</h2>
 * Describes the structure of a neural network
 */
public class NetworkGraph extends Graph
{
    public static final int LAYER_INPUT  = 0;
    public static final int LAYER_OUTPUT = 512;
    
    private TreeMap layers;         // The neural network layers. Map of (Integer(Layer number), Set of GraphNode)
    private int   []layBegin;       // [i] = Index of first neuron of layer i
    private int   []layEnd;         // [i] = Index of last neuron of layer i
    
    private long    neuid;          // Ordered unique ID counter
    
    // **********************************************************\
    // *                Network Layer Definition                *
    // **********************************************************/
    public void addLayer(int order, Set neurons) throws GraphException
    {
        Iterator  itneu;
        GraphNode neu;
        int       i;
        int     []begnew;
        int     []endnew;
        
        // Find out it which neuron index this layer begins
        begnew = new int[this.layBegin.length+1];
        for (i=0; i<this.layBegin.length; i++) begnew[i] = this.layBegin[i];
        begnew[i] = this.getNumberOfNodes();
        
        // Remember this layer
        this.layers.put(new Integer(order), neurons);
        
        // Add the neurons to the Graph
        itneu = neurons.iterator();
        while(itneu.hasNext())
        {
            neu = (GraphNode)itneu.next();
            neu.setID(this.neuid++);
            add(neu);
        }
        
        // And remember where this layer ends
        endnew = new int[this.layEnd.length+1];
        for (i=0; i<this.layEnd.length; i++) endnew[i] = this.layEnd[i];
        endnew[i] = this.getNumberOfNodes()-1;
        
        this.layBegin = begnew;
        this.layEnd   = endnew;
    }
    
    public int [][][]getNeuronInputs() throws LearnerException
    {
        Map         neuind;
        Set         neulay;
        int         pos;
        GraphNode   node;
        Iterator    itlay, itneu;
        
        // Make a map of Graph node -> Index
        pos    = 0;
        neuind = new HashMap();
        itlay  = this.layers.values().iterator();
        while(itlay.hasNext())
        {
            neulay = (Set)itlay.next();
            itneu  = neulay.iterator();
            while(itneu.hasNext())
            {
                node = (Node)itneu.next();
                neuind.put(node, new Integer(pos++));
            }
        }
        
        int [][][]layin = null;
        try
        {
            int         i,j,k;
            GraphNode []nei;
            GraphNode   neinow;
            
            // Make an array of [number of layers][number of neurons in layer][number of neighbors of neuron] 
            //                  = index of neighbor
            i     = 0;
            layin = new int[this.layers.size()][][];
            itlay = this.layers.values().iterator();
            while(itlay.hasNext())
            {
                neulay   = (Set)itlay.next();
                layin[i] = new int[neulay.size()][];
                itneu    = neulay.iterator();
                j        = 0;
                while(itneu.hasNext())
                {
                    node = (Node)itneu.next();
                    nei  = node.getNeighbors();
                    layin[i][j] = new int[nei.length];
                    for(k=0; k<nei.length; k++)
                    {
                        neinow = nei[k];
                        layin[i][j][k] = ((Integer)neuind.get(neinow)).intValue();
                    }
                    j++;
                }
                i++;
            }
        }
        catch(GraphException ex) { throw new LearnerException(ex); }
        
        return(layin);
    }
    
    // **********************************************************\
    // *                   Lattice Construction                 *
    // **********************************************************/
    public NetworkGraph()
    {
        this.layers   = new TreeMap();
        this.layBegin = new int[0];
        this.layEnd   = new int[0];
        this.neuid    = 0;
    }
}