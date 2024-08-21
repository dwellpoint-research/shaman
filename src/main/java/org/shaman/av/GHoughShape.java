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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Generalized Hough Shape</h2>
 * Description of a 2D shape, in the format needed
 * by the Generalized Hough Transform.
 */
public class GHoughShape 
{
    private static final int    GRADIENT_BINS       = 60;
    private static final double GRADIENT_RESOLUTION = Math.PI/GRADIENT_BINS;
    
    private List   lpoint;
    private double [][][]shape;
    private GHough ghough;
    
    // **********************************************************\
    // *                          Shape                         *
    // **********************************************************/
    public void draw(ObjectMatrix1D vidvec, int []maxpos)
    {
        double     x,y,s,o,xp,yp;
        double   []point;
        double     r,b;
        int        i,j;
        
        x = this.ghough.getXValue(maxpos[0]);
        y = this.ghough.getYValue(maxpos[1]);
        s = this.ghough.getScaleValue(maxpos[2]);
        o = this.ghough.getOrientationValue(maxpos[3]);
        System.err.println("SHAPE at X : "+x+", Y : "+y+"\n----------------"); //+", S : "+s+", O : "+o);
        
        for (i=0; i<this.shape.length; i++)
        {
            if (this.shape[i] != null)
            {
                for (j=0; j<this.shape[i].length; j++)
                {
                    point = this.shape[i][j];
                    r     = point[0];
                    b     = point[1];
                    xp    = x + (r*s*Math.cos(b+o));
                    yp    = y + (r*s*Math.sin(b+o));
                    drawPoint((int)xp, (int)yp, vidvec);
                }
            }
        }
    }
    
    private void drawPoint(int x, int y, ObjectMatrix1D vidvec)
    {
        final int pw = 3;
        final int ph = 3;
        double [][]matr, matg, matb;
        int        i,j,w,h;
        
        matr = (double [][])vidvec.getQuick(0);
        matg = (double [][])vidvec.getQuick(1);
        matb = (double [][])vidvec.getQuick(2);
        w    = matr[0].length;
        h    = matr.length;
        
        for (i=x; i<x+pw; i++)
        {
            for (j=y; j<y+ph; j++)
            {
                if ((j>0) && (i>0) && (j < h) && (i < w)) matr[j][i] = 1.0;
            }
        }
    }
    
    
    // **********************************************************\
    // *                     Import / Export                    *
    // **********************************************************/
    public void loadCircle()
    {
        int    i,s;
        List   lpoint;
        double x,y,g,r,a,astep;
        
        lpoint = new LinkedList();
        r      = 30;
        a      = 0;
        s      = 100;
        astep  = (Math.PI*2)/s;
        for (i=0; i<100; i++)
        {
            x = Math.cos(a)*r;
            y = Math.sin(a)*r;
            if (a < Math.PI) g = a;
            else             g = 2*Math.PI-a;
            lpoint.add(new double[]{x,y,g});
            a += astep;
        }
        toShape(lpoint);
    }
    
    public void toShape(List lpoint)
    {
        Iterator itpoint;
        double []point;
        double   r, b;
        int      bin;
        Integer Ibin;
        TreeMap  shapemap;
        List     binlist;
        
        this.lpoint = lpoint;
        
        // Make a map of (bin number -> List of (double[]{r, beta}) describing the shape
        shapemap = new TreeMap();
        itpoint  = lpoint.iterator();
        while(itpoint.hasNext())
        {
            point = (double [])itpoint.next();
            r     = Math.sqrt(point[0]*point[0] + point[1]*point[1]);
            
            double ac,as;
            double x,y;
            
            x  = point[0]; y = point[1];
            ac = x/r;     as = y/r;
            if (as != 0)
            {
                b = Math.abs(Math.asin(as));
                if      ( (x >= 0) && (y <= 0) ) b = (2*Math.PI) - b;
                else if ( (x <= 0) && (y >= 0) ) b = Math.PI - b;
                else if ( (x <= 0) && (y <= 0) ) b = Math.PI + b;
            }
            else
            {
                if (ac > 0) b = 0;
                else        b = Math.PI;
            }
            
            //if (point[0] != 0) b = 1.0/Math.tan(point[1]/point[0]);
            //else               b = 0;
            
            bin     = getBin(point[2]);
           Ibin     = new Integer(bin); 
            binlist = (List)shapemap.get(Ibin);
            if (binlist == null)
            {
                binlist = new LinkedList();
                shapemap.put(Ibin, binlist);
            }
            binlist.add(new double[]{r,b});
        }
        
        double [][][]shape;
        int    i,j;
        
        // Convert to shape to double array [bin][shape point][r,b]
        shape = new double[GRADIENT_BINS][][];
        for (i=0; i<GRADIENT_BINS; i++)
        {
            Ibin    = new Integer(i);
            binlist = (List)shapemap.get(Ibin);
            if (binlist != null)
            {
                j        = 0;
                shape[i] = new double[binlist.size()][];
                itpoint  = binlist.iterator();
                while(itpoint.hasNext())
                {
                    point = (double [])itpoint.next();
                    shape[i][j] = point;
                    j++;
                }
            }
        }
        this.shape = shape;
    }
    
    public double [][]getBinPoints(double g)
    {
        double [][]points;
        int        bin;
        
        bin    = getBin(g);
        points = this.shape[bin];
        
        return(points);
    }
    
    public int getBin(double g)
    {
        int bin;
        
        if (g<0) System.err.println(g);
        
        bin = (int)(g/GRADIENT_RESOLUTION);
        if (bin >= GRADIENT_BINS) bin = 0;
        
        return(bin);
    }
    
    // **********************************************************\
    // *               Initialization / Cleanup                 *
    // **********************************************************/
    public void setGHough(GHough ghough) { this.ghough = ghough; }
    
    public String toString()
    {
        int      i,j;
        double [][]points;
        double []point;
        StringBuffer sb;
        
        sb = new StringBuffer("GHoughShape\n");
        for (i=0; i<GRADIENT_BINS; i++)
        {
            if (this.shape[i] != null)
            {
                sb.append(i*GRADIENT_RESOLUTION+" = [");
                points = this.shape[i];
                for (j=0; j<points.length; j++)
                {
                    point = points[j];
                    sb.append(point[0]+", "+point[1]+"|");
                }
                sb.append("]\n");
            }
        }
        
        return(sb.toString());
    }
    
    public GHoughShape()
    {
    }
}
