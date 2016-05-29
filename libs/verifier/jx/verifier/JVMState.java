package jx.verifier;

import java.util.Vector;
import jx.verifier.*;
import jx.verifier.bytecode.*;

/** Class to hold all necessary information about the state of the JVM simulated during verification.*/
abstract public class JVMState {

    /**Simulate execution of nextBc on this state.
     * called by executeNextBC.
     * @return the resulting states, with nextBC correctly set to the next target.<br> NOTE: the returning states are directly used and NOT copied. Return JVMState[0] (i.e. empty array) to end this verification path. If result is null, all in nextBC.getTargets() will be verified with this as beforeState (resp. a copy of this)
     * @exception VerifyException if verification fails (e.g. because arguments to nextBC are of incorrect type).
     * @see JVMState#executeNextBC
     */
    abstract protected JVMState[] doExecuteNextBC() throws VerifyException;
    /**copy this state.
     * should return something created with<br>
     * <code>new JVMState(nextBC, stack.copy(), lVars.copy(), mv);</code><br>
     */
    abstract public JVMState copy();
    /**transform state as if an Exception of type eName had been thrown.
     * i.e. usually clear stack, push an object of type eName onto stack and change nextBC.
     * @param eName name of the exception thrown.
     * @param handler start of the exceptionhandler for this exception.
     * @exception VerifyException if verification fails
     */
    abstract public void exceptionThrown(String eName, ByteCode handler) 
	throws VerifyException;


    /**the verifier that verifies this method or subroutine*/
    private VerifierInterface mv;
    /**the operand stack*/
    private JVMOPStack stack;
    /**local variables */
    private JVMLocalVars lVars;
    /** holds the next bytecode to be executed*/
    private ByteCode nextBC;

    /**get the verifier that verifies this method or subroutine*/
    public VerifierInterface getMv(){return mv;}
    /**set the verifier that verifies this method or subroutine*/
    public void setMv(VerifierInterface mv) {this.mv = mv;}
    /**get the operand stack*/
    public JVMOPStack getStack() { return stack;}
    /**set the operand stack*/
    public void setStack(JVMOPStack stack) {this.stack = stack;}
    /**get local variables */
    public JVMLocalVars getlVars() { return lVars;}
    /**set local variables */
    public void setlVars(JVMLocalVars lVars) { this.lVars = lVars;}
    /** get the next bytecode to be executed*/
    public ByteCode getNextBC() { return nextBC; }
    /**set  the next bytecode to be executed*/
    public void setNextBC(ByteCode nextBC) {this.nextBC = nextBC;}

    /**to be called, if a subroutine beginning at 'srBegin' should be analyzed
     * @exception VerifyException if verification fails*/
    public void srCalled(ByteCode srBegin) throws VerifyException {
	this.setNextBC(srBegin);
    }

