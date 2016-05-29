package test.timer;

import jx.zero.*;
import jx.timer.*;

class MyTimer implements TimerHandler {
    public void timer(Object arg) {
	Debug.out.println("Hello World");
    }
}

public class Main {
    public static void main(String [] args) {
	Naming naming = InitialNaming.getInitialNaming();
	TimerManager timerManager = (TimerManager) LookupHelper.waitUntilPortalAvailable(naming, args[0]);
	timerManager.addMillisIntervalTimer(1000, 1000, new MyTimer(), null);
    }
}
