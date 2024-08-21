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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ShamanException;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.LearnerException;


/**
 * <h2>Network of Transformation</h2>
 * Group Transformations in a data-flow network
 * that acts as a Transformation itself. Can be
 * used to group together building blocks to
 * implement a more complicated action.
 *
 * e.g. MLP Classifier Network.
 *      PCA -) Standardization -) MLP
 *      Bayesian Classifier Network
 *      Discretization -) Naive Bayes
 */

// *********************************************************\
// *               Transformation Network                  *
// *********************************************************/
public class TransformationNetwork extends Transformation implements Persister
{
   // The number of input and output ports.
   protected int  numIn;
   protected int  numOut;

   // The Transformation in the Network
   protected int            [][]con;       // The connections [][0] = Source Transformation Index. (-1 is this)
                                           //                 [][1] = Source Output Port.
                                           //                 [][2] = Destination Transformation Index. (-1 is this)
                                           //                 [][3] = Destination Input Port.
   protected String         []traname;     // The Transformation's Class Names
   protected int            [][]tragrow;   // The Transformation's grow() method's parameter(s).
   protected int            [][]inMap;     // [i][0] : Input Port i is mapped on trans[[i][0]]
                                           // [i][1] : Input Port i "                        "'s input Port [i][1]
   protected int            [][]outMap;    // [i][0] : Output Port i is mapped on trans[[i][0]]
                                           // [i][1] : Output Port i "                        "'s output Port [[i][1]]

   // The Transformations in the Network
   protected PortMapping    idIn;          // Input Identity Transformation
   protected PortMapping    idOut;         // Output Identity Transformation
   protected Transformation []trans;       // The transformations
   
   // Debug Output storage of the internal structure
   private   NetworkNode       []netnodes;
   private   NetworkConnection []netcons;
   
   // **********************************************************\
   // *           Transformation Network Persistence           *
   // **********************************************************/
   public void loadState(ObjectInputStream oin) throws ConfigException
   {
       try
       {
           int i;
           
           // Read network topology.
           this.numIn   = oin.readInt();
           this.numOut  = oin.readInt();
           this.con     = (int [][])oin.readObject();
           this.traname = (String [])oin.readObject();
           this.tragrow = (int [][])oin.readObject();
           
           // Re-Grow and Re-Populate the Network.
           grow(this.numIn, this.numOut);
           
           if (this.tragrow == null) populate(this.traname, this.con);
           else                      populate(this.traname, this.tragrow, this.con);
           
           // Recursively load the State of the network's Transformations
           for (i=0; i<this.trans.length; i++)
           {
                if (this.trans[i] instanceof Persister) ((Persister)this.trans[i]).loadState(oin);
           }
       }
       catch(ClassNotFoundException ex) { throw new ConfigException(ex); }
       catch(IOException ex)            { throw new ConfigException(ex); }
   }
   
   public void saveState(ObjectOutputStream oout) throws ConfigException
   {
       try
       {
           int i;
           
           // Write network topology
           oout.writeInt(this.numIn);
           oout.writeInt(this.numOut);
           oout.writeObject(this.con);
           oout.writeObject(this.traname);
           oout.writeObject(this.tragrow);
           
           // Write States of individual network Transformations
           for (i=0; i<this.trans.length; i++)
           {
               if (this.trans[i] instanceof Persister) ((Persister)this.trans[i]).saveState(oout);
           }
       }
       catch(IOException ex) { throw new ConfigException(ex); }
   }
   
