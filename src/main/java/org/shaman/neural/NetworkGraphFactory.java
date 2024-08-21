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

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.shaman.graph.Graph;
import org.shaman.graph.GraphException;
import org.shaman.graph.GraphNode;
import org.shaman.graph.Node;



/**
 * <h2>Neural Network Graph Factory</h2>
 * Creates Graphs describing the connections between
 * the neurons in a Neural Network
 */

public class NetworkGraphFactory
{    
    // **********************************************************\
    // *       Make fully connection Hopfield network graph     *
    // **********************************************************/
    public static NetworkGraph makeHopfield(int numneu) throws GraphException
    {
        NetworkGraph graph;
        int          i;
        GraphNode    node;
        Iterator     itnode;
        TreeSet      nodeset, neiset;
        
        // Make a single layer of neurons, fully connected to eachother
        graph   = new NetworkGraph();
        nodeset = new TreeSet();
        for (i=0; i<numneu; i++)
        {
            node = new Node();
            nodeset.add(node);
        }
        itnode = nodeset.iterator();
        while(itnode.hasNext())
        {
            node   = (Node)itnode.next();
            neiset = (TreeSet)nodeset.clone();
            neiset.remove(node);
            node.setNeighbors((GraphNode [])neiset.toArray(new GraphNode[]{}));
        }
        
        // Add the layer as input
        graph.addLayer(NetworkGraph.LAYER_INPUT, nodeset);
        
        return(graph);
    }
    
    // **********************************************************\
    // *  Make Hopfield network connection like the given Graph *
    // **********************************************************/
    public static NetworkGraph makeHopfield(Graph graph) throws GraphException
    {
        NetworkGraph ngraph;
        TreeSet      nodes;
        
        // Set the graph's nodes as the hopfield single layer network
        ngraph = new NetworkGraph();
        nodes  = new TreeSet(graph.getNodeMap().values());
        ngraph.addLayer(NetworkGraph.LAYER_INPUT, nodes);
        
        return(ngraph);
    }
    
    // **********************************************************\
    // *   Make the network structure of a Self-Organizing Map  *
    // **********************************************************/
    public static NetworkGraph makeSOM(int inSize, int numNeu) throws GraphException
    {
        NetworkGraphFactory fact;
        NetworkGraph        graph;
        Set                 layneu;
        
        // Make single layer of the given amount of neurons fully connected to the input layer
        fact   = new NetworkGraphFactory();
        graph  = new NetworkGraph();
        layneu = fact.addInputLayer(inSize, graph);
        layneu = fact.addLayer(NetworkGraph.LAYER_OUTPUT, layneu, numNeu, graph);
        
        return(graph);
    }
    
    // **********************************************************\
    // * Make the network structure of a multi-layer neural net *
    // **********************************************************/
    public static NetworkGraph makeMLP(int inSize, int lay1Size, int lay2Size, int outSize) throws GraphException
    {
        NetworkGraphFactory fact;
        NetworkGraph        graph;
        Set                 layneu;
        
        // Make fully connected multi-layer neural network
        fact   = new NetworkGraphFactory();
        graph  = new NetworkGraph();
        layneu = fact.addInputLayer(inSize, graph);
        if (lay1Size != 0) layneu = fact.addLayer(1, layneu, lay1Size, graph);
        if (lay2Size != 0) layneu = fact.addLayer(2, layneu, lay1Size, graph);
        layneu = fact.addLayer(NetworkGraph.LAYER_OUTPUT, layneu, outSize, graph);
        
        return(graph);
    }
    
    private Set addLayer(int layNum, Set prevlayer, int laySize, NetworkGraph graph) throws GraphException
    {
        Set         nodes;
        GraphNode   neu;
        GraphNode []nodenei;
        int         i;
        
        // Connect this layer's nodes to all nodes of the previous layer
        nodenei = (GraphNode [])prevlayer.toArray(new GraphNode[]{});
        nodes   = new TreeSet();
        for (i=0; i<laySize; i++)
        {
            neu = new Node();
            neu.setNeighbors(nodenei);
            nodes.add(neu);
        }
        graph.addLayer(layNum, nodes);
        
        return(nodes);
    }
    
    private Set addInputLayer(int inSize, NetworkGraph graph) throws GraphException
    {
        int       i;
        Set       nodes;
        GraphNode neu;
        
        // Input nodes do not have connections to other nodes
        nodes = new TreeSet();
        for (i=0; i<inSize; i++)
        {
            neu = new Node();
            nodes.add(neu);
        }
        graph.addLayer(NetworkGraph.LAYER_INPUT, nodes);
        
        return(nodes);
    }
}