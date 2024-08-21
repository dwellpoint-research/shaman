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
 * <h2>One-to-One Transformation Network</h2>
 * <br>
 * @author Johan Kaers
 * @version 2.0
 */

// *********************************************************\
// *            NxM Transformation Network                 *
// *********************************************************/
public class TransformationNetwork1x1 extends TransformationNetwork
{
  public TransformationNetwork1x1()
  {
    super();
    grow(1,1);

    name        = "TransformationNetwork1x1";
    description = "One input to one Output Transformation Network";
  }
}