   // **********************************************************\
   // *                    Debug Output                        *
   // **********************************************************/
   public String toString()
   {
      int i;
      
      StringBuffer netbuf = new StringBuffer();
      if ((netnodes != null) && (netcons != null))
      {
        netbuf.append("***** "+getName()+" *******\n");
        for (i=0; i<netnodes.length; i++) netbuf.append("\t"+netnodes[i].toString()+"\n");
        netbuf.append("-----\n");
        for (i=0; i<netcons.length; i++)  netbuf.append("\t"+netcons[i].toString()+"\n");
      }
      else netbuf.append("Unkown internal structure");
      
      return(netbuf.toString());
   }

  
   // **********************************************************\
   // *      Transformation Network Building and Access        *
   // **********************************************************/
   public Set getTransformationsOfClassRecursive(String classname)
   {
       Set                   trfound;
       int                   i;
       Transformation        trnow;
       TransformationNetwork trnet;
       
       trfound = new HashSet();
       for (i=0; i<this.trans.length; i++)
       {
           // Look for Transformation of the given class, also in the sub-networks.
           trnow = this.trans[i];
           if (TransformationNetwork.class.isAssignableFrom(trnow.getClass()))
           {
               trnet = (TransformationNetwork)trnow;
               trfound.addAll(trnet.getTransformationsOfClassRecursive(classname));
           }
           else
           {
               if (classname.equals(trnow.getClass().getName())) trfound.add(trnow);
           }
       }
       
       return(trfound);
   }
   
   /**
    * Give the PortMapping transformation for this network's input ports.
    * @return The input ports' PortMapping transformation.
    */
   public PortMapping getInputPortMapping()
   {
      return(this.idIn);
   }
   
   /**
    * Give the PortMapping transformation for this network's output ports.
    * @return The output ports' PortMapping transformation
    */
   public PortMapping getOutputPortMapping()
   {
      return(this.idOut);
   }
   
   /**
    * Give the internal Transformation with the given index. 
    * @param ind The index of the internal Tranformation
    * @return The internal Transformation with the given index. 
    */
   public Transformation getTransformation(int ind) 
   {
     return(this.trans[ind]);
   }
   
   /**
    * Give the index of the internal Transformation with the given name
    * @param trname The name of the Transformation
    * @return The index of the Transformation with the given name
    * @throws LearnerException If no Transformation with the given name was found.
    */
   public int getTransformationIndex(String trname) throws ConfigException
   {
      boolean        found;
      int            i, indfound;
     
      found    = false;
      indfound = -1;
      for (i=0; (i<this.trans.length) && (!found); i++)
      {
         if (this.trans[i].getName().equals(trname)) { found = true; indfound = i; }
      }
      if (!found) throw new ConfigException("Cannot find Transformation with name '"+trname+"' in the network");
   
      return(indfound);
   }
   
   /**
    * Check if a Tranformation with the given name is present in this network.
    * @param trname The name of the Tranformation to look for.
    * @return <code>true</code> if there is a Tranformation with the given name.
    */
   public boolean hasTransformationWithName(String trname)
   {
      boolean found;
      int     i;
     
      found = false;
      for (i=0; (i<this.trans.length) && !found; i++)
      {
         if (this.trans[i].getName().equals(trname)) found = true;
      }
      
      return(found);
   }
   
   /**
    * Give the internal Transformation with the given name.
    * @param trname The name of the Transformation
    * @return The Transformation with the given name
    * @throws LearnerException If no Tranformation with the given name is found.
    */
   public Transformation getTransformation(String trname) throws ConfigException
   {
     Transformation trfound;
     boolean        found;
     int            i;
     
     found   = false;
     trfound = null;
     for (i=0; (i<this.trans.length) && (!found); i++)
     {
        if (this.trans[i].getName().equals(trname)) { found = true; trfound = this.trans[i]; }
     }
     if (!found) throw new ConfigException("Cannot find Transformation '"+trname+"' in the network");
     
     return(trfound);  
   }

  /**
   * Give an array of the internal Transformations of this network.
   * @return The array of internal Transformations.
   */
   public Transformation []getTransformations()
   {
     return(this.trans);
   }
   
   /**
    * Check if the given Tranformation is part of this network.
    * @param flow The Transformation to look for.
    * @return <code>true</code> if the given Transformation was found. <code>false</code> Otherwise.
    */
   public boolean containsTransformation(Transformation flow)
   {
      int     i;
      boolean found;
      
      found = false;
      for (i=0; i<this.trans.length; i++) if (this.trans[i] == flow) found = true;
      
      return(found);
   }
   
