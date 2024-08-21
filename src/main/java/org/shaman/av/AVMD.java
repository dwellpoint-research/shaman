/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                    Audio / Video                      *
 *                                                       *
 *  December 2004                                        *
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
import org.shaman.exceptions.LearnerException;

import cern.colt.matrix.ObjectFactory1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Adaptive Video Motion Detection</h2>
 * Motion Detection for Physical Security applications
 * TODO: Review learning
 *       Convert into real classifier
 */
public class AVMD extends Transformation
{   
    // Display Modes
    public static final int DISPLAY_LIVE        = 0;
    public static final int DISPLAY_FINDVIEW    = 1;
    public static final int DISPLAY_WATCH       = 2;
    public static final int DISPLAY_TRAIN       = 3;
    public static final int DISPLAY_MONITOR     = 4;
    public static final int DISPLAY_GRID_WEIGHT = 10;
    public static final int DISPLAY_GRID_MOTION = 11;
    public static final int DISPLAY_MOTION      = 20;
    
    // Data Processing Modes
    public static final int MODE_WATCH   = 1;      // View the world, adjust camera settings
    public static final int MODE_TRAIN   = 10;     // Train on the incoming video
    public static final int MODE_MONITOR = 100;    // Apply trained model on incoming video
    
    // AVMD Algorithm parameters
    private int    gridSaturate      = 100;          // Number of motion occurences to disregard the gridblock completely
    private double motionThreshold   = 40 / 256.0;   // Threshold of motion in 1 gridblock to have a motion occurence
    private int    motionSensitivity = 50;           // Sensitivity to motion during monitoring
    private int    motionDirection   = 50;           // Sensitivity to motion direction during monitoring
    private int    gridX             = 40;           // # of horizontal subdivisions
    private int    gridY             = 40;           // # of vertical subdivisions
    
    // Motion Vector Detection constants
    private static final int [][]MV_NEIPOS = { {1,0}, {1,1}, {0,1}, {-1,1}, {-1,0}, {-1,-1}, {0,-1}, {1,-1} };
     
    // Picture Buffers
    private static final  int NUMPICS = 3;           // Number of pictures to base motion detection on
    private int              picPos  = 0;            // Position in picture buffer
    private double     [][][]pics;                   // The last NUMPICS pictures of the video feed
    private double       [][]picMotion;              // The motion in the last picture
    private ObjectMatrix1D   picReference;           // A reference picture used to calibrate view position with
    private ObjectMatrix1D   picOut;                 // The output picture
    private double     [][]outr;                     //    red   channel of the output
    private double     [][]outg;                     //    green
    private double     [][]outb;                     //    blue
    
    
    // Parameter and Motion Grids 
    private double [][]grwei;                        // [i][j] = Motion value (0-1) in gridpoint (i,j)
    private double [][]grnow;                        // [i][j] = Current motion-level (0-1)
    private double [][]grmask;                       // [i][j] = 1 when girdpoint is hypersensitive, -1 when insensitive
    private double [][]grall;                        // [i][j] = Motion and Motion vector value in gridpoint (i,j)
    
    private double [][][]grmotion;                   // Motion Vector calculation grid
    private double [][]grmotCount;
    private double [][]grmotAct;
    private double [][][]grmotVec;

    // Brightness history
    private double []brhis;
    
    // Processing and Display modes
    private int  mode    = MODE_WATCH;
    private int  display = DISPLAY_WATCH;
    
    // Remember current picture as reference
    private boolean storeAsView = false;
    
    // Video Image
    private DataModelPropertyVideo propVid;
    private int                    width, height;
    private int                    gsx, gsy;
    
    // Output Values
    private double brightnessChange;
    private double motion;
    
    // **********************************************************\
    // *        Parameter Definition and Configuration          *
    // **********************************************************/
    public void setDisplay(int m)     { this.display = m; }
    public void setProcessMode(int m) { this.mode = m; }
    
    public void setGridX(int gridX)   { this.gridX = gridX; }
    public void setGridY(int gridY)   { this.gridY = gridY; }
    public void setMotionThreshold(int mt)   { this.motionThreshold = mt; }
    public void setMotionSensitivity(int ms) { this.motionSensitivity = ms; }
    public void setMotionDirection(int md)   { this.motionDirection = md; }
    
    public void fixView()
    {
        this.storeAsView = true;
    }
    
    // **********************************************************\
    // *                  Output Value Access                   *
    // **********************************************************/
    public double getMotionLevel()      { return(this.motion); }
    public double getBrightnessChange() { return(this.brightnessChange); }
    
    // **********************************************************\
    // *     Parameter Grid Configuration and Manipulation      *
    // **********************************************************/
    public void clearWeights()
    {
        int i,j;
        
        // Clear Motion Weights
        for (i=0; i<gridX; i++) for (j=0; j<gridY; j++) this.grall[i][j] = 0;
    }
    
    public void clearMask()
    {
        int i,j;
        
        // Clear Sensitivity Mask
        for (i=0; i<gridX; i++) for (j=0; j<gridY; j++) this.grmask[i][j] = 0;
    }
    
