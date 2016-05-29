package jx.zero.scheduler;

import jx.zero.CPUState;

/**
this interface defines a Notifying-Method of a HighLevel_Scheduler 
*/
public interface HLS_preempted{

/* y/n *//* specifies wether the IRQs are disabled or not */


             /** A thread was preempted. <BR>
		 the Scheduler should store the thread for later execution <BR>
	         <BR><B>! IRQ are disabled during execution of this method</B><BR> */
/* yes */    public void preempted  (CPUState newThread);
}
