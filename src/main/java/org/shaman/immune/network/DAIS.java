/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *              Artificial Immune Systems                *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2005 Shaman Research                   *
\*********************************************************/
package org.shaman.immune.network;

import org.shaman.graph.Graph;
import org.shaman.graph.GraphException;
import org.shaman.graph.GraphFactory;
import org.shaman.graph.Node;
import org.shaman.immune.core.Body;
import org.shaman.learning.Presenter;


// *********************************************************\
// *           Distributed Artificial Immune System        *
// *********************************************************/
public class DAIS
{
    // *********************************************************\
    // *        Network Topology and DAIS Parameters           *
    // *********************************************************/
    public static final int TYPE_MONITOR    = 0;         // Only report about the node's local health. Do not exchange immune system data.
    public static final int TYPE_POPULATION = 1;         // Share an immune space. Protect same 'kind' of body.
    public static final int TYPE_BODY       = 2;         // Share all immune system data. Protect the same virtual body.
    
    private String   name;                  // The unique name of this DAIS
    
    // Type of Distributed Immune System
    private int      type;                  // What kind of Distributed AIS
    
    // Network Graph Topology
    private int      topology;              // The topology of the network Graph
    private double []topoPar;               // Parameter of the Topology
    private int      numNodes;              // Number of nodes in the graph 
    
    // Node Activiation Parameters
    private int    trigger;                 // Live TRIGGER type (TIMED or NOTIFIED)
    private long   period;                  // Trigger Period in the case of a TRIGGER_TIMED
    private String []nots;                  // Notification Triggers in tha case of a TRIGGER_NOTIFIED
    
    // Distributed Body Type Parameters
    private Body      bodyStem;             // Body Stem containing the Immune Space and other Static parameters
    private Presenter selfDataSet;          // The DataSet containing the Self set
    private double    pFlowDet;             // Probability that a detector Flows to a neighbor on Activation
    private int       numDet;               // Number of Detectors to generate during Node initialization
    
    // ------ Internal Data ----------
    private Graph    graph;              // The Network Graph
    
    // *********************************************************\
    // *      Initialization and Parameter Specification       *
    // *********************************************************/
    public void init() throws DAISException
    {
        try
        {
            // Make a network graph
            GraphFactory graphfact = new GraphFactory(this.topology, this.numNodes, this.topoPar);
            this.graph = graphfact.create();
        }
        catch(GraphException ex) { throw new DAISException(ex); }
    }
    
    public void setTopology(int _topology, int _numNodes, double []_topoPar)
    {
        topology = _topology;
        topoPar  = _topoPar;
        numNodes = _numNodes;
    }
    
    public void setType(int _type) { type = _type; }
    
    // Distributed Body Parameters
    public void      setBodyStem(Body _bodyStem)                { bodyStem = _bodyStem; }
    public Body      getBodyStem()                              { return(bodyStem); }
    public void      setBodySelfDataSet(Presenter _selfDataSet) { selfDataSet = _selfDataSet; }
    public Presenter getBodySelfDataSet()                       { return(selfDataSet); }
    public void      setBodyNumberOfDetectors(int _numDet)      { numDet = _numDet; }
    public int       getBodyNumberOfDetectors()                 { return(numDet); }
    public void      setBodyPFlowDetector(double _pFlowDet)     { pFlowDet = _pFlowDet; }
    public double    getBodyPFlowDetector()                     { return(pFlowDet); }
    
    // Node Activation Parameters
    public void setTriggerTimed(long _period)
    {
        trigger = Live.TRIGGER_TIMER;
        period  = _period;
    }
    public void setTriggerNotified(String []_nots)
    {
        trigger = Live.TRIGGER_NOTIFICATION;
        nots    = _nots;
    }
    public int      getTriggerType()          { return(trigger); }
    public long     getTriggerPeriod()        { return(period); }
    public String []getTriggerNotifications() { return(nots); }
    
    // *********************************************************\
    // *    Add to Network Graph and Initialize a New Node     *
    // *********************************************************/
    public void register(Node newNode) throws DAISException
    {
        //try
        {
            // Add the node to the Graph
            //graph.add(newNode);
        }
        //catch(GraphException ex) { throw new DAISException(ex); }
    }
    
    // *********************************************************\
    // *            Remove the Node from the Network           *
    // *********************************************************/
    public void deregister(Node oldNode) throws DAISException
    {
    }
    
    // *********************************************************\
    // *                       Member Access                   *
    // *********************************************************/
    public Graph  getGraph()             { return(graph); }
    public int    getType()                { return(type); }
    public int    getTopology()            { return(topology); }
    public double []getTopologyParameter() { return(topoPar); }
    
    // *********************************************************\
    // *                      Constructor                      *
    // *********************************************************/
    public DAIS(String _name)
    {
        name = _name;
    }
}
