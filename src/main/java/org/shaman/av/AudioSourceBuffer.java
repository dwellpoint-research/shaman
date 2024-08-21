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

import javax.sound.sampled.AudioFormat;

import org.shaman.dataflow.Transformation;
import org.shaman.exceptions.DataFlowException;

import cern.colt.matrix.ObjectFactory1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Audio Source Buffer</h2>
 * Maintains audio input buffers and converts
 * raw audio data to Audio vectors.
 */
public class AudioSourceBuffer
{ 
    // Output parameters
    private int    outSize;
    private DataModelPropertyAudio propAudio;
    private AudioFormat            audioFormat;
    private int                    numChan;
    // Read buffers
    private double       [][]vecbuf;
    private double       [][]vecbuf2;
    private int            []vecpos;
    private boolean          vec2;
    // Time-synchronization
    private long             samplePos;
    private long             timePos;
    
    // **********************************************************\
    // *               Generate Audio Vectors                   *
    // **********************************************************/
    public ObjectMatrix1D makeAudioVector(byte []rbuf, int numread)
    {
        double         shnow;
        int            i,j,k;
        ObjectMatrix1D vecout;
        double       []vecaud;
        int            chan;
        int          []chanpos;
        double     [][]vbuf;
        
        // Position in channel and channel select
        chan    = 0;
        
        // Convert read bytes to double vectors of the right size
        vecout = null;
        if (this.vec2) vbuf = this.vecbuf2;
        else           vbuf = this.vecbuf;
        chanpos             = this.vecpos;
        for (i=0; i<numread; i+=2)
        {
            // Convert 2*8bit signed sample to double in [-1,1]
            shnow = ((((double)rbuf[i]*256)) + ((double)rbuf[i+1])) / 32767.0;
            
            // Add to buffer, if buffer full, switch to other one and output vector
            vbuf[chan][chanpos[chan]++] = shnow;
            
            if (chanpos[this.numChan-1] == this.outSize/this.numChan)
            {
                vecout = ObjectFactory1D.dense.make(this.numChan);
                for (j=0; j<this.numChan; j++)
                {
                    vecaud = new double[vbuf[j].length];
                    for (k=0; k<vecaud.length; k++) vecaud[k] = vbuf[j][k];
                    vecout.setQuick(j, vecaud);
                    
                    chanpos[j] = 0;
                }
                this.vec2 = !this.vec2;
                if (this.vec2) vbuf = this.vecbuf2;
                else           vbuf = this.vecbuf;
            }
            
            // Next channel
            chan++;
            if (chan == this.numChan) { chan = 0; }
        }
        this.vecpos = chanpos;
                
        return(vecout);
    }
    
    public void outputVector(ObjectMatrix1D audioVector, Transformation src) throws DataFlowException
    {
        double tpos;
        long   timePos;
        double tscale;
        
        // Calculate number of ms since begin of source using the sample counter
        tscale       = this.audioFormat.getSampleRate();
        tpos         = this.samplePos/tscale;
        timePos      = (long)(tpos*1000.0);
        this.timePos = timePos;
        this.propAudio.setTimePos(timePos);
        
        // Move on the sample counter
        this.samplePos += this.outSize/this.audioFormat.getChannels();
        
        // Output Audio Data
        src.setOutput(0, audioVector);
    }
    
    public void init(DataModelPropertyAudio propAudio)
    {
        int         numchan;
        AudioFormat format;
        
        format  = propAudio.getAudioFormat();
        numchan = format.getChannels();
        
        this.vecbuf  = new double[numchan][this.outSize/numchan];
        this.vecbuf2 = new double[numchan][this.outSize/numchan];
        this.vecpos  = new int[numchan];
        this.vec2    = false;
        this.numChan = numchan;
        this.propAudio   = propAudio;
        this.audioFormat = format;
    }
    
    // **********************************************************\
    // *                 Parameter Configuration                *
    // **********************************************************/
    public void setOutputSize(int outSize) { this.outSize = outSize; }
    
    public void resetSync()
    {
        // Reset time-synchronization
        this.samplePos = 0;
        this.timePos   = 0;
    }
    
    // **********************************************************\
    // *                      Constructor                       *
    // **********************************************************/
    public AudioSourceBuffer()
    {
    }
}