    public void clearMotion()
    {
        int i,j;
        
        // Clear Motion Vectors
        for (i=0; i<gridX; i++) for (j=0; j<gridY; j++)
        { 
            this.grmotion[i][j][0] = 0;
            this.grmotion[i][j][1] = 0;
            this.grmotAct[i][j]    = 0;
            this.grmotVec[i][j][0] = 0;
            this.grmotVec[i][j][1] = 0;
            this.grmotCount[i][j]  = 0;
        }
    }
    
    // Draw Masking regions
    // --------------------
    public void addMask(int x1, int y1, int x2, int y2, boolean left)
    {
        double pw, ph, gx, gy;
        int    i,j,i1,j1,i2,j2,ibuf,jbuf;
        
        // Add or clear sensitivity mask rectangle
        pw  = this.width;
        ph  = this.height;
        gx  = pw / gridX;
        gy  = ph / gridY;
        i1  = (int)(x1 / gx); if (i1 < 0) i1 = 0; if (i1 >= gridX) i1 = gridX-1;
        j1  = (int)(y1 / gy); if (j1 < 0) j1 = 0; if (j1 >= gridY) j1 = gridY-1;
        i2  = (int)(x2 / gx); if (x2 < 0) i2 = 0; if (i2 >= gridX) i2 = gridX-1;
        j2  = (int)(y2 / gy); if (y2 < 0) j2 = 0; if (j2 >= gridY) j2 = gridY-1;
        
        if (i1 > i2) { ibuf = i1; i1 = i2; i2 = ibuf; }
        if (j1 > j2) { jbuf = j1; j1 = j2; j2 = jbuf; }
        
        for (i=i1; i<=i2; i++)
            for (j=j1; j<=j2; j++)
            {
                if (left) this.grmask[i][j] = 1;
                else      this.grmask[i][j] = -1;
            }
    }
    
    // Manual Adjusting of the weighting grid
    // --------------------------------------
    public void adjustWeight(int px, int py, boolean left)
    {
        double pw, ph, gx, gy;
        double weistep;
        int    i,j;
        
        pw  = this.width;
        ph  = this.height;
        gx  = pw / gridX;
        gy  = ph / gridY;
        
        weistep      = this.gridSaturate / 8;
        i            = (int)(px / gx);
        j            = (int)(py / gy);
        if (this.grmask[i][j] != 0) this.grmask[i][j] = 0;
        else
        {
            if (left)
            {
                if (this.grall[i][j] >= this.gridSaturate-weistep) this.grall[i][j]  = gridSaturate;
                else                                               this.grall[i][j] += weistep;
            }
            else
            {
                if (this.grall[i][j] <= weistep) this.grall[i][j]  = 0;
                else                             this.grall[i][j] -= weistep;
            }
        }
        
    }
    
    // Initialize Picture Buffers
    // --------------------------
    void changeGridSize(int grid)
    {
        int gridX, gridY;
        int i,j;
        
        gridX = grid; gridY = grid;
        
        // Allocate weighting buffers of correct size
        grnow    = new double[gridX][gridY];
        grall    = new double[gridX][gridY];
        grwei    = new double[gridX][gridY];
        grmask   = new double[gridX][gridY];
        grmotion   = new double[gridX][gridY][2];
        grmotCount = new double[gridX][gridY];
        grmotAct   = new double[gridX][gridY];
        grmotVec   = new double[gridX][gridY][2];
        for (i=0; i<gridX; i++) for (j=0; j<gridY; j++) { grall[i][j] = 0; grwei[i][j] = 0; grmask[i][j] = 0; grmotion[i][j][0] = 0; grmotion[i][j][1] = 0; grmotCount[i][j] = 0; grmotVec[i][j][0] = 0; grmotVec[i][j][1] = 0; grmotAct[i][j] = 0; }
    
        this.gridX = gridX;
        this.gridY = gridY;
        this.gsx   = this.width  / gridX;
        this.gsy   = this.height / gridY;
    }
    
