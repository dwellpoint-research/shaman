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

// *********************************************************\
// *     Particle Base Interface for Bit-String or Fuzzy   *
// *    Self-Particle, Antigen, Detector, Memory-Particle  *
// *********************************************************/
public interface Particle
{
   // *********************************************************\
   // *                       Activation                      *
   // *********************************************************/
   public void    activate();
   public void    deActivate();
   public boolean isActive();

   // *********************************************************\
   // *  Converting from raw data to internal representation  *
   // *********************************************************/
   public void compile(Morphology morpho, DoubleMatrix1D datob) throws AISException;

   // *********************************************************\
   // *                      Data Access                      *
   // *********************************************************/
   public Body getBody();
   public void setBody(Body _bod);
   public int  getLength();

   // *********************************************************\
   // *                Contiguous Matching                    *
   // *********************************************************/
   public double matchParticle(Particle b, int matchlen) throws AISException;
   public double match(Self    b, int matchLen) throws AISException;
   public double match(Antigen b, int matchLen) throws AISException;
   public void   match(Detector b, int matchLen, double []out) throws AISException;
   public double match(Detector b, int matchLen) throws AISException;
   public double matchSubString(Detector b, int bpos, int epos) throws AISException;

   // *********************************************************\
   // *               Initialization & Cleanup                *
   // *********************************************************/
   public void init(int _len);
   public void init(int _len, Body _bod);
}
