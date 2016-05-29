package jx.zero;

public interface DomainLockManager extends Portal {
    void monitorenter(int addr);
    void monitorexit(int addr);
}
