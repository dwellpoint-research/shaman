package org.shaman.debug.resources;

import java.awt.Color;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

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
    private Locale locale;
    private MessageFormat messageFormat;

    private ResourceLoader() {
        instance = this;
        this.iconMap = new HashMap();
        this.resourceBundles = new HashMap();
        this.locale = Locale.getDefault();
        this.messageFormat = new MessageFormat("");
        this.messageFormat.setLocale(this.locale);
    }

    public static ResourceLoader getInstance() {
        if (instance == null)
            instance = new ResourceLoader();

        return instance;
    }

    public ImageIcon getIcon(String name) {
        if (iconMap.containsKey(name))
            return (ImageIcon) iconMap.get(name);

        ImageIcon icon = new ImageIcon(this.getClass().getResource(name));
        iconMap.put(name, icon);

        return icon;
    }

    public void setLocale(String language, String country) {
        this.locale = new Locale(language, country);
        this.messageFormat.setLocale(this.locale);
    }

    public String getString(String bundle, String key) {
        ResourceBundle rb = (ResourceBundle) this.resourceBundles.get(bundle);
        if (rb == null) {
            rb = ResourceBundle.getBundle("org.shaman.debug.resources." + bundle, this.locale);
            this.resourceBundles.put(bundle, rb);
        }
        return rb.getString(key);
    }

    public String formatString(String bundle, String key, Object[] values) {
        String pattern = getString(bundle, key);
        this.messageFormat.applyPattern(pattern);
        return this.messageFormat.format(values);
    }
}