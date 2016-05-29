package test.intercept;

import jx.zero.*;
import jx.zero.debug.*;
import jx.secmgr.*;

public class Main {
    public static void init(Naming naming, String args[]) {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));
	Main.main(args);
    }

    public static void main(String args[]) {
	Naming naming = InitialNaming.getInitialNaming();
	Debug.out.println("Domain test.interceptor speaking");

        CPUManager cpuManager = (CPUManager)naming.lookup("CPUManager");
        DomainManager domainManager = (DomainManager)naming.lookup("DomainManager");

	CentralSecurityManager secmgr = (CentralSecurityManager)naming.lookup("SecurityManager");
	if ( secmgr == null ){ 
	    Debug.out.println("CentralSecurityManager not found");
	    throw new Error("CentralSecurityManager not found");
	}

	Debug.out.println("Domain test.interceptor speaking again ");

	secmgr.installInterceptor(domainManager.getCurrentDomain(),"jx.secmgr.MySecurityPolicy");
	domainManager.getCurrentDomain().clearTCBflag();

	// create portal
	DaddyImpl daddy = new DaddyImpl();
	naming.registerPortal(daddy, "Daddy");
	
	// create Domains
	Domain domain1 = DomainStarter.createDomain("HansDomain", "test_intercept.jll","test/intercept/MainHans",
						    2*1024*1024,100000,naming,"jx.secmgr.MySecurityPolicy");
	
	Domain domain2 = DomainStarter.createDomain("AliceDomain", "test_intercept.jll","test/intercept/MainAlice",
						    2*1024*1024, 100000, naming,"jx.secmgr.MySecurityPolicy");


	// add Principals to Domains
	Principal hans = new Principal_impl("Hans",100);
	Principal alice = new Principal_impl("Alice",200);
	secmgr.addDomainAndPrincipal(domain1, hans);
	secmgr.addDomainAndPrincipal(domain2, alice);
    }
}
