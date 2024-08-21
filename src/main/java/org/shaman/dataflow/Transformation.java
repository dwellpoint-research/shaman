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
import java.util.ArrayList;
import java.util.List;

import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ShamanException;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.TransformationException;


/**
 * <h2>Data Vector Transforming  Base Class</h2>
 * Base class for a DataFlow that can execute a data-transformation
 * on vector based data.
 */

// **********************************************************\
// *  Abstract Base Class for Data Vector Transformations   *
// **********************************************************/
public abstract class Transformation implements DataFlow
{
    /** Connections to the suppliers DataFlows. One per input port. */
    protected InputRegistration    []inputs;
    /** Connections to the consumer DataFlows. One or more per output port. */
    protected OutputRegistration [][]outputs;
    /** The DataModels of the data vectors at the input ports. */
    protected DataModel            []inputModel;
    /** The DataModels of the data vectors at the output ports. */
    protected DataModel            []outputModel;
    /** A short name describing the function of this component */
    protected String name;           
    /** Longer description of the function and capabilities of this component */
    protected String description;
    
    // *** Data Flow Buffers ***
    // Input Port Data Objects.
    private Object []inputData;          // One data object per input port...
    // Input Availability BitVector.
    private long   []inputsAvailable;    // Bit is 1 if input port has data.
    
    // Input availability port masks
    private static final long []portMask;       // [i] =   1<<i   Bit of the port is set
    private static final long []portMaskInvert; // [i] = ~(1<<i)  All bits except the one of the port are set
    static
    {
        portMask       = new long[64];
        portMaskInvert = new long[64];
        for (int i=0; i<64; i++)
        {
           portMask[i]       =   1L<<((long)i);
           portMaskInvert[i] = ~(1L<<((long)i));
        }
    }
    
    // **********************************************************\
    // *               Neighborhoud (de)Construction            *
    // **********************************************************/
    /**
     * Connect this and the 'supplier' FlowBase.<br>
     * Register the supplier-consumer and consumer-supplier relations in
     * the 2 components' data-flow neighborhood definitions.
     * 
     * @param inputPort The input port of this FlowBase to connect the 'supplier' to.
     * @param Supplier  The 'supplier' FlowBase that is connected to this FlowBase.
     * @param supplierPort The output port of the 'supplier' to connect to the 'inputPort' of this FlowBase
     * 
     * @throws ShamanException If one of the ports is out of bounds.
     */ 
    public  void connect(int inputPort, DataFlow supplier, int supplierPort) throws ConfigException
    {
        this.registerSupplier(inputPort, supplier, supplierPort);
        supplier.registerConsumer(supplierPort, this, inputPort);
    }
    
    /**
     * Connect an output port of the given 'supplier' FlowBase to an input port
     * of this FlowBase.
     *
     * @param port The input port of this FlowBase to connect to.
     * @param supplier The 'supplier' FlowBase to connect to. 
     * @param supplierPort The output port of the 'supplier' to connect to.
     * 
     * @throws ShamanException If one the ports is out of bounds.
     */
    public void registerSupplier(int port, DataFlow supplier, int supplierPort) throws ConfigException
    {
        // Check input/output ports bounds.
        if (port >= getNumberOfInputs())
             throw new ConfigException("Input port "+port+" out of bounds in '"+getName()+"'. Only has "+getNumberOfInputs()+" input ports.");
        if (supplierPort >= supplier.getNumberOfOutputs())
             throw new ConfigException("Output port "+supplierPort+" out of bounds in '"+supplier.getName()+"'. Only has "+getNumberOfOutputs()+" output ports.");

        // Register the supplier.
        InputRegistration reg = new InputRegistration(supplierPort, supplier);
        this.inputs[port] = reg;
    }
    
