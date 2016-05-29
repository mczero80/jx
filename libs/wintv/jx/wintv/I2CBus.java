package jx.wintv;

/** 
 * Generic Interface to an I2C bus.
 * 
 * Locking
 * =======
 * 
 * Exclusive access to the bus must be aquired before you can use the
 * methods which are accessing the bus. If you try to access the bus
 * without the lock you may catch an execption. :-)
 * 
 * Note: Implicit locking of the bus within the access methods is not
 * possible, because that would prevent easy implementation of repeated
 * START commands.
 */
public interface I2CBus {
   
   /** 
    * Write a single byte to the device.
    */
   boolean write(int device, int data1);
   
   /** 
    * Write two bytes to the device. The first data byte is usually a
    * device subaddress, the second byte the real data. But this may differ
    * from device to device. 
    */
   boolean write(int device, int data1, int data2);
   
   /** Read a single Byte from a device */
   int read(int device);
   
   /** 
    * Read multiple bytes from a device. 
    * @return number of bytes read successfully.
    */
   int mread(int device, byte data[], int off, int len);
   
   /**
    */
   int mwrite(int device, byte data[], int off, int len);
   
   /**
    * Probe the bus for a device.
    * 
    * This method may be implemented by reading one byte from the device or
    * it by aborting a read/write after sending the device byte and
    * receiving the ACK/Timeout.
    */
   boolean probe(int device);
   
   /** 
    * Lock the bus for exclusive access.
    */
   void lock();
   
   /** 
    * Unlock the bus.
    */
   void unlock();
}
