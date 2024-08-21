/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                     Technologies                      *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  $VER : Join2.java v1.0 ( June 2002 )                 *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2002 Shaman Research                   *
\*********************************************************/
package org.shaman.dataflow;

/**
 * <h2>Data Flow Join of 2 inputs</h2>
 */

// *********************************************************\
// *             Joining 2 Identical Data Flows            *
// *********************************************************/
public class Join2 extends Join
{
    public Join2()
    {
        super();
        grow(2);
        
        name        = "Join2";
        description = "Joins two data flows with the same datamodel into one data flow.";
    }
}