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


/*********************************************************\
 *  Foreign Fuzzy particle generated by the Environment  *
\*********************************************************/
public class AntigenFuzzy extends ParticleFuzzy implements Antigen
{
   /*********************************************************\
    *                          Data                         *
   \*********************************************************/

   /*********************************************************\
    *               Initialization & Cleanup                *
   \*********************************************************/
   public AntigenFuzzy(Body _bod)
   {
     bod = _bod;
   }

   public AntigenFuzzy()
   {
      data = null; len = 0;
   }

}