    /**simulates the effect of the next ByteCode on this state and set the resulting states.
     * First, the state is copied. If the nextBC is a subroutinecall, the subroutine is verified. Else, if the code is protected by an exception handler, the handler is verified.<br>
     * Then doExecuteNextBC() is called and the results returned are merged into the corresponding states. If doExecuteNextBC() returns null, the state is merged with all targets of nextBC. <br>
     * All bytecodes whose beforeState changed during this process are marked for verification (by calling mv.checkBC()).
     * @exception VerifyException if some Verification failure occurs during doExecuteNextBC() or during merging.
     * @see VerifierInterface#checkBC
     * @see JVMState#merge
     */
    public void executeNextBC() throws VerifyException {
	JVMState tmpState = this.copy();
	
	//FEHLER prüfen, ob das auch überall in den abgeleiteten states richtig abgefangen wird!
	/*	//if execution end here, just return
	//FEHLER das gehört eigentlich HIER nicht her!
	if (nextBC.getOpCode() == ByteCode.ATHROW) {
	    //AThrow ends execution at this point; 
	    //Exception handlers are verified separately
	    //FEHLER ?
	    return;
	}
	*/
	//check if subroutine should be called
	if (nextBC.getOpCode() == ByteCode.JSR ||
	    nextBC.getOpCode() == ByteCode.JSR_W ) {
	    //verify subroutine and merge resulting state into states of all ret targets
	    tmpState.getMv().getSrs().transformState(tmpState, tmpState.getNextBC());
	    return;
	}

	//check all ExceptionHandlers
	if (nextBC.eHandlers != null) {
	    for (int i = 0; i < nextBC.eHandlers.length; i++) {
		//state for the exception is 'this', because if an exception is thrown
		//the bytecode has not changed the state yet!
		nextBC.eHandlers[i].simulateException(this);
	    }
	}

	//normal Simulation
	JVMState targetStates[] = null;
	try {
	    //Simulate State Changes
	    targetStates = tmpState.doExecuteNextBC();
	} catch (VerifyException e) {
	    //set the state to the state before executing the faulty bytecode
	    e.state = this;
	    throw e;
	}
	
	// Merge States for all successors
	boolean check = false;
	if (targetStates == null) {
	    //verify all targets of this bytecode
	    ByteCode[] targets = nextBC.getTargets();
	    check = false;
	    for (int i = 0; i < targets.length; i++) {
		check = false;
		if (targets[i].beforeState != null ) {
		    check = targets[i].beforeState.merge(tmpState);
		} else { //target's state was null
		    //i=0 gets tmpState itself, all others just get copies.
		    targets[i].beforeState = (i==0)? tmpState: tmpState.copy();
		    targets[i].beforeState.setNextBC(targets[i]);
		    check = true;
		}
		if (check) {
		    mv.checkBC(targets[i]);
		}
	    }
	} else {
	    for (int i = 0; i < targetStates.length; i++) {
		check = false;
		//if the 'nextBC' of the targetState already has a state, merge the two states
		//else, just tell that nextBC its new beforeState
		if (targetStates[i].getNextBC().beforeState != null ) {
		    check = targetStates[i].getNextBC().beforeState.merge(targetStates[i]);
		} else { //target's state was null
		    targetStates[i].getNextBC().beforeState = targetStates[i];
		    check = true;
		}
		if (check) {
		    mv.checkBC(targetStates[i].getNextBC());
		}
		
	    }
	}
    }

    
    /**Constructor for copy*/
    protected JVMState(JVMState other) {
	this(other.getNextBC(), other.getStack(), other.getlVars(), other.getMv());
    }
    /**Constructor.*/
    protected JVMState(ByteCode nextBC, JVMOPStack stack, JVMLocalVars lVars, 
		     VerifierInterface mv
		     ) {
	setNextBC(nextBC);
	this.stack = stack;
	this.lVars = lVars;
	this.mv = mv;
	//this.lastExecutedBC = lastExecutedBC;
    }

    /*Merge two states.
     * Checks Stack and Local Variables Consistency by calling merge for stack and local variables. When called from executeNextBC() the method is called at the state of the bytecode that already existed, not at the newly created state.
     * @param mergeState the state that should be merged with this state.
     * @return true, if states were not completely the same, so the bc has to be reverified
     * @exception VerifyException if states are not the same at all (so the exception is thrown by stack.merge or lvars.merge
     */
    public boolean merge(JVMState mergeState) throws VerifyException {
	//FEHLER debug
	//	System.out.println("this:\n" + this + "\nother:\n" + mergeState);
	boolean retval = false;
	try {
	    retval = stack.merge(mergeState.getStack()) | lVars.merge(mergeState.getlVars());
	} catch (VerifyException e) {
	    e.append("\nState for bytecode " + nextBC);
	    throw e;
	}
	//System.out.println("result:\n" + this+"\n" +retval+"\n------------------------------------");
	return retval;
	
    }

    public String toString() {
	String ret = "";
	ret += "Stack:" + stack;
	ret += "\nlocal Variables:" + lVars;
	ret += "\nnextBC: " + nextBC;
	ret += "\n";
	return ret;
    }
}

