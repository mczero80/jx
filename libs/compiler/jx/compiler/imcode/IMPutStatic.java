package jx.compiler.imcode; 

import jx.classfile.constantpool.*; 
import jx.classfile.datatypes.*; 
import jx.classfile.*;
import jx.zero.Debug; 
import jx.compiler.*;
import jx.compiler.nativecode.*;
import jx.compiler.symbols.*;
import jx.compiler.execenv.*;
import java.util.Vector;
// ***** IMPutStatic *****

final public class IMPutStatic extends IMOperant  {

    private IMOperant       vOpr;
    private FieldRefCPEntry cpEntry;
    private String fieldType;

    public IMPutStatic(CodeContainer container,int bc,int bcpos,FieldRefCPEntry cpEntry) {
	super(container);
	bytecode     = bc;
	bcPosition   = bcpos;
	this.cpEntry = cpEntry;
	fieldType    = cpEntry.getMemberTypeDesc();
	datatype     = BasicTypeDescriptor.getBasicDatatypeOf(fieldType);
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) throws CompileException {
	vOpr = stack.pop();
	stack.store(bcPosition);
	return this;
    }

    public IMNode inlineCode(CodeVector iCode,int depth, boolean forceInline) throws CompileException {
	vOpr = (IMOperant)vOpr.inlineCode(iCode, depth, forceInline);
	return this;
    }

    public IMNode constant_folding() throws CompileException{
	vOpr = (IMOperant)vOpr.constant_folding();
	return this;
    }

    public IMNode assignNewVars(CodeContainer newContainer,int slots[],IMOperant opr[],int retval,int bcPos) throws CompileException {
	bcPosition = bcPos;
	init(newContainer);
	vOpr = (IMOperant)vOpr.assignNewVars(newContainer,slots,opr,retval,bcPos);
	return this;
    }

    public String toReadableString() {
	return cpEntry.getMemberName()+" = "+vOpr.toReadableString();
    }

    public int getNrRegs() { return vOpr.getNrRegs(); }

    public void getCollectVars(Vector vars) { vOpr.getCollectVars(vars); }

    // IMPutStatic
    public void translate(Reg result) throws CompileException {
	

	Reg value=regs.chooseIntRegister(null);

	vOpr.translate(value);

	if (datatype==BCBasicDatatype.REFERENCE) 
	    execEnv.codeCheckMagic(this,value,bcPosition);

	code.startBC(bcPosition);
	execEnv.codePutStaticField(this,cpEntry,value,bcPosition);
	code.endBC();

	regs.freeIntRegister(value);
    }

    public void translate(Reg64 result) throws CompileException {
	if (datatype!=BCBasicDatatype.LONG) throw new CompileException("wrong datatype");
	
	Reg64 value=regs.chooseLongRegister();

	vOpr.translate(value);

	code.startBC(bcPosition);
	execEnv.codePutStaticFieldLong(this,cpEntry,value,bcPosition);
	code.endBC();

	regs.freeLongRegister(value);
    }
}
