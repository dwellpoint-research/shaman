package org.shaman.spatial;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import javax.swing.JComponent;
import javax.swing.JFrame;

import com.aparapi.Kernel;
import com.aparapi.Range;

public class SpatialEnsembleLearningGPU
{
    /* Period parameters */  
    private static final int N=624;
    private static final int M=397;
    private static final int MATRIX_A=0x9908b0df;   /* constant vector a */
    private static final int UPPER_MASK=0x80000000; /* most significant w-r bits */
    private static final int LOWER_MASK=0x7fffffff; /* least significant r bits */

    /* for tempering */   
    private static final int TEMPERING_MASK_B=0x9d2c5680;
    private static final int TEMPERING_MASK_C=0xefc60000;
      
    private static final int mag0 = 0x0;
    private static final int mag1 = MATRIX_A;
    //private static final int[] mag01=new int[] {0x0, MATRIX_A};
    /* mag01[x] = x * MATRIX_A  for x=0,1 */

    public static final int DEFAULT_SEED = 4357;
    
    public static class RandomKernel extends Kernel
    {
        private final int []rgb;
        private int[] mt;

        public RandomKernel(int[] _rgb, int []_mt)
        {
            this.rgb = _rgb;
            this.mt = _mt;
        }

        @Override
        public void run()
        {
            int x, y;

            x = getGlobalId(0);
            y = getGlobalId(1);
            int gid = getGlobalId(1) * getGlobalSize(0) + getGlobalId(0);

            nextBlock();
            
            
            if (mt[gid % N] > 0) rgb[gid] = 0x000000;
            else               rgb[gid] = 0xFFFFFF;
        }
        
        public void nextRandom()
        {
            
        }

        public void nextBlock()
        {
            int y;

            int kk;

            for (kk=0;kk<N-M;kk++) {
                y = (mt[kk]&UPPER_MASK)|(mt[kk+1]&LOWER_MASK);
                mt[kk] = mt[kk+M] ^ (y >>> 1) ^ ((y & 0x1) == 0 ? mag0 : mag1);
            }
            for (;kk<N-1;kk++) {
                y = (mt[kk]&UPPER_MASK)|(mt[kk+1]&LOWER_MASK);
                mt[kk] = mt[kk+(M-N)] ^ (y >>> 1) ^ ((y & 0x1) == 0 ? mag0 : mag1);
            }
            y = (mt[N-1]&UPPER_MASK)|(mt[0]&LOWER_MASK);
            mt[N-1] = mt[M-1] ^ (y >>> 1) ^ ((y & 0x1) == 0 ? mag0 : mag1);
        }

        public void setScaleAndOffset(float _scale, float _offsetx, float _offsety)
        {

        }
    }
    
    private static int []makeRandomSeed()
    {
        int seed;
        int []mt;

        seed = (int)System.currentTimeMillis();

        mt = new int[N];
        mt[0] = seed & 0xffffffff;
        for (int i = 1; i < N; i++)
        {
            mt[i] = (1812433253 * (mt[i-1] ^ (mt[i-1] >> 30)) + i); 
            mt[i] &= 0xffffffff;
        }

        return mt;
    }


    public static class MandelKernel extends Kernel{

        /** RGB buffer used to store the Mandelbrot image. This buffer holds (width * height) RGB values. */
        final private int rgb[];

        /** Palette used for each iteration value 0..maxIterations. */
        final private int pallette[];

        /** Maximum iterations we will check for. */
        final private int maxIterations;

        /** Mutable values of scale, offsetx and offsety so that we can modify the zoom level and position of a view. */
        private float scale = .0f;

        private float offsetx = .0f;

        private float offsety = .0f;

        /**
         * Initialize the Kernel.
         *  
         * @param _width Mandelbrot image width
         * @param _height Mandelbrot image height
         * @param _rgb Mandelbrot image RGB buffer
         * @param _pallette Mandelbrot image palette
         */
        public MandelKernel(int[] _rgb, int[] _pallette) {

            rgb = _rgb;
            pallette = _pallette;
            maxIterations = pallette.length - 1;

        }

        @Override public void run() {

            /** Determine which RGB value we are going to process (0..RGB.length). */
            int gid = getGlobalId(1) * getGlobalSize(0) + getGlobalId(0);

            /** Translate the gid into an x an y value. */
            float x = (((getGlobalId(0) * scale) - ((scale / 2) * getGlobalSize(0))) / getGlobalSize(0)) + offsetx;

            float y = (((getGlobalId(1) * scale) - ((scale / 2) * getGlobalSize(1))) / getGlobalSize(1)) + offsety;

            int count = 0;

            float zx = x;
            float zy = y;
            float new_zx = 0f;

            // Iterate until the algorithm converges or until maxIterations are reached.
            while (count < maxIterations && zx * zx + zy * zy < 8) {
                new_zx = zx * zx - zy * zy + x;
                zy = 2 * zx * zy + y;
                zx = new_zx;
                count++;
            }

            // Pull the value out of the palette for this iteration count.
            rgb[gid] = pallette[count];
        }