   /**
    * Check if a Transformation with the given name is part of the network.
    * @param traname The name of the transformation to look for.
    * @return <code>true</code> if the given Transformation was found. <code>false</code> Otherwise.
    */
   public boolean containsTransformation(String traname)
   {
       int     i;
       boolean found;
       
       found = false;
       for (i=0; i<this.trans.length; i++) if (this.trans[i].getName().equals(traname)) found = true;
       
       return(found);
   }
   
   /**
    * Give the NetworkNodes array used in populating this network.
    * @return The NetworkNode array used in populate.
    *          <code>null</node> if the network was not built using the NetworkNode/Connection populate method.
    */
   public NetworkNode []getNetworkNodes()
   {
      return(this.netnodes);
   }
   
   /**
    * Give the NetworkConnection array used in populating this network.
    * @return The NetworkConnection array used in populate.
    *         <code>null</code> if the network was not built using the NetworkNode/Connection popuilate method.
    */
   public NetworkConnection []getNetworkConnections()
   {
      return(this.netcons);
   }

   // **********************************************************\
   // *                Customized Flow Behavior                *
   // **********************************************************/
   /**
    * Checks the data-model fit with the internal Transformation connected to the given input port.
    * @param port The input port number to check the given DataModel against
    * @param dm The DataModel to check
    * @throws ShamanException If the DataModel does not fit. 
    */
   public void checkDataModelFit(int port, DataModel dm) throws ConfigException
   {
     if (port < getNumberOfInputs())
     {
       int i;

       for (i=0; i<this.con.length; i++)
       {
          if ((this.con[i][0] == -1) && (this.con[i][1] == port)) trans[this.con[i][2]].checkDataModelFit(this.con[i][3], dm);
       }
     }
     else throw new ConfigException("Input Port index out of bound. "+port+" >= "+getNumberOfInputs());
   }

   // Connecting Flows. Re-direct connections to the network's input and output Identity Transformations.
   // ----------------
   /**
    * Register an output port of a given Flow as supplier to the given inputport of this network.
    * Actually registers the supplier to this network input PortMapping transformation.
    * @param port The input port to register the supplier with.
    * @param supplier The supplier flow
    * @param supplierPort The output port for the supplier
    */
   public void registerSupplier(int port, DataFlow supplier, int supplierPort) throws ConfigException
   { 
      this.idIn.registerSupplier(port, supplier, supplierPort);
   }
   
   /**
    * Register an input port of a given Flow as consumer to the given outputport of this network.
    * Actually registers the output PortMapping to the consumer and also registers to the output
    * PortMapping as the supplier for the given consumer inputport.
    * @param port The output port to register the consumer with
    * @param consumer The consumer flow
    * @param consumerPort The input port of the consumer to register with 
    */
   public void registerConsumer(int port, DataFlow consumer, int consumerPort) throws ConfigException
   {
      this.idOut.registerConsumer(port, consumer, consumerPort);
      ((DataFlow)consumer).registerSupplier(consumerPort, this.idOut, port);
   }

   public void unRegisterSupplier(int port) throws DataFlowException
   {
      OutputRegistration [][]ors;
      OutputRegistration   []orsport;
      int                    i,j;

      // Unregister Input Indentity as Comsumer first.
      ors     = ((Transformation)this.idIn.getSupplier(port)).getOutputRegistrations();
      for (i=0; i<ors.length; i++)
      {
        orsport = ors[i];
        if (orsport != null)
        {
          for (j=0; j<orsport.length; j++)
          {
             if (orsport[j].getConsumer() == idIn)
             {
                ((DataFlow)this.idIn.getSupplier(port)).unRegisterConsumer(i, this.idIn);
             }
          }
        }
      }
      
      // Unregister Supplier from Input Identity
      idIn.unRegisterSupplier(port);
   }

