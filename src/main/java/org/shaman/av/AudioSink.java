/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                    Audio / Video                      *
 *                                                       *
 *  August - December 2004                               *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2004-5 Shaman Research                 *
\*********************************************************/
package org.shaman.av;

import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.shaman.dataflow.Transformation;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.TransformationException;

import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Audio Sink</h2>
 * Plays audio vectors
 */
public class AudioSink extends Transformation
{ 
    public static final int TYPE_SOUND = 1;
    public static final int TYPE_FILE  = 2;
    
    private int    type;                // Type of output
    private String filePath;            // Path to file in case of file-sink
    
    // **** Run-time Data ***
    private AudioFormat    format;
    private SourceDataLine line;
    private DataModel      datamodel;
    private int            bufSize;
    private int            numChan;
    private DataModelPropertyAudio propAudio;
    
    private AudioSinkInputStream audin;
    private Thread               saveThread;
    private byte               []wbuf;
    
    // **********************************************************\
    // *             Save incomming audio vectors               *
    // **********************************************************/
    public void startSaving(String filePath)
    {
        // Start the thread writing to file. Save() supplies the data to this Thread.
        this.filePath = filePath;
        startFile(filePath);
    }
    
    public void stopSaving()
    {
        // Signal end of save. Thread will shut down.
        if (this.audin != null) this.audin.saveBuffer(null);
    }
    
    private void save(ObjectMatrix1D vec) throws TransformationException
    {
        // Save buffer to file.
        if (this.audin != null)
        {
            // Convert vector to raw byte audio buffer
            toOutputBuffer(this.wbuf, vec);
            
            // Notify saver thread that there's more data
            this.audin.saveBuffer(this.wbuf);
        }
    }
    
    // **********************************************************\
    // *             Play incomming audio vectors               *
    // **********************************************************/
    private void play(ObjectMatrix1D vec) throws TransformationException
    {
        // Convert vector to output buffer
        wbuf  = new byte[this.bufSize];
        toOutputBuffer(wbuf, vec);
        
        // Play audio data
        if (this.line != null) this.line.write(wbuf, 0, wbuf.length);
    }
    
    public void toOutputBuffer(byte []wbuf, ObjectMatrix1D vec)
    {
        double []audvec;
        double [][]dvec;
        int    i, j, numit, pos;
        short  snow;
        byte   bh, bl;
        
        // First find the channels.
        dvec = new double[this.numChan][];
        for(j=0; j<this.numChan; j++)
        {
            audvec  = (double [])vec.getQuick(j);
            dvec[j] = audvec;
        }
        
        // Now convert floating point vector to 16-bit, channel interleaved data
        pos   = 0;
        numit = this.bufSize/(this.numChan*2);
        for (i=0; i<numit; i++)
        {
            for (j=0; j<this.numChan; j++)
            {
                snow = (short)(dvec[j][i]*32767);
                bh   = (byte)(snow>>8);
                bl   = (byte)(snow&0x00FF);
                wbuf[pos++] = bh;
                wbuf[pos++] = bl;
            }
        }
    }
    
    public void transform() throws DataFlowException
    {
        Object in;
        
        while (this.areInputsAvailable(0, 1))
        {
            in = this.getInput(0);
            if (in != null)
            {
                // Play or save the incoming sound vector
                if      (this.type == TYPE_SOUND) play((ObjectMatrix1D)in);
                else if (this.type == TYPE_FILE)  save((ObjectMatrix1D)in);
            }
        }
   }
    
    
    // **********************************************************\
    // *                 Parameter Configuration                *
    // **********************************************************/
    public void setType(int type)            { this.type = type; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    // **********************************************************\
    // *                Transformation Implementation           *
    // **********************************************************/
    public void init() throws ConfigException
    {
        DataModel              dmsup;
        DataModelPropertyAudio apa;
        AudioFormat            format;
     
        // Make sure the input is compatible with this transformation's data requirements
        dmsup = getSupplierDataModel(0);
        checkDataModelFit(0, dmsup);
        this.datamodel = dmsup;
        
        // Prepare the Audio output logic
        DataLine.Info    info;
        
        apa  = (DataModelPropertyAudio)this.datamodel.getProperty(DataModelPropertyAudio.PROPERTY_AUDIO);
        // Get AudioFormat of the audio supplied to this Sink
        if (dmsup.getAttributeCount() > 0)
        {
            format         = apa.getAudioFormat();
            this.propAudio = apa;
            
            // Open 
            if (format != null)
            {
                // Calculate buffersize
                this.format  = format;
                this.numChan = format.getChannels();
                this.bufSize = apa.getSize()*this.numChan;
            
                // Output through sound
                if (this.type == TYPE_SOUND)
                {
                    try
                    {
                        info      = new DataLine.Info(SourceDataLine.class, format);
                        this.line = (SourceDataLine)AudioSystem.getLine(info);
                        this.line.open(format);
                        this.line.start();
                    }
                    catch(LineUnavailableException ex) { throw new ConfigException("Audio output not available"); }
                }
                // Output to file
                else if (this.type == TYPE_FILE)
                {
                    // Make a audio data buffer
                    this.wbuf  = new byte[this.bufSize];
                    
                    // Don't start saving now. Wait for startSaving() call.
                    this.audin = null;
                }
            }
        }
        else this.numChan = 0;
    }
    
    private void startFile(String filePath)
    {
        File     fout;
        
        // Delete the file, if it's there.
        fout            = new File(filePath);
        if (fout.exists()) fout.delete();
        this.filePath   = filePath;
        
        // Start the data save to this file
        this.audin      = new AudioSinkInputStream(format, fout);
        this.saveThread = new Thread(audin);
        this.saveThread.start();
    }
    
    public void cleanUp() throws DataFlowException
    {
        if (this.line != null)
        {
            // Stop audio output
            this.line.stop();
            this.line = null;
        }
        else if (this.audin != null)
        {
            // Signal end of audio input stream
            this.audin.saveBuffer(null);
            this.audin = null;
        }
    }
    
    public void checkDataModelFit(int port, DataModel dmin) throws DataModelException
    {
        // Check for the Audio property in the attributes
        if ((dmin.getAttributeCount() > 0) && !dmin.hasProperty(DataModelPropertyAudio.PROPERTY_AUDIO))
           throw new DataModelException("Cannot find AudioProperty in attributes.");
    }
    
    public int getNumberOfInputs()  { return(1); }
    public int getNumberOfOutputs() { return(0); }
    public String getInputName(int port)
    {
         if   (port == 0) return("Audio Input");
         else             return(null);
    }
    public String getOutputName(int port) { return(null); }
    
    // **********************************************************\
    // *              Constructor/State Persistence             *
    // **********************************************************/
    public AudioSink()
    {
       super();
       name        = "Audio Sink";
       description = "Plays Audio Vectors";
    }
}
