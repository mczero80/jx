package jx.wintv;

import jx.framebuffer.PackedFramebuffer;
import jx.framebuffer.ClippingRectangle;
import jx.zero.Debug;
import jx.zero.debug.DebugPrintStream;

/********************************************************************/
/* Linked List                                                      */
/********************************************************************/

class LinkedList {
   LinkedListNode first;
   LinkedListNode last;
   
   class LinkedListNode {
      LinkedListNode prev;
      LinkedListNode next;
      Object data;
      LinkedListNode(LinkedListNode prev, Object data, LinkedListNode next){
	 this.prev = prev;
	 this.data = data;
	 this.next = next;
      }
   }
   
   
   LinkedList(){
      clear();
   }
   
   LinkedList(Object array[]){
      clear();
      
      for(int i=0; i<array.length; ++i)
	addElement(array[i]);
   }
   
   public void clear(){
      last = new LinkedListNode(null, null, null);
      first = new LinkedListNode(null, null, last);
      last.prev = first;
   }
   public void addElement(Object o){
      insertBeforeNode(last, o);
   }
   
   public LinkedListIterator listIterator(){
      return new LinkedListIterator(this);
   }
   
   void insertBeforeNode(LinkedListNode node, Object o){
      LinkedListNode tmp = new LinkedListNode(node.prev, o, node);
      node.prev = tmp;
      tmp.prev.next = tmp;
   }
   
   void removeNode(LinkedListNode node){
      node.prev.next = node.next;
      node.next.prev = node.prev;
      node.prev = null;
      node.next = null;
      node.data = null;
   }
   
   final public boolean isEmpty(){
      return first.next == last;
   }
   
   public void dump(DebugPrintStream out, LinkedListIterator start){
      out.println("LinkedList(");
      LinkedListNode cur = first;
      while(cur != null){
	 if( start.current == cur )
	   out.print("->");
	 if( cur.data == null )
	   out.println("\t(null)");
	 else 
	   out.println("\t"+cur.data.toString());
	 cur = cur.next;
      }
      out.println(")");
   }
}
