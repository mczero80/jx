package jx.devices.fb.mga;

import jx.zero.*;
import jx.zero.debug.*;

import jx.devices.pci.*;
import jx.devices.*;
import jx.framebuffer.PackedFramebuffer;
import jx.devices.fb.*;

import jx.wintv.ZeroDomain;		// tools 
import jx.wintv.Hex;			// tools
import jx.wintv.MySleep;		// tools

import jx.devices.mga.*;

public class MGA200Impl implements MGA200, MGA200Defines, Service {
    static Naming naming = InitialNaming.getInitialNaming();
   PCIDevice pcidevice;
   CapabilityAGP agp = null;
   
   DeviceMemory registers;		/* aka MGA Control Aperture (MGABASE1) */
   DeviceMemory fbufMem;		/* MGABASE2 */
   
   MGA200Framebuffer framebuffer;
   
    ColorSpace colorSpace = new ColorSpace(ColorSpace.CS_RGB16); // the only driver supported color space 

    public DeviceConfigurationTemplate[] getSupportedConfigurations () { 
	return new DeviceConfigurationTemplate[] {
	    new FramebufferConfigurationTemplate(800, 600, colorSpace),
	};
    }

    public void open(DeviceConfiguration conf) {
	FramebufferConfiguration c = (FramebufferConfiguration) conf;
	framebuffer.open();
    }
    public void close() {}
   
    public void setForegroundColor(PixelColor color) { framebuffer.foreground = ((MGAColor)color).value;}

    public void setBackgroundColor(PixelColor color) { framebuffer.background = ((MGAColor)color).value; }

    public PixelColor getRGBColor(int red, int green, int blue) { 
	return new MGAColor();
    }

    public void drawRect(int x, int y, int w, int h) {
	framebuffer.drawRect(x,y,w,h);
    }

    public void drawPoint(int x, int y) {
	framebuffer.drawPoint(x,y);
    }
    public void drawLine(int x, int y, int x1, int y1) {
	throw new Error();
    }
    
    	public int getWidth() 
	{ 
		return framebuffer.width(); 
	}
    	public int getHeight()
	{ 
		return framebuffer.height(); 
	}
    	public ColorSpace getColorSpace()
	{ 
		return colorSpace; 
	}    
	public int setMode (int nWidth, int nHeight, ColorSpace eColorSpace)
	{
		if (nWidth != 800 || nHeight != 600 || eColorSpace.getValue() != ColorSpace.CS_RGB16)
			return -1;
		return 0;			
	}
	public void startFrameBufferUpdate ()
	{
	}
	public void endFrameBufferUpdate ()
	{
	}
	public void startUpdate ()
	{
	}
	public void endUpdate ()
	{
	}
	public DeviceMemory getFrameBuffer ()
	{
		return framebuffer.devMemObj();
	}
	public int getFrameBufferOffset ()
	{
		return 0;
	}
	public int getBytesPerLine ()
	{
		int nBytesPerLine = framebuffer.width();
		if (framebuffer.depth() == 16)
			nBytesPerLine *= 2;
		if (framebuffer.depth() == 15)
			nBytesPerLine *= 2;			
		return nBytesPerLine;
	}
	public int drawLine (PixelRect cDraw, PixelRect cClipped, PixelColor cColor, DrawingMode nDrawingMode)
	{
		return -1;
	}
	public int fillRect (PixelRect cRect[], int nCount, PixelColor cColor, DrawingMode nMode)
	{
		return -1;
/*		
		if (nMode.getValue() != DrawingMode.DM_COPY)
			return -1;
		Debug.out.println ("TEST!! MGA200::fillRect: " + cColor + "," + Integer.toHexString ((int)cColor.toRGB16()));
		for (int i = 0; i < nCount; i++)
		{
		    framebuffer.drawRect(cRect[i].left (), cRect[i].top (), cRect[i].width () + 1, cRect[i].height () + 1);
		}			
		
		return 0;
*/		
	}
	public int bitBlt (PixelRect acOldPos[], PixelRect acNewPos[], int nCount)
	{
		return -1;
	}


