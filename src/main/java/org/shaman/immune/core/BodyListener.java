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
package org.shaman.immune.core;

import java.util.EventListener;

// *********************************************************\
// *     Receive notification if the body changed          *
// *********************************************************/
public interface BodyListener extends EventListener
{
   public static final String ENVIRONMENT_CHANGED     = "Environment Changed";
   public static final String SELF_CHANGED            = "Self Changed";
   public static final String DETECTORS_CHANGED       = "Detectors Changed";
   public static final String MEMORY_CHANGED          = "Memory Changed";
   public static final String CLONES_CHANGED          = "Clones Changed";
   public static final String IMMUNE_RESPONSE_CHANGED = "Immune Response Changed";
   public static final String DATAMODEL_CHANGED       = "Datamodel Changed";
   public static final String BODY_CHANGED            = "Body Changed";
   
   public static final String TIMER                   = "Timer";
   
   /**
    * Notify this Listener that something has happened.
    * @param message What happened?
    * @param val More data about the message.
    * @throws AISException If something goes wrong while notifying the Listerer.
    */
   public void notify(String message, Object val) throws AISException;

   /**
    * The listener has processed all messages of the given type.
    * @param message The type of message that was processed.
    */
   public void understood(String message);
}
