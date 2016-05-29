package jx.zero.scheduler;

import jx.zero.CPUState;

/**
this interface defines a Notifying-Method of a HighLevel_Scheduler 
*/
public interface HLS_blockedInPortalCall{

/* y/n *//* specifies wether the IRQs are disabled or not */

             /** A thread called a Portal. <BR>
		 a running thread was blocked due to a Portal call<BR>
	         <B>! IRQ are disabled during execution of this method</B> <BR>*/
/* yes */    public void blockedInPortalCall (CPUState Thread);
}
