
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
// ***** IMInvokeInterface *****

final public class IMInvokeInterface extends IMMultiOperant {

    private InterfaceMethodRefCPEntry cpEntry;
    private MethodTypeDescriptor typeDesc;

    public IMInvokeInterface(CodeContainer container,int bc,int bcpos,
			     InterfaceMethodRefCPEntry cpEntry) {
	super(container);
	bytecode     = bc;
	bcPosition   = bcpos;
	this.cpEntry = cpEntry;
	typeDesc     = new MethodTypeDescriptor(cpEntry.getMemberTypeDesc());
	datatype     = typeDesc.getBasicReturnType();
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) throws CompileException {

	int[] argTypes = typeDesc.getBasicArgumentTypes();

	args = new IMOperant[argTypes.length];
	for (int i=(argTypes.length-1);i>=0;i--) {
	    args[i] = stack.pop();
	}

	obj = stack.pop();

	saveVarStackMap(frame);

	if (datatype==BCBasicDatatype.VOID) {
	    return this;
	} else {
	    stack.push(this);
	    return null;
	}
    }

    public IMNode inlineCode(CodeVector iCode,int depth, boolean forceInline) throws CompileException {
	if (obj!=null) {
	    obj = (IMOperant)obj.inlineCode(iCode, depth, forceInline);
	}

	for (int i=0;i<args.length;i++) {
	    args[i] = (IMOperant)args[i].inlineCode(iCode, depth, forceInline);
	}
	return this;
    }

    public String toReadableString() {
	String retString = "";
	if (obj!=null) retString = obj.toReadableString()+".";
	retString += cpEntry.getMemberName();
	/*
	retString += "(";
	int i=0;
	while (i<args.length)  {
	    retString += args[i].toReadableString();
	    i++;
	   if (i<args.length) retString+=",";
	}
	return retString+")";
	*/
	retString += super.toReadableString();
	return retString;
    }

    // IMInvokeInterface
    public void translate(Reg result) throws CompileException {
	stat.invoke_interface();
	execEnv.codeInterfaceCall(this,cpEntry,obj,args,datatype,result,bcPosition);
    }

    public void translate(Reg64 result) throws CompileException {
	stat.invoke_interface();
	execEnv.codeInterfaceCallLong(this,cpEntry,obj,args,datatype,result,bcPosition);
    }
}
