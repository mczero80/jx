package jx.wintv;

/* 
 * working: 
 * 
 *     - select InputSource via 
 * 
 *		<source> <num>	select getSVideo(num)
 * 		<source> <tv>	select getTuner()
 * 		<source> <res>	select getChinch()
 * 		<source> <source> select color bars ;-)
 * 
 *     - change H/V delay via <reserved> <any cursor key>
 *     - change H/V width (active) <0> <any cursor key>
 *     - change H/V scaling via <minimize> <any cursor key>
 *     - reset H/V geometry via <fullscreen>
 *     - start/stop capture <mute>
 * 
 * TODO:
 *     - select tuner channel via
 * 
 * 		<tv> <num>	select some predefined channels
 * 
 */

import jx.framebuffer.PackedFramebuffer;
import jx.zero.*;

public class WinTVRemote implements RCKeys, Runnable {
   CaptureDevice wintv;
   BT878GeometryInterface btg;
   IRReceiver ir;			/* IR receiver  */
   PackedFramebuffer framebufferOdd;
   PackedFramebuffer framebufferEven;
   
   boolean oddFieldActive = true;
   boolean evenFieldActive = true;
   
   boolean doCapture = true;		/* are we capturing? */
   
   CaptureGeometry resetGeom = null;
   
   public WinTVRemote(CaptureDevice wintv, IRReceiver ir, 
	       PackedFramebuffer framebufferOdd, 
	       PackedFramebuffer framebufferEven){
      this.wintv = wintv;
      this.btg = wintv.getGeometryInterface();
      this.ir = ir;
      this.framebufferOdd = framebufferOdd;
      this.framebufferEven = framebufferEven;
   }
   
   public void run(){
      
      resetGeom = new CaptureGeometry(btg, CaptureGeometry.ODD);
      
      RCKey key;
      int fcap;
      while(true){
	 key = waitForKey(false);
	 handleKeyEvent(key);
      }
   }
   
