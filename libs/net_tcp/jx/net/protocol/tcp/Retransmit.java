package jx.net.protocol.tcp;
import java.lang.*;
import java.util.*;
import metaxa.os.*;
import jx.net.protocol.tcp.*;
import jx.timer.*;
import jx.zero.*;

/**
  * this is the Retransmit class which does the call to the retransmitter method of each TCP
  * after that it queues itself again into the timerqueue for the next call in RETRANSMITTIME time
  *
  */
class Retransmit implements TimerHandler {

    TimerManager timerManager;
    Retransmit(TimerManager timerManager) {
	this.timerManager = timerManager;
    }

  /**
    * the timer method is the method which gets called at a timer object
    * this one does all the retransmission stuff and queues a new Retransmit object for the next retransmission
    *
    * @param the argument object specific for this kind of timer class
    *
    */
  public void timer(Object arg) {
    TCP tcp = ((RetransmitArg)arg).tcp();
    
    tcp.retransmitter();
    
    timerManager.addMillisTimer(tcp.RETRANSMITTIME, new Retransmit(timerManager), arg);
  }

  /**
    * method to check wether an object is an instance of this class
    *
    * @param obj object to be checked 
    *
    */
  public boolean equals(Object obj) {
    return (obj instanceof Retransmit);
  }
  
}
