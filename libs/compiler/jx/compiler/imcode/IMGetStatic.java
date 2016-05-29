
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
// ***** IMGetStatic *****

final public class IMGetStatic extends IMOperant {

    private FieldRefCPEntry cpEntry;
    private String fieldType;

    public IMGetStatic(CodeContainer container,int bc,int bcpos,FieldRefCPEntry cpEntry) {
	super(container);
	bytecode     = bc;
	bcPosition   = bcpos;
	this.cpEntry = cpEntry;
	fieldType    = cpEntry.getMemberTypeDesc();
	datatype     = BasicTypeDescriptor.getBasicDatatypeOf(fieldType);
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) throws CompileException {
        //stack.store();
	stack.push(this);
	return null;
    }

    public String toReadableString() {
	return cpEntry.getMemberName();
    }

    public int getNrRegs() { return 1; }
    
    // IMGetStatic
    public void translate(Reg result) throws CompileException {
	code.startBC(bcPosition);
	execEnv.codeGetStaticField(this,cpEntry,result,bcPosition);
	code.endBC();
    }
    
    public void translate(Reg64 result) throws CompileException {
	    if (datatype!=BCBasicDatatype.LONG) throw new CompileException("wrong datatype");
	    code.startBC(bcPosition);
	    execEnv.codeGetStaticFieldLong(this,cpEntry,result,bcPosition);
	    code.endBC();
    }
}
