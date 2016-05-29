package jx.verifier;

import jx.verifier.bytecode.*;
import jx.verifier.VerifyException;
import java.util.Vector;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**Class to hold the Operand Stack for a method during verification.
 * stack consists of a linked list of JVMOPStackElement elements.
 *@see JVMOPStackElement
 */
abstract public class JVMOPStack {

    /** copy the stack and return a new Instance.*/
    abstract public JVMOPStack copy();

    /** Pointer to the topmost stackelement.*/
    protected JVMOPStackElement stackPointer;
    /** actual size of the stack.*/
    protected int count;
    /**maximum size of the stack.*/
    private int maxSize;
    /**get the maximal size of the stack.*/
    public final int getMaxSize() { return maxSize;}
    /** get the actual size of the stack.*/
    public int getCount() { return count;}
    /** pop the topmost element from the stack.
     *@return the popped stackelement.
     *@exception VerifyException if the stack is empty
     */
    public JVMOPStackElement pop() throws VerifyException{ 
	if (stackPointer == null) {
	    throw new VerifyException("Pop on empty stack!");
	}
	JVMOPStackElement ret = stackPointer;
	stackPointer = stackPointer.getPrev();
	count--;
	return ret;
    }
    /** return the topmost element from the stack, leaving it on the stack.
     *@return the topmost stackelement. Always nonnull.
     *@exception VerifyException if the stack is empty
     */
    public JVMOPStackElement peek() throws VerifyException {
	if (stackPointer == null) {
	    throw new VerifyException("Peek on empty stack!");
	}
	return stackPointer;
    } 
    /** pop the element at "top" - "index" from the stack.
     *@param index specifies the element to be returned. top is 0.
     *@return the popped stackelement.
     *@exception VerifyException if the actual stacksize is less than index+1.
     */
    public JVMOPStackElement peek(int index) throws VerifyException {
	if (stackPointer == null) {
	    throw new VerifyException("Peek on empty stack!");
	}
	JVMOPStackElement actElm = stackPointer;
	for (int i = 0; i < index; i++) {
	    actElm = actElm.getPrev();
	    if (actElm == null) {
		throw new VerifyException("Peek on empty stack!");
	    }
	}
	return actElm;
    }

    /** push a new element onto the stack.
     * Note: the "prev" member of the new element must be <code>null</code>.
     * @param elm the new element. Note that elm.prev must be <code>null</code>.
     * @exception VerifyException if maximum stack size is reached.
     */
    public void push(JVMOPStackElement elm) throws VerifyException {
	elm.setPrev(stackPointer);
	stackPointer = elm;
	count++;
	if (count > maxSize) {
	    throw new VerifyException("Maximum size ( " + maxSize +
				      ") for Operand-Stack exceeded!");
	}
    }

    /** get the stackPointer.
     * the same as peek, only that the returned value may be null.
     *@return the stackpointer (may be null).
     */
    public JVMOPStackElement getStackPointer() {return stackPointer;}

    /**Constructor
     * @param maxSize gives the maximum size of the stack
     */
    public JVMOPStack(int maxSize) { 
	stackPointer = null;
	this.maxSize = maxSize;
    }

    /**constructor for copy.
     * creates a new stack, identical to copyStack
     */
    public JVMOPStack(JVMOPStack copyStack) {
	this.stackPointer = copyStack.getStackPointer();
	this.count = copyStack.getCount();
	this.maxSize = copyStack.getMaxSize();
    }

