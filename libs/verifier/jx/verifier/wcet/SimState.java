package jx.verifier.wcet;

import jx.classfile.*;
import jx.classfile.constantpool.*;
import jx.classstore.ClassFinder;
import jx.verifier.bytecode.*;
import jx.verifier.*;

//Class for Simulated state needed for partial evaluation of loops
public class SimState {
    //local variables
    public SimData lVars[];
    //holds the time this method has already needed for execution
    public ExecutionTime eTime;
    //next Bytecode to be executed
    public ByteCode nextBC;    
    //last if that was "opened"
    public OpenIfListElement openIf = null;

    //set to true if information needed for simulation was not provided by valueprovider
    private  boolean iM = false;
    public void setInformationMissing() {
	iM = true;
    }
    public boolean informationMissing() {return iM;}
    public boolean timeExceeded = false;
    private SimData stack[];
    private int stackIndex = 0;
    public ClassFinder classFinder;

    private ValueProvider getValueProvider() { 
	return (ValueProvider)nextBC.beforeState.getMv().getParameter();
    }

    //pop data-word from stack
    public SimData pop() {
	stackIndex--;
	return stack[stackIndex];
    }

    //look at topmost data-word from stack
    public SimData peek() {
	return stack[stackIndex-1];
    }
    
    //push data-word on Operand stack
    public void push(SimData newElement) {
	stack[stackIndex] = newElement; 
	stackIndex++;
    }
    
    //throw away stack contents and set to empty stack.
    public void clearStack() {
	stackIndex = 0;
    }
    //returns the number of elements currently on the stack
    public int stackCount() {
	return stackIndex;
    }

    //returns the maximum size of the stack
    public int stackSize() {
	return stack.length;
    }

    //returns the number of local variables
    public int lVarSize() {
	return lVars.length;
    }

    public SimState(int lVarsSize, int stackSize, ClassFinder classFinder) {
	lVars = new SimData[lVarsSize];
	stack = new SimData[stackSize];
	stackIndex = 0;
	eTime = new SimpleExecutionTime();
	this.classFinder = classFinder;
    }

    //returns completely independant copy of this
    public SimState copy() {
	SimState cp = new SimState(lVars.length, stack.length, classFinder);
	for (int i = 0; i < lVars.length; i++) {
	    cp.lVars[i] = lVars[i];
	}
	for (int i = 0; i < stackIndex; i++) {
	    cp.push(stack[i]);
	}
	cp.eTime = eTime.copy();
	cp.nextBC = nextBC;
	cp.openIf = openIf;
	return cp;
    }

    //merge Simstates this and other into one state
    //returns the new state; 
    //NOTE: 'other' and 'this' could be changed, besides the returned state might be 
    //a reference to other or this.
    public SimState merge(SimState other) {
	//FEHLER zur Sicherheit müssten hier eigentlich noch die Stack und l-Var inhalte auf 
	//Konsistenz geprüft werden!
	if (this.stackCount() != other.stackCount()) {
	    throw new Error("Internal Error: merging SimStates with stacks of different size!");
	}
	//take maximum time of both as the new time
	this.eTime.max(other.eTime);
	
	return this;
    }


    /**execute state.nextBC on this state, for BCs that are necessary for partial evaluation
     * @return true if execution should continue, else false
     */

