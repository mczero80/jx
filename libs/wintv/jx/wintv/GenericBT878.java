/*
 * Im bt878a Manual ist bei "FIFO Overrun Conditions" die Rede von "in case
 * the FIFO overrun condition is cleared." Heißt das vielleicht, daß ich
 * die INT Flags löschen soll, damit weitere Daten geliefert werden?
 * 
 * Außerdem lohnt es sich vielleicht, mit der Latency herumzuspielen.
 * 
 * Im bt878a Manual ist ausführlich von "Byte Alignment" die Rede!! Das ist
 * wohl für SubFramebuffer wichtig!
 * 
 */

package jx.wintv;

import jx.framebuffer.*;
import jx.zero.*;
import jx.zero.debug.*;
import jx.devices.pci.*;
import java.util.Vector;
import java.util.Enumeration;

abstract class GenericBT878 implements CaptureDevice/*, BT878GeometryInterface*/, Service {
    static Naming naming = InitialNaming.getInitialNaming();
   
   /* Debugging options */
   final static boolean useHardcodedPLLs	= false;
   final static boolean allowInterrupts         = true;
   final static boolean optimzeFIFOTriggers	= true;
   
   /* field variables */
   PCIDevice pcidevice = null; 
   
   BT878Video bt = null;
   
   InterruptHandler irqHandler = null;
   RISCJumpBlock riscjumps = null;
   
   NotifierList notifiersOdd = new NotifierList();
   NotifierList notifiersEven = new NotifierList();
   
   Thread pollThread = null;
   
   FieldDescr passive[] = new FieldDescr[]{
      new FieldDescr(), 
      new FieldDescr()
   };
   
   FieldDescr active[] = new FieldDescr[] {
      new FieldDescr(), 
      new FieldDescr()
   };

    BT878GeometryInterface geometryInterface;

   /********************************************************************/
   /* Initialization & Setup                                           */
   /********************************************************************/
   
   /**
    * Initialize internal stuff, setup PCI subsystem of the board.
    */
   GenericBT878(PCIDevice pcidevice){
       init(pcidevice);
       geometryInterface = new BT878GeometryInterface() {
	       public void setGeometryOdd(int hscale, int hdelay, int hactive, 
					  int vscale, int vdelay, int vactive) {
		   GenericBT878.this.setGeometryOdd(hscale, hdelay, hactive, vscale, vdelay, vactive);
	       } 
	       public void setGeometryEven(int hscale, int hdelay, int hactive, int vscale, int vdelay, int vactive) {
		   GenericBT878.this.setGeometryEven(hscale, hdelay, hactive, vscale, vdelay, vactive);
	       }
	       
	       public int getHScaleOdd() {return GenericBT878.this.getHScaleOdd();}
	       public int getHDelayOdd() {return GenericBT878.this.getHDelayOdd();}
	       public int getHActiveOdd() {return GenericBT878.this.getHActiveOdd();}
	       
	       public int getVScaleOdd() {return GenericBT878.this.getVScaleOdd();}
	       public int getVDelayOdd() {return GenericBT878.this.getVDelayOdd();}
	       public int getVActiveOdd() {return GenericBT878.this.getVActiveOdd();}
	       
	       public int getHScaleEven() {return GenericBT878.this.getHScaleEven();}
	       public int getHDelayEven() {return GenericBT878.this.getHDelayEven();}
	       public int getHActiveEven() {return GenericBT878.this.getHActiveEven();}
	       
	       public int getVScaleEven() {return GenericBT878.this.getVScaleEven();}
	       public int getVDelayEven() {return GenericBT878.this.getVDelayEven();}
	       public int getVActiveEven() {return GenericBT878.this.getVActiveEven();}
	   };
   }
   
    protected void init(PCIDevice pcidevice){
      this.pcidevice = pcidevice;
      
      this.riscjumps = new RISCJumpBlock(2);
    }

   void initPCI(){
      /*****************************/
      /* initialize PCI function 0 */
      /*****************************/
      
      int base0 = pcidevice.getBaseAddress(0) & PCI.BASEADDRESS_MEM_MASK;
      if( base0 == 0 ){
	 Debug.out.println("ERROR: "+getBoardName()+" card isn't initialized by BIOS!");
	 throw new Error(getBoardName()+" card isn't initialized by BIOS!");
      }
      
      Debug.out.println("bt878 registers @ 0x"+Hex.toHexString(base0));
      DeviceMemory localRegisters = ZeroDomain.memMgr.allocDeviceMemory(base0, 4*1024);
      this.bt = new BT878Video(localRegisters);
      
      // set PCI Latency Timer
      int minGNT 	= pcidevice.getMinGNT();
      int latencyTimer	= pcidevice.getLatencyTimer();
      Debug.out.println("min GNT:       "+minGNT);
      Debug.out.println("latency timer: "+latencyTimer);
      if( latencyTimer < minGNT ){
	 pcidevice.setLatencyTimer((byte)minGNT);
	 Debug.out.println("increasing latency timer: "+pcidevice.getLatencyTimer());
      }
      
      // optimize FIFO trigger points
      if( optimzeFIFOTriggers )
	bt.setFifoTriggerPoints(0x2, 0x2, 0x2);

      /*****************************/
      /* initialize other stuff    */
      /*****************************/
      
      // initialize IRQ handler
      int irqno = pcidevice.getInterruptLine();
      if( irqno == 0 )
	throw new Error("no PCI interrupt found");
      irqHandler = new InterruptHandler(this, irqno);
      irqHandler.install();
      
      riscjumps.render();
      
      bt.sreset();
   }
   
