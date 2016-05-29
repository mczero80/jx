
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
// ***** IMCompare *****

final  public class IMCompare extends IMBinaryOperator {

    public IMCompare(CodeContainer container,int bc,int bcpos) {	
	super(container);
	bytecode   = bc;
	bcPosition = bcpos;
	datatype = BCBasicDatatype.INT;
    }

    public IMNode constant_folding() throws CompileException{
	if (rOpr.isOperator()) {
	    rOpr = (IMOperant)((IMOperator)rOpr).constant_folding();
	}
	if (lOpr.isOperator()) {
	    lOpr = (IMOperant)((IMOperator)lOpr).constant_folding();
	}
	if (rOpr.isConstant() && lOpr.isConstant()) {
	    if (opts.doVerbose("cf")) Debug.out.println("-- no folding "+toReadableString());
	}
	return this;
    }

    public String toReadableString() {
	return "compare("+lOpr.toReadableString()+","+rOpr.toReadableString()+")";
    }

    // IMCompare for long, float and double read spec
    public void translate(Reg result) throws CompileException {	
	if (opts.isOption("long") && bytecode==BC.LCMP) {
	    execEnv.codeLongCompare(this,lOpr,rOpr,result,bcPosition);
	    return ;
	}
	execEnv.codeThrow(this,-11,bcPosition);
    }

    public void translate(Reg64 result) throws CompileException {
	throw new CompileException("long not supported by compare");
    }
}
