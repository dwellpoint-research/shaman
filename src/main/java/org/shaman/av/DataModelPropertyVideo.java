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
package org.shaman.av;

import javax.media.Processor;

import org.shaman.datamodel.DataModelProperty;


/**
 * <h2>Video Attribute Property</h2>
 */
public class DataModelPropertyVideo implements DataModelProperty
{
    public static final String PROPERTY_VIDEO = "video";
    private int         width, height;
    private VideoEffect effect;
    private Processor   processor;
    private long        timepos;
    
    // **********************************************************\
    // *                Parameter Configuration                 *
    // **********************************************************/
    public void setDimension(int width, int height)
    {
        this.width  = width;
        this.height = height;
    }
    
    public void      setProcessor(Processor proc) { this.processor = proc; }
    public Processor getProcessor() { return(this.processor); }
    
    public void        setEffect(VideoEffect effect) { this.effect = effect; }
    public VideoEffect getEffect() { return(this.effect); }
    
    public int getWidth()  { return(this.width); }
    public int getHeight() { return(this.height); }

    // **********************************************************\
    // *                     Construction                       *
    // **********************************************************/
    public DataModelPropertyVideo()
    {
    }
}
