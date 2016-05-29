package jx.wintv;

import jx.zero.Debug;
import jx.zero.Naming;
import jx.zero.InitialNaming;
import jx.zero.LookupHelper;
import jx.zero.CPUManager;
import jx.timer.TimerManager;


class BT878I2CBus implements I2CBus {
    static Naming naming = InitialNaming.getInitialNaming();
   final int HWModeFlags = 
     BT878Video.I2CMODE |			/* hardware mode */
     BT878Video.I2CSCL |			/* 1 in HW mode */
     BT878Video.I2CSDA;				/* 1 in HW mode */
   
   BT878Video bt;
   
   Thread lockedBy = null;
   
    TimerManager timerManager;
    CPUManager cpuManager;

   BT878I2CBus(BT878Video bt){
      this.bt = bt;
      timerManager = (TimerManager)LookupHelper.waitUntilPortalAvailable(naming, "TimerManager");
      cpuManager = (CPUManager)LookupHelper.waitUntilPortalAvailable(naming, "CPUManager");

   }
   
   void checkLock(){
      if( lockedBy != Thread.currentThread() )
	throw new Error("tried to access I2C bus without locking");
   }
   
   boolean waitForCompletion(){
       // wait for I2CDONE going high
       int timeOutCount = timerManager.getCurrentTimePlusMillis(1000);
       int status = bt.getIntStat(bt.I2CDONE | bt.RACK);
       while( (status&bt.I2CDONE) == 0 && timeOutCount > timerManager.getCurrentTime()) {
	   status = bt.getIntStat(bt.I2CDONE | bt.RACK);
	   cpuManager.yield();
       }
       if((status & bt.I2CDONE)==0){
	   Debug.out.println("BT878I2CBus: ****timeout****");
	   return false;
      }
      // return status of transaction
      return (status & bt.RACK) != 0;
   }
   
   byte getRByte(int reg){
      return (byte)((reg & bt.I2CDB2) >> bt.I2CDB2_OFFSET_BIT);
   }
   
   
   /********************************************************************/
   /* I2CBus Interface                                                 */
   /********************************************************************/
   
   public boolean write(int device, int data1, int data2){
      checkLock();
      
      int reg = HWModeFlags | bt.I2CW3BRA;
      reg |= (device & 0xfe) << bt.I2CDB0_OFFSET_BIT;	/* note: RW-flag == W */
      reg |= (data1 & 0xff) << bt.I2CDB1_OFFSET_BIT;
      reg |= (data2 & 0xff) << bt.I2CDB2_OFFSET_BIT;
      
      bt.clearIntStat(bt.I2CDONE);
      bt.setI2C(reg);
      return waitForCompletion();
   }
   
   public boolean write(int device, int data){
      checkLock();
      
      int reg = HWModeFlags;
      reg |= (device & 0xfe) << bt.I2CDB0_OFFSET_BIT;	/* note RW-flag == W */
      reg |= (data & 0xff) << bt.I2CDB1_OFFSET_BIT;
      
      bt.clearIntStat(bt.I2CDONE);
      bt.setI2C(reg);
      return waitForCompletion();
   }
   
   public int read(int device){
      checkLock();
      
      int reg = HWModeFlags;
      reg |= (device & 0xff | 0x01) << bt.I2CDB0_OFFSET_BIT;   /* note> RW-flag = R */
      
      bt.clearIntStat(bt.I2CDONE);
      bt.setI2C(reg);
      if( waitForCompletion() )
	return bt.getDB2();
      return -1;
   }
   
   /********************************************************************/
   
   public int mread(int device, byte data[], int off, int len){
      checkLock();
      
      int reg = HWModeFlags;
      
      reg |= (device & 0xff | 0x01) << bt.I2CDB0_OFFSET_BIT;   /* note: RW-flag = R */
      reg |= bt.I2CNOSTOP;		/* do not send STOP */
      reg |= bt.I2CW3BRA;		/* send ACK instead of NACK */
      
      for(int endoff = off+len; off < endoff; ++off){
	 if( endoff - off == 1 )	/* check for last byte */
	   reg &= ~(bt.I2CNOSTOP|bt.I2CW3BRA);
	 
	 bt.clearIntStat(bt.I2CDONE);
	 bt.setI2C(reg);
	 if( !waitForCompletion() )
	   return off+len-endoff;	/* return #bytes read so far */
	 int d = bt.getI2C();
	 
	 data[off] = getRByte(d);
	 
	 reg |= bt.I2CNOS1B;		/* no more STARTs */
      }
      return len;
   }
   
   /********************************************************************/
   
   public int mwrite(int device, byte data[], int off, int len){
      Debug.assert(len > 0, "no data");
      checkLock();
      
      // write first byte with device byte
      int reg = HWModeFlags;
      reg |= (device & 0xfe) << bt.I2CDB0_OFFSET_BIT;
      reg |= (data[off] & 0xff) << bt.I2CDB1_OFFSET_BIT;
      reg |= bt.I2CNOSTOP;
      
      bt.clearIntStat(bt.I2CDONE);
      bt.setI2C(reg);
      if( !waitForCompletion() )
	return 0;
      
      reg = HWModeFlags
	| bt.I2CNOSTOP 
	| bt.I2CNOS1B;
      
      for(int endoff=off+len, i=off+1; i<endoff; ++i){
	 if( endoff-i == 1 )		/* last byte */
	   reg &= ~bt.I2CNOSTOP;
	 
	 reg &= ~bt.I2CDB0;		/* FIXME: or DB1 */
	 reg |= (data[i] & 0xff) << bt.I2CDB0_OFFSET_BIT ;
	 
	 bt.clearIntStat(bt.I2CDONE);
	 bt.setI2C(reg);
	 if( !waitForCompletion() )
	   return i-off;
      }
      return len;
   }
   
   /********************************************************************/
   
   public boolean probe(int device){
      checkLock();
      return read(device) >= 0;
   }
   
   public void scanBus(){
      checkLock();
      
      int dev;
      for(dev =0; dev<256; dev+=2){
	 int data = read(dev+1);
	 if( data >= 0 )
	   Debug.out.println("found i2c device on 0x"+Hex.toHexString((byte)dev)+": 0x"+Hex.toHexString((byte)data));
      }
   }
   
   
   public synchronized void lock(){
      // FIXME
      /*
      while( lockedBy == null ){
	 try {
	    wait();
	 }
	 catch(InterruptedException e){}
      }
      busIsLocked = Thread.current();
       */
      while( lockedBy != null )
	;
      
      lockedBy = Thread.currentThread();
   }
   public /* synchronized */ void unlock(){
      // FIXME
      /*
      busIsLocked = null;
      notify();
       */
      lockedBy = null;
   }
}

