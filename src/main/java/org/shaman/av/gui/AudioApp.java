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
package org.shaman.av.gui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import org.shaman.av.AudioFFT;
import org.shaman.av.AudioSink;
import org.shaman.av.AudioSource;
import org.shaman.av.ExpressionSynthesizer;
import org.shaman.av.ImageEmbedder;
import org.shaman.av.Speech;
import org.shaman.av.VideoSource;
import org.shaman.dataflow.NetworkConnection;
import org.shaman.dataflow.NetworkNode;
import org.shaman.dataflow.TransformationNetwork;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;

import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Audio Application</h2>
 * Audio Extensions demo application
 */

// **********************************************************\
// *                   Audio Application                    *
// **********************************************************/
public class AudioApp
{
    // GUI
    private AudioFrame   frame;
    private File         fileDir;
    private JFileChooser fchoose;
    
    // Visualization Components
    private AudioScope    scope;
    private AudioSpectrum spectrum;
    
    // Audio Components
    private TransformationNetwork audioNet;
    private AudioSource           audioSource;
    private ExpressionSynthesizer synth;
    private Speech                speech;
    private AudioSink             audioFileSink;
    
    private String embedText;
    private int    etPos;
    
    // **********************************************************\
    // *                          Control                       *
    // **********************************************************/
    public void play() throws DataFlowException, ConfigException
    {
        while(true)
        {
            // Push data through network
            if (this.audioSource != null)
            {
                if (this.audioSource.getActive()) this.audioSource.push();
                else
                {
                    try{Thread.sleep(10);} catch(InterruptedException ex) { ex.printStackTrace(); };
                }
                
                // Check embed text
                checkEmbedText();
            }
            else if (this.synth != null)
            {
                this.synth.push();
            }
            else if (this.speech != null)
            {
                if (this.speech.getActive()) this.speech.push();
                else
                {
                    try{Thread.sleep(10);} catch(InterruptedException ex) { ex.printStackTrace(); };
                }
                
                // Check text input
                checkTextInput();
            }
        }
    }
    
    private void checkTextInput() throws ConfigException
    {
        if ((this.embedText != null) && (this.embedText.endsWith("/")))
        {
            this.speech.setText(this.embedText.substring(1, this.embedText.length()-1));
            this.speech.setActive(true);
            this.embedText = null;
        }
    }
    
    private void checkEmbedText() throws ConfigException
    {
        ImageEmbedder imgemb;
        
        imgemb = (ImageEmbedder)this.audioNet.getTransformation("ImageEmbed");
        if (!imgemb.getEmbedding())
        {
            if (this.embedText != null)
            {
                 //System.err.println(this.embedText);
                 if (this.etPos >= this.embedText.length())
                 {
                     this.etPos     = 0;
                     this.embedText = null;
                 }
                 if (this.etPos > 0)
                 {
                     char c = this.embedText.charAt(this.etPos);
                     embedCharacter(c);
                 }
                 this.etPos++;
            }
            
        }
    }
    
    public void setEmbedText(String emtxt)
    {
        this.embedText = " "+emtxt;
    }
    
    public void embedCharacter(char c) throws ConfigException
    {
        ImageEmbedder imgemb;
        ObjectMatrix1D image;
        
        image  = VideoSource.loadCharacterImage(c, 40, 100);
        imgemb = (ImageEmbedder)this.audioNet.getTransformation("ImageEmbed");
        imgemb.startImage(image);
        
    }
    
    public void embedImage() throws ConfigException
    {
        File          afile;
        String        path;
        ImageEmbedder imgemb;
        ObjectMatrix1D image;
        
        if (this.fchoose.showOpenDialog(this.frame) == JFileChooser.APPROVE_OPTION)
        {
            afile  = this.fchoose.getSelectedFile();
            path   = afile.getAbsolutePath();
            
            //path = "c:/Johan/Film/Dracula_Bela_Lugosi1.jpg";
            //path = "./data/textimage.jpg";
            imgemb = (ImageEmbedder)this.audioNet.getTransformation("ImageEmbed");
            image  = VideoSource.loadImage(new File(path), -1, -1);
            imgemb.startImage(image);
        }
    }
    
