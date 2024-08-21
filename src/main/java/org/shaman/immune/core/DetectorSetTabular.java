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

import org.shaman.exceptions.DataModelException;

import cern.jet.random.Uniform;


/*********************************************************\
 *       A dynamic programming based algorithm           *
 *             for generating antibodies                 *
\*********************************************************/
public class DetectorSetTabular extends DetectorSet
{
    /*********************************************************\
     *      Dynamic programming based detector generator     *
    \*********************************************************/
    // Helman / Greedy Detector Generation / Cloning Algorithm Data
    // ------------------------------------------------------------
    int      [][]cr;                     // Right to left non-self schema count tables
    int      [][]cl;                     // Left to right non-self schema count tables
    int      [][]ds;                     // Valid detector schema indicator table
    int      [][]crr;                    // Right to left detector count table
    int      [][]crl;                    // Left to right detector count table
    int      [][]dr;                     // Detector count table
    int      []clen;                     // Lengths of the count arrays
    int      []numFMF;                   // # of FMF's at each of the positions
    int      [][]divind;                 // Schema generation data
    int      []dind;                     // Detector index buffer for generating neighbors
    int      []indleft;                  // Can hold the indexes of a schema's left neighbours
    int      []indright;                 // Can hold the indexes of a schema's right neighbours
    
    /*********************************************************\
     *       Helman Fuzzy-Detector Generation Algorithm      *
     \*********************************************************/
    public void generate(double pf, int maxDet) throws AISException
    {
        generate(true, pf, maxDet);
    }
    
    public void generate(int maxDet) throws AISException
    {
        generate(false, 1.0, maxDet);
    }
    
