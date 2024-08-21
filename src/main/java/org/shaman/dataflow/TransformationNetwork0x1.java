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
 * <h2>Zero-to-One Transformation Network</h2>
 * <br>
 * @author Johan Kaers
 * @version 2.0
 */

// *********************************************************\
// *            NxM Transformation Network                 *
// *********************************************************/
public class TransformationNetwork0x1 extends TransformationNetwork
{
  public TransformationNetwork0x1()
  {
    super();
    grow(0,1);

    name        = "TransformationNetwork0x1";
    description = "No input to one Output Transformation Network";
  }
}