package jx.zero.scheduler;

import jx.zero.CPUState;
import jx.zero.CPUStateLink;

/**
this interface defines a Notifying-Method of a HighLevel_Scheduler 
*/
public interface HLS_blockedInService{

/* y/n *//* specifies wether the IRQs are disabled or not */

             /** A thread called a Portal. <BR>
		 a running thread was blocked due to a Portal call<BR>

		 blockedInService ist equivalent to blockedInPortalCall
		 but the HLS gets a Proxy of the Portal-Thread
		 So the Domain can donate it's CPU time to the Portal Domain<BR>
	         <B>! IRQ are disabled during execution of this method</B> <BR>*/
/* yes */    public void blockedInService (CPUState Thread, CPUStateLink PortalThread);
}