   void handleKeyEvent(RCKey key){
      
      // select input source
      if( key.equals(key_Source) ){
	 Debug.out.println("**** Select InputSource:\n" +
			   "     1         Chinch input\n"+
			   "     2         SVideo input\n"+
			   "     TV        tuner\n"+
			   "     Source    color bars\n");
	 
	 key = waitForKey(false);
	 
	 Debug.out.println("XXXX");
	 
	 InputSource newsource = null;
	 String descr = null;
	 
	 if( key.equals(key_Tv) ){
	    newsource = wintv.getTuner();
	    descr = "Tuner";
	 }
	 else if( key.equals(key_1) ){
	    newsource = wintv.getChinch();
	    descr = "Chinch";
	 }
	 else if( key.equals(key_2) ){
	    newsource = wintv.getSVideo();
	    descr = "SVideo";
	 }
	 else if( key.equals(key_Source) ){
	    newsource = wintv.getColorBars();
	    descr = "ColorBars";
	 }
	 else {
	    Debug.out.println("**** unknown InputSource");
	    return;
	 }
	 
	 Debug.out.println("**** setting new InputSource: "+descr);
	 if( newsource instanceof Tuner ) {
	    Debug.out.println("setup tuner frequ...");
	    // ((Tuner)newsource).setFrequ(591250000);   /* C36 (Prog 2) */
	    ((Tuner)newsource).setFrequ(647250000);   /* C43 (Prog 1) */
	 }
	 wintv.getCurrentInputSource().deactivate();
	 newsource.activate();
	 return;
      }
      
      // H/V Delay
      if( key.equals(key_Reserved) ){
	 SetNumValue setval = new SetNumValue(this, "H/V-Delay", "HDelay", "VDelay") {
	    CaptureGeometry g = new CaptureGeometry(btg, CaptureGeometry.ODD);
	    void getValues(){
	       hval = g.hdelay;
	       vval = g.vdelay;
	    }
	    void setHValue(int val){
	       g.hdelay = val;
	       g.set(btg, CaptureGeometry.ODD);
	       g.set(btg, CaptureGeometry.EVEN);
	    }
	    void setVValue(int val){
	       g.vdelay = val;
	       g.set(btg, CaptureGeometry.ODD);
	       g.set(btg, CaptureGeometry.EVEN);
	    }
	 };
	 setval.doit();
	 return;
      }
      
      // H/V Active
      if( key.equals(key_0) ){
	 SetNumValue setval = new SetNumValue(this, "H/V-Active", "HActive", "VActive") {
	    CaptureGeometry g = new CaptureGeometry(btg, CaptureGeometry.ODD);
	    void getValues(){
	       hval = g.hactive;
	       vval = g.vactive;
	    }
	    void setHValue(int val){
	       g.hactive = val;
	       g.set(btg, CaptureGeometry.ODD);
	       g.set(btg, CaptureGeometry.EVEN);
	    }
	    void setVValue(int val){
	       g.vactive = val;
	       g.set(btg, CaptureGeometry.ODD);
	       g.set(btg, CaptureGeometry.EVEN);
	    }
	 };
	 setval.doit();
	 return;
      }
      
      // H/V-Scale
      if( key.equals(key_Minimize) ){
	 SetNumValue setval = new SetNumValue(this, "H/V-Scale", "HScale", "VScale") {
	    CaptureGeometry g = new CaptureGeometry(btg, CaptureGeometry.ODD);
	    void getValues(){
	       hval = g.hscale;
	       vval = g.vscale;
	    }
	    void setHValue(int val){
	       g.hscale = val;
	       g.set(btg, CaptureGeometry.ODD);
	       g.set(btg, CaptureGeometry.EVEN);
	    }
	    void setVValue(int val){
	       g.vscale = val;
	       g.set(btg, CaptureGeometry.ODD);
	       g.set(btg, CaptureGeometry.EVEN);
	    }
	 };
	 setval.doit();
	 return;
      }
      
      // reset geometry
      if( key.equals(key_FullScreen) ){
	 Debug.out.println("**** Resetting capture geometry");
	 resetGeom.set(btg, CaptureGeometry.ODD);
	 resetGeom.set(btg, CaptureGeometry.EVEN);
	 return;
      }
      
      // Capture On/Off
      if( key.equals(key_Mute) ){
	 if( doCapture )
	   wintv.captureOff();
	 else
	   wintv.captureOn();
	 doCapture = !doCapture;
	 Debug.out.println("**** toggled capture "+ (doCapture? "on": "off"));
	 return;
      }
      
      // TVNorm setting
      /* FIXME
      if( key.equals(key_Radio) ){
	 Debug.out.println("**** Number of IFORM:FORMAT :\n" +
			   "    0    Auto format detect\n" +
			   "    1    NTSC(M)\n" +
			   "    2    NTSC(J)\n" +
			   "    3    PAL(B,D,G,H,I)\n" +
			   "    4    PAL(M)\n" +
			   "    5    PAL(N)\n" +
			   "    6    SECAM\n" +
			   "    7    PAL(NC)\n"
			   );
	 
	 int format = -1;
	 key = waitForKey(false);
	 if( key.equals(key_0) )
	   format = bt.FORMAT_AUTO;
	 else if( key.equals(key_1) )
	   format = bt.FORMAT_NTSC_M;
	 else if( key.equals(key_2) )
	   format = bt.FORMAT_NTSC_J;
	 else if( key.equals(key_3) )
	   format = bt.FORMAT_PAL_BDGHI;
	 else if( key.equals(key_4) )
	   format = bt.FORMAT_PAL_M;
	 else if( key.equals(key_5) )
	   format = bt.FORMAT_PAL_N;
	 else if( key.equals(key_6) )
	   format = bt.FORMAT_SECAM;
	 else if( key.equals(key_7) )
	   format = bt.FORMAT_PAL_NC;
	 else 
	   Debug.out.println("unknown format: "+(key.code/4));
	 
	 if( format >= 0 ){
	    Debug.out.println("**** set format to "+format);
	    bt.setFormat(format);
	 }
	   
	 return;
      }
       */
      
      if( key.equals(key_1) ){
	 Debug.out.println("**** switch "+ (oddFieldActive ? "off": "on") + " odd field.");
	 wintv.setOddFieldActive( ! oddFieldActive );
	 oddFieldActive = ! oddFieldActive;
	 
	 framebufferOdd.clear();
	 return;
      }
      
      if( key.equals(key_2) ){
	 Debug.out.println("**** switch "+ (evenFieldActive ? "off": "on") + " even field.");
	 wintv.setEvenFieldActive( ! evenFieldActive );
	 evenFieldActive = ! evenFieldActive;
	 
	 framebufferEven.clear();
	 return;
      }
      
      /* FIXME
      if( key.equals(key_4) ){
	 Debug.out.println("**** PCI burst parameters:");
	 SetNumValue setval = new SetNumValue(this, 
					      "PCI burst parameters", 
					      "FIFO trigger point", 
					      "PCI Latency Timer", 1, 1) {
	    void getValues(){
	       hval = bt.getPackedFifoTriggerPoint();
	       vval = wintv.pcidevice.getLatencyTimer();
	    }
	    void setHValue(int val){
	       bt.setPackedFifoTriggerPoint(val & 0x0f);
	    }
	    void setVValue(int val){
	       wintv.pcidevice.setLatencyTimer((byte)val);
	    }
	    String decodeHValue(int tp){
	       switch(tp & 0x0f){
		case 0:	return "4";
		case 1:	return "8";
		case 2:	return "16";
		case 3:	return "32";
	       }
	       return "(unknown)";
	    }
	 };
	 setval.doit();
	 return;
      }
       */
      
      // Video Adjustment Menu
      if( key.equals(key_7) ){
	 handleVideoAdjustments();
	 return;
      }
      
      // Component / Composite Mode
      if( key.equals(key_9) ){
	 InputSource cur= wintv.getCurrentInputSource();
	 Debug.out.println("checking current InputSource...");
	 if( cur instanceof SVideo ){
	    SVideo svhs = (SVideo)cur;
	    boolean composite = !svhs.getChromaMode();
	    
	    Debug.out.println("**** switch Composite Mode "+(composite? "on" : "off"));
	    svhs.setChromaMode(composite);
	    return;
	 }
	 Debug.out.println("**** current InputSource is not SVideo.");
	 return;
      }
      
      if( key.equals(key_Tv) ){
	 Debug.out.println("**** current geometry:");
	 Debug.out.println("Geometry odd field : ("+
			   btg.getHScaleOdd()+","+btg.getHDelayOdd()+","+btg.getHActiveOdd()+","+
			   btg.getVScaleOdd()+","+btg.getVDelayOdd()+","+btg.getVActiveOdd()+")");
	 Debug.out.println("Geometry even field: ("+
			   btg.getHScaleEven()+","+btg.getHDelayEven()+","+btg.getHActiveEven()+","+
			   btg.getVScaleEven()+","+btg.getVDelayEven()+","+btg.getVActiveEven()+")");
      }
      
      Debug.out.println("**** unused key: "+key.label);
   }
   
