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

import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Generalized Hough Transform</h2>
 */
public class GHough extends Transformation 
{  
    protected static final int DIM_X = 100;
    protected static final int DIM_Y = 100;
    protected static final int DIM_S = 20;
    protected static final int DIM_O = 20;
    
    protected static final double S_BEGIN = 1.0;
    protected static final double S_END   = 1.0;
    protected static final double O_BEGIN = 0;
    protected static final double O_END   = Math.PI;
    
    private GHoughShape shape;
    
    // **** Run-time Data ***
    private int         w,h;
    private double    []sval;
    private double    []oval;
    private int       []xpos;
    private int       []ypos;
    private int [][][][]acc;
    private int         maxacc;
    private int       []maxpos;
    
    // **********************************************************\
    // *                     Shape Detection                    *
    // **********************************************************/
    private ObjectMatrix1D detectShape(ObjectMatrix1D vidvec, ObjectMatrix1D edgevec) throws TransformationException
    {
        ObjectMatrix1D vidout;
        double     [][]matamp, mator;
        int            i,j,k,l,w,h;
        
        // Get amplitude and direction matrices from input
        matamp = (double [][])edgevec.getQuick(0);
        mator  = (double [][])edgevec.getQuick(1);
        w      = matamp[0].length;
        h      = matamp.length;
        
        // Initialize detection if not there or wrong dimensions.
        checkDetectionBuffers(w,h);
        
        // Clear detection accumulator
        for (i=0; i<DIM_X; i++)
            for (j=0; j<DIM_Y; j++)
                for (k=0; k<DIM_S; k++)
                    for (l=0; l<DIM_O; l++) this.acc[i][j][k][l] = 0;
        this.maxacc = 0;
        this.maxpos = new int[4];
        
        // When a pixel is over the threshold, add it's contribute to the shape detection
        int cnt = 0;
        for (i=0; i<h; i++)
        {
            for (j=0; j<w; j++)
            {
                if (matamp[i][j] > 0.8)
                {
                    cnt++;
                    contributePixel2D(j, i, mator[i][j]);
                }
            }
        }
        
        // Draw shape onto video signal
        //System.err.println("cnt : "+cnt+" cntp "+cntp+" acc : "+maxacc);
        this.shape.draw(vidvec, this.maxpos);
        drawTransformed(vidvec);
        
        // Output video signal with detected shape
        vidout = vidvec;
        
        return(vidout);
    }
    
    private void drawTransformed(ObjectMatrix1D vidvec)
    {
        double [][]matr, matg, matb;
        int        x,y;
        double     macc, val;
        
        matr = (double [][])vidvec.getQuick(0);
        matg = (double [][])vidvec.getQuick(1);
        matb = (double [][])vidvec.getQuick(2);
        
        macc = 0;
        for (y=0; y<DIM_Y; y++)
            for (x=0; x<DIM_X; x++)
                if (this.acc[x][y][0][0] > macc) macc = this.acc[x][y][0][0];
        
        for (y=0; y<DIM_Y; y++)
            for (x=0; x<DIM_X; x++)
            {
                val = this.acc[x][y][0][0]/macc;
                matr[y][x] = val;
                matg[y][x] = val;
                matb[y][x] = val;
            }
        
    }
    
    private void contributePixel2D(int x, int y, double t)
    {
        int    i;
        double [][]points;
        double r,b;
        double xr,yr;
        int    xb,yb;
        
        points = this.shape.getBinPoints(t);
        if (points != null)
        {
            for (i=0; i<points.length; i++)
            {
               r = points[i][0];
               b = points[i][1];
            
               xr = x+r*Math.cos(b);
               yr = y+r*Math.sin(b);
               if (xr < 0) xr = 0; if (xr >= this.w) xr = w-1;
               if (yr < 0) yr = 0; if (yr >= this.h) yr = h-1;
               xb = this.xpos[(int)xr];
               yb = this.ypos[(int)yr];
                    
               this.acc[xb][yb][0][0]++;
               if (this.acc[xb][yb][0][0] > this.maxacc)
               {
                   this.maxpos = new int[]{xb,yb,0,0};
                   this.maxacc = this.acc[xb][yb][0][0];
               }
            }
        }
    }
    
    private void contributePixel4D(int x, int y, double t)
    {
        int    i,j,k;
        double [][]points;
        double r,b,s,o;
        double xr,yr;
        int    xb,yb;
        
        points = this.shape.getBinPoints(t);
        for (i=0; i<points.length; i++)
        {
            r = points[i][0];
            b = points[i][1];
            for (j=0; j<DIM_S; j++)
            {
                s = this.sval[j];
                for (k=0; k<DIM_O; k++)
                {
                    o  = this.oval[k];
                    xr = x+s*r*Math.cos(b+o);
                    yr = y+s*r*Math.sin(b+o);
                    if (xr < 0) xr = 0; if (xr >= this.w) xr = w-1;
                    if (yr < 0) yr = 0; if (yr >= this.h) yr = h-1;
                    xb = this.xpos[(int)xr];
                    yb = this.ypos[(int)yr];
                    
                    this.acc[xb][yb][j][k]++;
                    if (this.acc[xb][yb][j][k] > this.maxacc)
                    {
                        this.maxpos = new int[]{xb,yb,j,k};
                        this.maxacc = this.acc[xb][yb][j][k];
                    }
                }
            }
        }
    }
    
