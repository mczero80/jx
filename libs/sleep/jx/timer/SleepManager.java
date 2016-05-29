package jx.timer;

import jx.zero.Portal;

public interface SleepManager extends Portal {
    /**
       sleep for some microseconds
       @param microseconds the amount of time to sleep (in microseconds)
    */
    public void udelay(int microseconds);
    /**
       sleep for some milliseconds
       @param milliseconds the amount of time to sleep (in milliseconds)
    */
    public void mdelay(int milliseconds);
}
