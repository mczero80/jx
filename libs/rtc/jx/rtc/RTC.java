package jx.rtc;

import jx.zero.*;

public interface RTC extends Portal {
    public static final byte HZ_2     = 0xf;
    public static final byte HZ_4     = 0xe;
    public static final byte HZ_8     = 0xd;
    public static final byte HZ_16    = 0xc;
    public static final byte HZ_32    = 0xb;
    public static final byte HZ_64    = 0xa;
    public static final byte HZ_128   = 0x9;
    public static final byte HZ_256   = 0x8;
    public static final byte HZ_512   = 0x7;
    public static final byte HZ_1024  = 0x6;
    public static final byte HZ_2048  = 0x5;
    public static final byte HZ_4096  = 0x4;
    public static final byte HZ_8192  = 0x3;

    int getTime();
    void installIntervallTimer(AtomicVariable atomic, CPUState state, int msec);
}
