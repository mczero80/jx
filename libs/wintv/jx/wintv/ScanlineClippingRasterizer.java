package jx.wintv;

import jx.framebuffer.PackedFramebuffer;
import jx.framebuffer.ClippingRectangle;
import jx.zero.Debug;
import jx.zero.debug.DebugPrintStream;

class ActiveEdge {
   int x, ymax;
   ActiveEdge(int x, int ymax){
      this.x = x;
      this.ymax = ymax;
   }
   
   public String toString(){
      return "ActiveEdge("+x+", "+ymax+")";
   }
}

/********************************************************************/
/* ScanlineClippingRasterizer                                       */
/********************************************************************/

public class ScanlineClippingRasterizer {
   
   /*
   static void dump_aet(Iterator i){
      StringBuffer retval = new StringBuffer("ActiveEdgeTable(\n");
      while( i.hasNext() ){
	 Object o = i.next();
	 retval.append("\t"+o.toString()+"\n");
      }
      retval.append("\t);");
      Debug.out.println(retval);
   }
    */
   
   static void rasterizeScanline(LinkedListIterator aet_cur, ClippingRasterizerOutput out){
      if( aet_cur.isLast() )
	throw new Error("**** ERROR: empty AET");
      
      boolean flag_isin = true;
      boolean flag_sol = true;
      boolean flag_eol = false;
      ActiveEdge ae = (ActiveEdge)aet_cur.currentIncr();
      int d = ae.x;
      while( !aet_cur.isLast() ){
	 ae = (ActiveEdge)aet_cur.currentIncr();
	 d = ae.x - d;
	 if( d < 0 )
	   throw new Error("**** ERROR: AET not sorted!");
	 if( d > 0 ){
	    if( aet_cur.isLast() )
	      flag_eol = true;
	    
	    if( flag_isin )
	       out.write(d, flag_sol, flag_eol);
	    else
	      out.skip(d, flag_sol, flag_eol);
	    flag_sol = flag_eol = false;
	 }
	 else
	   if( !flag_sol )
	     Debug.out.println("**** Warning: zero framgent inside scanline!");
	 
	 d = ae.x;
	 flag_isin = !flag_isin;
      }
   }
   
   static void rasterizeET(ClippingEdgeTable et, ClippingRasterizerOutput out){
      LinkedList aet = new LinkedList();
      
      LinkedListIterator et_cur = et.listIterator();
      ClippingEdge edge = null;
      LinkedListIterator aet_cur = aet.listIterator();
      ActiveEdge ae;
      
      for(int yscan=0; !et_cur.isLast() || !aet.isEmpty(); ++yscan){
	 // remove all edges with ymax < yscan from AET
	 aet_cur.setFirst();
	 while( ! aet_cur.isLast() ){
	    ae = (ActiveEdge)aet_cur.current();
	    if( ae.ymax < yscan )
	      aet_cur.remove();
	    aet_cur.incr();
	 }
	 
	 // add edges from ET with ymin <= yscan.
	 // Because ET is sorted by ymin, stop at first element with ymin>yscan.
	 while( !et_cur.isLast() ){
	    edge = (ClippingEdge) et_cur.current();
	    if( edge.ymin > yscan )
	      break;
	    
	    // Insert new AE right in place, so sorting is not neccessary.
	    // 
	    ActiveEdge newedge = new ActiveEdge(edge.xmin, edge.ymax);
	    
	    /*
	    Debug.out.println("insert edge "+newedge+" into AET");
	    Debug.out.println("current ET:");
	    et.dump(Debug.out, et_cur.copy());
	    
	    Debug.out.println("current AET:");
	    aet.dump(Debug.out, aet_cur.copy());
	    */
	    
	    aet_cur.setFirst();
	    while( !aet_cur.isLast() ){
	       ae = (ActiveEdge)aet_cur.current();
	       if( newedge.x < ae.x )
		 break;
	       aet_cur.incr();
	    }
	    aet_cur.insertBeforeCurrent(newedge);
	    
	    /*
	    Debug.out.println("AET after insert:");
	    aet.dump(Debug.out, aet_cur.copy());
	    */
	    
	    et_cur.incr();
	 }
	 

	 if( aet.isEmpty() )
	   continue;
	 aet_cur.setFirst();
	 rasterizeScanline(aet_cur, out);
      }
      out.eof();
   }
   
   public static RISCFieldBlock rasterizeField(FieldBlockCreator creator, int synctype, PackedFramebuffer framebuffer, ClippingRectangle clippings[]){
      ClippingEdgeTable et = 
	new ClippingEdgeTable(new ClippingRectangle(0, 0, framebuffer.width()-1, framebuffer.height()-1),
			      clippings);
      
      RISCFieldBlock rfb = creator.createFieldBlock(16*1024);   /* FIXME: dynamic allocation? */
      
      ClippingRasterizerOutput output = new BT878PackedRISCOutput(framebuffer, rfb);
      
      rfb.sync(RISC.FIFO_FM1 | RISC.RESYNC | RISC.IRQ);
      rasterizeET(et, output);
      rfb.sync(synctype | RISC.RESYNC);
      rfb.jump(0x0);
      rfb.inval();
      
      return rfb;
   }
}