   InputSource makeInputSourcePortal(InputSource source, String type, String id){
      if( id != null )
	ZeroDomain.registerPortal(source, getBoardName()+"-"+id);
      return source;
   }
   
   /********************************************************************/
   /* Public Scratch Area                                              */
   /********************************************************************/
   
   /* capture and poll INT_STAT */
   public void startPollIntStat(final int millis){
      
      if( pollThread != null )
	return;
      
      pollThread = new Thread(){
	 public void run(){
	    while(true){
	       Debug.out.println(
				 "intstat=" +
				 bt.interruptStatus(StatusFlag.STATUS_SET, 
						    irqHandler.getSavedStatus()
						    & ~( bt.INT_FIELD | bt.GPINT | bt.HSYNC | bt.VSYNC | bt.RACK | bt.I2CDONE )
						    ) +
				 ";  dstat=" + bt.status(StatusFlag.STATUS_SET, bt.getDStatus()) 
				 );
	       bt.setDStatus(bt.LOF | bt.COF, 0);
	       irqHandler.clearStatus(bt.INT_STAT_MASK);
	       msleep(millis);
	    }
	 }
      };
      pollThread.start();
   }
   
   public void printIsVideoSignalStable(int millis){
      if( isVideoSignalStable(millis) )
	Debug.out.println("Video signal seems to be stable.");
      else 
	Debug.out.println("Video signal is *NOT* stable.");
   }
   
   public BT878Video getBt(){
      return bt;
   }
   
   public InterruptHandler getIrqHandler(){
      return irqHandler;
   }
   
   public void dumpControlBlock(int bytes){
      Debug.out.println("Control Block of " + getBoardName() + ":");
      bt.dumpControlBlock(Debug.out, bytes);
      Debug.out.println("End of Control Block");
   }
   
   public int getFieldCounter(boolean reset){
      int retval = bt.getFCap();
      if( reset )
	bt.clearFCap();
      return retval;
   }
   
   public void setTDec(boolean byField, boolean alignOddField, int rate){
      bt.setTDec(byField, alignOddField, rate);
   }

   public void doTimingTests(){
      tests.timingTests();
   }
   
   
   /*------------------------------------------------------------------*/
   /*- relocation of framebuffer                                      -*/
   /*------------------------------------------------------------------*/
   
   public void markOddFramebufferRelocateable(boolean isRelocatable){
      markFramebufferRelocateable(0, isRelocatable);
   }
   public void markEvenFramebufferRelocateable(boolean isRelocatable){
      markFramebufferRelocateable(1, isRelocatable);
   }
   void markFramebufferRelocateable(int fno, boolean isRelocatable){
      if( passive[fno].isValid() ){
	 PackedFramebuffer framebuffer = passive[fno].getFramebuffer();
	 int marked = passive[fno].setRelocatable(framebuffer, isRelocatable);
	 Debug.out.println("**** markReloc " + (fno == 0 ?  "Odd " : "Even ") +framebuffer+ ": "+ isRelocatable);
	 if( marked == 0 )
	   throw new NoFramebufferRegistered();
      }
      else 
	throw new NoFramebufferRegistered();
   }
   public void markFramebufferRelocateable(PackedFramebuffer framebuffer, boolean isRelocatable){
      Debug.out.println("markFramebufferRelocateable: " + framebuffer);
      
      int markedOdd = 0;
      if( passive[0].isValid() ){
	 markedOdd = passive[0].setRelocatable(framebuffer, isRelocatable);
	 if( markedOdd > 0 )
	   Debug.out.println("**** markReloc Odd "+framebuffer+ ": "+ isRelocatable);
      }
      
      int markedEven = 0;
      if( passive[1].isValid() ){
	 markedEven = passive[1].setRelocatable(framebuffer, isRelocatable);
	 if( markedEven > 0 )
	   Debug.out.println("**** markReloc Even "+framebuffer+ ": "+ isRelocatable);
      }
      
      if( markedOdd + markedEven == 0 )
	throw new NoFramebufferRegistered();
   }
   
