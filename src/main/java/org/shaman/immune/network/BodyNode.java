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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.shaman.exceptions.LearnerException;
import org.shaman.graph.GraphException;
import org.shaman.graph.GraphNode;
import org.shaman.graph.Node;
import org.shaman.immune.core.Body;
import org.shaman.immune.core.Detector;
import org.shaman.immune.core.DetectorSet;
import org.shaman.immune.core.DetectorSetRandom;
import org.shaman.immune.core.Morphology;
import org.shaman.learning.Presenter;

import cern.jet.random.Uniform;


// *********************************************************\
// *       Distributed Artificial Immune System Node       *
// *********************************************************/
public class BodyNode extends Node
{
    // DEBUG AND EXPERIMENT SWITCH : PLEASE IGNORE....
    // Don't make real detectors. Used for speed in antibody migration experiments.
    private boolean DETECTORS_BOGUS = true;
    // ----------------------------------------------------------------------------
    
    // *********************************************************\
    // *           Distributed AIS Node Parameters             *
    // *********************************************************/
    // Node State Machine
    public static final int STATE_NEW         = 0;   // Just created. Pre-initialized
    public static final int STATE_INITIALIZED = 1;   // Initialized local parameters
    public static final int STATE_REGISTERED  = 2;   // Joined in network graph. Received structural data.
    public static final int STATE_LIVE        = 3;   // Live. Check liveState for further state details.
    int    state;
    
    // Identification
    private String   name;             // The name of this Node (does NOT have to be unique) e.g. location of machine of machine it's running on
    private String   ID;               // The Unique ID of this Node
    
    // Distibuted Artificial Immune System connection
    private GraphNode []neighbor;        // End point of this Node's Edges in the Network Graph
    private DAIS      dais;              // The DAIS in which this node is active
    private int       daisType;          // Type of DAIS
    
    // The body running at this node
    private Body   bod;
    
    // Activation Parameters
    private int     _trigger;            // Activiation Parameters from the DAIS used to start the
    private long    _period;             // Live sub-class.
    private String  []_nots;
    
    // Distributed Body Parameters
    // ---------------------------
    private int    numDet;               // Number of Detectors to generate during initialization
    private double pFlowDet;             // Probability that a detector migrates to a neighbor during activation
    
    private HashMap inDet;               // Map of incomming detectors of the last timestep. (id, detector [])
    
    // *********************************************************\
    // *   Dynamic Complex Network Construction / Maintenance  *
    // *********************************************************/
    private BodyNode getNeighborNode(int i) // Access to the neighbors AIS Node Structure
    {
        return((BodyNode)neighbor[i]);
    }
    
    public int getNumberOfDetectors()
    {
        int      i;
        int      numdet;
        Detector []det;
        
        numdet = 0;
        det    = getBody().getDetectorSet().getDetectors();
        for (i=0; i<det.length; i++) if (det[i] != null) numdet++;
        
        return(numdet);
    }
    
    public void addToScaleFree(Node newnode, int m) throws GraphException
    {
        int       i, rind;
        int       numNodes;
        double    sumdegree;
        GraphNode []node;
        GraphNode []neinodes;
        double    ppa;
        
        // Make a array of the nodes in the local Sub Graph
        exploreLocalSubGraph();
        node     = getLocalNodes();
        numNodes = node.length;
        
        if (numNodes <= m)
        {
            // In the beginning treat as connected graph.
            neinodes = node;
        }
        else
        {
            // Connect according to scale-free model
            sumdegree = 0;
            for (i=0; i<node.length; i++) sumdegree += node[i].getDegree();
            
            neinodes = new GraphNode[m];
            i = 0;
            while (i < m)
            {
                // Choose a Random Node
                rind = Uniform.staticNextIntFromTo(0, node.length-1);
                
                // Connect acoording to Preferential Attachment PDF
                ppa = node[rind].getDegree() / sumdegree;
                //if (Random.generate(Random.BERNOULLI, ppa, 0) == 1.0)
                if (Uniform.staticNextDouble() <= ppa)
                {
                    neinodes[i] = node[rind]; i++;
                }
            }
        }
        
        // Add edges from the new node to it's neighbors and vice-versa.
        for (i=0; i<neinodes.length; i++)
        {
            newnode.addNeighbor(neinodes[i]);
            neinodes[i].addNeighbor(newnode);
        }
    }
    
    // *********************************************************\
    // *                  DAIS Node Activity                   *
    // *********************************************************/
    public void activate(String not) throws DAISException
    {
        // Active Component of the DAIS Node
        if (daisType == DAIS.TYPE_BODY) activateDistributedBody();
        else throw new DAISException("Only Distributed Boyy DAIS Supported for now.");
    }
    
