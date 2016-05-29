package jx.wintv;

import jx.zero.*;
import jx.zero.debug.*;

/*
 * Die Klasse bietet Unterstützung zur vereinfachten Erzeugung von
 * RISC-Programmen für den BT878 Chip. 
 */

class RISCGenerator implements RISC {
   Memory riscbuffer;
   int risc_index = 0;
   
   public RISCGenerator(Memory mem){
      riscbuffer = mem;
   }
   
   public RISCGenerator(int longs){
      riscbuffer = ZeroDomain.memMgr.allocAligned(longs*4, 4);
   }
   
   int getRiscIndex(){
      return risc_index;
   }
   
   int getRiscOffset(){
      return risc_index*4;
   }
   
   int getMaxIndex(){
      return riscbuffer.size() / 4;
   }
   
   int getRiscPC(){
      return riscbuffer.getStartAddress() + getRiscOffset();
   }
   
   int riscs(int reset, int set){
      return ((reset&0xf) << 20) | ((set&0xf) << 16);
   }
   
   void reset(){
      risc_index = 0;
   }
   
   int getStartAddress(){
      return riscbuffer.getStartAddress();
   }
   
   int riscOffset(int riscIP){
      /* Note: Due to some obscure reasons (eg. prefetch) a little offset is necessary */
      if( getStartAddress() <= riscIP && riscIP < (getRiscPC()+64) )   
	return riscIP - getStartAddress();
      return -1;
   }
   
   void dump(DebugPrintStream out){
      int bytes = risc_index*4;
      
      if( bytes < 128 ){
	 Hex.dumpHex32(out, riscbuffer, risc_index*4);
	 return;
      }
      
      // only output head and trailer
      Hex.dumpHex32(out, riscbuffer, 0x0, 16, 64, riscbuffer.getStartAddress());
      out.println("...");
      int start = (bytes-64) & ~0xf;
      Hex.dumpHex32(out, riscbuffer, start, 16, bytes, riscbuffer.getStartAddress()+start);
   }
   
   /******************************/
   /* packed pixel mode          */
   /******************************/
   
   public void write(int options, int count, int addr){
      riscbuffer.set32(risc_index++, WRITE | count | options);
      riscbuffer.set32(risc_index++, addr);
      
//      Debug.out.println("write: 0x"+ Hex.toHexString(WRITE | count | options) + " 0x" + Hex.toHexString(addr));
//      Debug.out.println("       0x"+ Hex.toHexString(riscbuffer.get32(risc_index-2)) + " 0x" + Hex.toHexString(riscbuffer.get32(risc_index-1)));

   }
   
   public void writec(int options, int count){
      riscbuffer.set32(risc_index++, WRITEC | count | options);
   }
   
   public void skip(int options, int count){
      riscbuffer.set32(risc_index++, SKIP | count | options);
   }
   
   /******************************/
   /* planar pixel mode          */
   /******************************/
   
   public void write123(int options, int count1, int addr1, int count2, int addr2, int count3, int addr3){
      riscbuffer.set32(risc_index++, WRITE123|options|count1);
      riscbuffer.set32(risc_index++, count3<<16 | count2 );
      riscbuffer.set32(risc_index++, addr1);
      riscbuffer.set32(risc_index++, addr2);
      riscbuffer.set32(risc_index++, addr3);
   }
   
   public void write1s23(int options, int count1, int addr1, int count2, int count3){
      riscbuffer.set32(risc_index++, WRITE1S23 | options | count1);
      riscbuffer.set32(risc_index++, count3<<16 | count2 );
      riscbuffer.set32(risc_index++, addr1);
   }
   
   public void skip123(int options, int count1, int count2, int count3){
      riscbuffer.set32(risc_index++, SKIP123 | options | count1);
      riscbuffer.set32(risc_index++, count3<<16 | count2 );
   }
   
   /******************************/
   /* general RISC stuff         */
   /******************************/
   public void jump(int addr){
      jump(0, addr);
   }
   public void jump(int options, int addr){
      riscbuffer.set32(risc_index++, JUMP | options);
      riscbuffer.set32(risc_index++, addr);
   }
   
   public void sync(int options){
      riscbuffer.set32(risc_index++, SYNC | options);
      riscbuffer.set32(risc_index++, 0);
      
//      Debug.out.println("sync: 0x"+ Hex.toHexString(SYNC | options) + " 0x00000000");
//      Debug.out.println("      0x"+ Hex.toHexString(riscbuffer.get32(risc_index-2)) + " 0x" + Hex.toHexString(riscbuffer.get32(risc_index-1)));
      
   }
   
   /* generate an invalid instruction which makes the DMA conroller abort */
   public void inval(){
      riscbuffer.set32(risc_index++, 0);
   }
}


