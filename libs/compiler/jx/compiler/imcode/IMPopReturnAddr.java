
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
// ***** IMPopReturnAddr *****

final public class IMPopReturnAddr extends IMOperant {

    public IMPopReturnAddr(CodeContainer container) {
	super(container);
	datatype = -2;
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) {
	stack.push(this);
	return null;
    }

    public String toReadableString() {
	return "return_addr";
    }

    public int getNrRegs() { return 1; }

    // IMPopReturnAddr
    public void translate(Reg result) throws CompileException {
	regs.allocIntRegister(result,datatype);
	code.popl(result);
    }
}
