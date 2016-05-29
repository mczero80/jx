package jx.verifier.wcet;

import java.util.Vector;
import java.util.Enumeration;
import jx.verifier.*;
import jx.verifier.bytecode.*;

public class WCETState extends  JVMState {
    /**Indicates how many ticks the runtime time counter should increase after passing this bytecode.
     * Values <=0 are ignored. Else, code is added just BEFORE this bytecode to increase the time counter.
     */
    public int rTTimeCountIncrement = 0;
    /**If true, a timeCheck is performed at runtime, just BEFORE executing this bytecode.*/
    public boolean rTTimeCheck = false;
    public BasicBlock bBlock = null; //basic block which contains nextBC
    public int pass = 0; //wcet has four passes (0-3)
    public void setPass(int newpass) { if (newpass > pass) pass = newpass;}

    public boolean necessary = false; //if true, the nextBC bytecode must be executed during partial evaluation of loops.
    private OpenIfList oIfList;
    public ExceptionHandler simulateException = null;

    public OpenIfList getOIfList() {return oIfList;}
    //Constructors
    protected WCETState(WCETState other) {
	super(other.getNextBC(), 
	      other.getStack().copy(), 
	      other.getlVars().copy(), 
	      other.getMv());
	//	oIfList = other.getOIfList().copy();
	pass = other.pass;
    }
    /*protected WCETState(ByteCode nextBC, JVMOPStack stack, JVMLocalVars lVars, 
		       VerifierInterface mv
		       ) {
	super(nextBC, stack, lVars, mv);
	}*/
    public WCETState(ByteCode nextBC, 
		     int numVars, 
		     int maxStackSize,
		     MethodVerifier mv) 
	throws VerifyException {
	super(nextBC,
	      new WCETStack(maxStackSize),
	      new WCETLocalVars(mv.getClassName(), 
				mv.getTypeDesc(), 
				mv.isStaticMethod(),
				numVars),
	      (VerifierInterface) mv);
	//	oIfList = new OpenIfList();
    }

    public void srCalled(ByteCode srBegin) throws VerifyException {
	switch(pass) {
	case 0:
	    super.srCalled(srBegin);
	    WCETgetStack().push();
	    break;
	default:
	    throw new Error("Subroutines cannot be verified yet!");
	}
    }

    public WCETStack WCETgetStack() { return (WCETStack) getStack();}

    public void executeNextBC() throws VerifyException {
	switch(pass) {
	case 0:
	    super.executeNextBC();
	    return;
	case 1:
	    executeBCPass1();
	    return;
	case 2:
	    ValueProvider vProvider = (ValueProvider)getMv().getParameter();
	    executeBCPass2(vProvider);
	    return;
	default:
	    throw new Error("Internal Error");
	}
    }
    
    private void executeBCPass1() {
	ByteCode targets[] = getNextBC().getTargets();
	    //do Transformations on exsistant states, don't copy anymore
	    if (getNextBC().getOpCode() == ByteCode.JSR ||
		getNextBC().getOpCode() == ByteCode.JSR_W ) {
		    //FEHLER
		    System.err.println("Subroutines cannot be verified yet (WCET)!\n");
		    getMv().endChecks();
		    
		    /*//verify subroutine and merge resulting state into states of all ret targets
		      getMv().getSrs().transformState(tmpState, tmpState.getNextBC());*/
		    return;
		}
	
	//propagate pass
	for (int i=0; i < targets.length; i++) {
	    ((WCETState)targets[i].beforeState).setPass(pass);
	}
	
	/*	    if (getNextBC().getOpCode() == ByteCode.ATHROW) {
		    //AThrow ends execution at this point; 
		    //Exception handlers are verified separately
		    //FEHLER ?
		    return;
		    }
	*/
	
	if (targets.length ==0) return;
	/*
	boolean check = ((WCETState)targets[0].beforeState).
	getOIfList().join(this.oIfList);*/
	
	if (targets.length == 2) {
	    //FEHLER pruefen, ob es wirklich immer so ist, dass [0] next und [1] sprung ist
	    //	    check |= ((WCETState)targets[0].beforeState).
	    //	getOIfList().add(getNextBC().getAddress(), 0);
	    //if (check) getMv().checkBC(targets[0]);
	    
	    //	    check = ((WCETState)targets[1].beforeState).
	    //	getOIfList().join(this.oIfList);
	    //	    check |=  ((WCETState)targets[1].beforeState).
	    //	getOIfList().add(getNextBC().getAddress(), 1);
	    //if (check) getMv().checkBC(targets[1]);
	    
	} else {
	    //if (check) getMv().checkBC(targets[0]);
	}
	
	return;
    }
    