    public boolean executeBC()  {
	
	//FEHLER debug
	//System.out.println("Simulating!");
    	//add the execution time of this bytecode to the total time
	eTime.addTimeOfBC(nextBC);
	timeExceeded = eTime.limitExceeded();

	//special simulation: simulate bytecode
	int opCode = nextBC.getOpCode();
	SimData tmpData1, tmpData2, tmpData3;
	int tmpInt1, tmpInt2, tmpInt3;
	switch (opCode) {
	case ByteCode.ICONST_M1:
	    push(new SimInt(nextBC.getAddress(), -1));
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.ICONST_0:
	    push(new SimInt(nextBC.getAddress(), 0));
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.ICONST_1:
	    push(new SimInt(nextBC.getAddress(), 1));
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.ICONST_2:
	    push(new SimInt(nextBC.getAddress(), 2));
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.ICONST_3:
	    push(new SimInt(nextBC.getAddress(), 3));
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.ICONST_4:
	    push(new SimInt(nextBC.getAddress(), 4));
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.ICONST_5:
	    push(new SimInt(nextBC.getAddress(), 5));
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.BIPUSH:
	    push(new SimInt(nextBC.getAddress(), ((int)nextBC.getByteArgs()[0])));
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.SIPUSH:
	    push(new SimInt(nextBC.getAddress(), 
				  ((((int)nextBC.getByteArgs()[0]) & 0xff)<<8) |
				  ((((int)nextBC.getByteArgs()[1]) &0xff))));
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.LDC:
	case ByteCode.LDC_W:
	    throw new Error("Internal Error: '" + nextBC + "' not implemented.");
	    //break;
	case ByteCode.ILOAD:
	    if (!(lVars[nextBC.getByteArgs()[0]] instanceof SimInt)) {
		//throw an error instead of an exception, as this method should
		//already have been typechecked, so if this happens, there is nothing
		//wrong with the bytecode, but with the verifier
		throw new Error("Internal Error. Type Mismatch");
	    }
	    push(lVars[nextBC.getByteArgs()[0]].copy());
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.ILOAD_0:
	    if (!(lVars[0] instanceof SimInt)) {
		throw new Error("Internal Error. Type Mismatch");
	    }
	    push(lVars[0].copy());
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.ILOAD_1:
	    if (!(lVars[1] instanceof SimInt)) {
		throw new Error("Internal Error. Type Mismatch");
	    }
	    push(lVars[1].copy());
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.ILOAD_2:
	    if (!(lVars[2] instanceof SimInt)) {
		throw new Error("Internal Error. Type Mismatch");
	    }
	    push(lVars[2].copy());
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.ILOAD_3:
	    if (!(lVars[3] instanceof SimInt)) {
		throw new Error("Internal Error. Type Mismatch");
	    }
	    push(lVars[3].copy());
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.ISTORE:
	    if (!(peek() instanceof SimInt)) {
		//throw an error instead of an exception, as this method should
		//already have been typechecked, so if this happens, there is nothing
		//wrong with the bytecode, but with the verifier
		throw new Error("Internal Error. Type Mismatch");
	    }
	    lVars[nextBC.getByteArgs()[0]] = pop();
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.ISTORE_0:
	    if (!(peek() instanceof SimInt)) {
		//throw an error instead of an exception, as this method should
		//already have been typechecked, so if this happens, there is nothing
		//wrong with the bytecode, but with the verifier
		throw new Error("Internal Error. Type Mismatch");
	    }
	    lVars[0] = pop();
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.ISTORE_1:
	    if (!(peek() instanceof SimInt)) {
		//throw an error instead of an exception, as this method should
		//already have been typechecked, so if this happens, there is nothing
		//wrong with the bytecode, but with the verifier
		throw new Error("Internal Error. Type Mismatch");
	    }
	    lVars[1] = pop();
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.ISTORE_2:
	    if (!(peek() instanceof SimInt)) {
		//throw an error instead of an exception, as this method should
		//already have been typechecked, so if this happens, there is nothing
		//wrong with the bytecode, but with the verifier
		throw new Error("Internal Error. Type Mismatch");
	    }
	    lVars[2] = pop();
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.ISTORE_3:
	    if (!(peek() instanceof SimInt)) {
		//throw an error instead of an exception, as this method should
		//already have been typechecked, so if this happens, there is nothing
		//wrong with the bytecode, but with the verifier
		throw new Error("Internal Error. Type Mismatch");
	    }
	    lVars[3] = pop();
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.POP:
	    pop();
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.POP2:
	    pop();
	    pop();
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.DUP:
	    push(peek().copy());
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.DUP_X1:
	case ByteCode.DUP_X2:
	case ByteCode.DUP2:
	case ByteCode.DUP2_X1:
	case ByteCode.DUP2_X2:
	    throw new Error("Internal Error: '" + nextBC + "' not implemented.");
	    //break;
	case ByteCode.SWAP:
	    tmpData1 = pop();
	    tmpData2 = pop();
	    push(tmpData1);
	    push(tmpData2);
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.IADD:
	    tmpInt2 = ((SimInt)pop()).getValue();
	    tmpInt1 = ((SimInt)pop()).getValue();
	    push(new SimInt(nextBC.getAddress(), tmpInt2 + tmpInt1));
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.ISUB:
	case ByteCode.IMUL:
	case ByteCode.IDIV:
	case ByteCode.IREM:
	case ByteCode.INEG:
	case ByteCode.ISHL:
	case ByteCode.ISHR:
	case ByteCode.IUSHR:
	case ByteCode.IAND:
	case ByteCode.IOR:
	case ByteCode.IXOR:
	    throw new Error("Internal Error: '" + nextBC + "' not implemented.");
	    //break;
	case ByteCode.IINC:
	    if (!(lVars[nextBC.getByteArgs()[0]] instanceof SimInt)) {
		throw new Error("Internal Error. Type Mismatch");
	    }
	    tmpInt1 = ((SimInt)lVars[nextBC.getByteArgs()[0]]).getValue();
	    tmpInt1 += ((int)nextBC.getByteArgs()[1]);
	    lVars[nextBC.getByteArgs()[0]] = new SimInt(nextBC.getAddress(),
							     tmpInt1);
	    //proceed to next ByteCode
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.IFGE:
	    if (((SimInt)pop()).getValue() >= 0) 
		nextBC = nextBC.getTargets()[1];
	    else
		nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.IF_ICMPEQ:
	    tmpInt2 = ((SimInt)pop()).getValue();
	    tmpInt1 = ((SimInt)pop()).getValue();
	    if(tmpInt1 == tmpInt2)
		nextBC = nextBC.getTargets()[1];
	    else
		nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.IF_ICMPNE:
	    tmpInt2 = ((SimInt)pop()).getValue();
	    tmpInt1 = ((SimInt)pop()).getValue();
	    if(tmpInt1 != tmpInt2)
		nextBC = nextBC.getTargets()[1];
	    else
		nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.IF_ICMPLT:
	    tmpInt2 = ((SimInt)pop()).getValue();
	    tmpInt1 = ((SimInt)pop()).getValue();
	    if(tmpInt1 < tmpInt2)
		nextBC = nextBC.getTargets()[1];
	    else
		nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.IF_ICMPGE:
	    tmpInt2 = ((SimInt)pop()).getValue();
	    tmpInt1 = ((SimInt)pop()).getValue();
	    if(tmpInt1 >= tmpInt2)
		nextBC = nextBC.getTargets()[1];
	    else
		nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.IF_ICMPGT:
	    tmpInt2 = ((SimInt)pop()).getValue();
	    tmpInt1 = ((SimInt)pop()).getValue();
	    if(tmpInt1 > tmpInt2)
		nextBC = nextBC.getTargets()[1];
	    else
		nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.IF_ICMPLE:
	    tmpInt2 = ((SimInt)pop()).getValue();
	    tmpInt1 = ((SimInt)pop()).getValue();
	    if(tmpInt1 <= tmpInt2)
		nextBC = nextBC.getTargets()[1];
	    else
		nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.GOTO:
	    nextBC = nextBC.getTargets()[0];
	    break;
	case ByteCode.RETURN:
	case ByteCode.IRETURN:
	case ByteCode.LRETURN:
	case ByteCode.FRETURN:
	case ByteCode.DRETURN:
	case ByteCode.ARETURN:
	    //bei return: nextBC == null und beenden
	    nextBC = null;
	    return true;
	case ByteCode.GETSTATIC:
	case ByteCode.GETFIELD:
	    {Integer tmpval;
	    BCCPArgOp bCode = (BCCPArgOp) nextBC;
	    int cPoolIndex = (bCode.getByteArgs()[0]<<8)|
		(((int)bCode.getByteArgs()[1])&0xff);	
	    String typeDesc = bCode.getCP().fieldRefEntryAt(cPoolIndex).getMemberTypeDesc();
	    String fieldName = bCode.getCP().fieldRefEntryAt(cPoolIndex).getMemberName();
	    String className = bCode.getCP().fieldRefEntryAt(cPoolIndex).getClassName();
	    //FEHLER auf null ueberpruefen!
	    tmpval = getValueProvider().getIntField(fieldName, typeDesc, className, nextBC);
	    if (tmpval == null) {
		setInformationMissing();
		return false;
	    }
	    push(new SimInt(nextBC.getAddress(), tmpval.intValue()));
	    nextBC = nextBC.getTargets()[0];}
	    break;
	case ByteCode.INVOKEVIRTUAL:
	case ByteCode.INVOKESPECIAL:
	case ByteCode.INVOKESTATIC:
	case ByteCode.INVOKEINTERFACE:
	    {
		Integer tmpint;
		BCCPArgOp bCode = (BCCPArgOp) nextBC;
		int cPoolIndex = (bCode.getByteArgs()[0]<<8)|
		    (((int)bCode.getByteArgs()[1])&0xff);	
		String typeDesc = bCode.getCP().methodRefEntryAt(cPoolIndex).getMemberTypeDesc();
		String fieldName = bCode.getCP().methodRefEntryAt(cPoolIndex).getMemberName();
		String className = bCode.getCP().methodRefEntryAt(cPoolIndex).getClassName();
		//FEHLER auf null ueberpruefen
		tmpint = getValueProvider().invokeIntMethod(fieldName, typeDesc, className, nextBC);
		if (tmpint == null) {
		    setInformationMissing();
		    return false;
		}
		push(new SimInt(nextBC.getAddress(), tmpint.intValue()));
		nextBC = nextBC.getTargets()[0];
	    }
	    break;
	case ByteCode.GOTO_W:
	    nextBC = nextBC.getTargets()[0];
	    break;
	default:
	    throw new Error("Internal Error: " + nextBC + " not implemented!\n");
	}
	return true;
    }


