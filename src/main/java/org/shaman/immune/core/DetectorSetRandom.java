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

import java.util.Vector;


// *********************************************************\
// *     The random algorithm for generating antibodies    *
// *********************************************************/
public class DetectorSetRandom extends DetectorSet
{
    // *********************************************************\
    // *      Create a mutated clone of the given detector     *
    // *********************************************************/
    public void clone(Detector det, Detector clone, double pmut) throws AISException
    {
        if ((det instanceof DetectorFuzzy) && (clone instanceof DetectorFuzzy))
        {
            cloneFuzzy((DetectorFuzzy)det, (DetectorFuzzy)clone, pmut);
        }
        else
        {
            cloneBit((DetectorBit)det, (DetectorBit)clone, pmut);
        }
    }
    
    private void cloneFuzzy(DetectorFuzzy det, DetectorFuzzy clone, double pmut) throws AISException
    {
        /*
         int     i,j;
         int     []numfuz;
         double  []cldat;
         int     len;
         boolean found, stopit;
         int     numtries, maxtry;
         int     mut;
         
         len    = bod.getDataTypes().getParticleLength();
         maxtry = 1000;
         
         // Make array of # of possible detector symbols at each position
          numfuz = new int[len];
          for (i=0; i<len; i++) numfuz[i] = bod.getDataTypes().getNumberOfFuzzyClasses(i);
          
          // Copy the data from the detector over to the clone
           cldat = clone.getData();
           
           // Introduce mutations in the clone but make sure it doesn't match with any self particle
            found = false; stopit = false; numtries = 1;
            while ((!found) && (!stopit))
            {
            for (j=0; j<len; j++) cldat[j] = det.getData()[j];
            for (j=0; j<len; j++)
            {
            mut = (int)Random.generate(Random.BERNOULLI, pmut, 0);
            if (mut == 1) cldat[j] = (int)Random.generate(Random.UNIFORM, 0, numfuz[j]);
            }
            
            if (bod.matchSelf(clone) == -1) found = true;
            else
            {
            numtries++;
            if (numtries >= maxtry) stopit = true;
            }
            }
            
            // If after much effort, a clone was not found, just copy the detector...
             if (stopit)
             {
             for (j=0; j<len; j++) cldat[j] = det.getData()[j];
             }*/
    }
    
    private void cloneBit(DetectorBit det, DetectorBit clone, double pmut) throws AISException
    {
        // Mutate the bits with the given probability
    }
    
    // *********************************************************\
    // *   Make random detectors and censor self-matching ones *
    // *********************************************************/
    int countdet;
    
    public void generate(double pf, int maxDet) throws AISException
    {
        int nd = guessNumberOfDetectors(pf, maxDet);
        generate(nd);
    }
    
    public void generate(int _numDet) throws AISException
    {
        boolean  ok;
        int      i;
        int      gendet;
        Vector   detvec;
        int      numDet;
        Detector detnow;
        
        // Guess number of detectors to generate
        gendet = _numDet;
        
        System.out.println("Generating "+gendet+" detectors.");
        
        // Generate detectors until finding a new one becomes really though.
        countdet = 0;
        detvec   = new Vector();
        numDet   = 0;
        while (numDet < gendet)
        {
            detnow = bod.createDetector(bod.getMorphology().getParticleLength());
            ok     = generateDetectorRandom(detnow);
            if (ok)
            {
                numDet++; countdet++;
                detvec.add(detnow);
                
                if (gendet > 1000)
                {
                    if      ((numDet % 250) == 0)  System.out.println(numDet);
                    else if ((numDet % 10)  == 0)  { System.out.print("."); System.out.flush(); }
                }
            }
        }
        
        // Copy the newly generated detectors into their storage
        det = new Detector[detvec.size()];
        detvec.copyInto(det);
        this.numDet = detvec.size();
    }
    
    public boolean generateDetectorRandom(Detector ab) throws AISException
    {
        int maxtry;
        int tries;
        
        maxtry = 1000;
        tries = generateDetectorRandom(ab, maxtry);
        
        if (tries == -1) return(false);
        else             return(true);
    }
    
    public int generateDetectorRandom(Detector ab, int maxtry) throws AISException
    {
        int     i;
        boolean found, stopit;
        int     numtries;
        int     matchind;
        
        // Generate a random detector that doesn't match with any of the self particles
        found = false; stopit = false; numtries = 1; matchind = -1;
        while ((!found) && (!stopit))
        {
            ab.makeRandom();
            matchind = bod.matchSelf(ab);
            if (matchind == -1) found = true;
            else
            {
                numtries++;
                if (numtries >= maxtry) stopit = true;
            }
        }
        if (stopit) numtries = -1;
        
        return(numtries);
    }
    
    public DetectorSetRandom(Body _bod)
    {
        bod = _bod;
        
        det    = null; clone    = null;
        numDet = 0;    numClone = 0;
    }
    
    private boolean generateDetectorsForrest() throws AISException
    {
        boolean ok;
        int     l,m,r;
        int     ns;
        double  pf, pfe, pm;
        int     nrt, nr0t, nr0e;
        int     i;
        
        ok = true;
        
        /*
         l = selfSize; r = matchSize; m = numDiv[0];
         
         pf   = 0.1;
         
         ns   = numSelf;
         pm   = Math.pow(m,-r) * (((double)(l-r))*(m-1)/(m+1));
         nrt  = (int)(-Math.log(pf) / pm);
         nr0t = (int)((-Math.log(pf)) / (pm*(Math.pow(1-pm,ns))));*/
        
        /*
         System.out.println("l = "+l+" r = "+r+" m = "+m);
         System.out.println("ns = "+ns);
         System.out.println("pf = "+pf+"   pm = "+pm);
         System.out.println("nr(theory)  = "+nrt);
         System.out.println("nr0(theory) = "+nr0t);
         */
        /*
         nr0e    = 0;
         //maxanti = nrt;
          numDet  = 0;
          while ((numDet < maxDet) && (ok))
          {
          if (generateDetectorRandomCrisp(det[numDet], nr0t) != -1) ok = true;
          else                                                      ok = false;
          if (ok) numDet++;
          }
          
          //System.out.println("nr0(experimental) = "+nr0e);;*/
        
        return(ok);
    }
}
