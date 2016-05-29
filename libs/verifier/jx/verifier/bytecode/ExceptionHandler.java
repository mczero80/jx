package jx.verifier.bytecode;

import jx.verifier.VerifyException;
import jx.verifier.Verifier;
import jx.verifier.JVMState;

public class ExceptionHandler {
    private String eName;
    public String getName() { return eName;}
    private ByteCode start;
    public int getStartAddress() { return start.getAddress();}
    private ByteCode end;
    public int getEndAddress() { return end.getAddress();}
    private ByteCode handler;
    public ByteCode getHandler() { return handler;}
    public int getHandlerAddress() { return handler.getAddress();}
    private int eTypeCPIndex;
    public int getETypeCPIndex(){return eTypeCPIndex;}

    /**Constructor.
     * @param start first protected bytecode
     * @param end  the first bytecode not protected.
     * @param handler start of the exception handler
     * @exception VerifyException if the handler is within start and end, or startaddress > endaddress.
     */
    public ExceptionHandler(String eName,
			    ByteCode start,
			    ByteCode end,
			    ByteCode handler,
			    int eTypeCPIndex) throws VerifyException {
	this.eName = eName;
	this.start = start;
	this.end = end.prev;
	this.handler = handler;
	this.eTypeCPIndex = eTypeCPIndex;

	if (this.handler.getAddress() <= this.end.getAddress() && 
	    this.handler.getAddress() >= this.start.getAddress()) {
	    throw new VerifyException("Exception handler starts within protected code!\n"+
			       this);
	}

	if (this.start.getAddress() > this.end.getAddress()) {
	    throw new VerifyException("Exception handler invalid:\n" + this);
	}
    }

    //simulate invokation of Exception handler. 
    // state is the state just before the bytecode that throws the Exception
    public void simulateException(JVMState state) throws VerifyException {
	JVMState eState = state.copy();
	// transform state
	eState.exceptionThrown(eName, handler);
	if (handler.beforeState == null) {
	    //handler has not been checked yet
	    handler.beforeState = eState;
	    eState.getMv().checkBC(handler);
	} else {
	    if (handler.beforeState.merge(eState)) {
		handler.beforeState.getMv().checkBC(handler);
	    }
	}
    }

    public String toString() {
	return "Exception '" + eName +
	    "' - Begin: " + Integer.toHexString(start.getAddress()) + " - End: " +
	    Integer.toHexString(end.getAddress()) + " - handler: " + 
	    Integer.toHexString(handler.getAddress());
    }

    public boolean equals(Object obj) {
	if (obj == this) return true;
	else if (obj == null) return false;
	else if (obj instanceof ExceptionHandler) return equals((ExceptionHandler) obj);
	else return false;
    }
    public boolean equals(ExceptionHandler eh) {
	return (eName.equals(eh.getName()) &&
		handler.getAddress() == eh.getHandler().getAddress() &&
		start.getAddress() == eh.getStartAddress() &&
		end.getAddress() == eh.getEndAddress());
    }
	    

}
