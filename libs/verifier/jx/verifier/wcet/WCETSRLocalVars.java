package jx.verifier.wcet;

import jx.verifier.bytecode.*;
import jx.verifier.*;
import java.util.Vector;


/* Local Variables for verifying Subroutines (jsr ... ret)
   records every access to a local Variable in accessed array
*/
public class WCETSRLocalVars extends JVMSRLocalVars {

    public JVMSRLocalVars copySR() {
	return new WCETSRLocalVars(lVars, getAccessed());
    }

    public WCETSRLocalVars(JVMLocalVars otherLV) {
	super(otherLV);
    }

    protected WCETSRLocalVars(JVMLocalVarsElement[] lVars, boolean[] accessed) {
	super(lVars, accessed);
    }

    protected void initVars(String className,
			    String methodTypeDesc,
			    boolean isStatic) throws VerifyException {
	//FEHLER -konstanten berücksichtigen?
	return;
    }
}

