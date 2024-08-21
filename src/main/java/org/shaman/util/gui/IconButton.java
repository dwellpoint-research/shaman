/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                   Utility Methods                     *
 *                                                       *
 \*********************************************************/
package org.shaman.util.gui;

import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.border.Border;

public class IconButton extends JButton {

	private Icon icon;
	private Icon grayIcon;
	
	private Border flatBorder;
	private Border raisedBorder;
	
	public IconButton(Icon icon) {
		this("", icon);
	}
	
    public IconButton(String text, Icon icon) {
        super(text, icon);
        this.flatBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1);
        this.raisedBorder = BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(1, 1, 0, 0, Color.white),
			BorderFactory.createMatteBorder(0, 0, 1, 1, Color.gray)
		);
        if (!text.equals(""))
	    {
	        setVerticalTextPosition(JButton.BOTTOM);
    	    setHorizontalTextPosition(JButton.CENTER);
        	setBorder(BorderFactory.createEmptyBorder(2, 4, 1, 4));
	    }
        setFocusPainted(false);

		GrayFilter filter = new GrayFilter(false, -200);

		ImageIcon i = (ImageIcon) this.getIcon();
        if (i != null)
        {
    		ImageIcon grayIcon = new ImageIcon();
    		ImageProducer prod = new FilteredImageSource(i.getImage().getSource(), filter);
    		Image grayImage = Toolkit.getDefaultToolkit().createImage(prod);
    		grayIcon.setImage(grayImage);
            this.grayIcon = grayIcon;
            setIcon(grayIcon);
            setRolloverIcon(icon);
        }
		
		setBorder(this.flatBorder);
		setOpaque(false);

		this.icon = icon;
		

    }

    public IconButton(Action a) {
        super(a);
        
        if (getText().equals("")) {
	        this.flatBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1);
	        this.raisedBorder = BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 1, 0, 0, Color.white),
				BorderFactory.createMatteBorder(0, 0, 1, 1, Color.gray)
			);
        }
        else {
			setVerticalTextPosition(JButton.BOTTOM);
	        setHorizontalTextPosition(JButton.CENTER);
	        this.flatBorder = BorderFactory.createEmptyBorder(2, 4, 1, 4);
	        this.raisedBorder = BorderFactory.createCompoundBorder(
				BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(1, 1, 0, 0, Color.white),
					BorderFactory.createMatteBorder(0, 0, 1, 1, Color.gray)
				),
				BorderFactory.createEmptyBorder(1, 3, 0, 3));
		}
        setFocusPainted(false);

		GrayFilter filter = new GrayFilter(false, -200);

		ImageIcon i = (ImageIcon) this.getIcon();
		ImageIcon grayIcon = new ImageIcon();
		ImageProducer prod = new FilteredImageSource(i.getImage().getSource(), filter);
		Image grayImage = Toolkit.getDefaultToolkit().createImage(prod);
		grayIcon.setImage(grayImage);
		
		setIcon(grayIcon);
		setRolloverIcon(i);
		setBorder(this.flatBorder);		
		setOpaque(false);
				
		this.icon = i;
		this.grayIcon = grayIcon;
       
    }
    
    public void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);        
    	if (e.getID() == MouseEvent.MOUSE_ENTERED)
			this.setBorder(this.raisedBorder);
		else if (e.getID() == MouseEvent.MOUSE_EXITED) {
			this.setBorder(this.flatBorder);
        }
    }
    
    protected void processFocusEvent(FocusEvent e) {
        super.processFocusEvent(e);
        if (e.getID() == FocusEvent.FOCUS_LOST) {
            this.getModel().setRollover(false);
            this.setBorder(this.flatBorder);
        }
    }
    
    
}
