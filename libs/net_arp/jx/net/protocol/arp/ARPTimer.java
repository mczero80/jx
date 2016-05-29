package jx.net.protocol.arp;

import jx.zero.*;
import jx.timer.*;

/**
  * this is the Timer class uses by the ARP class - it clears the ARPCache every 15 minutes 
  * otherwise a changed configuration would maybe never be recognized as the old information would stay in the cache forever
  */
class ARPTimer implements TimerHandler {

    TimerManager timerManager;

    ARPTimer(TimerManager timerManager) {
	this.timerManager = timerManager;
    }
    
    public void timer(Object arg) {
	
	ARP arp = ((ARPTimerArg)arg).handle();
    
	arp.clearCache();
	
	timerManager.addMillisTimer(900000,  new ARPTimer(timerManager), arg);
    }
    
    public boolean equals(Object obj) {
	return (obj instanceof ARPTimer);
    }
    
}

