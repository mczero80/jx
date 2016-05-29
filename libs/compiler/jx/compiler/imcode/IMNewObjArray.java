
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
// **** IMNewObjArray *****

final public class IMNewObjArray extends IMOperator {

    private IMOperant size;
    private ClassCPEntry cpEntry;
    
    public IMNewObjArray(CodeContainer container,int bc,int bcpos,ClassCPEntry cpEntry) {
	super(container);
	bytecode     = bc;
	bcPosition   = bcpos;
	datatype     = BCBasicDatatype.REFERENCE; // FIXME Array
	this.cpEntry = cpEntry;
    }
    
    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) throws CompileException {
	size = stack.pop();
	if (size.getDatatype()!=BCBasicDatatype.INT) {
	    if (verbose && System.err!=null) System.err.println("IMNewObjArray bcpos:"+Integer.toString(bcPosition));
	    throw new CompileException("!!! wrong datatype on stack !!!");
	}
	stack.store(bcPosition);
	stack.push(this);
	saveVarStackMap(frame);
	return null;
    }

    public IMNode inlineCode(CodeVector iCode,int depth, boolean forceInline) throws CompileException {
	size = (IMOperant)size.inlineCode(iCode, depth, forceInline);
	return this;
    }

    public void getCollectVars(Vector vars) { size.getCollectVars(vars); }

    public IMNode constant_folding() throws CompileException{
	size = (IMOperant)size.constant_folding();
	return this;
    }

    public IMNode assignNewVars(CodeContainer newContainer,int slots[],IMOperant opr[],int retval,int bcPos) throws CompileException {
	bcPosition = bcPos;
	init(newContainer);

	size = (IMOperant)size.assignNewVars(newContainer,slots,opr,retval,bcPos);

	return this;
    }

    public String toReadableString() {
	return "new "+cpEntry.getClassName()+"()["+size.toReadableString()+"]";
    }

    // IMNewObjArray
    public void translate(Reg result) throws CompileException {
	execEnv.codeNewObjectArray(this,cpEntry,size,result);
    }
}
