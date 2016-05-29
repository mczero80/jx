package jx.wintv;

import jx.zero.Service;
import jx.zero.Debug;

public class IRReceiverImpl implements IRReceiver, Service {
   
   /* first byte in message */
   final static byte phase1	= (byte)0xc0;
   final static byte phase2	= (byte)0xe0;
   
   /* third byte in message */
   final static byte lastbyte1	= (byte)0xc3;   /* used by internal onboard IR chip */
   final static byte lastbyte2  = (byte)0x02;   /* used by external IR chip */
   
   /* debugging options */
   final static boolean verbose = false;
   
   
   I2CBus i2cbus;
   int i2cdevice;
   RCKey keytable[];
   
   int   lastPhase = 0;
   RCKey lastKey   = null;
   
   IRReceiverImpl(I2CBus i2cbus, int i2cdevice, RCKey keytable[]){
      this.i2cbus = i2cbus;
      this.i2cdevice = i2cdevice;
      this.keytable = keytable;
   }
   
   /**
    * Poll the IR receiver.
    * 
    * @return Special key "lastKeyStillPressed" in which case you can get
    *     the value of that key with "lastKey()" or "null" if there is no key
    *     pressed or there are errors during transmission.
    * 
    */
   public RCKey poll(){
      byte data[] = new byte[3];
      int i=0;
      
      i2cbus.lock();
      try {
	 i = i2cbus.mread(i2cdevice, data, 0, data.length);
      }
      finally {
	 i2cbus.unlock();
      }
      
      // read a complete telegram?
      if( i != data.length ){
	 if( verbose )
	   Debug.out.println("incomplete IR telegram ("+i+" bytes): " + Hex.toHexDump(data));
	 return null;
      }
      
      // is it a "wellformed" telegram?
      if( (data[0] != phase1 && data[0] != phase2 && data[0] != 0) || ( data[2] != lastbyte1 && data[2] != lastbyte2) ){
	 if( verbose )
	   Debug.out.print("format mismatch: " + Hex.toHexDump(data));
	 return null;
      }
      
      // key pressed?
      if( data[0] == 0 ){
	 if( verbose )
	   Debug.out.print("no key pressed: " + Hex.toHexDump(data));
	 return null;
      }
      
      RCKey key = lookupKey(data[1]);
      if( key == null ){		/* unknown key */
	 if( verbose )
	   Debug.out.println("unknown key; telegram: "+Hex.toHexDump(data));
	 return null;
      }
      
      if( verbose )
	Debug.out.println("received valid key");
      
      if( key == lastKey && lastPhase == data[0] )
	return lastKeyStillPressed;
      
      lastKey = key;
      lastPhase = data[0];
      
      return key;
   }
   
   /**
    * Wait for the IR receiver to receive a key.
    * 
    * @return the id of the key, which might be "lastKeyStillPressed".
    */
   public RCKey getKey(){
      RCKey key;
      while( (key=poll()) == null ){
	 MySleep.msleep(100);
      }
      return key;
   }
   
   /**
    * Return the last key the IR receiver got during a "poll". This is
    * handy, if you forgot that key, or you received the special key
    * "lastKeyStillPressed".
    */
   public RCKey lastKey(){
      return lastKey;
   }
   
   RCKey lookupKey(byte code){
      RCKey key = null;
      for(int i=0; i<keytable.length; ++i){
	 key = keytable[i];
	 if( key.code == code )
	   return key;
      }
      return null;
   }
}
