
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
// **** IMNewMultiArray ****

final public class IMNewMultiArray extends IMOperator {

    private ClassCPEntry cpEntry;
    private IMOperant oprs[];
    private int dim;

    public IMNewMultiArray(CodeContainer container,int bc,int bcpos,ClassCPEntry cpEntry,int dim) {
	super(container);
	bytecode     = bc;
	bcPosition   = bcpos;
	datatype     = BCBasicDatatype.REFERENCE;
	this.cpEntry = cpEntry;
	this.dim     = dim;
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) throws CompileException {
	oprs = new IMOperant[dim];
	for (int i=0;i<dim;i++) {
	    oprs[i]=stack.pop();
	    if (oprs[i].getDatatype()!=BCBasicDatatype.INT) {
		throw new CompileException("IMNewMultiArray bcpos:"+Integer.toString(bcPosition)+" wrong datatype on stack!");
	    }
	}
	stack.store(bcPosition);
	stack.push(this);
	return null;
    }

    public IMNode constant_folding() throws CompileException{
	for (int i=0;i<oprs.length;i++) {
	    oprs[i] = (IMOperant)oprs[i].constant_folding();
	}
	return this;
    }

    public void getCollectVars(Vector vars) { 
	for (int i=0;i<oprs.length;i++) {
	    oprs[i].getCollectVars(vars); 		
	}
    }

    public IMNode assignNewVars(CodeContainer newContainer,int slots[],IMOperant opr[],int retval,int bcPos) throws CompileException {
	bcPosition = bcPos;
	init(newContainer);

	for (int i=0;i<oprs.length;i++) {
	    oprs[i] = (IMOperant)oprs[i].assignNewVars(newContainer,slots,opr,retval,bcPos);
	}

	return this;
    }

    public String toReadableString() {
	String retValue =  "new "+cpEntry.getClassName();
	for (int i=0;i<oprs.length;i++) {
	    retValue += "[" + oprs[i].toReadableString() +"]";
	}
	return retValue;
    }

    // IMNewMultiArray
    public void translate(Reg result) throws CompileException {
	execEnv.codeNewMultiArray(this,cpEntry,oprs,result);
    }
}
