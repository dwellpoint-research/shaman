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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.Codec;
import javax.media.ConfigureCompleteEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoDataSourceException;
import javax.media.NoPlayerException;
import javax.media.PlugInManager;
import javax.media.PrefetchCompleteEvent;
import javax.media.Processor;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.SizeChangeEvent;
import javax.media.UnsupportedPlugInException;
import javax.media.control.TrackControl;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.swing.ImageIcon;

import org.shaman.dataflow.Transformation;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;

import cern.colt.matrix.ObjectFactory1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Video Source</h2>
 * A source of video vectors
 */
public class VideoSource extends Transformation
{ 
    // Audio Input Parameters
    public static final int TYPE_FILE   = 1;
    public static final int TYPE_RECORD = 10;
    
    private int    inputType;
    private String filePath;
    
    // **** Run-time Data ***
    private boolean           active;
    // Video Stream
    private int               width, height; 
    private VideoEffect       videff;
    private Processor         vidpr;
    private ProcessorListener vidprl;
    // Video Description
    private DataModel         datamodel;
 
    // **********************************************************\
    // *               Generate Video Vectors                   *
    // **********************************************************/
    public boolean push()
    {
        boolean    more;
        
        more = active;
        if (more)
        {
        }
        
        return(more);
    }
    
    public void outputVideoVector(ObjectMatrix1D videoVector) throws DataFlowException
    {
        setOutput(0, videoVector);
    }
    
    public static ObjectMatrix1D loadCharacterImage(char c, int wdest, int hdest)
    {
        ObjectMatrix1D  imvec;
        BufferedImage   bi;
        Graphics2D      gr;
        Font            fnt;
        
        bi     = new BufferedImage(wdest, hdest, BufferedImage.TYPE_INT_RGB);
        fnt    = new Font("Arial Black", Font.PLAIN, 60);
        gr     = bi.createGraphics();
        gr.setFont(fnt);
        gr.drawString(""+c, 0 , 70);
        imvec = toImageVector(bi, wdest, hdest);
        
       
        // GHough Shape of Character Start
        GlyphVector gv = fnt.createGlyphVector(gr.getFontRenderContext(), "?");
        Shape sout     = gv.getGlyphOutline(0);
        PathIterator outit = sout.getPathIterator(null);
        FlatteningPathIterator fpit = new FlatteningPathIterator(outit, 10.0d);
        
        double []coords = new double[6];
        double   x,y;
        int      type;
        while(!fpit.isDone())
        {
            type = fpit.currentSegment(coords);
            x    = coords[0];
            y    = coords[1];
            if      (type == 0) System.err.println("MOVETO "+x+", "+y);
            else if (type == 1) System.err.println("LINETO "+x+", "+y);
            else                System.err.println("TYPE   "+type);
            fpit.next();
        }
        
        
//        Font []allfont;
//        allfont = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
//        for (int i=0; i<allfont.length; i++) System.err.println(allfont[i].getName());
        
        return(imvec);
    }
    
    public static ObjectMatrix1D loadImage(File fim, int wdest, int hdest)
    {
        ObjectMatrix1D  imvec;
        ImageIcon       icin, icsc;
        Image           im, imsc;
        BufferedImage   bi;
        Graphics2D      gr;
        int             width,height;
        
        // Load the given file as an image, rescale to specified dimensions
        icin   = new ImageIcon(fim.getAbsolutePath());
        width  = icin.getIconWidth();
        height = icin.getIconHeight();
        if (wdest == -1) wdest = width;
        if (hdest == -1) hdest = height;
        im     = icin.getImage();
        imsc   = im.getScaledInstance(wdest, hdest, Image.SCALE_DEFAULT);
        bi     = new BufferedImage(wdest, hdest, BufferedImage.TYPE_INT_RGB);
        icsc   = new ImageIcon(bi);
        icsc.setImage(imsc);
        gr     = bi.createGraphics();
        gr.drawImage(imsc, 0, 0, null);
        imvec  = toImageVector(bi, wdest, hdest);
        
        return(imvec);
    }
    
