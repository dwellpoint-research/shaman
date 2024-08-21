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


/**
 * <h2>FlowBase Input Port Connection</h2>
 * Contains a connection to a 'supplier' FlowBase and
 * an input data buffer.
 */
public class InputRegistration
{
        /** The FlowBase supplying data. */
        public DataFlow   supplier;
        /** The output port of the supplier connected to the input port associated with this input registration. */
        public int        supplierPort;

        /**
         * Give the FlowBase that supplies data to the port associated with this InputRegistration.
         * @return The supplier FlowBase.
         */
        public DataFlow getSupplier() { return(supplier); }
        
        /**
         * Give the output port of the supplier that is connected to this InputRegistration's input port.
         * @return the Supplier's output port that is connected.
         */ 
        public int getSupplierPort() { return(supplierPort); }
         
		/**
		 * Make an Input Port registration.
		 * @param supplierPort The output port on the Supplier to connect to.
		 * @param supplier The FlowBase that supplies data to the input port associated with this InputRegistration.
		 */
        public InputRegistration(int supplierPort, DataFlow supplier)
        {
            this.supplierPort = supplierPort;
            this.supplier     = supplier;
        }       
}