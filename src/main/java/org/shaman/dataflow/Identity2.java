/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                     Technologies                      *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  $VER : Identity2.java v1.0 ( July 2002 )             *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2002 Shaman Research                   *
\*********************************************************/
package org.shaman.dataflow;

/**
 * <h2>2-2 Identity Transformation</h2>
 * <br>
 * @author Johan Kaers
 * @version 2.0
 */

// *********************************************************\
// *              Indentity Transformation                 *
// *********************************************************/
public class Identity2 extends Identity
{
    public Identity2()
    {
        super();
        grow(2);
        
        name        = "Identity2";
        description = "Just passes the inputs to the next transformation";
    }
}