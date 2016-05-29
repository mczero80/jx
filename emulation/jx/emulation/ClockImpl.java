package jx.emulation;

import jx.zero.MemoryManager;
import jx.zero.*;

class ClockImpl implements Clock {
    int t;
    public int getTimeInMillis() { return (int)((System.currentTimeMillis()) & 0xffffffff); }
    public long getTicks() {return 0;}
    public int getTicks_low(){return (int)((System.currentTimeMillis()*500000) & 0xffffffff);}
    public int getTicks_high(){return (int)(((System.currentTimeMillis()*500000) >> 32) & 0xffffffff);}
    public void getCycles(CycleTime c) {}
    public void subtract(CycleTime result, CycleTime a, CycleTime b){}
    public int toMicroSec(CycleTime c) {return 0;}
    public int toNanoSec(CycleTime c) {return 0;}
    public int toMilliSec(CycleTime c) {return 0;}
}