    private void activateDistributedBody() throws DAISException
    {
        int i;
        // Do the DAIS thing.
        // e.g. exchange antibodies.
        //      tunnel pathogens over
        //      send immune response results to DAIS
        //System.out.println("Active "+name);
        
        // Migrate some antibodies to neighbors.
        sendDetectors();
        
        // Make the neighbors absorb the new antobodies.
        for (i=0; i<neighbor.length; i++) getNeighborNode(i).absorbDetectors();
        
        //System.out.println("Activate "+name);
    }
    
    public void sendDetectors() throws DAISException
    {
        int         i,j;
        DetectorSet detset;
        LinkedList  listout;
        Detector    []detbod;
        Detector    []detout;
        int         []recind;
        int         numout;
        
        // Select the detectors to send according to the pre-defined probability.
        // for all selected detectors, determine the neighbor where it will migrate to
        detset  = bod.getDetectorSet();
        detbod  = detset.getDetectors();
        recind  = new int[detbod.length];
        numout  = 0;
        for (i=0; i<detbod.length; i++)
        {
            if(detbod[i] != null)
            {
                if (Uniform.staticNextDouble() <= pFlowDet)
                {
                    numout++;
                    recind[i] = Uniform.staticNextIntFromTo(0, neighbor.length-1);
                }
                else recind[i] = -1;
            }
        }
        
        // Send the detectors to the neighborss
        for (i=0; i<neighbor.length; i++)
        {
            listout = new LinkedList();
            for (j=0; j<detbod.length; j++) if (recind[j] == i) listout.addLast(detbod[j]);
            detout = (Detector [])listout.toArray(new Detector[]{});
            ((BodyNode)neighbor[i]).receiveDetectors(getID(), detout);
        }
        
        // Delete the sent detectors from this body
        for (i=0; i<recind.length; i++) if (recind[i] != -1) detbod[i] = null;
        detset.setNumberOfDetectors(detset.getNumberOfDetectors() - numout);
    }
    
    synchronized public void receiveDetectors(long sender, Detector []newDet) throws DAISException
    {
        if (inDet == null) inDet = new HashMap();
        inDet.put(new Long(sender), newDet);
    }
    
    synchronized public void absorbDetectors() throws DAISException
    {
        // Add the incomming detectors to this Body's set.
        int         i,j;
        DetectorSet detset;
        Detector  []detsext;
        Detector  []dets;
        HashSet   newdetset;
        Detector  []newDet;
        Iterator  detit;
        int      numfree, numadd;
        
        if (inDet != null)
        {
            // Join the incomming detectors into 1 big array
            newdetset = new HashSet();
            detit     = inDet.values().iterator();
            while (detit.hasNext()) newdetset.addAll(Arrays.asList((Detector [])detit.next()));
            newDet    = (Detector [])newdetset.toArray(new Detector[]{});
            
            // The Detector Set of the Local Body
            detset = bod.getDetectorSet();
            dets   = detset.getDetectors();
            
            // Count the number of free spots.
            numfree = 0;
            for (i=0; i<dets.length; i++) if (dets[i] == null) numfree++;
            
            // If there's not enough space, extend the detector set.
            if (numfree < newDet.length)
            {
                detsext = new Detector[dets.length + (newDet.length - numfree)];
                for (i=0; i<dets.length; i++)    detsext[i] = dets[i];
                for (; i<detsext.length; i++) detsext[i] = null;
                dets = detsext;
            }
            
            // Store the new detectors in the set
            j = 0;
            for (i=0; (i<dets.length) && (j<newDet.length); i++)
            {
                if (dets[i] == null) dets[i] = newDet[j++];
            }
            
            // Clear the incomming detectors buffer.
            inDet.clear();
            
            // Adjust the size
            detset.setNumberOfDetectors(detset.getNumberOfDetectors()+newDet.length);
            detset.setDetectors(dets);
            
            //System.out.println("Absorbing "+newDet.length+" of "+inDet.size()+" sources ");
            //System.out.println("Detector Max Size "+detset.getDetectors().length+" # det "+detset.getNumberOfDetectors());
        }
    }
    