    /**simulate effect of bc by pushing the appropriate amount of empty words onto stack and 
     * into the local variables
     * @return true if simulation should continue, else false
     */
    public boolean simulateBC() {
	//FEHLER debug
	//System.out.println("normal!");
	//add the execution time of this bytecode to the total time
	eTime.addTimeOfBC(nextBC);
	timeExceeded = eTime.limitExceeded();

	//generic simulation: pop/push the appropriate amount of data on the stack and...
	//FEHLER das ist grauenhaft so. Besser eine funktion, die die wirkliche anzahl zurückliefert und dann funktionen die sich bei den einzelnen passes drum kuemmern die anzahl anzupassen
	int pops = BCEffectPass2.getStackOperands(nextBC);
	int pushs = BCEffectPass2.getStackResults(nextBC);
	pops = (pops > BCEffectPass0.POP[nextBC.getOpCode()])? pops : 
	    BCEffectPass0.POP[nextBC.getOpCode()];
	pushs = (pushs > BCEffectPass0.PUSH[nextBC.getOpCode()])? pushs : 
	    BCEffectPass0.PUSH[nextBC.getOpCode()];
	for (int i = 0; i< pops; i++) {
	    pop();
	}
	for (int i=0; i < pushs; i++) {
	    push(new SimData(nextBC.getAddress()));
	}
	//...invalidate all local Variables that are written to.
	int[] lVarIndices = BCEffectPass2.getLVarsResults(nextBC);
	for (int i = 0; i < lVarIndices.length; i++) {
	    lVars[lVarIndices[i]] = new SimData(nextBC.getAddress());
	}

	//handle method invokation
	if (nextBC.getOpCode() == ByteCode.INVOKEINTERFACE ||
	    nextBC.getOpCode() == ByteCode.INVOKESTATIC ||
	    nextBC.getOpCode() == ByteCode.INVOKEVIRTUAL ||
	    nextBC.getOpCode() == ByteCode.INVOKESPECIAL) {
	    
	    BCCPArgOp bCode = (BCCPArgOp) nextBC;
	    //get typedesc for method, methodName and targetClass
	    int cPoolIndex = (bCode.getByteArgs()[0]<<8)|
	    (((int)bCode.getByteArgs()[1])&0xff);

	    if (nextBC.getOpCode() == ByteCode.INVOKEINTERFACE) {
		String className = bCode.getCP().InterfaceMethodRefEntryAt(cPoolIndex).getClassName();
		String typeDesc = bCode.getCP().InterfaceMethodRefEntryAt(cPoolIndex).getMemberTypeDesc();
		String methodName = bCode.getCP().InterfaceMethodRefEntryAt(cPoolIndex).getMemberName();
		ValueProvider vProvider = getValueProvider();
		ExecutionTime methodETime = null;
		if (vProvider != null) {
		    methodETime = vProvider.getMethodWCET(className, 
							  methodName, 
							  typeDesc,
							  nextBC);
		} 
		if (vProvider == null || methodETime == null) {
		    setInformationMissing();
		    return false;
		}
		eTime.add(methodETime);
		timeExceeded = eTime.limitExceeded();
		
		//FEHLER nur auskommentiert zu test-zwecken!
		//throw new WCETNAException("Interface methods cannot yet be analyzed");
	    } else{ 
		//normal method.
		String className = bCode.getCP().methodRefEntryAt(cPoolIndex).getClassName();
		String typeDesc = bCode.getCP().methodRefEntryAt(cPoolIndex).getMemberTypeDesc();
		String methodName = bCode.getCP().methodRefEntryAt(cPoolIndex).getMemberName();
		if (className.equals("java/lang/Object")) {
		    ValueProvider vProvider = getValueProvider();
		    ExecutionTime methodETime = null;
		    if (vProvider != null) {
			methodETime = vProvider.getMethodWCET(className, 
							      methodName, 
							      typeDesc,
 							      nextBC);
		    } 
		    if (vProvider == null || methodETime == null) {
			setInformationMissing();
			return false;
		    }
		    eTime.add(methodETime);
		    timeExceeded = eTime.limitExceeded();
		} else {
		    ClassData otherClass = classFinder.findClass(className);
		    if (otherClass == null) 
			throw new Error("Internal Error: Class not found: " +
					className );
		    MethodData otherMethod = otherClass.getMethodDataNE(methodName, typeDesc);
		    if (otherMethod == null) 
			throw new Error("Internal Error: Method not found: " +
					className + "." + methodName + "(" + typeDesc + ")");
		    
		    eTime = simulateMethodInvokation(otherMethod, 
						     className, 
						     otherClass.getConstantPool(),
						     eTime);
		} //if java/lang/object
	    } //if interface
	}

	//proceed to next ByteCode
	if (nextBC.getTargets().length > 0) {
	    nextBC = nextBC.getTargets()[0];
	} else {
	    nextBC = null;
	    //FEHLER debug
	    //System.out.println("t.l=0: " + nextBC);
	}
	return true;
    }
    
