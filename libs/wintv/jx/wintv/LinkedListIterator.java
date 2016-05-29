package jx.wintv;

import jx.framebuffer.PackedFramebuffer;
import jx.framebuffer.ClippingRectangle;
import jx.zero.Debug;
import jx.zero.debug.DebugPrintStream;

final class LinkedListIterator implements Cloneable {
   LinkedList list;
   LinkedList.LinkedListNode current;
   
   LinkedListIterator(LinkedList list){
      this.list = list;
      setFirst();
   }
   
   final void setFirst(){
      this.current = list.first.next;
   }
   
   final void insertBeforeCurrent(Object o){
      if( current == list.first )
	list.insertBeforeNode(current.next, o);
      else 
	list.insertBeforeNode(current, o);
   }
   
   final void remove(){
      if( isLast() )
	throw new Error("no such element");
      
      LinkedList.LinkedListNode prev = current.prev;
      list.removeNode(current);
      current = prev;
   }
   
   final void add(Object o){
      if( current == list.first )
	list.insertBeforeNode(current.next, o);
      else
	list.insertBeforeNode(current, o);
   }
   
   final LinkedListIterator copy(){
      try {
	 return (LinkedListIterator)clone();
      } catch (Throwable t) {
	 return null;
      }
   }
   
   final boolean isLast(){
      return current == list.last;
   }
   
   final Object current(){
      if( isLast() )
	throw new Error("no such element");
      return current.data;
   }
   
   final Object currentIncr(){
      Object o;
      if( isLast() )
	throw new Error("no such element");
      o = current.data;
      current = current.next;
      return o;
   }
   
   final void incr(){
      if( isLast() )
	throw new Error("no such element");
      current = current.next;
   }
}