   public void unRegisterConsumer(int port, DataFlow consumer) throws DataFlowException
   {
      int                 i;
      InputRegistration []sups;
      InputRegistration   ir;
      int                 conport;

      // Unregister it as Supplier first.
      conport = -1;
      sups    = ((Transformation)consumer).getInputRegistrations();
      for (i=0; i<sups.length; i++)
      {
        ir = sups[i];
        if (ir.getSupplier() == this.idOut) conport = i;
      }
      if (conport != -1) ((Transformation)consumer).unRegisterSupplier(conport);

      // Unregister in Output Identity
      this.idOut.unRegisterConsumer(port, consumer);
   }
   
   /**
     * Get the consumers registered on the specified output port.
     * Actually returns the consumers on the output port of the output PortMapping of this network.
     * @param port The output port to get the consumers for.
     * @return A list of consumers registered on the port.
     * @throws ShamanException If the given output port is out of bounds.
     */
    public List getConsumers(int outputport) throws DataFlowException
    {
        List lcon;
        
        // Check port bounds.
        if (outputport >= getNumberOfOutputs())
            throw new DataFlowException("Output port "+outputport+" out of bounds in '"+getName()+"'. Only has '"+getNumberOfOutputs()+"' output ports.");
        
        // Re-route the question to the output PortMapping
        lcon = this.idOut.getConsumers(outputport);
        
        return(lcon);
    }
    
    /**
     * Get the supplier of the specified input port.
     * Actually gives the supplier registered with the input PortMapping at the given port.
     * @param port The input port to get the supplier for.
     * @return The supplier that is registered at the given port. <code>null</code> if no supplier is defined for the port.
     * @throws ShamanException If the given input port is out of bounds.
     */
    public DataFlow getSupplier(int inputport) throws DataFlowException
    {
        // Check port bounds.
        if (inputport >= getNumberOfInputs())
             throw new DataFlowException("Input port "+inputport+" out of bounds in '"+getName()+"' Only has '"+getNumberOfInputs()+"' input ports.");

        // Re-route the question to the input PortMapping
        return(this.idIn.getSupplier(inputport));
    }

  // **********************************************************\
  // *                     Transformation                     *
  // **********************************************************/
  /**
   * Initialize the internal Transformations of this network.
   * First initialize the input PortMapping. Then initialize
   * the internal Transformations in the order specified in
   * the population of the network. Then initialize the output
   * PortMapping.
   * @throws ShamanException When something goes wrong while
   *                        initializing an internal Transformation.
   */
  public void init() throws ConfigException
  {
     DataModel  dmin, dmout;
     int        i;

     // Initialize the Input Identity
     this.idIn.init();

     // Install Input Datamodels
     for (i=0; i<numIn; i++)
     {
       dmin = this.idIn.getInputDataModel(i);
       setInputDataModel(i, dmin);
     }

     // Initialize the Network's Transformations.
     for (i=0; i<this.trans.length; i++)
     {
         //System.out.println("Initializing "+getName()+"/"+trans[i]);
         this.trans[i].init();
     }

     // Initialize the Output Identity
     this.idOut.init();

     // Install Output DataModels
     for (i=0; i<numOut; i++)
     {
       dmout = idOut.getOutputDataModel(i);
       setOutputDataModel(i, dmout);
     }
  }
  
  public void cleanUp() throws DataFlowException
  {
      int i;
      
      // Cleanup all the Transformations
      for (i=0; i<this.trans.length; i++)
      {
         this.trans[i].cleanUp();
      }
  }
  
  /**
   * Clear the data-flow queues of the PortMappings
   * and the internal Transformations.
   */
  public void clear()
  {
     int i;
     
     this.idIn.clear();
     for (i=0; i<trans.length; i++) this.trans[i].clear();
     this.idOut.clear();
  }

  public String getOutputName(int port)
  {
    if (port < numOut) return("Output "+port);
    else return(null);
  }

  public String getInputName(int port)
  {
    if (port < numIn) return("Input "+port);
    else return(null);
  }

  public int getNumberOfInputs() { return(numIn);  }
  public int getNumberOfOutputs(){ return(numOut); }

