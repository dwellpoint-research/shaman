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
package org.shaman.av.gui;

import javax.swing.JComponent;

/**
 * <h2>Visualization Component</h2>
 */

// **********************************************************\
// *    Audio/Video Visualization Component Interface       *
// **********************************************************/
public interface VisualizerComponent extends Runnable
{
    public void setAudioVisualizer(AudioVisualizer vis);
    
    public void stop();
    
    public JComponent getVisualization();
}