package jx.verifier.bytecode;

import jx.classfile.*;
import jx.classfile.constantpool.*;

import jx.verifier.*;
import java.util.Vector;
import java.util.Enumeration;

public class BCLinkList {
    private ByteCode first;
    public Vector exceptionHandlers; //holds all exception handlers of the method

    public ByteCode getFirst() {return first; }
    public void setFirst(ByteCode newFirst) {first = newFirst; }

    public ByteCode getBCAt(int address) {
	ByteCode act = first;
	while (act != null && act.getAddress() != address) 
	    act = act.next;
	return act;  //act is null if address has not been found!
    }
    
    public BCLinkList (MethodSource method, ConstantPool cPool) throws VerifyException {
	this(method.getBytecode(), cPool);

	//register Exception handlers
	exceptionHandlers = new Vector(0);
	ExceptionHandlerData[] exceptions = method.getExceptionHandlers();
	for (int i = 0; i< exceptions.length; i++) {

	    String eName = null;
	    if (exceptions[i].getCatchTypeCPIndex() == 0) {
		eName = "java/lang/Throwable";
	    } else {
		eName= cPool.classEntryAt(exceptions[i].getCatchTypeCPIndex()).
		getClassName();
	    }
	    ExceptionHandler actHandler = 
		new ExceptionHandler(eName,
				     getBCAt(exceptions[i].getStartBCIndex()),
				     getBCAt(exceptions[i].getEndBCIndex()),
				     getBCAt(exceptions[i].getHandlerBCIndex()),
				     exceptions[i].getCatchTypeCPIndex());
	    this.exceptionHandlers.addElement(actHandler);

	    
	    //print exceptionhandlers
	    Verifier.stdPrintln(1,actHandler.toString());
	    //register exceptionhandler with the starting bytecode of the handler
	    actHandler.getHandler().startsEH = actHandler;
	    ByteCode actBC = getBCAt(actHandler.getStartAddress());
	    //register exceptionhandler with all bytecodes within start and endAddress
	    while(actBC.getAddress() <= actHandler.getEndAddress()) {
		if (actBC.eHandlers == null) {
		    actBC.eHandlers = new ExceptionHandler[1];
		    actBC.eHandlers[0] = actHandler;
		} else {
		    // there is already one or more Exception
		    ExceptionHandler[] tmpEH = 
			new ExceptionHandler[actBC.eHandlers.length+1];
		    for (int j = 0; j < actBC.eHandlers.length; j++) {
			tmpEH[j] = actBC.eHandlers[j];
		    }
		    tmpEH[actBC.eHandlers.length] = actHandler;
		    actBC.eHandlers = tmpEH;
		}
		
		actBC = actBC.next;
	    }
	}
    }
	
    public BCLinkList (byte[] byteCode, ConstantPool cPool) throws VerifyException {
	ByteIterator code = new ByteIterator(byteCode);

	first = ByteCode.newByteCode(code, cPool, null);
	ByteCode tmp = first;
	while (code.hasMore()) {
	    tmp = ByteCode.newByteCode(code, cPool, tmp);
	}
	for (ByteCode act = first; act != null; act = act.next) {
	    act.linkTargets(this, act.next);
	}

    }
    
    /**adds a new Bytecode just after the Bytecode 'after'.
     *  bc and after must be nonnull. Changing the target of after and any other bytecodes
     * appropriatly is left to the caller of the method.
     */
    public void addBC(ByteCode bc, ByteCode after) {
	if (bc == null || after == null) 
	    throw new Error("Internal Error!");
	
	ByteCode nextBC = after.next;
	after.next = bc;
	bc.prev = after;
	bc.next = nextBC;
	if (nextBC != null) 
	    nextBC.prev = bc;
	
    }

    /**recompute the Addresses of the bytecodes. 
     * Necessary after adding Bytecodes to the list.
     * the ByteCode.address are recomputed as well as the targets of all Branches (by calling  recomputeTargetAddress() for all bytecodes.
     */
    public void recomputeAddresses() {
	int actAddress = 0;
	ByteCode actBC = first;
	while(actBC != null) {
	    actBC.setAddress(actAddress);
	    actAddress += actBC.getSize();
	    actBC = actBC.next;
	}
	//recompute Address of all Targets
	actBC = first;
	while(actBC != null) {
	    actBC.recomputeTargetAddresses();
	    actBC = actBC.next;
	}
    }

    /**Recreate the bytearray for the bytecode.
     */
    public byte[] toByteArray() {
	//get the size of the bytecode.
	int size = 0;
	ByteCode actBC;
	for(actBC = first; actBC != null; actBC = actBC.next) {
	    size += actBC.getSize();
	}
	byte[] ret = new byte[size];
	int index = 0;
	for(actBC = first; actBC != null; actBC = actBC.next) {
	    index = actBC.toByteArray(index, ret);
	}
	return ret;
    }

    public String toString() {
	StringBuffer ret = new StringBuffer(1024);
	for (ByteCode act = first; act != null; act = act.next) {
	    ret.append(act.toString() +"\n");
	}
	//Exception table
	ret.append("Exceptions:\n");
	if (exceptionHandlers.size() == 0) {
	    ret.append("(none)\n");
	} else {
	    for (Enumeration e = exceptionHandlers.elements();  e.hasMoreElements();) {
		ret.append(e.nextElement());
	    }
	}
	return ret.toString();
    }
}
