package jx.devices.fb.emulation;


import jx.devices.DeviceFinder;
import jx.devices.Device;
import jx.zero.*;

import java.util.*;
import jx.zero.*;
import jx.zero.debug.*;
import jx.devices.*;
import jx.devices.fb.*;
import jx.wm.EventListener;
import jx.wm.Keycode;
import jx.wm.Qualifiers;
import jx.wm.WindowManager;

/**
 * Emulation FB Device finder
 * @author Michael Golm
 */
public class EmulFBFinder implements DeviceFinder {
    public Device[] find(String [] args) {
	FBEmulation fb = (FBEmulation)InitialNaming.getInitialNaming().lookup("FBEmulation");
	if (fb == null) return null;
	int mode = 0x0118;
	if (args.length == 1) mode = Integer.parseInt(args[0]);
	if (fb.open(mode) == false) return null;

	return new Device[] { new FBImpl(fb) };
    }
}


class FBImpl implements FramebufferDevice {
    FBEmulation fb;
    ColorSpace m_eColorSpace;
    
    FBImpl(FBEmulation fb)     {
	this.fb = fb;
	if (fb.getBitsPerPixel() == 32) {
	    m_eColorSpace = new ColorSpace (ColorSpace.CS_RGB32);
	} else {
	    m_eColorSpace = new ColorSpace (ColorSpace.CS_RGB16);
	}
    }
    public int getWidth() {return fb.getWidth();}
    public int getHeight(){return fb.getHeight();}
    public int setMode (int nWidth, int nHeight, ColorSpace eColorSpace){return 0;}
    public void startFrameBufferUpdate () {}
    public void endFrameBufferUpdate (){fb.update();}
    public void startUpdate (){}
    public void endUpdate (){fb.update();}
    public DeviceMemory getFrameBuffer (){return fb.getVideoMemory();}
    public int getFrameBufferOffset (){return 0;}
    public int getBytesPerLine (){return fb.getBytesPerLine();}
    public ColorSpace getColorSpace () {
	return m_eColorSpace;
    }
    public int drawLine (PixelRect cDraw, PixelRect cClipped, PixelColor cColor, DrawingMode nDrawingMode){return -1;}
    public int fillRect (PixelRect cRect[], int nCount, PixelColor cColor, DrawingMode nDrawingMode){return -1;}
    public int bitBlt (PixelRect acOldPos[], PixelRect acNewPos[], int nCount){return -1;}
    public void open(DeviceConfiguration conf){}
    public DeviceConfigurationTemplate[] getSupportedConfigurations (){
		return new DeviceConfigurationTemplate[] {
			new FramebufferConfigurationTemplate(fb.getWidth(), fb.getHeight(), m_eColorSpace)
			    };
    }
    public void close(){}
}


