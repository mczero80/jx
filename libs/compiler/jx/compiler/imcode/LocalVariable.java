package jx.compiler.imcode;

import jx.classfile.datatypes.BCBasicDatatype;

public class LocalVariable {

    // experimental 
    private boolean doLessChecks = false;

    /* we will only read from this variable */
    public boolean constant;
    /* in use or not */    
    public boolean unused;
    /* mem position */
    public int off;
    /* size */
    public int size;
    /* datatype */
    private int datatype;
    /* slottype */
    private int slottype;
    /* */
    private Object checked;
    /* */
    private Object checkedArray;
    /* */
    private int    checkedRange;
    /* */
    private LocalVariable checkedIndex;
    /* */
    private boolean modifyedIndex;

    public static final int MAX_TYPES = 5;
    public static final int ARGS      = 0;
    public static final int VARS      = 1;
    public static final int BLKS      = 2;
    public static final int TMPS      = 3;

    public LocalVariable(int stype,int dtype) {
	this(stype,dtype,0);	
    }

    public LocalVariable(int stype,int dtype,int offset) {
	off      = offset;
	datatype = dtype;
	slottype = stype;
	checked  = null;
	checked       = null;
	checkedArray  = null;
	checkedRange  = 0;
	checkedIndex  = null;
	modifyedIndex = true;
	size     = BCBasicDatatype.sizeInWords(dtype);
	unused   = false;
	if (stype==1) constant = true;
	else constant=false;
    }

    public void check(Object basicBlock) {
	checked = basicBlock;
        // test
	basicBlock = null;
    }

    public void modify() {
	checked       = null;
	checkedArray  = null;
	checkedRange  = 0;
	checkedIndex  = null;
	modifyedIndex = true;
	if (constant) throw new Error("local var is not constant");
    }

    public boolean isChecked(Object basicBlock) {
	if (checked!=null && constant && doLessChecks) return true;
	if (checked!=null && checked==basicBlock) return true;
	return false;
    }

    public boolean isModifyed() {
	if (!modifyedIndex) return false;
	modifyedIndex = false;
	return true;
    }

    public boolean isRangeChecked(Object basicBlock,int range) {
	if (checkedArray!=basicBlock) {
	    checkedIndex=null;
	    checkedRange=range;
	} else {
	    if (range<=checkedRange) return true;
	}
	checkedRange = range;
        checkedArray   = basicBlock;
	return false;
    }

    public boolean isIndexChecked(Object basicBlock,LocalVariable index) {
	if (checkedArray!=basicBlock) checkedRange=0;

	if (checkedIndex!=null && index==checkedIndex) {
	    if (!checkedIndex.isModifyed() && checkedArray==basicBlock) {
		return true;
	    }
	} else {
	    checkedIndex = index;
	    checkedIndex.isModifyed();
	}
	checkedArray = basicBlock;
	return false;
    }

    public void setDatatype(int dtype) {
	datatype = dtype;
    }

    public int getDatatype() {
	return datatype;
    }

    public boolean isDatatype(int dtype) {
	return datatype==dtype;
    }

    public int getType() {
	return slottype;
    }

    public boolean isType(int stype) {
	return slottype==stype;
    }

    public String addrString() {
	return Integer.toString(slottype)+":"+Integer.toString(off);
    }

    public String toString() {
	return BCBasicDatatype.toString(datatype)+" at "+addrString();
    }
}
