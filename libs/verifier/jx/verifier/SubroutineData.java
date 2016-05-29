package jx.verifier;

import java.util.Vector;
import java.util.Enumeration;

import jx.classfile.*;
import jx.classfile.constantpool.*;
import jx.verifier.bytecode.*;

/** Class to hold data for one Subroutine.
 * each instance has a Vector of callers that call this subroutine, the JVMState just before the first instruction of the subroutine and the state just before the ret instruction.<br>
 * When a jsr to this subroutine is encountered, the subroutine is verified once. When other jsrs are encountered, the subroutine is only verified again, if the state before its execution changed. If that is the case, the resulting state is set at all points which already called the subroutine (of cause only if the resulting state changed).
 */

public class SubroutineData  {
    private Vector jsrs;
    private int beginAddress;
    JVMState beforeState;
    JVMState afterState;
    JVMState finalState;

    /**returns the startaddress of the subroutine.*/
    public int getBeginAddress(){return beginAddress;}

    /**Constructor.
     * @param jsr a jsr which branches to this subroutine. Usually the first one encountered in the bytecode. Later occurences are added with "add".
     * @see SubroutineData#add
     */
    public SubroutineData (ByteCode jsr) {
	BCBranch b = (BCBranch) jsr;
	beginAddress = b.getTargetAddress();
	jsrs = new Vector(1);
	jsrs.addElement(jsr);
    } 

    /**add jsr to list of callers.*/
    public void add(ByteCode jsr) {
	//add(jsr.getAddress());
	jsrs.addElement(jsr);
    }

    /**get the state just before execution of the subroutine.*/
    public JVMState getBeforeState() {
	return beforeState;
    }

    /**set the state just before the execution of the subroutine.
     * the local variables of "bState" are converted to JVMSRLocalVars.
     *@see JVMSRLocalVars
     */
    public void setBeforeState(JVMState bState) {
	bState.setlVars(bState.getlVars().toSRLVars());
	beforeState = bState;
    }
    /**Get the staete just after execution of the subroutine.*/
    public JVMState getAfterState() {
	return afterState;
    }
    /**Set the state just after execution of the subroutine. 
     * This is done by the subroutineverifier.
     */
    public void setAfterState(JVMState state) {
	afterState = state;
    }

    /**Update the states of all callers if the state jsut after execution changed.
     * all caller's states (actually the states of the bytecode just after the caller - which is the bytecode to which control returns after the subroutine) are updated by merging with this subroutine's afterstate.
     * If a caller does not have a beforeState yet, a new state is constructed by copying the afterstate, changing its local variables to those of the beforeState and applying the SRlVars to these.
     * If the state of a caller changes, it is checked againg (by calling mv.checkBC()).
     * @exception VerifyException if merging fails.
     */
    public void updateCallerStates() throws VerifyException {
	for (int i = 0; i < jsrs.size(); i++) {
	    ByteCode actJsr = (ByteCode) jsrs.elementAt(i);
	    ByteCode nextBC = actJsr.next;
	    //merge afterState and nextBC.beforeState - normal merge for stack,
	    //apply for lVars.
	    if(actJsr.beforeState == null)
		continue;
	    boolean checkAgain = false;
	    if (nextBC.beforeState == null) {
		//if the next bc after the jsr has no state yet generate new one:
		//the state is mainly the afterstate of this sr, and the srlVars are applied
		//to the lvars of the jsr-state
		checkAgain = true;
		nextBC.beforeState = afterState.copy();
		nextBC.beforeState.setNextBC(nextBC);
		nextBC.beforeState.setlVars(actJsr.beforeState.getlVars());
		((JVMSRLocalVars)afterState.getlVars()).apply(nextBC.beforeState.getlVars());
	    } else {
		checkAgain = 
		    nextBC.beforeState.getStack().merge(afterState.getStack());
		checkAgain = checkAgain | 
		    ((JVMSRLocalVars)afterState.getlVars()).apply(nextBC.beforeState.getlVars());
		
	    }
	    if (checkAgain) {
		    //merging changed stack or local Variables --> reverify
		nextBC.beforeState.getMv().checkBC(nextBC);
	    }
	}
    }

    public String toString() {
	String ret = "\tbeginAddress: " + Integer.toHexString(beginAddress)+"\n";
	for (int i=0; i < jsrs.size(); i++) {
	    ByteCode tmp = (ByteCode) jsrs.elementAt(i);
	    ret += "\tcalled at address " + Integer.toHexString(tmp.getAddress()) + "\n";
	}
	return ret;
    }

} // SubroutineData
