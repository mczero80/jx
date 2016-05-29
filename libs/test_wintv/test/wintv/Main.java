package test.wintv;

import jx.zero.*;
import jx.zero.debug.*;
import jx.devices.pci.*;
import jx.devices.mga.*;
import jx.devices.fb.mga.*;
import jx.framebuffer.*;
import jx.wintv.*;
import jx.devices.Keyboard;

import jx.devices.fb.ColorSpace;
import jx.devices.fb.FramebufferDevice;



public class Main {
    static Naming naming = InitialNaming.getInitialNaming();
    final static boolean useDevice 	= true;
    final static boolean useIrqWatchBuffer	= false;

    // flags for testSimpleVideo
    final static boolean useInterlacedFramebuffer= false;
    final static boolean useRelocation		= false;
    final static boolean useLinkedFramebuffer	= true;
    final static boolean perturbateFramebuffers	= true;		// only with useLinkedFramebuffer
    final static boolean useRemoteControl	= true;
    final static int     useClipping		= -1;
   
   

    public static TVNorm initCaptureBoard(PackedFramebuffer framebuffer, CaptureDevice captureBoard, String tvnormName, boolean useTuner, int frequency) {
	// show IRQs on top of screen
	if( useIrqWatchBuffer ){
	    PackedFramebuffer irqWatchBuffer = new SubFramebuffer(framebuffer,
								  0, 0, 
								  framebuffer.width(), 8);
	    captureBoard.setIrqWatchBuffer(irqWatchBuffer);
	    //framebuffer = new SubFramebuffer(framebuffer, 0, 8, framebuffer.width(), framebuffer.height()-8);
	}

	
	// select video source
	Debug.out.println("Main: select video source...");
	if( useTuner ){
	    Tuner tuner = captureBoard.getTuner();
	    if( tuner != null ){
		//int f = 647250000  ;       // C43 (Prog 1)
		int f = frequency ;
		Debug.out.println("******* using tuner with " + f + " Hz *******");
		tuner.setFrequ(f);
		tuner.activate();
	    }
	    else {
		Debug.out.println("***** No Tuner! Using Chinch input! *****");
		captureBoard.getChinch().activate();
	    }
	}
	else {
	    Debug.out.println("selecting Chinch...");
	    captureBoard.getChinch().activate(); 
	}
      
	// set Video norm
	TVNorm tvnorm;
	if (tvnormName.equals("PAL")) {
	    tvnorm = TVNorms.pal;
	} else if (tvnormName.equals("NTSC")) {
	    tvnorm = TVNorms.ntsc;
	} else {
	    throw new Error("Unknown TV norm (PAL or NTSC): "+tvnormName);
	}
	captureBoard.setNorm(tvnorm);


	// Note: InputSource is already selected
	Debug.out.println("Main: testing video signal...");
	captureBoard.printIsVideoSignalStable(1000);

	return tvnorm;
    }

    /* Test Entry Point */
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

	TVNorm tvnorm;

	// get framegrabber board
	CaptureDevice captureBoard = (CaptureDevice)LookupHelper.waitUntilPortalAvailable(naming, framegrabberName);
          
	PackedFramebuffer framebuffer;

