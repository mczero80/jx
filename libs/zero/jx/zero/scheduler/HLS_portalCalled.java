package jx.zero.scheduler;

import jx.zero.CPUState;

/**
this interface defines a Notifying-Method of a HighLevel_Scheduler 
*/
public interface HLS_portalCalled{

/* y/n *//* specifies wether the IRQs are disabled or not */

             /** A thread called a portal-method. <BR>
		 a thread wants to call a portal.
		 The Scheduler can decide if this Portal should be executed immediate 
		 (off-hand-scheduling) or if the Portal-Thread should be unblocked and
		 executed the next time the corresponding Domain activates it. <BR>
		 if this Interface is not implemented, off-hand Scheduling is used. <BR>
	         <B>! IRQ are disabled during execution of this method</B> <BR>
		 @return 
		 TRUE: if off-hand scheduling is desired <BR>
		 FALSE: if the Portal Thread should be unblocked only*/
/* yes */    public boolean portalCalled(CPUState Thread);
}
