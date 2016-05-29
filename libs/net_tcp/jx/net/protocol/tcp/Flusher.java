package jx.net.protocol.tcp;
import java.lang.*;
import java.util.*;
import java.io.*;
import metaxa.os.*;
import jx.net.protocol.tcp.*;
import jx.timer.*;
import jx.zero.*;

/**
  * this is the Flusher class which does a call of the method flush of a output stream 
  * this is necessary as we store data to be send if it is to small because we don´t want to send segments with sizes of ony several bytes
  * we wait flushtime: if the buffer is filled not anymore and therefore the data is send automatically this call to flush each flushtime
  * guarantees that the data is not buffer infinitly
  *
  */
class Flusher implements TimerHandler {

    TimerManager timerManager;
    Flusher(TimerManager timerManager) {
	this.timerManager = timerManager;
    }

  /**
    * the timer method is the method which gets called at a timer object
    * we call the flush method to send all buffered data and queue ourself again
    *
    * @param arg an argument object specific for this kind of timer class
    *
    */
  public void timer(Object arg) {
    
  MyOutputStream output = ((FlushArg)arg).stream();
  int flushtime = ((FlushArg)arg).flushtime();
    
  try {
      output.flush();
  }
  catch (IOException e) {
      System.out.println("Flusher: flush() threw a IOException - internal error??");
  }
  timerManager.addMillisTimer(flushtime, new Flusher(timerManager), arg);
  }
    
  /**
    * method to check wether an object is an instance of this class
    *
    * @param obj object to be checked 
    *
    */
  public boolean equals(Object obj) {
    return (obj instanceof Flusher);
  }
  
}
