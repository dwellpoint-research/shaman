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
package org.shaman.av.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.shaman.av.AVMD;
import org.shaman.av.AudioSource;
import org.shaman.av.ParameterControl;
import org.shaman.av.VideoSink;
import org.shaman.av.VideoSource;
import org.shaman.dataflow.NetworkConnection;
import org.shaman.dataflow.NetworkNode;
import org.shaman.dataflow.Transformation;
import org.shaman.dataflow.TransformationNetwork;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;


/**
 * <h2>Video Application</h2>
 * Video Extensions demo application
 */

// **********************************************************\
// *                   Video Application                    *
// **********************************************************/
public class VideoApp
{
    // GUI
    private VideoFrame   frame;
    private JPanel       videoPanel;
    private File         fileDir;
    private JFileChooser fchoose;
    
    // Video Components
    private TransformationNetwork videoNet;
    private VideoSource           videoSource;
    private VideoSink             videoSink;
    
    // **********************************************************\
    // *                 Parameter Control                      *
    // **********************************************************/
    public void setParameter(double val)
    {
        int            i;
        Transformation []tra;
        boolean        done;
        
        done = false;
        tra  = this.videoNet.getTransformations();
        for (i=0; i<tra.length && !done; i++)
        {
            if (tra[i] instanceof ParameterControl)
            {
                ((ParameterControl)tra[i]).setParameter(val);
                done = true;
            }
        }
    }
    
    // **********************************************************\
    // *                  Application Control                   *
    // **********************************************************/
    public void play() 
    {
        while(true)
        {
            // Push data through network
            if (this.videoSource.getActive())
            {
                //this.videoSource.push();
                try{Thread.sleep(100);} catch(InterruptedException ex) { ex.printStackTrace(); };
            }
            else
            {
                try{Thread.sleep(100);} catch(InterruptedException ex) { ex.printStackTrace(); };
            }
        }
    }
    
    public void openVideoFile() throws ConfigException, DataFlowException
    {
        if (this.fchoose.showOpenDialog(this.frame) == JFileChooser.APPROVE_OPTION)
        {
            File   vfile;
            String path;
            
            // Cleanup current net
            cleanUpVideoNetwork();
            
            // Install the selected file. Start playing.
            vfile = this.fchoose.getSelectedFile();
            path  = vfile.getAbsolutePath();
            this.videoSource.setInputType(VideoSource.TYPE_FILE);
            this.videoSource.setFilePath(path);
            initVideoNetwork();
            
            installVideoComponent();
            
            this.frame.setTitle(VideoFrame.TITLE+" - "+vfile.getName());
        }
    }
    
    public void openRecord() throws ConfigException, DataFlowException
    {
        // Cleanup current net
        cleanUpVideoNetwork();
            
        // Start recording from default recording device.
        this.videoSource.setInputType(AudioSource.TYPE_RECORD);
        this.videoSource.setFilePath(null);
        initVideoNetwork();
        installVideoComponent();
        
        this.frame.setTitle(VideoFrame.TITLE+" - Recording Device");
    }
    
    private void installVideoComponent()
    {
        // Install the Video output component
        this.videoPanel.removeAll();
        this.videoPanel.add(BorderLayout.NORTH,  this.videoSink.getControlComponent());
        this.videoPanel.add(BorderLayout.CENTER, this.videoSink.getVideoComponent());
        this.videoPanel.updateUI();
    }
    
    public void setFilePath(String fpath)
    {
        // Default audio files directory
        this.fileDir = new File(fpath);
    }
  
    // **********************************************************\
    // *                   Initialization / Cleanup             *
    // **********************************************************/
    public void initialize() throws ConfigException
    {
        initGUI();
        makeTestVideoNetwork();
        //makeGHoughVideoNetwork();
    }
    
    private void makeTestVideoNetwork() throws ConfigException
    {
        ArrayList             alnod, alcon;
        NetworkNode         []nnod;
        NetworkConnection   []ncon;
        TransformationNetwork vidnet;
        
        // Make Video network
        alnod  = new ArrayList();
        alcon  = new ArrayList();
        alnod.add(new NetworkNode("VideoSource",   "org.shaman.av.VideoSource",   "Video Input",            0));
        alnod.add(new NetworkNode("AVMD",          "org.shaman.av.AVMD",          "Video Motion Detection", 1));
        alnod.add(new NetworkNode("VideoSink",     "org.shaman.av.VideoSink",     "Video Output",          10));
        alcon.add(new NetworkConnection("VideoSource", 0, "AVMD",      0));
        alcon.add(new NetworkConnection("AVMD",        0, "VideoSink", 0));
        
        nnod = (NetworkNode       [])alnod.toArray(new NetworkNode[]{});
        ncon = (NetworkConnection [])alcon.toArray(new NetworkConnection[]{});
        vidnet = new TransformationNetwork();
        vidnet.grow(0,0);
        vidnet.populate(nnod, ncon);
        
        VideoSource src;
        VideoSink   sink;
        AVMD        avmd;
        
        // Configure it's components
        src  = (VideoSource)vidnet.getTransformation("VideoSource");
        avmd = (AVMD)vidnet.getTransformation("AVMD");
        sink = (VideoSink)  vidnet.getTransformation("VideoSink");
        
        src.setInputType(VideoSource.TYPE_RECORD);
        //avmd.setDisplay(AVMD.DISPLAY_LIVE);
        //avmd.setDisplay(AVMD.DISPLAY_WATCH);
        //avmd.setDisplay(AVMD.DISPLAY_MOTION);
        avmd.setDisplay(AVMD.DISPLAY_TRAIN);
        //avmd.setDisplay(AVMD.DISPLAY_MONITOR);
        //avmd.setDisplay(AVMD.DISPLAY_FINDVIEW);
        //avmd.setProcessMode(AVMD.MODE_WATCH);
        avmd.setProcessMode(AVMD.MODE_TRAIN);
        
        // Remember references
        this.videoNet    = vidnet;
        this.videoSource = src;
        this.videoSink   = sink;
    }
    