    private void checkDetectionBuffers(int w, int h)
    {
        if (this.acc == null) initDetection(w,h);
        else
        {
            if ((w != this.w) || (h != this.h)) initDetection(w,h);
        }
    }
    
    private void initDetection(int w, int h)
    {
        int i;
        
        this.w  = w;
        this.h  = h;
        
        // Initialize shape detection table to 0
        this.acc = new int[DIM_X][DIM_Y][DIM_S][DIM_O];
        
        double []val;
        double vstep;
        
        // Make lookup tables for all scale and orientation values
        vstep = (S_END-S_BEGIN)/DIM_S;
        val   = new double[DIM_S];
        for(i=0; i<DIM_S; i++) val[i] = S_BEGIN+(i*vstep);
        this.sval = val;
        
        vstep = (O_END-O_BEGIN)/DIM_O;
        val   = new double[DIM_O];
        for(i=0; i<DIM_O; i++) val[i] = O_BEGIN+(i*vstep);
        this.oval = val;
        
        // And a lookup of x or y position -> x or y bin
        int  []poslook;
        double step;
        
        poslook = new int[w];
        step    = w/(double)DIM_X;
        for (i=0; i<w; i++)
        {
            poslook[i] = (int)(i/step);
        }
        this.xpos = poslook;
        
        poslook = new int[h];
        step    = h/(double)DIM_Y;
        for (i=0; i<h; i++)
        {
            poslook[i] = (int)(i/step);
        }
        this.ypos = poslook;
    }
    
    public Object []transform(Object []obin) throws DataFlowException
    {
        ObjectMatrix1D vidvec, edgevec, outvec;
        
        vidvec  = (ObjectMatrix1D)obin[0];
        edgevec = (ObjectMatrix1D)obin[1];
        outvec  = detectShape(vidvec, edgevec);
            
        return(new Object[]{outvec});
    }
    
    public int getXValue(int pos)
    {
        double step;
        int    x;
        
        step = ((double)this.w)/DIM_X;
        x    = (int)((step*pos)+(step/2.0));
        
        return(x);
    }
    
    public int getYValue(int pos)
    {
        double step;
        int    y;
        
        step = ((double)this.h)/DIM_Y;
        y    = (int)((step*pos)+(step/2.0));
        
        return(y);
    }
    
    public double getScaleValue(int pos)
    {
        double step, scale;
        
        step  = ((double)S_END-S_BEGIN)/DIM_S;
        scale = S_BEGIN + pos*step;
        
        return(scale);
    }
    
    public double getOrientationValue(int pos)
    {
        double step, ori;
        
        step = ((double)O_END-O_BEGIN)/DIM_O;
        ori  = O_BEGIN + pos*step;
        
        return(ori);
    }
    
    // **********************************************************\
    // *                 Parameter Configuration                *
    // **********************************************************/
    
    // **********************************************************\
    // *                Transformation Implementation           *
    // **********************************************************/
    public void init() throws ConfigException
    {
        DataModel  dmsup;
     
        // Make sure the inputs are compatible with this transformation's data requirements
        dmsup = getSupplierDataModel(0);
        checkDataModelFit(0, dmsup);
        setOutputDataModel(0, dmsup);
        dmsup = getSupplierDataModel(1);
        checkDataModelFit(1, dmsup);
        
        this.shape = new GHoughShape();
        this.shape.loadCircle();
        this.shape.setGHough(this);
        System.err.println(this.shape);
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    public void checkDataModelFit(int port, DataModel dmin) throws DataModelException
    {
        if (!dmin.hasProperty(DataModelPropertyVideo.PROPERTY_VIDEO))
            throw new DataModelException("Cannot find Video property in input datamodel.");
        
        if      (port == 0)
        {
            if (dmin.getAttributeCount() != 3)
               throw new DataModelException("Cannot find color video input datamodel at port 0.");
        }
        else if (port == 1)
        {
            if (dmin.getAttributeCount() != 2)
               throw new DataModelException("Cannot find 2 channel (amplitude, direction) video datamodel at port 1.");
        }
    }
    
    public int getNumberOfInputs()  { return(2); }
    public int getNumberOfOutputs() { return(1); }
    public String getInputName(int port)
    {
         if      (port == 0) return("Video Input");
         else if (port == 1) return("Edge Detected Input");
         else                return(null);
    }
    public String getOutputName(int port)
    {
         if   (port == 0) return("Video Output");
         else             return(null);
    }
     
    
    // **********************************************************\
    // *              Constructor/State Persistence             *
    // **********************************************************/
    public GHough()
    {
       super();
       name        = "GHough";
       description = "Generalized Hough Transform";
    }
}