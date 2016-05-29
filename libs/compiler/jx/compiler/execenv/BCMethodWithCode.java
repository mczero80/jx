package jx.compiler.execenv; 

import jx.compiler.*;

import jx.classfile.constantpool.*; 
import jx.classfile.*; 
import jx.classfile.datatypes.*; 

import jx.zero.Debug; 

import java.util.Vector;

/** 
    This class represents a Java method with its associated 
    bytecode. It's interface contains various 
    methods to transform and evaluate the method. 
    Ususally you will use this class to translate a method into 
    native code. 
*/ 
public class BCMethodWithCode extends BCMethod {
    //private int numLocalVariables; 
    //private int numOperandSlots; 
    private byte[] bytecode;
    private int byteCodeSize;

    /** 
	Offsets of these variables in the current 
	stackframe. 
    */ 
    //private int currentOperandsOffset; 
    //private int currentLocalVarOffset; 
    private ConstantPool  cPool;
    private ExceptionHandlerData[] handlers;
    private boolean traceEntry=false;
    
    /** 
	The following two variables are only expected values 
	for the offsets of local variables and operand slots. 
	They do not need to contain correct values, they are 
	only here to avoid unneccessary passes. 
    */ 
    private static final int EXPECTED_LOCVAR_OFFSET = 1; 
    private static final int EXPECTED_OPSVARS_OFFSET = 5; 

    /** 
	@param methodSource 
	@param cPool the constant pool of the method's class 
    */ 
    public BCMethodWithCode(MethodSource methodSource, ConstantPool cPool, Vector replaceInterfaceWithClass)  
	throws CompileException {
	super(methodSource);

	this.cPool = cPool;
	bytecode = methodSource.getBytecode(); 
	byteCodeSize = bytecode.length;
	this.handlers = methodSource.getExceptionHandlers();
    }

    public ExceptionHandlerData[] getExceptionHandlers() {
	return handlers;
    }
    
    /** 
     * @return a stream that allows to read the code of this method
     */
    public BytecodeInputStream getBytecodeStream() {
	return new BytecodeInputStream(bytecode, bytecode.length);
    }
    
    public ConstantPool getConstantPool() {
	return cPool;
    }
    
    public int getByteCodeSize() { return byteCodeSize;}
}