    /**Simulate effect of method invokation.
     * The time needed to execute the method is added to parameter 'eTime' and eTime is returned.
     * @param eTime the actual executiontime.
     * @return executionTime after completing the method. Is the same object as parameter eTime.
     */
    public ExecutionTime simulateMethodInvokation(MethodSource method, 
						  String className, 
						  ConstantPool cPool,
						  ExecutionTime eTime) 
	 {

	//check if the method should be analyzed or if the wcet is provided by a valueProvider
	if (getValueProvider().providesMethodWCET(className, 
						  method.getMethodName(), 
						  method.getMethodType(), 
						  nextBC)) {
	    ExecutionTime tmpET = getValueProvider().getMethodWCET(className, 
								   method.getMethodName(), 
								   method.getMethodType(), 
								   nextBC);
	    if (tmpET != null) {
		eTime.add(tmpET);
		timeExceeded = eTime.limitExceeded();
		return eTime;
	    } else {
		Verifier.errPrint(Verifier.DEBUG_NORMAL, "Warning: No wcet for method " +
				  className + "." + method.getMethodName() +
				  "(" + method.getMethodType() +") provided!");
	    }
	}
	//if method has already been analyzed and the wcet is known, just use it.
	WCETResult wRes = (WCETResult) method.getVerifyResult(VerifyResult.WCET_RESULT);
	//if the wcet is unknown, analyze method
	if (wRes == null ||   //method not analyzed
	    (wRes.getETime() != null && //method analyzed, but...
	     wRes.getETime().limitExceeded() &&  //limit exceeded and limit was less than actual limit
	     eTime.timeLeft() <= wRes.getETime().getTimeLimit())) {
	    try {
		Verifier.stdPrintln(1, "*********************************************************"+
				    "\nSimulating method " +className + "." + 
				    method.getMethodName() + "(" + method.getMethodType() + ")\n" + 
				    "*********************************************************");
		WCETAnalysis.verifyMethod(method, className, cPool, 
					  classFinder, eTime.timeLeft());
		Verifier.stdPrintln(1, "*********************************************************"+
				    "\nDone simulating method " +className + "." + 
				    method.getMethodName() + "(" + method.getMethodType() + ")\n" + 
				    "*********************************************************");
	    } catch (VerifyException e) {
		//FEHLER was tun mit der VE.?
	    }
	}

	//at this point, method has been analyzed with a limit >= actual limit
	wRes = (WCETResult) method.getVerifyResult(VerifyResult.WCET_RESULT);
	if (wRes == null)
	    throw new Error("Internal Error: result of wcet is null!");

	if (wRes.getETime() != null &&
	    !wRes.getETime().limitExceeded()) {
	    //wRes is a valid WCET. So just add to the time already consumed
	    //if the result is larger than the actual limit, an exception is thrown by add!
	    eTime.add(wRes.getETime());
	    timeExceeded = eTime.limitExceeded();
	    return eTime;
	} else  if (wRes.getETime() != null) {
	    //if the eTime of the method exceeded its limit, it exceeds the actual limit as well
	    //because wRes.eTime.limit >= eTime.limit ! (see if above)
	    eTime.useAllTime();
	    timeExceeded = eTime.limitExceeded();
	    return eTime;

	} else {
	    //wRes.eTime == null, so invoked method is not analyzable --> this method isnt either.
	    //however, before raising an exception, the valueProvider should be asked.
	    ExecutionTime tmpET = getValueProvider().getMethodWCET(className, 
								   method.getMethodName(), 
								   method.getMethodType(), 
								   nextBC);
	    if (tmpET != null) {
		eTime.add(tmpET);
		timeExceeded = eTime.limitExceeded();
		
		return eTime;
	    }

	    setInformationMissing();
	    return null;
	}


    }
}
