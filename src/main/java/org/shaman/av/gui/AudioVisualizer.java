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

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.sound.sampled.AudioFormat;

import org.shaman.av.DataModelPropertyAudio;
import org.shaman.dataflow.Transformation;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.TransformationException;

import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Audio Visualizer Sink</h2>
 * Visualizes Audio vectors
 */
public class AudioVisualizer extends Transformation
{ 
    private VisualizerComponent    visualConsumer;
    
    // *** Run-time Data ***
    private Map                  []timeAudioMap;      // Array[numChannels] of Map of (Long time -> ObjectMatrix1D audioVector)
    private DataModelPropertyAudio propAudio;
    private AudioFormat            audioFormat;
    private Thread                 consumerThread;
    private int                    numChan;
    // -------
    private long                   timeStart;
    
    // **********************************************************\
    // *     Time-synchronize Audio Vectors and Visualization   *
    // **********************************************************/
    private void buffer(ObjectMatrix1D vec) throws TransformationException
    {
        long   tpos;
        int    i;
        
        // Did a new source of audio just start?
        tpos = this.propAudio.getTimePos();
        if (tpos == 0)
        {
            for(i=0; i<this.numChan; i++) this.timeAudioMap[i].clear();
            this.timeStart = System.currentTimeMillis();
        }
        
        // Add the current vector to the map
        synchronized(this.timeAudioMap)
        {
            for (i=0; i<this.numChan; i++)
            {
                if (this.timeAudioMap[i].size()>100) this.timeAudioMap[i].clear();
                this.timeAudioMap[i].put(new Long(this.timeStart+tpos), vec.getQuick(i));
            }
        }
    }
    
    public void transform() throws DataFlowException
    {
        Object in;
        
        while (this.areInputsAvailable(0, 1))
        {
            in = this.getInput(0);
            if (in != null) buffer((ObjectMatrix1D)in);
        }
    }
    
    public double []getCurrentAudio(int channel)
    {
        Iterator  ittime;
        Set       spast;
        long      tnow, taudio;
        Long      tseek;
        boolean   stop;
        double  []audiovec;
        long      twait;
        
        // Channel out of bounds means no data
        if (channel >= this.numChan) return(null);
        
        // Get the audio vector for this point in time
        audiovec = null;
        tnow  = System.currentTimeMillis();
        twait = -1;
        synchronized(this.timeAudioMap)
        {
            // Make a Set with times of expired vectors
            spast    = new TreeSet();
            ittime   = this.timeAudioMap[channel].keySet().iterator();
            tseek    = null;
            stop     = false;
            while(ittime.hasNext() && (!stop))
            {
                tseek = (Long)ittime.next();
                if (tseek.longValue() < tnow) spast.add(tseek);
                else stop = true;
            }
            
            // Remove expired vectors
            ittime = spast.iterator();
            while(ittime.hasNext()) audiovec = (double [])this.timeAudioMap[channel].remove(ittime.next());
            
            // Get first vector from the map. Calculate time to sync.
            if (this.timeAudioMap[channel].size() > 0)
            {
                taudio   = ((Long)this.timeAudioMap[channel].keySet().iterator().next()).longValue();
                twait    = taudio-tnow;
                audiovec = (double [])this.timeAudioMap[channel].remove(new Long(taudio));
            }
        }
        
        // Wait for the time of this vector to arrive?
        if (twait != -1)
        {
            try { Thread.sleep(twait); } catch(InterruptedException ex) { ex.printStackTrace(); }
        }        
        
        return(audiovec);
    }
    
    // **********************************************************\
    // *                 Parameter Configuration                *
    // **********************************************************/
    public void setConsumer(VisualizerComponent viscon)
    {
        this.visualConsumer = viscon;
    }
    
    // **********************************************************\
    // *                Transformation Implementation           *
    // **********************************************************/
    public void init() throws ConfigException
    {
        DataModel              dmsup;
        DataModelPropertyAudio apa;
        AudioFormat            format;
        int                    i;
     
        // Make sure the input is compatible with this transformation's data requirements
        dmsup = getSupplierDataModel(0);
        checkDataModelFit(0, dmsup);
        
        // Get AudioFormat of the audio supplied to this Sink
        //if (dmsup.getAttributeCount() > 0)
        {
            apa    = (DataModelPropertyAudio)dmsup.getProperty(DataModelPropertyAudio.PROPERTY_AUDIO);
            format = apa.getAudioFormat();
            this.propAudio   = apa;
            this.audioFormat = format;
            this.numChan     = apa.getChannels();
            
            // Prepare Buffers
            this.timeAudioMap = new Map[this.numChan];
            for (i=0; i<this.numChan; i++) this.timeAudioMap[i] = Collections.synchronizedMap(new TreeMap());
        
            // Start Visualization
            this.visualConsumer.setAudioVisualizer(this);
            this.consumerThread = new Thread(this.visualConsumer);
            this.consumerThread.setPriority(Thread.MAX_PRIORITY);
            this.consumerThread.start();
        }
        //else this.numChan = 0;     
    }
    
    public void cleanUp()
    {
        if (this.visualConsumer != null) this.visualConsumer.stop();
        if (this.consumerThread != null)
        {
            this.consumerThread = null;
        }
    }
    
    public void checkDataModelFit(int port, DataModel dmin) throws DataModelException
    {
        // Check for the Audio property in the attributes
        if (!dmin.hasProperty(DataModelPropertyAudio.PROPERTY_AUDIO))
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
    public AudioVisualizer()
    {
       super();
       name        = "Audio Visualizer";
       description = "Visualizes Audio Vectors";
    }
}