  /**
   * Create a transformation network with the given number of input and output ports.
   * @param _numIn The number of input ports to install
   * @param _numOut The number of output ports to install
   */
  public void grow(int _numIn, int _numOut)
  {
     int  i;

     // Remember the number of in- and output ports
     this.numIn  = _numIn;
     this.numOut = _numOut;

     // Create place for network -> flow port mappings for the input and the output ports
     this.inMap  = new int[numIn][2];
     this.outMap = new int[numOut][2];
     for (i=0; i< inMap.length; i++) {  this.inMap[i][0] = -1;  this.inMap[i][1] = -1; }
     for (i=0; i<outMap.length; i++) { this.outMap[i][0] = -1; this.outMap[i][1] = -1; }

     // Make the input and output ports and datamodel arrays. 
     // They are never used.
     this.inputModel  = new DataModel[numIn];
     this.outputModel = new DataModel[numOut];
     
     // Create the Input and Output Indentity Transformation that (re)-map the input and output ports.
     // External Flows connect to the inputs of these PortMappings.
     // The Internal Flows connect to outputs of them.
     this.idIn  = new PortMapping();
     this.idIn.grow(numIn);
     this.idOut = new PortMapping();
     this.idOut.grow(numOut);
     
     // Finally, create the right number of input/output ports for the network itself
     super.grow(_numIn, _numOut);
  }

  
  /**
   * Configure the internal structure of the Transformation Network.
   * Add the Transformations to the network and connect them to each other as well as
   * to the network's input/output port.
   * @param _traname String array of Class names of classes that inherit from Transformation
   * @param _con     2D integer array with in every row 4 values :
   *                 [][0] = Index in _traname of the source transformation. (-1 for the Network's input port) <br>
   *                 [][1] = Output port of the source transformation. <br>
   *                 [][2] = Index in _traname of the destination transformation (-1 for the Network's output port)<br>
   *                 [][3] = Input port of the destination transformation.<br>
   * @throws LearnerException If one of the network's transformation or connections cannot be made.
   *                          or if there are input/output ports that are not used.
   */
  public void populate(String []_traname, int [][]_con) throws ConfigException
  {
    int [][]tragrow;
    
    // No Grow Sizes are Specified.
    tragrow = new int[_traname.length][];
    for (int i=0; i<tragrow.length; i++) tragrow[i] = null;
    
    populate(_traname, tragrow, _con);
  }

