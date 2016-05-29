package jx.zero;

public interface TimerEmulation extends Portal {
    int getTime();
    void installIntervallTimer(AtomicVariable atomic, CPUState state, int sec, int usec);
}