   /* PCI stuff */
   int readConfig(int adr){
      return pcidevice.readConfig(adr/4);
   }
   void writeConfig(int adr, int value){
      pcidevice.writeConfig(adr/4, value);
   }
   
   
   /* local Register Stuff */
   int readRegister(int adr){
      return registers.get32(adr/4);
   }
   void writeRegister(int adr, int value){
      registers.set32(adr/4, value);
   }
   byte readRegister8(int adr){
      return registers.get8(adr);
   }
   void writeRegister8(int adr, byte value){
      registers.set8(adr, value);
   }
   
   
   /* VGA stuff:
    * register selector at address "base", data register at "base+1" 
    */
   byte readVGA(int base, int index){
      writeRegister8(base, (byte)index);	/* select register */
      return readRegister8(base+1);
   }
   void writeVGA(int base, int index, int value){
      writeRegister8(base, (byte)index);	/* select register */
      writeRegister8(base+1, (byte)value);
   }
   
   
   /* DAC X_DATAREG stuff */
   byte readDAC8(byte index){
      writeRegister8(PALWTADD, index);   /* select register */
      return readRegister8(X_DATAREG);
   }
   void writeDAC8(byte index, int value){
      writeRegister8(PALWTADD, index);	/* select register */
      writeRegister8(X_DATAREG, (byte)value);
   }
   
   /********************************************************************/
   
   /**
    * Initialize the the graphic board and registrate it as a portal.
    */
   public static void main(String [] args){
      PCIAccess pcibus = (PCIAccess)LookupHelper.waitUntilPortalAvailable(naming, "PCIAccess");
      MGA200Impl mga = findDevice(pcibus);
      Debug.assert(mga != null, "found no MGA200 PCI device");
      naming.registerPortal(mga, "MGA200");
   }
   
   /********************************************************************/
   
   /**
    * Search a MGA200 graphic board on the given PCI bus.
    */
   public static MGA200Impl findDevice(PCIAccess pcibus){
      MGA200Impl mga = null;
      PCIDevice dev;
      int devc = pcibus.getNumberOfDevices();
      for(int devindex=0; devindex<devc; ++devindex){
	 dev = pcibus.getDeviceAt(devindex);
	 
	 int id = dev.readConfig(PCI.REG_DEVVEND);
	 if(id != 0x0520102b && 
	    id != 0x0521102b )
	   continue;
	 
	 mga = new MGA200Impl(dev);
	 break;
      }
      return mga;
   }

   /********************************************************************/
   
