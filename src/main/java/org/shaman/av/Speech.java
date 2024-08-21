/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                    Audio / Video                      *
 *                                                       *
 *  January 2005                                         *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2005 Shaman Research                   *
\*********************************************************/
package org.shaman.av;

import java.beans.PropertyVetoException;
import java.util.Locale;

import javax.sound.sampled.AudioFormat;
// import javax.speech.AudioException;
// import javax.speech.Central;
// import javax.speech.EngineException;
// import javax.speech.synthesis.Synthesizer;
// import javax.speech.synthesis.SynthesizerModeDesc;
// import javax.speech.synthesis.SynthesizerProperties;
// import javax.speech.synthesis.Voice;

import org.shaman.dataflow.Transformation;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;

import cern.colt.matrix.ObjectMatrix1D;

// import com.sun.speech.freetts.audio.AudioPlayer;
// import com.sun.speech.freetts.audio.JavaStreamingAudioPlayer;
// import com.sun.speech.freetts.jsapi.FreeTTSVoice;

/**
 * <h2>Audio Source</h2>
 * A source of audio vectors
 */
public class Speech extends Transformation
{ 
    // Output parameters
    private int    outSize;
    private String text;
    
    // **** Run-time Data ***
    //private Synthesizer synth;
    private boolean     active;
    
    // Audio Description
    private DataModel              datamodel;
    private DataModelPropertyAudio propAudio;
    private AudioFormat            audioFormat;
    //private AudioPlayerBridge      bridge;
    
    // **********************************************************\
    // *               Generate Audio Vectors                   *
    // **********************************************************/
    public boolean push() throws DataFlowException
    {
        boolean    more;
        
        // Start speaking if there's a text.
        if (this.text != null)
        {
            //this.synth.speakPlainText(text, null);
            this.text   = null;
            this.active = true;
        }
        
        // Wait around for a while
        try { Thread.sleep(10); } catch(InterruptedException ex) { throw new DataFlowException(ex); }
        
        // Check if speaking finished.
        //more = this.synth.getEngineState() != Synthesizer.QUEUE_EMPTY;
        more = false;
        if (this.active && !more) this.active = false;
        
        return(more);
    }
    
    // **********************************************************\
    // *                 Parameter Configuration                *
    // **********************************************************/
    public void setOutputSize(int outSize) { this.outSize = outSize; }
    
    public void setText(String text) { this.text = text; }
    
    public void setActive(boolean active) { this.active = active; }
    
    public boolean getActive() { return(this.active); }
    
    public DataModel getDataModel()
    {
        return(this.datamodel);
    }
        
    // **********************************************************\
    // *                Transformation Implementation           *
    // **********************************************************/
    public void init() throws ConfigException
    {
        DataModel outModel;
        
        /*
        try
        {
            SynthesizerModeDesc   sdesc;
            Synthesizer           synth;
            SynthesizerProperties sprop;
            String              voicename;
            Voice             []voices;
            Voice               voice;
            int                 i;
            
            // Allocate American voice synthesizer
            sdesc = new SynthesizerModeDesc(null, "general", Locale.US, null, null);
            synth = Central.createSynthesizer(sdesc);
            synth.allocate();
            synth.resume();
            
            // Install and customize 8KHz 16-bit voice
            voicename = "kevin16";
            sdesc  = (SynthesizerModeDesc)synth.getEngineModeDesc();
            voices = sdesc.getVoices();
            voice   = null;
            for (i=0; (i<voices.length) && (voice == null); i++)
                if (voices[i].getName().equals(voicename)) voice = voices[i];
            if (voice == null)
                throw new ConfigException("Cannot find voice '"+voicename+"'");
            sprop = synth.getSynthesizerProperties();
            sprop.setVoice(voice);
            sprop.setSpeakingRate(110.0f);
            sprop.setPitch(20);
            sprop.setPitchRange(10);
            
            // Link the voice with the Shaman A/V system
            FreeTTSVoice fvoice;
            com.sun.speech.freetts.Voice fv;
            AudioPlayerBridge apbridge;
            AudioFormat       audioFormat;
            
            fvoice      = (FreeTTSVoice)voice;
            fv          = fvoice.getVoice();
            audioFormat = fv.getAudioPlayer().getAudioFormat();
            audioFormat = new AudioFormat(audioFormat.getSampleRate()*2, 16, 1, true, true);
            
            apbridge = new AudioPlayerBridge(this, this.outSize/2);
            apbridge.setAudioFormat(audioFormat);
            fv.setAudioPlayer(apbridge);
            
            this.bridge      = apbridge;
            this.synth       = synth;
            this.audioFormat = audioFormat;
        }
        catch(EngineException ex)          { throw new ConfigException(ex); }
        catch(IllegalArgumentException ex) { throw new ConfigException(ex); }
        catch(PropertyVetoException ex)    { throw new ConfigException(ex); }
        catch(AudioException ex)           { throw new ConfigException(ex); }
        */
        
        // Make an Audio Stream DataModel and install it on the output port
        outModel = AVDataModels.makeAudioDataModel(this.audioFormat, this.outSize, this.propAudio);
        if (this.propAudio == null) 
            this.propAudio = (DataModelPropertyAudio)outModel.getProperty(DataModelPropertyAudio.PROPERTY_AUDIO);
        setOutputDataModel(0, outModel);
        
        // Initialize audio-player bridge
        //this.bridge.init(this.propAudio);
    }
    
    public void cleanUp() throws DataFlowException
    {
        /*
        try
        {
            this.synth.deallocate();
        }
        catch(EngineException ex) { throw new DataFlowException(ex); }
        */
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
        if   (port == 0) return("Speech Source");
        else return(null);
    }
    
    // **********************************************************\
    // *              Constructor/State Persistence             *
    // **********************************************************/
    public Speech()
    {
       super();
       name        = "Speech Synthesizer";
       description = "Speech Synthesizer component based on the FreeTTS library";
    }
    
    /*
    public static void main(String []args)
    {
        try
        {
            Speech speech = new Speech();
            speech.init();
            
            String text = "Without your space-helmet Dave, you will find that rather difficult.";
            speech.synth.speakPlainText(text, null);
            speech.synth.waitEngineState(Synthesizer.QUEUE_EMPTY);
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
        */
}

/**
 * Bridge between FreeTTS and Shaman Audio
 */
/*
class AudioPlayerBridge extends JavaStreamingAudioPlayer implements AudioPlayer 
{
    private AudioSourceBuffer  buffer;
    private int                outSize;
    private Speech             speech;
    
    public AudioPlayerBridge(Speech speech, int outSize)
    {
        super();
        this.speech  = speech;
        this.outSize = outSize;
    }
    
    public void init(DataModelPropertyAudio propAudio)
    {
        this.buffer = new AudioSourceBuffer();
        this.buffer.setOutputSize(this.outSize);
        this.buffer.init(propAudio);
    }
    
    public synchronized boolean write(byte []rbuf, int offset, int size)
    {
        ObjectMatrix1D vecout;
        boolean        ok;
        
        ok = true;
        try
        {
            // Make a new audio vector to output
            vecout = this.buffer.makeAudioVector(rbuf, size);
            
            // Output Audio Vector
            if (vecout != null) this.buffer.outputVector(vecout, this.speech);
        }
        catch(DataFlowException ex)
        {
            ex.printStackTrace();
            ok = false;
        }
        
        return(ok);
    }
}
    */