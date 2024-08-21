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
 * <h2>Two-to-Three Transformation Network</h2>
 * <br>
 * @author Johan Kaers
 * @version 2.0
 */

// *********************************************************\
// *            2x3 Transformation Network                 *
// *********************************************************/
public class TransformationNetwork3x3 extends TransformationNetwork
{
  public TransformationNetwork3x3()
  {
    super();
    grow(3,3);

    name        = "TransformationNetwork3x3";
    description = "Three inputs to three Output Transformation Network";
  }
}