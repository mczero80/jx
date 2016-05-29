package jx.wintv;

import jx.zero.*;
import jx.zero.debug.*;

class RISCFieldBlockReloc extends RISCFieldBlock implements Relocatable {
   int relocationIndex[] = null;
   int lastIndex = 0;
   
   RISCFieldBlockReloc(MemoryManager memMgr, int dwords){
      super(memMgr, dwords);
      relocationIndex = new int[riscbuffer.size()/4];
      lastIndex = 0;
   }
    /*   
   public static FieldBlockCreator getFieldCreator(){
      return getFieldCreator(ZeroDomain.memMgr);
   }
    */
   public static FieldBlockCreator getFieldCreator(MemoryManager memMgr){
      return new RISCFieldBlockRelocCreator(memMgr);
   }
   
   /* Override some methods from RISCGenerator */
   
   public void write(int options, int count, int addr){
      relocationIndex[lastIndex++] = risc_index+1;
      super.write(options, count, addr);
   }
   
   public void write123(int options, int count1, int addr1, int count2, int addr2, int count3, int addr3){
      relocationIndex[lastIndex++] = risc_index+2;
      relocationIndex[lastIndex++] = risc_index+3;
      relocationIndex[lastIndex++] = risc_index+4;
      super.write123(options, count1, addr1, count2, addr2, count3, addr3);
   }
   
   public void write1s23(int options, int count1, int addr1, int count2, int count3){
      relocationIndex[lastIndex++] = risc_index+2;
      super.write1s23(options, count1, addr1, count2, count3);
   }
   
   
   /* Relocation interface */
   
   
   /**
    * Patch RISC Code to write capture data to another memory address.
    * 
    * WARNING: You can easily overwrite any memory region with this.
    *     Because the RISCFieldBlockReloc class has no idea of
    *     "Framebuffers" or "Memory" Objects to prevent overwriting. The
    *     RISC Code must be trusted anyway so the right place to prevent
    *     leaving the regions of a framebuffer is located in higher levels.
    */
   public void relocateField(int offset){
      int ri;
      for(int i=0; i<lastIndex; ++i){
	 ri = relocationIndex[i];
	 riscbuffer.set32(ri, riscbuffer.get32(ri)+offset);
      }
   }
}

class RISCFieldBlockRelocCreator implements FieldBlockCreator {
   MemoryManager memMgr;
   
   RISCFieldBlockRelocCreator(MemoryManager memMgr){
      this.memMgr = memMgr;
   }
   
   public RISCFieldBlock createFieldBlock(int dwords){
      return new RISCFieldBlockReloc(memMgr, dwords);
   }
}
