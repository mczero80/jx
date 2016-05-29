
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
// ***** IMMultiOperant *****

public class IMMultiOperant extends IMOperant {

    protected IMOperant            obj;
    protected IMOperant[]          args;

    public IMMultiOperant(CodeContainer container) {
	super(container);
	obj=null;
    }

    public String debugInfo() {
	if (obj!=null) return debugTxt+" "+obj.debugInfo();
	else return debugTxt;
    }

    public IMNode constant_forwarding(IMNodeList varList) throws CompileException {
	if (obj!=null) {
	    obj = (IMOperant)obj.constant_forwarding(varList);
	}

	for (int i=0;i<args.length;i++) {
	    args[i] = (IMOperant)args[i].constant_forwarding(varList);;
	}
       
	return this;
    }

    public IMNode constant_folding() throws CompileException{
	if (obj!=null) {
	    obj = (IMOperant)obj.constant_folding();
	}

	for (int i=0;i<args.length;i++) {
	    args[i] = (IMOperant)args[i].constant_folding();
	}
	
	return this;
    }

    public IMNode assignNewVars(CodeContainer newContainer,int slots[],IMOperant opr[],int retval,int bcPos) throws CompileException {
	bcPosition = bcPos;
	init(newContainer);

	if (obj!=null) {
	    obj = (IMOperant)obj.assignNewVars(newContainer,slots,opr,retval,bcPos);
	}

	for (int i=0;i<args.length;i++) {
	    args[i] = (IMOperant)args[i].assignNewVars(newContainer,slots,opr,retval,bcPos);
	}
	
	return this;
    }

    public void getCollectVars(Vector vars) {	
	if (obj!=null) obj.getCollectVars(vars);
	for (int i=0;i<args.length;i++) args[i].getCollectVars(vars);
    }

    public int getNrRegs() { 
	int sum=0;
	if (obj!=null) sum=obj.getNrRegs();
	for (int i=0;i<args.length;i++) sum += args[i].getNrRegs();
	return sum;
    }

    public String toReadableString() {
	String retString = "(";
	int i=0;
	while (i<args.length)  {
	    retString += args[i].toReadableString();
	    i++;
	    if (i<args.length) retString+=",";
	}
	return retString+")";
    }
}
