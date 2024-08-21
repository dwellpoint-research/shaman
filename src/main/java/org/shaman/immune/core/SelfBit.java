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

import java.util.Hashtable;

/*********************************************************\
 *               Bit String Self Particle                *
\*********************************************************/
public class SelfBit extends ParticleBit implements Self
{
   /*********************************************************\
    *                          Data                         *
   \*********************************************************/
   public static Hashtable symbolsID = new Hashtable();
   static
   {
     symbolsID.put("selfbit", new Integer(0));
   }
   public Hashtable getID() { return(symbolsID); }

   /*********************************************************\
    *                  Visualization Support                *
   \*********************************************************/
   public void toVisualData(int []ind, double []co)
   {
     co[0] = 0.0; co[1] = 0.0; co[2] = 0.0;
   }

   /*********************************************************\
    *               Initialization & Cleanup                *
   \*********************************************************/
   public SelfBit(Body _bod)
   {
     bod    = _bod;
   }

   public SelfBit()
   {
     data = null; len = 0;
   }
}
