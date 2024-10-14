package org.shaman.av.gpu;

import java.util.Arrays;

import com.aparapi.Kernel;
import com.aparapi.Range;

public class CannyKernels
{
    final float GAUSSIAN_CUT_OFF = 0.005f;


    float kernelRadius;
    float lowThreshold;
    float highThreshold;
    int kernelWidth;
    boolean contrastNormalized;
    int width, height, picsize;

    final int []rgb;
    final int []outRGB;
    final int []data;
    final int []histogram;
    final int []remap;
    final float []kernel;
    final float []diffKernel;

    final int[]   magnitude;
    final float []orientation;

    final float[] xConv;
    final float[] yConv;
    final float[] xGradient;
    final float[] yGradient;

    public CannyKernels(int []rgb, int []outRGB, int width, int height)
    {
        this.rgb = rgb;
        this.outRGB = outRGB;

        this.contrastNormalized = false;

        this.lowThreshold = 2.5f;
        this.highThreshold = 7.5f;
        this.kernelRadius = 8f; //2f;
        this.kernelWidth = 8; //16;

        //generate the gaussian convolution masks
        this.kernel = new float[kernelWidth];
        this.diffKernel = new float[kernelWidth];
        int kwidth;
        for (kwidth = 0; kwidth < kernelWidth; kwidth++)
        {
            float g1 = gaussian(kwidth, kernelRadius);
            if (g1 <= GAUSSIAN_CUT_OFF && kwidth >= 2) break;
            float g2 = gaussian(kwidth - 0.5f, kernelRadius);
            float g3 = gaussian(kwidth + 0.5f, kernelRadius);
            kernel[kwidth] = (g1 + g2 + g3) / 3f / (2f * (float) Math.PI * kernelRadius * kernelRadius);
            diffKernel[kwidth] = g3 - g2;
        }

        this.width = width;
        this.height = height;
        this.picsize = width*height;

        this.data = new int[picsize];
        this.histogram = new int[256];
        this.remap = new int[256];
        this.magnitude = new int[picsize];
        this.orientation = new float[picsize];

        this.xConv = new float[picsize];
        this.yConv = new float[picsize];
        this.xGradient = new float[picsize];
        this.yGradient = new float[picsize];
    }

    private float gaussian(float x, float sigma)
    {
        return (float)Math.exp(-(x * x) / (2f * sigma * sigma));
    }

    static class Normalize extends Kernel
    {
        final int []rgb;
        final int []outRGB;
        final int []data;
        final int []histogram;
        final int []remap;

        public Normalize(CannyKernels canny)
        {
            this.rgb = canny.rgb;
            this.outRGB = canny.outRGB;
            this.data = canny.data;
            this.histogram = canny.histogram;
            this.remap = canny.remap;
        }

        public void run()
        {
            int x = getGlobalId(0);
            int y = getGlobalId(1);
            int w = getGlobalSize(0);
            int h = getGlobalSize(1);

            int i;
            int pixel, r, g, b, lum;

            // Convert color to luminance
            i     = y*w+x;
            pixel = rgb[i];
            r     = (pixel & 0xFF0000) >> 16;
            g     = (pixel & 0x00FF00) >> 8;
            b     = (pixel & 0x0000FF);
            lum = round(0.299f * r + 0.587f * g + 0.114f * b);
            data[i] = lum;
            
            // contribute to histogram counter
            histogram[lum]++;
            
            outRGB[i] = (lum << 16) | (lum << 8) | lum;
        }
        
        public Kernel execute(Range range)
        {
            // clear histogram first
            Arrays.fill(data, 0);

            // execute kernel
            super.execute(range);

            // calculate luminance remapping for next kernel
            int picsize = 0;
            for (int i=0; i<this.histogram.length; i++) picsize += histogram[i];
            
            int sum = 0;
            int j = 0;
            for (int i = 0; i < histogram.length; i++)
            {
                sum += histogram[i];
                int target = (sum*255)/picsize;
                for (int k = j+1; (k <=target) && (k<256) && (k>0); k++)
                {
                    remap[k] = i;
                }
                j = target;
            }
            
            // disable limunance remapping
            for(int i=0; i<this.remap.length; i++) this.remap[i] = i;

            return this;
        }
    }

