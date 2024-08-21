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

import org.shaman.datamodel.AttributePropertyFuzzy;
import org.shaman.datamodel.FMF;

import cern.colt.matrix.DoubleMatrix1D;



/*********************************************************\
 *       Fuzzy Real-String Particle Base Class for       *
 *   Self-Particle, Antigen, Detector, Memory-Particle   *
 *       with  contiguous fuzzy symbol matching rule     *
\*********************************************************/
public class ParticleFuzzy extends ParticleBase
{
   /*********************************************************\
    *                          Data                         *
   \*********************************************************/
   public double data[];          // Real valued fuzzy data

   /*********************************************************\
    *  Converting from raw data to internal representation  *
   \*********************************************************/
   public void compile(Morphology morpho, DoubleMatrix1D datob) throws AISException
   {
        int  i;
        int  []fieldPos;

        fieldPos = morpho.getFieldPositions();
        for (i=0; i<len; i++)
        {
          data[i] = datob.getQuick(fieldPos[i]);
        }
   }

   // *********************************************************\
   // *             Contiguous Fuzzy Symbol Matching          *
   // *********************************************************/
   public double matchParticle(Particle _b, int matchlen) throws AISException
   {
     // Find the highest match between 2 non-detector particles
     double                 mnow, mmax, mmaxind;
     int                    i,j;
     double                 fuzmax, fuznow, fuzind;
     AttributePropertyFuzzy atpfuz;
     FMF                  []fuz;
     FMF                    fuzact;
     double               []sim;
     ParticleFuzzy          b;

     throw new AISException("Operation not supported");

//     b = (ParticleFuzzy)_b;
//
//     // Create the array of fuzzy similarity between the particle's elements
//     sim  = new double[len];
//     for (i=0; i<len; i++)
//     {
//       atpfuz  = bod.getMorphology().getFuzzyPropertyAt(i);
//     }
//
//     // Get maximum
//     mmax = 0; mmaxind = -1;
//     for (i=0; i<len-matchlen; i++)
//     {
//       mnow = 0;
//       for (j=i; j<i+matchlen; j++)
//       {
//         mnow += sim[j];
//       }
//       if (mnow >= mmax) { mmax = mnow; mmaxind = i; }
//     }

     //return(mmax);
   }

   public void   match(Detector _b, int matchLen, double []out) throws AISException
   {
     // out[0] = position of match
     // out[1] = match strength
     int      i,j,len;
     double   []match;
     int      ind;
     int      mind;
     double   mval;
     double   mmax, mnow;
     double   []adat;
     double   []bdat;
     DetectorFuzzy b;

     b    = (DetectorFuzzy)_b;
     len  = b.getLength();
     adat = data;
     bdat = b.data;

     match = new double[len];
     // Calculate the match between the values of the particle and the detector
     for (i=0; i<len; i++)
     {
       match[i] = b.matchAt(i, adat[i]);
     }

     // Find highest contiguous match of 'matchLen' size
     mind = 0;
     mnow = 0; for (i=0; i<matchLen; i++) mnow += match[i];
     mmax = mnow;
     for (i=1; i<len-matchLen; i++)
     {
       mnow = 0; for (j=i; j<i+matchLen; j++) mnow += match[j];
       if (mnow > mmax) { mind = i; mmax = mnow; }
     }

     out[0] = mind; out[1] = mmax;
   }

   public double matchSubString(Detector _b, int bpos, int epos) throws AISException
   {
     int           i,j;
     int           find;
     double        match, mnow;
     double      []bdat;
     DetectorFuzzy b;

     b    = (DetectorFuzzy)_b;
     bdat = b.data;

     match = 0;
     for (i=bpos; i<epos; i++)
     {
       mnow   = b.matchAt(i, data[i]);
       match += mnow;
     }

     return(match);
   }

   /*********************************************************\
    *              Fuzzy matching data interface            *
   \*********************************************************/
   public double []getData() { return(data); }
   public void setData(double[] _data)
   {
     int i;

     if (_data.length == data.length)
     {
       for (i=0; i<len; i++) data[i] = _data[i];
     }
   }


   /*********************************************************\
    *               Initialization & Cleanup                *
   \*********************************************************/
   public String toString()
   {
     int    i;
     String sout;

     sout = "Particle Fuzzy : ";
     for (i=0; i<data.length; i++) sout += data[i]+" ";

     return(sout);
   }

   public void init(int _len)
   {
     bod    = null;
     len    = _len;
     data   = new double[len];
     active = true;
   }

   public void init(int _len, Body _bod)
   {
     bod    = _bod;
     len    = _len;
     data   = new double[len];
     active = true;
   }

   public ParticleFuzzy(int _len)
   {
     len    = _len;
     data   = new double[len];
     active = true;
   }

   public ParticleFuzzy()
   {
     data = null; len = 0; active = false;
   }
}
