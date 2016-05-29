package jx.secmgr;

import jx.zero.*;
import java.util.*;
import jx.zero.debug.*;

public class SecMgr implements Service, CentralSecurityManager {
    
    Hashtable domain2principal = new Hashtable();
    Hashtable domain2interceptor = new Hashtable();
    
    /** the trusted computing base */
    Principal tcb;
    Naming naming;
    CPUManager cpuManager;

    public static void init(Naming naming, String[]  args) {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));
	new SecMgr(naming);
    }

    public SecMgr() {
	this(InitialNaming.getInitialNaming());
    }

    public SecMgr(Naming naming) {
	this.tcb = new Principal_impl("TCB",0);
	this.naming = naming;
	cpuManager =  (CPUManager)naming.lookup("CPUManager");

        DomainManager domainManager = (DomainManager)naming.lookup("DomainManager");
	Domain dz = domainManager.getDomainZero();
	addDomainAndPrincipal(dz, tcb);
	addDomainAndPrincipal(domainManager.getCurrentDomain(), tcb);
	naming.registerPortal(this, "SecurityManager");
	installInterceptor(domainManager.getCurrentDomain(), "jx.secmgr.SecMgrSecurityPolicy");
   }
    
    public void addDomainAndPrincipal(Domain d, Principal p) {
	domain2principal.put(d, p);
    }

    public Principal getPrincipal(Domain d) {
	return (Principal)domain2principal.get(d);
    } 
    public void inheritPrincipal(Domain source, Domain destination) {
	Debug.out.println(destination.getName() + " inherits Principal from "+source.getName());
	if (domain2principal.containsKey(destination) )
	    return;   /* Domain already has a Principal */
	Principal p = getPrincipal(source);
	if (p == null) return;
	addDomainAndPrincipal(destination,  p);
    }

    public void inheritInterceptor(Domain source, Domain destination) {
	return; // this SecMgr does not inherit Interceptors 
/*
	if (domain2interceptor.containsKey(destination) )
	    return;   // Domain already has a Interceptor 
	String i = (String)domain2interceptor.get(source);
	if (i == null) return;
	installInterceptor(destination, i);
*/
    }
    
    public void installInterceptor(Domain domain, String interceptorClass){
	// create interceptor thread
	CPUState interceptor_thread = cpuManager.createCPUState(new ThreadEntry() {
		public void run() {}
	    });
	
	DomainManager domainManager = (DomainManager)naming.lookup("DomainManager");
	DomainBorder interceptor = null;

	VMClass cl = cpuManager.getClass(interceptorClass);
	interceptor = (DomainBorder)cl.newInstance();
	if (interceptor != null) {
	    domainManager.installInterceptor(domain, interceptor, interceptor_thread);
	    domain2interceptor.put(domain, interceptorClass);
	}
    }

 }
