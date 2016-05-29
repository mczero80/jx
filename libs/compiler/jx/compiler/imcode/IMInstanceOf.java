
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
// **** IMInstanceOf *****

final public class IMInstanceOf extends IMUnaryOperator {

    private ClassCPEntry cpEntry;

    public IMInstanceOf(CodeContainer container,int bc,int bcpos,ClassCPEntry cpEntry) {
	super(container);
	bytecode     = bc;
	bcPosition   = bcpos;
	datatype     = BCBasicDatatype.INT; // Array
	this.cpEntry = cpEntry;
    }
    
    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) throws CompileException {
        operant = stack.pop();
	if (operant.getDatatype()!=BCBasicDatatype.REFERENCE) {
	    throw new CompileException("IMInstanceOf bcpos:"+Integer.toString(bcPosition)+" wrong datatype on stack!");
	}
	stack.push(this);
	return null;
    }

    public String toReadableString() {
	return "("+operant.toReadableString()+" instanceof "+cpEntry.getClassName()+")";
    }

    // IMInstanceOf
    public void translate(Reg result) throws CompileException {

	Reg refObj = regs.chooseIntRegister(null);
	operant.translate(refObj);
	regs.freeIntRegister(refObj); 

	// execEnv.codeCheckReference(...); done by jxcore
        execEnv.codeInstanceOf(this,cpEntry,refObj,result,bcPosition);
    }
}