   /********************************************************************/
   /* Handle Video Adjustments */
   
   void handleVideoAdjustments(){
      
      // implemented: 
      //     Brighness
      //     Contrast (Luma Gain)
      //     Chroma(U/V) Gain (Saturation)
      //     Gamma Correction Removal
      // 
      // TODO:
      //     Hue Control
      //     White Crush Up
      //     White Crush Down
      //     Luma Coring
      //     Temporal Decimation (?)
      // 
      //     RGB Error Diffusion
      //     Dither Frame (?)
      
      
      Debug.out.println("**** Video Adjustement Menu:\n" +
			"     1    Brightness / Contrast Mode (" + wintv.getVideoAdjustment("Brightness") + " / " + wintv.getVideoAdjustment("Contrast") + ")\n" +
			"     2    Chroma U/V Gain Mode       (" + wintv.getVideoAdjustment("ChromaU") + " / " + wintv.getVideoAdjustment("ChromaV") + ")\n" +
			"     3    Gamma Correction Removal   (" + wintv.getVideoAdjustment("GammaCorrectionRemoval") + ")\n"
			);
      
      RCKey key = waitForKey(false);
      
      // Brightness, Contrast
      if( key.equals(key_1) ){
	 SetNumValue setval = new SetNumValue(this, "Brightness / Contrast", "Brightness", "Contrast", 5, 5){
	    BrightnessAdjustment br = (BrightnessAdjustment)wintv.getVideoAdjustment("Brightness");
	    ContrastAdjustment   co = (ContrastAdjustment)wintv.getVideoAdjustment("Contrast");
	    
	    void getValues(){
	       hval = br.getHWValue();
	       vval = co.getHWValue();
	    }
	    void setHValue(int val){
	       br.setHWValue(val);
	       wintv.setVideoAdjustment(br);
	    }
	    void setVValue(int val){
	       co.setHWValue(val);
	       wintv.setVideoAdjustment(co);
	    }
	    String decodeHValue(int val){
	       return br.toString(val);
	    }
	    String decodeVValue(int val){
	       return co.toString(val);
	    }
	 };
	 setval.doit();
	 return;
      }
      
      // Saturation U/V
      if( key.equals(key_2) ){
	 SetNumValue setval = new SetNumValue(this, "Chroma U / Chroma V", "Chroma U", "Chroma V", 1, 1){
	    ChromaUAdjustment cu = (ChromaUAdjustment)wintv.getVideoAdjustment("ChromaU");
	    ChromaVAdjustment cv = (ChromaVAdjustment)wintv.getVideoAdjustment("ChromaV");

	    void getValues(){
	       hval = cu.getHWValue();
	       vval = cv.getHWValue();
	    }
	    void setHValue(int val){
	       cu.setHWValue(val);
	       wintv.setVideoAdjustment(cu);
	    }
	    void setVValue(int val){
	       cv.setHWValue(val);
	       wintv.setVideoAdjustment(cv);
	    }
	    String decodeHValue(int val){
	       return cu.toString(val);
	    }
	    String decodeVValue(int val){
	       return cv.toString();
	    }
	 };
	 setval.doit();
	 return;
      }
      
      // Gamma Correction Removal
      if( key.equals(key_3) ){
	 GammaCorrectionRemovalAdjustment va = (GammaCorrectionRemovalAdjustment)wintv.getVideoAdjustment("GammaCorrectionRemoval");
	 va.setHWValue(!va.getHWValue());
	 wintv.setVideoAdjustment(va);
	 Debug.out.println("**** Toggled Gamma Correction Removal");
	 return;
      }
      

   }
   
   
   /********************************************************************/
   
