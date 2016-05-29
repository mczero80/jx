package jx.devices.fb;

import jx.devices.*;
import jx.devices.fb.ColorSpace;

public class FramebufferConfigurationTemplate extends DeviceConfigurationTemplate {
    public int xresolution;
    public int yresolution;
    public ColorSpace colorSpace;
    public FramebufferConfigurationTemplate(int xresolution, int yresolution, ColorSpace colorSpace) {
	this.xresolution = xresolution;
	this.yresolution = yresolution;
	this.colorSpace = colorSpace;
    }
}
