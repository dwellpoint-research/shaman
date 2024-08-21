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

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.shaman.util.gui.SeparatorPanel;

/**
 * <h2>Audio Spectrum</h2>
 * Frequency domain audio visualization
 */

// **********************************************************\
// *          Audio Frequency-domain Visualization          *
// **********************************************************/
public class AudioSpectrum extends JPanel implements VisualizerComponent
{
    private boolean         running;
    private AudioVisualizer avis;
    
    // **********************************************************\
    // *                   Audio Visualization                  *
    // **********************************************************/
    public void run()
    {
        double []curvec, audiovec;
        
        curvec       = null;
        this.running = true;
        while(this.running)
        {
            curvec   = this.avis.getCurrentAudio(0);
            if (curvec != null)
            {
                audiovec = new double[curvec.length];
                for (int i=0; i<audiovec.length; i++) audiovec[i] = curvec[i];
                drawSpectrum(audiovec);
            }
            else try{Thread.sleep(50); } catch (Exception ex) {}
        }
    }
    
    int xnow = 0;
    double vnorm = 20;
    
    private void drawSpectrum(double []audio)
    {
        final int wx = 2;
        final int wy = 2;
        Graphics gr;
        int      swide, shigh, vecsize;
        double   amp;
        int      x,y, vecpos;
        double   sr, si, pbeg, pend, p;
        float    colnow;
        
        swide   = getWidth();
        shigh   = getHeight();
        vecsize = audio.length;

        pbeg       = 0.75;
        pend       = 1.0;
        
        x          = 0;
        gr         = getGraphics();
        if ((gr != null) && (vecsize != 0))
        {
            x = this.xnow;
            if (xnow >= swide) x = 0;
            
            for(y=0; y<shigh; y+=wy)
            {
                p      = pbeg+((pend-pbeg)*((double)y/shigh));
                vecpos = (((int)(vecsize*p))/2)*2;
                if (vecpos == vecsize) vecpos -=2;
                
                sr  = audio[vecpos];
                si  = audio[vecpos+1];
                amp = Math.sqrt((sr*sr)+(si*si));
                
                amp   /= vnorm;
                if (amp > 1.00) amp = 1.0;
                colnow = 1.00f-(float)Math.pow(1-amp, 4);
                if (colnow <0) colnow = 0;
                if (colnow >1) colnow = 1;
                gr.setColor(new Color(colnow, colnow, colnow));
                gr.drawRect(x, y, wx, 1);
            }
            
            x += wx;
        }
        this.xnow = x;
    }
  
    public void setAudioVisualizer(AudioVisualizer avis) { this.avis = avis; }
    
    public void stop() { this.running = false; }
    
    public JComponent getVisualization() { return(this); }
  
    // **********************************************************\
    // *                       GUI Code                         *
    // **********************************************************/
    private void buildLayout()
    {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(new SeparatorPanel(300));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
    }
    
    public AudioSpectrum()
    {
        buildLayout();
    }
}