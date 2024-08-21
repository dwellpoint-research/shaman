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

import java.awt.Component;
import java.io.IOException;

import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.media.PrefetchCompleteEvent;
import javax.media.RealizeCompleteEvent;
import javax.media.Time;
import javax.media.protocol.DataSource;

import org.shaman.dataflow.Transformation;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;

import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Video Sink</h2>
 * Shows Video Vectors
 */
public class VideoSink extends Transformation
{ 
    // **** Run-time Data ***
    private DataModel          datamodel;
    private VideoEffect         videff;
    private Player              vidplayer;
    private VideoPlayerListener vidpll;
    
    private ObjectMatrix1D      vecnow;
    
    // **********************************************************\
    // *             Show incomming video vectors               *
    // **********************************************************/
    private void play(ObjectMatrix1D vidvec)
    {
        this.vecnow = vidvec;
    }
    
    public ObjectMatrix1D collectVideoVector()
    {
        return(this.vecnow);
    }
    
    public void transform() throws DataFlowException
    {
        Object in;
        
        while (this.areInputsAvailable(0, 1))
        {
            in = this.getInput(0);
            if (in != null) play((ObjectMatrix1D)in);
        }
    }
    
    
    // **********************************************************\
    // *                 Parameter Configuration                *
    // **********************************************************/
    public Component getVideoComponent()
    {
        Component video = this.vidpll.getVideoComponent();
        return(video);
    }
    
    public Component getControlComponent()
    {
        Component control = this.vidpll.getControlComponent();
        return(control);
    }

    public void initPlayer() throws ConfigException
    {
        try
        {
            DataModelPropertyVideo apv;
            DataSource             dsp;
           
            try { Thread.sleep(1000); } catch (Exception ex) {}
            
            apv            = (DataModelPropertyVideo)this.datamodel.getProperty(DataModelPropertyVideo.PROPERTY_VIDEO);
            dsp            = apv.getProcessor().getDataOutput();
            this.vidplayer = Manager.createPlayer(dsp);
            this.vidpll    = new VideoPlayerListener(this.vidplayer);

            // Wait for VideoStream to start Playing
            this.vidpll.waitForPlay();
        }
        catch(NoPlayerException ex) { throw new ConfigException(ex); }
        catch(IOException ex)       { throw new ConfigException(ex); }
    }

    public void stopPlayer()
    {
        if (this.vidpll != null)
        {
            this.vidplayer.removeControllerListener(this.vidpll);
            this.vidplayer.stop();
            this.vidpll    = null;
            this.vidplayer = null;
        }
    }
    
    // **********************************************************\
    // *                Transformation Implementation           *
    // **********************************************************/
    public void init() throws ConfigException
    {
        DataModel              dmsup;
        DataModelPropertyVideo apv;
        
        // Make sure the input is compatible with this transformation's data requirements
        dmsup = getSupplierDataModel(0);
        checkDataModelFit(0, dmsup);
        this.datamodel = dmsup;
        
        apv = (DataModelPropertyVideo)dmsup.getProperty(DataModelPropertyVideo.PROPERTY_VIDEO);
        this.videff = apv.getEffect();
        this.videff.setSinkDataModel(this.datamodel);
        this.videff.setVideoSink(this);
        
        // Start Video player
        initPlayer();
    }
    
    public void cleanUp() throws DataFlowException
    {
        stopPlayer();
    }
    
    public void checkDataModelFit(int port, DataModel dmin) throws DataModelException
    {
        // Check for the Audio property in the attributes
        if ((dmin.getAttributeCount() > 0) && !dmin.hasProperty(DataModelPropertyVideo.PROPERTY_VIDEO))
           throw new DataModelException("Cannot find VideoProperty in attributes.");
    }
    
    public int getNumberOfInputs()  { return(1); }
    public int getNumberOfOutputs() { return(0); }
    public String getInputName(int port)
    {
         if   (port == 0) return("Video Input");
         else             return(null);
    }
    public String getOutputName(int port) { return(null); }
    
    // **********************************************************\
    // *              Constructor/State Persistence             *
    // **********************************************************/
    public VideoSink()
    {
       super();
       name        = "Video Sink";
       description = "Shows Video Vectors";
    }
}

/**
 * JMS Video Player Listener
 */
class VideoPlayerListener implements ControllerListener
{
    private Player    mplayer;
    private Component visual  = null;
    private Component control = null;
    private boolean   waiter  = true;

    public VideoPlayerListener(Player player)
    {
        this.mplayer = player;
        this.mplayer.addControllerListener((ControllerListener)this);
        this.mplayer.realize();
    }
    
    public Component getVideoComponent()   { return(this.visual); }
    public Component getControlComponent() { return(this.control); }
    public Player    getMediaPlayer()      { return(this.mplayer); }

    public void waitForPlay()
    {
        // Wait until Prefetch of media is complete and visual component is present
        try
        {
            while (waiter) { Thread.sleep(100); }
        }
        catch(InterruptedException ex) { ex.printStackTrace(); }
    }

    public void controllerUpdate(ControllerEvent ce)
    {
        if (ce instanceof RealizeCompleteEvent)
        {
            this.mplayer.prefetch();
        }
        else if (ce instanceof PrefetchCompleteEvent)
        {
            if (this.visual == null)
            {
                // Get video and control components
                this.visual  = this.mplayer.getVisualComponent();
                this.control = this.mplayer.getControlPanelComponent();
                this.mplayer.start();
                
                // Stop waiting
                this.waiter = false;
            }
        }
        else if (ce instanceof EndOfMediaEvent)
        {
            this.mplayer.setMediaTime(new Time(0));
            this.mplayer.start();
        }
    }
}