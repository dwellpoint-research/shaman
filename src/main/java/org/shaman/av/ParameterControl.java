/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                    Audio / Video                      *
 *                                                       *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2004-5 Shaman Research                 *
\*********************************************************/
package org.shaman.av;


/**
 * <h2></h2>
 */

// **********************************************************\
// *              Generic Parameter Control                 *
// **********************************************************/
public interface ParameterControl
{
    public void   setParameter(double val);
    public double getParameter();
}