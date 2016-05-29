package jx.zero.scheduler;

import jx.zero.CPUState;

/**
this interface defines a Notifying-Method of a HighLevel_Scheduler 
*/
public interface HLS_blocked{

/* y/n *//* specifies wether the IRQs are disabled or not */

             /** A thread was blocked. <BR>
		 a running thread was blocked (maybe it called cpuManager::block)<BR>
	         <B>! IRQ are disabled during execution of this method</B> <BR>*/
/* yes */    public void blocked    (CPUState Thread);
}
