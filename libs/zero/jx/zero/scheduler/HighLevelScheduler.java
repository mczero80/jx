package jx.zero.scheduler;

import jx.zero.*;

/**
this interface defines a Scheduler for the Threads of a Domain
the inlined Portal jx.zero.HLSchedulerSupport contains some methods useful for this Scheduler
*/
public interface HighLevelScheduler{

/* y/n *//* specifies wether the IRQs are disabled or not */


    /*******************************/
    /* methods indicating an error */
    /*******************************/

             /** the Scheduler thread was preemted. (this indicates an error) <BR>
	         Scheduler schould return TRUE if the error is handled 
		 or FALSE if not, the Thread is resumed then (at the next CPU slot) <BR>
	         <B>! IRQ are disabled during execution of this method</B><BR> 
		 *
		 * @param  ---
		 * @return <code>true</code>, if error is handled 
		 <code>false</code> otherwise
		 */

/* yes */    public boolean Scheduler_preempted();
             /** the Scheduler thread was interrupted. (this indicates an error) <BR>
	         Scheduler schould return TRUE if the error is handled 
		 or FALSE if not, the Thread is resumed then (at the next CPU slot) <BR>
	         <B>! IRQ are disabled during execution of this method</B><BR> */
/* yes */    public boolean Scheduler_interrupted();


    /**********************************************/
    /* methods adding  a thread to the Scheduler  */
    /**********************************************/

             /** A new Thread  was created. <BR>
		 this methode notifies the Scheduler when a new Thread is created<BR>
		 the Scheduler should store the thread for later execution <BR>
	         <B>! IRQ are disabled during execution of this method</B><BR> */
/* yes */    public void created    (CPUState newThread);
 

             /** A thread switched to this Thread. <BR>
		 under construction!!!! <BR>
		 a Thread was unblocked and activated by the kernel (portalcall)
		 this method informs the the scheduler the situation<BR>
	         <B>! IRQ are disabled during execution of this method</B><BR> */
///* yes */    public void switchedTo(CPUState Thread);


    /********************************/
    /* methods activating a thread  */
    /********************************/

             /** the Domain scheduled by this Scheduler was activated.  <BR>
		 the scheduler schould select a Thread to execute next <BR>
		 the inlined Portal HLSchedulerSupport provides the Method 
		 <I>void activateThread(CPUState state);</I>
		 to activate the selected Thread <BR>
		 if there is no next thread to activate the scheduler can call
		 <I>HLSchedulerSupport::yield</I> to activate the next Domain<BR>
	         ! IRQ are <I>enabled</I> during execution of this method</B>*/
/* no! */    public void activated();

    /*****************/
    /* other methods */
    /*****************/

             /** Print information about the RunQ. <BR>
	         <B>! IRQ are disabled during execution of this method</B> <BR>*/
/* yes */    public void dump();

 
            /** This is the first Method called after the Scheduler was registered. <BR>
		 it is called from <I>SMPcpuManager::register_HLScheduler</I><BR>
                 and is executed on its final CPU to (e.g. to install an IRQ-Handler)<BR> 
	         <B>! IRQ are disabled during execution of this method</B> <BR>*/
/* yes */    public void registered();
 }
