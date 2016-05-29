package jx.wintv;

import jx.framebuffer.PackedFramebuffer;
import jx.framebuffer.ClippingRectangle;
import jx.zero.Debug;
import jx.zero.debug.DebugPrintStream;

class ClippingEdgeTable extends LinkedList {
   
   ClippingEdgeTable(ClippingEdge[] edges){
      for(int i=0; i<edges.length; ++i)
	insertSorted(edges[i]);
   }
   
   public ClippingEdgeTable(ClippingRectangle frame, ClippingRectangle rects[]){
      
      if( rects == null ){
	 // add only "standard edges" (see below)
	 insertSorted(new ClippingEdge(frame.ymin(), frame.xmin(), frame.ymax()));
	 insertSorted(new ClippingEdge(frame.ymin(), frame.xmax()+1, frame.ymax()));
	 return;
      }
      
      ClippingRectangle r;
      int xmin, xmax, ymin, ymax;
      for(int rect=0; rect<rects.length; ++rect){
	 r = rects[rect];
	 if( r == null )
	   continue;
	 
	 // clip rectangle against frame

	 if(   r.xmax() < frame.xmin()	/* complete rejection */
	    || r.xmin() > frame.xmax()
	    || r.ymax() < frame.ymin()
	    || r.ymin() > frame.ymax() )
	   continue;
	 
	 xmin = r.xmin();
	 if( xmin < frame.xmin() )
	   xmin = frame.xmin();
	 ymin = r.ymin();
	 if( ymin < frame.ymin() )
	   ymin = frame.ymin();
	 xmax = r.xmax();
	 if( xmax > frame.xmax() )
	   xmax = frame.xmax();
	 ymax = r.ymax();
	 if( ymax > frame.ymax() )
	   ymax = frame.ymax();
	 
	 processClippingEdge(new ClippingEdge(ymin, xmin, ymax),
			     rects, 0,
			     r);
	 processClippingEdge(new ClippingEdge(ymin, xmax+1, ymax),
			     rects, 0,
			     r);
      }
      
      // add "standard edges"

      /*
       * NOTE: The right edge is processes differently to avoid zero wide
       * fragments occuring when a clipped frame touches the right edge.
       * Otherwise EOL detection may fail! But the left edge cannot be
       * treated this way, because on the left side zero wide fragments
       * only mean: begin the line with a "skip", not with a "write".
       */

      insertSorted(new ClippingEdge(frame.ymin(), frame.xmin(), frame.ymax()));
      processClippingEdge(new ClippingEdge(frame.ymin(), frame.xmax()+1, frame.ymax()), rects, 0, null);
   }
   
   
   void processClippingEdge(ClippingEdge edge, ClippingRectangle rects[], int rect_start, ClippingRectangle rect_excl){
      ClippingRectangle r;
      for(; rect_start<rects.length; ++rect_start){
	 r = rects[rect_start];
	 if( r == rect_excl )
	   continue;
	 if( (edge.xmin < r.xmin()) || (edge.xmin > r.xmax()+1) ||
	     (edge.ymax < r.ymin()) || (edge.ymin > r.ymax()))
	   continue; 			/* completly outside, next rect */
	 if( (edge.ymin >= r.ymin()) && (edge.ymax <= r.ymax()) )
	   return;			/* completly inside, ignore edge */
	 
	 if( edge.ymin < r.ymin() ){
	    // edge starts above rect 
	    processClippingEdge(new ClippingEdge(edge.ymin, edge.xmin, r.ymin()-1), 
				rects, rect_start,
				rect_excl);
	    if( edge.ymax > r.ymax() )	/* edge ends below rect, too */
	      processClippingEdge(new ClippingEdge(r.ymax()+1, edge.xmin, edge.ymax), 
				  rects, rect_start,
				  rect_excl);
	 }
	 else {
	    // edge starts inside rect and end below
	    processClippingEdge(new ClippingEdge(r.ymax()+1, edge.xmin, edge.ymax), 
				rects, rect_start,
				rect_excl);
	 }
	 return;
      }
      insertSorted(edge);
   }
   
   void insertSorted(ClippingEdge edge){
      LinkedListIterator li = listIterator();
      ClippingEdge e2;
      while( ! li.isLast() ){
	 e2 = (ClippingEdge)li.current();
	 if( edge.ymin < e2.ymin ){
	    li.insertBeforeCurrent(edge);
	    return;
	 }
	 li.incr();
      }
      addElement(edge);
   }
   
   public String toString(){
      StringBuffer retval = new StringBuffer("ClippingEdgeTable(\n");
      LinkedListIterator i = listIterator();
      while( !i.isLast() ){
	 Object o = i.currentIncr();
	 retval.append("\t"+o.toString()+",\n");
      }
      retval.append("\t);");
      return retval.toString();
   }
}