   /** 
    * Initializes the graphic board and switch to SVGA mode 114.
    */
   public MGA200Impl(PCIDevice pcidevice){
      this.pcidevice = pcidevice;
      int val;
      int loops;
      
      // check if device is a MGA200
      val = readConfig(DEVID);
      if(val != 0x0520102b && 
	 val != 0x0521102b )
	throw new Error("MGA200: Device is not a MGA200");
      
      agp = (CapabilityAGP)pcidevice.getCapability(PCICap.ID_AGP);
      if( agp != null )
	Debug.out.println("found AGP capability");
      
      // initialize DEVCTRL
      writeConfig(DEVCTRL, 0x00000002);	/* want memspace */
      
      
      // get Memory Mapped regions
      MemoryManager memMgr = (MemoryManager)ZeroDomain.lookup("MemoryManager");
      if( memMgr == null )
	throw new Error("MGA200: no MemoryManager found");
      
      val = readConfig(MGABASE1) & 0xfffffff0;
      registers = memMgr.allocDeviceMemory(val, 0x3fff);   /* 16 KB */
      if( registers == null )
	throw new Error("MGA200: cannot allocate local registers");
      
      val = readConfig(MGABASE2) & 0xfffffff0;
      fbufMem = memMgr.allocDeviceMemory(val, 0xffffff);   /* 16MB */
      if( fbufMem == null )
	throw new Error("MGA200: cannot allocate framebuffer");
      
      // AGP setup (wild guessing)
      if( agp != null ){
	 Debug.out.println("AGP_REV =0x"+Hex.toHexString(agp.getRevision()) +
			   " AGP_STS=0x"+Hex.toHexString(agp.getStatus()) +
			   " AGP_CMD=0x"+Hex.toHexString(agp.getCommand()) +
			   " AGP_PLL="+readRegister(AGP_PLL));
	 
	 if( readRegister(AGP_PLL) == 0x0 ){
	    Debug.out.println("Fixing AGP_PLL...");
	    writeRegister(AGP_PLL, 0x1);	/* FIXME: seltsamerweise ist
						 * in AGP_CMD "x2" eigestellt,
						 * ohne dass AGP_PLL aktiviert
						 * ist. */
	 }
	 
	 if(agp.getRevision()  != 0x10       ||
	    agp.getStatus()    != 0x1f000203 ||
	    agp.getCommand()   != 0x1f000302 ||
	    readRegister(AGP_PLL) != 1 )
	   throw new Error("MGA200 AGP support seems not no be initialized!");
      }
      
      
      // cf MGA200 Spec, page 4-24 ff

      /* 
       * FIXME: ist das die richtige reihenfolge? erst RAM Initialisieren
       * und *dann* wieder resetten?
       */
      
      // initialize S?RAM 
      
      // step 1: turn off screen
      // val = readVGA(SEQ, 1);
      // writeVGA(SEQ, 1, val | 0x20 );
      // step 2: program MCTLWTST
      writeRegister(MCTLWTST, 0x80a49891);   /* default: 0x84a49921 */
      // step 3: program OPTION:memconfig
      val = readConfig(OPTION) & ~(0x7 << 10);
      writeConfig(OPTION, val | (0x5 << 10));   /* two banks, 16MB */
      // step 4: program OPTION2:mbuftype
      val = readConfig(OPTION2) & ~(0x3 << 12);
      writeConfig(OPTION2, val) ;
      // step 5: program MEMRDBK:mrsopcod to '0000'
      val = readRegister(MEMRDBK) & ~(0xf << 25);
      writeRegister(MEMRDBK, val);
      // step 6: program MEMRDBK:mclkbrd0 and mclkbrd1
      val = readRegister(MEMRDBK) & ~(0xf << 5) & ~(0xf << 0);
      writeRegister(MEMRDBK, val | (0x8 << 5) | 0x8);
      // step 7: wait 200 microsecons
      MySleep.usleep(200);
      // step 8: set MACCESS:memreset
      writeRegister(MACCESS, 1 << 15);
      MySleep.usleep(250);      // FIXME: should I stay or should I go? and why? ;-)
      // step 9: start refresh with OPTION.rfhcnt
      val = readConfig(OPTION) & ~(0x3f << 15);
      writeConfig(OPTION, val | (0x22<<15));
   
      
      // S?RAM Reset Sequenze
      
      // step 1: programm XVREFCTRL
      writeDAC8(XVREFCTRL, 0x3f);	/* vrmend uses 0x6f */
      MySleep.msleep(100);
      
      // step 2: power up system PLL
      val = readConfig(OPTION);
      writeConfig(OPTION, val | (1<<5));
      
      // step 3: wait for system PLL to lock (poll XSYSPLLSTAT.syslock until '1')
      for(loops=1000; (readDAC8(XSYSPLLSTAT) & 0x40) == 0 && loops >0 ; --loops)
	MySleep.msleep(10);
      if( loops <= 0 )
	Debug.out.println("**** ERROR: cannot lock system PLL! (nevertheless continuing)");
      
      // step 4: power up pixel PLL by setting XPIXCLKCTRL:pixpllpdN
      val = readDAC8(XPIXCLKCTRL);
      writeDAC8(XPIXCLKCTRL, val | 0x8);
      
      // step 5: wait for pixel PLL to lock by polling XPIXSTAT:pixlock until '1'
      for(loops=1000; (readDAC8(XPIXPLLSTAT) & 0x40) == 0 && loops > 0; --loops)
	MySleep.msleep(10);
      if( loops <= 0 )
	Debug.out.println("**** ERROR: cannot lock pixel PLL! (nevertheless continuing)");
      
      // step 6 & 7: 
      // power up LUT by setting XMISCCTRL:ramcs to 1
      // power up DAC by setting XMISCCTRL:dacpdN to 1
      val = readDAC8(XMISCCTRL);
      writeDAC8(XMISCCTRL, val | 0x11);
      // FIXME: vrmend unconditionally sets 0x1b (but does't work, too)
      
      
      // Set PLLs to desired frequencies and select them
      
      // 1. disable system clocks (set OPTION:sysclkdis)
      val = readConfig(OPTION);
      writeConfig(OPTION, val | 0x4);
      
      // 2. select system PLL (set OPTION:sysclksl)
      val = readConfig(OPTION) & ~0x3;
      writeConfig(OPTION, val | 0x1);
      
      // 3. enable system clocks (unset OPTION:sysclkdis)
      val = readConfig(OPTION) & ~0x4;
      writeConfig(OPTION, val);
      
      /*
       * How it *does* work (somehow/something):
       * 
       * // 4.
       * val = readDAC8(XPIXCLKCTRL);
       * writeDAC8(XPIXCLKCTRL, val|0x4);
       * 
       * // 5.
       * val = readDAC8(XPIXCLKCTRL) & 0x3;
       * writeDAC8(XPIXCLKCTRL, val | 0x1);
       *
       * // 6. 
       * val = readDAC8(XPIXCLKCTRL) & 0x04 ;
       * writeDAC8(XPIXCLKCTRL, val);
       *
       * But according to the spec it *should* work: 
       * 
       * // 4. disable pixel clock & video clock
       * val = readDAC8(XPIXCLKCTRL);
       * writeDAC8(XPIXCLKCTRL, val|0x4);
       *
       * // 5. select pixel PLL 
       * val = readDAC8(XPIXCLKCTRL) & ~0x3;
       * writeDAC8(XPIXCLKCTRL, val | 0x1);
       *
       * // 6. enable pixel & video clock: clear XPIXCLKCTRL:pixclkdis
       * val = readDAC8(XPIXCLKCTRL) & ~0x4;
       * writeDAC8(XPIXCLKCTRL, val);
       * 
       * Ahh! I forgot to set a valid pixelclock frequency, so the "wrong"
       * way reset the pixelclock source to PCI clock which seemed to work.
       */
      
      // 4. disable pixel clock & video clock
      val = readDAC8(XPIXCLKCTRL);
      writeDAC8(XPIXCLKCTRL, val|0x4);
      
      // 5. select pixel PLL 
      val = readDAC8(XPIXCLKCTRL) & ~0x3;
      writeDAC8(XPIXCLKCTRL, val | 0x1);
      
      // 6. enable pixel & video clock: clear XPIXCLKCTRL:pixclkdis
      val = readDAC8(XPIXCLKCTRL) & ~0x4;
      writeDAC8(XPIXCLKCTRL, val);
      
      
      // SetMode114
      
      // 1. wait for vertical retrace
      while( (readRegister8(INSTS1) & 0x8) == 0 )
	;
      
      // 2. disable video: set SEQ1:scroff
      // val = readVGA(SEQ, 1);
      // writeVGA(SEQ, 1, val | 0x20 );
      
      // 3. select SVGA mode
      writeVGA(CRTCEXT, 3, 0x81);	/* mgamode(7), scale(2:0) = 0.5 
					 * Page 4-62: 0x1 means 32 bpp But
					 * using the 16 bpp value doesn't
					 * work. */
      
      
      // 4. programm VGA registers
      writeVGA(SEQ, 1, 0x20);
      writeRegister(MISC_W, 0x03);
      
      // 5. initialize CRT
      writeVGA(CRTC, 0x11, 0x00);	/* unlock writelock */
      
      writeVGA(CRTC, 0x0, 0x7f);	/* htotal */
      writeVGA(CRTC, 0x1, 0x63);	/* hdisplay end */
      writeVGA(CRTC, 0x2, 0x63);	/* hblank start */
      writeVGA(CRTC, 0x3, 0x03);	/* hblank end */
      writeVGA(CRTC, 0x4, 0x68);	/* hsync start */
      writeVGA(CRTC, 0x5, 0x18);	/* hsync end */
      
      writeVGA(CRTC, 0x6, 0x72);	/* vtotal */
      writeVGA(CRTC, 0x7, 0xe0);	/* overflow */
      writeVGA(CRTC, 0x8, 0x00);	/* preset row scan */
      writeVGA(CRTC, 0x9, 0x60);	/* max scan line */
      writeVGA(CRTC, 0xA, 0x20);	/* cursor start (cursor disable)*/
      writeVGA(CRTC, 0xB, 0x00);	/* cursor end */
      writeVGA(CRTC, 0xC, 0x00);	/* start address high */
      writeVGA(CRTC, 0xD, 0x00);	/* start address low */
      writeVGA(CRTC, 0xE, 0x00);	/* cursor location high */
      writeVGA(CRTC, 0xF, 0x00);	/* cursor location low */
      writeVGA(CRTC, 0x10, 0x58);	/* vsync start */
      
      // !!!  THIS REGISTER IS WRITTEN TWICE TO BE ON THE SECURE SIDE !!!
      writeVGA(CRTC, 0x11, 0x2c);  	/* vsync end */
      writeVGA(CRTC, 0x12, 0x57);	/* vdisplay end */
      writeVGA(CRTC, 0x13, 0x64);	/* offset: 800*16*1/128 = 100 = 0x64 */
      writeVGA(CRTC, 0x14, 0x00);	/* underline location */
      writeVGA(CRTC, 0x15, 0x57);	/* vblank start */
      writeVGA(CRTC, 0x16, 0x73);	/* vblank end */
      
      // in MGA mode wbmode, selrowscan and cms have must be set to true
      writeVGA(CRTC, 0x17, 0xc3);	/* CRTC mode control */
      writeVGA(CRTC, 0x18, 0xFF);	/* line compare */
      writeVGA(CRTC, 0x22, 0x04);	/* CPU read latch (?????) */
      
      writeVGA(CRTCEXT, 0x0, 0x00);	/* add generator ext */
      writeVGA(CRTCEXT, 0x1, 0x00);	/* hcounter ext */
      writeVGA(CRTCEXT, 0x2, 0x00);	/* vcounter ext */
      
      writeVGA(CRTCEXT, 0x4, 0x00);	/* mem page */
      writeVGA(CRTCEXT, 0x5, 0x34);	/* h video half count (interlaced mode only) */
      writeVGA(CRTCEXT, 0x6, 0x00);	/* priority request control */
      writeVGA(CRTCEXT, 0x7, 0x00);	/* requester controller */
      
      // 6. initialize DAC and video PLL 
      
      /* set pixel clock to 56.25 MHz: (m, n, p, s) = ( 11, 24, 0, 0) */
      
      // 6.1. force screen off
      // 6.2. set XPIXCLKCTRL:pixclkdis(2) to 1 (disable pixel&video clock)
      val = readDAC8(XPIXCLKCTRL);
      writeDAC8(XPIXCLKCTRL, val | 0x4);
      
      // 6.3. reprogramm pixel PLL registers
					 writeDAC8(XPIXPLLCM, 11);
      writeDAC8(XPIXPLLCN, 24);
      writeDAC8(XPIXPLLCP, 0);		/* S & P */
      
      val = readDAC8(XPIXCLKCTRL) & ~0x3;
      writeDAC8(XPIXCLKCTRL, val | 0x01);   /* select PLL as source */
      
      val = readRegister8(MISC_R);
      writeRegister8(MISC_W, (byte)(val | (0x3 << 2)));   /* select PLL C */
      
      // 6.4. wait until PLL is locked
      for(loops=1000; (readDAC8(XPIXPLLSTAT) & 0x40) == 0  && loops > 0; --loops)
	MySleep.msleep(10);
      if( loops <= 0 )
	Debug.out.println("**** ERROR: cannot lock video PLL! (nevertheless continuing)");

            
      // 6.5. clear XPIXCLKCTRL:pixclkdis
      val = readDAC8(XPIXCLKCTRL) & ~ 0x4;
      writeDAC8(XPIXCLKCTRL, val);
      
      // 6.6. enable screen
      
      writeDAC8(XMULCTRL, 0x02);	/* 16 bpp palettized */
      
      val = readDAC8(XMISCCTRL);
      writeDAC8(XMISCCTRL, val | 0x8);	/* want 8 bit palette */

      
      /* initialize pallette */
      writeRegister8(PALWTADD, (byte)0);
      for(int i=0; i<64; ++i){
	 writeRegister8(PALDATA, (byte)(i*256/32));
	 writeRegister8(PALDATA, (byte)(i*256/64));
	 writeRegister8(PALDATA, (byte)(i*256/32));
      }
      
      
      // 7. initialize framebuffer
      
      
      // 8. wait for vertical retrace
      while( (readRegister8(INSTS1) & 0x8) == 0 )
	;
      
      // 9. enable video: clear SEG1:scroff
      val = readVGA(SEQ, 1) & ~0x20;
      writeVGA(SEQ, 1, val);

      framebuffer = new MGA200Framebuffer(fbufMem, 800, 600, 16, 800*2, 2);
   }

