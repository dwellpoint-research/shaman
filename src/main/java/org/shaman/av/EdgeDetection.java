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
import cern.jet.random.Uniform;


/**
 * <h2>Edge Detection</h2>
 * The ultimate edge detection algorithm.
 */
public class EdgeDetection extends Transformation implements ParameterControl
{     
    // **** Run-time Data ***
    private DataModel datamodel;
    private boolean   greyscale;
    private double [][]matvid;
    private double [][]sobhor, sobver;
    private double [][]matfilt;
    private double [][]matdir, matdirfilt;
    private double     threshold, alpha, alphadir;
    
    // Angle : 0 = 0, 0.5 = pi/2, 1.0 = pi
    
    private byte [][]e = new byte[][]
            { new byte []{0,0,0},
              new byte []{0,0,0},
              new byte []{0,0,0}
            };
    
    private byte [][]e0 = new byte[][]
            { new byte []{0,0,0},
              new byte []{1,1,1},        // 0
              new byte []{1,1,1}
            };
    
    private byte [][]e1 = new byte[][]
            { new byte []{0,0,1},
              new byte []{1,1,1},        // 0.18
              new byte []{1,1,1}
            };
    
    private byte [][]e2 = new byte[][]
            { new byte []{0,0,1},
              new byte []{0,1,1},        // 0.25
              new byte []{1,1,1}
            };
    
    private byte [][]e3 = new byte[][]
            { new byte []{0,1,1},
              new byte []{0,1,1},        // 0.5
              new byte []{0,1,1}
            };
    
    private byte [][]e4 = new byte[][]
            { new byte []{1,1,1},
              new byte []{0,1,1},        // 0.68
              new byte []{0,1,1}
            };
    
    private byte [][]e5 = new byte[][]
            { new byte []{1,1,1},
              new byte []{0,1,1},        // 0.75
              new byte []{0,0,1}
            };
    
    private byte [][]e6 = new byte[][]
            { new byte []{1,1,1},
              new byte []{1,1,1},        // 1.00
              new byte []{0,0,0}
            };
    
    private Object [][]edgetemp = new Object[][]{
            new Object[]{e0, new Double(0.00)},
            new Object[]{e1, new Double(0.18)},
            new Object[]{e2, new Double(0.25)},
            new Object[]{e3, new Double(0.5)},
            new Object[]{e4, new Double(0.68)},
            new Object[]{e5, new Double(0.75)},
            new Object[]{e6, new Double(1.00)},
    };
                                               
    
    // **********************************************************\
    // *                  Parameter Control                     *
    // ************************************************************/
    public void setParameter(double par)
    {
        System.err.println("Edge-detection parameter "+par);
        this.threshold = par;
    }
    
    public double getParameter()
    {
        return(this.threshold);
    }
    
    // **********************************************************\
    // *                     Edge Detection                     *
    // **********************************************************/
    private ObjectMatrix1D edgeDetect(ObjectMatrix1D vidvec) throws TransformationException
    {
        ObjectMatrix1D vidout;
        
        vidout = edgeCircle(vidvec);
        //vidout = edgeDetectReal(vidvec);
        
        return(vidout);
    }
    
