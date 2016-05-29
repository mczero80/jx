package jx.zero;

// inlined Portal
public interface LLSchedulerSupport extends Portal {

    /**@return  preempted Thread */
    CPUState preemptCurrentThread();
    /**@return  preempted Thread */
    CPUState interruptCurrentThread();
    void activateDomain(Domain domain);
    void activateIdleThread();
    
    /** Determines wether a Domain is runnable or not.
     *  Checks the maybeRunnable-Flag of the corresponding Highlevel-Scheduler.
     * @return <code>true</code> if this Domain is runnable. <BR>
     *         <code>false</code> if there is no runnable Thread in that domain.
     */
    boolean isRunnable(Domain domain);
    
    /* returns the amount of time a Domain is allowed to use the Processor */ 
    int  getDomainTimeslice();
    /* sets the amount of time to the next Timer event (IRQ)*/
    void tuneTimer (int time);
    /* returns the amount of time to the next Timer event (IRQ)*/
    int readTimer ();
    
    
    /* print info*/
    void dumpDomain (Domain domain);
    void dumpHLSched (Domain domain);
    void printDomainName (Domain domain);
}
