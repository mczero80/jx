
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
// ***** IMStoreBlockVariable *****

final public class IMStoreBlockVariable extends IMVarAccess {

    private IMOperant operant;
    private LocalVariable lvar;
    private int number;

    public IMStoreBlockVariable(CodeContainer container,int number, IMOperant opr) {
	super(container);
	tag |= IMNode.BLOCKVAR;
	this.number = number;
	operant    = opr;
	writeAccess= true;
	datatype   = opr.getDatatype();
	bcPosition = opr.getBCPosition();
	this.lvar  = frame.getBlockSlot(datatype,number);
	//addDebugInfo("b"+Integer.toString(number)+"="+BCBasicDatatype.toString(datatype));
    }

    public IMStoreBlockVariable(CodeContainer container,int number, IMOperant opr,int bcPosition) {
	super(container);
	tag |= IMNode.BLOCKVAR;
	this.number = number;
	operant    = opr;
	writeAccess= true;
	datatype   = opr.getDatatype();
	this.bcPosition = bcPosition;
	this.lvar = frame.getBlockSlot(datatype,number);
	//addDebugInfo("b"+Integer.toString(number)+"="+BCBasicDatatype.toString(datatype));
    }

    public int getBlockVarIndex() {
	return number;
    }

    public void setOperant(IMOperant opr) {
	operant = opr;
    }

    public IMOperant getOperant() {
	return operant;
    }

    public IMNode inlineCode(CodeVector iCode,int depth, boolean forceInline) throws CompileException {
	operant = (IMOperant)operant.inlineCode(iCode, depth, forceInline);
	return this;
    }

    public int getNrRegs() { return operant.getNrRegs(); }

    public void getCollectVars(Vector vars) { 
	operant.getCollectVars(vars);
	vars.addElement(this); 
	return; 
    }
    
   // IMStoreBlockVariable
    public void translate(Reg result) throws CompileException {
	int varOffset = frame.getOffset(lvar);
	lvar.setDatatype(datatype);
	if (operant.isConstant() && datatype!=BCBasicDatatype.REFERENCE) {
	    int value = ((IMConstant)operant).getIntValue();

	    code.startBC(bcPosition);

	    if (value==0 && regs.hasFreeIntRegister()) {
		regs.allocIntRegister(result,datatype);
		code.xorl(result,result);
		regs.writeIntRegisterToSlot(result,lvar);
	    } else {
		regs.writeIntToSlot(value,lvar);
		//lvar.modify();
		//code.movl(value,Ref.ebp.disp(varOffset));
	    }
	} else {	    
	    operant.translate(result);

	    if (datatype==BCBasicDatatype.REFERENCE)
		execEnv.codeCheckMagic(this,result,bcPosition);

	    code.startBC(bcPosition);

	    if (result.any()) {
		if (verbose && System.err!=null) System.err.println("operant: "+operant.toReadableString());
		throw new Error("can`t write to any (bcPos:"+bcPosition+")");
	    }

	    regs.writeIntRegisterToSlot(result,lvar);
	}

	code.endBC();
    } 

    public void translate(Reg64 result) throws CompileException {
	int varOffset = frame.getOffset(lvar);
	operant.translate(result);
	code.startBC(bcPosition);
	regs.writeLongRegisterToSlot(result,lvar);
	code.endBC();
    } 

    public IMReadBlockVariable getReadOperation() {
	//if (container==null) System.err.println("container == null");
    	return new IMReadBlockVariable(container,this,number,datatype,bcPosition);
    }
    
    public String toReadableString() {
	return "b"+Integer.toString(number)+" = "+operant.toReadableString();
    }    

    public String debugInfo() {
	return debugTxt+" "+operant.debugInfo();
    }
}