    // **********************************************************\
    // *           Adaptive Video Motion Detection              *
    // **********************************************************/
    private ObjectMatrix1D avmd(ObjectMatrix1D vidvec) throws LearnerException
    {
        int     i,j;
        
        // Check if this image has to be stored as the reference view
        if (this.storeAsView)
        {
            this.picReference = copyPicture(vidvec);
            storeAsView = false;
        }
        
        // Update picture and brightness buffers
        updatePictures(vidvec);
        
        // Calculate current motion (grid) values
        calculateMotion();
        
        // Contribute current motion grid when learning
        if ((this.mode == MODE_TRAIN) && (this.picPos >= NUMPICS))
        {
            for (i=0; i<this.gridX; i++)
            {
                for (j=0; j<this.gridY; j++)
                {
                    if ((this.grnow[i][j] > this.motionThreshold) && 
                        (this.grall[i][j] < this.gridSaturate)) this.grall[i][j]++;
                }
            }
        }
        
        // Calculate Motion Vectors
        double []mv;
        int      nummv;
        double   mvdot;
        
        mv    = calculateMotionVectors();
        nummv = (int)mv[0];
        mvdot = mv[1];
        
        // Calculate Weighted Motion Grid
        if ( picPos >= NUMPICS )
        {
            double grmax;
            
            grmax = 0;
            for (i=0; i<this.gridX; i++)
                for (j=0; j<this.gridY; j++) 
                    if (this.grall[i][j] > grmax) grmax = this.grall[i][j];
            if (grmax > this.gridSaturate) grmax = this.gridSaturate;
            
            if (grmax != 0)
            {
                for (i=0; i<this.gridX; i++)
                    for (j=0; j<this.gridY; j++) this.grwei[i][j] = this.grall[i][j] / grmax;
                for (i=0; i<this.gridX; i++)
                    for (j=0; j<this.gridY; j++)
                        if ( this.grwei[i][j] > 1 ) this.grwei[i][j] = 1;
            }
            else
            {
                for (i=0; i<this.gridX; i++)
                    for (j=0; j<this.gridY; j++) this.grwei[i][j] = 0;
            }
        }
        
        // Calculate Motion taking into account current weights
        double mtemp, wsum;
        int    numMasked, numGridBlocks;
        
        // Count number of masked out blocks
        numMasked = 0;
        for (i=0; i<this.gridX; i++)
            for (j=0; j<this.gridY; j++) 
                if (this.grmask[i][j] == -1) numMasked++;
            
        // Calculate overall motion taking into account the mask
        mtemp = 0; wsum = 0;
        for (i=0; i<this.gridX; i++)
        {
            for (j=0; j<this.gridY; j++)
            {
                if ((this.grmask[i][j] != -1) && 
                    ((this.grnow[i][j] * (1-this.grwei[i][j])) > this.motionThreshold))
                {
                    wsum++;
                }
            }
        }
        numGridBlocks = (gridX*gridY) - numMasked;
        if (numGridBlocks == 0) numGridBlocks = 1;
        wsum /= numGridBlocks;
        mtemp = motionSensitivity / (100.0 * 10);
        wsum /= mtemp;
        if (wsum > 1) wsum = 1;
        
        // Check for movement in any hypermasked points
        for (i=0; i<this.gridX; i++)
            for (j=0; j<this.gridY; j++)
            {
                if ((this.grmask[i][j] == 1) && 
                    ((this.grnow[i][j] * (1-this.grwei[i][j])) > this.motionThreshold)) wsum = 1.0;
            }
        
        // Merge the result with the motion vector result
        double mdmul = (motionDirection / 100.0) * 2.0;
        if ((nummv > 0) && (mvdot > 0))
        {
            wsum += wsum*mvdot*mdmul;
            if (wsum > 1.0) wsum = 1;
        }
        this.motion = wsum;
        //System.out.println("Motion = "+wsum);
        
        // Display the video feed in the correct mode
        // ------------------------------------------
        if      (this.display == DISPLAY_LIVE)        displayLive(vidvec);
        else if (this.display == DISPLAY_MOTION)      displayMotion(vidvec);
        else if (this.display == DISPLAY_TRAIN)       displayTrain(vidvec);
        else if (this.display == DISPLAY_MONITOR)     displayMonitor(vidvec);
        else if (this.display == DISPLAY_WATCH)       displayWatch(vidvec);
        else if (this.display == DISPLAY_FINDVIEW)    displayFindView(vidvec);
        else if (this.display == DISPLAY_GRID_MOTION) displayGridMotion(vidvec);
        else if (this.display == DISPLAY_GRID_WEIGHT) displayGridWeight(vidvec);
        
        return(this.picOut);
    }
    
    private void updatePictures(ObjectMatrix1D vidvec)
    {
        int        i,j,k;
        double     avg, br, mb;
        double [][]matr, matg, matb;
        double     r,g,b;
        
        // Convert to greyscale, measuse brightness and add last frame to motion history
        // -----------------------------------------------------------------------------
        matr = (double [][])vidvec.getQuick(0);
        matg = (double [][])vidvec.getQuick(1);
        matb = (double [][])vidvec.getQuick(2);
        if (this.picPos < NUMPICS)
        {
            // Remember picture
            br   = 0;
            for (i=0; i<this.height; i++)
            {
                for (j=0; j<this.width; j++)
                {
                    r    = matr[i][j];
                    g    = matg[i][j];
                    b    = matb[i][j];
                    avg  = (r+g+b) / 3.0;
                    pics[this.picPos][i][j] = avg;
                    br  += avg;
                }
            }
            br  /= (this.width*this.height);
            this.brhis[this.picPos] = br;
            this.picPos++;
        }
        else
        {
            // Shift old pictures forward
            for (i=0; i<this.height; i++)
            {
                for (j=0; j<this.width; j++)
                {
                    for (k=0; k<NUMPICS-1; k++) pics[k+1][i][j] = pics[k][i][j];
                }
            }
            
            // Put new picture in picture buffer
            br   = 0;
            for (i=0; i<this.height; i++)
            {
                avg = 0;
                for (j=0; j<this.width; j++)
                {
                    r    = matr[i][j];
                    g    = matg[i][j];
                    b    = matb[i][j];
                    avg  = (r+g+b) / 3.0;
                    pics[0][i][j] = avg;
                    br  += avg;
                }
            }
            br /= (this.width*this.height);
            
            // Update brightness history and brightness
            for (i=0; i<NUMPICS-1; i++) this.brhis[i] = this.brhis[i+1];
            this.brhis[NUMPICS-1] = br;
            
            mb = 0;
            for (i=0; i<NUMPICS; i++) mb += this.brhis[i];
            mb /= NUMPICS;
            
            // Environment Parameter = Brightness change over history
            br = Math.abs(br-mb)*3;
            this.brightnessChange = br;
        }
    }
    