   public void relocateFramebufferOdd(PackedFramebuffer framebuffer, int newStartAddress){
      relocateFramebuffer(0, framebuffer, newStartAddress);
   }
   public void relocateFramebufferEven(PackedFramebuffer framebuffer, int newStartAddress){
      relocateFramebuffer(1, framebuffer, newStartAddress);
   }
   public void relocateFramebuffer(PackedFramebuffer oldFramebuffer, PackedFramebuffer newFramebuffer, int newStartAddress){
      // FIXME
      if( oldFramebuffer instanceof LinkedPackedFramebuffer )
	throw new NotImpl();
      if( newFramebuffer instanceof LinkedPackedFramebuffer )
	throw new NotImpl();
      
      if( passive[0].framebuffer.equals( oldFramebuffer ) )
	relocateFramebuffer(0, newFramebuffer, newStartAddress);
      else if( passive[1].framebuffer.equals( oldFramebuffer ) )
	relocateFramebuffer(1, newFramebuffer, newStartAddress);
      else
	throw new NoFramebufferRegistered();
   }
   void relocateFramebuffer(int fno, PackedFramebuffer framebuffer, int newStartAddress){
      if( framebuffer == null && newStartAddress == 0 )
	throw new Error("do you really want relocation?");
      
      PackedFramebuffer current = passive[fno].getFramebuffer();
      if( current == null )
	current = active[fno].getFramebuffer();
      if( current == null )
	throw new NoFramebufferRegistered();
      
      if( framebuffer != null ){
	 // check for framebuffer compatibility (depth, enough memory to write data into)
	 if( current.depth() != framebuffer.depth() )
	   throw new IncompatibleFramebuffer();
	 // width & height are not important for compatibility as long as there is enough memory
	 
      }
      
      if( ! passive[fno].needRelocations() )
	throw new IncompatibleFramebuffer("framebuffer is not relocatable");
      // FIXME: ensure there is already some RISC code.
      // FIXME: ensure not to write outside of Memory object
      
      int curBase = passive[fno].getCurrentBase();
      int offset;
      if( newStartAddress == 0 )
	offset = framebuffer.startAddress() - curBase;
      else
	offset = newStartAddress - curBase;
      passive[fno].setCurrentBase(curBase + offset);
      
      RISCFieldBlockReloc rfb = (RISCFieldBlockReloc)passive[fno].getRISC();
      rfb.relocateField(offset);
      
      if( framebuffer != null )
	/* Do not use .setFramebuffer here, because this would imply a later setActive to render the RISC Code again. */
	passive[fno].framebuffer = framebuffer;
      
   }
   
   /********************************************************************/
   /* public interface                                                 */
   /********************************************************************/
   
   /*------------------------------------------------------------------*/
   /*- selection of video input source                                -*/
   /*------------------------------------------------------------------*/
   
   // The handling of the video input sources is handled in derived classes
   // which know "their" hardware much better.
   
   /*------------------------------------------------------------------*/
   /*- video picture adjustment interface                             -*/
   /*------------------------------------------------------------------*/
   
   public VideoAdjustment getVideoAdjustment(String name){
      if( name.equals("Contrast") )
	return new ContrastAdjustment(bt.getContrast());
      if( name.equals("Brightness") )
	return new BrightnessAdjustment(bt.getBright());
      if( name.equals("ChromaU") )
	return new ChromaUAdjustment(bt.getSatU());
      if( name.equals("ChromaV") )
	return new ChromaVAdjustment(bt.getSatV());
      if( name.equals("GammaCorrectionRemoval") )
	return new GammaCorrectionRemovalAdjustment(!bt.getGamma());
      if( name.equals("ChromaCombOdd") )
	return new ChromaCombAdjustment(ChromaCombAdjustment.ODD, bt.getChromaCombOdd());
      if( name.equals("ChromaCombEven") )
	return new ChromaCombAdjustment(ChromaCombAdjustment.EVEN, bt.getChromaCombEven());
      return null;
   }
   
   public void setVideoAdjustment(VideoAdjustment newValue){
      if( newValue instanceof FloatVideoAdjustment ){
	 FloatVideoAdjustment newFloatValue = (FloatVideoAdjustment)newValue;
	 if( newFloatValue instanceof ContrastAdjustment )
	   bt.setContrast(newFloatValue.getHWValue());
	 else if( newFloatValue instanceof BrightnessAdjustment )
	   bt.setBright(newFloatValue.getHWValue());
	 else if( newFloatValue instanceof ChromaUAdjustment )
	   bt.setSatU(newFloatValue.getHWValue());
	 else if( newFloatValue instanceof ChromaVAdjustment )
	   bt.setSatV(newFloatValue.getHWValue());
	 else
	   throw new Error("unknown subtype of FloatVideoAdjustment");
      }
      else if( newValue instanceof BooleanVideoAdjustment ){
	 BooleanVideoAdjustment newBoolValue = (BooleanVideoAdjustment)newValue;
	 if( newBoolValue instanceof GammaCorrectionRemovalAdjustment )
	   bt.setGamma(!newBoolValue.getHWValue());
	 else if( newBoolValue instanceof ChromaCombAdjustment ){
	    ChromaCombAdjustment cromacomb = (ChromaCombAdjustment)newBoolValue;
	    if( cromacomb.getField().equals(ChromaCombAdjustment.ODD) )
	      bt.setChromaCombOdd(cromacomb.getHWValue());
	    else
	      bt.setChromaCombEven(cromacomb.getHWValue());
	 }
	 else
	   throw new Error("unknown subtype of BooleanVideoAdjustment");
      }
      else
	throw new Error("unknown subtype of VideoAdjustment");
   }
   
   /*------------------------------------------------------------------*/
   /*- selection of video norm & picture geometry                     -*/
   /*------------------------------------------------------------------*/
   
   public void setNorm(TVNorm norm){
      /* FIXME: too many hardcoded things */
      
      /* setup input frequency */
      setFsc(norm.fsc);
      
      /* setup format */
      
      // FIXME: not strictly depending on Fsc...
      if( norm.equals(TVNorms.ntsc) ){
	 bt.setADelay(0x70);
	 bt.setBDelay(0x5d);
      }
      else if( norm.equals(TVNorms.pal) ){
	 bt.setADelay(0x7f);		/* FIXME:
					 * per calculation it should be
					 * 0x87 */
	 bt.setBDelay(0x72);
      }
      else 
	throw new Error("unknown setting of ADelay, BDelay"+norm.name);
      
      bt.setFormat(norm.btFormat);
      
      
      /* setup geometry for full capture */
      
      bt.setGeometryOdd (0x0000, norm.xoff, norm.width,
			 0x0000, norm.yoff, norm.height);
      bt.setGeometryEven(0x0000, norm.xoff, norm.width,
			 0x0000, norm.yoff, norm.height);
   }
   
