package jx.rpcsvc.nfs2;

import jx.rpc.*;

public class Timeval implements RPCData {
  public int seconds;
  public int useconds;

  public Timeval() {}
  public Timeval(boolean now) {
    long m = System.currentTimeMillis();
    seconds = (int)(m / 1000000);
    useconds = (int)(m - seconds * 1000000);
  }
  public Timeval(int sec, int usec) {
      this.seconds = sec;
      this.useconds = usec;
  }
}
