package test.portal.perf;

import jx.zero.*;
import jx.zero.debug.*;

public class TargetDomain_Impl implements TargetDomain, Service {

    public static void init(Naming naming) {
	new TargetDomain_Impl(naming);
    }

    public TargetDomain_Impl(Naming naming) {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));
	Debug.out.println("TargetDomain_Impl is speaking");
	
	TargetDomain target = this;
	naming.registerPortal(target, "Target");
    }

    public void noparam() {}
 
    public void intparam(int p) {}
      
    public int test(TestObject o, int v){
	//Debug.out.println("test called.");
	return 0;
    }

    public void objparam(NullObject p) {}
   public void listparam(ListObject p) {}
   public void arrparam(NullObject[] p) {}
    public void gc() {
	Naming naming = InitialNaming.getInitialNaming();
	DomainManager domainManager = (DomainManager) naming.lookup("DomainManager");
	domainManager.gc(domainManager.getCurrentDomain());
    }
}
