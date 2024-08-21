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
package org.shaman.av.gui.resources;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

public class ResourceLoader {

    private static ResourceLoader instance = null;

    public Color blackColor = new Color(0, 0, 0);
    public Color whiteColor = new Color(255, 255, 255);
    public Color veryLightGrayColor = new Color(240, 240, 240);
    public Color lightGrayColor = new Color(224, 224, 224);
    public Color grayColor = new Color(204, 205, 207);
    public Color darkGrayColor = new Color(128, 128, 128);
    public Color blueColor = new Color(0, 83, 134);
    public Color lightBlueColor = new Color(39, 122, 174);
    public Color yellowColor = new Color(255, 204, 0);
    public Color lightYellowColor = new Color(255, 245, 204);

    private Map iconMap;
    private Map resourceBundles;

    private ResourceLoader() {
        instance = this;
        this.iconMap = new HashMap();
        this.resourceBundles = new HashMap();
    }

    public static ResourceLoader getInstance() {
        if (instance == null)
            instance = new ResourceLoader();

        return instance;
    }

    public ImageIcon getIcon(String name) {
        if (iconMap.containsKey(name))
            return (ImageIcon) iconMap.get(name);

        ImageIcon icon = new ImageIcon(name); // this.getClass().getResource(name));
        iconMap.put(name, icon);

        return icon;
    }
}