    private ObjectMatrix1D edgeCircle(ObjectMatrix1D vidvec) throws TransformationException
    {
        ObjectMatrix1D vecout;
        double     [][]matin;
        double     [][]matamp, matdir;
        
        matin     = (double [][])vidvec.getQuick(0);
        checkVideoBuffer(matin);
        matamp = this.matfilt;
        matdir = this.matdir;
        
        
        double   w,h,mx,my,x,y,t,r,a,astep;
        int      s,i,ix,iy;
        
        w  = matamp[0].length;
        h  = matamp.length;
        
        // Clear area
        for (iy=0; iy<h; iy++)
        {
            for (ix=0; ix<w; ix++) { matdir[iy][ix] = -2; matamp[iy][ix] = 0.0; }
        }
        
        // Draw circle
        mx = w/2.0 + Uniform.staticNextIntFromTo(-100,100);
        my = h/2.0 + Uniform.staticNextIntFromTo(-80,80);
        //mx = w/2.0;
        //my = h/2.0;
        System.err.println("CIRCLE at "+mx+", "+my);
        r  = 30;
        s  = 1000;
        astep = (Math.PI*2)/s;
        a     = 0;
        for (i=0; i<s; i++)
        {
            x  = mx + Math.cos(a)*r;
            y  = my + Math.sin(a)*r;
            
            //t  = (a/Math.PI)-1.0;
            if (a < Math.PI) t = a;
            else             t = 2*Math.PI-a;
            
            ix = (int)x;
            iy = (int)y;
            if (ix <  0) ix = 0;
            if (ix >= w) ix = (int)w-1;
            if (iy <  0) iy = 0;
            if (iy >= h) iy = (int)h-1;
             
            matamp[iy][ix] = 1.00;
            matdir[iy][ix] = t;
            a += astep;
        }        
        
        vecout = ObjectFactory1D.dense.make(2);
        vecout.setQuick(0, matamp);
        vecout.setQuick(1, matdir);
        
        return(vecout);
    }
    
    private ObjectMatrix1D edgeDetectReal(ObjectMatrix1D vidvec) throws TransformationException
    {
        ObjectMatrix1D vidout;
        
        // Apply Sobel edge detection algorithm in color or greyscale
        if (this.greyscale) vidvec = edgeDetection(vidvec);
        else
        {
            // Edge detection
            vidvec = sobelColor(vidvec);
        }
        
        // Threshold and low-pass filter
        vidvec = threshold(vidvec);
        
        // Edge Templates
        //vidvec = edgeTemplates(vidvec);
        
        // Gradient detection
        vidvec = gradient(vidvec);
        
        
        // Done.
        vidout = vidvec;
            
        return(vidout);
    }
    
