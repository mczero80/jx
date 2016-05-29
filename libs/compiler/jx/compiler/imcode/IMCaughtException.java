
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
// ***** IMCaughtException *****

final public class IMCaughtException extends IMOperant {

    public IMCaughtException(CodeContainer container) {
	super(container);
	datatype = BCBasicDatatype.REFERENCE;
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) {
	stack.push(this);
	return null;
    }

    public String toReadableString() {
	return "<caught exception>";
    }

    public int getNrRegs() { return 1; }

    // IMCaughtException
    public void translate(Reg result) throws CompileException {
	regs.allocIntRegister(result,datatype);
	code.popl(result);
    }
}