    private void calculateMotion()
    {
        // Measure motion in last frames
        int    i,j,k;
        double var;
        double avg;
        
        for (i=0; i<this.height; i++)
        {
            for (j=0; j<this.width; j++)
            {
                avg = 0;
                for (k=0; k<NUMPICS; k++) avg += this.pics[k][i][j];
                avg /= NUMPICS;
                
                var = 0;
                for (k=0; k<NUMPICS; k++) var += (this.pics[k][i][j] - avg)*(this.pics[k][i][j] - avg);
                var /= NUMPICS;
                
                var *= 40;
                
                if      ( var > 1.0)        this.picMotion[i][j] = 1.0;
                else if ( var < 30.0/256.0) this.picMotion[i][j] = 0;
                else                        this.picMotion[i][j] = var;       
            }
        }
        
        // Calculate motion per gridblock
        double gsx, gsy;
        int    px, py, pxe, pye;
        int    l;
        int    ct;
        double pw, ph;
        
        pw  = this.width;
        ph  = this.height;
        gsx = pw / gridX;
        gsy = ph / gridY;
        
        for (i=0; i<this.gridX; i++)
        {
            for (j=0; j<this.gridY; j++)
            {
                avg = 0;
                ct  = 0;
                px  = (int)( i   *gsx); py  = (int)( j   *gsy);
                pxe = (int)((i+1)*gsx); pye = (int)((j+1)*gsy);
                if (pxe > pw) pxe = (int)pw;
                if (pye > ph) pye = (int)ph;
                for (k=px; k<pxe; k++)
                {
                    for (l=py; l<pye; l++)
                    {
                        avg += this.picMotion[l][k];
                        ct++;
                    }
                }
                avg /= ct;
                this.grnow[i][j] = avg;
            }
        }
    }
    
    private double []calculateMotionVectors()
    {
        int    i,j,k;
        double neiwei, mvSum0, mvSum1;
        int    neicount;
        
        // Do Motion Vector Training if necessary
        // --------------------------------------
        if ((this.mode == MODE_TRAIN) && (this.picPos >= NUMPICS))
        {
            for (i=0; i<gridX; i++)
            {
                for (j=0; j<gridY; j++)
                {
                    if ((grmotAct[i][j] > motionThreshold) && 
                            (grnow[i][j] < motionThreshold)    && 
                            (i>0)       && (j>0) && 
                            (i<gridX-1) && (j<gridY-1))
                    {
                        mvSum0 = 0; mvSum1 = 0;
                        neicount = 0;
                        for (k=0; k<8; k++)
                        {
                            if (grnow[i+MV_NEIPOS[k][0]][j + MV_NEIPOS[k][1]] > this.motionThreshold)
                            { 
                                neiwei = 1; neicount++;
                            }
                            else
                            {
                                neiwei = 0;
                            }
                            mvSum0 += (MV_NEIPOS[k][0]) * neiwei;
                            mvSum1 += (MV_NEIPOS[k][1]) * neiwei;
                        }
                        if (neicount != 0)
                        {
                            mvSum0 /= neicount;
                            mvSum1 /= neicount;
                            this.grmotion[i][j][0] += mvSum0;
                            this.grmotion[i][j][1] += mvSum1;
                            this.grmotCount[i][j]++;
                        }
                    }
                    this.grmotAct[i][j] = (this.grnow[i][j]+this.grmotAct[i][j])/2;
                }
            }
        }
        
        // Calculate Motion Vector Quality of the current motion
        // -----------------------------------------------------
        double mvdot;
        int    nummv;
        nummv = 0; mvdot = 0;
        
        if (this.picPos >= NUMPICS)
        {
            double mvGrid0, mvGrid1;
            double mvS, mvdotnow;
            
            for (i=0; i<gridX; i++)
            {
                for (j=0; j<gridY; j++)
                {
                    if ((this.grmask[i][j] == 0) && (this.grmotAct[i][j] > this.motionThreshold) &&
                            (this.grnow[i][j] < this.motionThreshold) &&
                            (i>0) && (j>0) && (i<this.gridX-1) && (j<this.gridY-1))
                    {
                        mvSum0 = 0; mvSum1 = 0;
                        neicount = 0;
                        for (k=0; k<8; k++)
                        {
                            if (grnow[i+MV_NEIPOS[k][0]][j+MV_NEIPOS[k][1]] > motionThreshold)
                            { 
                                neiwei = 1;
                                neicount++;
                            }
                            else neiwei = 0;
                            mvSum0 += (MV_NEIPOS[k][0]) * neiwei;
                            mvSum1 += (MV_NEIPOS[k][1]) * neiwei;
                        }
                        if (neicount != 0)
                        {
                            mvSum0 /= neicount;
                            mvSum1 /= neicount;
                            
                            if (this.grmotCount[i][j] > 0)
                            {
                                // Measure dot product between the average motion vector and the current one
                                mvGrid0 = this.grmotion[i][j][0] / this.grmotCount[i][j];
                                mvGrid1 = this.grmotion[i][j][1] / this.grmotCount[i][j];
                                
                                mvS = Math.sqrt((mvSum0*mvSum0) + (mvSum1*mvSum1));
                                mvSum0 /= mvS; mvSum1 /= mvS;
                                mvS = Math.sqrt((mvGrid0*mvGrid0) + (mvGrid1*mvGrid1));
                                mvGrid0 /= mvS; mvGrid1 /= mvS;
                                
                                mvdotnow = (mvGrid0*mvSum0) + (mvGrid1*mvSum1);
                                if (!Double.isNaN(mvdotnow))
                                { 
                                    nummv++;
                                    mvdot += mvdotnow;
                                }
                            }
                        }
                    }
                    this.grmotAct[i][j] = (this.grnow[i][j]+this.grmotAct[i][j]) / 2;
                }
            }
            if (Double.isNaN(mvdot)) nummv = 0;
            if (nummv > 0)
            {
                mvdot /= nummv;
                
                if (mvdot < 0)
                {
                    mvdot = -mvdot;
                }
                else mvdot = 0;
            }
        }
        
        return(new double[]{nummv, mvdot});
    }
    
