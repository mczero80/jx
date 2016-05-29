package jx.wm.plugin;

import jx.zero.*;
import jx.wm.*;
import jx.devices.fb.*;
import jx.formats.*;

import java.net.*;
import java.io.*;

/*
 * Use rawtoppm to convert the data to a usable image format 
*/
public class ScreenCapture {
    public static void main(String[] args) throws Exception {
	final String wmName = args[0];
	final String fbName = args[1];
	final String addr = args[2];
	final Naming naming = InitialNaming.getInitialNaming();
	final WindowManager wm = (WindowManager) LookupHelper.waitUntilPortalAvailable(naming, wmName);
	final FramebufferDevice fb = (FramebufferDevice) LookupHelper.waitUntilPortalAvailable(naming, fbName);
	wm.registerHotkeyPlugin(new HotkeyPlugin() {
		public void keyPressed() {
		    Debug.out.println("F1 key pressed");
		    DeviceMemory mem = fb.getFrameBuffer();
		    // save
		    Debug.out.println("Size: "+mem.size());
		    Debug.out.println("Width: "+fb.getWidth());
		    Debug.out.println("Height: "+fb.getHeight());
		    Debug.out.println("Color: "+fb.getColorSpace().getValue());
		    try {
			Socket socket = new Socket(addr, 6666);
			OutputStream out = socket.getOutputStream();
			InputStream in = socket.getInputStream();
			LittleEndianOutputStream lout = new LittleEndianOutputStream(out);
			lout.writeInt(fb.getWidth());
			lout.writeInt(fb.getHeight());
			lout.writeInt((mem.size()>>1) * 3);
			for(int i=0; i<mem.size() >> 1; i++) {
			    PixelColor p = PixelColor.fromRGB16 (mem.get16(i));
			    out.write(p.red()); 
			    out.write(p.green()); 
			    out.write(p.blue()); 
			}
			out.flush();
			socket.close();
		    } catch(Exception ex) {
			Debug.out.println("Exception during data transfer : "+ex);
			throw new Error();
		    }
		}
	    }, 2);
    }
}
