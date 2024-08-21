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

import javax.media.Buffer;
import javax.media.Control;
import javax.media.Effect;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;

import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.DataFlowException;

import cern.colt.matrix.ObjectFactory1D;
import cern.colt.matrix.ObjectMatrix1D;


public class VideoEffect implements Effect
{
    // Red, Green and Blue Masks
    private static final int MASK_R = 0x000000FF;
    private static final int MASK_G = 0x0000FF00;
    private static final int MASK_B = 0x00FF0000;
    
    // Source and Sink datamodels
    private DataModel    sourceDataModel;
    private DataModel    sinkDataModel;
    private VideoSource  source;
    private VideoSink    sink;
    
    // JMF Logic
    private String      effectName = "Shaman VideoEffect";
    private RGBFormat   supportedRGB;
    private VideoFormat inputFormat;
    private VideoFormat outputFormat;
    private Format    []supportedInputFormats;
    private Format    []supportedOutputFormats;

    // *** Run-time Data ***
    private int        numChannels;
    private int        width, height;
    private double [][]matr, matg, matb;
    
    // **********************************************************\
    // *              Video Processing Effect                   *
    // **********************************************************/
    public int process(Buffer inputBuffer, Buffer outputBuffer)
    {
        int []inb;
        int inLen, inOff;
        int []outb;
        int outLen, outOff;
        int            ret;
        ObjectMatrix1D vidvec;

        // Validate JMF
        inb    = (int [])inputBuffer.getData();
        inLen  = inputBuffer.getLength();
        inOff  = inputBuffer.getOffset();
        outb   = validateIntArraySize(outputBuffer, inLen);
        outLen = inLen;
        outOff = outputBuffer.getOffset();

        try
        {
            // Transform input to vector
            vidvec = toVector(inb, inLen,  inOff);
            
            // Process through transformation network
            if (this.source != null) this.source.outputVideoVector(vidvec);
            if (this.sink   != null) vidvec = this.sink.collectVideoVector();
            else vidvec = null;
            
            if (vidvec != null)
            {
                // Convert output to buffer
                toBuffer(outb, outOff, vidvec);
                updateOutput(outputBuffer, this.outputFormat, outLen, outOff);
        
                ret = BUFFER_PROCESSED_OK;
            }
            else ret = BUFFER_PROCESSED_FAILED;
        }
        catch(DataFlowException ex) { ret = BUFFER_PROCESSED_FAILED; }
        
        return(ret);
    }
    
    private ObjectMatrix1D toVector(int []inb, int inLen, int inOff)
    {
        ObjectMatrix1D vidvec;
        int            bufpos, i, j;
        int            v, c;
        double         d;
        
        // Move integer buffer to 3 (r,g,b) 2D double matrices
        vidvec = ObjectFactory1D.dense.make(3);
        vidvec.setQuick(0, this.matr);
        vidvec.setQuick(1, this.matg);
        vidvec.setQuick(2, this.matb);
        
        bufpos = inOff;
        for (i=0; i<this.height; i++)
        {
            for(j=0; j<this.width; j++)
            {
                v = inb[bufpos];
                c = v & MASK_R;
                d = c / 256.0;
                this.matr[i][j] = d;
                c = (v & MASK_G) >> 8;
                d = c / 256.0;
                this.matg[i][j] = d;
                c = (v & MASK_B) >> 16;
                d = c / 256.0;
                this.matb[i][j] = d;
                bufpos++;
            }
        }
        
        return(vidvec);
    }
    
    private void toBuffer(int []outb, int outOff, ObjectMatrix1D vidvec)
    {
        int  i,j,bufpos;
        
        bufpos = outOff;
        if (this.numChannels == 3)
        {
            double [][]matr, matg, matb;
            double     r,g,b;
            int        c, v;
            
            // Convert RGB color-channel 2D double matrices to integer buffer
            matr   = (double [][])vidvec.getQuick(0);
            matg   = (double [][])vidvec.getQuick(1);
            matb   = (double [][])vidvec.getQuick(2);
            for (i=0; i<this.height; i++)
            {
                for (j=0; j<this.width; j++)
                {
                    r = matr[i][j];
                    g = matg[i][j];
                    b = matb[i][j];
                    v = 0;
                    c = (int)(b*255.0);
                    v |= (c << 16);
                    c = (int)(g*255.0);
                    v |= (c << 8);
                    c = (int)(r*255.0);
                    v |= c;
                    outb[bufpos++] = v;
                }
            }
        }
        else if (this.numChannels == 1)
        {
            double [][]matgrey;
            double     g;
            int        c,v;
            
            // Convert intensity channel 2D double matrix to integer buffer
            matgrey = (double [][])vidvec.getQuick(0);
            for (i=0; i<this.height; i++)
            {
                for(j=0; j<this.width; j++)
                {
                    g = matgrey[i][j];
                    c = (int)(g*255.0);
                    v = c | (c<<8) | (c<<16);
                    outb[bufpos++] = v;
                }
            }
        }
        else if (this.numChannels == 2)
        {
            double [][]matamp, matcol;
            double     a, c, dr, dg, db;
            int        f,v;
            
            // Convert intensity and color channel 2D double matrix to integer buffer
            matamp = (double [][])vidvec.getQuick(0);
            matcol = (double [][])vidvec.getQuick(1);
            for (i=0; i<this.height; i++)
            {
                for(j=0; j<this.width; j++)
                {
                    if (matcol[i][j] != -2)
                    {
                        a = matamp[i][j];
                        c = matcol[i][j];
                        
                        c /= Math.PI;
                        
                        if (c<0.0) { db = 1.0; dr = 1.0; dg = 1.0+c; }
                        else       { db = 1.0; dg = 1.0; dr = 1.0-c; }
                        dr *=a; dg *= a; db *= a;
                        
                        v = 0;
                        f = (int)(db*255.0);
                        v |= (f << 16);
                        f = (int)(dg*255.0);
                        v |= (f << 8);
                        f = (int)(dr*255.0);
                        v |= f;
                        
                        //if (matcol[i][j] != 0.5) System.err.println(matcol[i][j]+" = "+v);
                        
                    }
                    else v = 0; //0xFFFFFF;
                    outb[bufpos++] = v;
                }
            }
        }
    }
    