    private ObjectMatrix1D copyPicture(ObjectMatrix1D vidvec)
    {
        int            i,j,k;
        ObjectMatrix1D vidcop;
        double     [][]channel;
        double     [][]chcop;
        
        // Copy the given video vector
        vidcop = ObjectFactory1D.dense.make(vidvec.size());
        for(i=0; i<vidcop.size(); i++)
        {
            channel = (double [][])vidcop.getQuick(i);
            chcop   = new double[channel.length][channel[0].length];
            for(j=0; j<channel.length; j++)
                for (k=0; k<channel[0].length; k++) chcop[j][k] = channel[j][k];
            vidcop.setQuick(i, chcop);
        }
        
        return(vidcop);
    }
    
    // **********************************************************\
    // *                  AVMD Output Video                     *
    // **********************************************************/
    private void displayLive(ObjectMatrix1D vidvec)
    {
        int    i,j;
        double [][]or, og, ob;
        double [][]matr, matg, matb;
        
        or   = this.outr;
        og   = this.outg;
        ob   = this.outb;
        matr = (double [][])vidvec.getQuick(0);
        matg = (double [][])vidvec.getQuick(1);
        matb = (double [][])vidvec.getQuick(2);
        
        // Just return input.
        for (i=0; i<this.height; i++)
        {
            for (j=0; j<this.width; j++)
            {
                or[i][j] = matr[i][j];
                og[i][j] = matg[i][j];
                ob[i][j] = matb[i][j];
            }
        }
    }
    
    private void displayMotion(ObjectMatrix1D vidvec)
    {
        int    i,j;
        double [][]or, og, ob;
        
        or   = this.outr;
        og   = this.outg;
        ob   = this.outb;
        
        // Display current motion picture in greyscale
        for (i=0; i<this.height; i++)
        {
            for (j=0; j<this.width; j++)
            {
                or[i][j] = this.picMotion[i][j];
                og[i][j] = this.picMotion[i][j];
                ob[i][j] = this.picMotion[i][j];
            }
        }
    }
    
    private void displayWatch(ObjectMatrix1D vidvec)
    {
        int        i,j,k,l;
        int        px,py,pxe,pye;
        double [][]matr, matg, matb;
        double     r,g,b,ro,go,bo;
        double [][]or, og, ob;
        int        pw, ph;
        double     grwei, wei;
        
        pw   = this.width;
        ph   = this.height;
        or   = this.outr;
        og   = this.outg;
        ob   = this.outb;
        matr = (double [][])vidvec.getQuick(0);
        matg = (double [][])vidvec.getQuick(1);
        matb = (double [][])vidvec.getQuick(2);
        
        // Show motion in grid (yellow) superimposed over live video feed
        for (i=0; i<this.gridX; i++)
        {
            for (j=0; j<this.gridY; j++)
            {
                grwei = this.grnow[i][j];
                px    = (int)( i   *this.gsx); py  = (int)( j   *this.gsy);
                pxe   = (int)((i+1)*this.gsx); pye = (int)((j+1)*this.gsy);
                if (pxe > pw) pxe = (int)pw;
                if (pye > ph) pye = (int)ph;
                
                for (k=px; k<pxe; k++)
                {
                    for (l=py; l<pye; l++)
                    {
                        r    = matr[l][k];
                        g    = matg[l][k];
                        b    = matb[l][k];
                        if ((k != px) && (l != py))
                        {
                            if ((this.grnow[i][j] > this.motionThreshold) && (this.grmask[i][j] == 0))
                            {
                                ro  = grwei + (1 - grwei) * r;
                                go  = grwei + (1 - grwei) * g;
                                bo  =         (1 - grwei) * b;
                            }
                            else { ro = r; go = g; bo = b; }
                            
                            wei = 0.5;
                            if      (this.grmask[i][j] == -1)
                            {
                                ro  =       (1-wei) * r;
                                go  =       (1-wei) * g;
                                bo  = wei + (1-wei) * b;
                            }
                            else if (this.grmask[i][j] == 1)
                            {
                                ro  = wei + (1-wei) * r;
                                go  =       (1-wei) * g;
                                bo  = wei + (1-wei) * b;
                            }
                        }
                        else
                        {
                            ro = r-(20/256.0); if (ro < 0) ro = 0;
                            go = g-(20/256.0); if (go < 0) go = 0;
                            bo = b-(20/256.0); if (bo < 0) bo = 0;
                        }
                        or[l][k] = ro;
                        og[l][k] = go;
                        ob[l][k] = bo;
                    }
                }
            }
        }
    }
    
