package jx.verifier.npa;

import jx.verifier.bytecode.*;
import jx.verifier.*;
import java.util.Vector;


public class NPAStack extends JVMOPStack {

    public JVMOPStack copy() {
	return new NPAStack(this);
    }

    public NPAValue NPApop() throws VerifyException { 
	return ((NPAStackElement)super.pop()).getType();
    }

    public NPAValue NPApeek() throws VerifyException {
	return ((NPAStackElement)super.peek()).getType();
    }	
    public NPAValue NPApeek(int index) throws VerifyException {
	return ((NPAStackElement)super.peek(index)).getType();
    }	

    public void push(NPAValue type, int bcAddr) throws VerifyException {
	push(new NPAStackElement(type, null, bcAddr));
    }

    public void push(NPAValue[] type, int bcAddr) throws VerifyException {
	for (int i = 0; i < type.length;i++)
	    push(new NPAStackElement(type[i], null, bcAddr));
    }

    public NPAStack(int maxSize) {
	super(maxSize);
    }
    //constructor for copy
    public NPAStack(JVMOPStack other) {
	super(other.getMaxSize());
	count = other.getCount();
	if (other.getStackPointer() == null) {
	    stackPointer = null;
	    return;}
	stackPointer = ((NPAStackElement)other.getStackPointer()).copyNull();
	NPAStackElement actOther = (NPAStackElement) other.getStackPointer();
	JVMOPStackElement actThis = stackPointer;
	do {
	    actThis.setPrev(actOther.copyNull());
	    actOther = (NPAStackElement) actOther.getPrev();
	    actThis = actThis.getPrev();
	} while(actOther != null);
	//top Element was copied twice!
	stackPointer = stackPointer.getPrev();
    }

    //find all stackelements with same id as value (must be a valid id!) and change their
    //value to newVal.
    public void setValue(NPAValue value, int newVal) {
	NPAStackElement actElm = (NPAStackElement)stackPointer;
	while (actElm != null) {
	    if (actElm.getType().getId() == value.getId()) {
		//the values are the same so update information
		actElm.setType(new NPAValue(newVal, value.getId()));
	    }
	    actElm = (NPAStackElement)actElm.getPrev();
	}
    }


}
	