    /**Pass2: Mark all bytecodes that must be simulated. 
     * @throws VerifyException if a bytecode is necessary that cannot be simulated (e.g. getfield).
     */
    public void  executeBCPass2(ValueProvider vProvider) throws VerifyException {
	ByteCode[] sources = getNextBC().getSources();
	//FEHLER debug
	//System.out.println("vorher:\n" + this);
	if (getNextBC().getOpCode() == ByteCode.JSR ||
	    getNextBC().getOpCode() == ByteCode.JSR_W ) {
	    //FEHLER
	    System.err.println("Subroutines cannot be verified yet (WCET)!\n");
	    getMv().endChecks();
	    
	    /*//verify subroutine and merge resulting state into states of all ret targets
	      getMv().getSrs().transformState(tmpState, tmpState.getNextBC());*/
	    //return;
	    throw new VerifyException();
	}
	//propagate pass
	for (int i=0; i < sources.length; i++) {
	    ((WCETState)sources[i].beforeState).setPass(pass);
	}
	
	boolean stateChanged=false;
	WCETState otherState;
	int opCode = getNextBC().getOpCode();
	for (int i = 0; i < getNextBC().getTargets().length; i++) {
	    
	    otherState = (WCETState)getNextBC().getTargets()[i].beforeState;
	    Enumeration stack1 = getStack().elements();
	    Enumeration stack2 = otherState.getStack().elements();
	    
	    //if the result of this bytecode is marked in any of the target states,
	    //mark its operands in this states.
	    if (opCode == ByteCode.DUP_X1 ||
		opCode == ByteCode.DUP_X2 ||
		opCode == ByteCode.DUP2_X1 ||
		opCode == ByteCode.DUP2_X2 ||
		opCode == ByteCode.SWAP ) {
		//Special handling required for dup_x1, dup_x2, du2_x1, dup2_x2 and swap
		//because the result of these bytecodes is not at the top of the stack
		//and the stack order is changed
		//besides the topmost stack element is not always necessary

		//Stack...
		WCETStackElement[] ops;
		boolean [] res;
		if (opCode == ByteCode.SWAP) {
		    ops = new WCETStackElement[2];
		    res = new boolean[2];
		    ops[0] = ((WCETStackElement)stack1.nextElement());
		    ops[1] = ((WCETStackElement)stack1.nextElement());
		    res[1] = ((WCETStackElement)stack2.nextElement()).necessary;
		    res[0] = ((WCETStackElement)stack2.nextElement()).necessary;
		} else if(opCode == ByteCode.DUP_X1) {
		    ops = new WCETStackElement[2];
		    res = new boolean[2];
		    ops[0] = ((WCETStackElement)stack1.nextElement());//word1
		    ops[1] = ((WCETStackElement)stack1.nextElement());//word2
		    res[0] = ((WCETStackElement)stack2.nextElement()).necessary;//word1
		    res[1] = ((WCETStackElement)stack2.nextElement()).necessary;//word2
		    res[0] |= ((WCETStackElement)stack2.nextElement()).necessary;//word1
		} else if(opCode == ByteCode.DUP_X2) {
		    ops = new WCETStackElement[3];
		    res = new boolean[3];
		    ops[0] = ((WCETStackElement)stack1.nextElement());//word1
		    ops[1] = ((WCETStackElement)stack1.nextElement());//word2
		    ops[2] = ((WCETStackElement)stack1.nextElement());//word3
		    res[0] = ((WCETStackElement)stack2.nextElement()).necessary;//word1
		    res[1] = ((WCETStackElement)stack2.nextElement()).necessary;//word2
		    res[2] = ((WCETStackElement)stack2.nextElement()).necessary;//word3
		    res[0] |= ((WCETStackElement)stack2.nextElement()).necessary;//word1
		    res[1] |= ((WCETStackElement)stack2.nextElement()).necessary;//word2
		} else if(opCode == ByteCode.DUP2_X1) {
		    ops = new WCETStackElement[4];
		    res = new boolean[4];
		    ops[0] = ((WCETStackElement)stack1.nextElement());//word1
		    ops[1] = ((WCETStackElement)stack1.nextElement());//word2
		    ops[2] = ((WCETStackElement)stack1.nextElement());//word3
		    ops[3] = ((WCETStackElement)stack1.nextElement());//word4
		    res[0] = ((WCETStackElement)stack2.nextElement()).necessary;//word1
		    res[1] = ((WCETStackElement)stack2.nextElement()).necessary;//word2
		    res[2] = ((WCETStackElement)stack2.nextElement()).necessary;//word3
		    res[3] = ((WCETStackElement)stack2.nextElement()).necessary;//word4
		    res[0] |= ((WCETStackElement)stack2.nextElement()).necessary;//word1
		    res[1] |= ((WCETStackElement)stack2.nextElement()).necessary;//word2
		} else if(opCode == ByteCode.DUP2_X2) {
		    ops = new WCETStackElement[3];
		    res = new boolean[3];
		    ops[0] = ((WCETStackElement)stack1.nextElement());//word1
		    ops[1] = ((WCETStackElement)stack1.nextElement());//word2
		    ops[2] = ((WCETStackElement)stack1.nextElement());//word3
		    res[0] = ((WCETStackElement)stack2.nextElement()).necessary;//word1
		    res[1] = ((WCETStackElement)stack2.nextElement()).necessary;//word2
		    res[2] = ((WCETStackElement)stack2.nextElement()).necessary;//word3
		    res[0] |= ((WCETStackElement)stack2.nextElement()).necessary;//word1
		} else {
		    //this point should never be reached
		    throw new Error("Internal Error");
		}
		boolean oldNecessary = this.necessary;
		for (int j = 0; j < ops.length; j++) {
		    if (!ops[j].necessary  && res[j]) {
			this.necessary = true;
			stateChanged = true;
			ops[j].necessary = true;
			//Check if the (necessary) bytecode is within an exception handler
			//if so, stop simulation
			//FEHLER eigentlich nur die Simulation stoppen und zur LZÜ übergehen
			//aber kein Fehler schmeissen
			//FEHLER der check fehlt!!!
		    }
		}
		if (oldNecessary != this.necessary) {
		    //mark all ifs needed to reach this point
		    //		    oIfList.markOpenIfs(getMv());
		    //FEHLER eigentlich müsste da eine richtige Addresse stehen!
		    bBlock.markWithIf(0, this.getNextBC());
		}

		//local Variables... (just copy)
		WCETLocalVarsElement le1, le2;
		for (int j=0; j < getlVars().getNumVars(); j++) {
		    le1 = (WCETLocalVarsElement) getlVars().read(j);
		    le2 = (WCETLocalVarsElement) otherState.getlVars().read(j);
		    if (le2.necessary && !le1.necessary) {
			le1.necessary = true;
			stateChanged = true;
		    }
		}
	    } else {
		//all Bytecodes that pop and push some words but 
		//don't change the stack order
		int stackResult = BCEffectPass2.getStackResults(getNextBC());
		int stackOps = BCEffectPass2.getStackOperands(getNextBC());
		int[] lVarResult = BCEffectPass2.getLVarsResults(getNextBC());
		int[] lVarOps = BCEffectPass2.getLVarsOperands(getNextBC());
		boolean resultNecessary = false;

		//if propagateNecessary is set to false, the operands of the bc are not marked as necessary, even if the bc itself is marked. This is the case for bcs like <t>aload, which cannot be simulated anyway, so it would not make sense to mark its operands as necessary.
		boolean propagateNecessary = true;
		if (getNextBC().getOpCode() == ByteCode.INVOKEINTERFACE ||
		    getNextBC().getOpCode() == ByteCode.INVOKEVIRTUAL ||
		    getNextBC().getOpCode() == ByteCode.INVOKESPECIAL ||
		    getNextBC().getOpCode() == ByteCode.INVOKESTATIC) {
		    propagateNecessary = false;
		} else if (stackOps < BCEffectPass0.POP[getNextBC().getOpCode()]) {
		    propagateNecessary = false;
		    stackOps = BCEffectPass0.POP[getNextBC().getOpCode()];
		}
		
		//check if the result of this bytecode is necessary
		for (int j=0; j < stackResult;j++) { //loop over results
		    if(((WCETStackElement)stack2.nextElement()).necessary)
			{
			    resultNecessary = true;
			    break;
			}
		}
		//rest of stack is copied later.
		
		//check for necessary results in local Variables and copy local Vars
		WCETLocalVarsElement le1, le2;
		for (int j=0; j < getlVars().getNumVars(); j++) {
		    //if the local Variable is a result of this bytecode,
		    //this is necessary, and the local Variable is not necessary in the
		    //beforestate (because this bytecode will overwrite whatever was in it
		    //before!)
		    boolean isResult = false;
		    for (int k=0; k<lVarResult.length; k++) {
			if (j==lVarResult[k]) isResult = true;
		    }
		    if (isResult) { //this local var is a result of the bytecode
			resultNecessary |= ((WCETLocalVarsElement) otherState.getlVars().
					    read(j)).necessary;
			le1 = (WCETLocalVarsElement) getlVars().read(j);
			if (le1.necessary) {
			    //the local variable is not necessary because it will be overwritten
			    le1.necessary = false;
			    stateChanged = true;
			}
		    } else { //all other local variables (not result of this bytecode)
			//copy
			le1 = (WCETLocalVarsElement) getlVars().read(j);
			le2 = (WCETLocalVarsElement) otherState.getlVars().read(j);
			if (le2.necessary && !le1.necessary) {
			    le1.necessary = true;
			    stateChanged = true;
			}
		    }
		}  //end for..local vars
		
		
		if (resultNecessary || this.necessary) { //the result of this bytecode is needed, so its operands are necessary too!
		    stateChanged |= (necessary ^ true);
		    necessary = true;
		    //mark all ifs that are necessary to reach this bc
		    //oIfList.markOpenIfs(getMv());
		    
		    //check if this bytecode can be simulated or not. If not, information
		    //must already be available. Else, the method cannot be simulated!
		    if (getNextBC().getOpCode() == ByteCode.GETFIELD ||
			getNextBC().getOpCode() == ByteCode.GETSTATIC) {
			if (vProvider == null || !vProvider.valueAvailable(getNextBC())) {
			    throw new VerifyException();
			}
		    }

		    if (bBlock == null) {
			throw new Error("Internal Error: ByteCode " + getNextBC() + " has no Basic Block!");
		    }
		    //FEHLER eigentlich müsste da eine richtige Addresse stehen!
		    bBlock.markWithIf(0, this.getNextBC());
		    
		    for (int j=0; j<stackOps;j++) { //loop over operands
			WCETStackElement e = (WCETStackElement)stack1.nextElement();
			stateChanged |= (e.necessary ^ true);
			e.necessary = (true && propagateNecessary);
		    }
		    for (int j = 0; j < lVarOps.length; j++) {
			WCETLocalVarsElement e = (WCETLocalVarsElement)this.getlVars().
			    read(lVarOps[j]);
			stateChanged |= (e.necessary ^ true);
			e.necessary |= (true && propagateNecessary) ;
		    }
		} else {
		    //skip operands
		    for (int j=0; j<stackOps;j++) {
			stack1.nextElement();
		    }
		}

		//FEHLER debug
		//System.out.println("stack1: " + getStack() + "\nstack2: " + otherState.getStack() + "\nbCode: " + getNextBC()); 
		//System.out.println("POP: " + stackOps + " PUSH: " + stackResult );
		//copy the rest of the stack
		//at this point, nextElement() of stack1 and stack2 point to the first 
		//element on the stack that ist not touched by this bytecode
		//thus, both contain the same number of elements!
		//mark all stack/lvars-elements of this state, if they are marked in any 
		//of the target states.
		WCETStackElement e1, e2;
		while(stack1.hasMoreElements()) {
		    e1 = (WCETStackElement) stack1.nextElement();
		    e2 = (WCETStackElement) stack2.nextElement();
		    if (e2.necessary && !e1.necessary) {
			e1.necessary = true;
			stateChanged = true;
		    }
		}
		//just make sure both stack1 and stack2 really were of the same size
		if (stack2.hasMoreElements()) {
		    throw new Error("WCET - Internal Error: Stacks of different size!");
		}

	    } //endif dup, swap
	    
	    
	    
	} //endfor targetstates
	//if this state has changed, register all previous states for checking.
	if (stateChanged) {
	    for (int i = sources.length-1; i >=0 ; i--) {
		//System.out.println("\t" + sources[i]);
		getMv().checkBC(sources[i]);
	    }
	}
	//FEHLER debug
	//System.out.println("nacher:\n" + this+"\n***********************************************");
	return;
    }
    