   public void setFsc(int frequ){
      Debug.out.println("GenericBT878.setFsc: fsc="+frequ +
			" fscNTSC="+(BT878PLL.fsc8NTSC/8)+
			" fscPAL="+(BT878PLL.fsc8PAL/8) );
      
      int fxt0 = getXT0Frequ();
      BT878PLL pll = null;
      
      frequ *= 8;
      if( frequ == fxt0 ){
	 // do nothing (assuming XT0 is at right frequency)
      }
      else {
	 if( useHardcodedPLLs ){
	    if( fxt0 == BT878PLL.fsc8NTSC && frequ == BT878PLL.fsc8PAL ){
	       /* initialize PLL to PAL Fsc*8 (bt-manual S.74) */
	       pll = new BT878PLL(true,		/* PLL_X */
				  0x0e,		/* PLL_I */
				  0xdcf9,	/* PLL_F */
				  false);	/* PLL_C */
	    }
	    else 
	      throw new Error("unsupported PLL frequency (debugging mode!): "+frequ);
	 }
	 else {
	    /* FIXME: how to use dynamic dividers ? */
	    pll = new BT878PLL(fxt0, frequ, 12);
	 }
      }
      
      // setup input clock now
      if( pll == null ){		/* use XT0 clock */
	 // put PLL to sleep
	 bt.setPLL(0, 0x0, 0x0, 0);
	 bt.setTGCKI(bt.TGCKI_XTAL);
	 Debug.out.println("disabled PLL, using XT0");
      }
      else {
	 bt.setPLL(pll);
	 
	 int tries = lockPLL();
	 Debug.out.println("PLL stabilized after "+tries+" tries");
	 bt.setTGCKI(bt.TGCKI_PLL);
	 
	 int f = getPLLFrequ();
	 Debug.out.println("PLL is at "+f+", should be "+frequ+" delta="+(frequ-f));
      }
   }
   
   /*------------------------------------------------------------------*/
   /*- geometry interface                                             -*/
   /*------------------------------------------------------------------*/

   public BT878GeometryInterface getGeometryInterface(){
      return geometryInterface;
   }
   
   public void setGeometryOdd(int hscale, int hdelay, int hactive, 
			      int vscale, int vdelay, int vactive){
      bt.setGeometryOdd(hscale, hdelay, hactive,
			vscale, vdelay, vactive);
   }
   
   public void setGeometryEven(int hscale, int hdelay, int hactive, 
			       int vscale, int vdelay, int vactive){
      bt.setGeometryEven(hscale, hdelay, hactive,
			 vscale, vdelay, vactive);
   }
   
   public int getHScaleOdd(){
      return bt.getHScaleOdd();
   }
   public int getHDelayOdd(){
      return bt.getHDelayOdd();
   }
   public int getHActiveOdd(){
      return bt.getHActiveOdd();
   }
   
   public int getVScaleOdd(){
      return bt.getVScaleOdd();
   }
   public int getVDelayOdd(){
      return bt.getVDelayOdd();
   }
   public int getVActiveOdd(){
      return bt.getVActiveOdd();
   }
   
   public int getHScaleEven(){
      return bt.getHScaleEven();
   }
   public int getHDelayEven(){
      return bt.getHDelayEven();
   }
   public int getHActiveEven(){
      return bt.getHActiveEven();
   }
   
   public int getVScaleEven(){
      return bt.getVScaleEven();
   }
   public int getVDelayEven(){
      return bt.getVDelayEven();
   }
   public int getVActiveEven(){
      return bt.getVActiveEven();
   }
   

   /*------------------------------------------------------------------*/
   /*- picture output and capture control                             -*/
   /*------------------------------------------------------------------*/
   
   
   public void setFramebufferOdd(PackedFramebuffer framebuffer){
      setFramebuffer(0, framebuffer);
   }
   public void setFramebufferEven(PackedFramebuffer framebuffer){
      setFramebuffer(1, framebuffer);
   }
   protected void setFramebuffer(int fno, PackedFramebuffer framebuffer){

      if( framebuffer != null ){	/* register framebuffer */
	 passive[fno].setFramebuffer(framebuffer);
      }
      else {				/* unregister framebuffer */
	 if( active[fno].isValid() )
	   throw new Error("framebuffer is still active.");
	 passive[fno].setFramebuffer(null);
      }
   }
   
   
   public void setClippingOdd(ClippingRectangle clippings[]){
      setClippings(0, clippings);
   }
   public void setClippingEven(ClippingRectangle clippings[]){
      setClippings(1, clippings);
   }
   public void setClipping(PackedFramebuffer framebuffer, ClippingRectangle clippings[]){
      int set = 
	passive[0].setClippings(framebuffer, clippings) +
	passive[1].setClippings(framebuffer, clippings);
      if( set == 0 )
	throw new NoFramebufferRegistered();
   }
   protected void setClippings(int fno, ClippingRectangle clippings[]){
      if( !passive[fno].isValid() && !active[fno].isValid() )
	throw new NoFramebufferRegistered();
      passive[fno].setClippings(passive[fno].getFramebuffer(), clippings);
   }
   
