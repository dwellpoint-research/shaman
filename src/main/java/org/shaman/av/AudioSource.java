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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.shaman.dataflow.Transformation;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
// import org.tritonus.sampled.file.AiffAudioFileReader;
// import org.tritonus.sampled.file.AuAudioFileReader;
// import org.tritonus.sampled.file.WaveAudioFileReader;
// import org.tritonus.sampled.file.jorbis.JorbisAudioFileReader;
import org.tritonus.sampled.file.mpeg.MpegAudioFileReader;

import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Audio Source</h2>
 * A source of audio vectors
 */
public class AudioSource extends Transformation
{ 
    // Audio Input Parameters
    public static final int TYPE_WAVE       = 0;
    public static final int TYPE_MPEG       = 1;
    public static final int TYPE_OGG_VORBIS = 2;
    public static final int TYPE_AU         = 3;
    public static final int TYPE_AIFF       = 4;
    public static final int TYPE_RECORD     = 10;
    
    private int    inputType;                           // Type of input file or microphone
    private String filePath;                            // Path to audio file
    
    // Output parameters
    private int    outSize;                             // Number of bytes in output buffer
    
    // **** Run-time Data ***
    private boolean                active;              // Active of paused?
    private AudioInputStream       audioIn;             // AudioFile input
    private TargetDataLine         micIn;               // Microphone input
    private DataModel              datamodel;           // Output Audio DataModel
    private DataModelPropertyAudio propAudio;           // Audio description property
    private AudioFormat            audioFormat;         // Audio format specification
    private AudioSourceBuffer      buffer;              // Raw bytes to Audio Vector converter and buffer
    private byte                 []readbuf;             // Raw byte buffer for audio input reading
    
    // **********************************************************\
    // *               Generate Audio Vectors                   *
    // **********************************************************/
    public boolean push() throws DataFlowException
    {
        int        numread;
        byte     []rbuf;
        boolean    more;
        
        rbuf    = this.readbuf;
        numread = 0;
        try
        {
            if (this.active)
            {
                // Read the next batch of audio data from the right source
                if      (this.audioIn != null) numread = this.audioIn.read(rbuf, 0, rbuf.length);
                else if (this.micIn != null)   numread = this.micIn.read(rbuf, 0, rbuf.length);
                else numread = 0;
                
                // If there was data read
                if (numread > 0)
                {
                    ObjectMatrix1D vecout;
                    
                    // Make a new audio vector to output
                    vecout = this.buffer.makeAudioVector(rbuf, numread);
                
                    // Output Audio Vector
                    if (vecout != null) this.buffer.outputVector(vecout, this);
                }
            }
        }
        catch(IOException ex) { throw new DataFlowException(ex); }
        
        // More data available?
        more = (numread != -1);
        if (!more) this.active = false;
        
        return(more);
    }
    
    // **********************************************************\
    // *                 Parameter Configuration                *
    // **********************************************************/
    public void setInputType(int type) { this.inputType = type; }
    
    public void setFilePath(String path) { this.filePath = path; }
    
    public void setOutputSize(int outSize) { this.outSize = outSize; }
    
    public DataModel getDataModel()
    {
        return(this.datamodel);
    }
    
    public void setActive(boolean active)
    {
        // Reset synchronization when (re)activating
        if (active) this.buffer.resetSync();
        this.active = active;
    }
    
    public boolean getActive() { return(this.active); }
    
    public int guessFileType(String path)
    {
        int    type;
        String ext;
        
        type = TYPE_MPEG;
        if (path.indexOf('.') != -1)
        {
            ext = path.substring(path.indexOf('.')+1);
            ext = ext.trim().toLowerCase();
                 if (ext.equals("mp3"))  type = TYPE_MPEG;
            else if (ext.equals("ogg"))  type = TYPE_OGG_VORBIS;
            else if (ext.equals("wav"))  type = TYPE_WAVE;
            else if (ext.equals("au"))   type = TYPE_AU;
            else if (ext.equals("aiff")) type = TYPE_AIFF;
        }
        
        return(type);
    }
    
