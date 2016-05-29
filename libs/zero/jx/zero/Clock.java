package jx.zero;

public interface Clock extends Portal {
    public int getTimeInMillis();
    public long getTicks();
    public int getTicks_low();
    public int getTicks_high();
    public void getCycles(CycleTime c);
    public void subtract(CycleTime result, CycleTime a, CycleTime b); // result = a - b;
    public int toMicroSec(CycleTime c);
    public int toNanoSec(CycleTime c);
    public int toMilliSec(CycleTime c);
}