   public void prepareOddField(){
      prepareField(0);
   }
   public void prepareEvenField(){
      prepareField(1);
   }
   protected void prepareField(int fno){
      
      PackedFramebuffer framebuffer = null;
      if( (framebuffer = passive[fno].getFramebuffer()) == null ){
	 if( (framebuffer = active[fno].getFramebuffer()) == null )
	   throw new NoFramebufferRegistered();
      }
      
      int synctype = fno == 0 ? RISC.FIFO_VRO : RISC.FIFO_VRE;
      
      FieldBlockCreator creator;
      if( passive[fno].needRelocations() )
	  creator = RISCFieldBlockReloc.getFieldCreator((MemoryManager)naming.lookup("MemoryManager")); /*RISCFieldBlockManager.getInstance());*/
      else 
	  creator = RISCFieldBlock.getFieldCreator((MemoryManager)naming.lookup("MemoryManager")); /*RISCFieldBlockManager.getInstance());*/
      
      RISCFieldBlock rfb;
      ClippingRectangle clippings[] = passive[fno].getClippings();
      if( clippings == null )
	rfb = RISCCoder.renderRectangleWithOutput(creator, synctype, framebuffer);
      // rfb = RISCCoder.renderDirectRectangle(creator, synctype, framebuffer);
      else 
	rfb = ScanlineClippingRasterizer.rasterizeField(creator, synctype, framebuffer, clippings);
      
      // free old RISC Block if possible
      if( passive[fno].getRISC() != active[fno].getRISC() )
	irqHandler.freeOnNextFieldEnd(fno, passive[fno].getRISC().riscbuffer);
      
      passive[fno].setRISC(rfb);
   }
   
   
   public void setOddFieldActive(boolean activate){
      setFieldActive(0, activate);
   }
   public void setEvenFieldActive(boolean activate){
      setFieldActive(1, activate);
   }
   public void setFieldsActive(boolean oddActive, boolean evenActive){
      setFieldActive(0, oddActive);
      setFieldActive(1, evenActive);
   }
   protected void setFieldActive(int fno, boolean activate){
      if( !passive[fno].isValid() && !active[fno].isValid() )
	throw new NoFramebufferRegistered();
      if( passive[fno].needUpdate() )
	prepareField(fno);
      
      if( activate ){			/* activate field */
	 PackedFramebuffer framebuffer = null;
	 if( (framebuffer = passive[fno].getFramebuffer()) == null ){
	    if( (framebuffer = active[fno].getFramebuffer()) == null )
	      throw new NoFramebufferRegistered();
	 }
	 
	 if( fno == 0 )
	   bt.setColorOdd(getColorFormat(framebuffer.depth()));
	 else if( fno == 1 )
	   bt.setColorEven(getColorFormat(framebuffer.depth()));
	 
	 /*
	  * Note: The active RISC code block will be replaced now. But
	  * because the BT chip may use this code right now (for the last
	  * time, thouh). So remember some things:
	  * 
	  *  - Don't reuse the old memory block immediatly.
	  *  - Remember, that the video data might be written to the old location.
	  * 
	  * The whole issue isn't one if the field is switched in the the
	  * corresponding interrrupt notifier.
	  */
	 
	 // free old RISC Block if possible
	 if( active[fno].getRISC() != null && passive[fno].getRISC() != active[fno].getRISC() )
	   irqHandler.freeOnNextFieldEnd(fno, active[fno].getRISC().riscbuffer);

	 active[fno].copyFrom(passive[fno]);
	 riscjumps.setFieldBlock(fno, active[fno].getRISC());
	 riscjumps.switchField(true, fno);
	 
//	 Debug.out.println(getBoardName()+": activated field "+fno+": 0x"+hex(active[fno].getRISC().getStartAddress())+" - 0x"+hex(active[fno].getRISC().getRiscPC()));
      }
      else {				/* deactivate field */
	 // FIXME: untested
	 riscjumps.switchField(false, fno);
	 riscjumps.setFieldBlock(fno, null);
	 
	 active[fno].setFramebuffer(null);
	 active[fno].setRISC(null);
      }
   }
   
   public void captureOn(){
      
      // which fields should be captured?
      int capturebits = 0;
      if( active[0].isValid() )
	capturebits |= bt.CAPTURE_ODD;
      if( active[1].isValid() )
	capturebits |= bt.CAPTURE_EVEN;
      
      if( capturebits == 0 )
	throw new Error(getBoardName()+": no active fields");
      
      
      // setup interrups for capture
      if (allowInterrupts) {
	  /* clear all interrupt flags */
	 bt.clearIntStat(bt.INT_STAT_MASK);
	 bt.setIntMask(bt.getIntMask()
//		       | bt.FDSR | bt.SCERR	// indicating sync problems
//		       | bt.INT_VPRES | bt.INT_HLOCK	
//		       | bt.INT_STAT_PCI 	// indicating PCI problems
		       | bt.VSYNC	/* needed for Memory tracking */
		       | bt.RISCI		// watchdog interrupt
		       );
      }
      else 
	bt.setIntMask(0xffffffff, 0);
      
      // load RISC Code
      bt.setRiscStartAdd(riscjumps.getStartAddress());
      
      // enable FIFOs and RISC code
      bt.setDMACtl(bt.RISC_ENABLE | bt.FIFO_ENABLE);
      
      // enable capture
      bt.setCapCtl(capturebits);
   }
      