	if (useDevice) {
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
	    framebuffer = new MemoryFramebuffer(fb.getFrameBuffer(), fb.getWidth(), fb.getHeight(), depth);
	} else{
	    framebuffer = new MemoryFramebuffer(800, 600, 16);
	}
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
	tvnorm = initCaptureBoard(framebuffer, captureBoard, tvnormName, useTuner, frequency);

      
	if(testName.equals("VerySimpleVideoTest") )
	    testVerySimpleVideo(captureBoard, tvnorm, framebuffer);
	else if(testName.equals("SimpleVideoTest") )
	    testSimpleVideo(captureBoard, tvnorm, framebuffer);
	else if(testName.equals("VideoTransferTest") )
	    testVideoTransfer(captureBoard, tvnorm, framebuffer);
	else if(testName.equals("PuzzleTest") ){
	    PuzzleInputDevice input = null;
	    if (controlName.equals("NOCTRL")){
		input = null;
	    } else {
		IRReceiver ir = (IRReceiver)naming.lookup(irReceiverName);
		if (controlName.equals("IR") && ir != null ){
		    Debug.out.println("found IRReceiver; using it as input device");
		    input = new PuzzleIRInput(ir);
		} else {
		    Debug.out.println("Using keyboard as input device");
		    Keyboard keyboard = (Keyboard)naming.lookup(keyboardName);
		    input = new PuzzleKeybdInput(keyboard);
		}
	    }

	    Puzzle puzzle = new Puzzle(captureBoard, tvnorm, framebuffer);
	    puzzle.permutate();
	    while(true){
		puzzle.autoplay(input);
		puzzle.play(input);
	    }
	} else { 
	    throw new Error("Unknown Test: "+testName);
	}
	
	
    }
    
    /********************************************************************/
   
    static void testVerySimpleVideo(CaptureDevice captureBoard, TVNorm tvnorm, PackedFramebuffer framebuffer){
	Debug.out.println("Main: prepare framebuffers for capture...");
	captureBoard.setFramebufferOdd(framebuffer);
	Debug.out.println("Main: activating fields...");
	captureBoard.setOddFieldActive(true);
      
      
	// misc video adjustments
	captureBoard.setVideoAdjustment(new GammaCorrectionRemovalAdjustment(false));
	captureBoard.setVideoAdjustment(new ChromaCombAdjustment(ChromaCombAdjustment.ODD,  false));
	captureBoard.setVideoAdjustment(new ChromaCombAdjustment(ChromaCombAdjustment.EVEN, false));

	// notifier
	TestNotifier notifier = new TestNotifier(framebuffer);
	captureBoard.addNotifierOdd(notifier);

      
	Debug.out.println("Main: capture...");
	captureBoard.captureOn();
      
      	Debug.out.println("wait 5 seconds");
	MySleep.msleep(5000);
		
    }

    /********************************************************************/
   
    static void testSimpleVideo(CaptureDevice captureBoard, TVNorm tvnorm, PackedFramebuffer framebuffer){
      
	PackedFramebuffer notifierBuffer  = new SubFramebuffer(framebuffer, 
							       0, framebuffer.height()-8, 
							       framebuffer.width(), 8 );
	framebuffer = new SubFramebuffer(framebuffer, 
					 0, 0, 
					 framebuffer.width(), framebuffer.height()-8);
      
	// divide into separate areas for odd and even field
	PackedFramebuffer framebufferOdd  = null;
	PackedFramebuffer framebufferEven = null;
      
	if( useInterlacedFramebuffer ){
	    framebufferOdd  = new InterlacedFramebuffer(framebuffer, 0);
	    framebufferEven = new InterlacedFramebuffer(framebuffer, 1);
	}
	else {
	    framebufferOdd  = new SubFramebuffer(framebuffer,
						 0, 0,
						 framebuffer.width()/2, framebuffer.height()/2);
	    framebufferEven = new SubFramebuffer(framebuffer,
						 0, framebuffer.height()/2,
						 framebuffer.width()/2, framebuffer.height()/2);
	}
      
	Debug.out.println("test.wintv.Main: found a " + captureBoard.getBoardName() + " device");
      
	if( useRemoteControl ){
	    WinTVRemote wintvRemote = null;
	    if( captureBoard.getBoardName().equals("WinTV") ){
		IRReceiver ir = (IRReceiver)naming.lookup("WinTV-IRReceiver");
		if( ir != null ){
		    wintvRemote = new WinTVRemote(captureBoard, ir, 
						  framebufferOdd, framebufferEven);
		    new Thread(wintvRemote).start();
		}
	    }
	}
      
	// Note: InputSource is already selected
	Debug.out.println("Main: testing video signal...");
	captureBoard.printIsVideoSignalStable(1000);
      
	Debug.out.println("Main: setup framebuffers for fields...");
	PackedFramebuffer normbufferOdd   = null;
	PackedFramebuffer normbufferEven  = null;
      
	if( framebufferOdd != null )
	    normbufferOdd =  new SubFramebuffer(framebufferOdd, 0, 0, 
						Math.min(tvnorm.width, framebufferOdd.width()),
						Math.min(tvnorm.height/2, framebufferOdd.height()));
	if( framebufferEven != null ){
	    if( useLinkedFramebuffer )
		normbufferEven = calcLinkedFramebuffer(framebufferEven);
	    else
		normbufferEven =  new SubFramebuffer(framebufferEven, 0, 0, 
						     Math.min(tvnorm.width, framebufferEven.width()),
						     Math.min(tvnorm.height/2, framebufferEven.height()));
	}
      
	Debug.out.println("normufferOdd  : "+normbufferOdd);
	Debug.out.println("normbufferEven: "+normbufferEven);
      
	Debug.out.println("Main: setup geometry...");
	FramebufferScaler scaler = new FramebufferScaler(captureBoard);
	Debug.out.println("odd framebuffer : " + normbufferOdd);
	scaler.setScalingOdd(tvnorm, normbufferOdd, useInterlacedFramebuffer);
	if( normbufferEven != null ){
	    Debug.out.println("even framebuffer: " + normbufferEven);
	    scaler.setScalingEven(tvnorm, normbufferEven, useInterlacedFramebuffer);
	}
      
	Debug.out.println("Main: prepare framebuffers for capture...");
	captureBoard.setFramebufferOdd(normbufferOdd);
	captureBoard.setFramebufferEven(normbufferEven);
      
	if( useClipping > 0 ){
	    ClippingRectangle clippings[] = getClippings(framebuffer, useClipping);
	 
	    Debug.out.println("Main: translate clippings for odd framebuffer...");
	    ClippingRectangle codd[] = clippings;
	    if( framebufferOdd instanceof ClippingTransformer )
		codd = ((ClippingTransformer)framebufferOdd).transformClippings(codd);
	    if( normbufferOdd instanceof ClippingTransformer )
		codd = ((ClippingTransformer)normbufferOdd).transformClippings(codd);
	    captureBoard.setClippingOdd(codd);
	 
	    Debug.out.println("Main: translate clippings for even framebuffer...");
	    ClippingRectangle ceven[] = clippings;
	    if( framebufferEven instanceof ClippingTransformer )
		ceven = ((ClippingTransformer)framebufferEven).transformClippings(ceven);
	    if( normbufferEven instanceof ClippingTransformer )
		ceven = ((ClippingTransformer)normbufferEven).transformClippings(ceven);
	    captureBoard.setClippingEven(ceven);
	}
      
	PackedFramebuffer relocbuffer = null;
	if( normbufferEven instanceof LinkedPackedFramebuffer ){
	    LinkedPackedFramebuffer l = (LinkedPackedFramebuffer)normbufferEven;
	    Debug.out.println("**** LinkedPackedFramebuffer ****: "+ l);
	    relocbuffer = l.getFramebuffer(l.getNHoriz()-1, l.getNVert()-1);
	}
	else {
	    Debug.out.println("**** not LinkedPackedFramebuffer ****");
	    relocbuffer = normbufferEven;
	}
      
	if( useRelocation ){
	    Debug.out.println("Main: mark framebuffers as relocatable...");
	    captureBoard.markFramebufferRelocateable(relocbuffer, true);
	}
      
	Debug.out.println("Main: activating fields...");
	captureBoard.setFieldsActive(true, true);
      
      
	// misc video adjustments
	captureBoard.setVideoAdjustment(new GammaCorrectionRemovalAdjustment(false));
	captureBoard.setVideoAdjustment(new ChromaCombAdjustment(ChromaCombAdjustment.ODD,  false));
	captureBoard.setVideoAdjustment(new ChromaCombAdjustment(ChromaCombAdjustment.EVEN, false));
      
	Debug.out.println("Main: capture...");
	captureBoard.captureOn();
      
      
	/*
	  TestNotifier notifier = new VideoTestNotifier(notifierBuffer, captureBoard, 
	  relocbuffer, 200, 2);
      
	  captureBoard.addNotifierEven(notifier.getDepHandle());
	*/
      
	// now perform some testings
	// MySleep.msleep(5*1000);
	// board.dumpControlBlock(0x120);
      
	// captureBoard.startPollIntStat(3000);
      
	Debug.out.println("wait 5 seconds");
	MySleep.msleep(5000);
	
	if( !useLinkedFramebuffer ){
	    int nOff = 50;
	    int delta = 4;
	    int base = relocbuffer.startAddress();
	    int end = base + nOff * delta;
	    int current = base;
	    
	    Debug.out.println("enter relocation loop");
	    while(true){
		current += delta;
		if( current >= end )
		    current = base;
		
		captureBoard.relocateFramebufferEven(null, current);
		captureBoard.setEvenFieldActive(true);
		MySleep.msleep(500);
	    }
	}
	else {
	    LinkedPackedFramebuffer linked = (LinkedPackedFramebuffer)normbufferEven;
	    
	    // do nothing
	}
	
    }
   
    /********************************************************************/

    static void searchChannels(CaptureDevice captureBoard, TunerChannel channels[]){
	Debug.out.println("Searching TV-channels:");
	Tuner tuner = captureBoard.getTuner();
	for(int i=0; i<channels.length; ++i){
	    Debug.out.println("New channel: "+channels[i]);
	    tuner.setFrequ(channels[i].getFrequ());
	    tuner.activate();
	    MySleep.msleep(2*1000);
	}
    }
   
    /********************************************************************/

    static ClippingRectangle[] getClippings(PackedFramebuffer framebuffer, int clippingType){
	int MAXX = framebuffer.width()-1;
	int MAXY = framebuffer.height()-1;
      
	ClippingRectangle clippings[] = null;
      
	if( clippingType == 0 ){		/* Simple Clippings */
	    clippings = new ClippingRectangle[]{
	    };
	}
	else if( clippingType == 1 ){	/* Complex Clippings */
	    final int ex = MAXX / 40;
	    final int ey = MAXY / 25;
	 
	    clippings = new ClippingRectangle[] {
		/* two intersecting rectangles */
		new ClippingRectangle(  1*ex,  5*ey,  11*ex,  9*ey ),
		new ClippingRectangle(  6*ex,  7*ey,  16*ex,  11*ey ),
	    
		/* two 'side by side' rectangles */
		new ClippingRectangle(  1*ex,   1*ey,  3*ex-1,    2*ey ),
		new ClippingRectangle(  3*ex,   1*ey,    6*ex,    2*ey ),
	    
		/* a "standalone" rectangle */
		new ClippingRectangle( 25*ex, 10*ey, 35*ex, 19*ey),
	    
		/* "on the edge" rectangles */
		new ClippingRectangle(           0,       17*ey,  5*ex,  21*ey ),	/* left edge */
		new ClippingRectangle( MAXX - 5*ex,        8*ey,  MAXX,  10*ey ),	/* right edge  */
		new ClippingRectangle( MAXX - 5*ex, MAXY - 4*ey,  MAXX,     MAXY)	/* lower right edge */
	    };
	}
	else if( clippingType == 2 ){	/* Extreme Clippings */
	    final int RX = 10;		/* number of rects horizontaly */
	    final int RY = 10;		/* number of rects verticaly */
	 
	    clippings = new ClippingRectangle[RX*RY];
	 
	    final int BX = MAXX/RX;		/* width of the "box" around each rect */
	    final int BY = MAXY/RY;		/* height */
      
	    for(int y=0; y<RY; ++y){
		for(int x=0; x<RX; ++x){
		    clippings[y*RX+x] = 
			new ClippingRectangle(BX*x + BX/2, BY*y + BY/2, 
					      BX*x + BX  , BY*y + BY);
		}
	    }
	}
	else 
	    throw new NotImpl("unknown clipping type");
      
	return clippings;
    }
   
    /********************************************************************/
   
    static LinkedPackedFramebuffer calcLinkedFramebuffer(PackedFramebuffer framebuffer){
	final int nx = 4;
	final int ny = 4;
      
	final int dx = (framebuffer.width()  -2*(nx-1)) / nx;
	final int dy = (framebuffer.height() -2*(ny-1))/ ny;
      
      
	// Debug.out.println("size   framebuffer: "+framebuffer);
	LinkedPackedFramebuffer linkedfb = new LinkedPackedFramebuffer(nx, ny);
      
	// generate subframebuffers
	PackedFramebuffer framebuffers[] = new PackedFramebuffer[nx*ny];
	for(int x=0; x<nx; ++x){
	    for(int y=0; y<ny; ++y){
		PackedFramebuffer subfb= new SubFramebuffer(framebuffer,
							    x * (dx +2),      y * (dy +2),
							    dx, dy);
		Debug.out.println( "("+x+", "+y+
				   "): new " + subfb +
				   " @ "+PFBTools.calcX(framebuffer, subfb)+"+"+PFBTools.calcY(framebuffer, subfb)
				   );
		framebuffers[y*nx + x] = subfb;
	    }
	}
      
	if( perturbateFramebuffers ){
	    // perturbate subframebuffers 
	    final int n = 2*nx*ny;
	    PackedFramebuffer fb;
	    int r1, r2;
	    //java.util.Random rg = new java.util.Random();
	    MyRandom rg = new MyRandom();
	    for(int i=0; i<n; ++i){
		r1 = Math.abs(rg.nextInt() % framebuffers.length);
		r2 = Math.abs(rg.nextInt() % framebuffers.length);
	    
		// Debug.out.println(r1 + "<->" +r2);
		// swap framebuffers
		fb = framebuffers[r1];
		framebuffers[r1] = framebuffers[r2];
		framebuffers[r2] = fb;
	    }
	}
      
	// install SubFramebuffers in LinkedPackedFramebuffer
	for(int x=0; x<nx; ++x)
	    for(int y=0; y<ny; ++y)
		linkedfb.setFramebuffer(x, y, framebuffers[y*nx + x]);
      
	return linkedfb;
    }
   

    /********************************************************************/
   
   
    /********************************************************************/
   
    static void testVideoTransfer(CaptureDevice captureBoard, TVNorm tvnorm, PackedFramebuffer framebuffer){
	LocalSender.init(framebuffer);
      
	LocalReceiver.main( new SubFramebuffer(framebuffer, 
					       framebuffer.width()/2, 0,
					       framebuffer.width()/2, framebuffer.height()/2));
	Debug.out.println("WHAT?!?");
	throw new Error();
    }
}

/********************************************************************/

class VideoTestNotifier extends TestNotifier {
    CaptureDevice captureBoard;
    PackedFramebuffer relocbuffer;
    int nOff, delta;
    int base;
    int current;
   
    VideoTestNotifier(PackedFramebuffer notifierBuffer, CaptureDevice captureBoard, PackedFramebuffer relocbuffer, int nOff, int delta){
	super(notifierBuffer);
	this.captureBoard = captureBoard;
	this.nOff = nOff;
	this.delta = delta;
      
	current = base = relocbuffer.startAddress();
    }

    public void notifyEvent(){
	super.notifyEvent();
      
	current += delta;
	if( current > base + nOff * delta )
	    current = base;
      
	// Debug.out.println("relocateFramebufferEven " + Hex.toHexString(current) + " = base + " + Hex.toHexString(current-base) );
	captureBoard.relocateFramebufferEven(null, current);
	captureBoard.setEvenFieldActive(true);
    }
}
