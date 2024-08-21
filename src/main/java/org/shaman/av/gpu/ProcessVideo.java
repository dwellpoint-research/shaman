package org.shaman.av.gpu;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;

import javax.swing.JComponent;
import javax.swing.JFrame;

import com.aparapi.Kernel;
import com.aparapi.Range;
import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.IMediaTool;
import com.xuggle.mediatool.IMediaViewer;
import com.xuggle.mediatool.MediaToolAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;

public class ProcessVideo
{


    int []imageRgb;            // RGB buffer of BufferedImage displayed in frame
    int []videoRGB;            // RGB input buffer containing video image
    int []outRGB;              // RGB output buffer containing processed video image
    Kernel []videoKernels;     // Video processing kernels converting input- to output-buffer

    JComponent viewer;
    int width, height;

    public static class VideoKernel extends Kernel
    {
        final float []CONVOLUTION_NONE   = new float[]{0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f};
        final float []CONVOLUTION_BLUR   = new float[]{1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f};
        final float []CONVOLUTION_EMBOSS = new float[]{-2f, -1f, 0f, -1f, 1f, 1f, 0f, 1f, 2f};

        final float[]SOBEL_VERTICAL   = new float[]{-1, 0, 1, -2, 0, 2, -1, 0, 1 };
        final float[]SOBEL_HORIZONTAL = new float[]{-1,-2,-1, 0, 0, 0 , 1, 2, 1 };

        final int []rgb;
        final int []outRGB;

        public VideoKernel(int []rgb, int []outRGB)
        {
            this.rgb    = rgb;
            this.outRGB = outRGB;
        }

        public void run()
        {
            //runGreyscale();
            runConvolution(CONVOLUTION_EMBOSS);
        }

        private void runConvolution(float []filter)
        {
            int x = getGlobalId(0);
            int y = getGlobalId(1);
            int w = getGlobalSize(0);
            int h = getGlobalSize(1);

            if (x > 1 && x < (w - 1) && y > 1 && y < (h - 1))
            {
                int result = 0;

                for (int rgbShift = 0; rgbShift < 24; rgbShift += 8)
                { 
                    int channelAccum = 0;
                    float accum = 0;

                    for (int count = 0; count < 9; count++)
                    {
                        int dx = (count % 3) - 1; // 0,1,2 -> -1,0,1
                        int dy = (count / 3) - 1; // 0,1,2 -> -1,0,1

                        int rgb = (this.rgb[((y + dy) * w) + (x + dx)]);
                        int channelValue = ((rgb >> rgbShift) & 0xff);
                        accum += filter[count];
                        channelAccum += channelValue * filter[count++];
                    }
                    channelAccum /= accum;
                    //channelAccum += offset;
                    channelAccum = max(0, min(channelAccum, 0xff));
                    result |= (channelAccum << rgbShift);
                }
                this.outRGB[y * w + x] = result;
            }
        }

        private void runGreyscale()
        {
            int x = getGlobalId(0);
            int y = getGlobalId(1);
            int w = getGlobalSize(0);
            int h = getGlobalSize(1);

            int pixel;
            int r,g,b;
            int grey, greypixel;

            // Convert to black and white video
            pixel = this.rgb[y*w+x];
            r     = (pixel & 0xFF0000) >> 16;
                    g     = (pixel & 0x00FF00) >> 8;
            b     = (pixel & 0x0000FF);
            grey  = (r+g+b)/3;
            greypixel = (grey<<16) | (grey<<8) | grey;
            this.outRGB[y*w+x] = greypixel;
        }
    }


    public void play(String filePath)
    {        
        IMediaReader reader;
        IMediaViewer viewer;
        IMediaTool   process;

        // Setup Xuggle to play / process / display the given video file
        reader = ToolFactory.makeReader(filePath);;
        viewer = ToolFactory.makeViewer();
        process = new ProcessTool();
        reader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
        reader.addListener(process);
        process.addListener(viewer);

        while (reader.readPacket() == null) do {} while(false);
    }

    public void initViewer()
    {
        // Display processed video
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.imageRgb = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        this.videoRGB = new int[image.getWidth()*image.getHeight()];
        this.outRGB = new int[image.getWidth()*image.getHeight()];
        JFrame frame = new JFrame("Video GPU");
        JComponent viewer = new JComponent()
        {
            public void paintComponent(Graphics g)
            {
                g.drawImage(image, 0, 0, width, height, this);
            }
        };
        viewer.setPreferredSize(new Dimension(width, height));
        frame.getContentPane().add(viewer);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        this.viewer = viewer;


        // Create the Kernel
        CannyKernels canny = new CannyKernels(this.videoRGB, this.outRGB, this.width, this.height);
        //this.videoKernels = new Kernel[]{new VideoKernel(videoRGB, outRGB)};

        this.videoKernels = new Kernel[]{
                new CannyKernels.Normalize(canny),
                new CannyKernels.Convolutions(canny),
                new CannyKernels.Gradients(canny),
                new CannyKernels.Magnitude(canny),
                new CannyKernels.Threshold(canny)
                };
    }

    public void displayVideo()
    {
        // Update displayed image with the output of the video processing
        System.arraycopy(outRGB, 0, imageRgb, 0, outRGB.length);
        viewer.repaint();
    }

    class ProcessTool extends MediaToolAdapter
    {
        public void onVideoPicture(IVideoPictureEvent event)
        {
            BufferedImage image;

            // Initialize frame first on the first video frame
            image = event.getImage();
            if (viewer == null)
            {
                width = image.getWidth();
                height = image.getHeight();
                initViewer();
            }

            // Extract the raw video byte-stream and convert to RGB integer 
            final byte[] rgb = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
            for(int i=0; i<rgb.length; i+=3)
            {
                videoRGB[i/3] = ((int)rgb[i]) + ((int)rgb[i+1] << 8) + ((int)rgb[i+2] << 16);
            }

            // Process Video
            Range videoRange = Range.create2D(width, height);
            for (int i=0; i<videoKernels.length; i++)
            {
                videoKernels[i].execute(videoRange);
            }

            // Display processed / original video.
            displayVideo();
            super.onVideoPicture(event);
        }
    }

    public static void main(String []args)
    {
        ProcessVideo process = new ProcessVideo();
        args = new String[]{"/Volumes/Public/Shared Videos/alien.mov"};
        process.play(args[0]);
    }
}