   public void captureOff(){
      bt.setDMACtl(bt.RISC_ENABLE| bt.FIFO_ENABLE, 0);
      bt.setCapCtl(bt.CAPTURE_ODD | bt.CAPTURE_EVEN, 0);
   }

   /*------------------------------------------------------------------*/
   /*- notification support                                           -*/
   /*------------------------------------------------------------------*/

   public void addNotifierOdd (Notifier notifyObject){
      notifiersOdd.addNotifier(notifyObject);
   }
   public void addNotifierEven(Notifier notifyObject){
      notifiersEven.addNotifier(notifyObject);
   }

//   void addNotifierOdd(TriggerPoint triggerTime, int numberOfTriggers, Notifier notifyObject);
//   void addNotifierEven(TriggerPoint triggerTime, int numberOfTriggers, Notifier notifyObject);

   public void deleteNotifier(Notifier notifyObject){
      notifiersOdd.removeNotifier(notifyObject);
      notifiersEven.removeNotifier(notifyObject);
   }
   
   // called by Interrupt Handler
   void callHandler(int istatus, int imask, int dstatus){
      int mi = istatus & imask;
      if( (mi & (bt.VSYNC | bt.RISCI)) != 0 ){
	 if( (istatus & bt.INT_FIELD) == 0 ) // odd field
	   notifiersOdd.notifyObjects();
	 else			// even field
	   notifiersEven.notifyObjects();
      }
   }
   
   /********************************************************************/
   /* internal support stuff                                           */
   /********************************************************************/
   
   int getPLLFrequ(){
      return bt.getPLL().toInteger();
   }
   
   /**
    * Lock internal PLL by polling and resetting DSTATUS:PLOCK .
    * The PLL registers must be set apropriatly.
    * @return Number of tries before the PLL got locked. (usefull for debugging only)
    */
   int lockPLL(){
      int tend = ZeroDomain.clock.getTimeInMillis() + 500;   /* timeout in 500 ms */
      int tries;
      for(tries=0; ; ++tries){
	 if( ZeroDomain.clock.getTimeInMillis() >= tend )	/* ok, lets timeout */
	   throw new Error(getBoardName()+": can't stabilize PLL");
	 if( bt.getDStatus(bt.PLOCK) == 0 )
	   break;
	 bt.clearPLock();
	 msleep(10);
      }
      return tries;
   }
   
   /**
    * Calculate the color format for the depth of a framebuffer.
    */
   int getColorFormat(int depth){
      switch(depth){
       case 8:
	 return bt.COLOR_FMT_RGB8;
       case 15:
	 return bt.COLOR_FMT_RGB15;
       case 16:
	 return bt.COLOR_FMT_RGB16;
       case 24:
	 return bt.COLOR_FMT_RGB24;
       case 32:
	 return bt.COLOR_FMT_RGB32;
      }
      throw new Error(getBoardName()+": unsupportet color depth: "+depth);
   }
   
   /* check for stable video signal */
   public boolean isVideoSignalStable(int millis){
      boolean retval;
      
      int oldmask = bt.getIntMask();
      bt.setIntMask(0);
      
      bt.clearIntStat(bt.INT_VPRES|bt.INT_HLOCK);
      msleep(millis);
      
      int dstatus = bt.getDStatus(bt.HLOC|bt.PRES);
      int istatus = bt.getIntStat(bt.INT_HLOCK|bt.INT_VPRES);
      if( istatus == (bt.INT_HLOCK|bt.INT_VPRES) 	/* dstatus has changed  */
	 || dstatus != (bt.HLOC|bt.PRES) )   		/* no stable signal */
	retval = false;
      else 
	retval = true;
      
      bt.setIntMask(bt.INT_STAT_MASK, oldmask);
      return retval;
   }
   

   /********************************************************************/
   /* debugging stuff                                                  */
   /********************************************************************/
   
   final static StatusFlag pciStatusFlags[] = {
      new StatusFlag(PCI.CMD_STATUS_PARITY_ERROR, "no D_PERR", "PERR"),
      new StatusFlag(PCI.CMD_STATUS_SYSTEM_ERROR, "no SERR", "SERR"),
      new StatusFlag(PCI.CMD_STATUS_MASTER_ABORT, "no R_MABORT", "R_MABORT"),
      new StatusFlag(PCI.CMD_STATUS_TARGET_ABORT, "no R_TABORT", "R_TABORT"),
      new StatusFlag(PCI.CMD_STATUS_SIG_TARGET_ABORT, "no S_TABORT", "S_TABORT"),
      new StatusFlag(PCI.CMD_STATUS_PARITY_ERROR_R, "no PERR_R", "PERR_R"),
      new StatusFlag(PCI.CMD_STATUS_FB2B_CAP, "no FB2B", "FB2B"),
   };
   
   final static StatusFlag pciCommandFlags[] = {
      new StatusFlag(PCI.CMD_COMMAND_SERR_ENABLE, "no SERR_ENA", "SERR_ENA"),
      new StatusFlag(PCI.CMD_COMMAND_PERR_ENABLE, "no PERR_ENA", "PERR_ENA"),
      new StatusFlag(PCI.CMD_COMMAND_BM_ENABLE, "no BusMaster", "BusMaster"),
      new StatusFlag(PCI.CMD_COMMAND_MEM_SPACE, "no MemSpace", "MemSpace")
   };

   
   