    /**
     * Connect an input port of the given 'consumer' FlowBase to an output port
     * of this FlowBase.
     *
     * @param port The output port of this FlowBase to connect the consumer to.
     * @param consumer The 'consumer' FlowBase.
     * @param consumerPort The input port the 'consumer' to connect to.
     * 
     * @throws ShamanException If one the ports is out of bounds.
     */
    public void registerConsumer(int port, DataFlow consumer, int consumerPort) throws ConfigException
    {
        // Check input/output ports bounds.
        if (port >= getNumberOfOutputs())
             throw new ConfigException("Output port "+port+" out of bounds in '"+getName()+"'. Only has "+getNumberOfOutputs()+" output ports.");
        if (consumerPort >= consumer.getNumberOfInputs())
             throw new ConfigException("Input port "+port+" out of bounds in '"+consumer.getNumberOfInputs()+"'. Only has "+consumer.getNumberOfInputs()+" input ports.");

        // Add the consumer to the list of consumers for the given output port.
        OutputRegistration []regold;
        OutputRegistration []regnew;
        OutputRegistration reg;
        int                i;
        
        reg    = new OutputRegistration(consumerPort, consumer);
        regold = this.outputs[port];
        if (regold == null) regnew = new OutputRegistration[1];
        else                regnew = new OutputRegistration[regold.length+1];
        for (i=0; (regold != null) && (i < regold.length); i++) regnew[i] = regold[i];
        regnew[i] = reg;
        this.outputs[port] = regnew;
    }

    /**
     * Unregisters the supplier on the specified input port.
     *
     * @param port The input port from which the supplier should be removed.
     * 
     * @throws ShamanException If the given input port is out of bounds.
     */
    public void unRegisterSupplier(int inputport) throws DataFlowException
    {
        // Check port bounds
        if (inputport >= getNumberOfInputs())
            throw new DataFlowException("Input port "+inputport+" out of bounds in '"+getName()+"'. Only has "+getNumberOfInputs()+" input ports.");

        // Remove the current input registration for the given port.
        this.inputs[inputport] = null;
    }
    
    /**
     * Unregisters a consumer on the specified output port.
     *
     * @param port The output port to remove a supplier from.
     * @param consumer The consumer to remove.
     * 
     * @throws ShamanException If the given output port is out of bounds or the given consumer is not found there.
     */
    public void unRegisterConsumer(int outputport, DataFlow consumer) throws DataFlowException
    {
        // Check port bounds
        if (outputport >= getNumberOfOutputs())
            throw new DataFlowException("Output port "+outputport+" out of bounds in '"+getName()+"'. Only has '"+getNumberOfOutputs()+"' output ports.");
            
        // Remove the given consumer's outputregistration
        boolean            found;
        int                i, posfound;
        OutputRegistration []outreg;
        OutputRegistration []newreg;
        
        outreg = this.outputs[outputport];
        found  = false; posfound = -1;
        for (i=0; i<outreg.length; i++)
           if (outreg[i].getConsumer() == consumer)
           {
              found    = true;
              posfound = i;
           }
        if (found)
        {
           newreg = new OutputRegistration[outreg.length-1];
           for (i=0; i<outreg.length; i++)
           {
              if      (i < posfound) newreg[i]   = outreg[i];
              else if (i > posfound) newreg[i-1] = outreg[i];
           }  
           this.outputs[outputport] = outreg;
        }
     }

    /**
     * Get the supplier of the specified input port.
     * @param port The input port to get the supplier for.
     * @return The supplier that is registered at the given port. <code>null</code> if no supplier is defined for the port.
     * @throws ShamanException If the given input port is out of bounds.
     */
    public DataFlow getSupplier(int inputport) throws DataFlowException
    {
        // Check port bounds.
        if (inputport >= getNumberOfInputs())
             throw new DataFlowException("Input port "+inputport+" out of bounds in '"+getName()+"' Only has '"+getNumberOfInputs()+"' input ports.");

        // Get the supplier of the port.
        DataFlow supplier = this.inputs[inputport].getSupplier();
        return supplier;
    }