    // *********************************************************\
    // * Initialize the State of the Body running in this Node *
    // *********************************************************/
    private void initDistributedBody() throws DAISException
    {
        Body        bodstem;
        Body        bodnew;
        Presenter   pself;
        Morphology  mor;
        
        // Get the Distributed Body Parameters from the DAIS
        bodstem  = dais.getBodyStem();
        pFlowDet = dais.getBodyPFlowDetector();
        numDet   = dais.getBodyNumberOfDetectors();
        
        // Build a fully function body from the Body Stem of the DAIS
        try
        {
            // Clone the Body
            bodnew = (Body)bodstem.clone();
            
            if (!DETECTORS_BOGUS)
            {
                // Make the (MHC) FieldPosition
                mor = bodnew.getMorphology();
                mor.makeFieldPositions();
                
                // Check if a Self Set has been pre-compiled into the BodyStem
                if (!bodnew.getStoreSelf())
                {
                    // If not. Do it now with the DataSet provided by the DAIS
                    pself = dais.getBodySelfDataSet();
                    if (pself != null) bodnew.compileSelfSet(pself);
                    else throw new DAISException("No Self particles in Stem Body can't get a Self DataSet from DAIS either!");
                }
                
                // Create a new Detector Set
                bodnew.generateDetectors();
            }
            else
            {
                // Create a detector set with random detectors. Used for speed in migration experiments.
                DetectorSet detset;
                Detector    []dets;
                int         i;
                
                dets = new Detector[numDet];
                for (i=0; i<dets.length; i++) dets[i] = bodnew.createDetector();
                
                detset = new DetectorSetRandom(bodnew);
                detset.setDetectors(dets);
                detset.setNumberOfDetectors(numDet);
                bodnew.setDetectorSet(detset);
            }
            
            // Commit Result
            bod = bodnew;
        }
        catch(CloneNotSupportedException ex) { throw new DAISException("Cannot Clone Body Stem.", ex); }
        catch(LearnerException ex)           { throw new DAISException(ex); }
    }
    
    public void setStructure(DAIS _dais) throws DAISException
    {
        if (state == STATE_NEW)
        {
            // Remeber the AIS this node is part of
            dais     = _dais;
            daisType = dais.getType();
            
            // Get the Activation Parameters from the DAIS
            _trigger = dais.getTriggerType();
            _period  = dais.getTriggerPeriod();
            _nots    = dais.getTriggerNotifications();
            
            // Depending on the type of DAIS. Do the appropriate initialization.
            if (dais.getType() == DAIS.TYPE_BODY) initDistributedBody();
            else throw new DAISException("Only support for DAIS type BODY for now.");
            
            // Change state to INITIALIZED. All is ready for joining the DAIS now.
            state = STATE_INITIALIZED;
        }
        else throw new DAISException("Node not in NEW state.");
    }
    
    
    
    /*********************************************************\
     *           Register / Deregister with a DAIS           *
     \*********************************************************/
    public void register(DAIS ais) throws DAISException
    {
        if (state != STATE_NEW) throw new DAISException("Node not in INITIALIZED state.");
        ais.register(this);
    }
    
    public void deregister(DAIS ais) throws DAISException
    {
        if ((state != STATE_LIVE) || (state != STATE_REGISTERED))
            throw new DAISException("Node is not LIVE or REGISTERED.");
    }
    
    
    /*********************************************************\
     *                       Member Access                   *
     \*********************************************************/
    public int  getState() throws DAISException           { return(state); }
    public void setState(int _state) throws DAISException { state = _state; }
    
    public String getName() throws DAISException           { return(name); }
    public void setName(String _name) throws DAISException { name = _name; }
    
    public Body getBody() { return(bod); }
    public void setBody(Body _bod) { bod = _bod; }
    
    public void test(String text) throws DAISException
    {
        System.out.println("Testing "+ID+"  : "+text);
    }
    
    public void print()
    {
        System.out.println(toString());
    }
    
    public String toString()
    {
        int   i;
        String pout;
        
        pout = "";
        try
        {
            pout = getName()+" neighbors :   ";
            if (neighbor != null)
            {
                for (i=0; i<neighbor.length; i++) pout += getNeighborNode(i).getName()+" ";
            }
        }
        catch(DAISException ex) { ex.printStackTrace(); }
        
        return(pout);
    }
    
    /*********************************************************\
     *          Constructor and ID Initialization            *
     \*********************************************************/
    private void makeID()
    {
        int  i;
        char []chid = new char[32];
        
        // Make a 'unique' ID for this node acoording to an ancient recipe.
        for (i=0; i<chid.length; i++)
            //chid[i] = (char)Random.generate(Random.UNIFORM, (double)'0', (double)'9');
            chid[i] = (char)Uniform.staticNextIntFromTo((int)'0', (int)'9');
        ID = new String(chid);
    }
    
    public BodyNode()
    {
        state    = STATE_NEW;
        neighbor = new GraphNode[0];
    }
    
    public BodyNode(String _name)
    {
        name     = _name;
        makeID();
        neighbor = new GraphNode[0];
        state    = STATE_NEW;
    }
    
    public BodyNode(String _name, String _ID)
    {
        name     = _name;
        ID       = _ID;
        neighbor = new GraphNode[0];
        state    = STATE_NEW;
    }
}
