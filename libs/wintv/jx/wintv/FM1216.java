package jx.wintv;

import jx.zero.*;

public class FM1216 extends MUXPin implements Tuner, IrqSubHandler, Service {
   final static FreqRange frequRanges[] = {
      new FreqRange( 87500000, 108000000),	/* FM radio band */
      new FreqRange( 48250000, 170000000),	/* low band */
      new FreqRange(170000000, 450000000),	/* mid band */
      new FreqRange(450000000, 855250000)	/* high band */
   };
   final static int ports[] = {	/* same order as frequRanges !!! */
      0xa4,				/* FM radio band */
      0xa0,				/* low band */
      0x90,				/* mid band */
      0x30				/* high band */
   };
   
   I2CBus i2cbus;
   int i2cdev;
   
   int dumpTicks = 0;			/* debugging only */
   
   
   FM1216(BT878Video bt, int mux, I2CBus i2cbus, int i2cdev){
      super(bt, mux);
      this.i2cbus = i2cbus;
      this.i2cdev = i2cdev;
   }
   
   /********************************************************************/
   /* public interface "Tuner"                                         */
   /********************************************************************/
   
   public FreqRange[] getFrequRanges(){
      return frequRanges;
   }
   
   public void setFrequ(int hz){
      /* 
       * f_{RF(pc)} means Channel Picture Carrier, f_{IF(pc)} means
       * Intermediate Frequency picture carrier and is 38.9 MHz for the
       * FM1216 (noted above channel tables and at page 3) 
       */
      final int frf = 3890*16/100;
      
      int cb = 0;			/* controll byte */
      int pb = 0;			/* ports byte */
      int n = 0;			/* divider */
      
      cb =
	0x80 |			/* control byte identifier */
	0x40 |			/* fast tune */
	0x08 |			/* t = 001  */
	0x06;			/* normal pciture search */
      
      pb = getPortsByte(hz);
      
      hz /= 10000;
      n = hz*16/100 + frf;
      
      int n1 = (n & 0x7f00) >> 8;
      int n2 = (n & 0x00ff);
      
      i2cbus.lock();
      try {
	 if( !i2cbus.write(i2cdev, cb, pb) )
	   Debug.out.println("FM1216: failed writing CP, PB!");
	 if( !i2cbus.write(i2cdev, n1, n2) )
	   Debug.out.println("FM1216: failed writing N1, N2!");
      }
      finally {
	 i2cbus.unlock();
      }
   }
   
   public boolean isFrequLocked(){
      throw new NotImpl("isFrequLocked");
   }
   
   public void waitForLock(){
      throw new NotImpl("waitForLock");
   }
   
   /********************************************************************/
   /* internal support stuff                                           */
   /********************************************************************/
   
   int getPortsByte(int hz){
      Debug.assert(frequRanges.length == ports.length, "different number of frequency ranges and ports");
      for(int i=0; i<ports.length; ++i){
	 if(frequRanges[i].low <= hz &&
	    frequRanges[i].high>= hz )
	   return ports[i];
      }
      throw new Error("frequency out of supported range: "+hz);
   }
   
   public int getInterruptBits(){
      return 0;				/* FIXME: always with any other interrupt ;-) */
   }
   
   public void callHandler(int istatus, int imaks, int dstatus){/* public only due to interface */
      if( ++dumpTicks < 39 )
	return;
      dumpTicks = 0;
      
      int i;
      i2cbus.lock();
      try {
	 i = FM1216.this.i2cbus.read(FM1216.this.i2cdev);
      }
      finally {
	 i2cbus.unlock();
      }
      
      if( i < 0 )
	Debug.out.println("FM1216: failed to read status!");
      else 
	Debug.out.println("FM1216 status: 0x"+Hex.toHexString((byte)i));
   }
}