    /** merges two operand stacks.
     * 'this' and 'otherStack' are merged into new stack 'this'.<br>
     * if there are any changes, all Stack ELEMENTS have to be copied,
     * because stack-Elements may be used in more than one stack so they must not change.<br>
     * merging is done by calling the merge method of each stackelement.<br>
     * If called from JVMState.merge(), "this" is the stack of the old state, "otherStack" of the new state (see JVMState).<br>
     * The prev reference of the actual elements in both stacks are compared. If the reference is the same, merge is done. Else merge of JVMOPStackElement is called. This is done until the references are the same (probably null). 
     * @return false if no changes were made to this, true if merging successfull but contents of 'this' changed
     * @exception VerifyException if stacks are not consistent.
     * @see JVMState
     */
    public boolean merge(JVMOPStack otherStack)  throws VerifyException {
	if (otherStack == null) 
	    return false;
	if (maxSize != otherStack.getMaxSize()) {
	    throw new Error("Internal Error: Maximum sizes for stacks differ!\nthis: " +
			    maxSize + " - other: " + otherStack.getMaxSize());
	}
	if (count != otherStack.getCount()) {
	    throw new VerifyException("Stacks of different size!\n"+
				      "  this : " + this.toString() +
				      "\n  other: " + otherStack.toString());
	}
	
	//merge the two stacks:
	//start at the top and create new JVMOPStackElement by merging the top ones of the
	//  two stacks
	//proceed to previous element in stacks and create the merged Element.
	//  set the newly created element as prev of the element created before
	//until the two stackpointers are the same. then set the prev-pointer of the
	//  last created element to the stackpointer.
	
	if ((stackPointer == null) || (otherStack.getStackPointer() == null)) {
	    if (stackPointer != otherStack.getStackPointer()) {
		throw new VerifyException("Stacks Inconsistent:\n"+
					  "  this : " + this.toString() +
					  "\n  other:" + otherStack.toString());
	    } else {
		return false;
	    }
	}
	JVMOPStackElement sp1 = stackPointer;
	JVMOPStackElement sp2 = otherStack.getStackPointer();
	JVMOPStackElement mergedElm = null;
	//dirty Trick: topElement is a dummy Element which is only used to record the
	//top Element of the stack! As JVMOPStackElement is an abstract class,
	//it can not be instantiated. So merge dp1 with itself to get a new element of
	//the appropriate type.
	JVMOPStackElement topElement = sp1.merge(sp1);
	JVMOPStackElement actStackBottom = topElement; //records actual position in  stack
	
	//stackelements have a 'prev' pointer that can never be changed
	// --> if sp1 and sp2 are the same once, all previous elements are the same too
	boolean retval = false; 
	while (sp1 != sp2 ) {
	    if ((sp1 == null) || (sp2 == null)){
		throw new VerifyException("Stacks Inconsistent:\n"+
					  "  this : " + this.toString() +
					  "\n  other:" + otherStack.toString());
	    }
	    try {
		// merge
		//if (!sp1.dataEquals(sp2))
		mergedElm =  sp1.merge(sp2); 
		//if changes occured --> check again
 		if (!mergedElm.dataEquals(sp1)) 
		    retval = true;
	    } catch (VerifyException e) {
		e.append(" while comparing Stacks:\n" +
			 "  this :" + this.toString() + 
			 "\n  other:" + otherStack.toString());
	    }
	    //copy Stack
	    actStackBottom.setPrev(mergedElm);
	    
	    actStackBottom = actStackBottom.getPrev();
	    sp1 = sp1.getPrev();
	    sp2 = sp2.getPrev();
	}
	//sp1 and sp2 are the same here, but not necessarily null.
	//-->set actStackBottom.prev to either of them
	actStackBottom.setPrev(sp1);
	
	//the first element of the stack is prev of topElement!
	stackPointer = topElement.getPrev();
	return retval;
    }
    
    public String toString() {
	return (stackPointer != null)? stackPointer.toString():
	    "(Empty Stack)";
    }

    /**returns an Enumeration of all stack Elements, starting at the TOP.*/
    public Enumeration elements() {
	return new StackEnum(stackPointer);
    }
}

/**Class for Enumeration of stackelements.*/
class StackEnum implements Enumeration {
    JVMOPStackElement current;
    public StackEnum(JVMOPStackElement stackPointer) {
	current = stackPointer;
    }
    public boolean hasMoreElements() {
	return (current != null);
    }
    public Object nextElement() throws NoSuchElementException {
	if (!hasMoreElements())
	    throw new NoSuchElementException();
	Object retval = current;
	current = current.getPrev();
	return retval;
    }
}	

