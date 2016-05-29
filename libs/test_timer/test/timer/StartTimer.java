package test.timer;

import jx.zero.*;
import jx.timer.*;
import timerpc.*;

public class StartTimer {
    public static void main(String [] args) {
	TimerManager timerManager = new TimerManagerImpl();
	Naming naming = InitialNaming.getInitialNaming();
	naming.registerPortal(timerManager, args[0]);
    }
}