    static class Convolutions extends Kernel
    {
        final int[] data;
        final int []outRGB;
        final float[]xConv;
        final float[]yConv;
        final float[]convKernel;
        final int []remap;

        final int kernelWidth;

        public Convolutions(CannyKernels canny)
        {
            this.data = canny.data;
            this.remap = canny.remap;
            this.outRGB = canny.outRGB;
            this.xConv = canny.xConv;
            this.yConv = canny.yConv;

            this.convKernel = canny.kernel;
            this.kernelWidth = canny.kernelWidth;
        }

        public void run()
        {
            int x = getGlobalId(0);
            int y = getGlobalId(1);
            int w = getGlobalSize(0);
            int h = getGlobalSize(1);
            int index = y*w+x;

            if (x >= this.kernelWidth && x <= (w-this.kernelWidth) && y >= this.kernelWidth && (y<=h-this.kernelWidth))
            {
                // normalize luminance
                this.data[index] = this.remap[this.data[index]];
                
                //perform convolution in x and y directions
                float sumX = this.data[index] * this.convKernel[0];
                float sumY = sumX;
                int xOffset = 1;
                int yOffset = w;
                for(; xOffset < this.kernelWidth; )
                {
                    sumY += this.convKernel[xOffset] * (this.data[index - yOffset] + this.data[index + yOffset]);
                    sumX += this.convKernel[xOffset] * (this.data[index - xOffset] + this.data[index + xOffset]);
                    yOffset += w;
                    xOffset++;
                }
                this.yConv[index] = sumY;
                this.xConv[index] = sumX;
                
                
                int lum = this.data[index];
                this.outRGB[index] = (lum << 16) | (lum << 8) | lum;
            }
            else
            {
                this.yConv[index] = 0;
                this.xConv[index] = 0;
            }
        }
    }

    static class Gradients extends Kernel
    {
        final float[]xConv;
        final float[]yConv;
        final float[]xGradient;
        final float[]yGradient;

        final float []diffKernel;
        final int   kernelWidth;
        final float kernelRadius;

        public Gradients(CannyKernels canny)
        {
            this.xConv = canny.xConv;
            this.yConv = canny.yConv;
            this.xGradient = canny.xGradient;
            this.yGradient = canny.yGradient;

            this.kernelWidth  = canny.kernelWidth;
            this.kernelRadius = canny.kernelRadius;
            this.diffKernel = canny.diffKernel;
        }


        public void run()
        {
            int x = getGlobalId(0);
            int y = getGlobalId(1);
            int w = getGlobalSize(0);
            int h = getGlobalSize(1);
            int index = y*w+x;


            float sumx = 0;
            float sumy = 0;
            if (x >= this.kernelWidth && x <= (w-this.kernelWidth) && y >= this.kernelWidth && (y<=h-this.kernelWidth))
            {
                sumx = 0;
                for (int i = 1; i < this.kernelWidth; i++)
                {
                    sumx += this.diffKernel[i] * (this.yConv[index - i] - this.yConv[index + i]);
                }

                sumy = 0;
                int yOffset = w;
                for (int i = 1; i < this.kernelWidth; i++)
                {
                    sumy += this.diffKernel[i] * (this.xConv[index - yOffset] - this.xConv[index + yOffset]);
                    yOffset += w;
                }
            }

            this.xGradient[index] = sumx;
            this.yGradient[index] = sumy;
        }
    }

    static class Magnitude extends Kernel
    {
        final float MAGNITUDE_SCALE = 100F;
        final float MAGNITUDE_LIMIT = 1000F;
        final int MAGNITUDE_MAX = (int) (MAGNITUDE_SCALE * MAGNITUDE_LIMIT);

        final float[]xGradient;
        final float[]yGradient;
        final int[] magnitude;
        final float[]orientation;