   void autoSearchWidth(){
      /*
      MemoryManager memMgr = (MemoryManager)ZeroDomain.lookup("MemoryManager");
      
      PackedFramebuffer oldFramebufferOdd = framebufferOdd;
      PackedFramebuffer oldFramebufferEven = framebufferEven;
      
      RISCJumpBlock jump = new RISCJumpBlock(memMgr, 2);
      
      Debug.out.println("setup RISC preface...");
      RISCFieldBlock preface = new RISCFieldBlock(memMgr, 6*2);
      preface.sync(RISC.FIFO_FM1 | RISC.RESYNC);
      preface.sync(RISC.FIFO_VRE | RISC.RESYNC);
      preface.sync(RISC.FIFO_FM1 | RISC.RESYNC);
      preface.sync(RISC.FIFO_VRO | RISC.RESYNC);
      preface.jump(preface.riscs(0xff, 0x00), 0x0);
      preface.inval();
      
      jump.setPreface(preface);
      
      int width = 400;
      int delta = 0;
      
      Debug.out.println("looping...");
      for(; width<850; width += 10){
	 wintv.captureOff();
	 
//	 Debug.out.println("clear odd framebuffer");
	 framebufferOdd.clear();
//	 Debug.out.println("clear even framebuffer");
	 framebufferEven.clear();
	 
//	 Debug.out.println("render new risc code for odd field ...");
	 jump.setFieldBlock(0, render(width, framebufferOdd, RISC.FIFO_VRE));
//	 Debug.out.println("render new risc code for even field ...");
	 jump.setFieldBlock(1, render(width, framebufferEven, RISC.FIFO_VRO));
//	 Debug.out.println("render new risc code for jump block ...");
	 jump.render();
	 
	 bt.setRiscStartAdd(jump.getStartAddress());

	 Debug.out.println("testing width="+width+" delta="+delta);
	 bt.clearIntStat(bt.FDSR);
	 wintv.captureOn();
	 
	 MySleep.msleep(1000);
	 
	 Debug.out.println("INT status: "+
			   bt.interruptStatus(StatusFlag.STATUS_SET, 
					      wintv.irqHandler.getSavedStatus()
					      & ~( bt.INT_FIELD | bt.GPINT | bt.HSYNC | bt.VSYNC )
					      ));
	 wintv.irqHandler.clearStatus(bt.INT_STAT_MASK);
	 
      }
       */
   }
   
   
   RISCFieldBlock render(int width, PackedFramebuffer framebuffer, int syncType){
      MemoryManager memMgr = (MemoryManager)ZeroDomain.lookup("MemoryManager");
      FieldBlockCreator creator = RISCFieldBlock.getFieldCreator();
      
      // calculate size of RISC-Code
      int size = 0;
      size += 8;			/* sync(FM1) */
      size += framebuffer.height() * 8;	/* write for each scanline */
      size += framebuffer.height() * 8;	/* 2nd write */
      size += 8;			/* sync(VRO/VRE) */
      size += 8;			/* jump */
      size += 4;			/* inval */
      
      RISCFieldBlock rb = creator.createFieldBlock(size/4);
      
      int fbp = framebuffer.startAddress();
      int scanlineOffset = framebuffer.scanlineOffset();
      int pixelOffset = framebuffer.pixelOffset();
      int scanlineLength = (width-1) * pixelOffset;
      int height = framebuffer.height();
      
      rb.sync(RISC.FIFO_FM1 | RISC.RESYNC | RISC.IRQ);
      for(int y=0; y<height; ++y){
	 rb.write(RISC.SOL, scanlineLength, fbp);
	 rb.write(RISC.EOL | rb.riscs(0x00, 0x01), pixelOffset, fbp+scanlineLength);
	 fbp += scanlineOffset;
      }
      rb.sync(syncType | RISC.RESYNC);
      rb.jump(0x0);
      rb.inval();
      
      return rb;
   }
   

