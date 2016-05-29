package jx.verifier;

import jx.verifier.bytecode.*;
import jx.classfile.MethodSource;

public interface VerifierInterface {
    public void runChecks() throws VerifyException;
    public void checkBC(ByteCode e);
    public Subroutines getSrs();
    public String getClassName();
    public MethodSource getMethod();
    public void endChecks();
    /**get User-defined parameter.
     *@return parameter, might also return <code>null</code>.
     */
    public Object getParameter();
}
