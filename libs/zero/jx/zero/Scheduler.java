package jx.zero;

// Fast Portal
public interface Scheduler extends Portal {
    void disableThreadSwitching();
    void enableThreadSwitching();
    void blockAndEnableThreadSwitching();
}