    private ObjectMatrix1D gradient(ObjectMatrix1D vidvec)
    {
        double [][]matamp;
        double [][]matdir;
        int        i,j,w,h;
        double a00, a01, a02, a03, a04;
        double a10, a11, a12, a13, a14;
        double a20, a21, a22, a23, a24;
        double a30, a31, a32;
        double a40, a41, a42;
        double gx, gy;
        double g;
        
        final double [][]gradv = new double[][]
              {
                 new double []{-1,-2,-3,-2,-1},
                 new double []{ 0, 0, 0, 0, 0},
                 new double []{ 1, 2, 3, 2, 1}
              };
        final double [][]gradh = new double[][]
              {
                 new double []{-1, 0, 1},
                 new double []{-2, 0, 2},
                 new double []{-3, 0, 3},
                 new double []{-2, 0, 2},
                 new double []{-1, 0, 1},
              };
        
        matamp = (double [][])vidvec.getQuick(0);
        matdir = (double [][])vidvec.getQuick(1);
        w      = matamp[0].length;
        h      = matamp.length;
        
        double max = 0;
        double min = 50;
        for(i=2; i<h-2; i++)
        {
            for (j=2; j<w-2; j++)
            {
                if (matamp[i][j] > 0.2)
                {
                    a00 = matamp[i-2][j-2]; a01 = matamp[i-2][j-1]; a02 = matamp[i-2][j]; a03 = matamp[i-2][j+1]; a04 = matamp[i-2][j+2];
                    a10 = matamp[i-1][j-2]; a11 = matamp[i-1][j-1]; a12 = matamp[i-1][j]; a13 = matamp[i-1][j+1]; a14 = matamp[i-1][j+2];
                    a20 = matamp[i  ][j-2]; a21 = matamp[i  ][j-1]; a22 = matamp[i  ][j]; a23 = matamp[i  ][j+1]; a24 = matamp[i  ][j+2];
                    a30 = matamp[i+1][j-2]; a31 = matamp[i+1][j-1]; a32 = matamp[i+1][j];
                    a40 = matamp[i+2][j-2]; a41 = matamp[i+2][j-1]; a42 = matamp[i+2][j];
                    gy  = a00*gradv[0][0] + a01*gradv[0][1] + a02*gradv[0][2] + a03*gradv[0][3] + a04*gradv[0][4] +
                          a10*gradv[1][0] + a11*gradv[1][1] + a12*gradv[1][2] + a13*gradv[0][3] + a14*gradv[0][4] +
                          a20*gradv[2][0] + a21*gradv[2][1] + a22*gradv[2][2] + a23*gradv[0][3] + a24*gradv[0][4];
                    gx  = a00*gradh[0][0] + a01*gradh[0][1] + a02*gradh[0][2] +
                          a10*gradh[1][0] + a11*gradh[1][1] + a12*gradh[1][2] +
                          a20*gradh[2][0] + a21*gradh[2][1] + a22*gradh[2][2] +
                          a30*gradh[3][0] + a31*gradh[3][1] + a32*gradh[3][2] +
                          a40*gradh[4][0] + a41*gradh[4][1] + a42*gradh[4][2];
                    if (gx != 0) g = (Math.atan(gy/gx) / (Math.PI/2)); // + 0.5;
                    else         g = -2;
                }
                else g = -2;
                matdir[i][j] = g;
            }
        }
                
        return(vidvec);
    }
    
    
    private ObjectMatrix1D edgeTemplates(ObjectMatrix1D vidvec)
    {
        double [][]matamp;
        double [][]matdir;
        int        i,j,k,w,h;
        byte       a00, a01, a02;
        byte       a10, a11, a12;
        byte       a20, a21, a22;
        Object   []tempob;
        byte   [][]temp;
        boolean    cm;
        double     dir, dirfilt;
        final double ampthres = 0.2;
        
        matamp = (double [][])vidvec.getQuick(0);
        matdir = (double [][])vidvec.getQuick(1);
        w      = matamp[0].length;
        h      = matamp.length;
        
        for (i=1; i<h-1; i++)
        {
            for (j=1; j<w-1; j++)
            {
                a00 = (matamp[i-1][j-1] > ampthres)?(byte)1:(byte)0;
                a01 = (matamp[i-1][j  ] > ampthres)?(byte)1:(byte)0;
                a02 = (matamp[i-1][j+1] > ampthres)?(byte)1:(byte)0;
                a10 = (matamp[i  ][j-1] > ampthres)?(byte)1:(byte)0;
                a11 = (matamp[i  ][j  ] > ampthres)?(byte)1:(byte)0;
                a12 = (matamp[i  ][j+1] > ampthres)?(byte)1:(byte)0;
                a20 = (matamp[i+1][j-1] > ampthres)?(byte)1:(byte)0;
                a21 = (matamp[i+1][j  ] > ampthres)?(byte)1:(byte)0;
                a22 = (matamp[i+1][j+1] > ampthres)?(byte)1:(byte)0;
                
                dir = -1;
                cm  = false;
                for (k=0; (k<this.edgetemp.length) && (!cm); k++)
                {
                    tempob = this.edgetemp[k];
                    temp   = (byte [][])tempob[0];
                    cm     = (a00 == temp[0][0]) && (a01 == temp[0][1]) && (a02 == temp[0][2]) && 
                             (a10 == temp[1][0]) && (a11 == temp[1][1]) && (a12 == temp[1][2]) && 
                             (a20 == temp[2][0]) && (a21 == temp[2][1]) && (a22 == temp[2][2]);
                    if (cm)
                    {
                        dir = ((Double)tempob[1]).doubleValue();
                        
                        if (this.matdirfilt[i][j] == -1) dirfilt = dir;
                        else
                        {
                            dirfilt = (this.matdirfilt[i][j]*this.alphadir) + ((1.0-this.alphadir)*dir);
                        }
                        matdir[i][j]          = dirfilt;
                        this.matdirfilt[i][j] = dirfilt;
                    }
                }
                
                if (this.matdirfilt[i][j] == -1) dirfilt = dir;
                else
                {
                    if (cm) dirfilt = (this.matdirfilt[i][j]*this.alphadir) + ((1.0-this.alphadir)*dir);
                    else    dirfilt = -1;//this.matdirfilt[i][j];
                }
                matdir[i][j]          = dirfilt;
                this.matdirfilt[i][j] = dirfilt;
            }
        }
        
        return(vidvec);
    }
    
