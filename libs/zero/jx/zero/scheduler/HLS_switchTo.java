package jx.zero.scheduler;

import jx.zero.CPUState;

/**
this interface defines a Method of a HighLevel_Scheduler 
*/
public interface HLS_switchTo{

/* y/n *//* specifies wether the IRQs are disabled or not */


    /** A thread switched to this Thread. <BR>
	under construction!!!
	a Thread was unblocked and activated by the kernel (portalcall)

	<B>! IRQ are disabled during execution of this method</B><BR> */
    
/* yes */    public void switchedTo(CPUState Thread);
}