        public void setScaleAndOffset(float _scale, float _offsetx, float _offsety) {
            offsetx = _offsetx;
            offsety = _offsety;
            scale = _scale;
        }

    }
    

    /** User selected zoom-in point on the Mandelbrot view. */
    public static volatile Point to = null;

    @SuppressWarnings("serial") public static void main(String[] _args) {

        JFrame frame = new JFrame("MandelBrot");

        /** Mandelbrot image height. */
        final Range range = Range.create2D(768, 768);
        System.out.println("range= " + range);

        /** Maximum iterations for Mandelbrot. */
        final int maxIterations = 256;

        /** Palette which maps iteration values to RGB values. */
        final int pallette[] = new int[maxIterations + 1];

        //Initialize palette values
        for (int i = 0; i < maxIterations; i++) {
            float h = i / (float) maxIterations;
            float b = 1.0f - h * h;
            pallette[i] = Color.HSBtoRGB(h, 1f, b);
        }

        /** Image for Mandelbrot view. */
        final BufferedImage image = new BufferedImage(range.getGlobalSize(0), range.getGlobalSize(1), BufferedImage.TYPE_INT_RGB);
        final BufferedImage offscreen = new BufferedImage(range.getGlobalSize(0), range.getGlobalSize(1), BufferedImage.TYPE_INT_RGB);
        // Draw Mandelbrot image
        JComponent viewer = new JComponent(){
            @Override public void paintComponent(Graphics g) {

                g.drawImage(image, 0, 0, range.getGlobalSize(0), range.getGlobalSize(1), this);
            }
        };

        // Set the size of JComponent which displays Mandelbrot image
        viewer.setPreferredSize(new Dimension(range.getGlobalSize(0), range.getGlobalSize(1)));

        final Object doorBell = new Object();

        // Mouse listener which reads the user clicked zoom-in point on the Mandelbrot view 
        viewer.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e) {
                to = e.getPoint();
                synchronized (doorBell) {
                    doorBell.notify();
                }
            }
        });

        // Swing housework to create the frame
        frame.getContentPane().add(viewer);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Extract the underlying RGB buffer from the image.
        // Pass this to the kernel so it operates directly on the RGB buffer of the image
        final int[] rgb = ((DataBufferInt) offscreen.getRaster().getDataBuffer()).getData();
        final int[] imageRgb = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        // Create a Kernel passing the size, RGB buffer and the palette.
        final MandelKernel kernel = new MandelKernel(rgb, pallette);
        
        //final int []mt = makeRandomSeed();
        //final RandomKernel kernel = new RandomKernel(rgb, mt);

        float defaultScale = 3f;

        // Set the default scale and offset, execute the kernel and force a repaint of the viewer.
        kernel.setScaleAndOffset(defaultScale, -1f, 0f);
        kernel.execute(range);
        System.arraycopy(rgb, 0, imageRgb, 0, rgb.length);
        viewer.repaint();

        // Report target execution mode: GPU or JTP (Java Thread Pool).
        System.out.println("Execution mode=" + kernel.getExecutionMode());

        // Window listener to dispose Kernel resources on user exit.
        frame.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent _windowEvent) {
                kernel.dispose();
                System.exit(0);
            }
        });

        // Wait until the user selects a zoom-in point on the Mandelbrot view.
        while (true) {

            // Wait for the user to click somewhere
            while (to == null) {
                synchronized (doorBell) {
                    try {
                        doorBell.wait();
                    } catch (InterruptedException ie) {
                        ie.getStackTrace();
                    }
                }
            }

            float x = -1f;
            float y = 0f;
            float scale = defaultScale;
            float tox = (float) (to.x - range.getGlobalSize(0) / 2) / range.getGlobalSize(0) * scale;
            float toy = (float) (to.y - range.getGlobalSize(1) / 2) / range.getGlobalSize(1) * scale;

            // This is how many frames we will display as we zoom in and out.
            int frames = 128;
            long startMillis = System.currentTimeMillis();
            for (int sign = -1; sign < 2; sign += 2) {
                for (int i = 0; i < frames - 4; i++) {
                    scale = scale + sign * defaultScale / frames;
                    x = x - sign * (tox / frames);
                    y = y - sign * (toy / frames);

                    // Set the scale and offset, execute the kernel and force a repaint of the viewer.
                    kernel.setScaleAndOffset(scale, x, y);
                    kernel.execute(range);
                    System.arraycopy(rgb, 0, imageRgb, 0, rgb.length);
                    viewer.repaint();
                }
            }

            long elapsedMillis = System.currentTimeMillis() - startMillis;
            System.out.println("FPS = " + frames * 1000 / elapsedMillis);

            // Reset zoom-in point.
            to = null;

        }

    }

}