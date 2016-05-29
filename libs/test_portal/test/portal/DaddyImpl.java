package test.portal;

import jx.zero.*;

public class DaddyImpl implements Daddy, Service {
    private int count;
    CPUManager cpuManager;
    int c;

    DaddyImpl(CPUManager cpuManager) {
	this.cpuManager = cpuManager;
    }

    public void longRunning() {
	cpuManager.assertInterruptEnabled();
	//	for(int j=0; j<1000000; j++) {
	    for(int i=0; i<1000000; i++) {
		c = c + 1;
	    }
	    //}
    }

    public void inc() {
	cpuManager.assertInterruptEnabled();
	count++;
    }

    public int getCount() {
	return count;
    }
}
