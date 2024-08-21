/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                    Audio / Video                      *
 *                                                       *
 *  August 2004                                          *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2004-5 Shaman Research                 *
\*********************************************************/
package org.shaman.av;

import javax.media.Processor;
import javax.sound.sampled.AudioFormat;

import org.shaman.datamodel.AttributeObject;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelObject;
import org.shaman.exceptions.DataModelException;


/**
 * <h2>Audio/Video DataModels</h2>
 * Make DataModels for Audio and Video streams.
 */
public class AVDataModels
{
    // **********************************************************\
    // *                     Video Stream                       *
    // **********************************************************/
    public static DataModelObject makeVideoDataModel(int width, int height, VideoEffect videff, Processor vidpr) throws DataModelException
    {
        DataModelPropertyVideo apv;
        DataModelObject        dm;
        AttributeObject        at;
        int                    i;
        final String []primcol = new String[]{"red", "green", "blue"};
        
        // Color Video DataModel : ObjectVector of (Matrix2D red, Matrix2D green, Matrix2D blue)
        dm  = new DataModelObject("Video", 3);
        apv = new DataModelPropertyVideo();
        apv.setDimension(width, height);
        apv.setProcessor(vidpr);
        apv.setEffect(videff);
        dm.addProperty(DataModelPropertyVideo.PROPERTY_VIDEO, apv);
        for (i=0; i<3; i++)
        {
            at  = new AttributeObject();
            at.initAsFreeObject(double [][].class.getName());
            at.setName("channel_"+primcol[i]);
            at.setIsActive(true);
            dm.setAttribute(i, at);
        }
        
        return(dm);
    }
    
    public static DataModelObject makeGreyScaleDataModel(DataModel dmcol)
    {
        DataModelObject        dmgrey;
        AttributeObject        atgrey;
        DataModelPropertyVideo apv;
        
        // GreyScale Video DataModel : ObjectVector of (Matrix2D grey)
        apv    = (DataModelPropertyVideo)dmcol.getProperty(DataModelPropertyVideo.PROPERTY_VIDEO);
        dmgrey = new DataModelObject(dmcol.getName(), 1);
        dmgrey.addProperty(DataModelPropertyVideo.PROPERTY_VIDEO, apv);
        atgrey = new AttributeObject();
        atgrey.initAsFreeObject(double [][].class.getName());
        atgrey.setName("channel_grey");
        atgrey.setIsActive(true);
        dmgrey.setAttribute(0, atgrey);
        
        return(dmgrey);
    }
    
    public static DataModel makeEdgeDataModel(DataModel dmcol)
    {
        DataModelObject        dmedge;
        AttributeObject        atchan;
        DataModelPropertyVideo apv;
        
        // Edge Detected Video DataModel : ObjectVector of (Matrix2D amplitide, Matrix2D direction)
        apv    = (DataModelPropertyVideo)dmcol.getProperty(DataModelPropertyVideo.PROPERTY_VIDEO);
        dmedge = new DataModelObject(dmcol.getName(), 2);
        dmedge.addProperty(DataModelPropertyVideo.PROPERTY_VIDEO, apv);
        atchan = new AttributeObject();
        atchan.initAsFreeObject(double [][].class.getName());
        atchan.setName("channel_amplitude");
        atchan.setIsActive(true);
        dmedge.setAttribute(0, atchan);
        atchan = new AttributeObject();
        atchan.initAsFreeObject(double [][].class.getName());
        atchan.setName("channel_direction");
        atchan.setIsActive(true);
        dmedge.setAttribute(1, atchan);
        
        return(dmedge);
    }
    
    // **********************************************************\
    // *               Multi-Channel Audio Stream               *
    // **********************************************************/
    public static DataModelObject makeAudioDataModel(AudioFormat aformat, int outSize, DataModelPropertyAudio propAudio) throws DataModelException
    {
        DataModelObject        dm;
        AttributeObject        at;
        int                    i, chan;
        
        // Make a primitive datamodel of numbers in [-1,1].
        if (aformat != null) chan = aformat.getChannels();
        else                 chan = 0;
        
        // (Make and) Initialize an Audio DataModel property
        if (propAudio == null) propAudio = new DataModelPropertyAudio();
        propAudio.setAudioFormat(aformat);
        propAudio.setChannels(chan);
        propAudio.setSize(outSize);
        
        // Make an Object based DataModel with for every channel a double vector attribute
        dm  = new DataModelObject("Audio", chan);
        dm.addProperty(DataModelPropertyAudio.PROPERTY_AUDIO, propAudio);
        for (i=0; i<chan; i++)
        {
            at  = new AttributeObject();
            at.initAsFreeObject(double [].class.getName());
            at.setName("channel"+i);
            at.setIsActive(true);
            dm.setAttribute(i, at);
        }
        
        return(dm);
    }
}
