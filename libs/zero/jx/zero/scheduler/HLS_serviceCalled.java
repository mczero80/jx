package jx.zero.scheduler;

import jx.zero.CPUState;
import jx.zero.CPUStateLink;

/**
this interface defines a Notifying-Method of a HighLevel_Scheduler 
*/
public interface HLS_serviceCalled{

/* y/n *//* specifies wether the IRQs are disabled or not */

             /** A thread called a portal-method. <BR>

		 The Scheduler can decide if this Portal should be executed immediate 
		 (off-hand-scheduling) or if the Portal-Thread should be unblocked and
		 executed the next time the corresponding Domain activates it.  <BR>

		 serviceCalled ist equivalent to portalCalled
		 but the HLS gets a Proxy of the Portal-Thread
		 So the Domain can donate it's CPU time to the Portal
		 every time it's activated <BR>

	 	 if this Interface is not implemented, off-hand Scheduling is used. <BR>
                 <B>! IRQ are disabled during execution of this method</B> <BR>
		 @return 
		 TRUE: if off-hand scheduling is desired <BR>
		 FALSE: if the Portal Thread should be unblocked only*/
/* yes */    public boolean serviceCalled(CPUState Thread, CPUStateLink PortalThread);
}
