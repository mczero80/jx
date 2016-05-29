
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
// **** IMCompactNew ****

final public class IMCompactNew extends IMOperator {

    private ClassCPEntry         cpEntry;
    private MethodRefCPEntry     cpEntryMethod;
    private IMOperant[]          args;

    public IMCompactNew(CodeContainer container,int bc,IMNew newObj,IMInvoke initObj,IMOperant[] args) {
	super(container);
	bytecode     = -1;
	datatype     = BCBasicDatatype.REFERENCE;

	this.cpEntry       = newObj.getClassCPEntry();

	bcPosition         = initObj.getBCPosition();
	this.cpEntryMethod = initObj.getMethodRefCPEntry();
	this.args          = args;

	varStackMap = initObj.getVarStackMap();
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) throws CompileException {
	throw new Error("this method is not used!");
    }

    public IMNode constant_folding() throws CompileException{

	for (int i=0;i<args.length;i++) {
	    args[i] = (IMOperant)args[i].constant_folding();
	}
	
	return this;
    }

    public IMNode assignNewVars(CodeContainer newContainer,int slots[],IMOperant opr[],int retval,int bcPos) throws CompileException {
	bcPosition = bcPos;
	init(newContainer);

	for (int i=0;i<args.length;i++) {
	    args[i] = (IMOperant)args[i].assignNewVars(newContainer,slots,opr,retval,bcPos);
	}
	
	return this;
    }

    public IMNode inlineCode(CodeVector iCode,int depth, boolean forceInline) throws CompileException {
	for (int i=0;i<args.length;i++) {
	    args[i] = (IMOperant)args[i].inlineCode(iCode, depth, forceInline);
	}
	return this;
    }

    public void getCollectVars(Vector vars) { 
	for (int i=0;i<args.length;i++) args[i].getCollectVars(vars);
    }

    public String toReadableString() {
	String retString = "new "+cpEntry.getClassName();

	retString += "(";
	int i=0;
	while (i<args.length)  {
	    retString += args[i].toReadableString();
	    i++;
	    if (i<args.length) retString+=",";
	}
	return retString+")";
    }

    // IMCompactNew
    public void translate(Reg result) throws CompileException {	
	execEnv.codeCompactNew(this,cpEntry,cpEntryMethod,args,result);	
    }
}
