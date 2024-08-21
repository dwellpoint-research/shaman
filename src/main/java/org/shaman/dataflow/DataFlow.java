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

import java.util.List;

import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ShamanException;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;


/**
 * <h2>Data Flow Interface</h2>
 * Defines the core data-flow mechanism.
 * Every node in a data processing network of components
 * uses this mechanism to propagate data to its neighbors.
 * Every DataFlow has a number of input and output ports.
 * An input port connects to exactly one 'supplier'.
 * An output port connects to one or more 'consumers'.
 * Data vectors flow from suppliers to consumers. Diagnostics can
 * be enabled to analyse the data-flow through networks.
 * <p>
 */

// **********************************************************\
// *                  Data-Flow Interface                   *
// **********************************************************/
public interface DataFlow
{
    // **********************************************************\
    // *               Neighborhoud (de)Construction            *
    // **********************************************************/
    /**
     * Connect this DataFlow to the 'supplier'.<br>
     * Register the supplier-consumer and consumer-supplier relations in
     * the 2 components' data-flow neighbourhood definitions.
     * 
     * @param inputPort The input port of this DataFlow to connect the 'supplier' to.
     * @param Supplier  The 'supplier' DataFlow that is connected to this DataFlow.
     * @param supplierPort The output port of the 'supplier' to connect to the 'inputPort' of this FlowBase
     * 
     * @throws ShamanException If one of the ports is out of bounds.
     */ 
    public  void connect(int inputPort, DataFlow supplier, int supplierPort) throws ConfigException;
    
    /**
     * Connect an output port of the given 'supplier' DataFlow to an input port
     * of this DataFlow.
     *
     * @param port The input port of this DataFlow to connect to.
     * @param supplier The 'supplier' DataFlow to connect to. 
     * @param supplierPort The output port of the 'supplier' to connect to.
     * 
     * @throws ShamanException If one the ports is out of bounds.
     */
    public void registerSupplier(int port, DataFlow supplier, int supplierPort) throws ConfigException;
    
    
    /**
     * Connect an input port of the given 'consumer' DataFlow to an output port
     * of this DataFlow.
     *
     * @param port The output port of this DataFlow to connect the consumer to.
     * @param consumer The 'consumer' DataFlow.
     * @param consumerPort The input port the 'consumer' to connect to.
     * 
     * @throws ShamanException If one the ports is out of bounds.
     */
    public void registerConsumer(int port, DataFlow consumer, int consumerPort) throws ConfigException;
    

    /**
     * Unregisters the supplier on the specified input port.
     *
     * @param port The input port from which the supplier should be removed.
     * 
     * @throws ShamanException If the given input port is out of bounds.
     */
    public void unRegisterSupplier(int inputport) throws DataFlowException;
    
    
    /**
     * Unregisters a consumer on the specified output port.
     *
     * @param port The output port to remove a supplier from.
     * @param consumer The consumer to remove.
     * 
     * @throws ShamanException If the given output port is out of bounds or the given consumer is not found there.
     */
    public void unRegisterConsumer(int outputport, DataFlow consumer) throws DataFlowException;
    

    /**
     * Get the supplier of the specified input port.
     * @param port The input port to get the supplier for.
     * @return The supplier that is registered at the given port. <code>null</code> if no supplier is defined for the port.
     * @throws ShamanException If the given input port is out of bounds.
     */
    public DataFlow getSupplier(int inputport) throws DataFlowException;

    /**
     * Get the consumers registered on the specified output port.
     * @param port The output port to get the consumers for.
     * @return A list of consumers registered on the port.
     * @throws ShamanException If the given output port is out of bounds.
     */
    public List getConsumers(int outputport) throws DataFlowException;
    
    // **********************************************************\
    // *                  DataModel Integration                 *
    // **********************************************************/
    public DataModel getInputDataModel(int port) throws ConfigException;
    
    public DataModel getOutputDataModel(int port) throws ConfigException;
    
    public void setInputDataModel(int port, DataModel dmin) throws ConfigException;
    
    public void setOutputDataModel(int port, DataModel dmout) throws ConfigException;
    
    public abstract void checkDataModelFit(int port, DataModel dm) throws ConfigException;
    
    // **********************************************************\
    // *          Component Initialization / Cleanup            *
    // **********************************************************/
    public void init() throws ConfigException;
    
    public void cleanUp() throws DataFlowException;
    
    // **********************************************************\
    // *                  Data Flow Mechanism                   *
    // **********************************************************/
    /**
     * Gets the next input data object from the specified port.<p>
     *
     * @param inputPort The input port for which the next data object must be retreived.
     * @return The next input data object, or <code>null</code> if no data is available.
     * @throws ShamanException if something goes wrong getting data from the supplier on the given input port.
     */
    public Object getInput(int inputPort) throws DataFlowException;

    /**
     * Pushes an Object to the inputs of the consumers registered
     * to the given outputPort
     *
     * @param outputPort the port on which the data will be written
     * @param data the Object the push forward
     * @throws ShamanException if something goes wrong while pushing data to the consumers.
     */
    public void setOutput(int outputPort, Object data) throws DataFlowException;
    
    /**
     * Transform the input data to the output data.<p>
     *
     * The structure of the implementation of this method is typically the
     * following:
     * <ol>
     * <li>Check whether the necessary number of input data objects on each
     *     input port is available, by using the method: <code>
     *     areInputsAvailable</code>
     * <li>Get the input data objects from the input ports, by using the method:
     *     <code>getInput</code>
     * <li>Transform the input data objects to the output data objects.
     * <li>Write the output data objects to the output ports, by using the
     *     method: <code>setOutput</code>
     * </ol>
     *
     * @throws ShamanException if something goes wrong while transforming the data
     */
    public void transform() throws DataFlowException;
    
    /**
     * Checks whether a specified number of input data objects are available on
     * the specified input port.
     *
     * This method must only be used from within the <code>transform</code>
     * method.
     *
     * @param port the port to check
     * @param numberOfInputs the number of input data objects that must be available
     * @return <code>true</code> if the input data objects are available, <code>false</code> otherwise.
     * @throws ShamanException if something goes wrong while checking the available input
     */
    public boolean areInputsAvailable(int inputPort, int numberOfInputs) throws DataFlowException;
    
    /**
     * Clears the data queues at input and output ports.<p>
     *
     * This method is typically called to reset the state of a network
     * of DataFlow objects.
     */
    public void clear();
    

    // **********************************************************\
    // *         Port naming and mutiplicity definition         *
    // **********************************************************/
    /**
     * Defines the number of input ports.
     * @return the number of input ports in this DataFlow.
     */
    public int getNumberOfInputs();

    /**
     * Defines the number of output ports.
     * @return the number of output ports in this DataFlow.
     */
    public int getNumberOfOutputs();
    
    /**
     * Gets the name of the specified input port.
     * @param inputport The input port for which the name must be retreived
     * @return The name of the specified input port
     */
    public String getInputName(int inputport);

    /**
     * Gets the name of the specified output port.
     * @param outputport The output port for which the name must be retreived
     * @return the name of the specified output port
     */
    public String getOutputName(int outputport);
    
    
   /**
     * Get a short description describing the function of this DataFlow in relation to it's neighbors.
     * @return The name of this DataFlow
     */
    public String getName();
    
    /**
     * Get a longer description of the function and capabilities of this DataFlow.
     * @return A full description of this DataFlow.
     */ 
    public String getDescription();
}