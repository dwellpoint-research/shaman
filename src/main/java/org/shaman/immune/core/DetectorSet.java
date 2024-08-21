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
 *       A Set of Detectors that protect the Self        *
\*********************************************************/
public abstract class DetectorSet
{
    public abstract void generate(double pf, int maxDet) throws AISException;                     // Algorithm that generates the detectors
    public abstract void generate(int _numDet) throws AISException;
    public void clone(Detector det, Detector clone, double pmut) throws AISException
    {
        System.out.println("This must be a mistake!");
    }; // Make a detector clone
    
    Body     bod;                  // The Body in which the detectors exist
    Detector []det;                // The set of detectors
    int      numDet;               // The amount of detectors in the set
    
    Detector []clone;              // The set of cloned detectors
    int      numClone;             // Number of clones in the clone set
    
    /*********************************************************\
     *       Guess the number of detectors necessary         *
     \*********************************************************/
    public int guessNumberOfDetectors(double pf, int maxDet)
    {
        return(100000);
    }
    
    /*********************************************************\
     *                        Member Access                  *
     \*********************************************************/
    public Detector []getDetectors()                  { return(det); }
    public void     setDetectors(Detector []_det)     { det = _det; }
    public int      getNumberOfDetectors()            { return(numDet); }
    public void     setNumberOfDetectors(int _numDet) { numDet = _numDet; }
    public Detector []getClones()                     { return(clone); }
    public void     setClones(Detector []_clone)      { clone = _clone; }
    public int      getNumberOfClones()               { return(numClone); }
    public void     setNumberOfClones(int _numClone)  { numClone = _numClone; }
    
    public Detector getDetector(int i) { return(det[i]); }
    public Detector getClone(int i)    { return(clone[i]); }
    public void     setDetector(int i, Detector detnew) { det[i]   = detnew; }
    public void     setClone(int i, Detector clonew)    { clone[i] = clonew; }
    
    public Body     getBody() { return(bod); }
    public void     setBody(Body _bod)
    {
        int i;
        bod = _bod;
        if (det   != null) for (i=0; i<det.length;   i++) if (det[i]   != null) det[i].setBody(bod);
        if (clone != null) for (i=0; i<clone.length; i++) if (clone[i] != null) clone[i].setBody(bod);
    }
    
    /*********************************************************\
     *             Make space for 'numDet' detectors         *
     \*********************************************************/
    public void init(int _numDet)
    {
        numDet = _numDet;
        det    = new DetectorFuzzy[numDet];
    }
    
    /*********************************************************\
     *   Check if a any of the detectors match the particle  *
     \*********************************************************/
    public int match(Particle par) throws AISException
    {
        int     i;
        double  mnow;
        boolean match;
        int     mind;
        
        match = false; mind = -1;
        if (det != null)
        {
            for (i=0; (i<det.length) && (!match); i++)
            {
                if ((det[i] != null) && (det[i].getMature()))
                {
                    mnow = par.match(det[i], bod.getMatchLength());
                    if (mnow >= bod.getMatchLength()) { match = true; mind = i; }
                }
            }
        }
        
        return(mind);
    }
}