   /********************************************************************/
   
   /**
    * Return the description of the graphic subsystem.
    */
   public PackedFramebuffer getFramebuffer(){
      return framebuffer;
   }
   

   /**
    * Do some simple tests with the framebuffer.
    */
   public void testFramebuffer(){
      framebuffer.test();
   }
}

/********************************************************************/
/********************************************************************/

class MGA200Framebuffer implements PackedFramebuffer {
   public final static short COL_WHITE	= (short)0xffff;
   public final static short COL_RED	= (short)0xf800;
   public final static short COL_GREEN	= (short)0x07e0;
   public final static short COL_BLUE	= (short)0x001f;
   public final static short COL_BLACK	= (short)0x0000;
   
   DeviceMemory framebuffer;
   int width, height, depth;
   int scanlineOffset;
   int pixelOffset;
   
    short foreground = COL_BLACK;
    short background = COL_WHITE;

   MGA200Framebuffer(DeviceMemory framebuffer, int width, int height, int depth, int scanlineOffset, int pixelOffset){
      this.framebuffer = framebuffer;
      this.width = width;
      this.height = height;
      this.depth = depth;
      this.scanlineOffset = scanlineOffset;
      this.pixelOffset = pixelOffset;
      
      Debug.assert(scanlineOffset > 0, "wrong scanlineOffset");
      Debug.assert(pixelOffset > 0, "wrong pixelOffset");
      Debug.assert(depth >= 8 ,"wrong depth");
      
      Debug.assert(width >= 800, "dubious width");
      Debug.assert(height >= 600, "dubious height");
   }

