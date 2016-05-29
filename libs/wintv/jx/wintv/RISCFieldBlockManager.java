package jx.wintv;

import java.util.Vector;
import jx.zero.*;

/* 
 * As long as the garbage collection does not work, this class helps to
 * reuse old Memory-objects which contain usually RISC Code. 
 * 
 */

class RISCFieldBlockManager /*implements MemoryManager*/ {
   final static boolean verbose	= false;
   
   static RISCFieldBlockManager instance = null;
   
   Vector blocks = new Vector();
   
   int allocated = 0;
   
   public static RISCFieldBlockManager getInstance(){
      if( instance == null )
	instance = new RISCFieldBlockManager();
      return instance;
   }
   
   public RISCFieldBlockManager(){
   }
   public int getTotalFreeMemory() {return 0;}   
   public int getTotalMemory() {return 0;}   
   public int getFreeHeapMemory() {return 0;}   
   public Memory alloc(int size){
      return allocAligned(size, 1);
   }
   
   public Memory allocAligned(int size, int aligned){
      Memory mem = searchAllocedBlock(size, aligned);
      if( mem != null ){
	 if( verbose )
	   Debug.out.println("reusing block @"+mem.getStartAddress()+", "+mem.size()+" bytes");
	 return mem;
      }
      ++allocated;
      mem = ZeroDomain.memMgr.allocAligned(size, aligned);
      if( verbose )
	Debug.out.println("using new block @"+mem.getStartAddress()+", "+mem.size()+" bytes");
      return mem;
   }
   
   public DeviceMemory allocDeviceMemory(int start, int size){
      throw new NotImpl();
   }

   public void freeBlock(Memory mem){
      if( mem == null )
	return;
      
      if( blocks.indexOf(mem) < 0 ){
	 if( verbose )
	   Debug.out.println("free block @"+mem.getStartAddress());
	 blocks.addElement(mem);
      }
      else
	Debug.out.println("WARNING: RISCFieldBlockManager: tried to free block @"+mem.getStartAddress()+" twice!");
   }
   
   public static void freeBlockS(Memory mem){
      getInstance().freeBlock(mem);
   }
   
   Memory searchAllocedBlock(int size, int aligned){
      Memory m;
      for(int i=0; i<blocks.size(); ++i){
	 m = (Memory)blocks.elementAt(i);
	 if( (m.size() >= size) && (m.getStartAddress() % aligned == 0) ){
	    blocks.removeElementAt(i);
	    return m;
	 }
      }
      return null;
   }
   
   public String toString(){
      return "allocated memory objects: "+allocated +
	"; reuseable memory objects: " + blocks.size();

   }
}
