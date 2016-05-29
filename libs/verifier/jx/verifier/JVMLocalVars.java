package jx.verifier;

import jx.verifier.bytecode.*;
import jx.verifier.VerifyException;
import java.util.Vector;


/**Class for JVM State's local variables.
 */
abstract public class JVMLocalVars {
    /**create new JVMSRLocalVars (local variables for subroutines), with same content in lVars as this.
     * @return new SRLvars, with same content in lVars as this.
     * @see JVMSRLocalVars
     */
    abstract public JVMSRLocalVars toSRLVars();

    /**Set the initial values of the local Variables.
     *has to provide initial Values for local Variables. 
     * @param isStatic true, if the method whose local Variables are initialized is static.
     * @param methodTypeDesc the typedescriptor of the method whose local variables should be initialized.
     * @exception VerifyException if any problems are encountered, e.g methodTypeDesc is invalid.
     */
    abstract protected void initVars(String className,
				     String methodTypeDesc,
				     boolean isStatic)
				     throws VerifyException;

    /** returns a copy of this. 
     * the copy should be a new JVMLocalVars instance. The JVMLocalVarsElement entries in the lVars array should be the same as in <code>this</code> or should point to copies of those entries.
     */
    public abstract JVMLocalVars copy(); 

    protected JVMLocalVarsElement lVars[];
    public String toString() {
	StringBuffer ret = new StringBuffer(16);
	for (int i = 0; i < lVars.length; i++) {
	    ret.append(" <"+i + "|"+ lVars[i]+">");
	}
	return ret.toString();
    }
    
    /** get the number of local Variables.*/
    public int getNumVars() { return lVars.length;}

    /** create new JVMLocalVars.
     * @param methodTypeDesc same as for initVars.
     * @param isStatic same as for initVars.
     * @param numVars the number of Variables the method has. Note that numVars must be greater than the number of parameters (plus one if isStatic is false).
     * @exception VerifyException initVars is called and might throw an exception.
     * @see JVMLocalVars#initVars
     */
    public JVMLocalVars (String className, String methodTypeDesc, boolean isStatic, 
			 int numVars) throws VerifyException {
	lVars = new JVMLocalVarsElement[numVars];
	initVars(className, methodTypeDesc, isStatic);
    }

    /**constructor for copy.*/
    protected JVMLocalVars(JVMLocalVarsElement lVars[]) {
	this.lVars = new JVMLocalVarsElement[lVars.length];
	for (int i = 0; i < this.lVars.length; i++) {
	    this.lVars[i] = lVars[i];
	}
    }

    /** write e into local variable index.
     * @param index the index of the local variable which should be written. Indices start at 0.
     * @param e the element that should be written.
     * @exception VerifyException if the index is out of bounds.
     */
    public void write(int index, JVMLocalVarsElement e) throws VerifyException {
	if (index >= lVars.length) {
	    throw new VerifyException("Write: index (" + index + 
				      ") out of Range (" + lVars.length + ")!"); 
	}
	lVars[index] = e;
    }

    /** write from local variable index.
     * @param index the index of the local variable which should be written. Indices start at 0.
     * @return the element that was read. Can be null, if no previous write occured.
     * @exception VerifyException if the index is out of bounds.
     */
    public JVMLocalVarsElement read(int index) throws VerifyException {
	if (index >= lVars.length) {
	    throw new VerifyException("read: index (" + index + 
				      ") out of Range (" + lVars.length + ")!"); 
	}
	return lVars[index];
	 
    }
    
    /** get the local variables.*/
    public JVMLocalVarsElement[] getLVars() { return lVars;}

    /** merge the local variables of two JVMStates.
     * First the size of the two local variables are compared. Then JVMLocalVarsElement.merge is called for each entry.<br>
     * If called from JVMState.merge(), "this" are the local variables of the old state, "otherVars" of the new state (see JVMState).<br>
     * @param otherVars the local variables that should be merged with "this".
     * @return true if lvars were not the same and merge changed something.
     * @exception VerifyException if the local variables were of different size.
     * @see JVMState
     */
    public boolean merge(JVMLocalVars otherVars) throws VerifyException {
	JVMLocalVarsElement otherLVars[] = otherVars.getLVars();
	if (otherLVars.length != lVars.length) {
	    throw new Error("Internal Error:  Merging two local Variables of different size!");
	}
	
	boolean retval = false;
	for (int i = 0; i < lVars.length; i++) {
	    if (lVars[i] != otherLVars[i]) {
		JVMLocalVarsElement oldElm = lVars[i];
		if (otherLVars[i] == null) {
		    //lVars[i] != otherLVars[i] --> lVars[i] != null
		    write(i, lVars[i].merge(null));
		} else if (lVars[i] == null) {
		    write(i, otherLVars[i].merge(null));
		} else {
		    //lVars[i] != otherLVars[i] && both != null
		    write(i, lVars[i].merge(otherLVars[i]));
		}
		//check if something changed!
		if (oldElm != lVars[i]) {
		    if (oldElm != null && oldElm.dataEquals(lVars[i])) {
			//retval = retval | false; //= do nothing.
		    } else {
			retval = true;
		    }
		}
	    }
	}
	return retval;
    }



}