    /**
     * Get the consumers registered on the specified output port.
     * @param port The output port to get the consumers for.
     * @return A list of consumers registered on the port.
     * @throws ShamanException If the given output port is out of bounds.
     */
    public List getConsumers(int outputport) throws DataFlowException
    {
        ArrayList          consumers;
        
        // Check port bounds.
        if (outputport >= getNumberOfOutputs())
            throw new DataFlowException("Output port "+outputport+" out of bounds in '"+getName()+"'. Only has '"+getNumberOfOutputs()+"' output ports.");
        
        // Make a new List containing the consumers of the given port.
        consumers = new ArrayList();
        for (int i=0; i<this.outputs[outputport].length; i++)
           consumers.add(this.outputs[outputport][i].getConsumer());
           
        return consumers;
    }
    
    public InputRegistration getInputRegistration(int inputport)
    {
        return(this.inputs[inputport]);
    }
    
    protected InputRegistration    []getInputRegistrations()  { return(this.inputs);  }
    protected OutputRegistration [][]getOutputRegistrations() { return(this.outputs); }
    
    // **********************************************************\
    // *     Naming and detailed description for debug output   *
    // **********************************************************/
    /**
     * Get a short description describing the function of this FlowBase in relation to it's neighbors.
     * @return The name of this FlowBase
     */
    public String getName()        { return(name); }
    
    /**
     * Get a longer description of the function and capabilities of this FlowBase.
     * @return A full description of this FlowBase.
     */ 
    public String getDescription() { return(description); }

    /**
     * Change the name of this FlowBase
     * @param name The new name.
     */ 
    public void setName(String name)
    {
        this.name = name;
    }
    
    /**
     * Change the description of this FlowBase
     * @param description The new description
     */ 
    public void setDescription(String description)
    {
        this.description = description;
    }
   
    public String toString()
    {
         return getName() + " (" + getDescription() + ")";
    }
   
    // **********************************************************\
    // *          Speed Optimized Forward Data Flow             *
    // **********************************************************/
    public Object getInput(int port) throws DataFlowException
    {
         Object result;
         
         result = this.inputData[port];
         if (result != null)
         {
             this.inputsAvailable[port/64] &= portMaskInvert[port%64];
             this.inputData[port]           = null;
         }

         return(result);
    }
    
    public boolean areInputsAvailable(int inputPort, int numberOfInputs) throws DataFlowException
    {
        return((this.inputsAvailable[inputPort/64] & portMask[inputPort%64]) != 0);
    }
    
    public void setOutput(int port, Object data) throws DataFlowException
    {
        int                   i;
        TransformationNetwork net;
        Transformation        con;
        Transformation        confast;
        OutputRegistration    oreg;
        int                   conport;
            
        // For all registered consumers on the given port.
        if (this.outputs[port] != null)
        {
            for (i=0; i<this.outputs[port].length; i++)
            {
                oreg = this.outputs[port][i];
                con  = (Transformation)oreg.consumer;
                
                // Route data to the input identity of a network not the network itself
                if (con instanceof TransformationNetwork)
                {
                   net = (TransformationNetwork)oreg.getConsumer();
                   con = (Transformation)net.getInputPortMapping();
                }
                confast = (Transformation)con;
                
                // Set the new data to in the consumer's input port.
                conport                    = oreg.consumerPort;
                confast.inputData[conport] = data;
                confast.inputsAvailable[conport/64] |= portMask[conport%64];
                
                // Propagate data through the network.
                con.transform();
            }
        }
    }
    
    public void transform() throws DataFlowException
    {
       // Get data from input queue. Transform the data to (possibly) multiple outputs. Add them to output queue.
       int     i;
       Object  obin;
       Object  []obsin;
       Object  []fout;
       boolean datathere;

       // If there's data present...
       datathere = true;
       for (i=0; (i<getNumberOfInputs()) && (datathere); i++) datathere = areInputsAvailable(i, 1);
       if (datathere)
       {
           if (getNumberOfInputs() == 1) // Very often used, so special case for 1 port.
           {
               // Get data from 1 port input data
               obin = getInput(0);

               // Transform the input to the output.
               fout = transform(obin);
           }
           else
           {
               // Get data from all ports
               obsin = new Object[getNumberOfInputs()];
               for (i=0; i<obsin.length; i++) obsin[i] = getInput(i);

               // Transform the inputs into outputs
               fout = transform(obsin);
           }

           // Add the transformation output to the output queue.
           if (fout != null) for (i=0; i<fout.length; i++) setOutput(0, fout[i]);
        }
        else fout = null;
    }