    // **********************************************************\
    // *                Transformation Implementation           *
    // **********************************************************/
    public void init() throws ConfigException
    {
        try
        {
            AudioInputStream  ain, audioIn;
            TargetDataLine    tdl;
        
            // Reset sources
            audioIn = null;
            tdl     = null;
            
            if (this.filePath != null)
            {
                AudioFormat.Encoding targetEncoding;
                File faudio;
            
                // Open the Audio File using the right reader
                faudio = new File(this.filePath);
                if (!faudio.exists()) throw new ConfigException("Cannot find specified audio file at "+this.filePath);
                if      (this.inputType == TYPE_MPEG)
                {
                    ID3StripperInputStream sis;
                    
                    // Tritonus ID3v2 tag bug work-around.
                    sis = new ID3StripperInputStream(new FileInputStream(faudio));
                    ain = new MpegAudioFileReader().getAudioInputStream(sis);
                } 
                // else if (this.inputType == TYPE_OGG_VORBIS) ain = new JorbisAudioFileReader().getAudioInputStream(faudio);
                // else if (this.inputType == TYPE_WAVE)       ain = new WaveAudioFileReader().getAudioInputStream(faudio);
                // else if (this.inputType == TYPE_AU)         ain = new AuAudioFileReader().getAudioInputStream(faudio);
                // else if (this.inputType == TYPE_AIFF)       ain = new AiffAudioFileReader().getAudioInputStream(faudio);
                else throw new ConfigException("Cannot find right type of audio decoded");
           
                // Output 16-bit signed pulse code modulation sound
                targetEncoding   = AudioFormat.Encoding.PCM_SIGNED;
                audioIn          = AudioSystem.getAudioInputStream(targetEncoding, ain);
                this.audioFormat = audioIn.getFormat();
            }
            else if (this.inputType == TYPE_RECORD)
            {
                // Open Recording Device that can handle serious sound input.
                try
                {
                    AudioFormat form = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,  44100.0f, 16, 2, 4, 44100.0f, true);
                    Line.Info inf = new DataLine.Info(TargetDataLine.class, form);
                    Line.Info []tar = AudioSystem.getTargetLineInfo(inf);
                    tdl = (TargetDataLine)AudioSystem.getLine(tar[0]);
                    tdl.open(form);
                    tdl.start();
                    this.audioFormat = form;
                }
                catch(LineUnavailableException ex) { throw new ConfigException(ex); }
            }
            
            // Install audio input
                 if (audioIn != null) this.audioIn     = audioIn;
            else if (tdl     != null) this.micIn       = tdl;
            else                      this.audioFormat = null;
        }
        catch(UnsupportedAudioFileException ex) { throw new ConfigException(ex); }
        catch(IOException                   ex) { throw new ConfigException(ex); }
        
        DataModel outModel;
        
        // Make an Audio Stream DataModel and install it on the output port
        outModel = AVDataModels.makeAudioDataModel(this.audioFormat, this.outSize, this.propAudio);
        if (this.propAudio == null) 
            this.propAudio = (DataModelPropertyAudio)outModel.getProperty(DataModelPropertyAudio.PROPERTY_AUDIO);
        setOutputDataModel(0, outModel);
        
        // Make raw input and vector buffers
        if (this.audioFormat != null)
        {
            this.buffer = new AudioSourceBuffer();
            this.buffer.setOutputSize(this.outSize);
            this.buffer.init(this.propAudio);
            
            // Reset time-sync
            this.buffer.resetSync();
            
            // Make raw data buffer
            this.readbuf = new byte[this.outSize*2];
        }
    }
    
    public void cleanUp() throws DataFlowException
    {
        try
        {
            // Close Sources
            if (this.audioIn != null)
            {
                this.audioIn.close();
                this.audioIn = null;
            }
            
            if (this.micIn != null)
            {
                this.micIn.close();
                this.micIn = null;
            }
            this.active  = false;
        }
        catch(IOException ex) { throw new DataFlowException(ex); }
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
        if   (port == 0) return("Audio Source");
        else return(null);
    }
    
    // **********************************************************\
    // *                     Constructor                        *
    // **********************************************************/
    public AudioSource()
    {
       super();
       name        = "Audio Source";
       description = "Source of Audio Vectors";
       
       this.active = false;
    }
}

/**
 * ID3v2 tag bug work-around
 */
class ID3StripperInputStream extends PushbackInputStream
{
    boolean atStart = true;
    public ID3StripperInputStream(InputStream in) {
        super(in, 10);
    }

    /**
     * Reads the ID3 header if it exists and ignores it. if the header does not exist the read bytes are
     * pushed back on to the stream to be read as normal
     */
    public int read(byte[] b, int off, int len) throws IOException
    {
        if(atStart){
            byte[] startOfFile = new byte[10];
            super.read(startOfFile,0,10);
            //@todo do version check of ID3 tag
            if(new String(startOfFile,0,3).equals("ID3")){
                int id3v2Len = decodeID3v2Size(startOfFile);
                skip(id3v2Len);
            }
            else {
                unread(startOfFile);
            }
            atStart=false;
        }
        return super.read(b, off, len);
    }

    /**
     * decode the id3 size from the format 01111111 01111111 01111111 01111111
     * to a normal java number
     * @param header byte[]
     * @return long
     */
    public int decodeID3v2Size(byte [] header){
        int retval1 = header[9];
        int retval2 = header[8];
        int retval3 = header[7];
        int retval4 = header[6];
        int retval = retval1 + (retval2*128) + (retval3*16384) + (retval4*2097152);
        return retval;
    }
}