        final int kernelWidth;

        public Magnitude(CannyKernels canny)
        {
            this.kernelWidth  = canny.kernelWidth;
            this.xGradient = canny.xGradient;
            this.yGradient = canny.yGradient;
            this.magnitude = canny.magnitude;
            this.orientation = canny.orientation;
        }

        public void run()
        {
            int x = getGlobalId(0);
            int y = getGlobalId(1);
            int w = getGlobalSize(0);
            int h = getGlobalSize(1);
            int index = y*w+x;

            if (x >= this.kernelWidth && x <= (w-this.kernelWidth) && y >= this.kernelWidth && (y<=h-this.kernelWidth))
            {
                int indexN = index - w;
                int indexS = index + w;
                int indexW = index - 1;
                int indexE = index + 1;
                int indexNW = indexN - 1;
                int indexNE = indexN + 1;
                int indexSW = indexS - 1;
                int indexSE = indexS + 1;

                float xGrad = xGradient[index];
                float yGrad = yGradient[index];
                float gradMag = hypot(xGrad, yGrad);

                //perform non-maximal supression
                float nMag = hypot(xGradient[indexN], yGradient[indexN]);
                float sMag = hypot(xGradient[indexS], yGradient[indexS]);
                float wMag = hypot(xGradient[indexW], yGradient[indexW]);
                float eMag = hypot(xGradient[indexE], yGradient[indexE]);
                float neMag = hypot(xGradient[indexNE], yGradient[indexNE]);
                float seMag = hypot(xGradient[indexSE], yGradient[indexSE]);
                float swMag = hypot(xGradient[indexSW], yGradient[indexSW]);
                float nwMag = hypot(xGradient[indexNW], yGradient[indexNW]);
                float tmp;
                boolean maximal = true;
                if (maximal)
                {
                    this.magnitude[index] = gradMag >= MAGNITUDE_LIMIT ? MAGNITUDE_MAX : (int) (MAGNITUDE_SCALE * gradMag);
                    this.orientation[index] = atan2(this.yGradient[index], this.xGradient[index]);
                }
                else
                {
                    magnitude[index] = 0;
                }
            }
        }

        public float hypot(float x, float y)
        {
            return abs(x) + abs(y);
            //return (float) Math.hypot(x, y);
        }
    }

    static class Threshold extends Kernel
    {
        final float PI = (float)Math.PI;
        final int []outRGB;
        final float []orientation;
        final int []magnitude;

        final int []ORIENTATION_COLORS = new int[]
                {
                0x008800, 0x00FF00, 0x00FF88, 0x00FFFF, 0x88FFFF, 0xFFFFFF
                };

        public Threshold(CannyKernels canny)
        {
            this.outRGB = canny.outRGB;
            this.orientation = canny.orientation;
            this.magnitude = canny.magnitude;
        }

        public void run()
        {
            int x = getGlobalId(0);
            int y = getGlobalId(1);
            int w = getGlobalSize(0);
            int h = getGlobalSize(1);
            int index = y*w+x;

            float mag, ori;
            int   m;


            //mag = magnitude[index] / 256.0f;
            //mag = mag*mag;
            //m   = (int)(mag*256);

            m = magnitude[index];
            
            
            //if (m > 20) m = 255;
            //if (m < 20) m = 0;
         
            /*
            m = magnitude[index];
            if (m>255) m = 255;*/
            
            outRGB[index] = m << 16 | m << 8 | m;
        

            /*
            if (m > 32)
            {
                ori = round(((orientation[index] + PI) / 2*PI)*6);
                outRGB[index] = ORIENTATION_COLORS[(int)ori];
            }
            else outRGB[index] = 0;
         */
            
            /*
            mag = magnitude[index]*2;
            if (mag > 255) mag = 255;
            if (mag < 64) mag = 0;
             */

            /*
            ori = round(((orientation[index] + PI) / 2*PI)*5);
            outRGB[index] = ORIENTATION_COLORS[(int)ori];
             */

        }
    }
}
