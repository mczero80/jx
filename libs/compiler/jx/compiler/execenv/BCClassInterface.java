package jx.compiler.execenv; 

import jx.classfile.constantpool.*; 
import jx.classfile.*; 

import java.util.Vector;

import jx.compiler.CompileException;

public interface BCClassInterface {

    public void setInfo(Object info);
    public Object getInfo();

    public boolean isInterface();
    public ClassSource getClassSource();
    public BCMethod getMethod(String name, String type);
    public String getClassName();
    public BCMethodWithCode getMethodWithCode(String name, String type) throws CompileException;

    // get all methods of this class
    // returns also abstract methods
    // filters out native methods
    public BCMethod[] getAllMethodsWithCode(Vector transformIfcallToClass) throws CompileException;
    public ConstantPool getConstantPool();
    public void invalidateMethod(BCMethod method);
}
