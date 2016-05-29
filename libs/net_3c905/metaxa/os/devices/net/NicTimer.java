package metaxa.os.devices.net;
import metaxa.os.*;

import jx.timer.*;
import jx.zero.*;

class NicTimer implements TimerHandler {

    IRQ irq;
    TimerManager timerManager;
    NicTimer(IRQ irq, TimerManager timerManager) {
	this.irq = irq;
	this.timerManager = timerManager;
    }

    public void timer(Object arg) {
	NicInformation Adapter = ((NicTimerArg)arg).get_NicInfo();
	D3C905 handle = ((NicTimerArg)arg).get_Handle();
	
	synchronized (Adapter.lock) {
	    
	    Adapter.InTimer = true;
	    
	    irq.disableIRQ(Adapter.PCI.get_Interrupt());
	    Adapter.Statistics.UpdateInterval +=  Adapter.Resources.get_TimerInterval();
	    if ((Adapter.Statistics.UpdateInterval) > 1000) {
		
		Adapter.Statistics.UpdateInterval = 0;
		handle.UpdateStatisticsEvent();
	    }
	    
	    //
	    // Check every five seconds for media changed speed or duplex
	    //
	    
	    Adapter.Hardware.set_UpdateInterval(Adapter.Hardware.get_UpdateInterval() +  Adapter.Resources.get_TimerInterval());
	    if ((Adapter.Hardware.get_UpdateInterval()) > 5000) {
		
		Adapter.Hardware.set_UpdateInterval(0);
		handle.TickMediaHandler();
	    }
	    
	    irq.enableIRQ(Adapter.PCI.get_Interrupt());
	    
	    timerManager.addMillisTimer(1000, new NicTimer(irq, timerManager), arg);
	    
	    Adapter.InTimer = false;
	}
    }
    
    public boolean equals(Object obj) {
	return (obj instanceof NicTimer);
    }
    
}