   /* Implementation of interface FramebufferDevice */

    public void open() {
	clear();
    }


    public void drawRect(int x, int y, int w, int h) {
	for(int y0=y; y0<y+h; y0++)
	    for(int x0=x; x0<x+w; x0++)
		drawPoint(x0,y0);
    }

    public void drawPoint(int x, int y) {
	framebuffer.set16(y*width +  x, foreground);
    }

   /* Implementation of interface Framebuffer */
   
   public Memory memObj(){
      return framebuffer;
   }
   public DeviceMemory devMemObj ()
   {
   	return framebuffer;
   }
   public int startAddress(){
      return framebuffer.getStartAddress();
   }
   public int scanlineOffset(){
      return scanlineOffset;
   }
   public int pixelOffset(){
      return pixelOffset;
   }
   public int width(){
      return width;
   }
   public int height(){
      return height;
   }
   public int depth(){
      return depth;
   }
   public String toString(){
      String retval = "MGA200Framebuffer(";
      retval += width + "x" + height + "x" +depth;
      retval += " @ 0x" + Hex.toHexString(startAddress());
      retval += " pOff=" + pixelOffset;
      retval += " sOff=" + scanlineOffset;
      retval += " memObj@0x" + Hex.toHexString(framebuffer.getStartAddress());
      retval += ") ";
      return retval;
   }
   public String framebufferType(){
      return "MGA200Framebuffer";
   }

