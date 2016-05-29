package jx.verifier;

import jx.verifier.bytecode.*;
import jx.verifier.VerifyException;
import java.util.Vector;

/** Class for the elements of JVMLocalVars.
 * this class only provides the interface and holds the address of the bytecode that created this element.
 * @see JVMLocalVars
 */
abstract public class JVMLocalVarsElement {
    
    /** merge two local variables into one.
     * this is needed, when merging two states. Each local variable in the states is then merged into the corresponding variable in the other state.<br>
     * if merging changes the data of the Element, a NEW(!) JVMLocalVarsElement should be returned (i.e. merge should not change "this" or "other"!).<br>
     * If called from JVMState.merge() (through JVMLocalVars.merge), "this" is the local variable of the old state, "other" of the new state (see JVMState).<br>
     * @return the merged lVars Element. 
     * @exception VerifyException if not mergeable.
     * @see JVMLocalVars
     * @see JVMState
     */
    public abstract JVMLocalVarsElement merge(JVMLocalVarsElement other) 
	throws VerifyException;

    /**Comparison for the data contained in the local variable.
     * Usually used to decide, if merge changed something or not. 
     * @return true, if the data of other equals the data of this.
     */
    public abstract boolean dataEquals(JVMLocalVarsElement other);
    /**address of the bytecode that created this element.*/
    private int bcAddr;
    /** get the address of the bytecode that created this element. */
    public final int getBcAddr() { return bcAddr;}
    /**constructor.
     *@param bcAddr the address of the bytecode creating this element. -1 if it is a parameter to the method that is analyzed.
     */
    public JVMLocalVarsElement(int bcAddr) {
	this.bcAddr = bcAddr;
    }
    public String toString() {
	return "("+((bcAddr >=0)?Integer.toHexString(bcAddr):"init")+")";
	
    }
}

