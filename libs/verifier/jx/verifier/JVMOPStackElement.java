package jx.verifier;

import jx.verifier.bytecode.*;
import jx.verifier.VerifyException;
import java.util.Vector;

/**Class for stack elements.
 * The stackelements hold only a pointer to the previous element (null if there is no previous element) and the address of the bytecode that pushed this element onto the stack. Everything else is left to the subclasses.
 */
abstract public class JVMOPStackElement {
    ///////////////////
    // ABSTRACT PART //
    ///////////////////

    /**merges two StackElements.
     * two stack elements can be merged, if the are either exactly the same, or they are of type Reference. In the latter case they are merged in the "greatest" common reference type.<br>
     * If called from JVMState.merge() (through JVMOPStack.merge()), "this" is an element of the old stack, "other" of the new stack (see JVMState).<br>
     * @return a NEW(!) JVMOPStackElement (even if this and other were the same!), whose prev pointer must be null (so it can be set to another value later see setPrevious())
     * @exception VerifyException if not mergeable
     * @see JVMOPStackElement#setPrevious
     * @see JVMState
     * @see JVMOPStack
     */
    public abstract JVMOPStackElement merge(JVMOPStackElement other) throws VerifyException;
    /** check if the data of two stack elements is the same.
     * @return true, if the data of other equals the data of this (i.e. no change required when merging the two stack elements).
     */
    public abstract boolean dataEquals(JVMOPStackElement other);


    //constants for special bcAddresses
    public static final int ADDR_INIT = -1;
    public static final int ADDR_EXCEPTION = -2; //to be used when initializing exception handling
    public static final int ADDR_MERGE = -3; //used when new element is due to merging two other ones.

    private JVMOPStackElement prev;
    private int bcAddr;
    /** returns the address of the creating bytecode.
     *@return address, or ADDR_INIT if the element was created during initialization, ADDR_EXCEPTION when initializing exception handling and ADDR_MERGE when it is an result of the merging of two stackElements.
     */
    public final int getBCAddr() { return bcAddr;}
    public final JVMOPStackElement getPrev() {return prev;}

    /** set previous reference if null.
     * only if prev is null, it can be set to a new value. This helps copying stacks starting at the top.
     * @exception java.lang.Error when called on an element which prev reference is not null (no VerifyException is thrown because this can only happen if there is an error in the verifier itself, not in the analyzed code).
     */
    public final void setPrev(JVMOPStackElement prev) {
	if (this.prev == null) {
	    this.prev = prev;
	} else {
	    throw new Error("Internal Error: trying to change prev Element of JVMOPStackElement that is not null!");
	}
    }

    /**Constructor.
     * address will be ADDR_INIT.
     * @param prev the reference to the previous stack element. Must be null if setPrev should be used later.
    */
    public JVMOPStackElement(JVMOPStackElement prev) {
	this(prev, -1);
    }

    /**Constructor.
     * @param prev the reference to the previous stack element. Must be null if setPrev should be used later.
     * @param bcAddr the address of the bytecode creating the element.
     */
    public JVMOPStackElement(JVMOPStackElement prev, int bcAddr) {
	this.prev = prev;
	this.bcAddr = bcAddr;
    }
    

    public String addressString() {
	String addr = null;
	switch (bcAddr) {
	case ADDR_INIT:
	    addr = "init";
	    break;
	case ADDR_EXCEPTION:
	    addr = "exception";
	    break;
	case ADDR_MERGE:
	    addr = "merge";
	    break;
	default:
	    addr = Integer.toHexString(bcAddr);
	}
	return addr;
    }
    public String toString() {
	return (prev != null)? prev.toString() + ": ("+ addressString() + ")"
	    : 
	    "(" + addressString() + ")";
    }
}
