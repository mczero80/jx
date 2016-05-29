
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
// ***** IMReadBlockVariable *****

final public class IMReadBlockVariable extends IMVarAccess {

    private int number;
    private LocalVariable lvar;
    private Object refCheckMarke;

    public IMReadBlockVariable(CodeContainer container,Object marke,int number,int datatype,int bcPosition) {
	super(container);
	tag |= IMNode.BLOCKVAR;
	this.number     = number;
	this.datatype   = datatype;
	this.bcPosition = bcPosition;
	this.lvar  = frame.getBlockSlot(datatype,number);	
	this.refCheckMarke = marke;
    }

    public int getBlockVarIndex() {
	return number;
    }

    public String toReadableString() {
	return "b"+Integer.toString(number);
    }

    public boolean checkReference() {

	if (!execEnv.doOptimize(1)) return true;
	if (lvar.isChecked(refCheckMarke)==true) return false;
	lvar.check(refCheckMarke);

	return true;
    }

    public int getNrRegs() { return 1; }

    // IMReadBlockVariable
    public void translate(Reg result) throws CompileException {
	code.startBC(bcPosition);
	regs.readIntRegisterFromSlot(lvar,result,datatype);
	if (datatype==BCBasicDatatype.REFERENCE)
	    execEnv.codeCheckMagic(this,result,bcPosition);
	code.endBC();
    }

    public void translate(Reg64 result) throws CompileException {
	code.startBC(bcPosition);
	regs.readLongRegisterFromSlot(lvar,result);
	code.endBC();
    }
}