    public void openAudioFile() throws DataFlowException, ConfigException
    {
        if (this.fchoose.showOpenDialog(this.frame) == JFileChooser.APPROVE_OPTION)
        {
            File   afile;
            String path;
            int    type;
            
            // Cleanup current net
            cleanUpAudioNetwork();
            
            // Install the selected file. Start playing.
            afile = this.fchoose.getSelectedFile();
            path  = afile.getAbsolutePath();
            type  = this.audioSource.guessFileType(path);            
            this.audioSource.setInputType(type);
            this.audioSource.setFilePath(path);
            initAudioNetwork();
            
            this.frame.setTitle(AudioFrame.TITLE+" - "+afile.getName());
        }
    }
    
    public void openRecord() throws DataFlowException, ConfigException
    {
        // Cleanup current net
        cleanUpAudioNetwork();
            
        // Start recording from default recording device.
        this.audioSource.setInputType(AudioSource.TYPE_RECORD);
        this.audioSource.setFilePath(null);
        initAudioNetwork();
        
        this.frame.setTitle(AudioFrame.TITLE+" - Recording Device");
    }
    
    public void togglePause() throws DataFlowException
    {
        // (de)Pause audio source
        this.audioSource.setActive(!this.audioSource.getActive());
    }
    
    public void setFilePath(String fpath)
    {
        // Default audio files directory
        this.fileDir = new File(fpath);
    }
    
    public void startSaving()
    {
        String savefile;
        
        savefile = "output.wav";
        this.audioFileSink.startSaving(savefile);
    }
    
    public void stopSaving()
    {
        this.audioFileSink.stopSaving();
    }
  
    // **********************************************************\
    // *                   Initialization / Cleanup             *
    // **********************************************************/
    public void initialize() throws ConfigException
    {
        initGUI();
        makeAudioNetworkEmbed();
        //makeAudioNetworkSynth();
        //makeAudioNetworkSpeech();
    }
    
    private void makeAudioNetworkSpeech() throws ConfigException
    {
        ArrayList             alnod, alcon;
        NetworkNode         []nnod;
        NetworkConnection   []ncon;
        TransformationNetwork audionet;
        
        // Make Audio network :
        //  ---------------------------------------------
        // | Synth --   FFT -- Inverse FFT -- Sound Sink |
        // |       \_ Scope \_ Spectrum    \_ File Sink  |
        //  ---------------------------------------------
        alnod  = new ArrayList();
        alcon  = new ArrayList();
        alnod.add(new NetworkNode("Synth",       "org.shaman.av.Speech",                "Speech Synth",           0));
        alnod.add(new NetworkNode("Scope",       "org.shaman.av.gui.AudioVisualizer",   "Time-domain Visualizer", 1));
        alnod.add(new NetworkNode("FFT",         "org.shaman.av.AudioFFT",              "Fast Fourier Transform", 1));
        alnod.add(new NetworkNode("Spectrum",    "org.shaman.av.gui.AudioVisualizer",   "Freq-domain Visualizer", 4));
        alnod.add(new NetworkNode("IFFT",        "org.shaman.av.AudioFFT",              "Inverse FFT",            5));
        alnod.add(new NetworkNode("AudioSink",   "org.shaman.av.AudioSink",             "Sound Output",           6));
        alnod.add(new NetworkNode("FileSink",    "org.shaman.av.AudioSink",             "Sound File Output",      6));
        alcon.add(new NetworkConnection("Synth",   0, "Scope",      0));
        alcon.add(new NetworkConnection("Synth",   0, "FFT",        0));
        alcon.add(new NetworkConnection("FFT",     0, "IFFT",       0));
        alcon.add(new NetworkConnection("FFT",     0, "Spectrum",   0));
        alcon.add(new NetworkConnection("IFFT",    0, "AudioSink",  0));
        alcon.add(new NetworkConnection("IFFT",    0, "FileSink",   0));
        
        nnod = (NetworkNode       [])alnod.toArray(new NetworkNode[]{});
        ncon = (NetworkConnection [])alcon.toArray(new NetworkConnection[]{});
        audionet = new TransformationNetwork();
        audionet.grow(0,0);
        audionet.populate(nnod, ncon);
        
        //ExpressionSynthesizer synth;
        Speech           speech;
        AudioSink        sink, fsink;
        AudioVisualizer  vis, vis2;
        AudioFFT         fft, ifft;
        
        // Configure it's components
        speech = (Speech)         audionet.getTransformation("Synth");
        vis    = (AudioVisualizer)audionet.getTransformation("Scope");
        vis2   = (AudioVisualizer)audionet.getTransformation("Spectrum");
        fft    = (AudioFFT)       audionet.getTransformation("FFT");
        ifft   = (AudioFFT)       audionet.getTransformation("IFFT");
        sink   = (AudioSink)      audionet.getTransformation("AudioSink");
        fsink  = (AudioSink)      audionet.getTransformation("FileSink");
        speech.setOutputSize(1024);
        vis.setConsumer(this.scope);
        fft.setForward(true);
        vis2.setConsumer(this.spectrum);
        ifft.setForward(false);
        sink.setType(AudioSink.TYPE_SOUND);
        fsink.setType(AudioSink.TYPE_FILE);
        audionet.init();
        
        //speech.setText("I tell you, they did not die. They lead our hands, you and I.");
        speech.setText("The Angel of death stands between heaven and earth. Identified with Satan, he is full of eyes.");
        speech.setActive(true);
        
        // Remember references
        this.audioNet      = audionet;
        this.speech        = speech;
        this.audioFileSink = fsink;
    }
    
