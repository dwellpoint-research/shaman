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
 * <h2>Audio Scope</h2>
 * Time domain audio visualization
 */

// **********************************************************\
// *            Audio Time-domain Visualization             *
// **********************************************************/
public class AudioScope extends JPanel implements VisualizerComponent
{
    private boolean         running;
    private AudioVisualizer avis;
    
    // **********************************************************\
    // *                   Audio Visualization                  *
    // **********************************************************/
    public void run()
    {
        double []curvec, audiovec;
        
        this.running = true;
        curvec       = null;
        while(this.running)
        {
            // Get the audio vector for the next moment in time.
            // When not there yet, wait until time catches up.
            curvec   = this.avis.getCurrentAudio(0);
            if (curvec != null)
            {
                // Draw scope on copy of the vector
                audiovec = new double[curvec.length];
                for (int i=0; i<audiovec.length; i++) audiovec[i] = curvec[i];
                drawAudio(audiovec);
            }
            else try{Thread.sleep(50); } catch (Exception ex) {}
        }
    }
    
    private void drawAudio(double []audio)
    {
        Graphics gr;
        int      swide, shigh, numSamples;
        double   sample;
        double   step, y, yprev;
        int      pos, x;
        
        swide      = getWidth();
        shigh      = getHeight();
        numSamples = audio.length;
        gr         = getGraphics();
        if ((gr != null) && (numSamples != 0))
        {
            // Clear Canvas
            gr.setColor(new Color(0,0,0));
            gr.fillRect(0,0,swide,shigh);

            // Draw Scope
            gr.setColor(new Color(180,50,150));
            step  = (double)numSamples / swide;
            yprev = shigh/2;
            for (x=1; x<swide; x++)
            {
                pos    = (((int)(step*x))/2)*2;
                sample = audio[pos];
                y      = (sample * (shigh/2)) + shigh/2;
                gr.drawLine(x-1,(int)yprev,x,(int)y);
                yprev = y;
            }
        }
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
    
    public AudioScope()
    {
        buildLayout();
    }
}