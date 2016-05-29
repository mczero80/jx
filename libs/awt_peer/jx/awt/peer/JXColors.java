package jx.awt.peer;

import java.awt.Color;
import java.util.*;
import jx.zero.*;
import jx.zero.memory.MemoryInputStream; 
import jx.bootrc.*;

/**
 * This class contains all colors used in the current
 * JX AWT scheme. It also provides a mechanism to load
 * color definitions from a file.
 */
public class JXColors {

    // standard style
    // This style is the default for the JX AWT. To replace it by
    // another scheme, simply assign other colors by calling
    // loadColorInformation() or by setting it manually.
    public static Color normalTextColor = Color.black;
    public static Color menuTextColor = Color.black;
    public static Color textTextColor = Color.black;
    public static Color disabledTextColor = Color.gray;
    public static Color normalBgColor = Color.lightGray;
    public static Color menuBgColor = Color.lightGray;
    public static Color menuSelTextColor = Color.white;
    public static Color menuSelBgColor = Color.blue;
    public static Color textBgColor = Color.white;
    public static Color disabledBgColor = Color.lightGray;
    public static Color arrowColor = Color.black;
    public static Color disabledArrowColor = Color.gray;
    public static Color caretColor = Color.black;
    public static Color hoverColor = Color.gray;
    public static Color focusColor = Color.black;
    public static Color checkboxBgColor = Color.white;
    public static Color checkboxFgColor = Color.black;


    /**
     * The JX color file name
     */
    private String colorFile = "jxcolors.ini";
    
    /**
     * Used by loadColorInformation() to access config file and parse
     * color values.
     */
    private Color parseColor(ConfigFile conf, String key) {
	String s = conf.get(key);
	//System.out.println("found value " + s + " for " + key);
	if (s != null) {
	    StringTokenizer st = new StringTokenizer(s, ",");
	    int r = Integer.parseInt(st.nextToken().trim());
	    int g = Integer.parseInt(st.nextToken().trim());
	    int b = Integer.parseInt(st.nextToken().trim());
	    //System.out.println(" new Color is " + new Color (r, g, b));
	    return new Color(r, g, b);
	}
	return null;
    }

    /**
     * Loads color information from the JX color file and resets the
     * internal color scheme table.
     */
    public void loadColorInformation() {
        BootFS bootFS = (BootFS) InitialNaming.getInitialNaming().lookup("BootFS");
        if (bootFS == null)
	    throw new Error("No BootFS found!");
        ReadOnlyMemory mem = bootFS.getFile(colorFile);
        if (mem == null)
	    throw new Error("File " + colorFile + " not found!");
	ConfigFile conf = new ConfigFile(mem);
	// now start loading color information
	normalTextColor = parseColor(conf, "normalTextColor");
	menuTextColor = parseColor(conf, "menuTextColor");
	textTextColor = parseColor(conf, "textTextColor");
	disabledTextColor = parseColor(conf, "disabledTextColor");
	normalBgColor = parseColor(conf, "normalBgColor");
	menuBgColor = parseColor(conf, "menuBgColor");
	menuSelTextColor = parseColor(conf, "menuSelTextColor");
	menuSelBgColor = parseColor(conf, "menuSelBgColor");
	textBgColor = parseColor(conf, "textBgColor");
	disabledBgColor = parseColor(conf, "disabledBgColor");
	arrowColor = parseColor(conf, "arrowColor");
	disabledArrowColor = parseColor(conf, "disabledArrowColor");
	caretColor = parseColor(conf, "caretColor");
	hoverColor = parseColor(conf, "hoverColor");
	focusColor = parseColor(conf, "focusColor");
	checkboxBgColor = parseColor(conf, "checkboxBgColor");
	checkboxFgColor = parseColor(conf, "checkboxFgColor");
    }
}