    // **********************************************************\
    // *                     DataFlow Interface                 *
    // **********************************************************/
    private synchronized void makeVectorBuffers()
    {
        // Make video channel buffers
        this.matr = new double[this.height][this.width];
        this.matg = new double[this.height][this.width];
        this.matb = new double[this.height][this.width];
    }
    
    private synchronized void clearVectorBuffers()
    {
    }
    
    public void setSourceDataModel(DataModel sourceDataModel) { this.sourceDataModel = sourceDataModel; }
    
    public void setSinkDataModel(DataModel sinkDataModel)
    {
        this.sinkDataModel = sinkDataModel;
        this.numChannels   = this.sinkDataModel.getAttributeCount();
    }
    
    public void setVideoSource(VideoSource source) { this.source = source; }
    public void setVideoSink(VideoSink sink)       { this.sink   = sink; }
    
    // **********************************************************\
    // *              Video Processing Effect                   *
    // **********************************************************/
    private int[] validateIntArraySize(Buffer buffer, int newSize)
    {
        Object objectArray;
        int  []typedArray;
        
        objectArray = buffer.getData();
        if (objectArray instanceof int[])
        { 
            typedArray = (int[])objectArray;
            if (typedArray.length >= newSize ) return typedArray;
        }
        typedArray = new int[newSize];
        buffer.setData(typedArray);
        
        return typedArray;
    }
    
    private void updateOutput(Buffer outputBuffer, Format format, int length, int offset)
    {
        outputBuffer.setFormat(format);
        outputBuffer.setLength(length);
        outputBuffer.setOffset(offset);
    }
    
    public int getWidth()  { return(this.width); }
    public int getHeight() { return(this.height); }

    public VideoEffect()
    {
        // JMF
        this.supportedRGB = new RGBFormat(null,  // size
				     Format.NOT_SPECIFIED,   // maxDataLength
				     int[].class,            // buffer type
				     Format.NOT_SPECIFIED,   // frame rate
				     32,                     // bitsPerPixel
				     MASK_R, MASK_G, MASK_B, // component masks
				     1,                      // pixel stride
				     Format.NOT_SPECIFIED,   // line stride
				     Format.FALSE,           // flipped
				     Format.NOT_SPECIFIED    // endian
				     );
	    this.supportedInputFormats  = new VideoFormat[1];
        this.supportedOutputFormats = new VideoFormat[1];
        this.supportedInputFormats[0]  = supportedRGB;
        this.supportedOutputFormats[0] = supportedRGB;
    }
    
    public void reset()
    {
    }
    
    public void open() throws ResourceUnavailableException
    {
        this.width  = (int)this.inputFormat.getSize().getWidth();
        this.height = (int)this.inputFormat.getSize().getHeight();
        makeVectorBuffers();
    }
    
    public void close()
    {
        clearVectorBuffers();
    }
    
    public Object[] getControls()
    {
        return (Object[]) new Control[0];
    }
    
    public Object getControl(String controlType)
    {
        try
        {
            Class  cls  = Class.forName(controlType);
            Object cs[] = getControls();
            for (int i = 0; i < cs.length; i++)
            {
                if (cls.isInstance(cs[i]))
                return cs[i];
            }
            return null;
        }
        catch (Exception e) { return null; }
    }
    
    public Format setInputFormat(Format input)
    {
        this.inputFormat = (VideoFormat)input;
        return (Format)this.inputFormat;
    }
    
    public Format setOutputFormat(Format output)
    {
        this.outputFormat = (VideoFormat)output;
        return (Format)this.outputFormat;
    }
    
    protected Format getInputFormat()  {  return this.inputFormat;  }
    protected Format getOutputFormat() {  return this.outputFormat; }
    
    public Format [] getSupportedInputFormats() { return this.supportedInputFormats; }
    public Format [] getSupportedOutputFormats(Format in)
    {
        if (! (in instanceof RGBFormat)) return new Format[0];

        RGBFormat ivf = (RGBFormat)in;
        if (!ivf.matches(this.supportedInputFormats[0])) return new Format[0];

        RGBFormat outf = new RGBFormat(ivf.getSize(),	      // size
				       Format.NOT_SPECIFIED,   // maxDataLength
				       int[].class,            // buffer type
				       Format.NOT_SPECIFIED,   // frame rate
				       ivf.getBitsPerPixel(),  // bitsPerPixel
				       MASK_R, MASK_G, MASK_B, // component masks
				       ivf.getPixelStride(),   // pixel stride
				       ivf.getLineStride(),    // line stride
				       ivf.getFlipped(),       // flipped
				       ivf.getEndian()         // endian
				     );

        return new Format[] {outf};
    }
    
    public String getName()  {  return this.effectName; }
}