    private ObjectMatrix1D threshold(ObjectMatrix1D vidvec)
    {
        int     i,j,w,h;
        double  [][]matv;
        ObjectMatrix1D vecedge;
        
        // Output has 2 channels : amplitude and direction
        vecedge = ObjectFactory1D.dense.make(2);
        matv = (double [][])vidvec.getQuick(0);
        vecedge.setQuick(0, matv);
        vecedge.setQuick(1, this.matdir);
        
        w    = matv[0].length;
        h    = matv.length;
        
        // Threshold
        for (i=0; i<h-2; i++)
        {
            for (j=0; j<w-2; j++)
            {
                if (matv[i][j] >this.threshold) matv[i][j] = 1.0;
                else                            matv[i][j] = 0.0;
            }
        }
        
        // Low-pass filter
        double v;
        
        for (i=0; i<h-2; i++)
        {
            for (j=0; j<w-2; j++)
            {
                if (this.matfilt[i][j] == -1)
                {
                    v = matv[i][j];
                    this.matdir[i][j] = 0.5;
                }
                else
                {
                    v = (this.matfilt[i][j]*this.alpha) + ((1-this.alpha)*matv[i][j]);
                }
                this.matfilt[i][j] = v;
                matv[i][j] = v;
                
                //this.matdir[i][j]  = -1;
            }
        }
        
        return(vecedge);
    }
    
    private ObjectMatrix1D sobelColor(ObjectMatrix1D vidvec)
    {
        ObjectMatrix1D  edgevec;
        double      [][]matr, matg, matb;
        
        matr    = (double [][])vidvec.getQuick(0);
        matg    = (double [][])vidvec.getQuick(1);
        matb    = (double [][])vidvec.getQuick(2);
        checkVideoBuffer(matg);
        edgevec = ObjectFactory1D.dense.make(1);
        edgevec.setQuick(0, this.matvid);
        
        sobelColor(this.matvid, matr, matg, matb);
        
        return(edgevec);
    }
    
    private void sobelColor(double [][]dest, double [][]srcr, double [][]srcg, double[][]srcb)
    {
        int    width, height;
        int    i,j;
        double dr, dg, db, d;
        
        width  = srcr[0].length;
        height = srcr.length;

        // detect edges in the main area of the image
        for(i=0; i<height-2; i++)
        {
            for(j=0; j<width-2; j++)
            {
                dr = Math.max(Math.abs(srcr[i][j+2] + 2*srcr[i+1][j+2] + srcr[i+2][j+2] - srcr[i][j] - 2*srcr[i+1][j] - srcr[i+2][j]), Math.abs(srcr[i+2][j] + 2*srcr[i+2][j+1] + srcr[i+2][j+2] - srcr[i][j] - 2*srcr[i][j+1] - srcr[i][j+2]));
                dg = Math.max(Math.abs(srcg[i][j+2] + 2*srcg[i+1][j+2] + srcg[i+2][j+2] - srcg[i][j] - 2*srcg[i+1][j] - srcg[i+2][j]), Math.abs(srcr[i+2][j] + 2*srcg[i+2][j+1] + srcg[i+2][j+2] - srcg[i][j] - 2*srcg[i][j+1] - srcg[i][j+2]));
                db = Math.max(Math.abs(srcb[i][j+2] + 2*srcb[i+1][j+2] + srcb[i+2][j+2] - srcb[i][j] - 2*srcb[i+1][j] - srcb[i+2][j]), Math.abs(srcr[i+2][j] + 2*srcb[i+2][j+1] + srcb[i+2][j+2] - srcb[i][j] - 2*srcb[i][j+1] - srcb[i][j+2]));
                d  = Math.max(Math.max(dr, dg), db) / 4;                
                dest[i][j] = d;
            }
       }
    }
        