   /********************************************************************/
   
   void autodetectVideoFormat(GenericBT878 card){
      int commonFsc[] = {
	 3579545,			/* NTSC, some PAL variants */
	   4433619,			/* PAL */
      };
      
      BT878Video bt = card.bt;

      Debug.out.println("check for stable signal..");
      if( card.isVideoSignalStable(1000) ){
	 Debug.out.println("signal seems stable");
	 //Debug.out.println("signal seems stable, abort autodetection");
	 //return;
      }
      
      int i;
      for(i=0; i<commonFsc.length; ++i){
	 int fsc = commonFsc[i];
	 int a = 68*fsc*4/10/1000000 + 15;
	 int b = 65*fsc*4/10/1000000;
	 Debug.out.println("try Fsc "+fsc + " adelay="+a+" bdelay="+b);
	 card.setFsc(fsc);
	 card.bt.setADelay(a);
	 card.bt.setBDelay(b);
	 if( card.isVideoSignalStable(1000) ){
	    Debug.out.println("found Fsc with stable signal: "+fsc);
	    break;
	 }
      }
      if( i == commonFsc.length ){
	 Debug.out.println("did not find a Fsc with stable signal; aborting autodetection");
	 return;
      }
      
      Debug.out.println("Exit autodetection mode");
   }
   
   /********************************************************************/
   
   /* note: this kind of interface looses the "phase" information */
   RCKey waitForKey(boolean wantRepeats){
      RCKey key = null;
      
      // Debug.out.println("waitForKey("+wantRepeats+")");
      while(true){
	 key = ir.getKey();
	 
	 // invalidate key if repetitions are not wanted
	 if( key.equals(ir.lastKeyStillPressed) ){
	    if ( ! wantRepeats ){
	       // Debug.out.println("rejecting repeated key");
	       continue;
	    }
	    key = ir.lastKey();
	 }
	 // Debug.out.println("returning key "+key.label);
	 return key;
      }
   }
   
}

/********************************************************************/

