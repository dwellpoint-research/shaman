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

import org.shaman.dataflow.Transformation;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;

import cern.colt.matrix.ObjectFactory1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Audio FFT</h2>
 * (Inverse) Fast Fourier Transform for of Audio Vectors
 */
public class AudioFFT extends Transformation
{ 
    // Forward or Inverse Fourier Transform
    private boolean forward;
    
    // **** Run-time Data ***
    private DataModel datamodel;
    private int       numChan;
    private double    max;
    
    // **********************************************************\
    // *           (Inverse) Fast Fourier Transform             *
    // **********************************************************/
    private void fft(ObjectMatrix1D vec) throws DataFlowException
    {
        double       []vecin, vecout;
        int            i;
        ObjectMatrix1D vecfft;
        
        // (Inverse)FFT for all the channels
        vecfft = ObjectFactory1D.dense.make(this.numChan);
        for (i=0; i<this.numChan; i++)
        {
            vecin = (double [])vec.getQuick(i);
            if (this.forward) vecout = doFFT(vecin);
            else              vecout = doIFFT(vecin);
            vecfft.setQuick(i, vecout);
        }
        
        if (vecfft != null) setOutput(0, vecfft);        
    }
    
    private double []doIFFT(double []vec)
    {
        double     u_r,u_i, w_r,w_i, t_r,t_i;
        int        ln, nv2, k, l, le, le1, j, ip, i, n;
        double [][]array;
        double   []vecout;
        
        // Move input vector to FFT buffer
        array = new double[vec.length/2][2];
        for (i=0; i<vec.length; i+=2)
        {
            array[i/2][0] = vec[i];
            array[i/2][1] = vec[i+1];
        }

        // Do Inverse FFT
        n   = array.length;
        nv2 = n / 2;
        j   = 1;
        for (i = 1; i < n; i++ )
        {
            if (i < j)
            {
                t_r = array[i - 1][0];
                t_i = array[i - 1][1];
                array[i - 1][0] = array[j - 1][0];
                array[i - 1][1] = array[j - 1][1];
                array[j - 1][0] = t_r;
                array[j - 1][1] = t_i;
            }
            k = nv2;
            while (k < j)
            {
                j = j - k;
                k = k / 2;
            }
            j = j + k;
        }

        ln  = (int)( Math.log( (double)n )/Math.log(2) + 0.5 );
        for (l = 1; l <= ln; l++)
        {   
            le = (int)(Math.exp( (double)l * Math.log(2) ) + 0.5 );
            le1 = le / 2;
            u_r = 1.0;
            u_i = 0.0;
            w_r =  Math.cos( Math.PI / (double)le1 );
            w_i =  Math.sin( Math.PI / (double)le1 );
            for (j = 1; j <= le1; j++)
            {
                for (i = j; i <= n; i += le)
                {
                    ip = i + le1;
                    t_r = array[ip - 1][0] * u_r - u_i * array[ip - 1][1];
                    t_i = array[ip - 1][1] * u_r + u_i * array[ip - 1][0];

                    array[ip - 1][0] = array[i - 1][0] - t_r;
                    array[ip - 1][1] = array[i - 1][1] - t_i; 

                    array[i - 1][0] =  array[i - 1][0] + t_r;
                    array[i - 1][1] =  array[i - 1][1] + t_i;  
                } 
                t_r = u_r * w_r - w_i * u_i;
                u_i = w_r * u_i + w_i * u_r;
                u_r = t_r;
            } 
        }
        
        // Adjust floating normalization factor
        for (i=0; i<array.length; i++)
        {
            if (Math.abs(array[i][0])>max) max = Math.abs(array[i][0]);
        }
        
        // Move IFFT buffer to Audio Vector. Ignore imaginary part.
        double val;
        
        vecout = new double[array.length];
        for (i=0; i<array.length; i++)
        {
            val       = array[i][0]/max;
            vecout[i] = val;
        }
          
        return vecout;
    }
    
    private double []doFFT(double []vec)
    {
        double [][]array;
        double     u_r,u_i, w_r,w_i, t_r,t_i;
        int        ln, nv2, k, l, le, le1, j, ip, i, n;
        double   []vecout;

        // Move audio vector to real part of FFT buffer. 
        array = new double[vec.length][2];
        for (i=0; i<array.length; i++)
        {
            array[i][0] = vec[i];
            array[i][1] = 0;
        }

        // Do FFT.
        n   = array.length;
        ln  = (int)( Math.log( (double)n )/Math.log(2) + 0.5 );
        nv2 = n / 2;
        j   = 1;
        for (i = 1; i < n; i++ )
        {
            if (i < j)
            {
                t_r = array[i - 1][0];
                t_i = array[i - 1][1];
                array[i - 1][0] = array[j - 1][0];
                array[i - 1][1] = array[j - 1][1];
                array[j - 1][0] = t_r;
                array[j - 1][1] = t_i;
            }
            k = nv2;
            while (k < j)
            {
                j = j - k;
                k = k / 2;
            }
            j = j + k;
        }

        for (l = 1; l <= ln; l++)
        {   
            le  = (int)(Math.exp( (double)l * Math.log(2) ) + 0.5 );
            le1 = le / 2;
            u_r = 1.0;
            u_i = 0.0;
            w_r =  Math.cos( Math.PI / (double)le1 );
            w_i = -Math.sin( Math.PI / (double)le1 );
            for (j = 1; j <= le1; j++)
            {
                for (i = j; i <= n; i += le)
                {
                    ip = i + le1;
                    t_r = array[ip - 1][0] * u_r - u_i * array[ip - 1][1];
                    t_i = array[ip - 1][1] * u_r + u_i * array[ip - 1][0];

                    array[ip - 1][0] = array[i - 1][0] - t_r;
                    array[ip - 1][1] = array[i - 1][1] - t_i; 

                    array[i - 1][0] =  array[i - 1][0] + t_r;
                    array[i - 1][1] =  array[i - 1][1] + t_i;  
                } 
                t_r = u_r * w_r - w_i * u_i;
                u_i = w_r * u_i + w_i * u_r;
                u_r = t_r;
            } 
        }
        
        // Interweave real and imaginary parts of the FFT in output vector
        vecout = new double[vec.length*2];
        for (i=0; i<array.length; i++)
        {
            vecout[i*2    ] = array[i][0];
            vecout[(i*2)+1] = array[i][1];
        }
        
        return(vecout);
    }
    
    public void transform() throws DataFlowException
    {
        Object in;
        while (this.areInputsAvailable(0, 1))
        {
            in = this.getInput(0);
            if (in != null) fft((ObjectMatrix1D)in);
        }
   }
    
    
    // **********************************************************\
    // *                 Parameter Configuration                *
    // **********************************************************/
    public void setForward(boolean forward)
    {
        this.forward = forward;
    }    
    
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
        
        // Get number of channels
        apa = (DataModelPropertyAudio)dmsup.getProperty(DataModelPropertyAudio.PROPERTY_AUDIO);
        this.numChan = apa.getChannels();
        
        // Set normalization factor
        this.max = 0;
    }
    
    public void cleanUp() throws DataFlowException
    {
        
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
         if   (port == 0) return("Audio FFT Output");
         else             return(null);
    }
     
    
    // **********************************************************\
    // *              Constructor/State Persistence             *
    // **********************************************************/
    public AudioFFT()
    {
       super();
       name        = "Audio FFT";
       description = "Fast Fourier Transform or Inverse Fourier Transform";
    }
}
