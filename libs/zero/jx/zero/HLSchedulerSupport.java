package jx.zero;

// Fast Portal
public interface HLSchedulerSupport extends Portal {

        /* destroys the current Thread and .... */
               /* ...fully activates the given one */
/* no! */  void activateThread(CPUState state);

               /* ... donate the CPU to the given Portal */
               /* this Method returns if the Portalthread is blocked 
		* return value is always FALSE */
/* no! */  boolean activateService(CPUStateLink state);

           /* ...activates the next Domain */
/* no! */  void yield();

    /** Clears the maybeRunnable-Flag for the current Scheduler.*/
    void clearMyRunnableFlag();

    /*  sets the time (in 100usec) of the timeslice (for the current CPU) */
    int getTimeBaseInMicros();
    int  getDomainTimeslice ();
    void setMyTimeslice (int time);
    void setTimeslice (Domain domain, int time);

    /* store/get Info-Object associated with a Thread*/
    void storeThreadInfo(CPUState state, Object data);
    Object getThreadInfo (CPUState state);
    
     /* print info */
    void dumpThread (CPUState state);
}
