
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
// **** IMNew ****

final public class IMNew extends IMOperator {

    private ClassCPEntry cpEntry;

    public IMNew (CodeContainer container,int bc,int bcpos,ClassCPEntry cpEntry) {
	super(container);
	bytecode     = bc;
	bcPosition   = bcpos;
	datatype     = BCBasicDatatype.REFERENCE;
	this.cpEntry = cpEntry;
    }

    public ClassCPEntry getClassCPEntry() {
	return cpEntry;
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) throws CompileException {
	stack.store(bcPosition);
	stack.push(this);
	saveVarStackMap(frame);
	return null;
    }
    
    public String toReadableString() {
	return "new "+cpEntry.getClassName();
    }

    // IMNew
    public void translate(Reg result) throws CompileException {
	execEnv.codeNewObject(this,cpEntry,result);
    }
}