    private void displayTrain(ObjectMatrix1D vidvec)
    {
        int        i,j,k,l;
        int        px,py,pxe,pye;
        double [][]matr, matg, matb;
        double     r,g,b,ro,go,bo;
        double [][]or, og, ob;
        int        pw, ph;
        double     wei;
        
        pw   = this.width;
        ph   = this.height;
        or   = this.outr;
        og   = this.outg;
        ob   = this.outb;
        matr = (double [][])vidvec.getQuick(0);
        matg = (double [][])vidvec.getQuick(1);
        matb = (double [][])vidvec.getQuick(2);
        
        // Show video feed overlayed with Callibration weights
        for (i=0; i<this.gridX; i++)
        {
            for (j=0; j<this.gridY; j++)
            {
                wei   = this.grwei[i][j];
                px    = (int)( i   *gsx); py  = (int)( j   *gsy);
                pxe   = (int)((i+1)*gsx); pye = (int)((j+1)*gsy);
                if (pxe > pw) pxe = (int)pw;
                if (pye > ph) pye = (int)ph;
                for (k=px; k<pxe; k++)
                {
                    for (l=py; l<pye; l++)
                    {
                        r    = matr[l][k];
                        g    = matg[l][k];
                        b    = matb[l][k];
                        if (this.grmask[i][j] == 0)
                        {
                            ro  =       (1-wei) * r;
                            go  = wei + (1-wei) * g;
                            bo  =       (1-wei) * b;
                        }
                        else
                        {
                            wei = 0.5;
                            if (this.grmask[i][j] == -1)
                            {
                                ro  =       (1-wei) * r;
                                go  =       (1-wei) * g;
                                bo  = wei + (1-wei) * b;
                            }
                            else
                            {
                                ro  = wei  + (1-wei) * r;
                                go  =        (1-wei) * g;
                                bo  = wei  + (1-wei) * b;
                            }
                        }
                        or[l][k] = ro;
                        og[l][k] = go;
                        ob[l][k] = bo;
                    }
                }
            }
        }
        
        double mx1, mx2, my1, my2;
        int    ix1, ix2, iy1, iy2, iy, ix, ixend, iyend;
        
        for (i=0; i<this.gridX; i++)
        {
            for (j=0; j<this.gridY; j++)
            {
                if ((this.grmask[i][j] == 0) && (this.grmotCount[i][j] != 0))
                {
                    this.grmotVec[i][j][0] = this.grmotion[i][j][0] / this.grmotCount[i][j];
                    this.grmotVec[i][j][1] = this.grmotion[i][j][1] / this.grmotCount[i][j];
                    if (this.grmotVec[i][j][0] > 1) this.grmotVec[i][j][0] = 1;
                    if (this.grmotVec[i][j][1] > 1) this.grmotVec[i][j][1] = 1;
                    
                    // Calculate begin and end of motion vector
                    px    = (int)( i   *gsx); py  = (int)( j   *gsy);
                    pxe   = (int)((i+1)*gsx); pye = (int)((j+1)*gsy);
                    mx1   = (px+pxe)/2;
                    my1   = (py+pye)/2;
                    mx2   = mx1 + ((gsx)*grmotVec[i][j][0]);
                    my2   = my1 + ((gsy)*grmotVec[i][j][1]);
                    
                    if (mx1 >= pw) mx1 = pw-1; if (mx1 < 0) mx1 = 0;
                    if (my1 >= ph) my1 = ph-1; if (my1 < 0) my1 = 0;
                    if (mx2 >= pw) mx2 = pw-1; if (mx2 < 0) mx2 = 0;
                    if (my2 >= ph) my2 = ph-1; if (my2 < 0) my2 = 0;
                    
                    final double pixo    = 0.2;
                    final double pixoend = 1.0;
                    
                    // Draw little line. The standard stuff.
                    ix1 = (int)mx1; ix2 = (int)mx2; iy1 = (int)my1; iy2 = (int)my2;
                    ixend = ix2; iyend = iy2;
                    
                    int dix = Math.abs(ix2 - ix1);
                    int diy = Math.abs(iy2 - iy1);
                    if ((dix > 2) && (diy > 2))        // Make sure there is a little line to draw, otherwise just skip it.
                    {
                        if      ( dix == 0 )
                        {
                            if (iy1 > iy2) { iy = iy1; iy1 = iy2; iy2 = iy; }
                            ix  = ix1;
                            for (iy=iy1; iy<=iy2; iy++)
                            {
                                or[iy][ix] = pixo;
                                og[iy][ix] = pixo;
                                ob[iy][ix] = pixo;
                            }
                        }
                        else if ( diy == 0 )
                        {
                            if (ix1 > ix2) { ix = ix1; ix1 = ix2; ix2 = ix; }
                            iy  = iy1;
                            for (ix=ix1; ix<=ix2; ix++)
                            {
                                or[iy][ix] = pixo;
                                og[iy][ix] = pixo;
                                ob[iy][ix] = pixo;
                            }
                        }
                        else
                        {
                            // x1 is always left
                            if (ix2 < ix1) { ix = ix1; iy = iy1; ix1 = ix2; iy1 = iy2; ix2 = ix; iy2 = iy;  }
                            
                            double xstep, ystep;
                            xstep = (ix2 - ix1) / (iy2 - iy1);
                            if (iy2 > iy1) ystep = 1; else ystep = -1;
                            mx1 = ix1; mx2 = mx1+xstep;
                            for (my1 = iy1; my1 <= iy2; my1 += ystep)
                            {
                                ix  = (int)mx1;
                                ix2 = (int)mx2;
                                iy  = (int)my1;
                                for (k=ix; k<=ix2; k++)
                                {
                                    or[iy][k] = pixo;
                                    og[iy][k] = pixo;
                                    ob[iy][k] = pixo;
                                }
                                mx1 += xstep; mx2 += xstep;
                            }
                        }
                        or[iyend][ixend] = pixoend;
                        og[iyend][ixend] = pixoend;
                        ob[iyend][ixend] = pixoend;
                    }
                }
            }
        }
    }
    
