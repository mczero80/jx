package jx.net.protocol.tcp;
import jx.timer.*;
import jx.zero.*;

/**
  * a argument object for the Flusher class - it has a reference to the Clock in order to queue itself again
  * furthermore there is a reference to the stream to flush and the interval time for flush() method calls
  *
  */
class FlushArg {
  private TimerManager timerManager;
  private MyOutputStream stream;
  private int flushtime;
  
  /**
    * construct the argument object holding a TimerManager reference, the output stream to flush and the flushing interval time
    *
    */
  public FlushArg(TimerManager c, MyOutputStream s, int t) {
    timerManager = c;
    stream = s;
    flushtime = t;
    }

  /**
    * return handle to the timerManager 
    *
    * @return the handle of the timerManager the argument hides 
    *
    */
  public TimerManager timerManager() {
    return timerManager;
  }  
  /**
    * return handle to a MyOutputStream
    *
    * @return the handle of the output stream to flush 
    *
    */
    public MyOutputStream stream() {
	return stream;
    }
  /**
    * return time of flushing interval
    *
    * @return interval time at which a flush call is done to the stream 
    *
    */
    public int flushtime() {
	return flushtime;
    }
}