  /**
   * Configure the internal structure of the Transformation Network.
   * Add the Transformations to the network and connect them to each other as well as
   * to the network's input/output port.
   * @param _traname String array of Class names of classes that inherit from Transformation
   * @param _tragrow 2D integer array with in every row the parameters of grow() to be called on the Transformation
   *                 [] = null if no grow() method should be called
   *                 [] = int[1] if grow([][0]) should be called
   *                 [] = int[2] if grwo([][0], [][1]) should be called
   * @param _con     2D integer array with in every row 4 values :
   *                 [][0] = Index in _traname of the source transformation. (-1 for the Network's input port) <br>
   *                 [][1] = Output port of the source transformation. <br>
   *                 [][2] = Index in _traname of the destination transformation (-1 for the Network's output port)<br>
   *                 [][3] = Input port of the destination transformation.<br>
   * @throws LearnerException If one of the network's transformation or connections cannot be made.
   *                          or if there are input/output ports that are not used.
   */
  public void populate(String []_traname, int [][]_tragrow, int [][]_con) throws ConfigException
  {
    int            i,j;
    Class          traclass;
    Transformation tr1, tr2;
    boolean        found;

    this.traname = _traname;
    this.tragrow = _tragrow;
    this.con     = _con;
    i       = 0;
    try
    {
      // Create the internal Transformations.
      this.trans = new Transformation[traname.length];
      for (i=0; i<traname.length; i++)
      {
        // As if they were constructed with the empty constructor.
        traclass = Class.forName(traname[i]);
        Object obj = traclass.newInstance();
        if (!(obj instanceof Transformation))
            throw new ConfigException("Cannot create a TransformationNetwork containing an Object that is not a Transformation but a '"+obj.getClass().getName()+"'");
        this.trans[i] = (Transformation)obj;
        // Check if the new Transformation's grow() method should be called
        Method         growmethod;
        Class          []growmethodpar;
        Object         []growargs;
        
        if (this.tragrow[i] != null)
        {
            // Call the grow() methods with the correct number of parameters.
            try
            {
              growmethodpar = new Class[this.tragrow[i].length];
              growargs      = new Object[this.tragrow[i].length];
              for (j=0; j<this.tragrow[i].length; j++)
              {
                  growmethodpar[j] = Integer.TYPE;
                  growargs[j]      = new Integer(tragrow[i][j]);
              }             
              growmethod    = traclass.getMethod("grow", growmethodpar);
              growmethod.invoke(trans[i], growargs);
            }
            catch(NoSuchMethodException ex)
            {
                throw new ConfigException("Cannot find grow() method in "+traclass.getName()+" with "+tragrow[i].length+" arguments");
            }
            catch(InvocationTargetException ex)
            {
                throw new ConfigException("Error in grow() method in "+traclass.getName()+" with "+tragrow[i].length+" arguments");
            }
        }
      }

      // Connect the Internals of the Flow Network
      for (i=0; i<this.con.length; i++)
      {
        if ((this.con[i][0] != -1) && (this.con[i][2] != -1))
        {
          tr1 = this.trans[con[i][0]];
          tr2 = this.trans[con[i][2]];

          // Check if port number are legal
          if (this.con[i][1] >= tr1.getNumberOfOutputs())
            throw new ConfigException("Cannot connect output of '"+tr1.getName()+"' to input of '"+tr2.getName()+"'. Output port "+this.con[i][1]+" out of bounds.");
          if (this.con[i][3] >= tr2.getNumberOfInputs())
            throw new ConfigException("Cannot connect output of '"+tr1.getName()+"' to input of '"+tr2.getName()+"'. Input port "+this.con[i][3]+" out of bounds.");

          tr2.registerSupplier(this.con[i][3], tr1, this.con[i][1]);
          tr1.registerConsumer(this.con[i][1], tr2, this.con[i][3]);
        }
      }

      // Find out how the network is connected to the outside world
      this.inMap  = new int[numIn][2];
      this.outMap = new int[numOut][2];
      for (i=0; i<numIn; i++)
      {
        found = false;
        this.inMap[i][0] = -1; this.inMap[i][1] = -1;
        for (j=0; j<this.con.length; j++)
        {
          if ((this.con[j][0] == -1) && (this.con[j][1] == i))
          {
            this.inMap[i][0] = this.con[j][2];
            this.inMap[i][1] = this.con[j][3];
            found       = true;
          }
        }
        if (!found) throw new ConfigException("Flow Network contains an unconnected Input Port : port "+i);
      }
      for (i=0; i<numOut; i++)
      {
        found = false;
        this.outMap[i][0] = -1; this.outMap[i][1] = -1;
        for (j=0; j<this.con.length; j++)
        {
          if ((this.con[j][2] == -1) && (this.con[j][3] == i))
          {
            this.outMap[i][0] = this.con[j][0];
            this.outMap[i][1] = this.con[j][1];
            found = true;
          }
        }
        if (!found) throw new ConfigException("Flow Network contains an unconnected Output Port : port "+i);
      }

      // Connect internal transformations to the Identity Transformations
      Transformation tranow;
      
      for (i=0; i<this.con.length; i++)
      {
        if (this.con[i][0] == -1)
        {
          if (con[i][2] == -1) tranow = this.idOut;
          else                 tranow = this.trans[con[i][2]];
            
          if (tranow.getNumberOfInputs() <= this.con[i][3])
             throw new ConfigException("Cannot connect network input "+this.con[i][1]+" to '"+tranow.getName()+"'. Input port "+this.con[i][3]+" out of bounds.");
          tranow.registerSupplier(this.con[i][3], this.idIn, this.con[i][1]);
          
          if (this.idIn.getNumberOfOutputs() <= this.con[i][1])
            throw new ConfigException("Network input "+this.con[i][1]+" out of bounds. Can't connect "+this.trans[this.con[i][2]].getName()+" to input.");
          this.idIn.registerConsumer(this.con[i][1], tranow, this.con[i][3]);
        }

        if (this.con[i][2] == -1)
        {
          if (this.con[i][0] == -1) tranow = this.idIn;
          else                      tranow = this.trans[this.con[i][0]];
            
          if (this.idOut.getNumberOfOutputs() <= this.con[i][3])
            throw new ConfigException("Network Output "+this.con[i][3]+" out of bounds. Can't connect "+tranow.getName()+" to output.");
          this.idOut.registerSupplier(this.con[i][3], tranow, this.con[i][1]);
          if (tranow.getNumberOfOutputs() <= this.con[i][1])
            throw new ConfigException("Connect connect '"+tranow.getName()+"' to network output "+this.con[i][3]+". Output port "+this.con[i][1]+" out of bounds.");
          tranow.registerConsumer(this.con[i][1], this.idOut, this.con[i][3]);
        }
      }
    }
    catch(IllegalAccessException ex) { throw new ConfigException(ex); }
    catch(InstantiationException ex) { throw new ConfigException(ex); }
    catch(ClassNotFoundException ex) { throw new ConfigException("Cannot create TransformationNetwork containing an unknown type of Transformation class : "+traname[i]); }
  }
  
