//Worst Case Execution Time Analysis
package jx.verifier.wcet;

import jx.classfile.*;
import jx.classfile.constantpool.*;
import jx.classstore.ClassFinder;
import jx.verifier.bytecode.*;
import jx.verifier.*;
//FEHLER debug
import jx.verifier.typecheck.TypeCheck;

import java.util.Vector;
import java.util.Enumeration;

public class WCETAnalysis {

    /**Name of the exception that should be thrown at runtime, if the timelimit is exceeded.*/
    static public final String runtimeExceptionType = "jx/verifier/wcet/TimeLimitExceededException";

    static public void verifyMethod(MethodSource method, 
				    String className, 
				    ConstantPool cPool,
				    ClassFinder classFinder,
				    int timeLimitMS) 
	throws VerifyException {
	
	//ValueProvider vP = new InteractiveValueProvider();
	ValueProvider vP = new StaticValueProvider();
	vP.setContext(className, method.getMethodName(), method.getMethodType());
	MethodVerifier m = new MethodVerifier(method,
					      className,
					      cPool,
					      (Object)vP);
	m.setInitialState(new WCETState(m.getCode().getFirst(), 
					m.getMethod().getNumLocalVariables(),
					m.getMethod().getNumStackSlots(),
					m));
	
	//Pass 0 build operand stacks
	m.runChecks();
	//build opStacks for exceptionhandlers...
	boolean checkAgain = false;
	for (Enumeration e = m.getCode().exceptionHandlers.elements(); 
	     e.hasMoreElements();
	     ) {
	    ExceptionHandler eh = (ExceptionHandler) e.nextElement();
	    if (eh.getHandler().beforeState == null) {
		checkAgain = true;
		eh.getHandler().beforeState = new WCETState(eh.getHandler(), 
					m.getMethod().getNumLocalVariables(),
					m.getMethod().getNumStackSlots(),
					m);
		((WCETState)eh.getHandler().beforeState).WCETgetStack().push(); //exception object
	    }
	}
	if (checkAgain) {
	    m.continueChecks();
	}
	Verifier.stdPrintln(1,"Pass 0 Complete");

	//FEHLER debug
	//for (ByteCode actBC = m.getCode().getFirst(); actBC != null; actBC = actBC.next) {
	//    System.out.println(actBC.beforeState.getStack() + " - " + actBC);
	//}

	//Pass 1
	//search loops
	((WCETState)m.getCode().getFirst().beforeState).setPass(1);

	//System.out.println(m.getCode());
	//FEHLER debug
	System.out.print("Building Control Flow Graph...");
	CFGraph cfg = new CFGraph(m.getCode());
	System.out.println("done");
	//System.out.println("Graph:\n" + cfg.getNodes().toString());

	if (!cfg.simplify()) {
	    //Graph could not be reduced to exactly one node. So simulation is not possible,
	    //instead a runtimechecked version of the method is generated.
	    MethodData nM = createETCheckedMethod((MethodData)method, 
						  cPool, 
						  m.getCode());
	    //save result
	    method.setVerifyResult(new WCETResult(nM));
	    //FEHLER debug
	    System.out.println("WCETA of " + className + "." +
			       method.getMethodName() + "(" +
			       method.getMethodType() + ") failed!");
	    return;
	}
	
	for (ByteCode act = m.getCode().getFirst();
	     act != null;
	     act = act.next) {
	    
	    //FEHLER ist nur wegen der noch nicht funktionierenden subroutinen da
	    //wenn die (und die exceptions)funktionieren duerften keine null-states mehr existieren! 
	    if (act.beforeState == null)
		continue;
	    ((WCETState)act.beforeState).setPass(1);
	    //((WCETState)act.beforeState).getOIfList().cleanupList();

	    if (act.getTargets().length == 2 && //i.e. its an if
		((WCETState)act.beforeState).necessary) {
		//FEHLER debug ausgabe
		//System.out.print("*****" + act);
		//System.out.println(" ("+((WCETState)act.beforeState).getOIfList());
		m.checkBC(act);
		((WCETState)act.beforeState).setPass(2);

	    } else {
		//FEHLER debug ausgabe
		//if (((WCETState)act.beforeState).simulateException != null) 
		//    System.out.print("eeeee" + act);
		//else
		//    System.out.print("     " + act);

		//System.out.println(" ("+((WCETState)act.beforeState).getOIfList());
	    }
	}

	//Pass 2
	try {
	    m.continueChecks();
	} catch(VerifyException e) {
	    //Pass2 could not be completed successfully, because there is insufficient information for simulation.
	    //-->runtime checks.
	    MethodData nM = createETCheckedMethod((MethodData)method, 
						  cPool, 
						  m.getCode());
	    //save result
	    method.setVerifyResult(new WCETResult(nM));
	    //FEHLER debug
	    System.out.println("WCETA of " + className + "." +
			       method.getMethodName() + "(" +
			       method.getMethodType() + ") failed!");
	    return;
	}


	Verifier.stdPrintln(1,"Pass 2 Complete");

	//FEHLER debug ausgabe
	for (ByteCode act = m.getCode().getFirst();
	     act != null;
	     act = act.next) {
	    if (act.beforeState == null) {
		System.out.println("(nil)" + act);
	    } else if (((WCETState)act.beforeState).necessary) {
		System.out.println("*****" + act);
	    } else
		System.out.println("     " + act);
	}
	

	//compute WCET by simulating the method
	SimState simState = new SimState(m.getMethod().getNumLocalVariables(),
					 m.getMethod().getNumStackSlots(), classFinder);
	simState.nextBC = m.getCode().getFirst();
	simState.eTime.setTimeLimit(timeLimitMS);
	ExecutionTime eTime = null;
	//find all necessary local variables
	JVMLocalVars tmpLV = m.getCode().getFirst().beforeState.getlVars();
		for (int i = 0; i < tmpLV.getNumVars(); i++) {
		    WCETLocalVarsElement e = (WCETLocalVarsElement) tmpLV.read(i);
		    if (e.necessary) {
			Integer tmp = vP.getMethodArgument(i, 
							   className, 
							   method.getMethodName(), 
							   method.getMethodType());
			if (tmp == null){
			    MethodData nM = createETCheckedMethod((MethodData)method, 
								  cPool, 
								  m.getCode());
			    //save result
			    method.setVerifyResult(new WCETResult(nM));
			    return;
			}
			simState.lVars[i] = new SimInt(-1, tmp.intValue());
			
		    }
		    
		}
		simState = cfg.simulate(simState);
		eTime = simState.eTime;
		if (simState.informationMissing()) {
		    MethodData nM = createETCheckedMethod((MethodData)method, 
							  cPool, 
							  m.getCode());
		    //save result
		    method.setVerifyResult(new WCETResult(nM));
		    //FEHLER debug
		    System.out.println("WCETA of " + className + "." +
				       method.getMethodName() + "(" +
				       method.getMethodType() + ") failed!");
		    return;
		}
	
	Verifier.stdPrintln(1,"Pass 3 Complete");

	Verifier.stdPrint(1, "Time: " + eTime);
	if (eTime.limitExceeded()) {
	    Verifier.stdPrintln(1, "Exceeds limit of " + eTime.getTimeLimit() +"ms!");
	} else {
	    Verifier.stdPrintln(1, "Within limit of " + eTime.getTimeLimit() +"ms!");
	}

	//save result
	method.setVerifyResult(new WCETResult(eTime));
	return;
    }

