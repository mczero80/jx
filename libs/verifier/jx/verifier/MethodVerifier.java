package jx.verifier;

import java.util.Vector;
import jx.classfile.*;
import jx.classfile.constantpool.*;
import jx.classstore.ClassFinder;
import jx.verifier.bytecode.*;

/**Class for verification of one method.
 * The class has a checkqueue which contains all bytecodes that should be checked.
 * Method verification is initiated by a call to runChecks().*/
public class MethodVerifier implements VerifierInterface {
    private Vector checkQueue;
    private BCLinkList code;
    private MethodSource method;
    private String className;
    private Subroutines srs;
    private JVMState initialState;
    Object vParameter = null;
    
    /**returns the name of the class the verified method belongs to.*/
    public String getClassName() { return className;}
    /**returns the MethodSource of the method that is verified.*/
    public MethodSource getMethod() { return method;}
    /**returns the code of the method*/
    public BCLinkList getCode() {return code;}
    /**returns the Typedescriptor of the method being verified.*/
    public String getTypeDesc() { return method.getMethodType();}
    /**returns true if the method is static*/
    public boolean isStaticMethod() { return method.isStatic();}
    /**returns the subroutines of the method.
     *@see Subroutines
     */
    public Subroutines getSrs() { return srs;}
    /**set the initial state, with which verification is started at the first bytecode.
     * Must be set to a nonnull value before calling runChecks().*/
    public void setInitialState(JVMState iState) {initialState = iState;}
    /**returns the VerifierParameter. 
     * @return parameter or null.
     */
    public Object getParameter(){return vParameter;}

    /**Constructor.
     * creates new constructor and reads the bytecode into a BCLinkList. While creating the BCLinkList, some inconsitencies may be detected (e.g. branch to an invalid address) so an Exception may be thrown.
     * @param method the method that should be verified.
     * @param className the name of the class or interface to which the method belongs.
     * @cPool the constantPool of the class.
     * @exception VerifyException if inconsitencies are detected when creating BCLinkList.
     * @see BCLinkList
     * @see jx.classfile.MethodSource
     * @see jx.classfile.constantpool.ConstantPool
     */
    public MethodVerifier(MethodSource method, String className, ConstantPool cPool) 
	throws VerifyException {

	Verifier.stdPrintln(1,"----------------------------------------------------");
	Verifier.stdPrintln(1,"Method " + className + "." + method.getMethodName());

	this.code = new BCLinkList(method, cPool);
	this.method = method;
	this.className = className;
    }

    /**Constructor.
     * the same as above, but receives one more argument: a VerifierParamter.
     */
    public MethodVerifier(MethodSource method, String className, ConstantPool cPool, 
			  Object vParameter) throws VerifyException {
	this(method, className, cPool);
	this.vParameter = vParameter;
    }


    /*
    public void run() {
	//FEHLER mal sehen, was man da machen kann.
	try{runChecks();}catch (VerifyException e) {}
    }
*/
    /**Start verifying.
     * First all subroutines are searched and registered  and the checkQueue is initialized. Then the first bytecode receives "initialState" as "beforeState" and is added to the checkQueue. Finally continueChecks() is called.
     * @exception VerifyException if the method fails verification.
     * @exception java.lang.Error if initialState is null.
     * @see MethodVerifier#continueChecks
     */
    public void runChecks() throws VerifyException {
	if (initialState == null) {
	    throw new Error("Internal Error: MethodVerifier.initialState has not yet been initialized!");
	}

	checkQueue = new Vector();
	ByteCode actBC = code.getFirst();
	actBC.beforeState = initialState.copy();
	//make sure the state starts at actBC
	actBC.beforeState.setNextBC(actBC);

	//Initialize subroutines
	srs = new Subroutines();
	Verifier.stdPrint(1,"Searching subroutines....");
	srs.registerSrs(code);
	Verifier.stdPrintln(1,"done");
	Verifier.stdPrint(1,srs.toString());
	
	checkBC(actBC);
	continueChecks();
	Verifier.stdPrintln(1,"checkQueue empty --> done");
    }

    /**checks all bytecodes in the check-queue.
       Goes through all bytecodes in the checkQueue and calls beforeState.executeNextBC(). Note: The CheckQueue is LIFO so the verification always finishes one branch before starting verification of another one. Else Verification of one subroutine could be startet twice.
       * @exception VerifyException if verification fails for some bytecode/state.
       */
    public void continueChecks() throws VerifyException{
	ByteCode actBC;
	while (!checkQueue.isEmpty()) {
	    //Check Queue must be LIFO!!!
	    //FEHLER stimmt das: 
	    // Else Verification of one subroutine could be startet twice!!
	    actBC = (ByteCode) checkQueue.lastElement();
	    checkQueue.removeElementAt(checkQueue.size()-1);
	    actBC.mvCheckCount = 0;
	    try {//FEHLER debug
	    try {
		actBC.beforeState.executeNextBC();
	    } catch(VerifyException e) {
		e.methodName = method.getMethodName();
		e.className = className;
		e.code = code;
		throw e;
	    }
	    } catch(Error e) {
		Verifier.errPrintln(0,code.toString());
		throw e;
	    }
	}
    }
    
    /**Add Bytecode to the checkQueue.
     * Every bytecode has a counter so it can only be added to the checkQueue once. checkBC calls with bytecodes that are already in the queue do nothing.
     */
    public void checkBC(ByteCode e) {
	if (e.mvCheckCount > 0)
	    return; //e is already in checkQueue
	e.mvCheckCount++;
	checkQueue.addElement(e);
    }
   
    /**Stop verification and empty runQueue.
     * Check is stopped, and runChecks / continueCheck will return.
     * CheckQueue is emptied (and the checkQueue counters of all bytecodes are reset to 0).
     */
    public void endChecks() {
	//empty the checkQueue
	while (!checkQueue.isEmpty()) {
	    ((ByteCode) checkQueue.lastElement()).mvCheckCount = 0;
	    checkQueue.removeElementAt(checkQueue.size()-1);
	}

    }
	
}
