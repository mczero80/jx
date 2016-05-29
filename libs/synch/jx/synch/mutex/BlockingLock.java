package jx.synch.mutex;

public interface BlockingLock {
    void lock();
    void unlock();
}
