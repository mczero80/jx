package jx.wintv;

import jx.zero.*;
import jx.zero.debug.*;
import java.util.Vector;

/* 
 * Implementation Note:
 * 
 * It would be possible to strip down this class by storing the subfields
 * outside this class. This would degrade the class to only do the
 * bitmanipulation of the JUMPs.
 * 
 * *BUT* removing the references of the subfield objects also means, that
 * theese could be garbage-collected while they are still in use by the
 * BT878 hardware.
 * 
 * Therefore I (asheiduk) prefer to keep the references inside this object
 * to avoid any possible confusion.
 * 
 */

class RISCJumpBlock extends RISCGenerator {
   RISCFieldBlock preface;
   RISCFieldBlock subblocks[];
    int fields;
    static MemoryManager memMgr = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");

   RISCJumpBlock(int fields){
      super(memMgr.allocAligned(calcSize(fields)+8, 4));
      this.fields = fields;
      subblocks = new RISCFieldBlock[fields];
   }
   
   void setFieldBlock(int field, RISCFieldBlock rb){
      subblocks[field] = rb;
   }
   
   RISCFieldBlock getFieldBlock(int field){
      Debug.assert( 0 <= field && field < fields, "bad field number");
      return subblocks[field];
   }
   
   void setPreface(RISCFieldBlock preface){
      this.preface = preface;
   }
   
   RISCFieldBlock getPreface(){
      return preface;
   }
   
   int getNumFields(){ 
      return fields;
   }
   
   void render(){
      reset();
      
      if( preface != null ){
	 jump(preface.getStartAddress());
	 preface.setReturnAddress(getRiscPC());
      }
      else {
	 jump(getRiscPC()+8);
      }
      
      int loopstart = getRiscPC();
      
      RISCFieldBlock rb;
      for(int i=0; i<subblocks.length; ++i){
	 rb = subblocks[i];
	 if( rb == null ){
	    jump(getRiscPC()+8);
	 }
	 else {
	    jump(rb.getStartAddress());
	    rb.setReturnAddress(getRiscPC());
	 }
      }
      jump(loopstart);
   }
   
   void switchField(boolean on, int field){
      int addrindex = (field+1)*2 + 1;
      
      int newaddr;
      if( on ){
	 newaddr = subblocks[field].getStartAddress();
	 subblocks[field].setReturnAddress( getStartAddress()+(addrindex+1)*4 );
      }
      else 
	newaddr = getStartAddress()+(field+2)*8;
      riscbuffer.set32(addrindex, newaddr);
   }
   
   void dump(DebugPrintStream out){
      out.println("RISCJumpBlock: 0x"+Hex.toHexString(getStartAddress())+"+"+getRiscOffset());
      super.dump(out);
      
      RISCFieldBlock f;
      for(int i=0; i<fields; ++i){
	 f = subblocks[i];
	 out.println();
	 out.println("Subfield " + i + ": 0x"+Hex.toHexString(f.getStartAddress())+"+"+f.getRiscOffset());
	 f.dump(out);
      }
   }
   
   static int calcSize(int fields){
      int size = 0;
      size += fields * 8;		/* jump for each field */
      size += 8;			/* loop jump */
      return size;
   }
}
