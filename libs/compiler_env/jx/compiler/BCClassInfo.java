package jx.compiler;

import jx.compiler.execenv.*; 

import jx.zero.Memory;

import java.util.Vector;
import jx.compiler.vtable.MethodTable;

/**
 * connected to BCClass
 * contains information that is only interesting
 * in this execution environment
 */
public class BCClassInfo {
    public BCClass superClass;
    public BCClass[] interfaces;
    public BCMethod[] methods;
    public NativeCodeContainer[] nativeCode;
    public Memory[] machineCode;
    public MethodTable methodTable;
    public int instanceSize;
    public int staticSize;
    public FieldLayout objectLayout;
    public FieldLayout classLayout;
    public CompactFieldLayout mappedLayout; // only for MappedObjects

    public String toString() {
	return "BCClassInfo methods:"+methods;
    }
}
