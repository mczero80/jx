package jx.verifier.typecheck;

import jx.verifier.bytecode.*;
import jx.verifier.VerifyException;
import jx.verifier.JVMOPStack;
import jx.verifier.JVMOPStackElement;
import java.util.Vector;


public class OPStack extends JVMOPStack{

    public JVMOPStack copy() {
	return new OPStack(this);
    }
    public OPStack(int maxSize) { super(maxSize);}
    public OPStack(JVMOPStack otherStack) { super(otherStack);}
    public TCTypes TCpop() throws VerifyException { 
	return ((OPStackElement)pop()).getType();
    }
    public TCTypes TCpeek() {
	return ((OPStackElement)stackPointer).getType();
    }	

    public void push(TCTypes type, int bcAddr) throws VerifyException{
	if( type.getType() == TCTypes.ANY || 
	    type.getType() == TCTypes.ANY_REF ||
	    type.getType() == TCTypes.BYTE || 
	    type.getType() == TCTypes.SHORT || 
	    type.getType() == TCTypes.CHAR ){
	    throw new Error("Internal Error: Type " + type + 
			    " must not pushed onto stack! Address:" + 
			    Integer.toHexString(bcAddr));
	}

	super.push(new OPStackElement(type,null, bcAddr));
    }
    public void push(TCTypes[] types, int bcAddr) throws VerifyException {
	for (int i = 0; i < types.length; i++) {
	    push(types[i], bcAddr);
	}
    }

}
	

