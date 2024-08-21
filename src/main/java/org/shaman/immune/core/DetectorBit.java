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

import cern.colt.bitvector.BitVector;
import cern.jet.random.Uniform;

// *********************************************************\
// *              A bit-string non-self detector           *
// *********************************************************/
public class DetectorBit extends ParticleBit implements Detector
{
   // *********************************************************\
   // *                          Data                         *
   // *********************************************************/
   // Training immune response.
   boolean mature;                                 // Has the detector survived the censor
   int     age;                                    // Number of time-steps the detector has been alive
   double  activation;                             // Overal activation by normal (< anomaly period long) matches of the detector
   int     matchPeriod;                            // Number of timesteps of consequetive matches

   // Immune Response State : Used in on-line adaptation, affinity maturation, various other immune mechanisms...
   double  []act;                                  // Per position activation level
   int     idle;                                   // Number of cycles of complete non-activation

   /*********************************************************\
    *          Generate a random (immature) detector        *
   \*********************************************************/
   public void initRandom(int _len, Body _bod)
   {
     int   i;

     bod   = _bod;
     len   = _len;
     data  = new BitVector(_len);
     act   = new double[len];

     for (i=0; i<len; i++) { act[i] = 0; }
     makeRandom();

     mature      = true;   // Not censored yet -> Not mature yet.
     age         = 0;
     activation  = 0;
     matchPeriod = 0;
   }

   public void makeRandom()
   {
     int i;

     this.data.clear();
     for (i=0; i<this.len; i++) if (Uniform.staticNextBoolean()) this.data.set(i);
   }


   /*********************************************************\
    *               Immune Response Model                   *
   \*********************************************************/
   public void    setMature(boolean _mature) { mature = _mature; }
   public boolean getMature() { return(mature); }
   public int     getMatchPeriod() { return(matchPeriod); }
   public void    setMatchPeriod(int _matchPeriod) { matchPeriod = _matchPeriod; }
   public void    setActivation(double _activation) { activation = _activation; }
   public double  getActivation() { return(activation); }
   public void    setAge(int _age) { age = _age; }
   public int     getAge() { return(age); }

   public int  overActivationThreshold(int matchlen, double th)
   {
     int     i,j;
     int     find;
     boolean found;
     boolean here;

     found = false; find = -1;
     for (i=0; (i<=len-matchlen) && (!found); i++)
     {
        here = true;
        for (j=i; (j<i+matchlen) && (here); j++)
        {
          if (act[j] < th) here = false;
        }
        if (here) { found = true; find = i; }
     }

     return(find);
   }

   public void resetImmuneResponse()
   {
     int i;

     idle = 0;
     for (i=0; i<len; i++) act[i] = 0;
   }

   public int  giveIdle()
   {
     return(idle);
   }

   public void increaseIdle()
   {
     idle++;
   }

   public boolean isActive()
   {
     int     i;
     boolean yes;

     yes = false;
     for (i=0; (i<len) && (!yes); i++) { if (act[i] > 0) yes = true; }

     return(yes);
   }

   public void decreaseActivation(double sub)
   {
     int i;

     for (i=0; i<len; i++)
     {
       if (act[i] > sub) act[i] -= sub;
       else              act[i] = 0;
     }
   }

   public void increaseActivation(int pos, int matchLen, double add)
   {
     int i;

     idle = 0;
     for (i=pos; i<pos+matchLen; i++)
     {
       act[i] += add;
     }
   }

   /*********************************************************\
    *                        Cloning                        *
   \*********************************************************/
   public void makeClone(Detector _det, Particle ag, double fmt)
   {
     // THINK ABOUT THIS
     //        ... Modify by using an array of mutSize or so...
     DetectorBit det;
     int         i;

     det = (DetectorBit)_det;
     for (i=0; i<len; i++)
     {

     }
   }
   
   /*********************************************************\
    *               Initialization & Cleanup                *
   \*********************************************************/
   public void init(int _len, Body _bod)
   {
     int i;

     super.init(_len, _bod);

     act   = new double[len];
     for (i=0; i<len; i++) act[0] = 0;
     idle  = 0;

     bod         = _bod;
     mature      = true;
     age         = 0;
     activation  = 0;
     matchPeriod = 0;
   }

   public void init(int _len)
   {
     int i;

     super.init(_len);

     act  = new double[len];
     for (i=0; i<len; i++) act[i] = 0;
     idle        = 0;
     mature      = true;
     age         = 0;
     activation  = 0;
     matchPeriod = 0;
   }

   public DetectorBit(Body _bod)
   {
     bod = _bod; data = null; act = null; idle = 0; len = 0;
   }

   public DetectorBit()
   {
     bod = null; data = null; act = null; idle = 0; len = 0;
   }
}
