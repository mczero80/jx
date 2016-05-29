package jx.zero;

// inlined Portal
public interface JAVASchedulerSupport extends Portal {

    CPUState getCurThr();          
    void switchTo(CPUState state); 

    /*  returns the idle Thread of the current cpu */
    CPUState getIdleThread();

    /*  sets the time (in 10msec) of the timeslice (for the current CPU) */
    void setTimeslice (int time);  //ttt: obsolete??

    /* store/get Info-Object associated with a Thread*/
    void storeThreadInfo(CPUState state, Object data);
    Object getThreadInfo (CPUState state);

    /* returns the Domain of a given Thread */
    Domain getThreadDomain (CPUState state);

    /* print info*/
    void dumpThread (CPUState state);
}