    /**Mark all bytecodes before which a runtime check should be added.
     */
    static public void scheduleRTChecks(BCLinkList code) {
	int timeCounter = 0;
	for (ByteCode actBC = code.getFirst(); 
	     actBC != null; 
	     actBC = actBC.next) {
	    //find basic blocks, at the end of a basic block the appropriate amount of time is added.
	    //FEHLER es sollte auch wirklich ZEIT draufgezählt werden,  nicht nur die Anzahl der Befehle
	    if (actBC.getTargets().length == 1 &&
		actBC.getSources().length <= 1   ) {
		timeCounter++;
	    } else {
		timeCounter++;
		((WCETState)actBC.beforeState).rTTimeCountIncrement = timeCounter;
		timeCounter = 0;
	    }
	    
	    //if a backward branch is encountered, a check must be added.
	    for (int i = 0; i < actBC.getTargets().length; i++) {
		if (actBC.getTargets()[i].getAddress() <= actBC.getAddress()) {
		    ((WCETState)actBC.beforeState).rTTimeCheck = true;
		}
	    }
	}
    }
    

    /**Create new method containig runtimechecks for execution time bounds.
     * Note that this method changes the "code" BCLinkList without adjusting the
     * target and source entries of each ByteCode.
     */
    public static MethodData createETCheckedMethod(MethodData method, 
						   ConstantPool cPool, 
						   BCLinkList code) {
	
	scheduleRTChecks(code);
	System.err.println(method.getDeclaringClass().getClassName() +"."+ method.getMethodName() + "(" + method.getMethodType()+")");
	MethodData newMethod = method.copy();

	//FEHLER debug
	System.out.println("createEtCheckedMethod called");
	//FEHLER debug
	/*
	//change type descriptor
	String typeDesc = newMethod.getMethodType();
	int tmp = typeDesc.lastIndexOf(')');
	String newTypeDesc = typeDesc.substring(0, tmp) + 
	    "Ljx/verifier/wcet/RTTimeBound;" +
	    typeDesc.substring(tmp);

	//Create constanpool entries
	UTF8CPEntry td = new UTF8CPEntry(newTypeDesc);
	cPool.addEntry(td);
	NameAndTypeCPEntry nt = new NameAndTypeCPEntry(newMethod.getMethodNameCPEntry(), td);
	cPool.addEntry(nt);
	MethodRefCPEntry mr = new MethodRefCPEntry(cPool.classEntryAt(newMethod.getDeclaringClass().getThisClassCPIndex()), nt);
	cPool.addEntry(mr);
	newMethod.setMethodTypeCPEntry(td);

	UTF8CPEntry rttimeBoundName = new UTF8CPEntry("jx/verifier/wcet/RTTimeBound");
	cPool.addEntry(rttimeBoundName);
	UTF8CPEntry rttimeBoundValueName = new UTF8CPEntry("value");
	cPool.addEntry(rttimeBoundValueName);
	UTF8CPEntry rttimeBoundValueType = new UTF8CPEntry("I");
	cPool.addEntry(rttimeBoundValueType);
	UTF8CPEntry rttimeBoundLimitName = new UTF8CPEntry("limit");
	cPool.addEntry(rttimeBoundLimitName);
	UTF8CPEntry rttimeBoundLimitType = new UTF8CPEntry("I");
	cPool.addEntry(rttimeBoundLimitType);
	NameAndTypeCPEntry rtTimeBoundValueNT = new NameAndTypeCPEntry(rttimeBoundValueName, rttimeBoundValueType);
	cPool.addEntry(rtTimeBoundValueNT);
	NameAndTypeCPEntry rtTimeBoundLimitNT = new NameAndTypeCPEntry(rttimeBoundLimitName, rttimeBoundLimitType);
	cPool.addEntry(rtTimeBoundLimitNT);
	ClassCPEntry rtTimeBoundClass = new ClassCPEntry(rttimeBoundName);
	cPool.addEntry(rtTimeBoundClass);
	FieldRefCPEntry rtTimeBoundValueFR = new FieldRefCPEntry(rtTimeBoundClass,
								 rtTimeBoundValueNT);
	int cPoolIndexRTValue = cPool.addEntry(rtTimeBoundValueFR);
	FieldRefCPEntry rtTimeBoundLimitFR = new FieldRefCPEntry(rtTimeBoundClass,
								 rtTimeBoundLimitNT);
	int cPoolIndexRTLimit = cPool.addEntry(rtTimeBoundLimitFR);
	UTF8CPEntry rtException = new UTF8CPEntry(runtimeExceptionType);
	cPool.addEntry(rtException);
	ClassCPEntry rtExceptionClass = new ClassCPEntry(rtException);
	int cPoolIndexEx = cPool.addEntry(rtExceptionClass);

	//adjust CodeData
	CodeData cData = newMethod.getCode();
	cData.increaseMaxLocals(); //two more local variable is needed, to hold the RTTimeBound and its value
	cData.increaseMaxLocals(); 
	cData.increaseMaxStack(); //two more stack slot, because the counter has to be loaded onto the stack for comparison. 
	cData.increaseMaxStack(); 

	ByteCode actBC = code.getFirst();
	addPreamble(newMethod.getParameterTypes().length, 
		    newMethod.getNumLocalVariables(), cPoolIndexRTValue, code, cPool);
	
	for (; actBC != null; actBC = actBC.next) {
	    //if necessary, add checks or counts
	    if (((WCETState)actBC.beforeState).rTTimeCountIncrement > 0) { //increase counter
		addTimeCount(actBC, 
			     ((WCETState)actBC.beforeState).rTTimeCountIncrement,
			     newMethod.getNumLocalVariables());
	    }
	    if (((WCETState)actBC.beforeState).rTTimeCheck) { //check
		saveCounter(actBC, cPoolIndexRTValue, cPool, newMethod.getNumLocalVariables());
		addTimeCheck(actBC, cPoolIndexRTLimit, 
			     cPoolIndexEx, cPool,  newMethod.getNumLocalVariables());
	    }
	    //if actBC ends execution of method, save counter to RTTimeBound.
	    if (actBC.getOpCode() == ByteCode.ATHROW ||
		actBC.getOpCode() == ByteCode.IRETURN ||
		actBC.getOpCode() == ByteCode.LRETURN ||
		actBC.getOpCode() == ByteCode.FRETURN ||
		actBC.getOpCode() == ByteCode.DRETURN ||
		actBC.getOpCode() == ByteCode.ARETURN ||
		actBC.getOpCode() == ByteCode.RETURN ) {
		saveCounter(actBC, cPoolIndexRTValue, cPool, newMethod.getNumLocalVariables());
	    }

	}

	code.recomputeAddresses();
	byte[] newByteArray = code.toByteArray();

	cData.setCodeBytes(newByteArray);
	
	//adjust Exception handlers
	//FEHLER eigentlich müssten auch noch cpool entries für die eHandler eingefügt werden(?)
	ExceptionHandlerData[] ehs = cData.getExceptionHandlers();
	if (code.exceptionHandlers.size() != ehs.length)
	    throw new Error("Internal Error");
	int i=0;
	for (Enumeration e = code.exceptionHandlers.elements(); e.hasMoreElements();i++) {
	    //ExceptionHandler
	    ExceptionHandler eh = (ExceptionHandler)e.nextElement();
	    ehs[i] = new ExceptionHandlerData(eh.getStartAddress(), 
					      eh.getEndAddress(), 
					      eh.getHandlerAddress(), 
					      eh.getETypeCPIndex());
	}

	
	//FEHLER debug
	//System.out.println("code:\n" + code);

	//FEHLER debug
	//Test...
	try {
	    TypeCheck.verifyMethod(newMethod, 
				   cPool.getClassName(),
				   cPool);
	} catch(VerifyException e) {
	    e.printStackTrace();
	    //System.exit(1);
	    System.out.print("Press any key .....");
	    try {System.in.read();} catch(Throwable t) {}
	}
	*/
	return newMethod;
    }

