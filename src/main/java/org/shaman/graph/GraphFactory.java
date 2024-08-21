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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;

import cern.jet.random.Empirical;
import cern.jet.random.EmpiricalWalker;
import cern.jet.random.Uniform;
import cern.jet.random.engine.DRand;
import cern.jet.random.engine.RandomEngine;

/**
 * <h2>Graph Factory</h2>
 * Creates graphs of some well-know topologies
 */
public class GraphFactory
{
    // Ring shaped network graph. Every node has 2 neighbours.
    public static final int TOPOLOGY_RING       = 0;
    // Fully connected graph. Every node is connected to all the other ones.
    public static final int TOPOLOGY_CONNECTED  = 1;
    // Random Graph. Following the Erdos-Renyi Model.
    public static final int TOPOLOGY_RANDOM     = 2;  // par[0] = p,  rewire every par[1] nodes
    // Scale Free Graph. With power-law degree distribution.
    public static final int TOPOLOGY_SCALE_FREE = 3;  // par[0] = m
    // Connect Graph nodes according to given degree distribution
    private static final int TOPOLOGY_DEGREE_DISTRIBUTION = 4; // par[] = degree distribution
    // Wraparound grid.
    public static final int TOPOLOGY_WRAPAROUND_GRID = 5;
    
    // Neightborhood (degree of Nodes) for wraparound grid topology
    public static final double GRID_NEIGHBORHOOD_C9  = 0.0;
    public static final double GRID_NEIGHBORHOOD_L5  = 1.0;
    public static final double GRID_NEIGHBORHOOD_L9  = 2.0;
    public static final double GRID_NEIGHBORHOOD_C13 = 3.0;
    public static final double GRID_NEIGHBORHOOD_L4  = 4.0;
    
    // Neighborhood definition: arrays of relative positions of neighbors
    public static final int [][]NEIGHBORS_L4 = new int[][]
            {
        new int[]{ 0, -1},
        new int[]{ 0,  1},
        new int[]{ 1,  0}
            };
    
    public static final int [][]NEIGHBORS_L5 = new int[][]
            {
        new int[]{-1,  0},
        new int[]{ 0, -1},
        new int[]{ 0,  1},
        new int[]{ 1,  0}
            };
    public static final int [][]NEIGHBORS_L9 = new int[][]
            {
        new int[]{-2,  0},
        new int[]{-1,  0},
        new int[]{ 0, -2},
        new int[]{ 0, -1},
        new int[]{ 0,  1},
        new int[]{ 0,  2},
        new int[]{ 1,  0},
        new int[]{ 2,  0}
            };
    public static final int [][]NEIGHBORS_C9 = new int[][]
            {
        new int[]{-1, -1},
        new int[]{ 0, -1},
        new int[]{ 1, -1},
        new int[]{-1,  0},
        new int[]{ 1,  0},
        new int[]{-1,  1},
        new int[]{ 0,  1},
        new int[]{ 1,  1}
            };
    public static final int [][]NEIGHBORS_C13 = new int[][]
            {
        new int[]{-2,  0},
        new int[]{-1,  0},
        new int[]{ 0, -2},
        new int[]{ 0, -1},
        new int[]{ 0,  1},
        new int[]{ 0,  2},
        new int[]{ 1,  0},
        new int[]{ 2,  0},
        new int[]{-1, -1},
        new int[]{-1,  1},
        new int[]{ 1,  1},
        new int[]{ 1, -1}
            };

    private int      topology;                   // The topology of the network graph
    private double []topoPar;                    // Parameters of the topology (e.g. 'm' in scale-free, 'p' in random)
    private int      numNodes;                   // Number of nodes in the graph
    private Graph    graph;                      // The Graph

    // Construction data
    private GraphNode firstNode;                 // TOPOLOG_RING : First node in the Graph
    private double    sumDegree;                 // TOPOLOGY_SCALE_FREE

    // *********************************************************\
    // *                 Construction Methods                  *
    // *********************************************************/
    public static Graph makeRing(int numNodes) throws GraphException
    {
        GraphFactory fact;

        fact = new GraphFactory(TOPOLOGY_RING, numNodes, null);
        fact.create();

        return(fact.getGraph());
    }

    public static Graph makeConnected(int numNodes) throws GraphException
    {
        GraphFactory fact;

        fact = new GraphFactory(TOPOLOGY_CONNECTED, numNodes, null);
        fact.create();

        return(fact.getGraph());
    }

