
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
// ***** IMPutField *****

final public class IMPutField extends IMOperant  {

    private FieldRefCPEntry cpEntry;
    private IMOperant rvalue;
    private IMOperant obj;
    private String fieldType;

    public IMPutField(CodeContainer container,int bc,int bcpos,FieldRefCPEntry cpEntry) {
	super(container);
	bytecode     = bc;
	bcPosition   = bcpos;
	this.cpEntry = cpEntry;
	fieldType    = cpEntry.getMemberTypeDesc();
	datatype     = BasicTypeDescriptor.getBasicDatatypeOf(fieldType);
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) throws CompileException {
	rvalue = stack.pop();
	obj   = stack.pop();
	stack.store(bcPosition);
	return this;
    }

    public IMNode inlineCode(CodeVector iCode,int depth, boolean forceInline) throws CompileException {
	obj    = (IMOperant)obj.inlineCode(iCode, depth, forceInline);
	rvalue = (IMOperant)rvalue.inlineCode(iCode, depth, forceInline);
	return this;
    }

    public IMNode constant_folding() throws CompileException{
	obj = (IMOperant)obj.constant_folding();
	rvalue = (IMOperant)rvalue.constant_folding();
	return this;
    }

    public IMNode assignNewVars(CodeContainer newContainer,int slots[],IMOperant opr[],int retval,int bcPos) throws CompileException {
	bcPosition = bcPos;
	init(newContainer);
	obj    = (IMOperant)obj.assignNewVars(newContainer,slots,opr,retval,bcPos);
	rvalue = (IMOperant)rvalue.assignNewVars(newContainer,slots,opr,retval,bcPos);
	return this;
    }

    public String toReadableString() {
	return obj.toReadableString()+"."+cpEntry.getMemberName()+" = " + rvalue.toReadableString();
    }

    public void getCollectVars(Vector vars) { 
	obj.getCollectVars(vars);
	rvalue.getCollectVars(vars); 
    }

    public int getNrRegs() { return obj.getNrRegs() + rvalue.getNrRegs(); }

    // IMPutField
    public void translate(Reg result) throws CompileException {
	Reg value =regs.chooseIntRegister(null);
	Reg refObj=regs.chooseIntRegister(value);

	rvalue.translate(value);
	obj.translate(refObj);
	regs.readIntRegister(value);

	code.startBC(bcPosition);
	
	if (obj.checkReference()) 
	    execEnv.codeCheckReference(this,refObj,bcPosition);

	if (datatype==BCBasicDatatype.REFERENCE) 
	    execEnv.codeCheckMagic(this,value,bcPosition);

	execEnv.codePutField(this,cpEntry,refObj,value,bcPosition);
	code.endBC();

	regs.freeIntRegister(refObj);
	regs.freeIntRegister(value);
    }

    public void translate(Reg64 result) throws CompileException {

	if (datatype!=BCBasicDatatype.LONG) throw new CompileException("wrong datatype");

	//Reg64 value =regs.chooseLongRegister(null);
	Reg64 value = result;
	Reg   refObj=regs.chooseIntRegister(value.low,value.high);

	rvalue.translate(value);
	obj.translate(refObj);

	regs.readLongRegister(value);

	code.startBC(bcPosition);
	
	if (obj.checkReference()) 
	    execEnv.codeCheckReference(this,refObj,bcPosition);

	execEnv.codePutFieldLong(this,cpEntry,refObj,value,bcPosition);
	code.endBC();

	regs.freeIntRegister(refObj);
	regs.freeLongRegister(value);
    }
}
