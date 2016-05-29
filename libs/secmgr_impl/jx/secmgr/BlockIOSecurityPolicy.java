package jx.secmgr;

import jx.zero.*;
import java.util.*;

public class BlockIOSecurityPolicy implements DomainBorder {
    
    private static final boolean debug = false;
    /** the trusted computing base */
    Principal tcb;

    Naming naming;
    CentralSecurityManager secMgr;

    BlockIOSecurityPolicy() {
	this.tcb = new Principal_impl("TCB",0);
	this.naming = InitialNaming.getInitialNaming();
	this.secMgr = (CentralSecurityManager)naming.lookup("SecurityManager");
	if ( this.secMgr == null ) throw new Error("CentralSecurityManager not found");
     }
    
    public boolean outBound(InterceptOutboundInfo info) {
	if (debug) Debug.out.println("BIO OUTBOUND CALL ");
	return checkCall(info.getSourceDomain(), info.getTargetDomain(), info.getMethod(), info.getParameters() );
    }
    public boolean inBound(InterceptInboundInfo info) {
	if (debug) Debug.out.println("BIO INBOUND CALL");
	return 	checkCall(info.getSourceDomain(), info.getTargetDomain(), info.getMethod(), new Object[0]);
    }

    public boolean createPortal(PortalInfo info) {
	return true;
    }

    public void destroyPortal(PortalInfo info) {
    }
    
    private boolean checkCall(Domain sourceDom, Domain targetDom, VMMethod m, Object params[]) {
	Principal sourcePrincipal = secMgr.getPrincipal(sourceDom);
	Principal targetPrincipal = secMgr.getPrincipal(targetDom);
	if (debug) {
	    Debug.out.println("  from "+ sourcePrincipal +" ("+sourceDom.getName() + ")");
	    Debug.out.println("  to "+ targetPrincipal+" ("+targetDom.getName()+ ")" );
	    Debug.out.println("  Method: " + m.getName() + " " + m.getSignature());
	}
	
	if (sourcePrincipal.equals(tcb) && targetPrincipal.equals(tcb)) return true;
	Debug.out.println("REJECT CALL");
	return false;
    }

  }