    // Transform the single input into (one or more) outputs
    public Object []transform(Object in) throws DataFlowException { return(null); }

    // Transform the multiple inputs into (one or more) outputs
    public Object []transform(Object []in) throws DataFlowException { return(null); }
    
    public void clear()
    {
        int i;
        
        // Clear all data still present in the input data-flow buffers
        for (i=0; i<inputData.length; i++)       inputData[i] = null;
        for (i=0; i<inputsAvailable.length; i++) inputsAvailable[i] = 0;
    }
   
    // **********************************************************\
    // *                  Unit-Test Integration                 *
    // **********************************************************/
    public void isolate() throws ShamanException
    {
        int          i;
        VectorSource vecsrc;
        Block        block;
        
        for (i=0; i<getNumberOfInputs(); i++)
        {
            vecsrc = new VectorSource();
            vecsrc.registerConsumer(0, this, i);
            vecsrc.setFit(VectorSource.FIT_NONE);
            vecsrc.setDataModel(getInputDataModel(i));
            vecsrc.setName("Isolated Source "+i);
            this.inputs[i] = new InputRegistration(0, vecsrc);
            vecsrc.init();
        }
        for (i=0; i<getNumberOfOutputs(); i++)
        {
            block = new Block();
            block.setCollect(true);
            block.registerSupplier(0, this, i);
            block.setName("Isolated Block "+i);
            this.outputs[i]    = new OutputRegistration[1];
            this.outputs[i][0] = new OutputRegistration(0, block);
            block.init();
        }
    }
    
    public void setSupplierData(int inputPort, Object data) throws DataFlowException
    {
        VectorSource src;
        if (inputPort < getNumberOfInputs())
        {
            src = (VectorSource)getSupplier(inputPort);
            if (data != null) src.forceOutput(data);
        }
        else throw new TransformationException("InputPort out of bounds. "+inputPort+" >= "+getNumberOfInputs());
    }
    
    public Object []getConsumerData(int outputPort) throws DataFlowException
    {
        Object []out;
        Block  block;
        
        out = null;
        if (outputPort < getNumberOfOutputs())
        {
            List outQu;
           
            block = (Block)getConsumers(outputPort).get(0);
            outQu = block.getBlockedData();
            if (outQu.size() > 0)
            {
               out   = outQu.toArray();
               block.clearBlockedData();
            }
            else out = null;
        }
        else throw new TransformationException("OutputPort out of bounds. "+outputPort+" >= "+getNumberOfOutputs());
       
        return(out);
    }

    // **********************************************************\
    // *   Extensions for Data Vector Transformtion Framework   *
    // **********************************************************/
    public DataModel getInputDataModel(int port) throws ConfigException
    {
        if ((port >= 0) && (port < this.inputModel.length)) return(this.inputModel[port]);
        else throw new ConfigException("Input Port index "+port+" is out of bound. Transformation '"+getName()+"' has "+inputModel.length+" input DataModels.");
    }

    public DataModel getOutputDataModel(int port) throws ConfigException
    {
        if ((port >= 0) && (port < this.outputModel.length)) return(this.outputModel[port]);
        else throw new ConfigException("Output Port index "+port+" is out of bound. Transformation '"+getName()+"' has "+getNumberOfOutputs()+" output ports.");
    }

    public void setInputDataModel(int port, DataModel dmin) throws ConfigException
    {
        // Check if the port number is legal
        if ((port >= 0) && (port < getNumberOfInputs())) 
        {
            // Install the new DataModel for the specified input port
            inputModel[port] = dmin;
        }
        else throw new ConfigException("Input Port index "+port+" is out of bound. This Transformation has "+getNumberOfInputs()+" input ports.");
    }

