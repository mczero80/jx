package jx.zero.scheduler;

import jx.zero.CPUState;

/**
this interface defines a Notifying-Method of a HighLevel_Scheduler 
*/
public interface HLS_destroyed{

/* y/n *//* specifies wether the IRQs are disabled or not */

             /** A thread was destroyed. <BR>
		 a running thread has ended its execution<BR>
		 the scheduler can destroy all information stored about this thread<BR>
	         <B>! IRQ are disabled during execution of this method</B> <BR>*/
/* yes */    public void destroyed  (CPUState Thread);
}
