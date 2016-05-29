package jx.verifier.wcet;

import jx.verifier.bytecode.*;
import jx.verifier.*;
import java.util.Vector;


public class WCETStack extends JVMOPStack {

    public JVMOPStack copy() {
	return new WCETStack(this);
    }

    //maxSize gives the maximum size of the stack
    public WCETStack(int maxSize) { 
	super(maxSize);
    }

    //constructor for copy
    public WCETStack(JVMOPStack copyStack) {
	super(copyStack);
    }

    public void push() throws VerifyException {
	super.push(new WCETStackElement(null));
    }

    public void push(int bcAddr) throws VerifyException {
	super.push(new WCETStackElement(null, bcAddr));
    }

}
	

