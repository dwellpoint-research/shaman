/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                    Audio / Video                      *
 *                                                       *
 *  September 2004                                       *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2004-5 Shaman Research                 *
\*********************************************************/
package org.shaman.av;

import org.shaman.dataflow.Transformation;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.TransformationException;

import cern.colt.matrix.DoubleMatrix1D;


/**
 * <h2>Frequency Filter</h2>
 * Audio Frequency Filter in the Frequency domain.
 */
public class FrequencyFilter extends Transformation
{ 
    public static final int TYPE_SQUARE = 1;
    
    // Cut-off parameters
    private int    type;
    private double par1;
    private double par2;
    
    // **** Run-time Data ***
    private DataModel datamodel;
    
    // **********************************************************\
    // *                  Frequency Filter                      *
    // **********************************************************/
    private DoubleMatrix1D filter(DoubleMatrix1D vec) throws TransformationException
    {
        int   i, nd2;
        int   pos1,  pos2;
        int   posi1, posi2;
        
        nd2   = vec.size()/2;
        pos1  = (int)((nd2*par1)/2);
        pos2  = (int)((nd2*par2)/2);
        posi1 = nd2-pos1;
        posi2 = nd2-pos2;
        
        //System.err.println(pos1+" - "+pos2+" and "+posi1+" - "+posi2);
        
        
        //if ((i<4020) || (i>vec.size()-4020))continue;
        
        pos1  = (int)(nd2*(1.0-par1));
        pos2  = (int)(nd2*(1.0-par2));
        posi1 = vec.size()-pos1;
        posi2 = vec.size()-pos2;
        System.err.println(pos1+" - "+pos2+" and "+posi1+" - "+posi2);
        for (i=0; i<vec.size(); i+=2)
        {
            if ((i<pos1) || (i>posi1)) { vec.setQuick(i, 0); vec.setQuick(i+1, 0); }
//            if (i<nd2)
//            {
//                if ((i<pos2) || (i>pos1)) { vec.setQuick(i, 0); vec.setQuick(i+1, 0); } 
//            }
//            else
//            {
//                if ((i<posi1) || (i>posi2)) { vec.setQuick(i, 0); vec.setQuick(i+1, 0); } 
//            }
        }
        
        return(vec);
    }
    
    
    
    public void transform() throws DataFlowException
    {
        Object in;
        while (this.areInputsAvailable(0, 1))
        {
            in = this.getInput(0);
            if (in != null)
            {
                DoubleMatrix1D out;
                
                out = filter((DoubleMatrix1D)in);
                setOutput(0, out);
            }
        }
   }
    
    
    // **********************************************************\
    // *                 Parameter Configuration                *
    // **********************************************************/
    public void setType(int type)
    {
        this.type = type;
    }
    
    public void setParameter1(double par1)
    {
        this.par1 = par1;
    }
    
    public void setParameter2(double par2)
    {
        this.par2 = par2;
    }
    
    // **********************************************************\
    // *                Transformation Implementation           *
    // **********************************************************/
    public void init() throws ConfigException
    {
        DataModel  dmsup;
     
        // Make sure the input is compatible with this transformation's data requirements
        dmsup = getSupplierDataModel(0);
        checkDataModelFit(0, dmsup);
        this.datamodel = dmsup;
        setOutputDataModel(0, dmsup);
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    public void checkDataModelFit(int port, DataModel dmin) throws DataModelException
    {
         // Check if input datamodel is an audio model
         if (!dmin.getName().startsWith("Audio"))
           throw new DataModelException("Input DataModel does not contain Audio data");
    }
    
    public int getNumberOfInputs()  { return(1); }
    public int getNumberOfOutputs() { return(1); }
    public String getInputName(int port)
    {
         if   (port == 0) return("Audio Input");
         else             return(null);
    }
    public String getOutputName(int port)
    {
         if   (port == 0) return("Audio Frequency Output");
         else             return(null);
    }
     
    
    // **********************************************************\
    // *              Constructor/State Persistence             *
    // **********************************************************/
    public FrequencyFilter()
    {
       super();
       name        = "Audio Filter";
       description = "Filter Audio Frequencies";
    }
}
