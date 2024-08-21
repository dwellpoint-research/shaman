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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.shaman.av.gui.resources.ResourceLoader;
import org.shaman.exceptions.ShamanException;
import org.shaman.util.gui.IconButton;
import org.shaman.util.gui.SeparatorPanel;


/**
 * Video Application Main Frame
 */

// **********************************************************\
// *              Video Application Main Frame              *
// **********************************************************/
public class VideoFrame extends JFrame
{
    public static final String TITLE = "Shaman Video";
    
    private VideoApp      app;
    private JPanel        videoPanel;
    
    // **********************************************************\
    // *                    Video Visualization                 *
    // **********************************************************/
    public JPanel getVideoPanel() { return(this.videoPanel); }
    
    // **********************************************************\
    // *                         GUI Code                       *
    // **********************************************************/
    private void buildLayout()
    {
        ResourceLoader rl;
        Container      contentPane;
        final VideoApp app;
        
        // Frame
        app = this.app;
        rl  = ResourceLoader.getInstance();
        setSize(640,580);
        setTitle(TITLE);
        contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.setBackground(Color.BLACK);
        
        // Control Panel
        // *************
        IconButton     butRecord;
        IconButton     butOpen;
        JSlider        slide;
        
        // Input from Recording device
        butRecord  = new IconButton(new AbstractAction("Camera", rl.getIcon("Import24.gif"))
        {
            public void actionPerformed(ActionEvent e)
            {
                try { app.openRecord(); }
                catch(ShamanException ex) { ex.printStackTrace(); }
            }
        });
        // Open file
        butOpen      = new IconButton(new AbstractAction("Open", rl.getIcon("Open24.gif"))
        {
            public void actionPerformed(ActionEvent e)
            {
                try { app.openVideoFile(); }
                catch(ShamanException ex) { ex.printStackTrace(); }
            }
        });
        
        // Make button bar
        Dimension d = butRecord.getPreferredSize();
        JPanel toolBar = new JPanel();
        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.X_AXIS));
        Color shadow = (Color) UIManager.get("controlShadow");
        Color highlight = (Color) UIManager.get("controlLtHighlight");
        toolBar.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, shadow),
                BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 1, 0, highlight),
                    BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, shadow),
                        BorderFactory.createEmptyBorder(0, 0, 0, 0)))));
        toolBar.add(butRecord);
        toolBar.add(new SeparatorPanel((int)d.getHeight()));
        toolBar.add(butOpen);
        toolBar.add(new SeparatorPanel((int)d.getHeight()));
        slide = new JSlider(0, 100);
        slide.setMaximumSize(new Dimension(200, 20));
        slide.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e)
            {
                JSlider s  = (JSlider)e.getSource();
                double val = (s.getValue()/100.0);
                app.setParameter(val);
            }
        });
        toolBar.add(slide);
        contentPane.add(toolBar, BorderLayout.NORTH);
        
        
        // Visualization Panel
        // *******************
        JPanel     visPanel;
        
        visPanel   = new JPanel();
        this.videoPanel = visPanel;
        visPanel.setLayout(new BorderLayout());
        visPanel.setBorder(BorderFactory.createEtchedBorder());
        visPanel.setMaximumSize(new Dimension(370, 305));
        visPanel.setMinimumSize(new Dimension(370, 305));
        visPanel.setPreferredSize(new Dimension(370, 305));
        contentPane.add(visPanel, BorderLayout.CENTER);
        
        // Close
        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                app.exit();
            }
        });
    }
    
    public VideoFrame(VideoApp vis)
    {
        this.app = vis;
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        buildLayout();
    }
}