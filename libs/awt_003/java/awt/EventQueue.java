package java.awt;

import java.awt.event.*;
import jx.buffer.multithread.MultiThreadList;


/**
 * This is an own implementation of the AWT EventQueue class.
 * It uses the JX MultiThreadList to perform synchronized
 * operations on the event queue.
 *
 * @author Marco Winter
 */
public class EventQueue {

    /** The underlying dispatch thread */
    private EventDispatchThread dispatchThread;
    /** The underlying list implementation */
    private MultiThreadList list;
    
    

    public EventQueue() {
	list = new MultiThreadList();
	dispatchThread = new EventDispatchThread(this);
    }


    /**
     * Gets the next event from the event queue.
     */
    public AWTEvent getNextEvent() {
	return (AWTEvent) list.undockFirstElement();
    }

    /**
     * Inserts a new event in the event queue.
     */
    public void postEvent(AWTEvent evt) {
	list.appendElement(evt);
    }

    /**
     * Dispatches the event to the event's source
     * according to the event's type.
     */
    public void dispatchEvent(AWTEvent evt) {
	/*    if (evt instanceof ActiveEvent)
	      {
	      ActiveEvent active_evt = (ActiveEvent) evt;
	      active_evt.dispatch();
	      }
	      else
	      {*/
	Object source = evt.getSource();

	if (source instanceof Component)
	    {
		Component srccmp = (Component) source;
		srccmp.dispatchEvent(evt);
	    }
	else if (source instanceof MenuComponent)
	    {
	 	MenuComponent srccmp = (MenuComponent) source;
		srccmp.dispatchEvent(evt);
	    }
    }
}
