/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                     Technologies                      *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  $VER : ResourceLoader.java v1.0 (April 2003)         *
 *                                                       *
 *  by Johan Kaers                                       *
 *  Copyright (c) 2003 Shaman Research            *
\*********************************************************/
package org.shaman.resource;

import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

/**
 * <h2>Test Resource Loader<h2>
 * Provides access to test data-sets and other resources.
 */
public class ResourceLoader
{
    private static ResourceLoader instance = null;

    // Company Colors
    public Color blackColor     = new Color(0, 0, 0);
    public Color whiteColor     = new Color(255, 255, 255);
    public Color grayColor      = new Color(204, 205, 207);
    public Color blueColor      = new Color(0, 83, 134);
    public Color lightBlueColor = new Color(39, 122, 174);
    public Color yellowColor    = new Color(255, 204, 0);
    
    // Icon Buffer
    private Map iconMap;
    
    // **********************************************************\
    // *     Get the test-resource in the correct format        *
    // **********************************************************/
    public InputStream getInputStream(String resourcename)
    {
       return(ResourceLoader.class.getResourceAsStream(resourcename));
    }

    public ImageIcon getIcon(String name)
    {
        if (iconMap.containsKey(name))  return (ImageIcon) iconMap.get(name);

        ImageIcon icon = new ImageIcon(this.getClass().getResource(name));
        iconMap.put(name, icon);

        return icon;
    }

    public Image getImage(String name)
    {
      Image im = Toolkit.getDefaultToolkit().createImage(this.getClass().getResource(name));
      
      return(im);
    }
    
    // **********************************************************\
    // *             Singleton Test Resource Loader             *
    // **********************************************************/
    private ResourceLoader()
    {
        instance = this;
        iconMap = new HashMap();
    }

    public static ResourceLoader getInstance()
    {
        if (instance == null) instance = new ResourceLoader();

        return instance;
    }
}