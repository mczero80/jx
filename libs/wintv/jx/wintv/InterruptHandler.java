package jx.wintv;

import jx.zero.*;
import jx.zero.debug.*;
import jx.framebuffer.PackedFramebuffer;
import java.util.Vector;

class InterruptHandler implements FirstLevelIrqHandler, Service {
   GenericBT878 board;
   BT878Video bt;
   int irqno;
   IrqHandler irqDEPHandle = null;
   Thread irqThread = null;
   
   PackedFramebuffer framebuffer = null;
   PFBTools tool;
   int x, y;
   int curpixelOffset;
   boolean invertExit = false;
   final static short COLOR_ENTRY = (short)0xF800;
   final static short COLOR_EXIT  = (short)0x001F;
   final static short COLOR_EXIT2 = (short)0x07e0;
   
   int savedIntStat = 0;
   
   Vector memblocks[] = {		/* FIXME: access should be synchronized */
      new Vector(),
      new Vector()
   };
   
   InterruptHandler(GenericBT878 board, int irqno){
      this.board = board;
      this.bt    = board.bt;
      this.irqno = irqno;
   }
   
   public void install(){
      // first: NO interrupts please!
      bt.setIntMask(BT878Video.INT_STAT_MASK, 0);
      
      /*
      irqDEPHandle = 
	(IrqHandler)ZeroDomain.promoteDEP(this, "jx/zero/IrqHandler");
      irqThread = new Thread("BT878IrqThread(unused)") {
	 public void run() {
	    ZeroDomain.cpuMgr.receive(irqDEPHandle);
	 }
      };
      irqThread.start();
      */

      ZeroDomain.irqMgr.installFirstLevelHandler(irqno, this);
      ZeroDomain.irqMgr.enableIRQ(irqno);
      
      Debug.out.println("installed handler for IRQ "+irqno);
   }
   
   public void setFramebuffer(PackedFramebuffer framebuffer){
      this.framebuffer = framebuffer;
      this.tool = new PFBTools(framebuffer);
      
      x = y = 0;
      curpixelOffset = tool.getPixelOffset16(x, y);
   }
   
   void setPixel(short color, boolean advance){
      if( invertExit  && color == COLOR_EXIT )
	color = COLOR_EXIT2;
      framebuffer.memObj().set16(curpixelOffset, color);
      if( !advance )
	return;
      
      ++x;
      if( x >= framebuffer.width() ){
	 x = 0;
	 ++y;
      }
      if( y >= framebuffer.height() ){
	 x = y = 0;
	 invertExit = !invertExit;
      }
      curpixelOffset = tool.getPixelOffset16(x, y);
   }

   public void interrupt() {
      
      if(framebuffer != null)
	setPixel(COLOR_ENTRY, false);
      
      int istatus = bt.getIntStat(BT878Video.INT_STAT_MASK);
      int imask	  = bt.getIntMask();
      int mi      = istatus & imask;
      int dstatus = bt.getDStatus();
      
      if( mi == 0x0 ){			/* not my interrupt (eg. shared IRQ line) */
	 if( framebuffer != null )
	   setPixel(COLOR_ENTRY, true);
	 return;
      }
      
      savedIntStat &= ~bt.RISCS;	/* clear RISCS bits */
      savedIntStat |= istatus;		/* merge other bits */
      bt.clearIntStat(mi);		/* clear status in register (only INT enabled bits)*/
      
      
      if( (mi & (bt.VSYNC)) != 0 ){
	 if( (istatus & bt.INT_FIELD) == 0 )	/* odd field */
	   freeBlocks(0);
	 else					/* even field */
	   freeBlocks(1);
      }
      
      // call other subhandlers
      // FIXME: the handler are called with *disabled* Interrupts. 
      // FIXME: debugging if( (mi & bt.VSYNC) != 0 )
      board.callHandler(istatus, imask, dstatus);
      
      if(framebuffer != null)
	setPixel(COLOR_EXIT, true);
   }
   
   int getSavedStatus(){
      if( !GenericBT878.allowInterrupts )
	return bt.getIntStat(bt.INT_STAT_MASK);
      return savedIntStat;
   }
   void clearStatus(int mask){
      bt.clearIntStat(mask & ~bt.I2CDONE);/* leave I2CDONE, needed by I2CBus */
      savedIntStat = 0;
   }
   
   
   void freeOnNextFieldEnd(int field, Memory block){
      memblocks[field].addElement(block);
   }
   
   void freeBlocks(int field){
      Vector v = memblocks[field];
      int size = v.size();
      for(int i=size-1; i>=0; --i){
	RISCFieldBlockManager.getInstance().freeBlock((Memory)v.elementAt(i));
	 v.removeElementAt(i);
      }
   }
}