   public void test(){
      // fill framebuffer with white
      Debug.out.println("\nfilling framebuffer with white...");
      framebuffer.fill16(COL_WHITE, 0, width*height*pixelOffset/2);
      
      // fill framebuffer with black
      Debug.out.println("\nfilling framebuffer with black...");
      framebuffer.fill16(COL_BLACK, scanlineOffset/2, width*(height-2)*pixelOffset/2);
      
      
      // fill framebuffer with base colors
      Debug.out.println("\nfilling framebuffer with red...");
      framebuffer.fill16(COL_RED, 0, width*height*pixelOffset/2);
      
      Debug.out.println("\nfilling framebuffer with green...");
      framebuffer.fill16(COL_GREEN, 0, width*height*pixelOffset/2);
      
      Debug.out.println("\nfilling framebuffer with blue...");
      framebuffer.fill16(COL_BLUE, 0, width*height*pixelOffset/2);
   
      Debug.out.println("\nfilling framebuffer with three colors ...");
      
      int lineindex;
      for(int y=0; y<height; ++y){
	 lineindex = y * scanlineOffset / 2;
	 for(int x=0; x<width; ){ 
	    
	    /*
	     * Note: If the pixels of the three base colors are set side by
	     * side, they may get interpolated by the monitor to gray lines.
	     * so I put some white pixels between each of them.
	     */
	     
	    framebuffer.set16(lineindex + x++, COL_RED);
	    framebuffer.set16(lineindex + x++, COL_WHITE);
	    
	    framebuffer.set16(lineindex + x++, COL_GREEN);
	    framebuffer.set16(lineindex + x++, COL_WHITE);
	    
	    framebuffer.set16(lineindex + x++, COL_BLUE);
	    framebuffer.set16(lineindex + x++, COL_WHITE);
	 }
      }
      
      Debug.out.println("\nfilling framebuffer with shades...");
      for(int y=0; y<=height; ++y)
	framebuffer.fill16((short)y, y*scanlineOffset, width*pixelOffset);
      
      Debug.out.println("\nend of MGA200Framebuffer test");
   }
   
