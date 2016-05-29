
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
// ***** IMVarAccess ****

public class IMVarAccess extends IMOperator implements IMVarAccessInterface {

    protected int           ivar;
    protected LocalVariable lvar;
    protected boolean       writeAccess=false; 

    public IMVarAccess(CodeContainer container) {super(container);}

    public LocalVariable getLocalVariable() {
	return lvar;
    }

    public int getVarIndex() {
	return ivar;
    }
    
    public int getNrRegs() { return 1; }

    public void getCollectVars(Vector vars) { vars.addElement(this); return; }

    public String toSymbolname() {
	return BCBasicDatatype.toSymbol(datatype)+Integer.toString(ivar);
    }

    public boolean writeAccess() {return writeAccess;}
}