    /**Add Preamble to method. Just put the las parameter of the method (the RTTimeBound structure) into the penultimate variable and load the value field into the last one. (*Heist das eigentlich Preamble wie im deutschen oder preamble?)
     * @return the new start of the method (i.e. the start of the preamble*/
    public static void addPreamble(int numPars, int numLVars, int cPoolIndexRT, BCLinkList code, ConstantPool cPool) {
	byte[] codeBytes = new byte[10];
	
	codeBytes[0] = (byte) ByteCode.ALOAD;
	//FEHLER eigentlich müsste hier noch unterschieden werden, ob static oder nicht static methode. Wenn nämlich nicht static, dann fangen par. bei 0 an, sonst erst bei 1!
	codeBytes[1] = (byte) numPars;
	codeBytes[2] = (byte) ByteCode.DUP;
	codeBytes[3] = (byte) ByteCode.ASTORE;
	codeBytes[4] = (byte) (numLVars-2);
	codeBytes[5] = (byte) ByteCode.GETFIELD;
	codeBytes[6] = (byte) ((cPoolIndexRT &0xff00) >> 8);
	codeBytes[7] = (byte) (cPoolIndexRT & 0xff);
	codeBytes[8] = (byte) ByteCode.ISTORE;
	codeBytes[9] = (byte) (numLVars-1);
	BCLinkList newCode;
	try {
	   newCode  = new BCLinkList(codeBytes, cPool);
	} catch (VerifyException e) {
	    System.err.println("Internal Error: Exception caught:");
	    e.printStackTrace();
	    throw new Error("Internal Error");
	}

	//link new code with rest of code
	ByteCode first = newCode.getFirst();
	ByteCode last = first;
	while(last.next != null) {
	    last = last.next;
	}
	first.prev = code.getFirst().prev;
	last.next = code.getFirst();
	code.getFirst().prev = last;
	if (first.prev != null)
	    first.prev.next = first;
	code.setFirst(first);
	
    }

