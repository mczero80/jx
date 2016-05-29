package test.wintv.file;

import jx.zero.*;
import jx.zero.*;
import jx.zero.debug.*;
import jx.devices.pci.*;
import jx.devices.mga.*;
import jx.devices.fb.mga.*;
import jx.framebuffer.*;
import jx.wintv.*;
import test.wintv.file.TestNotifier;

import jx.devices.fb.ColorSpace;
import jx.devices.fb.FramebufferDevice;


import jx.bio.BlockIO;

class Main {
    static Naming naming = InitialNaming.getInitialNaming();

    public static void main(String[] args) {
	String framegrabberName = args[0];
	String framebufferName = args[1];
	String tvnormName = args[2];
	String testName = args[3];
	String controlName = args[4];
	String keyboardName = args[5];
	String tunerName = args[6];
	String freqName = args[7];
	String irReceiverName = args[8];
	String bioName = args[9];

	TVNorm tvnorm;

	// get framegrabber board
	CaptureDevice captureBoard = (CaptureDevice)LookupHelper.waitUntilPortalAvailable(naming, framegrabberName);

	FramebufferDevice fb = (FramebufferDevice)LookupHelper.waitUntilPortalAvailable(naming, framebufferName);
	ColorSpace s = fb.getColorSpace();
	int depth=0;
	if (s.getValue() == ColorSpace.CS_RGB16) {
	    depth=16;
	} else if (s.getValue() == ColorSpace.CS_RGB32) {
	    depth=32;
	} else {
	    throw new Error("Unsupported color space. Cannot use frame buffer.");
	}
	MemoryFramebuffer visframebuffer = new MemoryFramebuffer(fb.getFrameBuffer(), fb.getWidth(), fb.getHeight(), depth);

          
	PackedFramebuffer framebuffer = new MemoryFramebuffer(800, 600, 16);
	framebuffer.clear();
	framebuffer = new SubFramebuffer(framebuffer, 0, 0, framebuffer.width(), framebuffer.height()); // ???
      
	boolean useTuner;
	int frequency=0;
	if (tunerName.equals("tuner")) {
	    useTuner=true;
	    frequency = Integer.parseInt(freqName);
	} else if (tunerName.equals("chinch")) {
	    useTuner=false;
	} else {
	    throw new Error("Unknown capture input (tuner,chinch) : "+tunerName);
	}
	tvnorm = test.wintv.Main.initCaptureBoard(framebuffer, captureBoard, tvnormName, useTuner, frequency);


	BlockIO bio = (BlockIO) LookupHelper.waitUntilPortalAvailable(naming, bioName);

	captureBoard.setFramebufferOdd(framebuffer);
	Debug.out.println("Main: activating fields...");
	captureBoard.setOddFieldActive(true);
      
      	// misc video adjustments
	captureBoard.setVideoAdjustment(new GammaCorrectionRemovalAdjustment(false));
	captureBoard.setVideoAdjustment(new ChromaCombAdjustment(ChromaCombAdjustment.ODD,  false));
	captureBoard.setVideoAdjustment(new ChromaCombAdjustment(ChromaCombAdjustment.EVEN, false));

	// notifier
	TestNotifier notifier = new TestNotifier(framebuffer,bio,visframebuffer);
	captureBoard.addNotifierOdd(notifier);

	Debug.out.println("Main: capture...");
	captureBoard.captureOn();
    }

}
