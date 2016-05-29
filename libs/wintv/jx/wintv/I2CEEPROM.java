package jx.wintv;

import jx.zero.*;
import jx.zero.debug.*;


class I2CEEPROM {
   I2CBus i2cbus;
   int device;
   
   I2CEEPROM(I2CBus i2cbus, int device){
      this.i2cbus = i2cbus;
      this.device = device;
   }
   
   public int read(int addr, byte data[], int off, int len){
      i2cbus.lock();
      try {
	 if( !i2cbus.write(device, addr) )
	   return -1;
	 return i2cbus.mread(device, data, off, len);
      }
      finally {
	 i2cbus.unlock();
      }
   }
   
   public int read(int addr){
      i2cbus.lock();
      try {
	 if( !i2cbus.write(device, addr) )
	   return -1;
	 return i2cbus.read(device);
      }
      finally {
	 i2cbus.unlock();
      }
   }
   
   public void dump(DebugPrintStream out, int size){
      byte data[] = new byte[size];
      
      i2cbus.lock();
      try {
	 if( ! i2cbus.write(device, 0) )/* set start address */
	   throw new Error("I2CEEPROM.dump: cannot set start address");
	 
	 int r = i2cbus.mread(device, data, 0, data.length);
	 if( r != data.length )
	   throw new Error("I2CEEPROM.dump: read aborted after "+r+" bytes.");
      }
      finally {
	 i2cbus.unlock();
      }
      Hex.dumpHex(out, data, 0x00);
   }
}

