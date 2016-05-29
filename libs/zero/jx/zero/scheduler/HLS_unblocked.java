package jx.zero.scheduler;

import jx.zero.CPUState;

/**
this interface defines a Notifying-Method of a HighLevel_Scheduler 
*/
public interface HLS_unblocked{

/* y/n *//* specifies wether the IRQs are disabled or not */

             /** A thread was unblocked. <BR>
		 a thread was marked RUNNABLE<BR>
		 the scheduler should store the thread for later execution <BR>
	         <B>! IRQ are disabled during execution of this method</B><BR> */
/* yes */    public void unblocked  (CPUState newThread);
}
