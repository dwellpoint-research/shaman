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
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

public class SeparatorPanel extends JPanel {

    public SeparatorPanel(int height) {
        super();
   		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 1, 0, 0, Color.gray),
			BorderFactory.createMatteBorder(0, 0, 0, 1, Color.white)
		));
		
		setPreferredSize(new Dimension(2, height));
		setMinimumSize(new Dimension(2, height));
		setMaximumSize(new Dimension(2, height));
    }

}
