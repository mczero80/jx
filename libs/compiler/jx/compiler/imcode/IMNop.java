
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
// ***** IMNop ***** 

public class IMNop extends IMOperant {

    public IMNop(CodeContainer container,int bcpos) {
	super(container);
	bcPosition = bcpos;
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) {
	if (verbose && System.err!=null) System.err.println("!! NOP");
	return this;
    }


    public IMNode inlineCode(CodeVector iCode,int depth, boolean forceInline) throws CompileException {	
	return this;
    }
    
    public void translate(Reg result) throws CompileException {	
	code.nop();
    }     
}
