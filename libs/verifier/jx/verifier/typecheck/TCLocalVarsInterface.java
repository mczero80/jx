package jx.verifier.typecheck;

import jx.verifier.bytecode.*;
import jx.verifier.VerifyException;
import jx.verifier.JVMLocalVars;
import jx.verifier.JVMLocalVarsElement;
import jx.verifier.JVMSRLocalVars;
import java.util.Vector;



public interface TCLocalVarsInterface {
    public void write(int index, TCTypes type, int bcAddr) throws VerifyException;    
    public TCTypes read(int index, TCTypes type) throws VerifyException;    
}