    //Simulate execution of nextBc on this stack.
    //returns the resulting states, with nextBC correctly set to the next target
    //NOTE: the returning states are directly used and NOT copied.
    //return JVMState[0] to end this verification path.
    //if result is null, all in nextBC.getTargets() will be verified with this as beforeState
    //throws exception if verification fails.
    protected JVMState[] doExecuteNextBC() throws VerifyException {
	switch (pass) {
	    case 0:
	    return BCEffectPass0.simulateBC(this);
	    case 1:
		//this should never be reached, as pass 1 is handled at executeNextBC
		throw new Error("Internal Error!"); 
	    case 2:
	    //return BCEffectPass2.simulateBC(this);
	    case 3:
	    //return BCEffectPass3.simulateBC(this);
	    default:
	    throw new Error("Internal Error!");
	}
    }
    
    //copy this state.
    //should return something created with
    //new JVMState(nextBC, stack.copy(), lVars.copy(), mv);
    public JVMState copy() {
	return new WCETState(this);
    }
    //transform state as if an Exception of type eName had been thrown,
    //i.e. usually clear stack, push an object of type eName onto stack and change nextBC.
    public void exceptionThrown(String eName, ByteCode handler) 
	throws VerifyException {
	switch(pass) {
	case 0:
	    setStack(new WCETStack(getStack().getMaxSize()));
	    WCETgetStack().push();
	    setNextBC(handler);
	    break;
	default:
	    throw new Error("Exceptions cannot be verified yet");
	}
    }