    private void displayMonitor(ObjectMatrix1D vidvec)
    {
        int        i,j,k,l;
        int        px,py,pxe,pye;
        double [][]matr, matg, matb;
        double     r,g,b,ro,go,bo;
        double [][]or, og, ob;
        int        pw, ph;
        double     wei, mwei, mot;
        
        pw   = this.width;
        ph   = this.height;
        or   = this.outr;
        og   = this.outg;
        ob   = this.outb;
        matr = (double [][])vidvec.getQuick(0);
        matg = (double [][])vidvec.getQuick(1);
        matb = (double [][])vidvec.getQuick(2);
        
        // Show motion in red, taking into account the trained motion grid
        for (i=0; i<this.gridX; i++)
        {
            for (j=0; j<this.gridY; j++)
            {
                wei = this.grwei[i][j];
                
                px  = (int)( i   *gsx); py  = (int)( j   *gsy);
                pxe = (int)((i+1)*gsx); pye = (int)((j+1)*gsy);
                if (pxe > pw) pxe = (int)pw;
                if (pye > ph) pye = (int)ph;
                for (k=px; k<pxe; k++)
                {
                    for (l=py; l<pye; l++)
                    {
                        r   = matr[l][k];
                        g   = matg[l][k];
                        b   = matb[l][k];
                        mot = this.picMotion[l][k];
                        mot = (1-wei)*mot;
                        
                        if (this.grmask[i][j] == 0)
                        {
                            ro  = mot + (1-mot) * r;
                            go  =       (1-mot) * g;
                            bo  =       (1-mot) * b;
                        }
                        else
                        {
                            mwei = 0.5;
                            mot  = this.picMotion[l][k] / 256;
                            if (grmask[i][j] == -1)
                            {
                                ro  =        (1-mwei) * r;
                                go  =        (1-mwei) * g;
                                bo  = mwei + (1-mwei) * b;
                            }
                            else
                            {
                                ro  = mwei + (1-mwei) * (mot + (1-mot)*r);
                                go  =        (1-mwei) * g;
                                bo  = mwei + (1-mwei) * b;
                            }
                        }
                        or[l][k] = ro;
                        og[l][k] = go;
                        ob[l][k] = bo;
                    }
                }
            }
        }
    }
    
    private void displayFindView(ObjectMatrix1D vidvec)
    {
        int    i,j;
        double [][]or, og, ob;
        double [][]matr,  matg,  matb;
        double [][]matr2, matg2, matb2;
        
        or    = this.outr;
        og    = this.outg;
        ob    = this.outb;
        matr  = (double [][])vidvec.getQuick(0);
        matg  = (double [][])vidvec.getQuick(1);
        matb  = (double [][])vidvec.getQuick(2);
        matr2 = (double [][])this.picReference.getQuick(0);
        matg2 = (double [][])this.picReference.getQuick(1);
        matb2 = (double [][])this.picReference.getQuick(2);
        
        // Mix Reference View with live image
        if (this.picReference != null)
        {
            // Return mix between reference View and input
            for (i=0; i<this.height; i++)
            {
                for (j=0; j<this.width; j++)
                {
                    or[i][j] = (matr[i][j]+matr2[i][j])/2.0;
                    og[i][j] = (matg[i][j]+matg2[i][j])/2.0;
                    ob[i][j] = (matb[i][j]+matb2[i][j])/2.0;
                }
            }
        }
        else displayLive(vidvec);
    }
    
