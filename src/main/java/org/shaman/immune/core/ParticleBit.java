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
import org.shaman.datamodel.DataModel;

import cern.colt.bitvector.BitVector;
import cern.colt.function.IntProcedure;
import cern.colt.matrix.DoubleMatrix1D;


// *********************************************************\
// *           Bit-String Particle Base Class for          *
// *   Self-Particle, Antigen, Detector, Memory-Particle   *
// *           with contiguous bit matching rule           *
// *********************************************************/
public class ParticleBit extends ParticleBase
{
    // ********************************************************\
    // *                          Data                         *
    // ******************************************************/
    BitVector data;               // Bitstring data
    
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(" 1x"+data.size()+" bitvector\n");
        for (int i=0; i<data.size(); i++)
        {
            sb.append((data.get(i)?1.0:0.0)+" ");
        }
        
        return(sb.toString());
    }
    
    // *******************************************************\
    //  Converting from raw data to internal representation  *
    // ******************************************************/
    public void compile(Morphology morpho, DoubleMatrix1D datob) throws AISException
    {
        DataModel   dm;
        Attribute []atts;
        int         i, numatt;
        int       []idat;
        int         datnow, bitpos;
        boolean     bitnow;
        int       []fieldpos;
        
        dm       = morpho.getDataModel();
        atts     = dm.getActiveAttributes();
        numatt   = atts.length;
        fieldpos = morpho.getFieldPositions();
        idat     = new int[numatt];
        for (i=0; i<numatt; i++) idat[i] = 0;
        
        // Make integer data from the raw records (only the active fields)
        for (i=0; i<numatt; i++)
        {
            idat[i] = (int)datob.get(i); // toIntegerData(dm, i, datob.get(i));
        }
        
        // Order the integer data into this particle's data store according to the body's field order
        data.clear();
        for (i=0; i<this.len; i++)
        {
            datnow = idat[fieldpos[i]%numatt];
            bitpos =      fieldpos[i]/numatt;
            bitnow = (datnow & (1<<bitpos)) != 0;
            if (bitnow) data.set(i);
        }
    }
    
    private double toIntegerData(DataModel dm, int pos, double d)
    {
        return(d);
    }
    
    // *******************************************************\
    // *           Contiguous Bit String Matching            *
    // *******************************************************/
    public double matchParticle(Particle b, int matchlen)
    {
        ParticleBit bbit;
        BitVector   matchpattern;
        int         patternmatchlength;
        
        // Calculate match positions of the 2 particle's bitvectors
        bbit         = (ParticleBit)b;
        matchpattern = this.data.copy();
        matchpattern.xor(bbit.getData());
        
        // Apply Contiguous matching rule.
        ContiguousMatcher matcher = new ContiguousMatcher();
        matchpattern.forEachIndexFromToInState(0, data.size()-1, false, matcher);
        
        // Get the maximum number of contiguous bit that match in both particles
        patternmatchlength = matcher.getMatchLength();
        
        System.out.println("BIT MATCH LENGTH "+patternmatchlength);
        
        return(patternmatchlength);
    }
    
    public double match(Detector b, int matchLen)
    {
        double []out = new double[2];
        
        match(b,matchLen, out);
        
        return(out[1]);
    }
    
    public void   match(Detector b, int matchLen, double []out)
    {
        // out[0] = position of match
        // out[1] = match strength
        
        ParticleBit bbit;
        BitVector   matchpattern;
        int         patternmatchlength;
        
        // Calculate match positions of the 2 particle's bitvectors
        bbit         = (DetectorBit)b;
        matchpattern = this.data.copy();
        matchpattern.xor(bbit.getData());
        
        // Apply Contiguous matching rule.
        ContiguousMatcher matcher = new ContiguousMatcher();
        matchpattern.forEachIndexFromToInState(0, data.size()-1, false, matcher);
        
        // Get the maximum number of contiguous bit that match in both particles and where this match starts
        out[0] = matcher.getMatchPosition();
        out[1] = matcher.getMatchLength();
        
        //System.out.println("BITPARTICLE - DETECTOR MATCH LENGTH "+out[1]);
    }
    
    public double matchSubString(Detector b, int bpos, int epos)
    {
        int      i,j;
        
        return(0);
    }
    
    // *********************************************************\
    // *              Bit-String data interface                *
    // *********************************************************/
    public BitVector getData() { return(data); }
    public void setData(BitVector data)
    {
        this.data = data;
    }
    
    // *********************************************************\
    // *               Initialization & Cleanup                *
    // *********************************************************/
    public void init(int _len)
    {
        this.bod    = null;
        this.len    = _len;
        this.data   = new BitVector(_len);
        this.active = true;
    }
    
    public void init(int _len, Body _bod)
    {
        this.bod    = _bod;
        this.len    = _len;
        this.data   = new BitVector(_len);
        this.active = true;
    }
    
    public ParticleBit(int _len)
    {
        this.active = true;
        this.len    = _len;
        this.data   = new BitVector(_len);
    }
    
    public ParticleBit()
    {
        this.data = null; this.len = 0; this.active = false;
    }
}


/**
 * COLT Procedure for contiguous bit match length calculation.
 * @author Johan Kaers
 */
class ContiguousMatcher implements IntProcedure
{
    // Length of current match
    private int matchlen;
    
    // Current position in particle
    private int pos; 
    
    // Length and Position of maximum match
    private int maxmatchlen;
    private int maxbeginpos;
    
    public boolean apply(int element)
    {
        if (element == (pos+1))
        {
            matchlen++;
            pos++;
        }
        else
        {
            // Remember the match if it was the biggest so far.
            if (matchlen > maxmatchlen)
            {
                maxmatchlen = matchlen;
                maxbeginpos = pos-matchlen;
            }
            
            // Reset the matchlength and position to start values
            matchlen = 1;
            pos      = element;
        }
        
        return(true);
    }
    
    public int getMatchLength()
    {
        return(maxmatchlen);
    }
    
    public int getMatchPosition()
    {
        return(maxbeginpos);
    }
    
    public ContiguousMatcher()
    {
        this.matchlen    =  0;
        this.pos         = -1;
        this.maxmatchlen =  0;
        this.maxbeginpos = -1;
    }
}