    public static Graph makeRandom(int numNodes, double p) throws GraphException
    {
        GraphFactory fact;

        fact = new GraphFactory(TOPOLOGY_RANDOM, numNodes, new double[]{p, numNodes/5});
        fact.create();

        return(fact.getGraph());
    }

    public static Graph makeScaleFree(int numNodes) throws GraphException
    {
        GraphFactory fact;

        fact = new GraphFactory(TOPOLOGY_SCALE_FREE, numNodes, new double[]{10});
        fact.create();

        return(fact.getGraph());
    }

    public static Graph makeScaleFree(int numNodes, double g) throws GraphException
    {
        int          k;
        double       p, ptot;
        double     []pk;
        GraphFactory fact;
        Graph        graph;

        // Make degree distribution p(k) = Ak^(-g)
        pk   = new double[numNodes];
        for (k=0; k<numNodes; k++)
        {
            if (k<3) pk[k] = 0.0;
            else
            {
                p     = Math.pow(k, -g);
                pk[k] = p;
            }
        }
        ptot = 0;
        for (k=0; k<numNodes; k++) ptot += pk[k];
        for (k=0; k<numNodes; k++) pk[k] /= ptot;

        // Create graph connected according to this degree distribution
        fact = new GraphFactory(TOPOLOGY_DEGREE_DISTRIBUTION, numNodes, pk);
        graph = fact.create();

        return(graph);
    }

    public static Graph makeGrid(int w, int h, double neighborhood) throws GraphException
    {
        if (w != h)
            throw new GraphException("Cannot create grid graph where width is not equal to hight.");

        GraphFactory fact;
        Graph graph;

        fact = new GraphFactory(TOPOLOGY_WRAPAROUND_GRID, w*h, new double[]{w, neighborhood});
        graph = fact.create();

        return(graph);
    }

    // *********************************************************\
    // *               Network Graph Construction              *
    // *********************************************************/
    public GraphFactory(int topology, int numNodes, double []topoPar)
    {
        this.topology = topology;
        this.numNodes = numNodes;
        this.topoPar  = topoPar;
    }

    public Graph create() throws GraphException
    {
        Graph     graph;

        graph = null;
        if ((this.topology == TOPOLOGY_CONNECTED) ||
                (this.topology == TOPOLOGY_RANDOM) ||
                (this.topology == TOPOLOGY_RING) ||
                (this.topology == TOPOLOGY_SCALE_FREE))             graph = createGrow();
        else if (this.topology == TOPOLOGY_DEGREE_DISTRIBUTION) graph = createDegreeDistribution();
        else if (this.topology == TOPOLOGY_WRAPAROUND_GRID)     graph = createGrid();
        else throw new GraphException("Unknown graph Topology given.");
        this.graph = graph;

        return(graph);
    }

    private Graph createGrid() throws GraphException
    {
        Graph graph;
        GraphNode [][]node;
        GraphNode []neighbors;
        int i, j, k, dim;
        GraphNode nodenow;

        // Create a rectangular grid of Nodes
        graph = new Graph();
        dim   = (int)this.topoPar[0];
        node  = new GraphNode[dim][dim];
        for(i=0; i<dim; i++)
        {
            for(j=0; j<dim; j++)
            {
                nodenow    = new Node();
                node[i][j] = nodenow;
                graph.add(nodenow);
            }
        }

        int [][]NEIGHBORS;

        // Connect the nodes according to the given neighborhood
        if (this.topoPar.length == 2)
        {
            if      (this.topoPar[1] == GRID_NEIGHBORHOOD_L5)  NEIGHBORS = NEIGHBORS_L5;
            else if (this.topoPar[1] == GRID_NEIGHBORHOOD_L9)  NEIGHBORS = NEIGHBORS_L9;
            else if (this.topoPar[1] == GRID_NEIGHBORHOOD_C9)  NEIGHBORS = NEIGHBORS_C9;
            else if (this.topoPar[1] == GRID_NEIGHBORHOOD_C13) NEIGHBORS = NEIGHBORS_C13;
            else if (this.topoPar[1] == GRID_NEIGHBORHOOD_L4)  NEIGHBORS = NEIGHBORS_L4;
            else NEIGHBORS = NEIGHBORS_C9;
        }
        else NEIGHBORS = NEIGHBORS_C9;

        // Connect them, wrapping around the edges
        for(i=0; i<dim; i++)
        {
            for(j=0; j<dim; j++)
            {
                neighbors = new GraphNode[NEIGHBORS.length];
                for(k=0; k<NEIGHBORS.length; k++)
                    neighbors[k] = getNodeAt(node, i+NEIGHBORS[k][0], j+NEIGHBORS[k][1], dim);
                node[i][j].setNeighbors(neighbors);
            }
        }

        return graph;
    }

