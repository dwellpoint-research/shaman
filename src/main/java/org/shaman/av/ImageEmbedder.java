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

import org.shaman.dataflow.Transformation;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.TransformationException;

import cern.colt.matrix.ObjectMatrix1D;
import cern.jet.random.Uniform;


/**
 * <h2>Image Embedder</h2>
 * Embeds an image in the audio frequency domain of an audio stream.
 */
public class ImageEmbedder extends Transformation
{ 
    // **** Run-time Data ***
    private DataModel      datamodel;
    
    private int            numChan;
    private ObjectMatrix1D image;
    private int            posx;
    
    // **********************************************************\
    // *                Image in Sound Embedder                 *
    // **********************************************************/
    private ObjectMatrix1D embed(ObjectMatrix1D vec) throws TransformationException
    {
        int      i;
        double []vecaud;
        
        for (i=0; i<this.numChan; i++)
        {
            vecaud = (double [])vec.getQuick(i);
            vecaud = embedChannel(vecaud);
            vec.setQuick(i, vecaud);
        }
        
        return(vec);
    }
    
    private double []embedChannel(double []vecaud) throws TransformationException
    {
        // When embedding an image...
        if (this.posx >= 0)
        {
            int    vecsize, imwide, imhigh, x, y;
            double pbeg, pend;
            double pix;
            int    pos, posbeg, posend;
            double pstep, impos;
            double [][]imr, img, imb;
        
            x    = this.posx;
            
            // Find dimensions of Audio and Video data
            vecsize    = vecaud.length;
            imr        = (double [][])this.image.getQuick(0);
            img        = (double [][])this.image.getQuick(1);
            imb        = (double [][])this.image.getQuick(2);
            imwide     = imr[0].length;
            imhigh     = imr.length;
       
            // Determine begin, end and step-size in audio frequency domain;
            pbeg       = 0.75;
            pend       = 0.95;
            posbeg     = (int)(vecsize*pbeg);
            posend     = (int)(vecsize*pend);
            pstep      = 1.0/(posend-posbeg);
            
            // For all the frequency components in the embedded range
            impos = 0;
            for(pos=posbeg; pos<posend; pos++)
            {
                // If the pixel corresponding to the current frequency is bright
                y   = (int)(impos*imhigh);
                pix = (imr[y][x] + img[y][x] + imb[y][x]) / 3.0;
                if (pix > 0.5)
                {
                    // Make some noise on this frequency.
                    pix = Uniform.staticNextDoubleFromTo(-20, 20)*pix;
                    vecaud[pos]         = pix;
                    vecaud[vecsize-pos] = pix;
                }
                impos += pstep;
            }
            
            // Move to the next column in the image. Stop embedded when the edge is reached.
            this.posx++;
            if (this.posx == imwide) this.posx = -1;
        }
        
        return(vecaud);
    }
    
    public Object []transform(Object obin) throws TransformationException
    {
         Object obout;
         
         obout = null;
         if (obin instanceof ObjectMatrix1D)
         {
             obout = embed((ObjectMatrix1D)obin);
         }
         
         return(new Object[]{obout});
    }
    
    // **********************************************************\
    // *                 Parameter Configuration                *
    // **********************************************************/
    
    // **********************************************************\
    // *                Transformation Implementation           *
    // **********************************************************/
    public void init() throws ConfigException
    {
        DataModel              dmsup;
        DataModelPropertyAudio apa;
     
        // Make sure the input is compatible with this transformation's data requirements
        dmsup = getSupplierDataModel(0);
        checkDataModelFit(0, dmsup);
        this.datamodel = dmsup;
        setOutputDataModel(0, dmsup);
        
        apa    = (DataModelPropertyAudio)this.datamodel.getProperty(DataModelPropertyAudio.PROPERTY_AUDIO);
        this.numChan = apa.getChannels();
        
        // No image
        this.posx = -1;
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    public boolean getEmbedding() { return(this.posx != -1); }
    
    public void startImage(ObjectMatrix1D image)
    {
        this.image = image;
        this.posx  = 0;
    }
    
    public void checkDataModelFit(int port, DataModel dmin) throws DataModelException
    {
         // Check if input datamodel is an audio model
         if (!dmin.hasProperty(DataModelPropertyAudio.PROPERTY_AUDIO))
             throw new DataModelException("No Audio Property found in DataModel Attributes.");
    }
    
    public int getNumberOfInputs()  { return(1); }
    public int getNumberOfOutputs() { return(1); }
    public String getInputName(int port)
    {
         if   (port == 0) return("Audio Input");
         else             return(null);
    }
    public String getOutputName(int port)
    {
         if   (port == 0) return("Audio Frequency Output");
         else             return(null);
    }
     
    
    // **********************************************************\
    // *              Constructor/State Persistence             *
    // **********************************************************/
    public ImageEmbedder()
    {
       super();
       name        = "Audio Filter";
       description = "Filter Audio Frequencies";
    }
}
