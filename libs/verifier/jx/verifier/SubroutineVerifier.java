package jx.verifier;

import java.util.Vector;
import jx.classfile.*;
import jx.classfile.constantpool.*;
import jx.verifier.bytecode.*;

/**Class for verification of a subroutine within a method.
 * 
 */
public class SubroutineVerifier implements VerifierInterface {
    private Vector checkQueue;
    private ByteCode srBegin;
    private JVMState beginState;
    private SubroutineData srData;
    private Subroutines srs;
    private String className;
    Object vParameter;
    private MethodSource method;

    public String getClassName() { return className;}
    public MethodSource getMethod() {return method;}
    public SubroutineData getSrData() { return srData;}
    public void setSrData(SubroutineData s) {srData = s;}

    public Subroutines getSrs() { return srs;}

    /**returns the VerifierParameter. 
     * @return parameter or null.
     */
    public Object getParameter(){return vParameter;}
    public SubroutineVerifier(ByteCode srBegin,
			      SubroutineData srData,
			      Subroutines srs,
			      String className, 
			      MethodSource method) throws VerifyException {
	this.srs = srs;
	this.srData = srData;
	this.srBegin = srBegin;
	this.className = className;
	this.beginState = srData.getBeforeState().copy();
	this.beginState.srCalled(srBegin);
	this.method = method;
    }

    public SubroutineVerifier(ByteCode srBegin,
			      SubroutineData srData,
			      Subroutines srs,
			      String className,
			      MethodSource method,
			      Object vParameter) throws VerifyException {
	this(srBegin, srData, srs, className, method);
	this.vParameter = vParameter;
    }


    public JVMState getFinalState() {
	return srData.finalState;
    }
    public void runChecks() throws VerifyException {
	Verifier.stdPrintln(1,"Now Verifying subroutine starting at " + Integer.toHexString(srBegin.getAddress()));
	checkQueue = new Vector();
	ByteCode actBC = srBegin;
	actBC.beforeState = beginState;
	actBC.beforeState.setMv(this);

	checkBC(actBC);
	while (!checkQueue.isEmpty()) {
	    //Check Queue must be LIFO!!!
	    // Else Verification of one subroutine could be startet twice!!
	    actBC = (ByteCode) checkQueue.lastElement();
	    checkQueue.removeElementAt(checkQueue.size()-1);
	    actBC.svCheckCount = 0;
	    
	    if (actBC.getOpCode() == ByteCode.RET) {
		if (srData.finalState == null) {
		    srData.finalState = actBC.beforeState;
		} else {
		    srData.finalState.merge(actBC.beforeState);
		}
	    } else try {  //bc!=ret
		actBC.beforeState.executeNextBC();
	    } catch(VerifyException e) {
		e.srBCode = e.bCode; //the JVMState of the calling jsr instruction will set bCode to jsr so save real bcode in srBCode!
		e.append("in subroutine beginning at " + 
			 Integer.toHexString(srBegin.getAddress()));
		throw e;
	    }
	}
	if (srData.finalState == null) {
	    throw new VerifyException("Subroutine ended without return!");
	}
	Verifier.stdPrintln(1,"Verification of Subroutine done");

    }

    public void checkBC(ByteCode e) {
	if (e.svCheckCount > 0)
	    return; //e is already in checkQueue
	e.svCheckCount++;
	checkQueue.addElement(e);

    }
    //check is stopped, and runChecks / continueCheck will return
    //checkQueue is emptied!
    public void endChecks() {
	checkQueue = new Vector(0);
    }
}