    private GraphNode getNodeAt(GraphNode [][]node, int x, int y, int dim)
    {        
        if      (x < 0)    x = dim + x;
        else if (x >= dim) x = x - dim;

        if      (y < 0)   y = dim + y;
        else if (y >= dim) y = y - dim;

        return(node[x][y]);
    }


    public Graph createDegreeDistribution() throws GraphException
    {
        int         i;
        Graph       graph;
        GraphNode   nodenow;
        GraphNode []node;

        // Make a Graph with disconnected nodes
        graph = new Graph();
        node  = new GraphNode[this.numNodes];
        for (i=0; i<this.numNodes; i++)
        {
            nodenow = new Node();
            node[i] = nodenow;
            graph.add(nodenow);
        }

        EmpiricalWalker pk;
        int             degree, j, k, neipos;
        GraphNode     []nodenei;
        boolean       []neimark;
        RandomEngine    rand;
        Uniform         posrand;

        neimark = new boolean[this.numNodes];
        rand    = new DRand();
        posrand = new Uniform(0, this.numNodes-1, rand);
        pk      = new EmpiricalWalker(this.topoPar, Empirical.NO_INTERPOLATION, rand);

        // For all nodes
        for (i=0; i<this.numNodes; i++)
        {
            // Connect with a number of other nodes according to the degree distribution
            degree = pk.nextInt();
            if (degree < this.numNodes-1)
            {
                // Select the nodes to connect to at random
                for (j=0; j<neimark.length; j++) neimark[j] = false;
                for (j=0; j<degree; j++)
                {
                    neipos = posrand.nextInt();
                    while((neipos == j) || neimark[neipos])
                    {
                        if (neipos == this.numNodes-1) neipos = 0;
                        else                           neipos++;
                    }
                    neimark[neipos] = true;
                }

                // Set the neighbors of the current node to the selected nodes
                k       = 0;
                nodenei = new GraphNode[degree];
                for (j=0; j<neimark.length; j++)
                {
                    if (neimark[j]) nodenei[k++] = node[j];
                }
                node[i].setNeighbors(nodenei);

                // Also connect the other way around
                for (k=0; k<nodenei.length; k++)
                {
                    if (!nodenei[k].hasNeighbor(node[i])) nodenei[k].addNeighbor(node[i]);
                }
            }
        }

        return(graph);
    }


    public Graph createGrow() throws GraphException
    {
        Graph     graph;
        GraphNode node;
        int       i;

        // Add the specified number of nodes to the Graph
        graph = new Graph();
        for (i=0; i<this.numNodes; i++)
        {
            node = new Node();
            addNode(graph, node);
        }
        this.graph = graph;

        return(graph);
    }

    public Graph getGraph()
    {
        return(this.graph);
    }

    private void addNode(Graph graph, GraphNode newnode) throws GraphException
    {
        Map  nodeMap;

        // Check if the Graph already contains this node.
        nodeMap = graph.getNodeMap();

        // Chose the neighbors of the new graph according to the desired topology.
        if      (this.topology == TOPOLOGY_RING)       addToRing(nodeMap, newnode);
        else if (this.topology == TOPOLOGY_CONNECTED)  addToConnected(graph, newnode);
        else if (this.topology == TOPOLOGY_SCALE_FREE) addToScaleFree(graph, newnode);
        else if (this.topology == TOPOLOGY_RANDOM)     addToRandom(graph, nodeMap, newnode);
        else throw new GraphException("Unsupported topology.");

        // Add the new node to the Graph
        graph.add(newnode);
    }

    // *********************************************************\
    // *             Enforced Degree Distribution              *
    // *********************************************************/


    // *********************************************************\
    // *         Deterministic Topology Construction           *
    // *********************************************************/
    private void addToRing(Map nodeMap, GraphNode newnode) throws GraphException
    {
        if      (nodeMap.size() == 0)    // First Node in the ring.
        {
            this.firstNode = newnode;
        }
        else if (nodeMap.size() == 1)    // Second Node in the ring
        {
            GraphNode singleNode = (GraphNode)nodeMap.values().iterator().next();
            newnode.setNeighbors(new GraphNode[]{singleNode});
            singleNode.setNeighbors(new GraphNode[]{newnode});
        }
        else                            // 2 Nodes or more Nodes already there
        {
            newnode.setNeighbors(this.firstNode.getNeighbors());
            firstNode.setNeighbors(new GraphNode[]{newnode});
        }
    }