   public void setIrqWatchBuffer(PackedFramebuffer framebuffer){
      Debug.out.println("using framebuffer for IRQ display: "+framebuffer);
      if( irqHandler != null )
	irqHandler.setFramebuffer(framebuffer);
      else
	Debug.out.println("WARNING: no irqHandler installed!");
   }
   
   public String pciStatus(int options){
      short status = pcidevice.getStatus();
      String str = "(0x"+hex(status)+") ";
      str += StatusFlag.decode(status, pciStatusFlags, options);
      return str;
   }
   
   public String pciCommand(int options){
      short cmd = pcidevice.getCommand();
      String str = "(0x"+hex(cmd) + ") ";
      str += StatusFlag.decode(cmd, pciCommandFlags, options);
      return str;
   }
   
   public void registerTest(DebugPrintStream out){
      bt.registerTest(out);
   }
   
   public void dumpStatus(){
      dumpStatus(StatusFlag.STATUS_ALL);
   }
   
   public void dumpStatus(int options){
      
      // general status registers
      Debug.out.println("PCI status : "+pciStatus(options));
      Debug.out.println("PCI command: "+pciCommand(options));
      Debug.out.println("BT  status : "+bt.status(options));
      Debug.out.println("IRQ status : "+bt.interruptStatus(options));
      Debug.out.println("RISC PC    : 0x"+hex(bt.getRiscCount()) + ", RISC Start : 0x"+hex(bt.getRiscStartAdd()));
      
      // RISC Code
      Debug.out.println();
      riscjumps.dump(Debug.out);
      
      Debug.out.println("RISCFieldBlockManager: " + RISCFieldBlockManager.getInstance().toString());
   }
   
   public boolean pciCheck(){
      boolean retval = true;
      int val;
      
      Debug.out.println();
      Debug.out.println("Checking PCI for proper values...");
      
      /* PCI command register */
      
      val = pcidevice.readConfig(PCI.REG_STATCMD) & PCI.COMMAND_MASK;
      Debug.out.println("PCI command: " + pciCommand(StatusFlag.STATUS_ALL));
      if( val == 0 ){
	 Debug.out.println("WARNING: PCI device disabled!");
	 retval = false;
      }
      
      /* PCI latency timer register, PCI interrupt */
      
      val = pcidevice.readConfig(PCI.REG_LGII);
      int max_lat = ( val & PCI.MAXLATENCY_MASK ) >> PCI.MAXLATENCY_SHIFT;
      int min_gnt = ( val & PCI.MINGNT_MASK ) >> PCI.MINGNT_SHIFT;
      int irq	  = ( val & PCI.INTERRUPTLINE_MASK ) >> PCI.INTERRUPTLINE_SHIFT;
      
      Debug.out.println("Max Lat : " + max_lat);
      Debug.out.println("Min Gnt : " + min_gnt);
      Debug.out.println("Int line: " + irq);
      
      val = pcidevice.getLatencyTimer();
      Debug.out.println("Latency Timer : " + val );
      if( (val == 0) || (val < min_gnt) ){
	 Debug.out.println("WARNING: latency timer is too low!");
	 retval = false;
      }
      
      if( irq == 0 ){
	 Debug.out.println("WARNING: no IRQ set in PCI header!");
	 retval = false;
      }
      
      /* PCI base address */
      
      val = pcidevice.getBaseAddress(0) & PCI.BASEADDRESS_MEM_MASK;
      Debug.out.println("PCI Base Address: 0x"+ hex(val));
      if( val == 0 ){
	 Debug.out.println("Warning: PCI Base Address not valid!");
	 retval = false;
      }
      
      return retval;
   }
   
   /********************************************************************/
   /* Interface to derived classes                                     */
   /********************************************************************/
   
   public abstract String getBoardName();
   public abstract int getXT0Frequ();
   
   /********************************************************************/
   /* shorthands                                                       */
   /********************************************************************/
   
   void msleep(int milis){
      MySleep.msleep(milis);
   }
   
   String hex(byte v){
      return Hex.toHexString(v);
   }
   
   String hex(short v){
      return Hex.toHexString(v);
   }

   String hex(int v){
      return Hex.toHexString(v);
   }
   
   boolean isAligned(int address){
      return (address & 0x3) == 0;
   }
   

   /********************************************************************/
   /* Inner Classes                                                    */
   /********************************************************************/

   class FieldDescr implements Cloneable {
      PackedFramebuffer framebuffer;
      ClippingRectangle clippings[];
      boolean relocatable;
      RISCFieldBlock risccode;
      int curbase = 0;
      boolean needUpdate = true;	/* meaning: RISC code may be outdated */
      
      FieldDescr subfields[];
      
      FieldDescr(){
      }
      
      FieldDescr(PackedFramebuffer framebuffer) {
	 this.framebuffer = framebuffer;
      }
            
