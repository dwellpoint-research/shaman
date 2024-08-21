/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                     Technologies                      *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  $VER : Merge2.java v1.0 ( June 2002 )                *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2002 Shaman Research                   *
 \*********************************************************/
package org.shaman.dataflow;

/**
 * <h2>Data Flow Merge with 2 inputs</h2>
 */

// *********************************************************\
// *                  Merging 2 Data Flows                 *
// *********************************************************/
public class Merge2 extends Merge
{
    public Merge2()
    {
        super();
        grow(2);
        
        name        = "Merge2";
        description = "Concatenates two data flows into one.";
    }
}