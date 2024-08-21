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
package org.shaman.av;

import javax.sound.sampled.AudioFormat;

import org.shaman.datamodel.DataModelProperty;


/**
 * <h2>Audio Attribute Property</h2>
 */
public class DataModelPropertyAudio implements DataModelProperty
{
    public static final String PROPERTY_AUDIO = "audio";
    
    private AudioFormat format;       // Format of the audio handled by the datamodel this property is linked to
    private int         channel;      // Number of audio channles
    private int         size;         // Number of samples output in one operation
    private long        timepos;      // Number of ms since begin of media clock
    
    // **********************************************************\
    // *                Parameter Configuration                 *
    // **********************************************************/
    public void        setAudioFormat(AudioFormat format) { this.format = format; }
    public AudioFormat getAudioFormat()                   { return(this.format); }
    
    public void        setTimePos(long timepos) { this.timepos = timepos; }
    public long        getTimePos()             { return(this.timepos); }
    
    public void        setChannels(int channel) { this.channel = channel; }
    public int         getChannels()            { return(this.channel); }
    
    public void        setSize(int size) { this.size = size; }
    public int         getSize()         { return(this.size); }

    // **********************************************************\
    // *                     Construction                       *
    // **********************************************************/
    public DataModelPropertyAudio()
    {
    }
}