    //override merge..
    public boolean merge(JVMState mergeState) throws VerifyException {
	switch(pass) {
	case 0:
	    return false;  //if two states are merged there is never a change in pass 0!
	default:
	    throw new Error("Internal Error");
	}
	
    }

    //returns true if the bytecode cannot be simulated, false if it can.
    public static boolean notSimulatable(ByteCode bCode) {
	switch(bCode.getOpCode()) {
	case ByteCode.ARRAYLENGTH:
	case ByteCode.GETFIELD:
	case ByteCode.IALOAD:
	case ByteCode.LALOAD:
	case ByteCode.FALOAD:
	case ByteCode.DALOAD:
	case ByteCode.AALOAD:
	case ByteCode.BALOAD:
	case ByteCode.CALOAD:
	case ByteCode.SALOAD:
	    return true;
	default:
	    return false;
	}

    }

    public String toString() {
	String ret = "beforeState of " + getNextBC();
	ret += "\npass:" + pass;
	switch(pass) {
	case 0:
	    ret += "\nStacksize: " + getStack().getCount();
	    ret += "\nnextBC: " + getNextBC();
	    break;
	case 1:
	    ret += "\nOpen ifs: " + oIfList;
	    break;
	case 2:
	    ret += "\nNecessary: " + necessary;
	    ret += "\nStack: " + getStack();
	    ret += "\nlVars: " + getlVars();
	    break;
	default:
	    ret += "\nPass not implemented";
	}
	ret += "\n";
	return ret;
    }


}