    private void makeGHoughVideoNetwork() throws ConfigException
    {
        ArrayList             alnod, alcon;
        NetworkNode         []nnod;
        NetworkConnection   []ncon;
        TransformationNetwork vidnet;
        
        // Make Video network :
        //  -----------------
        // | Source --  Sink |
        //  -----------------
        alnod  = new ArrayList();
        alcon  = new ArrayList();
        alnod.add(new NetworkNode("VideoSource",   "org.shaman.av.VideoSource",   "Video Input",     0));
        alnod.add(new NetworkNode("EdgeDetection", "org.shaman.av.EdgeDetection", "Edge Detection",  1));
        alnod.add(new NetworkNode("GHough",        "org.shaman.av.GHough",        "Shape Detection", 2));
        alnod.add(new NetworkNode("VideoSink",     "org.shaman.av.VideoSink",     "Video Output",   10));
        alcon.add(new NetworkConnection("VideoSource",   0, "EdgeDetection", 0));
        alcon.add(new NetworkConnection("VideoSource",   0, "GHough",        0));
        alcon.add(new NetworkConnection("EdgeDetection", 0, "GHough",        1));
        alcon.add(new NetworkConnection("GHough",        0, "VideoSink",     0));
        
//        alnod.add(new NetworkNode("VideoSource", "org.shaman.av.VideoSource", "Video Input",        0));
//        alnod.add(new NetworkNode("VideoSink",   "org.shaman.av.VideoSink",   "Video Output",      10));
//        alcon.add(new NetworkConnection("VideoSource", 0, "VideoSink", 0));
        
//        alnod.add(new NetworkNode("VideoSource", "org.shaman.av.VideoSource", "Video Input",        0));
//        alnod.add(new NetworkNode("GreyScale",   "org.shaman.av.GreyScale",   "Color to GreyScale", 1));
//        alnod.add(new NetworkNode("Convolution", "org.shaman.av.Convolution", "3x3 Convolution",    2));
//        alnod.add(new NetworkNode("VideoSink",   "org.shaman.av.VideoSink",   "Video Output",       10));
//        alcon.add(new NetworkConnection("VideoSource", 0, "GreyScale",   0));
//        alcon.add(new NetworkConnection("GreyScale",   0, "Convolution", 0));
//        alcon.add(new NetworkConnection("Convolution",   0, "VideoSink", 0));
        
        nnod = (NetworkNode       [])alnod.toArray(new NetworkNode[]{});
        ncon = (NetworkConnection [])alcon.toArray(new NetworkConnection[]{});
        vidnet = new TransformationNetwork();
        vidnet.grow(0,0);
        vidnet.populate(nnod, ncon);
        
        VideoSource src;
        VideoSink   sink;
        
        // Configure it's components
        src  = (VideoSource)vidnet.getTransformation("VideoSource");
        sink = (VideoSink)  vidnet.getTransformation("VideoSink");
        
        src.setInputType(VideoSource.TYPE_RECORD);
        
        // Remember references
        this.videoNet    = vidnet;
        this.videoSource = src;
        this.videoSink   = sink;
    }
    
    private void initVideoNetwork() throws ConfigException
    {
        this.videoNet.init();
        this.videoSource.setActive(true);
    }
    
    private void cleanUpVideoNetwork() throws DataFlowException, ConfigException
    {
        VideoSource src;
        VideoSink   sink;
        
        src  = (VideoSource)this.videoNet.getTransformation("VideoSource");
        sink = (VideoSink)  this.videoNet.getTransformation("VideoSink");
        src.cleanUp();
        sink.cleanUp();
    }
    
    private void initGUI()
    {
        // Make GUI
        this.frame = new VideoFrame(this);    
        frame.validate();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();
        if (frameSize.height > screenSize.height) { frameSize.height = screenSize.height; }
        if (frameSize.width > screenSize.width)   { frameSize.width  = screenSize.width;  }
        frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // Get GUI components
        this.videoPanel = this.frame.getVideoPanel();
        
        // File
        this.fchoose = new JFileChooser(this.fileDir);
    }
    
    public void exit()
    {
        System.exit(0);
    }
  
    // **********************************************************\
    // *                     Construction                       *
    // **********************************************************/
    public static void main(String[] args)
    {
        try
        {
            VideoApp instance;
            
            instance = new VideoApp();
            if (args.length == 1) instance.setFilePath(args[0]);
            
            instance.initialize();
            //instance.openRecord();
            instance.play();
            instance.exit();
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
}