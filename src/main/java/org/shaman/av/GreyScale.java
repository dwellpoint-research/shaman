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
import org.shaman.exceptions.TransformationException;

import cern.colt.matrix.ObjectFactory1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>GreyScale</h2>
 * Colour to Greyscale video convertor
 */
public class GreyScale extends Transformation
{     
    // **** Run-time Data ***
    private DataModel  datamodel;
    private double [][]matgrey;
    
    // **********************************************************\
    // *                  Frequency Filter                      *
    // **********************************************************/
    private ObjectMatrix1D greyScale(ObjectMatrix1D vidvec) throws TransformationException
    {
        ObjectMatrix1D greyvec;
        double     [][]matr, matg, matb;
        
        
        matr    = (double [][])vidvec.getQuick(0);
        matg    = (double [][])vidvec.getQuick(1);
        matb    = (double [][])vidvec.getQuick(2);
        checkVideoBuffer(matg);
        greyvec = ObjectFactory1D.dense.make(1);
        greyvec.setQuick(0, this.matgrey);
        
        greyScale(matr, matg, matb, this.matgrey);
        
        return(greyvec);
    }
    
    public static void greyScale(double [][]matr, double [][]matg, double [][]matb, double [][]matgrey)
    {
        int            height, width, i,j;
        double         r,g,b,grey;
        
        height = matr.length;
        width  = matr[0].length;
        for (i=0; i<height; i++)
        {
            for (j=0; j<width; j++)
            {
                 r    = matr[i][j];
                 g    = matg[i][j];
                 b    = matb[i][j];
                 grey = (r+g+b)/3;
                 matgrey[i][j] = grey;
            }
        }
    }
    
    private void checkVideoBuffer(double [][]mat)
    {
        // Make sure the work buffers have the right size
        if ((this.matgrey == null) || 
            ((this.matgrey != null) && ((matgrey.length != mat.length) || (matgrey[0].length != mat[0].length))))
        {
            int w,h;
            
            h = mat.length;
            w = mat[0].length;
            this.matgrey = new double[h][w];
        }
    }
    
    public Object []transform(Object obin) throws DataFlowException
    {
        if (obin instanceof ObjectMatrix1D)
        {
            ObjectMatrix1D vidvec, greyvec;
            
            vidvec  = (ObjectMatrix1D)obin;
            greyvec = greyScale(vidvec);
            
            return(new Object[]{greyvec});
        }
        else throw new TransformationException("Cannot operate on non video vectors");
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
        
        // Convert input (Color) DataModel to the corresponding GreyScale one
        dmout = AVDataModels.makeGreyScaleDataModel(this.datamodel);
        
        // Install greyscale datamodel as output model.
        setOutputDataModel(0, dmout);
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    public void checkDataModelFit(int port, DataModel dmin) throws DataModelException
    {
        if (!dmin.getAttribute(0).hasProperty(DataModelPropertyVideo.PROPERTY_VIDEO))
            throw new DataModelException("Cannot find Video property in input datamodel.");
        else
        {
            if (dmin.getAttributeCount() != 3)
                throw new DataModelException("Color Video datamodel expected but not found at input.");
        }
    }
    
    public int getNumberOfInputs()  { return(1); }
    public int getNumberOfOutputs() { return(1); }
    public String getInputName(int port)
    {
         if   (port == 0) return("Color Video Input");
         else             return(null);
    }
    public String getOutputName(int port)
    {
         if   (port == 0) return("GreyScale Video Output");
         else             return(null);
    }
     
    
    // **********************************************************\
    // *              Constructor/State Persistence             *
    // **********************************************************/
    public GreyScale()
    {
       super();
       name        = "GreyScale";
       description = "Color to Greyscale video convertion";
    }
}