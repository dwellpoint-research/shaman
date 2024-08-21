/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                    Audio / Video                      *
 *                                                       *
 *  October 2004                                         *
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

import cern.colt.matrix.ObjectFactory1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Convolution</h2>
 * 3x3 2D Convolition.
 */
public class Convolution extends Transformation
{     
    // **** Run-time Data ***
    private DataModel  datamodel;
    private boolean    greyscale;
    private double [][]matr, matg, matb;
    
    // **********************************************************\
    // *                    3x3 Convolution                     *
    // **********************************************************/
    private final double [][]CONVOLUTION_BLUR = new double[][]
    {
        new double[]{1.0, 1.0, 1.0},
        new double[]{1.0, 1.0, 1.0},
        new double[]{1.0, 1.0, 1.0}
    };
    
    private ObjectMatrix1D convolve(ObjectMatrix1D vidvec)
    {
        ObjectMatrix1D vidout;
        
        // Treat 1 or 3 channels depending on color or greyscale
        if (this.greyscale) vidout = convolveGrey(vidvec);
        else                vidout = convolveColor(vidvec);
        
        return(vidout);
    }
    
    private ObjectMatrix1D convolveGrey(ObjectMatrix1D vidvec)
    {
        double     [][]mg;
        ObjectMatrix1D vecout;
        double     [][]conv;
        
        // Get video matrix from input
        mg     = (double [][])vidvec.getQuick(0);
        
        // Prepare video buffer for output
        vecout = ObjectFactory1D.dense.make(1);
        checkVideoBuffer(mg);
        vecout.setQuick(0, this.matg);
        
        // Convolution
        conv = CONVOLUTION_BLUR;
        convolutionChannel(mg, this.matg, conv);
        
        return(vecout);
    }
    
    private ObjectMatrix1D convolveColor(ObjectMatrix1D vidvec)
    {
        double     [][]mr, mg, mb;
        ObjectMatrix1D vecout;
        double     [][]conv;
        
        // Get video matrices from input
        mr     = (double [][])vidvec.getQuick(0);
        mg     = (double [][])vidvec.getQuick(1);
        mb     = (double [][])vidvec.getQuick(2);
        
        // Prepare video buffer for output
        vecout = ObjectFactory1D.dense.make(3);
        checkVideoBuffer(mr);
        vecout.setQuick(0, this.matr);
        vecout.setQuick(1, this.matg);
        vecout.setQuick(2, this.matb);
        
        // Convolution
        conv = CONVOLUTION_BLUR;
        convolutionChannel(mr, this.matr, conv);
        convolutionChannel(mg, this.matg, conv);
        convolutionChannel(mb, this.matb, conv);
        
        return(vecout);
    }
    
    public static void convolutionChannel(double [][]matin, double [][]matout, double [][]conv)
    {
        int            h,w,i,j;
        double         a00, a01, a02, a10, a11, a12, a20, a21, a22, val;
        
        // 3x3 2D convolution
        h    = matin.length;
        w    = matin[0].length;
        for (i=1; i<h-1; i++)
        {
            for (j=1; j<w-1; j++)
            {
                a00 = matin[i-1][j-1]; a01 = matin[i-1][j  ]; a02 = matin[i-1][j+1];
                a10 = matin[i  ][j-1]; a11 = matin[i  ][j  ]; a12 = matin[i  ][j+1];
                a20 = matin[i+1][j-1]; a21 = matin[i+1][j  ]; a22 = matin[i+1][j+1];
                val = ( a00*conv[0][0] + a01*conv[0][1] + a02*conv[0][2]+
                        a10*conv[1][0] + a11*conv[1][1] + a12*conv[1][2]+
                        a20*conv[2][0] + a21*conv[2][1] + a22*conv[2][2]) / 9.0;
                matout[i][j] = val;
            }
        }
    }
    
    private void checkVideoBuffer(double [][]mat)
    {
        // Make sure the work buffers have the right size
        if ((this.matr == null) || 
            ((matr != null) && ((matr.length != mat.length) || (matr[0].length != mat[0].length))))
        {
            int w,h;
            
            h = mat.length;
            w = mat[0].length;
            this.matr = new double[h][w];
            this.matg = new double[h][w];
            this.matb = new double[h][w];
        }
    }
    
    public Object []transform(Object obin) throws DataFlowException
    {
        if (obin instanceof ObjectMatrix1D)
        {
            ObjectMatrix1D vidvec, outvec;
            
            vidvec = (ObjectMatrix1D)obin;
            outvec = convolve(vidvec);
            
            return(new Object[]{outvec});
        }
        else throw new DataFlowException("Cannot operate on non video vectors");
    }
    
    // **********************************************************\
    // *                 Parameter Configuration                *
    // **********************************************************/
    
    // **********************************************************\
    // *                Transformation Implementation           *
    // **********************************************************/
    public void init() throws ConfigException
    {
        DataModel  dmsup, dmout;
     
        // Make sure the input is compatible with this transformation's data requirements
        dmsup = getSupplierDataModel(0);
        checkDataModelFit(0, dmsup);
        this.datamodel = dmsup;
        dmout = dmsup;
        setOutputDataModel(0, dmout);
        
        // If there's one channel, input is in grey-scale
        if (this.datamodel.getAttributeCount() == 1) this.greyscale = true;
        else                                         this.greyscale = false;
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    public void checkDataModelFit(int port, DataModel dmin) throws ConfigException
    {
        if (!dmin.hasProperty(DataModelPropertyVideo.PROPERTY_VIDEO))
            throw new DataModelException("Cannot find Video property in input datamodel.");
    }
    
    public int getNumberOfInputs()  { return(1); }
    public int getNumberOfOutputs() { return(1); }
    public String getInputName(int port)
    {
         if   (port == 0) return("Video Input");
         else             return(null);
    }
    public String getOutputName(int port)
    {
         if   (port == 0) return("Video Output");
         else             return(null);
    }
     
    
    // **********************************************************\
    // *              Constructor/State Persistence             *
    // **********************************************************/
    public Convolution()
    {
       super();
       name        = "Convolution";
       description = "3x3 2D Convolution";
    }
}