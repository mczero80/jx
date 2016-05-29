package jx.zero;

public interface Domain extends Portal {
    int getID();
    void clearTCBflag();
    boolean isActive();
    boolean isTerminated();
    String getName();
}
