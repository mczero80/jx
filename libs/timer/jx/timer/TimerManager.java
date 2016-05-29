package jx.timer;

import jx.zero.Portal;
import jx.zero.CPUState;

public interface TimerManager extends Portal {
    Timer addMillisTimer(int expiresFromNowInMillis, TimerHandler handler, Object argument);
    Timer addMillisIntervalTimer(int expiresFromNowInMillis, int intervalInMillis, TimerHandler handler, Object argument);
    Timer addTimer(int expiresFromNow, int interval, TimerHandler handler, Object argument);
    boolean deleteTimer(TimerHandler t);
    int getTimeInMillis();
    int getTimeBaseInMicros();
    int getCurrentTime();
    int getCurrentTimePlusMillis(int milliSeconds);
    void unblockInMillis(CPUState thread, int timeFromNowInMillis);
    void unblockInMillisInterval(CPUState thread, int expiresFromNowInMillis, int intervalInMillis);
}
