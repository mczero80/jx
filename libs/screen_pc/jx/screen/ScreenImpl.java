package jx.screen;

import jx.devices.Screen;
import jx.zero.*;
import jx.zero.debug.*;
import jx.zero.Ports;

public class ScreenImpl implements Screen, Service {
    final static int CGA_SCREEN  = 0xb8000;
    final static int MONO_SCREEN  = 0xb0000;
    //final static int SCREEN = MONO_SCREEN;
    final static int SCREEN = CGA_SCREEN;

    final static int CGAvideoPortReg = 0x3d4;
    final static int CGAvideoPortVal = 0x3d5;

    final static int MONOvideoPortReg = 0x3d4;
    final static int MONOvideoPortVal = 0x3d5;

    final static int videoPortReg = CGAvideoPortReg;
    final static int videoPortVal = CGAvideoPortVal;

    //final static int videoPortReg = MONOvideoPortReg;
    //final static int videoPortVal = MONOvideoPortVal;

    DeviceMemory video;
    int x,y;
    DebugPrintStream out;
    Ports ports;
    Naming naming;
    
    public static void init(Naming naming) {
	new ScreenImpl(naming);
    }
    public ScreenImpl(Naming naming) {
	this.naming = naming;
	DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	out = new DebugPrintStream(new DebugOutputStream(d));
	MemoryManager memMgr = (MemoryManager) naming.lookup("MemoryManager");
	ports = (Ports) naming.lookup("Ports");
	video = memMgr.allocDeviceMemory(SCREEN, 80 * 25 * 2);
	//clear();	
    }

    public int getWidth() { return 80; }
    public int getHeight() { return 25; }

    public void putAt(int x, int y, char c) {
	//out.println("putAt");
	if (x >= 80 || y >= 25 || x < 0 || y < 0) return;
	video.set8((80*y + x) * 2,  (byte)c);
	video.set8((80*y + x) * 2 + 1,  (byte)0x0f);
    }

    public void moveCursorTo (int x, int y) {
	int offset;
    
	offset = 80 * y + x;

	// high byte
	ports.outb (videoPortReg, (byte)0xe);
	ports.outb (videoPortVal, (byte)((offset >> 8) & 0xff)); //offset / 256

	// low byte
	ports.outb (videoPortReg, (byte)0xf);
	ports.outb (videoPortVal, (byte)(offset & 0xff)); // offset % 256
    } 

    public void clear() {
	video.fill16((short)0x0f00, 0, 80*24);
	x = y = 0;
    }

    public DeviceMemory getVideoMemory() {
	return video;
    }

}


