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

// ***** IMGetField *****

final public class IMGetField extends IMUnaryOperator {

    private FieldRefCPEntry cpEntry;
    private String fieldType;

    public IMGetField(CodeContainer container,int bc,int bcpos,
		      FieldRefCPEntry cpEntry) {
	super(container);
	bytecode     = bc;
	bcPosition   = bcpos;
	this.cpEntry = cpEntry;
	fieldType    = cpEntry.getMemberTypeDesc();
	datatype     = BasicTypeDescriptor.getBasicDatatypeOf(fieldType);
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) throws CompileException {
	operant = stack.pop();
	//stack.store();
	stack.push(this);
	return null;
    }

    public String toReadableString() {
	return operant.toReadableString()+"."+cpEntry.getMemberName();
    } 

    // IMGetField
    public void translate(Reg result) throws CompileException {	
	Reg objRef = regs.chooseIntRegister(result);	
	operant.translate(objRef);
	code.startBC(bcPosition);

	// Example NPA 

	if (false) {
	    /*
	    jx.verifier.npa.NPAResult npaResult 
		= (jx.verifier.npa.NPAResult)container.getBCMethod().getVerifyResult(jx.verifier.npa.VerifyResult.NPA_RESULT);	    
	    if (!(npaResult!=null && npaResult.notNull(bcPosition))) {
		if (operant.checkReference()) 
		    execEnv.codeCheckReference(this,objRef,bcPosition);
	    }
	    */
	    
	} else {
	    if (operant.checkReference()) 
		execEnv.codeCheckReference(this,objRef,bcPosition);
	}

	execEnv.codeGetField(this,cpEntry,objRef,result,bcPosition);
	code.endBC();
	regs.freeIntRegister(objRef);			     
    }

    public void translate(Reg64 result) throws CompileException {	
	if (datatype!=BCBasicDatatype.LONG) throw new CompileException("wrong datatype");

	Reg objRef = regs.chooseIntRegister(result.low,result.high);	
	operant.translate(objRef);
	code.startBC(bcPosition);

	if (operant.checkReference()) 
		execEnv.codeCheckReference(this,objRef,bcPosition);

	execEnv.codeGetFieldLong(this,cpEntry,objRef,result,bcPosition);
	code.endBC();

	regs.freeIntRegister(objRef);			     
    }
}