   public void clear(){
      clear(background);
   }

   public void clear(short color){
      framebuffer.fill16(color, 0, width*height*pixelOffset/2);
   }
   
}

/********************************************************************/
/********************************************************************/

interface MGA200Defines {
   /* Configuration registers */
   int DEVID	= 0x00;
   int DEVCTRL	= 0x04;
   int MGABASE2	= 0x10;
   int MGABASE1	= 0x14;
   int OPTION	= 0x40;
   int OPTION2	= 0x50;
   
   
   /* MGA registes */
   int MACCESS	= 0x1c04;
   int MCTLWTST	= 0x1c08;
   int MEMRDBK	= 0x1e44;
   int AGP_PLL	= 0x1e4c;
   
   /* VGA registers */
   int MISC_W	= 0x1fc2;
   int MISC_R	= 0x1fcc;
   int INSTS1	= 0x1fda;
   
   /* VGA "multiplexed" registers */
   int SEQ	= 0x1fc4;
   int CRTC	= 0x1fd4;
   int CRTCEXT	= 0x1fde;
   
   /* DAC registers */
   int PALWTADD	= 0x3c00;
   int PALDATA	= 0x3c01;
   int PALRDADD	= 0x3c03;
   int X_DATAREG= 0x3c0a;
   
   
   /* DAC X_DATAREG indices */
   byte XVREFCTRL   = 0x18;
   byte XMULCTRL    = 0x19;
   byte XPIXCLKCTRL = 0x1a;
   byte XMISCCTRL   = 0x1e;
   byte XSYSPLLSTAT = 0x2f;
   byte XPIXPLLSTAT = 0x4f;
   
   byte XPIXPLLCM   = 0x4c;
   byte XPIXPLLCN   = 0x4d;
   byte XPIXPLLCP   = 0x4e;

};
