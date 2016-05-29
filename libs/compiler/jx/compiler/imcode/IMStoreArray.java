
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
// ***** IMStoreArray *****

final public class IMStoreArray extends IMOperant  {

    private IMOperant iOpr;
    private IMOperant aOpr;
    private IMOperant rvalue;

    public IMStoreArray(CodeContainer container,int bc,int bcpos) {
	super(container);
	bytecode    = bc;
	bcPosition  = bcpos;
	datatype    = BCBasicDatatype.INT + (bc-BC.IASTORE);
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) {
	//stack.store(bcPosition);
	rvalue = stack.pop();
	iOpr   = stack.pop();
	aOpr   = stack.pop();
	//stack.store();
	return this;
    }

    public IMNode inlineCode(CodeVector iCode,int depth, boolean forceInline) throws CompileException {
	aOpr   = (IMOperant)aOpr.inlineCode(iCode, depth, forceInline);
	iOpr   = (IMOperant)iOpr.inlineCode(iCode, depth, forceInline);
	rvalue = (IMOperant)rvalue.inlineCode(iCode, depth, forceInline);
	return this;
    }

    public IMNode constant_folding() throws CompileException{
	rvalue = (IMOperant)rvalue.constant_folding();
	aOpr   = (IMOperant)aOpr.constant_folding();
	iOpr   = (IMOperant)iOpr.constant_folding();
	return this;
    }

    public IMNode assignNewVars(CodeContainer newContainer,int slots[],IMOperant opr[],int retval,int bcPos) throws CompileException {
	bcPosition = bcPos;
	init(newContainer);

	rvalue = (IMOperant)rvalue.assignNewVars(newContainer,slots,opr,retval,bcPos);
	iOpr   = (IMOperant)iOpr.assignNewVars(newContainer,slots,opr,retval,bcPos);
	aOpr   = (IMOperant)aOpr.assignNewVars(newContainer,slots,opr,retval,bcPos);

	return this;
    }

    public String toReadableString() {
	return aOpr.toReadableString()+"["
	    +iOpr.toReadableString()+"] = "
	    +rvalue.toReadableString();
    }

    public int getNrRegs() { return rvalue.getNrRegs() + aOpr.getNrRegs() + iOpr.getNrRegs(); }

    public void getCollectVars(Vector vars) { 
	rvalue.getCollectVars(vars);
	aOpr.getCollectVars(vars);
	iOpr.getCollectVars(vars);
	return;
    }

    // IMStoreArray
    public void translate(Reg result) throws CompileException {
	rvalue.translate(result);
	
	Reg array = regs.chooseIntRegister(result);
	aOpr.translate(array);

	code.startBC(bcPosition);

	if (aOpr.checkReference())
	    execEnv.codeCheckReference(this,array,bcPosition);

	if (false && iOpr.isConstant() && (((IMConstant)iOpr).getIntValue()<128)) {
	    int index = ((IMConstant)iOpr).getIntValue();
	    if (aOpr.checkArrayRange(index)) 
		execEnv.codeCheckArrayRange(this,array,index,bcPosition);
	    execEnv.codePutArrayField(this,array,datatype,index,result,bcPosition);
	} else {
	    Reg indx  = regs.chooseIntRegister(result,array);
	    iOpr.translate(indx);
	    if (aOpr.checkArrayRange(iOpr)) 
		execEnv.codeCheckArrayRange(this,array,indx,bcPosition);

	    if (datatype==BCBasicDatatype.REFERENCE) 
		execEnv.codeCheckMagic(this,result,bcPosition);

	    execEnv.codePutArrayField(this,array,datatype,indx,result,bcPosition);
	    regs.freeIntRegister(indx);
	}

	code.endBC();

	regs.freeIntRegister(array);
    }
 
    // IMStoreArray
    public void translate(Reg64 result) throws CompileException {
	Debug.out.println("warn: IMStoreArray.tanslateLong not impl");
	/*
	    rvalue.translate(result);

	    Reg array = regs.chooseIntRegister(result);
	    aOpr.translate(array);

	    code.startBC(bcPosition);

	    if (aOpr.checkReference())
		    execEnv.codeCheckReference(this,array,bcPosition);

	    Reg indx  = regs.chooseIntRegister(result,array);
	    iOpr.translate(indx);
	    if (aOpr.checkArrayRange(iOpr)) 
		    execEnv.codeCheckArrayRange(this,array,indx,bcPosition);

	    if (datatype==BCBasicDatatype.REFERENCE) 
		    execEnv.codeCheckMagic(this,result,bcPosition);

	    execEnv.codePutArrayField(this,array,datatype,indx,result,bcPosition);
	    regs.freeIntRegister(indx);

	    code.endBC();

	    regs.freeIntRegister(array);
	 */
    } 

}
