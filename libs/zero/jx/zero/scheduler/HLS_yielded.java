package jx.zero.scheduler;

import jx.zero.CPUState;

/**
this interface defines a Notifying-Method of a HighLevel_Scheduler 
*/
public interface HLS_yielded{

/* y/n *//* specifies wether the IRQs are disabled or not */


              /** A thread called <I>Thread::yield</I>. <BR>
		 the Scheduler may activate it again immediately or<BR>
		 store the thread for later execution <BR>
		 Warnig: if the Thread is the only Thread in this Domain, 
		 it can be undesirable to activate this thread immediatly
		 the scheduler should rather call HLScheduler::yield <BR>
	         <B>! IRQ are disabled during execution of this method</B><BR> */
/* yes */    public void yielded    (CPUState newThread);

}
