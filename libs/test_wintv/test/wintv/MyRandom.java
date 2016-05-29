package test.wintv;
import jx.zero.*;
class MyRandom {
    static Clock clock = (Clock) InitialNaming.getInitialNaming().lookup("Clock");
    public int nextInt() {
	    return clock.getTicks_low();
    }
}
