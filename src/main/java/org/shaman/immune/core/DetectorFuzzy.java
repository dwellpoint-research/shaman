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

import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.AttributePropertyFuzzy;
import org.shaman.datamodel.FMF;
import org.shaman.exceptions.DataModelException;

import cern.jet.random.Uniform;


/*********************************************************\
 *               A fuzzy non-self detector               *
\*********************************************************/
public class DetectorFuzzy extends ParticleFuzzy implements Detector
{
   /*********************************************************\
    *                          Data                         *
   \*********************************************************/
   // Fuzzy Membership Function Source
   private static final int FUZZY_BODY = 1;        // Data indexes the Fuzzy Memebership Functions of the data type
   private static final int FUZZY_OWN  = 2;        // This detector has it's own data fuzzy membership functions array (used when body is live)
   int    fuzzy;                                   // FMF source
   FMF    []fmf;                                   // The Fuzzy Detector FMFs
   double []fth;                                   // The Fuzzy Memberhsip Thresholds

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
   public void initRandom(int _len, Body _bod) throws AISException
   {
     int   i;

     bod   = _bod;
     len   = _len;
     data  = new double[len];
     act   = new double[len];

     try
     {
       for (i=0; i<len; i++)
       {
         data[i] = Uniform.staticNextIntFromTo(0, getAttribute(i).getNumberOfCategories()-1);
         act[i]  = 0;
       }
     }
     catch(DataModelException ex) { throw new AISException(ex); }

     mature      = false;   // Not censored yet -> Not mature yet.
     age         = 0;
     activation  = 0;
     matchPeriod = 0;
   }

   public void makeRandom() throws AISException
   {
      if (!bod.getCrisp()) makeRandomFuzzy();
      else                 makeRandomCrisp();
   }

   private void makeRandomCrisp() throws AISException
   {
       int             i, numcat;
       AttributeDouble atnow;
       int             rancat;
       double          ranval;
       
       try
       {
          for (i=0; i<len; i++)
          {
             atnow   = (AttributeDouble)getAttribute(i);
             numcat  = atnow.getLegalValues().length;
             rancat  = Uniform.staticNextIntFromTo(0, numcat-1);
             ranval  = atnow.getCategoryDouble(rancat);
             data[i] = ranval;
          }
       }
       catch(DataModelException ex) { throw new AISException(ex); }
   }

   private void makeRandomFuzzy() throws AISException
   {
     int                    i;
     AttributePropertyFuzzy atpfuz;
     FMF                    fuz;

     try
     {
           // Create Random Detector....
           for (i=0; i<len; i++)
           {
             atpfuz  = getFuzzyProperty(i);
             fmf[i]  = atpfuz.getRandomFMF();
             fth[i]  = atpfuz.getThreshold();

             data[i] = 0; act[i]  = 0;  // Legacy
           }
     }
     catch(DataModelException ex) { throw new AISException(ex); }
   }


   /*********************************************************\
    *                      Matching rule                    *
   \*********************************************************/
   public double matchAt(int ind, double x) throws AISException
   {
       if (!bod.getCrisp()) return(matchAtFuzzy(ind, x));
       else                 return(matchAtCrisp(ind, x));
   }
   
   private double matchAtCrisp(int ind, double x) throws AISException
   {
       if (data[ind] == x) return(1.0);
       else                return(0.0);
   }
   
   private double matchAtFuzzy(int ind, double x) throws AISException
   {
     double match;

     try
     {
       if (fmf[ind].apply(x) >= fth[ind]) match = 1.0;
       else                               match = 0.0;
     }
     catch(DataModelException ex) { throw new AISException(ex); }

     return(match);
   }

   public Attribute getAttribute(int ind) throws AISException
   {
     Attribute at;

     at = null;
     if (ind < len)
     {
       if (fuzzy == FUZZY_BODY) at = bod.getMorphology().getAttributeInParticleAt(ind);
       else                     at = null; //
     }

     return(at);
   }

