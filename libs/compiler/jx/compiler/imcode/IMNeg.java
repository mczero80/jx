
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
// ***** IMNeg *****

final  public class IMNeg extends IMUnaryOperator {

    public IMNeg(CodeContainer container,int bc,int bcpos) {
	super(container);
	bytecode   = bc;
	bcPosition = bcpos;
	datatype = BCBasicDatatype.INT + (bc-BC.INEG);
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) {
	operant = stack.pop();
	stack.push(this);
	return null;
    }

    public IMNode constant_folding() throws CompileException {
	int       value = 0;

	super.constant_folding();
	
	if (datatype == BCBasicDatatype.INT) {
	    // simpel case -(c) => -c
	    if (operant.isConstant()) {
		if (opts.doVerbose("cf")) Debug.out.println("++ folding -(c) "+toReadableString());
		IMConstant rcOpr = operant.nodeToConstant();
		rcOpr.setIntValue(-rcOpr.getIntValue());
		return rcOpr;
	    }
	}
	
	return this;
    }

    public String toReadableString() {
	return "-"+operant.toReadableString();
    }

    // IMNeg
    public void translate(Reg result) throws CompileException {	
	operant.translate(result);
	code.startBC(bcPosition);
	regs.writeIntRegister(result);
	code.negl(result);
	code.endBC();
    }

    public void translate(Reg64 result) throws CompileException {	
	operant.translate(result);
	code.startBC(bcPosition);

	regs.writeLongRegister(result);

	code.negl(result.low);
	code.adcl(0,result.high);
	code.negl(result.high);

	code.endBC();
    }
}