    private ObjectMatrix1D edgeDetection(ObjectMatrix1D vidvec)
    {
        final  double[][]SOBEL_VERTICAL
         = new double[][]{ new double[]{-1, 0, 1},
                           new double[]{-2, 0, 2},
                           new double[]{-1, 0, 1} };
        final  double[][]SOBEL_HORIZONTAL
         = new double[][]{ new double[]{-1,-2,-1},
                           new double[]{ 0, 0, 0},
                           new double[]{ 1, 2, 1} };
        double     [][]mg;
        ObjectMatrix1D vecout;
        
        // Get video matrix from input
        mg     = (double [][])vidvec.getQuick(0);
        checkVideoBuffer(mg);
        
        // Apply 2 convolutions
        Convolution.convolutionChannel(mg, this.sobhor, SOBEL_HORIZONTAL);
        Convolution.convolutionChannel(mg, this.sobver, SOBEL_VERTICAL);
        
        int    i,j,w,h;
        double d;
        
        // Combine output of 2 convolutions
        double max;
        w = mg[0].length;
        h = mg.length;
        max = 0;
        for (i=0; i<h; i++)
        {
            for (j=0; j<w; j++)
            {
                d = (this.sobhor[i][j] + this.sobver[i][j])/2;
                if (d > 0.01) d = .9;
                else          d = 0.0;
                this.matvid[i][j] = d;
                
                if (d > max) max = d;
            }
        }
        
        // Prepare video buffer for output
        vecout = ObjectFactory1D.dense.make(1);
        vecout.setQuick(0, this.matvid);
        
        return(vecout);
    }
    
    private void checkVideoBuffer(double [][]mat)
    {
        // Make sure the work buffers have the right size
        if ((this.matvid == null) || 
            ((matvid != null) && ((matvid.length != mat.length) || (matvid[0].length != mat[0].length))))
        {
            int w,h;
            
            h = mat.length;
            w = mat[0].length;
            this.matvid  = new double[h][w];
            this.sobhor  = new double[h][w];
            this.sobver  = new double[h][w];
            
            this.matfilt    = new double[h][w];
            for(int i=0; i<h; i++) for(int j=0; j<w; j++) this.matfilt[i][j] = -1;
            this.matdir     = new double[h][w];
            this.matdirfilt = new double[h][w];
            for(int i=0; i<h; i++) for(int j=0; j<w; j++) this.matdirfilt[i][j] = -1;
        }
    }
    
    public Object []transform(Object obin) throws DataFlowException
    {
        if (obin instanceof ObjectMatrix1D)
        {
            ObjectMatrix1D vidvec, outvec;
            
            vidvec = (ObjectMatrix1D)obin;
            outvec = edgeDetect(vidvec);
            
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
        
        // If there's one channel, input is in grey-scale
        if (this.datamodel.getAttributeCount() == 1) this.greyscale = true;
        else                                         this.greyscale = false;
        
        // Output datamodel : 2 video channels containing amplitude and direction
        dmout = AVDataModels.makeEdgeDataModel(dmsup);
        setOutputDataModel(0, dmout);
        
        // Heuristics
        this.threshold = 0.1;
        this.alpha     = 0.8;
        this.alphadir  = 0.9;
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
         if   (port == 0) return("Edge Detected Video Output");
         else             return(null);
    }
    
    // **********************************************************\
    // *              Constructor/State Persistence             *
    // **********************************************************/
    public EdgeDetection()
    {
       super();
       name        = "Edge Detection";
       description = "The ultimate edge detection algorithm";
    }
}