    public void setOutputDataModel(int port, DataModel dmout) throws ConfigException
    {
        // Check if the port number is legal
        if ((port >= 0) && (port < getNumberOfOutputs()))
        {
            // Install the new output datamodel
            outputModel[port] = dmout;
        }
        else throw new ConfigException("Output Port index "+port+" is out of bound. This Transformation has "+getNumberOfOutputs()+" output ports.");
    }
   
    public DataModel getSupplierDataModel(int inputPort) throws ConfigException
    {
        InputRegistration ir;
        DataModel         dmsup;
      
        // Check port bounds.
        if (inputPort >= getNumberOfInputs())
             throw new ConfigException("Input port "+inputPort+" out of bounds in '"+getName()+"'. Only has '"+getNumberOfInputs()+"' input ports.");
      
        // Get the DataModel of the supplier's output port that is connected to the given input port of this Transformation. 
        ir    = getInputRegistration(inputPort);
        dmsup = ((Transformation)ir.getSupplier()).getOutputDataModel(ir.getSupplierPort());
      
        return(dmsup);
    }
    
    public DataModel setSupplierAsInputDataModel(int inputPort) throws ConfigException
    {
        DataModel dmsup;
        
        // Set the supplier's output-DataModel of the port connected to the given input-port
        // as the DataModel of that input-port
        dmsup = getSupplierDataModel(inputPort);
        setInputDataModel(inputPort, dmsup);
        
        return(dmsup);
    }
    
    // **********************************************************\
    // *             Data Flow Port Construction                *
    // **********************************************************/
    protected void grow(int _numInput, int _numOutput)
    {
        // Create a List of InputRegistration objects. One for each input port.
        this.inputs     = new InputRegistration[_numInput];
        this.inputModel = new DataModel[_numInput];

        // Create a List of Lists for each output port that will contains the OutputRegistration objects.
        this.outputs     = new OutputRegistration[_numOutput][];
        this.outputModel = new DataModel[_numOutput];
        
        // Make the Fast Input Buffers
        this.inputData       = new Object[_numInput];
        this.inputsAvailable = new long[((_numInput-1)/64)+1];
        for (int i=0; i<this.inputsAvailable.length; i++) this.inputsAvailable[i] = 0;
    }
    
    // *********************************************************\
    // *            Persister Methods for Sub-classes          *
    // *********************************************************/
    protected void loadState(ObjectInputStream oin) throws ConfigException
    {
        try
        {
            this.name        = (String)oin.readObject();
            this.description = (String)oin.readObject();
        }
        catch(ClassNotFoundException ex) { throw new ConfigException(ex); }
        catch(IOException ex)            { throw new ConfigException(ex); }
    }
    
    protected void saveState(ObjectOutputStream oout) throws ConfigException
    {
        try
        {
            oout.writeObject(this.name);
            oout.writeObject(this.description);
        }
        catch(IOException ex) { throw new ConfigException(ex); }
    }
   
    // **********************************************************\
    // *                      Construction                      *
    // **********************************************************/
    public Transformation()
    {
        int numin, numout;
       
        numin  = getNumberOfInputs();
        numout = getNumberOfOutputs();
       
        // Create a List of InputRegistration objects. One for each input port.
        this.inputs     = new InputRegistration[numin];
        this.inputModel = new DataModel[numin];

        // Create a List of Lists for each output port that will contains the OutputRegistration objects.
        this.outputs     = new OutputRegistration[numout][];
        this.outputModel = new DataModel[numout];
        
        // Make Input data and availability buffers
        this.inputsAvailable    = new long[1];
        this.inputsAvailable[0] = 0;
        this.inputData          = new Object[numin];  
    
        // Initialize the naming and description
        this.name        = "Transformation";
        this.description = "Abstract Transformation description. Add a proper description the class '"+this.getClass().getName()+"' please.";
    }
}
