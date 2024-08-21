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
import org.shaman.datamodel.AttributePropertyFuzzy;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;

import cern.jet.random.Uniform;



// *********************************************************\
// *                 Antibody Morphology                   *
// *********************************************************/
public class Morphology
{
    // *********************************************************\
    // *                         Data                          *
    // *********************************************************/
    private Body      bod;
    private DataModel dataModel;
    
    // Immune System Morphology Parameters
    public static final int MHC_NONE   = 0;
    public static final int MHC_RANDOM = 1;
    private int mhc;
    
    // AIS Particle Morphology
    private int particleLength;        // The length of a particle
    private int []fieldPos;            // The field positions
    
    // *********************************************************\
    // *                      Data Access                      *                                                                                                                                                                                                                                                                                                                                                                                                           *
    // *********************************************************/
    public Attribute getAttributeInParticleAt(int ind) throws AISException
    {
        Attribute []actatt;
        
        actatt = dataModel.getActiveAttributes();
        if (ind < particleLength) return(actatt[fieldPos[ind]]);
        else throw new AISException("Index in particle out of bounds. "+ind+" >= "+particleLength);
    }
    
    public AttributePropertyFuzzy getFuzzyPropertyAt(int ind) throws AISException
    {
        Attribute              at;
        AttributePropertyFuzzy atpfuz;
        
        at     = getAttributeInParticleAt(ind);
        atpfuz = (AttributePropertyFuzzy)at.getProperty(AttributePropertyFuzzy.PROPERTY_FUZZY);
        
        return(atpfuz);
    }
    
    public void setDataModel(DataModel _dataModel)
    {
        dataModel = _dataModel;
    }
    
    public DataModel getDataModel()
    {
        return(this.dataModel);
    }
    
    public int getParticleLength()   { return(particleLength); }
    public int []getFieldPositions() { return(fieldPos); }
    
    // *********************************************************\
    // *              Field Position Calculation               *                                                                                                                                                                                                                                                                                                                                                                                                           *
    // *********************************************************/
    public void makeFieldPositions() throws LearnerException
    {
        this.dataModel = bod.getDataModel();
        if      (bod.getDataRepresentation() == Body.DATA_FUZZY) fuzzyMakeFieldPositions();
        else if (bod.getDataRepresentation() == Body.DATA_BIT)   bitMakeFieldPositions();
    }
    
    private void fuzzyMakeFieldPositions()
    {
        int       i,j;
        int       parlen;
        int       []fpnew;
        Attribute []atts;
        
        // If no affinities defined, use the field order as they occur in the input data.
        // Count the number of active fields.
        atts   = dataModel.getAttributes();
        parlen = 0;
        for (i=0; i<atts.length; i++)
        {
            if (atts[i].getIsActive()) parlen++;
        }
        particleLength = parlen;
        
        // Make sure the particle strucure is valid
        fpnew = new int[parlen];
        j = 0;
        for (i=0; i<atts.length; i++)
        {
            if (atts[i].getIsActive()) fpnew[j] = j++;
        }
        fieldPos = fpnew;
        
        // Apply Major HistoComplex Field Reordering if required
        if (mhc == MHC_RANDOM) doMHC();
        
        System.out.println("Fuzzy Field Positions : ");
        for (i=0; i<particleLength; i++) System.out.print(fieldPos[i]+" ");
        System.out.println();
    }
    
    private void bitMakeFieldPositions() throws LearnerException
    {
        int       i,j,k,l;
        int       fparlen;
        int       []flen;
        int       parlen;
        int       []fp;
        Attribute []atts;
        int       numcat;
        
        // Count the number of active data fields
        atts    = dataModel.getAttributes();
        fparlen = 0;
        for (i=0; i<atts.length; i++) if (atts[i].getIsActive()) fparlen++;
        
        // Make a table with field length in bits
        flen = new int[fparlen];
        j    = 0;
        for (i=0; i<atts.length; i++)
        {
            if (atts[i].getIsActive())
            {
                // Number of Bits needed for a categorical attribute with n possible values is log2(n)
                if (atts[i].hasProperty(Attribute.PROPERTY_CATEGORICAL))
                {
                    try
                    {
                        numcat  = atts[i].getNumberOfCategories();
                        flen[j] = (int)(Math.log(numcat) / Math.log(2));
                    }
                    catch(DataModelException ex) { throw new LearnerException(ex); }
                }
                else throw new LearnerException("Cannot determine number of bits needed for a non-categorical attribute '"+atts[i].getName()+"'");
                j++;
            }
        }
        
        // Make the fieldpositions string.
        parlen = 0;
        for (i=0; i<fparlen; i++) parlen += flen[i];
        fp     = new int[parlen];
        l = 0; k = 0;
        for (i=0; i<atts.length; i++)
        {
            if (atts[i].getIsActive())
            {
                for (j=0; j<flen[l]; j++) fp[k++] = (j*atts.length) + i;
                l++;
            }
        }
        particleLength = parlen;
        fieldPos       = fp;
        
        // Apply Major HistoComplex Field Reordering if required
        if (mhc == MHC_RANDOM) doMHC();
        
        System.out.println("BitString Field Positions : ");
        for (i=0; i<particleLength; i++) System.out.print(fieldPos[i]+" ");
        System.out.println();
    }
    
    // *********************************************************\
    // *           Major Histo-compatibility Complex           *                                                                                                                                                                                                                                                                                                                                                                                                           *
    // *********************************************************/
    private void doMHC()
    {
        // Apply MHC field reordering to the current Field Position array
        int     i, fplen, buf, pos1, pos2;
        int     []perm;  // Random permutation for the FieldPostions
        
        fplen = fieldPos.length;
        perm  = new int[fplen];
        
        // Make a random permutation
        for (i=0; i<fplen; i++) perm[i] = fieldPos[i];
        for (i=0; i<fplen; i++)
        {
            pos1 = Uniform.staticNextIntFromTo(0, fplen-1);
            pos2 = Uniform.staticNextIntFromTo(0, fplen-1);
            buf        = perm[pos1];
            perm[pos1] = perm[pos2];
            perm[pos2] = buf;
        }
        
        // Reorder the fieldpositions according to the permutation
        fieldPos = perm;
    }
    
    // *********************************************************\
    // *                 Morphology Options                    *                                                                                                                                                                                                                                                                                                                                                                                                           *
    // *********************************************************/
    public void setMHC(int mhc) { this.mhc = mhc;   }
    public int  getMHC()        { return(this.mhc); }
    
    // ********************************************************\
    // *             Initialization & Cleanup                 *
    // ********************************************************/
    public Object clone() throws CloneNotSupportedException
    {
        Morphology mout;
        
        mout                = new Morphology(bod);
        mout.mhc            = this.mhc;
        mout.dataModel      = (DataModel)this.dataModel.clone();
        mout.fieldPos       = (int [])this.fieldPos.clone();
        mout.particleLength = this.particleLength;
        
        return(mout);
    }
    
    public Morphology(Body _bod)
    {
        bod = _bod;
    }
}
