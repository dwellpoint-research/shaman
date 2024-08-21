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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * Audio Input Stream that writes itself to a File
 * Receives raw data input from other Threads.
 */
class AudioSinkInputStream  extends AudioInputStream implements Runnable
{
    private byte []buf;           // The current audio buffer.
    private File   fout;          // The destinatation file
    private Object waiter;        // External data supplier synchronization Objecy

    /**
     * Write audio to the given file in the given format.
     * @param audioFormat
     * @param fout
     */
    public AudioSinkInputStream(AudioFormat audioFormat, File fout)
    {
        super(new ByteArrayInputStream(new byte[0]), audioFormat, AudioSystem.NOT_SPECIFIED);
        this.fout  = fout;
    }
    
    public void run()
    {
        try
        {
            int len;
            
            // Start writing to the file in this Thread.
            this.waiter = new Object();
            len = AudioSystem.write(this, AudioFileFormat.Type.WAVE, this.fout);
            System.err.println("Closed audio file '"+fout.getAbsolutePath()+"' wrote "+len+" bytes.");
        }
        catch(IOException ex) { ex.printStackTrace(); }
    }
    
    public void saveBuffer(byte []buf)
    {
        this.buf   = buf;
        
        // New data to save. Notify saver thread.
        synchronized(this.waiter)
        {
            this.waiter.notifyAll();
        }
    }
    
    int cnt = 0;
    
    public int read(byte[] abData, int nOffset, int nLength) throws IOException
    {
        int i, bpos;
        
        // Saver thread waits for new data.
        synchronized(this.waiter)
        {
            try { this.waiter.wait(); } catch(InterruptedException ex) { }
        }
        
        if (this.buf != null)
        {
            // New data available. Copy to output buffer
            bpos = 0;
            for (i=nOffset; (i<abData.length) && (bpos < this.buf.length); i++) abData[i] = this.buf[bpos++];
        }
        else bpos = -1; // End of stream.
        
        return(bpos);
    }
    
    public int available()
    {
        if (this.buf != null) return(this.buf.length);
        else                  return(0);
    }
    
    public int read()throws IOException
    {
        throw new IOException("cannot use this method currently");
    }
}