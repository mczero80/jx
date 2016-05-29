
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
// **** IMNewArray ****

final public class IMNewArray extends IMOperator {

    private int atype;
    private IMOperant size;
    private String stackMap="[]";

    public IMNewArray (CodeContainer container,int bc,int bcpos,int atype) {
	super(container);
	bytecode     = bc;
	bcPosition   = bcpos;
	datatype     = BCBasicDatatype.REFERENCE; // FIXME Array !?!
	this.atype   = atype;
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) throws CompileException {
	// TODO new local variable allocation ??
	size = stack.pop();
	if (size.getDatatype()!=BCBasicDatatype.INT) {
	    if (verbose && System.err!=null) System.err.println("IMNewArray bcpos:"+Integer.toString(bcPosition));
	    throw new CompileException("!!! wrong datatype on stack !!!");	    
	    //System.exit(-1);
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

    public IMNode constant_folding() throws CompileException{
	size = (IMOperant)size.constant_folding();
	return this;
    }

    public void getCollectVars(Vector vars) { size.getCollectVars(vars); }

    public IMNode assignNewVars(CodeContainer newContainer,int slots[],IMOperant opr[],int retval,int bcPos) throws CompileException {
	bcPosition = bcPos;
	init(newContainer);

	size = (IMOperant)size.assignNewVars(newContainer,slots,opr,retval,bcPos);

	return this;
    }

    public String toReadableString() {
	return "new "+BCBasicDatatype.toString(atype)+"["+size.toReadableString()+"]";
    }

    // IMNewArray
    public void translate(Reg result) throws CompileException {
	execEnv.codeNewArray(this,atype,size,result);
    }
}