    private void makeAudioNetworkSynth() throws ConfigException
    {
        ArrayList             alnod, alcon;
        NetworkNode         []nnod;
        NetworkConnection   []ncon;
        TransformationNetwork audionet;
        
        // Make Audio network :
        //  ---------------------------------------------
        // | Synth --   FFT -- Inverse FFT -- Sound Sink |
        // |       \_ Scope \_ Spectrum    \_ File Sink  |
        //  ---------------------------------------------
        alnod  = new ArrayList();
        alcon  = new ArrayList();
        alnod.add(new NetworkNode("Synth",       "org.shaman.av.ExpressionSynthesizer", "Audio Synth",            0));
        alnod.add(new NetworkNode("Scope",       "org.shaman.av.gui.AudioVisualizer",   "Time-domain Visualizer", 1));
        alnod.add(new NetworkNode("FFT",         "org.shaman.av.AudioFFT",              "Fast Fourier Transform", 1));
        alnod.add(new NetworkNode("Spectrum",    "org.shaman.av.gui.AudioVisualizer",   "Freq-domain Visualizer", 4));
        alnod.add(new NetworkNode("IFFT",        "org.shaman.av.AudioFFT",              "Inverse FFT",            5));
        alnod.add(new NetworkNode("AudioSink",   "org.shaman.av.AudioSink",             "Sound Output",           6));
        alnod.add(new NetworkNode("FileSink",    "org.shaman.av.AudioSink",             "Sound File Output",      6));
        alcon.add(new NetworkConnection("Synth",   0, "Scope",      0));
        alcon.add(new NetworkConnection("Synth",   0, "FFT",        0));
        alcon.add(new NetworkConnection("FFT",     0, "IFFT",       0));
        alcon.add(new NetworkConnection("FFT",     0, "Spectrum",   0));
        alcon.add(new NetworkConnection("IFFT",    0, "AudioSink",  0));
        alcon.add(new NetworkConnection("IFFT",    0, "FileSink",   0));
        
        nnod = (NetworkNode       [])alnod.toArray(new NetworkNode[]{});
        ncon = (NetworkConnection [])alcon.toArray(new NetworkConnection[]{});
        audionet = new TransformationNetwork();
        audionet.grow(0,0);
        audionet.populate(nnod, ncon);
        
        ExpressionSynthesizer synth;
        AudioSink        sink, fsink;
        AudioVisualizer  vis, vis2;
        AudioFFT         fft, ifft;
        
        // Configure it's components
        synth = (ExpressionSynthesizer)audionet.getTransformation("Synth");
        vis   = (AudioVisualizer)audionet.getTransformation("Scope");
        vis2  = (AudioVisualizer)audionet.getTransformation("Spectrum");
        fft   = (AudioFFT)       audionet.getTransformation("FFT");
        ifft  = (AudioFFT)       audionet.getTransformation("IFFT");
        sink  = (AudioSink)      audionet.getTransformation("AudioSink");
        fsink = (AudioSink)      audionet.getTransformation("FileSink");
        synth.setOutputSize(4096);
        vis.setConsumer(this.scope);
        fft.setForward(true);
        vis2.setConsumer(this.spectrum);
        ifft.setForward(false);
        sink.setType(AudioSink.TYPE_SOUND);
        fsink.setType(AudioSink.TYPE_FILE);
        audionet.init();
        
        // Remember references
        this.audioNet      = audionet;
        this.synth         = synth;
    }
    