    public void generate(boolean guess, double pf, int maxDet) throws AISException
    {
        boolean       ok;
        int           l, r, lmr;
        int           i, j, k, m;
        double        th;
        int           tpos;
        int           dinow;
        double        dmin, dmax, dstep;
        Self          ps;
        double      []ptdat;
        int           cilen, cmul;
        int         []dind;
        DetectorFuzzy pt;
        DetectorFuzzy deti;
        Vector        detvec;
        
        ok = true;
        
        // Initialize abbreviations
        numDet = 0;
        detvec = new Vector();
        
        l      = bod.getMorphology().getParticleLength();
        r      = bod.getMatchLength();
        lmr    = l-r;
        //th     = fuzzyMatchThreshold;
        th     = bod.getMatchLength();
        
        System.out.println("selfsize : "+l+" matchsize "+r+" fuzzyMatchThreshold "+th);
        
        // Initialize tables
        cr     = new int[l-r+1][];
        cl     = new int[l-r+1][];
        clen   = new int[l-r+1];
        divind = new int[l-r+1][r];
        dind   = new int[r];
        
        // Make a working detector
        pt = new DetectorFuzzy();
        pt.init(l, bod);
        ptdat = pt.getData();
        
        // Get # of Fuzzy Matching Functions per position
        numFMF = new int[l];
        try
        {
            for (i=0; i<l; i++)
            {
                numFMF[i] = bod.getMorphology().getAttributeInParticleAt(i).getNumberOfCategories();
            }
        }
        catch(DataModelException ex) { throw new AISException("Can't work with non-categorical data", ex); }
        
        // Perform sanity check on the match-length. Drop out when things look to huge to handle.
        boolean huge;
        huge = false;
        for (i=0; (i<l-r+1) && (!huge); i++)
        {
            cilen = 1;
            for (j=i; j<i+r; j++)
            {
                cilen *= numFMF[j];
                if (cilen >= Integer.MAX_VALUE/2) huge = true;
            }
        }
        if (huge) throw new AISException("Algorithm overflow danger! Please use other algorithm!");
        
        
        // Calculate length of the Cr tables. Then allocate and initialize them.
        try
        {
            for (i=0; i<l-r+1; i++)
            {
                cilen    = 1;
                for (j=i; j<i+r; j++) cilen *= numFMF[j];
                clen[i]  = cilen;
                cr[i]    = new int[cilen];
                cl[i]    = new int[cilen];
                for (j=0; j<cilen; j++) { cr[i][j] = -1; cl[i][j] = -1; }
            }
        }
        catch(OutOfMemoryError ex)
        {
            ex.printStackTrace();
            throw new AISException("Not enough memory! Please use other algorithm!");
        }
        
        // Initialize the schema generation data tables
        for (i=0; i<l-r+1; i++)
        {
            dinow = 1;
            for (j=r-1; j>=0; j--)
            {
                divind[i][j] = dinow;
                dinow       *= numFMF[i+j];
            }
        }
        
        // Preprocessing step : Put all r-length-detectorschemas that match with a self-particle as 0's in c
        // Runtime Complexity O(numself * (numdiv^r)) !   Highest of the whole algorithm.
        //   possible optimization... Use self->detector convertion code and only zero that one....
        
        
        System.out.println("Creating fuzzy detector tables");
        
        for (i=0; (i<bod.getNumberOfSelfs()); i++)
        {
            ps    = bod.getSelf(i);
            
            for (j=0; (j<l-r+1); j++)
            {
                for (k=0; (k<clen[j]); k++)
                {
                    makeDetectorSchema(pt, j, k, r);    // Make the detectorschema
                    if (ps.matchSubString(pt, j, j+r) >= th) cr[j][k] = 0;
                }
            }
        }
        for (i=0; (i<clen[l-r]); i++) if (cr[l-r][i] == -1) cr[l-r][i] = 1;
        System.out.println();
        
        // Solve recursion equation
        int cis, cisp, jmod;
        int []ddiv;
        int []ddivp1;
        
        System.out.println("Solving the fuzzy detector recursion equation");
        for (i=l-r-1; (i>=0); i--)
        {
            ddiv   = divind[i];
            ddivp1 = divind[i+1];
            
            for (j=0; (j<clen[i]); j++)
            {
                if (cr[i][j] != 0)
                {
                    jmod = j;
                    for (k=0; k<r; k++)
                    {
                        dind[k] = jmod / ddiv[k];
                        jmod    = jmod % ddiv[k];
                    }
                    for (k=1; k<r; k++) dind[k-1] = dind[k];
                    dind[r-1] = 0;
                    
                    cisp = 0;
                    for (k=0; k<r; k++) cisp += dind[k]*ddivp1[k];
                    
                    cis = 0; for (k=0; k<numFMF[i+r]; k++) {  cis += cr[i+1][cisp]; cisp++;  }
                    cr[i][j] = cis;
                }
            }
        }
        
        // Count maximal amount of antibodies
        int maxantic;
        maxantic = 0;
        for (i=0; (i<clen[0]); i++)
        {
            maxantic += cr[0][i];
        }
        
        // Determine the number of detectors
        if (maxantic == 0) numDet = 0;
        else
        {
            if (maxantic < maxDet)
            {
                if (guess)
                {
                    // ADD VOODOO TO DO THIS
                    numDet = (int)(maxantic*pf);
                }
                else numDet = maxantic;
            }
            else
            {
                numDet = (int)(maxDet*pf);
                //if (bod.guessNumDet) numDet = (int)(bod.maxDet*bod.pf);
                //else                 numDet = bod.maxDet;
            }
        }
        System.out.println("Maximum number of detectors "+maxantic+". Number of detectors "+numDet);
        if (numDet == 0) return;
        
        // Generate detectors from the table
        if (det != null)
        {
            if (det.length != numDet) det = new Detector[numDet];
        }
        else det = new Detector[numDet];
        
        int []detind = new int[numDet];
        
        // Generate 'numDet' random indices in the detector 'c' table
        System.out.println("Selecting "+numDet+" of "+maxantic+" detectors");
        int     ranind;
        boolean already, founddouble;
        
        ranind    = Uniform.staticNextIntFromTo(0, maxantic-1);
        detind[0] = ranind;
        for (i=1; (i<numDet); i++)
        {
            ranind = Uniform.staticNextIntFromTo (0, maxantic-1);
            do
            {
                already     = false;
                for (j=0; (j<i) && (!already); j++)
                {
                    if (detind[j] == ranind) already = true;
                }
                if (already)
                {
                    ranind++; //      = (int)Random.generate(Random.UNIFORM, 0, maxantic);
                    if (ranind >= maxantic) ranind = 0;
                }
            }
            while (already);
            
            detind[i] = ranind;
        }
        System.out.println("");
        
        // Generate the detectors
        System.out.println("Generating "+numDet+" of "+maxantic+" possible detectors");
        
        double []detdat;
        int    indsum, indbeg, cinddiv, indact, inddiv, indpar, ciind;
        
        for (i=0; i<numDet; i++)
        {
            ranind  = detind[i];
            
            deti    = new DetectorFuzzy();
            deti.init(l, bod);
            detdat  = deti.getData();
            
            indbeg = 0;
            indact = 0;
            j      = 0;
            while ((j < l))
            {
                if (j==0)
                {
                    // Generate the first 'r' variables of the detector
                    
                    // At which position in c[0] is the detector located
                    indbeg = 0; indsum = cr[0][0];
                    indact = ranind+1;
                    while (indsum < indact) { indbeg++; indsum += cr[0][indbeg]; }
                    
                    // Calculate the first 'r' positions of the detector from the position in c[0]
                    indact = indbeg;
                    inddiv = 1; for (k=0; k<r-1; k++) inddiv *= numFMF[k];
                    for (k=r-1; (k>=0); k--)
                    {
                        indpar    = indact / inddiv;
                        indact    = indact % inddiv;
                        detdat[j] = indpar;
                        if (k > 0) inddiv   /= numFMF[k-1];
                        j++;
                    }
                    
                    // Calculate position of the detector in c[r] ... c[l]
                    indact = ranind+1;
                    for (k=0; (k<indbeg); k++) indact -= cr[0][k];
                }
                else
                {
                    // Generate the next variable of the detector
                    detdat[j] = 0;
                    ciind     = 0;
                    
                    // Find begin-index in cr[j-r+1] table
                    cmul = 1;
                    for (k=j-r+2; (k<=j); k++) cmul *= numFMF[k];
                    for (k=j-r+1; (k<j); k++)
                    {
                        ciind += detdat[k] * cmul;
                        cmul  /= numFMF[k+1];
                    }
                    
                    // Find position from the begin-index. This is the value of this position.
                    indsum = cr[j-r+1][ciind]; indpar = 0;
                    while ((indsum < indact)){  indpar++; indsum += cr[j-r+1][ciind+indpar]; }
                    detdat[j] = indpar;
                    for (k=0; (k<indpar); k++) indact -= cr[j-r+1][ciind+k];
                    
                    j++;
                }
            }
            
            // Add detector to buffer
            detvec.add(deti);
        }
        System.out.println("");
        
        if (ok)
        {
            det = new Detector[detvec.size()];
            detvec.copyInto(det);
            numDet = detvec.size();
        }
        
        // Print c table
        /*
         System.out.println();
         for (i=0; i<maxDet; i++)
         {
         for (j=0; j<l; j++) System.out.print(det[i].getData()[j]+" ");
         System.out.println();
         }
         System.out.println();
         
         for (i=0; i<l-r+1; i++)
         {
         for (j=0; j<clen[i]; j++) System.out.print(cr[i][j]+" ");
         System.out.println();
         }*/
    }
    
    /*********************************************************\
     *          Fuzzy-Detector Generation Algorithm          *
     \*********************************************************/
    private void makeDetectorSchema(DetectorFuzzy d, int b, int ind, int r)
    {
        // Subfunction of generateDetectorsFuzzyGreedy() and -Helman()
        // b = beginposition
        // i = index in the count[b] table
        // r = matchlen
        
        int    i;
        int    indnow;
        int    inddig;
        double []ddat;
        int    []di;
        
        di     = divind[b];
        ddat   = d.getData();
        indnow = ind;
        for (i=b; i<b+r; i++)
        {
            inddig  = indnow / di[i-b];
            indnow -= inddig * di[i-b];
            ddat[i] = inddig;
        }
    }
    
    public DetectorSetTabular(Body _bod)
    {
        bod = _bod;
    }
}