   public AttributePropertyFuzzy getFuzzyProperty(int ind) throws AISException
   {
     Attribute              at;
     AttributePropertyFuzzy atpfuz;

     if      (fuzzy == FUZZY_BODY)
     {
       at     = getAttribute(ind);
       atpfuz = (AttributePropertyFuzzy)at.getProperty(AttributePropertyFuzzy.PROPERTY_FUZZY);
     }
     else throw new AISException("Can't get Fuzzy Property");

     return(atpfuz);
   }

   public FMF getFMF(int ind) throws AISException
   {
     FMF                    fuz;
     Attribute              at;
     AttributePropertyFuzzy atpfuz;

     try
     {
       if      (fuzzy == FUZZY_BODY)
       {
         at     = getAttribute(ind);
         atpfuz = (AttributePropertyFuzzy)at.getProperty(AttributePropertyFuzzy.PROPERTY_FUZZY);
         fuz    = atpfuz.getFMF(data[ind]);
       }
       else fuz = fmf[(int)data[ind]];
     }
     catch(DataModelException ex) { throw new AISException(ex); }

     return(fuz);
   }

   /*********************************************************\
    *      Make most plausible guess at detector data       *
   \*********************************************************/
   /*
   public void setDataFuzzy(Particle _par)
   {
     int    i,j,len;
     double []pdat;
     int    fuzind;
     double fuzmax, fuznow;
     FMF    []fuz;
     ParticleFuzzy par;

     par = (ParticleFuzzy)_par;

     // Detector data[i] = FMF that has highest value for par.data[i]
     pdat = par.data;
     len  = par.len;
     if (!(par instanceof Detector))
     {
       for (i=0; i<len; i++)
       {
         fuzind = bod.dataTypes.fieldPos[i];
         fuz    = bod.dataTypes.type[fuzind].getFMFs();
         fuzmax = fuz[0].func(pdat[i]); fuzind = 0;
         for (j=1; j<fuz.length; j++)
         {
           fuznow = fuz[j].func(pdat[i]);
           if (fuznow > fuzmax) { fuzmax = fuznow; fuzind = j; }
         }
         data[i] = fuzind;
       }
     }
     else
     {
       for(i=0; i<len; i++) data[i] = par.data[i];
     }
   }*/

   /*********************************************************\
    *                        Cloning                        *
   \*********************************************************/
   public void makeClone(Detector _det, Particle ag, double fmt) throws AISException
   {
     // THINK ABOUT THIS
     //        ... Modify by using an array of mutSize or so...
     DetectorFuzzy det;
     int           i;
     FMF           fuz;

     det = (DetectorFuzzy)_det;
     for (i=0; i<len; i++)
     {
       fuz = det.getFMF(i);

       // Make detector with it's own FMFs, displace it according to FMF bounds

       //data[i] = fuz.generateRandom();
       //System.out.print(data[i]+"  ");

       data[i] = det.data[i];
       data[i] += Uniform.staticNextDoubleFromTo(-.2, .2); // RIGGED for Onion
     }

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
    *               Initialization & Cleanup                *
   \*********************************************************/
   public void init(int _len, Body _bod)
   {
     int   i;
     len   = _len;
     data  = new double[len];
     act   = new double[len];
     fmf   = new FMF[len];
     fth   = new double[len];

     for (i=0; i<len; i++) act[0] = 0;
     mature      = true;
     age         = 0;
     activation  = 0;
     matchPeriod = 0;
     bod    = _bod;
   }

   public void init(int _len)
   {
     int i;

     len  = _len;
     data = new double[len];
     act  = new double[len];
     fmf  = new FMF[len];
     fth  = new double[len];
     idle = 0;
     fuzzy = FUZZY_BODY;
   }

   public DetectorFuzzy(int _len)
   {
     int i;
     len   = _len;
     data  = new double[len];
     act   = new double[len];
     fmf  = new FMF[len];
     fth  = new double[len];
     idle  = 0;
     fuzzy = FUZZY_BODY;
   }

   public DetectorFuzzy(Body _bod)
   {
     bod = _bod; idle = 0; len = -1; fuzzy = FUZZY_BODY;
   }

   public DetectorFuzzy()
   {
     bod = null; idle = 0; len = -1; fuzzy = FUZZY_BODY;
   }
}
