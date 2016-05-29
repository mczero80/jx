package jx.compiler.imcode;

import jx.classfile.datatypes.*;
import jx.classfile.constantpool.*;
import jx.classfile.BC;

//import jx.jit.bytecode.code.*;

public class VirtualOperantenStack {
    
    private IMOperant[] opr_stack;
    private int ptr;
    private CodeContainer imCode;

    public VirtualOperantenStack(CodeContainer imCode) {
	opr_stack = new IMOperant[10];
	ptr=0;
	this.imCode = imCode;
    }

    public void push(IMOperant opr) {
	Object nullPointer=null;
	if (ptr>=opr_stack.length) extend(10);
	if (opr==null) {
	    System.err.println(" store null pointer \n");
	    nullPointer.toString();
	}    
	opr_stack[ptr++]=opr;
    }

    public IMOperant pop() {
	Object nullPointer=null;
	if (ptr==0) {
	    System.err.println(" operanten stack underrun !! \n");
	    nullPointer.toString();
	}
	return opr_stack[--ptr];
    }  

    public void clear() {
	ptr=0;
    }

    public void store(int bcPosition) {
	int next_block_ndx=0;

    	for (int i=0;i<ptr;i++) {
	    IMOperant curr = opr_stack[i];
	    if (curr.isConstant()) continue;
	    if (curr instanceof IMReadLocalVariable) continue; 
	    if (curr.isBlockVariable() && (curr instanceof IMReadBlockVariable)) {
		if (((IMReadBlockVariable)curr).getBlockVarIndex()==i) continue;
	    }

	    IMStoreBlockVariable var = new IMStoreBlockVariable(imCode,i,opr_stack[i],bcPosition);
	    join(var);
	    opr_stack[i]=var.getReadOperation();
	}
    }

    public void store2(int bcPosition,int varIndex) {
	int next_block_ndx=0;

    	for (int i=0;i<ptr;i++) {
	    IMOperant curr = opr_stack[i];
	    if (curr.isConstant()) continue;
	    if (curr instanceof IMReadLocalVariable) {
		IMReadLocalVariable var = (IMReadLocalVariable)curr;
		if (varIndex!=var.getVarIndex()) continue; 
	    }
	    if (curr.isBlockVariable() && (curr instanceof IMReadBlockVariable)) {
		if (((IMReadBlockVariable)curr).getBlockVarIndex()==i) continue;
	    }

	    IMStoreBlockVariable var = new IMStoreBlockVariable(imCode,i,opr_stack[i],bcPosition);
	    join(var);
	    opr_stack[i]=var.getReadOperation();
	}
    }

    public void init(IMOperant[] stack) {
	ptr=0;
	if (stack==null) return;
	for (int i=0;i<stack.length;i++) if (stack[i]!=null) push(stack[i]);
    }

    public IMOperant[] leave() {
	IMOperant[] leaveStack = new IMOperant[ptr+1];
	for (int i=0;i<ptr;i++) {

            if (opr_stack[i] instanceof IMReadBlockVariable) {
                if (((IMReadBlockVariable)opr_stack[i]).getBlockVarIndex()==i) {
			leaveStack[i]=opr_stack[i];
			continue;
		}
            }

	    IMStoreBlockVariable store = new IMStoreBlockVariable(imCode,i,opr_stack[i]);
	    join(store);
	    leaveStack[i] = store.getReadOperation();
	}
	ptr=0;
	return leaveStack;
    }

    public String toTypeString() {
	String ret = "";

	for (int i=0;i<ptr;i++) {
	    if (opr_stack[i]!=null) {
		ret += " " + BCBasicDatatype.toString(opr_stack[i].getDatatype());
	    }
	}
	
	return ret;
    }

    public String stackToString() {
	String ret = "";

	for (int i=0;i<ptr;i++) {
	    ret += " " + opr_stack[i].toReadableString();
	}
	
	return ret;
    }

    public void extend(int value) {
	IMOperant[] new_stack = new IMOperant[opr_stack.length+value];
	for (int i=0;i<opr_stack.length;i++) {
	    new_stack[i]=opr_stack[i];
	}
	opr_stack =  new_stack;
	new_stack = null;
    }

    public void join(IMNode node) {	
	imCode.join(node);
    }
}
