
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
// **** IMCheckCast ****

final public class IMCheckCast extends IMUnaryOperator {

    private ClassCPEntry cpEntry;

    public IMCheckCast(CodeContainer container,int bc,int bcpos,ClassCPEntry cpEntry) {
	super(container);
	bytecode     = bc;
	bcPosition   = bcpos;
	datatype     = BCBasicDatatype.REFERENCE;
	this.cpEntry = cpEntry;
    }
    
    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) throws CompileException {
        operant = stack.pop();
	if (operant.getDatatype()!=BCBasicDatatype.REFERENCE) {
	    if (verbose && System.err!=null) {
		System.err.println("IMCheckCast bcpos:"+Integer.toString(bcPosition));
		System.err.println("!!! wrong datatype on stack !!!");
	    }
	    System.exit(-1);
	}
	stack.push(this);
	return null;
    }

    public String toReadableString() {
	return "("+cpEntry.getClassName()+")("+operant.toReadableString()+")";
    }

    // IMCheckCast
    public void translate(Reg result) throws CompileException {	
	operant.translate(result);
	execEnv.codeCheckCast(this,cpEntry,result,bcPosition);
    }
}
