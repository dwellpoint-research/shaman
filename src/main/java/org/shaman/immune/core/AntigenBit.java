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


/**
 *  Bit String Self Particle
 **/
public class AntigenBit extends ParticleBit implements Antigen
{
   /*********************************************************\
    *                          Data                         *
   \*********************************************************/


   /*********************************************************\
    *               Initialization & Cleanup                *
   \*********************************************************/
   public AntigenBit(Body _bod)
   {
     bod    = _bod;
   }

   public AntigenBit()
   {
     data = null; len = 0;
   }
}
