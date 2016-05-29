package jx.secmgr;

import jx.zero.*;
import java.util.*;

public class MySecurityPolicy implements DomainBorderIn,DomainBorderOut {
  
    private static final boolean debug = true;

    /** the trusted computing base */
    Principal tcb;

    Naming naming;
    CPUManager cpuManager;
    CentralSecurityManager secMgr;

    MySecurityPolicy() {
	this.tcb = new Principal_impl("TCB",0);
	this.naming = InitialNaming.getInitialNaming();
	this.secMgr = (CentralSecurityManager)naming.lookup("SecurityManager");
	if ( this.secMgr == null ) throw new Error("CentralSecurityManager not found");
     }
    
    public boolean outBound(InterceptOutboundInfo info) {
	if (debug) Debug.out.println("** OUTBOUND CALL ");
	boolean ret = true;
	ret = checkCall(info.getSourceDomain(), info.getTargetDomain(), info.getMethod(), info.getParameters() );
	return ret;
    }
    public boolean inBound(InterceptInboundInfo info) {
	if (debug) Debug.out.println("** INBOUND CALL");
	boolean ret = true;
	ret = checkCall(info.getSourceDomain(), info.getTargetDomain(), info.getMethod(), new Object[0]);
/*	VMObject obj = cpuManager.getVMObject();
	for (boolean status = info.getFirstParameter(obj); status == true; status = info.getNextParameter(obj)) {
	dumpVMObject(obj,"");
	}
*/
	return ret;
    }

    public boolean createPortal(PortalInfo info) {
	if (debug) Debug.out.println("** CREATE PORTAL ");
	return true;
    }

    public void destroyPortal(PortalInfo info) {
	if (debug) Debug.out.println("DESTROY PORTAL ");
    }
    
    private boolean checkCall(Domain sourceDom, Domain targetDom, VMMethod m, Object params[]) {
	Principal sourcePrincipal = secMgr.getPrincipal(sourceDom);
	Principal targetPrincipal = secMgr.getPrincipal(targetDom);
	if (debug) {
	    Debug.out.println("  from "+ sourcePrincipal +" ("+sourceDom.getName() + ")");
	    Debug.out.println("  to "+ targetPrincipal+" ("+targetDom.getName()+ ")" );
	    
	    Debug.out.println("  Method: " + m.getName() + " " + m.getSignature());
	    
	    Debug.out.println("    number Param: " + params.length);
	    for(int i=0; i<params.length; i++) {
		Debug.out.println("    Param: " + params[i]);
	    }
	}
	
	if (sourcePrincipal.equals(targetPrincipal)) return true;
	if (targetPrincipal.equals(tcb) ) return true;
	if (debug)  Debug.out.println("REJECT CALL");
	return false;
    }

    private void dumpVMObject (VMObject obj, String prefix)
    {
	VMClass cl = obj.getVMClass();
	if (cl == null)
	    Debug.out.println(prefix+"primitiver Datentyp: "+obj.getPrimitiveData());
	else {
	    Debug.out.print(prefix+cl.getName());
	    if (cl.getName().equals("java.lang.String"))
		Debug.out.println(": "+obj.getString());
	    else {
		Debug.out.println();
		VMObject obj2 = cpuManager.getVMObject();
		for (boolean status = obj.getFirstSubObject(obj2); status == true; status = obj.getNextSubObject(obj2)) {
		    VMClass cl2 = obj2.getVMClass();
		    dumpVMObject(obj2,prefix+"   ");
		}
	    }
	}
    }

 }
