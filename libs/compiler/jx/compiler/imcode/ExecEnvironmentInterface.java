package jx.compiler.imcode;

import jx.compiler.*;
import jx.compiler.nativecode.*;
import jx.compiler.symbols.*;
import jx.compiler.execenv.*;

import jx.classfile.constantpool.*;

public interface ExecEnvironmentInterface {
   
    public void setCodeContainer(CodeContainer container);
    public void setCurrentlyCompiling(BCClass aClass);

    public BCMethod getBCMethod(MethodRefCPEntry methodRefCPEntry);

    public boolean doOptimize(int level);
    public CompilerOptionsInterface getCompilerOptions();
    public int getExtraStackSpace();

    public void codeProlog() throws CompileException;
    public void codeEpilog() throws CompileException;

    public void codeCheckReference(IMNode node,Reg reg,int bcPosition) throws CompileException;
    public void codeCheckMagic(IMNode node,Reg reg,int bcPosition) throws CompileException;   
    public void codeCheckDivZero(IMNode node,Reg reg,int bcPosition) throws CompileException;
    public void codeCheckArrayRange(IMNode node,Reg array,int index,int bcPosition) throws CompileException;
    public void codeCheckArrayRange(IMNode node,Reg array,Reg index,int bcPosition) throws CompileException;

    public void codeNewObject(IMNode node,ClassCPEntry classCPEntry,Reg result) throws CompileException;
    public void codeCompactNew(IMNode node,ClassCPEntry classCPEntry,MethodRefCPEntry methodRefCPEntry,IMOperant[] args,Reg result) throws CompileException;
    public void codeNewArray(IMNode node,int type,IMOperant size,Reg result) throws CompileException;
    public void codeNewObjectArray(IMNode node,ClassCPEntry classCPEntry,IMOperant size,Reg result) throws CompileException;

    public void codeGetArrayField(IMNode node,Reg array,int datatype,int index,Reg result,int bcPosition) throws CompileException;
    public void codeGetArrayField(IMNode node,Reg array,int datatype,Reg index,Reg result,int bcPosition) throws CompileException;
    public void codeGetArrayFieldLong(IMNode node,Reg array,int datatype,Reg index,Reg64 result,int bcPosition) throws CompileException;
    public void codePutArrayField(IMNode node,Reg array,int datatype,int index,Reg value,int bcPosition) throws CompileException;
    public void codePutArrayField(IMNode node,Reg array,int datatype,Reg index,Reg value,int bcPosition) throws CompileException;
    public void codeNewMultiArray(IMNode node,ClassCPEntry type,IMOperant[] oprs,Reg result) throws CompileException;
    public void codeGetArrayLength(IMNode node,Reg array,Reg result) throws CompileException;

    public void codeThrow(IMNode node,int exception,int bcPosition) throws CompileException;	
    public void codeThrow(IMNode node,IMOperant exception,int bcPosition) throws CompileException;
    public void codeCheckCast(IMNode node,ClassCPEntry classCPEntry,Reg objRef,int bcPosition) throws CompileException;
    public void codeInstanceOf(IMNode node,ClassCPEntry classCPEntry,Reg objRef,Reg regEAX,int bcPosition) throws CompileException;

    public void codeMonitorEnter(IMNode node,IMOperant obj,int bcPosition) throws CompileException;
    public void codeMonitorLeave(IMNode node,IMOperant obj,int bcPosition) throws CompileException;

    public SymbolTableEntryBase getStringRef(StringCPEntry cpEntry) throws CompileException;
    public void codeLoadStringRef(StringCPEntry cpEntry,Reg result,int bcPosition) throws CompileException;

    public void codeGetField(IMNode node,FieldRefCPEntry fieldRefCPEntry,Reg objRef,Reg result,int bcPosition) throws CompileException ;
    public void codeGetStaticField(IMNode node,FieldRefCPEntry fieldRefCpEntry,Reg result,int bcPosition) throws CompileException;
    public void codePutField(IMNode node,FieldRefCPEntry fieldRefCPEntry,Reg objRef,Reg value,int bcPosition) throws CompileException;
    public void codePutStaticField(IMNode node,FieldRefCPEntry fieldRefCpEntry,Reg result,int bcPosition) throws CompileException;