    /**Save the Counter (value in last local variable) to  RTTimeBound.value.*/
    public static void saveCounter(ByteCode before, int cPoolIndexRT, ConstantPool cPool, 
				   int numLVars) {
	byte[] codeBytes = new byte[7];
	codeBytes[0] = (byte) ByteCode.ALOAD;
	codeBytes[1] = (byte) (numLVars-2);
	codeBytes[2] = (byte) ByteCode.ILOAD;
	codeBytes[3] = (byte) (numLVars-1);
	codeBytes[4] = (byte) ByteCode.PUTFIELD;
	codeBytes[5] = (byte) ((cPoolIndexRT &0xff00) >> 8);
	codeBytes[6] = (byte) (cPoolIndexRT & 0xff);
	BCLinkList newCode;
	try {
	   newCode  = new BCLinkList(codeBytes, cPool);
	} catch (VerifyException e) {
	    throw new Error("Internal Error");
	}

	//link new code with rest of code
	ByteCode first = newCode.getFirst();
	ByteCode last = first;
	while(last.next != null) {
	    last = last.next;
	}
	first.prev = before.prev;
	last.next = before;
	before.prev = last;
	if (first.prev != null)
	    first.prev.next = first;
	
    }

    /**add runtime executiontime check just befor bytecode "before".
     * note that the addresses of the newly inserted bytecodes are not correct!
     * @param cPoolIndexRT the index to the constantpool entry for the RTTimeBound.limit field.
     * @param cPoolIndexEx the index to the constantpool entry for the exception to be thrown when the limit is exceeded.
     */
    public static void addTimeCheck(ByteCode before, int cPoolIndexRT, 
			     int cPoolIndexEx, ConstantPool cPool, int numLVars) {
	if (numLVars > 256) {
	    throw new Error("Internal Error: Methods with more than 254 local variables not implemented");
	}

	//aaload RTTimeBound-object
	//getfield cPoolIndexRT
	//iload counter
	//if_icmple continue
	//new cPoolIndexEx
	//athrow
	//continue: nop
	byte[] codeBytes = new byte[15];
	codeBytes[0] = (byte) ByteCode.ALOAD; //load object
	codeBytes[1] = (byte) (numLVars-2);
	codeBytes[2] = (byte) ByteCode.GETFIELD; //load limit
	codeBytes[3] = (byte) ((cPoolIndexRT &0xff00) >> 8);
	codeBytes[4] = (byte) (cPoolIndexRT & 0xff);
	codeBytes[5] = (byte) ByteCode.ILOAD; //load counter
	codeBytes[6] = (byte) (numLVars-1);
	codeBytes[7] = (byte) ByteCode.IF_ICMPLE; //compare
	codeBytes[8] = (byte) 0x0;
	codeBytes[9] = (byte) (codeBytes.length-8); //offset to the last bytecode in this array
	codeBytes[10] = (byte) ByteCode.NEW; //create exception-instance
	codeBytes[11] = (byte) ((cPoolIndexEx &0xff00) >> 8);
	codeBytes[12] = (byte) (cPoolIndexEx & 0xff);
	codeBytes[13] = (byte) ByteCode.ATHROW; //throw it
	codeBytes[14] = (byte) ByteCode.NOP; //continue

	BCLinkList newCode;
	try {
	   newCode  = new BCLinkList(codeBytes, cPool);
	} catch (VerifyException e) {
	    System.out.println("Internal Error - Exception caught: ");
	    e.printStackTrace();
	    throw new Error("Internal Error");
	}

	//link new code with rest of code
	ByteCode first = newCode.getFirst();
	ByteCode last = first;
	while(last.next != null) {
	    last = last.next;
	}
	first.prev = before.prev;
	last.next = before;
	before.prev = last;
	if (first.prev != null)
	    first.prev.next = first;
	
    }
    