    private void addToConnected(Graph graph, GraphNode nodenew) throws GraphException
    {
        int        i;
        GraphNode  []nei;
        LinkedList neilist;
        LinkedList neinowlist;
        GraphNode  []neinow;

        // All nodes has all other nodes as neighbor
        nei     = graph.getNodes();
        neilist = new LinkedList(Arrays.asList(nei));
        neilist.addLast(nodenew);
        for (i=0; i<nei.length; i++)
        {
            neinowlist = (LinkedList)neilist.clone();
            neinowlist.remove(nei[i]);
            neinow     = (GraphNode [])neinowlist.toArray(new GraphNode[]{});
            nei[i].setNeighbors(neinow);
        }
        neinowlist = (LinkedList)neilist.clone();
        neinowlist.remove(nodenew);
        neinow     = (GraphNode [])neinowlist.toArray(new GraphNode[]{});
        nodenew.setNeighbors(neinow);
    }

    // *********************************************************\
    // *            Random Graph Model Construction            *
    // *********************************************************/
    private void addToRandom(Graph graph, Map nodeMap, GraphNode nodenew) throws GraphException
    {
        int        i,j;
        double     p;
        int        numupdate;
        GraphNode  []node;
        LinkedList nodelist;
        LinkedList lnn;
        GraphNode  []neinow;

        p         = topoPar[0];
        numupdate = (int)topoPar[1];
        if ((nodeMap.size()+1) % numupdate == 0)  // If the time to re-wire is there.
        {
            // For all nodes. Select a random portion 'p' of the nodes to connect to.
            node     = graph.getNodes();
            nodelist = new LinkedList(Arrays.asList(node));
            nodelist.add(nodenew);
            node     = (GraphNode [])nodelist.toArray(new GraphNode[]{});
            for (i=0; i<node.length; i++)
            {
                lnn = new LinkedList();
                for (j=0; j<node.length; j++)
                {
                    if ((i!=j) && (Uniform.staticNextDouble() <= p)) lnn.addLast(node[j]);
                }
                neinow = (GraphNode [])lnn.toArray(new GraphNode[]{});
                node[i].setNeighbors(neinow);
            }

            // Make the connections back
            for (i=0; i<node.length; i++)
            {
                for (j=0; j<node.length; j++)
                {
                    if (node[j].hasNeighbor(node[i]))
                    {
                        node[i].addNeighbor(node[j]);
                    }
                }
            }
        }
    }

    // *********************************************************\
    // *         Scale-Free Graph Model Construction           *
    // *********************************************************/
    private void addToScaleFree(Graph graph, GraphNode nodenew) throws GraphException
    {
        int        i;
        int        numNodes;
        int        m;
        int        rind;
        double     ppa;
        GraphNode  []node;
        GraphNode  []neinodes;
        LinkedList neilist;
        LinkedList neilistnow;
        GraphNode  []neinow;

        m = (int)topoPar[0];

        numNodes = graph.getNumberOfNodes();
        if (numNodes <= m)
        {
            // In the beginning treat as connected graph.
            neinodes = graph.getNodes();
            nodenew.setNeighbors(neinodes);

            neilist = new LinkedList(Arrays.asList(neinodes));
            neilist.addLast(nodenew);
            for (i=0; i<neinodes.length; i++)
            {
                neilistnow = (LinkedList)neilist.clone();
                neilistnow.remove(neinodes[i]);
                neinow = (GraphNode[])neilistnow.toArray(new GraphNode[]{});
                neinodes[i].setNeighbors(neinow);
            }

            // Total number of edges
            this.sumDegree = numNodes*(numNodes-1);
        }
        else
        {
            node = graph.getNodes();

            // Connect the new node according to scale-free model
            i        = 0;
            neilist  = new LinkedList();
            while (i < m)
            {
                // Choose a Random Node that is not connected yet.
                rind = Uniform.staticNextIntFromTo(0, node.length-1);
                if (!neilist.contains(node[rind]))
                {
                    // Connect acoording to Preferential Attachment PDF
                    ppa = node[rind].getDegree() / this.sumDegree;
                    if (Uniform.staticNextDouble() <= ppa)
                    {
                        neilist.add(node[rind]);
                        i++;
                    }
                }
            }
            neinodes = (GraphNode [])neilist.toArray(new GraphNode[]{});
            for (i=0; i<neinodes.length; i++)
            {
                nodenew.addNeighbor(neinodes[i]);
                neinodes[i].addNeighbor(nodenew);
            }

            // Recalculate sum of degress
            this.sumDegree = m;
            for (i=0; i<node.length; i++) this.sumDegree += node[i].getDegree();
        }
    }
}
