
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
// **** IMArrayLength ****

final public class IMArrayLength extends IMOperator {

    private IMOperant array;

    public IMArrayLength(CodeContainer container,int bc,int bcpos) {
	super(container);
	bytecode     = bc;
	bcPosition   = bcpos;
	datatype     = BCBasicDatatype.INT;
    }
    
    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) throws CompileException {
        array = stack.pop();
	if (array.getDatatype()!=BCBasicDatatype.REFERENCE) 
	    throw new CompileException("wrong datatype "+Integer.toString(array.getDatatype()));
	//stack.store();
	stack.push(this);
	return null;
    }

    public IMNode inlineCode(CodeVector iCode,int depth, boolean forceInline) throws CompileException {
	array = (IMOperant)array.inlineCode(iCode, depth, forceInline);
	return this;
    }

    public IMNode constant_folding() throws CompileException{
	array = (IMOperant)array.constant_folding();
	return this;
    }

    public IMNode assignNewVars(CodeContainer newContainer,int slots[],IMOperant opr[],int retval,int bcPos) throws CompileException {
	bcPosition = bcPos;
	init(newContainer);

	array = (IMOperant)array.assignNewVars(newContainer,slots,opr,retval,bcPos);

	return this;
    }
    
    public String toReadableString() {
	return array.toReadableString()+".length";
    }

    public int getNrRegs() { return array.getNrRegs()+1; }

    public void getCollectVars(Vector vars) { array.getCollectVars(vars); }

    // IMArrayLength
    public void translate(Reg result) throws CompileException {
	
	Reg arrayRef = regs.chooseIntRegister(result);	
	array.translate(arrayRef);

	if (array.checkReference())
	    execEnv.codeCheckReference(this,arrayRef,bcPosition);

	execEnv.codeGetArrayLength(this,arrayRef,result);

	regs.freeIntRegister(arrayRef);
    }
}
