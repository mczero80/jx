package jx.wintv;

import jx.zero.*;
import jx.zero.debug.*;

class RISCFieldBlock extends RISCGenerator {
   int jumpIndex = 0;
   
   RISCFieldBlock(MemoryManager memMgr, int dwords){
      super(getMemory(memMgr, dwords*4));
   }
   
   static Memory getMemory(MemoryManager memMgr, int size){
      return memMgr.allocAligned(size, 4);
   }
   
   public void setReturnAddress(int address){
      Debug.assert(jumpIndex > 0, "no JUMP to manipulate");
      riscbuffer.set32(jumpIndex+1, address);
   }
    
   public static FieldBlockCreator getFieldCreator(){
      return getFieldCreator(ZeroDomain.memMgr);
   }
    
   public static FieldBlockCreator getFieldCreator(MemoryManager memMgr){
      return new RISCFieldBlockCreator(memMgr);
   }
   

   /* Override some methods of superclass */
   
   public void jump(int addr){
      jumpIndex = risc_index;
      super.jump(addr);
   }
   public void jump(int options, int addr){
      jumpIndex = risc_index;
      super.jump(options, addr);
   }
}

class RISCFieldBlockCreator implements FieldBlockCreator {
   MemoryManager memMgr;
   
   RISCFieldBlockCreator(MemoryManager memMgr){
      this.memMgr = memMgr;
   }

   public RISCFieldBlock createFieldBlock(int dwords){
      return new RISCFieldBlock(memMgr, dwords);
   }
}