    private static ObjectMatrix1D toImageVector(BufferedImage bi, int wdest, int hdest)
    {
        int             i,j,pos;
        ObjectMatrix1D  imvec;
        double    [][][]imbuf;
        int             pix;
        double          r,g,b;
        int           []rgbbuf;
        final int rMask = 0x000000FF;
        final int gMask = 0x0000FF00;
        final int bMask = 0x00FF0000;
        
        // Convert image data to Video Vector format
        rgbbuf = bi.getRGB(0, 0, wdest, hdest, null, 0, wdest);
        imbuf  = new double[3][hdest][wdest];
        pos    = 0;
        for (i=0; i<hdest; i++)
        {
            for(j=0; j<wdest; j++)
            {
                pix = rgbbuf[pos];
                r   = ((pix & bMask) >> 16)/255.0;
                g   = ((pix & gMask) >> 8 )/255.0;
                b   = ((pix & rMask)      )/255.0;
                imbuf[0][i][j] = r;
                imbuf[1][i][j] = g;
                imbuf[2][i][j] = b;
                pos++;
            }
        }
        
        imvec = ObjectFactory1D.dense.make(3);
        imvec.setQuick(0, imbuf[0]);
        imvec.setQuick(1, imbuf[1]);
        imvec.setQuick(2, imbuf[2]);
        
        return(imvec);
    }
    
    // **********************************************************\
    // *                 Parameter Configuration                *
    // **********************************************************/
    public void setInputType(int type) { this.inputType = type; }
    
    public void setFilePath(String path) { this.filePath = path; }
    
    public void    setActive(boolean active) { this.active = active; }
    public boolean getActive()               { return(this.active); }
    
    public DataModel getDataModel()
    {
        return(this.datamodel);
    }
    
    // **********************************************************\
    // *                Transformation Implementation           *
    // **********************************************************/
    public void init() throws ConfigException
    {
        MediaLocator mediaLoc;
        
        // Make the video effect if not yet there
        if (this.videff == null) makeVideoEffect();
        
        // Get URL of video source
        mediaLoc = null;
        if      (this.inputType == TYPE_FILE)
        {
            // Just a File...
            try
            { 
                URL mediaURL = new File(this.filePath).toURL();
                mediaLoc = new MediaLocator(mediaURL);
            }
            catch(MalformedURLException ex) { throw new ConfigException(ex); }
        }
        else if (this.inputType == TYPE_RECORD) mediaLoc = initVideoInput(); // JMF Media Input
        
        // Open Source
        openVideo(mediaLoc);
        
        DataModel outModel;
        
        // Make a Color Video Stream datamodel on set it as Output DataModel.
        outModel       = AVDataModels.makeVideoDataModel(this.width, this.height, this.videff, this.vidpr);
        this.datamodel = outModel;
        this.videff.setSourceDataModel(outModel);
        setOutputDataModel(0, outModel);
    }
    
    public MediaLocator initVideoInput() throws ConfigException
    {
        YUVFormat yuvspec;
        Vector    viddevlist;
        CaptureDeviceInfo di;
        
        // Check if there is a Video Input Device with the right capabilities
        yuvspec    = new YUVFormat(new Dimension(352,288), Format.NOT_SPECIFIED, byte [].class, (float)15.0, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,Format.NOT_SPECIFIED,Format.NOT_SPECIFIED,Format.NOT_SPECIFIED,Format.NOT_SPECIFIED );
        viddevlist = CaptureDeviceManager.getDeviceList(yuvspec);
        di         = (CaptureDeviceInfo)viddevlist.elementAt(0);
       
        return(di.getLocator());
    }
    
    private void makeVideoEffect() throws ConfigException
    {
        final int rMask = 0x000000FF;
        final int gMask = 0x0000FF00;
        final int bMask = 0x00FF0000;
        Format       []supportedInputFormats;
        Format       []supportedOutputFormats;
        
        // Make Video Processing Effect Chain
        RGBFormat supportedRGB = 
            new RGBFormat(null,                  // size
                          Format.NOT_SPECIFIED,  // maxDataLength
                          int[].class,           // buffer type
                          Format.NOT_SPECIFIED,  // frame rate
                          32,                    // bitsPerPixel
                          rMask, gMask, bMask,   // component masks
                          1,                     // pixel stride
                          Format.NOT_SPECIFIED,  // line stride
                          Format.FALSE,          // flipped
                          Format.NOT_SPECIFIED   // endian
                     );
        supportedInputFormats  = new VideoFormat[]{supportedRGB};
        supportedOutputFormats = new VideoFormat[]{supportedRGB};
        PlugInManager.addPlugIn("A/V VideoEffect", supportedInputFormats, supportedOutputFormats, PlugInManager.EFFECT);
        this.videff = new VideoEffect();
        this.videff.setInputFormat(supportedRGB);
        this.videff.setOutputFormat(supportedRGB);
        this.videff.setVideoSource(this);
    }
       