    public void codeGetFieldLong(IMNode node,FieldRefCPEntry fieldRefCPEntry,Reg objRef,Reg64 result,int bcPosition) throws CompileException ;
    public void codeGetStaticFieldLong(IMNode node,FieldRefCPEntry fieldRefCpEntry,Reg64 result,int bcPosition) throws CompileException;
    public void codePutFieldLong(IMNode node,FieldRefCPEntry fieldRefCPEntry,Reg objRef,Reg64 value,int bcPosition) throws CompileException;
    public void codePutStaticFieldLong(IMNode node,FieldRefCPEntry fieldRefCpEntry,Reg64 value,int bcPosition) throws CompileException;

    public void codeLongMul(IMNode node,IMOperant lOpr,IMOperant rOpr,Reg64 result,int bcPosition) throws CompileException;
    public void codeLongDiv(IMNode node,IMOperant lOpr,IMOperant rOpr,Reg64 result,int bcPosition) throws CompileException;
    public void codeLongRem(IMNode node,IMOperant lOpr,IMOperant rOpr,Reg64 result,int bcPosition) throws CompileException;
    public void codeLongShr(IMNode node,IMOperant lOpr,IMOperant rOpr,Reg64 result,int bcPosition) throws CompileException;
    public void codeLongShl(IMNode node,IMOperant lOpr,IMOperant rOpr,Reg64 result,int bcPosition) throws CompileException;
    public void codeLongUShr(IMNode node,IMOperant lOpr,IMOperant rOpr,Reg64 result,int bcPosition) throws CompileException;
    public void codeLongCompare(IMNode node,IMOperant lOpr,IMOperant rOpr,Reg result,int bcPosition) throws CompileException;

    public void codeVirtualCall(IMNode node,
				MethodRefCPEntry methodRefCPEntry,
				IMOperant        obj,
				IMOperant[]      args,
				int              datatype,
				Reg              result,
				int              bcPosition) throws CompileException;
    public void codeSpecialCall(IMNode node,
				MethodRefCPEntry methodRefCPEntry,
				IMOperant        obj,
				IMOperant[]      args,
				int              datatype,
				Reg              result,
				int              bcPosition) throws CompileException;
    public void codeInterfaceCall(IMNode node,
				  InterfaceMethodRefCPEntry interfaceRefCPEntry,
				  IMOperant        obj,
				  IMOperant[]      args,
				  int              datatype,
				  Reg              result,
				  int              bcPosition) throws CompileException;
    public void codeStaticCall(IMNode node,
			       MethodRefCPEntry methodRefCPEntry,
			       IMOperant[]      args,
			       int              datatype,
			       Reg              result,
			       int              bcPosition) throws CompileException;

    public void codeVirtualCallLong(IMNode node,
				    MethodRefCPEntry methodRefCPEntry,
				    IMOperant        obj,
				    IMOperant[]      args,
				    int              datatype,
				    Reg64            result,
				    int              bcPosition) throws CompileException;
    public void codeSpecialCallLong(IMNode node,
				    MethodRefCPEntry methodRefCPEntry,
				    IMOperant        obj,
				    IMOperant[]      args,
				    int              datatype,
				    Reg64            result,
				    int              bcPosition) throws CompileException;
    public void codeInterfaceCallLong(IMNode node,
				      InterfaceMethodRefCPEntry interfaceRefCPEntry,
				      IMOperant        obj,
				      IMOperant[]      args,
				      int              datatype,
				      Reg64            result,
				      int              bcPosition) throws CompileException;
    public void codeStaticCallLong(IMNode node,
				   MethodRefCPEntry methodRefCPEntry,
				   IMOperant[]      args,
				   int              datatype,
				   Reg64            result,
				   int              bcPosition) throws CompileException;
    
    public void codeStackMap(IMNode node,int InstructionPointer) throws CompileException;
    public UnresolvedJump createExceptionCall(int exception,int bcPosition);
}
