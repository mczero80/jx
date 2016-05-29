package test.portal;

import jx.zero.*;
import jx.zero.debug.*;

class GCDaddy implements Daddy, Service {
    int c;

    public void longRunning() {
    }

    public void inc() {
	for(int i=0; i<1000; i++) {Object []x = new Object[100];}
	c++;
    }

    public int getCount() {
	return c;
    }
}

public class GCTest {

    public static void main(String[] args) {
	Naming naming = InitialNaming.getInitialNaming();

        CPUManager cpuManager = (CPUManager)naming.lookup("CPUManager");
        DomainManager dm = (DomainManager)naming.lookup("DomainManager");

	// create portal
	Daddy daddy = new GCDaddy();
	naming.registerPortal(daddy, "Daddy");
	
	Domain domain1 = DomainStarter.createDomain("DomainHans",
						"test_portal.jll",
						"test/portal/MainHans",
						2*1024*1024);
	
	for(int i=0; i<5; i++) {
	    DomainStarter.createDomain("DomainAlice"+i,
				    "test_portal.jll",
				    "test/portal/MainAlice",
				    1*1024*1024);
	}

    }
}
