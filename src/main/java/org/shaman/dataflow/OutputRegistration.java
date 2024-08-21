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

import java.util.LinkedList;

/**
 * <h2>FlowBase Output Port Connection</h2>
 * Contains a connection to a 'consumer' FlowBase and 
 * an output data buffer.
 */
public class OutputRegistration
{
        /** A list containing data object to be processed */
        private LinkedList outputQueue;
        /** The FlowBase consuming the data */
        public DataFlow   consumer;
        /** The Input Port of the consumer connected to the output port associated with this OutputRegistration */
        public int        consumerPort;
        
		/**
		 * Get the data buffer of the output port connection of this OutputRegistration.
		 * @return LinkedList
		 */
        public LinkedList getOutputQueue() { return(outputQueue); }
        
        /**
         * Change the output queue of this output registration.
         * @param outputQueue The new queue of ouput data object.
         */ 
        public void setOutputQueue(LinkedList outputQueue)
        {
            this.outputQueue = outputQueue;
        } 
        
		/**
		 * Get the consumer connected to the output port of this OutputRegistration.
		 * @return FlowBase
		 */
        public DataFlow getConsumer() { return(consumer); }
        
		/**
		 * Get the input port of the consumer to which the output port of this OutputRegistration is connected to. 
		 * @return int
		 */
        public int getConsumerPort() { return(consumerPort); }
        
        
		/**
		 * Make an Output Port registration.
		 * @param consumerPort The input port of the consumer FlowBase to connect to.
		 * @param consumer The FlowBase to consumes data from the output port associated with this OutputRegistration.
		 */
        public OutputRegistration(int consumerPort, DataFlow consumer)
        {
            this.consumerPort = consumerPort;
            this.consumer     = consumer;
            this.outputQueue  = new LinkedList();
        }
}