      void setFramebuffer(PackedFramebuffer framebuffer){
	 this.framebuffer = framebuffer;
	 this.needUpdate = true;
	 if( framebuffer instanceof LinkedPackedFramebuffer ){
	    // Note: The code tries to reuse already allocated subfields. With a
	    // working garbagge collector this wouldn't be a problem. To avoid
	    // strange side effects this should be undone ASAP. (see also method "copyFrom")
	    LinkedPackedFramebuffer linked = (LinkedPackedFramebuffer)framebuffer;
	    if( subfields == null || subfields.length != linked.getNHoriz() * linked.getNVert() )
	      subfields = new FieldDescr[ linked.getNHoriz() * linked.getNVert() ];
	    for(int i=0; i<subfields.length; ++i){
	       if( subfields[i] == null )
		 subfields[i] = new FieldDescr(linked.getFramebuffer(i));
	       else
		 subfields[i].setFramebuffer(linked.getFramebuffer(i));
	    }
	 }
	 else {
	    subfields = null;
	 }
      }
      PackedFramebuffer getFramebuffer(){
	 return framebuffer;
      }
      boolean isValid(){
	 return framebuffer != null;
      }
      
      int setClippings(PackedFramebuffer clippbuffer, ClippingRectangle clippings[]){
	 if( clippbuffer.equals( framebuffer ) ){
	    this.clippings = clippings;
	    this.needUpdate = true;
	    return 1;
	 }
	 if( framebuffer instanceof LinkedPackedFramebuffer ){
	    int found = 0;
	    for(int i=0; i<subfields.length; ++i)
	      found += subfields[i].setClippings(clippbuffer, clippings);
	    if( found > 0 )
	      this.needUpdate = true;
	    return found;
	 }
	 return 0;
      }
      ClippingRectangle[] getClippings(){
	 return clippings;
      }
      
      int setRelocatable(PackedFramebuffer relocbuffer, boolean relocatable){
	 if( relocbuffer.equals(framebuffer) ){
	    this.relocatable = relocatable;
	    this.needUpdate = true;
	    return 1;
	 }
	 if( framebuffer instanceof LinkedPackedFramebuffer ){
	    Debug.out.println("**** setRelocatable: LinkedPackedFramebuffer");
	    int found = 0;
	    for(int i=0; i<subfields.length; ++i)
	      found += subfields[i].setRelocatable(relocbuffer, relocatable);
	    if( found > 0 )
	      this.needUpdate = true;
	    return found;
	 }
	 return 0;
      }
      boolean needRelocations(){
	 return relocatable;
      }
      
      void setRISC(RISCFieldBlock risccode){
	 this.risccode = risccode;
	 this.needUpdate = false;
      }
      RISCFieldBlock getRISC(){
	 return risccode;
      }
      
      boolean needUpdate(){
	 return needUpdate;
      }
      
      int getCurrentBase(){
	 if( curbase != 0 )
	   return curbase;
	 if( framebuffer != null )
	   return framebuffer.startAddress();
	 return 0;
      }
      
      void setCurrentBase(int newbase){
	 curbase = newbase;
      }
      
      
      // make deep copy
      void copyFrom(FieldDescr other){
	 this.framebuffer = other.framebuffer;
	 
	 if( other.clippings == null )
	   this.clippings = null;
	 else {
	    this.clippings = new ClippingRectangle[other.clippings.length];
	    for(int i=0; i< clippings.length; ++i)
	      this.clippings[i] = (ClippingRectangle)other.clippings[i].clone();
	 }
	 this.relocatable = other.relocatable;
	 this.risccode = other.risccode;
	 this.needUpdate = other.needUpdate;
	 
	 // Note: The code tries to reuse already allocated subfields. With a
	 // working garbagge collector this wouldn't be a problem. To avoid
	 // strange side effects this should be undone ASAP. (see also method "setFramebuffer")

	 if( other.subfields == null )
	   this.subfields = null;
	 else {
	    if( this.subfields == null || this.subfields.length != other.subfields.length )
	      this.subfields = new FieldDescr[other.subfields.length];
	    for(int i=0; i< subfields.length; ++i){
	       if( this.subfields[i] == null )
		 this.subfields[i] = (FieldDescr) other.subfields[i].clone();
	       else
		 subfields[i].copyFrom(other.subfields[i]);
	    }
	 }
      }
      
      public Object clone() {
	 try {
	    return super.clone();
	 }
	 catch(CloneNotSupportedException e){}
	 return null;
      }   
   }					/* FieldDescr */
   
   /********************************************************************/
   
   class NotifierList implements Runnable {
      Vector notifiers;
       CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
       CPUState notifyThread;

      NotifierList(){
	 this.notifiers = new Vector();
	 new Thread(this).start();
      }
      
      void addNotifier(Notifier n){
	 if( notifiers.contains(n) ){
	    Debug.out.println(getBoardName()+".NotifierList.addNotifier: Notifier already registered: "+n);
	    return;
	 }
	 notifiers.addElement(n);
      }
      
      void removeNotifier(Notifier n){
	 int i;
	 while( (i=notifiers.indexOf(n)) != -1 )
	   notifiers.removeElementAt(i);
      }
      
       void notifyObjects(){ // called by IRQ thread
	   if (! cpuManager.unblock(notifyThread)) {
	       //Debug.out.println("GenericBT878: LOST NOTIFICATION");
	   }
      }

      public void run(){
	 Notifier n;
	 notifyThread = cpuManager.getCPUState();
	 for(;;) {
	     cpuManager.block();
	     int size = notifiers.size();
	     for(int i=0; i<size; ++i){
		 n = (Notifier)notifiers.elementAt(i);
		 try {
		     n.notifyEvent();
		 }
		 catch(Throwable t){
		     Debug.out.println(getBoardName()+": "+n+": caught " + t);
		 }
	     }
	 }
      }
   }					/* NotifierList */
}

