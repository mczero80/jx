package jx.zero;

// Fast Portal
public interface CPUManager extends Portal {
    void yield();
    void sleep(int msec, int usec);
    void wait(Object var, int millis, int nanos);
    void notify(Object var);
    void notifyAll(Object var);
    void dump(String msg, Object obj);
    void switchTo(CPUState state);
    CPUState getCPUState();

    /** block this thread */
    void block();

    /** avoid lost unblocks by blocking only if the thread was not unblocked when RUNNABLE */
    void blockIfNotUnblocked();
    /** clear the flag that tells if the thread was unblocked when RUNNABLE */
    void clearUnblockFlag();

    /** wait until thread blocks */
    void waitUntilBlocked(CPUState thread);

    /** wait until the given thread terminates */
    void join(CPUState thread);
    /** 
     * @return true if thread was unblocked otherwise false
     */
    boolean unblock(CPUState state);
    CPUState createCPUState(ThreadEntry entry);
    //CPUState createAvailableCPUState();
    boolean start(CPUState state);
    void printStackTrace();
    AtomicVariable getAtomicVariable();
    void setThreadName(String name);
    void attachToThread(Object portalParameter);
    Object getAttachedObject();
    Credential getCredential();
    int createNewEvent(String label);
    void recordEvent(int nr);
    void recordEventWithInfo(int nr, int info);
    CAS getCAS(String classname, String fieldname);

    /* this has nothing to do with managing a CPU; it's here until we find a better place */
    VMClass getClass(String name);
    VMClass getVMClass(Object obj);

    /* this too has nothing to do with managing a CPU; it's here until we find a better place */
    VMObject getVMObject(); /* returns a "new" VMObject */

    void assertInterruptEnabled();
    void executeClassConstructors(int componentID);

    // move this to a portal that is not that easyly accessible
    void reboot();

    // stack frame information
    int getStackDepth();
    String getStackFrameClassName(int depth);
    String getStackFrameMethodName(int depth);
    int getStackFrameLine(int depth);
    int getStackFrameBytecode(int depth);

    // scheduler
    void inhibitScheduling();
    void allowScheduling();
}