 /**
  * Use an array of named nodes and an array of connection to populate the network.
  * @param nnod Array of Network Node descriptions
  * @param ncon Arrayof Network Connection descriptions referencing the nodes.
  * @throws LearnerException If the translation to internal format cannot be made or the internal population method fails.
  */
  public void populate(NetworkNode []nnod, NetworkConnection []ncon) throws ConfigException
  {
     int               i;
     ArrayList         alnod;
     NetworkNode       nodnow;
     NetworkConnection connow;
     String            namenow;
     String          []trnames;
     int             [][]trcon;
     int             [][]trgrow;
     
     // Remember for later reference
     this.netnodes = nnod;
     this.netcons  = ncon;
     if (netnodes.length == 0) throw new ConfigException("Cannot create empty transformation network.");
     
     // Make the arrays of Class-names and Grow arguments.
     alnod   = new ArrayList();
     trnames = new String[nnod.length];
     trgrow  = new int[nnod.length][];
     for (i=0; i<nnod.length; i++)
     {
        nodnow       = (NetworkNode)nnod[i];
        namenow      = nodnow.getClassName();
        alnod.add(nodnow.getName());
        
        trnames[i]   = namenow;
        trgrow[i]    = nodnow.getGrow();
     }
     
     // Make the connections array
     trcon = new int[ncon.length][4];
     for (i=0; i<ncon.length; i++)
     {
        connow = ncon[i];
        if (connow.getSourceName() == null) trcon[i][0] = -1;
        else
        {
          trcon[i][0] = alnod.indexOf(connow.getSourceName());
          if (trcon[i][0] == -1) throw new ConfigException("Cannot find Source Transformation '"+connow.getSourceName()+"'");
        }
        if (connow.getDestinationName() == null) trcon[i][2] = -1;
        else
        {
          trcon[i][2] = alnod.indexOf(connow.getDestinationName());
          if (trcon[i][2] == -1) throw new ConfigException("Cannot find Destination Transformation '"+connow.getDestinationName()+"'");
        }
        trcon[i][1] = connow.getSourcePort();
        trcon[i][3] = connow.getDestinationPort();
     }
     
     // Populate the Network
     populate(trnames, trgrow, trcon);
     
     // Rename the Transformations to the given names
     for (i=0; i<nnod.length; i++)
     {
       nodnow = (NetworkNode)nnod[i];
       this.trans[i].setName(nodnow.getName());
       this.trans[i].setDescription(nodnow.getDescription());
     }
  }
  

  public TransformationNetwork()
  {
    super();
    this.name        = "TransformationNetwork";
    this.description = "Flow Network";
    this.numIn       = 0;
    this.numOut      = 0;
    this.trans       = null;
    this.idIn        = null;
    this.idOut       = null;
    this.con         = null;
    this.traname     = null;
    this.inMap       = null;
    this.outMap      = null;
  }
}