    private void displayGridMotion(ObjectMatrix1D vidvec)
    {
        int        i,j,k,l;
        int        px,py,pxe,pye;
        double [][]matr, matg, matb;
        double     r,g,b,ro,go,bo;
        double [][]or, og, ob;
        int        pw, ph;
        double     grwei;
        
        pw   = this.width;
        ph   = this.height;
        or   = this.outr;
        og   = this.outg;
        ob   = this.outb;
        matr = (double [][])vidvec.getQuick(0);
        matg = (double [][])vidvec.getQuick(1);
        matb = (double [][])vidvec.getQuick(2);
        
        for (i=0; i<gridX; i++)
        {
            for (j=0; j<gridY; j++)
            {
                grwei = this.grnow[i][j];
                px    = (int)( i   *gsx); py  = (int)( j   *gsy);
                pxe   = (int)((i+1)*gsx); pye = (int)((j+1)*gsy);
                if (pxe > pw) pxe = (int)pw;
                if (pye > ph) pye = (int)ph;
                for (k=px; k<pxe; k++)
                {
                    for (l=py; l<pye; l++)
                    {
                        r   = matr[l][k];
                        g   = matg[l][k];
                        b   = matb[l][k];
                        
                        ro  = grwei  + (1-grwei) * r;
                        go  =          (1-grwei) * g;
                        bo  =          (1-grwei) * b;
                        
                        or[l][k] = ro;
                        og[l][k] = go;
                        ob[l][k] = bo;
                    }
                }
            }
        }
    }
    
    private void displayGridWeight(ObjectMatrix1D vidvec)
    {
        int        i,j,k,l;
        int        px,py,pxe,pye;
        double [][]or, og, ob;
        int        pw, ph;
        double     wei;
        
        pw   = this.width;
        ph   = this.height;
        or   = this.outr;
        og   = this.outg;
        ob   = this.outb;
        
        // Show learned weighting grid
        for (i=0; i<gridX; i++)
        {
            for (j=0; j<gridY; j++)
            {
                wei = this.grwei[i][j];
                px  = (int)( i   *gsx); py  = (int)( j   *gsy);
                pxe = (int)((i+1)*gsx); pye = (int)((j+1)*gsy);
                if (pxe > pw) pxe = (int)pw;
                if (pye > ph) pye = (int)ph;
                for (k=px; k<pxe; k++)
                {
                    for (l=py; l<pye; l++)
                    {
                        or[l][k] = wei;
                        og[l][k] = wei;
                        ob[l][k] = wei;
                    }
                }
            }
        }
    }    
    
    // **********************************************************\
    // *              Transformation Implementation             *
    // **********************************************************/
    public Object []transform(Object obin) throws DataFlowException
    {
        if (obin instanceof ObjectMatrix1D)
        {
            ObjectMatrix1D vidvec, outvec;
            
            try
            {
                vidvec = (ObjectMatrix1D)obin;
                outvec = avmd(vidvec);
            }
            catch(LearnerException ex) { throw new DataFlowException(ex); }
            
            return(new Object[]{outvec});
        }
        else throw new DataFlowException("Cannot operate on non video vectors");
    }
    
    // **********************************************************\
    // *                Transformation Implementation           *
    // **********************************************************/
    public void init() throws ConfigException
    {
        DataModel  dmsup, dmout;
     
        // Make sure the input is compatible with this transformation's data requirements
        dmsup = getSupplierDataModel(0);
        checkDataModelFit(0, dmsup);
        dmout = dmsup;
        setOutputDataModel(0, dmout);
        
        // Remember width and height of input video
        this.propVid = (DataModelPropertyVideo)dmsup.getProperty(DataModelPropertyVideo.PROPERTY_VIDEO);
        this.width   = this.propVid.getWidth();
        this.height  = this.propVid.getHeight();
        this.gsx     = this.width  / gridX;
        this.gsy     = this.height / gridY;
        
        // Initialize algorithm buffers
        initBuffers(dmsup);
    }
    
    void initBuffers(DataModel dm)
    {
        int i,j;
        
        // Get Picture Buffers
        this.pics         = new double[NUMPICS][this.height][this.width];
        this.picReference = null;
        this.picOut       = ObjectFactory1D.dense.make(dm.getAttributeCount());
        this.outr         = new double[this.height][this.width];
        this.outg         = new double[this.height][this.width];
        this.outb         = new double[this.height][this.width];
        this.picOut.setQuick(0, this.outr);
        this.picOut.setQuick(1, this.outg);
        this.picOut.setQuick(2, this.outb);
        
        // Get GridBuffers
        this.grnow      = new double[this.gridX][this.gridY];
        this.grall      = new double[this.gridX][this.gridY];
        this.grwei      = new double[this.gridX][this.gridY];
        this.grmask     = new double[this.gridX][this.gridY];
        this.grmotion   = new double[this.gridX][this.gridY][2];
        this.grmotCount = new double[this.gridX][this.gridY];
        this.grmotAct   = new double[this.gridX][this.gridY];
        this.grmotVec   = new double[this.gridX][this.gridY][2];
        for (i=0; i<this.gridX; i++) 
            for (j=0; j<this.gridY; j++)
            { 
                this.grall[i][j]       = 0;
                this.grwei[i][j]       = 0;
                this.grmask[i][j]      = 0;
                this.grmotion[i][j][0] = 0;
                this.grmotion[i][j][1] = 0;
                this.grmotCount[i][j]  = 0;
                this.grmotVec[i][j][0] = 0;
                this.grmotVec[i][j][1] = 0;
                this.grmotAct[i][j]    = 0;
            }
        
        // Get motionbuffer
        this.picMotion = new double[this.height][this.width];
        this.brhis     = new double[NUMPICS];
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
    public AVMD()
    {
       super();
       name        = "AVMD";
       description = "Adaptive Video Motion Detection";
    }
}