    private void makeAudioNetworkEmbed() throws ConfigException
    {
        ArrayList             alnod, alcon;
        NetworkNode         []nnod;
        NetworkConnection   []ncon;
        TransformationNetwork audionet;
        
        // Make Audio network :
        //  ---------------------------------------------------------------
        // | Source --   FFT -- ImageEmbedder -- Inverse FFT -- Sound Sink |
        // |        \_ Scope                  \_ Spectrum    \_ File Sink  |
        //  ---------------------------------------------------------------
        alnod  = new ArrayList();
        alcon  = new ArrayList();
        alnod.add(new NetworkNode("AudioSource", "org.shaman.av.AudioSource",         "Outputs Audio",          0));
        alnod.add(new NetworkNode("Scope",       "org.shaman.av.gui.AudioVisualizer", "Time-domain Visualizer", 1));
        alnod.add(new NetworkNode("FFT",         "org.shaman.av.AudioFFT",            "Fast Fourier Transform", 1));
        alnod.add(new NetworkNode("ImageEmbed",  "org.shaman.av.ImageEmbedder",       "Embed image in audio",   3));
        alnod.add(new NetworkNode("Spectrum",    "org.shaman.av.gui.AudioVisualizer", "Freq-domain Visualizer", 4));
        alnod.add(new NetworkNode("IFFT",        "org.shaman.av.AudioFFT",            "Inverse FFT",            5));
        alnod.add(new NetworkNode("AudioSink",   "org.shaman.av.AudioSink",           "Sound Output",           6));
        alnod.add(new NetworkNode("FileSink",    "org.shaman.av.AudioSink",           "Sound File Output",      6));
        alcon.add(new NetworkConnection("AudioSource", 0, "Scope",      0));
        alcon.add(new NetworkConnection("AudioSource", 0, "FFT",        0));
        alcon.add(new NetworkConnection("FFT",         0, "ImageEmbed", 0));
        alcon.add(new NetworkConnection("ImageEmbed",  0, "Spectrum",   0));
        alcon.add(new NetworkConnection("ImageEmbed",  0, "IFFT",       0));
        alcon.add(new NetworkConnection("IFFT",        0, "AudioSink",  0));
        alcon.add(new NetworkConnection("IFFT",        0, "FileSink",   0));
        
        nnod = (NetworkNode       [])alnod.toArray(new NetworkNode[]{});
        ncon = (NetworkConnection [])alcon.toArray(new NetworkConnection[]{});
        audionet = new TransformationNetwork();
        audionet.grow(0,0);
        audionet.populate(nnod, ncon);
        
        AudioSource      src;
        AudioSink        sink, fsink;
        AudioVisualizer  vis, vis2;
        AudioFFT         fft, ifft;
        
        // Configure it's components
        src   = (AudioSource)    audionet.getTransformation("AudioSource");
        vis   = (AudioVisualizer)audionet.getTransformation("Scope");
        vis2  = (AudioVisualizer)audionet.getTransformation("Spectrum");
        fft   = (AudioFFT)       audionet.getTransformation("FFT");
        ifft  = (AudioFFT)       audionet.getTransformation("IFFT");
        sink  = (AudioSink)      audionet.getTransformation("AudioSink");
        fsink = (AudioSink)      audionet.getTransformation("FileSink");
        src.setOutputSize(4096);
        vis.setConsumer(this.scope);
        fft.setForward(true);
        vis2.setConsumer(this.spectrum);
        ifft.setForward(false);
        sink.setType(AudioSink.TYPE_SOUND);
        fsink.setType(AudioSink.TYPE_FILE);
        
        // Remember references
        this.audioNet      = audionet;
        this.audioSource   = src;
        this.audioFileSink = fsink;
    }
    
    private void initAudioNetwork() throws ConfigException
    {
        this.audioNet.init();
        if (this.audioSource != null) this.audioSource.setActive(true);
    }
    
    private void cleanUpAudioNetwork() throws ConfigException, DataFlowException
    {
        if (this.audioNet != null) this.audioNet.cleanUp();
    }
    
    private void initGUI()
    {
        // Make GUI
        this.frame = new AudioFrame(this);    
        frame.validate();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();
        if (frameSize.height > screenSize.height) { frameSize.height = screenSize.height; }
        if (frameSize.width > screenSize.width)   { frameSize.width  = screenSize.width;  }
        frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // Get Visualization Components (Panels)
        this.scope    = this.frame.getScope();
        this.spectrum = this.frame.getSpectrum();
        
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
            AudioApp instance;
            
            instance = new AudioApp();
            if (args.length == 1) instance.setFilePath(args[0]);
            
            instance.initialize();
            instance.play();
            instance.exit();
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
}