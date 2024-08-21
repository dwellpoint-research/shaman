/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                     Technologies                      *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  $VER : Identity1.java v1.0 ( July 2002 )             *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2002 Shaman Research                   *
 \*********************************************************/
package org.shaman.dataflow;

/**
 * <h2>1-1 Identity Transformation</h2>
 * <br>
 * @author Johan Kaers
 * @version 2.0
 */

// *********************************************************\
// *              Indentity Transformation                 *
// *********************************************************/
public class Identity1 extends Identity
{
    public Identity1()
    {
        super();
        grow(1);
        
        name        = "Identity1";
        description = "Just passes the input to the next transformation";
    }
}