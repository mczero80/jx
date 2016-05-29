package jx.verifier.typecheck;

import jx.verifier.bytecode.*;
import jx.verifier.*;
import java.util.Vector;


/* Local Variables for verifying Subroutines (jsr ... ret)
   records every access to a local Variable in accessed array
*/
public class TCSRLocalVars extends JVMSRLocalVars implements TCLocalVarsInterface {

    public TCSRLocalVars(JVMLocalVars otherLV) {
	super(otherLV);
    }

    private TCSRLocalVars(JVMLocalVarsElement[] lVars, boolean[] accessed) {
	super(lVars, accessed);
    }

    public JVMLocalVars copy() { return new TCSRLocalVars(getLVars(), getAccessed()); }
    public JVMSRLocalVars copySR() { return new TCSRLocalVars(getLVars(), getAccessed()); }

    //methods from TCLocalVars///////////////////////////

    //has to provide initial Values for local Variables. isStatic is true,
    //if the method whose local Variables are initialized is static.
    protected void initVars(String className,
			    String methodTypeDesc,
			    boolean isStatic) throws VerifyException {
	TCTypes tmp[] = TCTypes.argTypeFromMethod(methodTypeDesc);
	int i=0;
	try {
	    if (!isStatic) {
		write(i++, new TCObjectTypes(className), -1);
	    }
	    for ( int j = 0; j < tmp.length; j++) {
		write(i++, tmp[j], -1);
	    }
	    for (; i < lVars.length; i++) {
		write(i, TCTypes.T_UNINITIALIZED, -1);
	    }
	} catch (VerifyException e) {
	    e.append("while initializing local Variables for method to " +
		     methodTypeDesc);
	    throw e;
	}
    }

    public void write(int index, TCTypes type, int bcAddr) throws VerifyException{
	
	if (type.getType() == TCTypes.ANY || type.getType() == TCTypes.ANY_REF){
	    throw new Error("Internal Error: Type " + type + 
			    " must not be saved in lvars!; Address:" + 
			    Integer.toHexString(bcAddr));
	}
	/* Short, Byte and Char Values are stored as Ints!*/
	if (type.getType() == TCTypes.SHORT || 
	    type.getType() == TCTypes.CHAR ||
	    type.getType() == TCTypes.BYTE ) {
	    throw new Error("Internal Error: Type " + type + 
			    " must not be written to local Variables! Address:" + 
			    Integer.toHexString(bcAddr));
	}
	super.write(index, new TCLocalVarsElement(type, bcAddr));
    }
    public TCTypes read(int index, TCTypes type) throws VerifyException {
	((TCLocalVarsElement) super.read(index)).getType().consistentWith(type);
	return ((TCLocalVarsElement) super.read(index)).getType();
	 
    }
}