    private void openVideo(MediaLocator videoLoc) throws ConfigException
    {
        try
        {
            Codec        []videffchain;
            TrackControl []tc;
            DataSource     ds;
            
            // Effect chain just contains the VideoEffect
            videffchain    = new VideoEffect[1];
            videffchain[0] = this.videff;
        
            // Configure Video Processor
            ds = (DataSource)Manager.createDataSource(videoLoc);
            this.vidpr  = Manager.createProcessor(ds);
            this.vidprl = new ProcessorListener(this.vidpr, this);
            this.vidpr.addControllerListener(this.vidprl);
            this.vidpr.configure();
            this.vidprl.waitForState(Processor.Configured);
            this.vidpr.setContentDescriptor(new ContentDescriptor(ContentDescriptor.RAW));

            // Set Effect Chain
            tc = vidpr.getTrackControls();
            tc[0].setCodecChain(videffchain);

            // Realize Processor
            this.vidpr.realize();
            this.vidprl.waitForState(Processor.Realized);

            // Start Processor
            this.vidpr.prefetch();
            this.vidprl.waitForState(Processor.Prefetched);
            this.vidpr.start();
               
            // Get actual dimensions from effect
            this.width  = this.videff.getWidth();
            this.height = this.videff.getHeight();
       }
       catch(UnsupportedPlugInException ex) { throw new ConfigException(ex); }
       catch(NoDataSourceException ex)      { throw new ConfigException(ex); }
       catch(NoPlayerException ex)          { throw new ConfigException(ex); }
       catch(IOException ex)                { throw new ConfigException(ex); }
    }

    public void endProcessor()
    {
        // Stop the processor. Cleanup resources.
        if (this.vidpr != null)
        {
            this.vidpr.stop();
            this.vidpr.close();
            this.vidprl.waitForState(Processor.Unrealized);
            this.vidpr = null;
            this.videff.close();
            this.videff = null;
            this.vidprl = null;
        }
    }
    
    public void cleanUp() throws DataFlowException
    {
        endProcessor();
        this.active = false;
    }
    
    public void checkDataModelFit(int port, DataModel dmin) throws DataModelException
    {
        ; // All fine. No inputs anyway
    }
    
    public int getNumberOfInputs()  { return(0); }
    public int getNumberOfOutputs() { return(1); }
    public String getInputName(int port) {  return(null); }
    public String getOutputName(int port)
    {
        if   (port == 0) return("Video Source");
        else return(null);
    }
    
    // **********************************************************\
    // *              Constructor/State Persistence             *
    // **********************************************************/
    public VideoSource()
    {
       super();
       name        = "Video Source";
       description = "Source of Video Vectors";
    }
}

/**
 * JMF Processor State Listener
 */
class ProcessorListener implements ControllerListener
{
    private VideoSource src;
    private Processor   p;
    private Object      waitSync          = new Object();
    private boolean     stateTransitionOK = true;

    public ProcessorListener(Processor p, VideoSource src)
    {
        this.p   = p;
        this.src = src;
    }
    
    public boolean waitForState(int state)
    {
        synchronized (this.waitSync)
        {
            try { while (this.p.getState() < state && 
                         stateTransitionOK) this.waitSync.wait();
                } catch (Exception e) {}
        }
        
        return this.stateTransitionOK;
    }
    
    public void controllerUpdate(ControllerEvent evt)
    {
        if (evt instanceof ConfigureCompleteEvent || evt instanceof RealizeCompleteEvent ||  evt instanceof PrefetchCompleteEvent)
        {
            // Ready for action when pre-fetch finished.
            if (evt instanceof PrefetchCompleteEvent)
            {
                this.src.setActive(true);
            }
            
            // Notify the waiting thread that an event has occured
            synchronized (waitSync)
            {
                this.stateTransitionOK = true;
                this.waitSync.notifyAll();
            }
        }
        else if (evt instanceof ResourceUnavailableEvent)
        {
            synchronized (waitSync)
            {
                this.stateTransitionOK = false;
                this.waitSync.notifyAll();
            }
        }
        else if (evt instanceof EndOfMediaEvent)
        {
            // No more data.
            this.src.setActive(false);
        }
        else if (evt instanceof SizeChangeEvent)
        {
        }
    }
}