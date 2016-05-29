package jx.verifier;

import jx.verifier.bytecode.*;
import jx.verifier.VerifyException;
import java.util.Vector;


/** Local Variables for verifying Subroutines (jsr ... ret).
 * every access to local variables is recorded. When "merging" (see apply()) the Variables with
 * other local variables (JVMLocalVars) the only those elements are merged which have been accessed.
 * @see JVMLocalVars
*/
abstract public class JVMSRLocalVars extends JVMLocalVars{

    /**return a new instance which is a copy of this.*/
    abstract public JVMSRLocalVars copySR();

    /** same as copySR(), only necessary to overwrite the method of JVMLocalVars.*/
    public JVMSRLocalVars toSRLVars(){
	//this would probably be ok as well, but copy is safe, 
	//after all one expects a NEW object
	return this.copySR();}

    /**returns a new instance which is a copy of this, casted to JVMLocalVars.
     * same as copySR(), only casted to JVMLocalVars.
     *@see JVMSRLocalVars#copySR
     *@see JVMLocalVars
     */
    public JVMLocalVars copy(){
	return (JVMLocalVars) this.copySR();
    }

    /**this array records which local variables were accessed.*/
    private boolean accessed[];
    /**returns the accessed array*/
    public boolean[] getAccessed() { return accessed;}

    /**Constructor.
     * constructs a new JVMSRLocalVars instance out of other local variables.
     * @param otherLV the other local variables which should be copied to create the new instance.
     */
    public JVMSRLocalVars(JVMLocalVars otherLV) {
	super(otherLV.getLVars());
	accessed = new boolean[lVars.length]; 
	for (int i = 0; i < accessed.length; i++) {
	    accessed[i] = false;
	}
    }

    /**Constructor for copySR().*/
    protected JVMSRLocalVars(JVMLocalVarsElement[] lVars, boolean[] accessed) {
	super(lVars);
	this.accessed = new boolean[accessed.length];
	for (int i = 0; i < this.accessed.length; i++) {
	    this.accessed[i] = accessed[i];
	}
    }

    /**read local Variable.
     * the read-access is recorded in <code>accessed[index]</code>
     * @param index the index of the variable to be read. Indices start at 0.
     * @return the element that was read. Always nonnull.
     * @exception VerifyException if the underlying call to JVMLocalVars.read() throws it.
     * @see JVMLocalVars#read
     */
    public JVMLocalVarsElement read(int index) throws VerifyException {
	accessed[index] = true;
	return super.read(index);
    }
    
    /**write local Variable.
     * the write-access is recorded in <code>accessed[index]</code>
     * @param index the index of the variable to be read. Indices start at 0.
     * @param e the element that should be written. Must be nonnull.
     * @exception VerifyException if the underlying call to JVMLocalVars.write() throws it.
     * @see JVMLocalVars#write
     */
    public void write(int index, JVMLocalVarsElement e) throws VerifyException {
	accessed[index] = true;
	super.write(index, e);
    }

    /** merge the Subroutine local variables of two JVMStates.
     * First merge of JVMLocalVars is caled, then the accessed array is merged: if a variable was accessed in any of the two sources, it is accessed in the result as well.
     * @param otherVars the Subroutine local variables that should be merged with "this".
     * @return true if lvars were not the same and merge changed something.
     * @exception VerifyException if thrown by JVMLocalVars
     * @see JVMLocalVars#merge
     */
    public boolean merge(JVMSRLocalVars otherVars) throws VerifyException {
	boolean retval = super.merge(otherVars);
	for (int i = 0; i < accessed.length; i++) {
	    accessed[i] = accessed[i] || otherVars.getAccessed()[i];
	}

	return retval;
    }

    /**Merge with other JVMSRLocalVars.
     * Merge with other local Variables. However a JVMSRLocalVars cannot be merged with a JVMLocalVar (use apply instead), so otherVars mus be of type JVMSRLocalVars. It is declared as JVMLocalVars only for compability with normal local variables.
     * @param otherVars instance of JVMSRLocalVars that should be merged with this. If it is only an instance of JVMLocalVars and Error is thrown.
     * @return true if merge changed something, else false
     * @exception VerifyException if the underlying call to JVMSRLocalVars.merge(JVMSRLocalVars) throws one.
     * @exception java.lang.Error if otherVars is not instanceof JVMSRLocalVars.
     * @see JVMSRLocalVars#merge(JVMSRLocalVars)
     * @see JVMSRLocalVars#apply
     */
    public boolean merge(JVMLocalVars otherVars) throws VerifyException {
	if (otherVars instanceof JVMSRLocalVars) {
	    return merge((JVMSRLocalVars) otherVars);
	} else {
	    throw new Error("Internal Error: merge(JVMLocalVars) called instead of apply!");
	}
    }


    /**"Merge" into normal local Variables.
     * checks all local Variables that were accessed and merge with other'l local variables.
     * the "result" is held in other!
     * @param other local variables into which this should be merged. other will be changed, if necessary.
     * @return true if otherVars were changed.
     */
    public boolean apply(JVMLocalVars other) throws VerifyException {
	boolean retval = false;
	JVMLocalVarsElement[] otherLVars = other.getLVars();
	for (int i=0; i < getLVars().length; i++) {
	    if (accessed[i]) {
		//merge element i
		JVMLocalVarsElement oldElm = getLVars()[i];
		if (getLVars()[i] != otherLVars[i]) {
		    if (otherLVars[i] == null) {
			//lVars[i] != otherLVars[i] --> lVars[i] != null
			write(i, getLVars()[i].merge(null));
		    } else if (getLVars()[i] == null) {
			write(i, otherLVars[i].merge(null));
		} else {
		    //lVars[i] != otherLVars[i] && both != null
		    write(i, getLVars()[i].merge(otherLVars[i]));
		}
		    //check if something changed!
		    retval |= 
			((oldElm == getLVars()[i]) ||
			 (oldElm != null && oldElm.dataEquals(getLVars()[i])));
		}
	 
	    }
	}
	return retval;
    }

    public String toString() {
	return "(SR)" + super.toString();
    }
}

