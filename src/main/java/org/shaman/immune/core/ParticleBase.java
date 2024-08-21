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

import cern.colt.matrix.DoubleMatrix1D;

/*********************************************************\
 *      Particle Base Class for Bit-String or Fuzzy      *
 *    Self-Particle, Antigen, Detector, Memory-Particle  *
\*********************************************************/
public abstract class ParticleBase implements Particle
{
   /*********************************************************\
    *                          Data                         *
   \*********************************************************/
   protected Body    bod;             // Body that interacts with this particle
   protected int     len;             // Length of the data-array
   protected int     beg, end;        // Begin- and endposition of the data
   protected boolean active;          // Activation flag

   /*********************************************************\
    *                       Activation                      *
   \*********************************************************/
   public void    activate()   { active = true; }
   public void    deActivate() { active = false; }
   public boolean isActive()   { return(active); }

   /*********************************************************\
    *                      Data Access                      *
   \*********************************************************/
   public Body getBody()   { return(bod); }
   public void setBody(Body _bod) { bod = _bod; }
   public int  getLength() { return(len); }

   /*********************************************************\
    *  Converting from raw data to internal representation  *
   \*********************************************************/
   public abstract void compile(Morphology morpho, DoubleMatrix1D datob) throws AISException;

   /*********************************************************\
    *                Contiguous Matching                    *
   \*********************************************************/
   public abstract double matchParticle(Particle b, int matchlen) throws AISException;
   public double match(Self    b, int matchLen)  throws AISException { return(matchParticle(b,matchLen)); }
   public double match(Antigen b, int matchLen)  throws AISException { return(matchParticle(b,matchLen)); }
   public abstract void match(Detector b, int matchLen, double []out) throws AISException;
   public double match(Detector b, int matchLen) throws AISException
   {
     double []out = new double[2];

     match(b,matchLen, out);

     return(out[1]);
   }

   public abstract double matchSubString(Detector b, int bpos, int epos) throws AISException;

   /*********************************************************\
    *               Initialization & Cleanup                *
   \*********************************************************/
   public abstract void init(int _len);
   public abstract void init(int _len, Body _bod);
}
