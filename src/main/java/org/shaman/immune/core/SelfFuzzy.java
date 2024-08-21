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
 *                  Fuzzy Self Particle                  *
 \*********************************************************/
public class SelfFuzzy extends ParticleFuzzy implements Self
{
    /*********************************************************\
     *                          Data                         *
    \*********************************************************/
    
    
    /*********************************************************\
     *               Initialization & Cleanup                *
    \*********************************************************/
    public SelfFuzzy(Body _bod)
    {
        bod = _bod;
    }
    
    public SelfFuzzy()
    {
        data = null; len = 0;
    }
    
}
