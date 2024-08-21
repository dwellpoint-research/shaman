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
 * <h2>Port Mapping Identity</h2>
 * Identity transformation used to re-map the input and output ports
 * of a TransformationNetwork.
 */

// *********************************************************\
// *     In/Output Port Mapping Identity Transformation    *
// *********************************************************/
public class PortMapping extends Identity
{
  public PortMapping()
  {
    super();
    name        = "PortMapping";
    description = "Maps to input or output port of a TransformationNetwork";
  }
}