    /**add code to increase the timecounter by "time" ticks, just before bytecode "before".
     * assumes, that the last local variable holds the counter (int).
     * note that the addresses of the newly inserted bytecodes are not correct!
     * @param before the bytecode before which the counter should be incremented. The bytecode and its beforeState must be nonnull (Because the number of local variables is needed)!
     */
    public static void addTimeCount(ByteCode before, int time, int numLVars) {
	byte[] addBytecode = new byte[3]; //getfield
	addBytecode[0] = (byte) ByteCode.IINC;
	//FEHLER sollte noch unterstützt werden (wider einbauen)
	if (numLVars > 256) {
	    throw new Error("Internal Error: Methods with more than 254 local variables not implemented");
	}

	addBytecode[1] = (byte) (numLVars-1);
	//take care of values larger than one byte
	while(time > 0xff) {
	    time -= 0xff;
	    addTimeCount(before, 0xff,  numLVars);
	}
	addBytecode[2] = (byte)time;
	//Create ByteCode
	ByteCode iinc = new ByteCode(addBytecode);
	iinc.prev = before.prev;
	iinc.next = before;
	before.prev = iinc;
	if (iinc.prev != null)
	    iinc.prev.next = iinc;
	
    }

    
    public int tm2(int i) {
	for (int k = i; k < 25; k++)
	    i+=k;
	return i;
    }
    public int test = 0;
    public int testMethod(int i) {
	for (int j = 0; j < 200; j++) {
	    i += j;
	}
	return i;
    }
}
