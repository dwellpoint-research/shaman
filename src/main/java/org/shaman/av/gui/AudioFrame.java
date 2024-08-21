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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.shaman.av.gui.resources.ResourceLoader;
import org.shaman.exceptions.ShamanException;
import org.shaman.util.gui.IconButton;
import org.shaman.util.gui.SeparatorPanel;


/**
 * Audio Application Main Frame
 */

// **********************************************************\
// *              Audio Application Main Frame              *
// **********************************************************/
public class AudioFrame extends JFrame
{
    public static final String TITLE = "Shaman Audio";
    
    private AudioApp      app;
    private AudioScope    scopePanel;
    private AudioSpectrum spectrumPanel;
  
    // **********************************************************\
    // *              Audio Spectrum Visualization              *
    // **********************************************************/
    public AudioScope    getScope()    { return(this.scopePanel); }
    public AudioSpectrum getSpectrum() { return(this.spectrumPanel); }
  
    // **********************************************************\
    // *                         GUI Code                       *
    // **********************************************************/
    private void buildLayout()
    {
        ResourceLoader rl;
        Container      contentPane;
        final AudioApp       app;
        
        // Frame
        app = this.app;
        rl  = ResourceLoader.getInstance();
        setSize(800,700);
        setTitle(TITLE);
        contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.setBackground(Color.BLACK);
        
        
        // Control Panel
        // *************
        IconButton     butRecord;
        IconButton     butOpen;
        IconButton     butPause;
        IconButton     butImage;
        IconButton     butStartSave;
        IconButton     butStopSave;
        final JTextField     txt;
        
        // Input from Recording device
        butRecord  = new IconButton(new AbstractAction("Record", rl.getIcon("Import24.gif"))
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
                try { app.openAudioFile(); }
                catch(ShamanException ex) { ex.printStackTrace(); }
            }
        });
        // Toggle pause
        butPause    = new IconButton(new AbstractAction("Pause", rl.getIcon("Pause24.gif"))
        {
            public void actionPerformed(ActionEvent e)
            {
                try { app.togglePause(); }
                catch(ShamanException ex) { ex.printStackTrace(); }
            }
        });        
        // Embed image
        butImage   = new IconButton(new AbstractAction("Embed", rl.getIcon("Movie24.gif"))
        {
            public void actionPerformed(ActionEvent e)
            {
                try { app.embedImage(); }
                catch(ShamanException ex) { ex.printStackTrace(); }
            }
        });
        // Start saving to file
        butStartSave   = new IconButton(new AbstractAction("Save to File", rl.getIcon("Export24.gif"))
        {
            public void actionPerformed(ActionEvent e)
            {
               app.startSaving();
            }
        });
        // Start saving to file
        butStopSave   = new IconButton(new AbstractAction("Stop Saving", rl.getIcon("Stop24.gif"))
        {
            public void actionPerformed(ActionEvent e)
            {
                app.stopSaving();
            }
        });
        
        Dimension d;
        d = new Dimension(150,20);
        txt = new JTextField(20);
        txt.setPreferredSize(d);
        txt.setMaximumSize(d);
        
        // Make button bar
        d = butRecord.getPreferredSize();
        butRecord.setPreferredSize(d);
        butOpen.setPreferredSize(d);
        butPause.setPreferredSize(d);
        butImage.setPreferredSize(d);
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
        toolBar.add(butPause);
        toolBar.add(new SeparatorPanel((int)d.getHeight()));
        toolBar.add(butImage);
        toolBar.add(new SeparatorPanel((int)d.getHeight()));
        toolBar.add(txt);
        toolBar.add(new SeparatorPanel((int)d.getHeight()));
        toolBar.add(butStartSave);
        toolBar.add(new SeparatorPanel((int)d.getHeight()));
        toolBar.add(butStopSave);
        toolBar.add(new SeparatorPanel((int)d.getHeight()));
        contentPane.add(toolBar, BorderLayout.NORTH);
        
        // Visualization Panel
        // *******************
        JPanel     visPanel;
        
        visPanel   = new JPanel();
        visPanel.setLayout(new BorderLayout());
        this.scopePanel = new AudioScope();
        visPanel.add(this.scopePanel, BorderLayout.NORTH);
        this.spectrumPanel = new AudioSpectrum();
        visPanel.add(this.spectrumPanel, BorderLayout.CENTER);
        contentPane.add(visPanel, BorderLayout.CENTER);
        
        // Change embedding text when character typed in the text field
        KeyAdapter ka = new KeyAdapter()
        {
            public void keyTyped(KeyEvent e)
            { 
                app.setEmbedText(txt.getText());
            }
        };
        txt.setFocusable(true);
        txt.addKeyListener(ka);
        
        // Close
        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                app.exit();
            }
        });
    }
    
    public AudioFrame(AudioApp vis)
    {
        this.app = vis;
        enableEvents(AWTEvent.WINDOW_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        buildLayout();
    }
}