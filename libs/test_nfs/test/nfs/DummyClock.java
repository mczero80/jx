package test.nfs;

import jx.zero.*;

final class DummyClock implements Clock {
    int t;
    public int getTimeInMillis() { return t++; }
    public long getTicks() {return 0;}
    public int getTicks_low(){return 0;}
    public int getTicks_high(){return 0;}
    public  void getCycles(CycleTime c){}
    public void subtract(CycleTime result, CycleTime a, CycleTime b){}
    public int toMicroSec(CycleTime c) {return 0;}
    public int toNanoSec(CycleTime c) {return 0;}
    public int toMilliSec(CycleTime c) {return 0;}
}