abstract class SetNumValue {
   int hval;
   int vval;
   int oldh;
   int oldv;
   String descr;
   String hdescr;
   String vdescr;
   WinTVRemote remote;
   RCKeys rc;
   
   int hincr = 2;
   int vincr = 1;
   
   SetNumValue(WinTVRemote remote, String descr, String hdescr, String vdescr){
      this(remote, descr, hdescr, vdescr, 2, 1);
   }
      
   SetNumValue(WinTVRemote remote, String descr, String hdescr, String vdescr, int hincr, int vincr){
      this.remote = remote;
      this.descr = descr;
      this.hdescr = hdescr;
      this.vdescr = vdescr;
      this.hincr = hincr;
      this.vincr = vincr;
   }
   
   void doit(){
      Debug.out.println("**** Change "+descr+" via 'Cursor' keys");
      
      RCKey key = null;
      
      // poll until the first key is released.
      key=remote.waitForKey(false);
      
      while( true ){
	 key=remote.waitForKey(true);
	 if( !key.equals(rc.key_ChUp) && !key.equals(rc.key_ChDown) &&
	    !key.equals(rc.key_VolUp) && !key.equals(rc.key_VolDown) )
	   break;
	 
	 getValues();
	 int oldh = hval;
	 int oldv = vval;
	 
	 if( key.equals(rc.key_ChUp) )
	   vval += vincr;
	 if( key.equals(rc.key_ChDown) )
	   vval -= vincr;
	 if( key.equals(rc.key_VolUp) )
	   hval += hincr;
	 if( key.equals(rc.key_VolDown) )
	   hval -= hincr;
	 
	 if( vval != oldv ){
	    Debug.out.println("**** setting "+vdescr+": "+decodeVValue(oldv)+" -> "+decodeVValue(vval));
	    setVValue(vval);
	 }
	 if( hval != oldh ){
	    Debug.out.println("**** setting "+hdescr+": "+decodeHValue(oldh)+" -> "+decodeHValue(hval));
	    setHValue(hval);
	 }
      }
      Debug.out.println("**** leaving "+descr+" change mode");
   }
   
   String decodeHValue(int hval){
      return Integer.toString(hval);
   }
   String decodeVValue(int vval){
      return Integer.toString(vval);
   }
   
   abstract void getValues();
   abstract void setVValue(int val);
   abstract void setHValue(int val);
}

/********************************************************************/

class CaptureGeometry {
   final static int ODD = 1;
   final static int EVEN = 2;
   
   int hscale, vscale;
   int hdelay, vdelay;
   int hactive, vactive;
   
   CaptureGeometry(BT878GeometryInterface btg, int field){
      get(btg, field);
   }
   
   CaptureGeometry(int hscale, int hdelay, int hactive, 
		   int vscale, int vdelay, int vactive){
      this.hscale = hscale;
      this.vscale = vscale;
      this.hdelay = hdelay;
      this.vdelay = vdelay;
      this.hactive = hactive;
      this.vactive = vactive;
   }
   
   void get(BT878GeometryInterface btg, int field){
      if( field == ODD ){
	 hscale  = btg.getHScaleOdd();
	 hdelay  = btg.getHDelayOdd();
	 hactive = btg.getHActiveOdd();
	 vscale  = btg.getVScaleOdd();
	 vdelay  = btg.getVDelayOdd();
	 vactive = btg.getVActiveOdd();
      }
      else if( field == EVEN ){
	 hscale  = btg.getHScaleEven();
	 hdelay  = btg.getHDelayEven();
	 hactive = btg.getHActiveEven();
	 vscale  = btg.getVScaleEven();
	 vdelay  = btg.getVDelayEven();
	 vactive = btg.getVActiveEven();
      }
      else 
	throw new Error("What field?");
   }
   
   void set(BT878GeometryInterface btg, int field){
      if( field == ODD ){
	 btg.setGeometryOdd(hscale, hdelay, hactive,
			   vscale, vdelay, vactive);
      }
      else if( field == EVEN ){
	 btg.setGeometryEven(hscale, hdelay, hactive,
			    vscale, vdelay, vactive);
      }
      else 
	throw new Error("What field?");
   }
}
