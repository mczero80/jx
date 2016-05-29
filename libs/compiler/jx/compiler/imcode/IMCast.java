
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
// ***** IMCast *****

final  public class IMCast extends IMUnaryOperator {

    private int shortOpr;

    public IMCast(CodeContainer container,int bc,int bcpos) {	
	super(container);
	bytecode   = bc;
	bcPosition = bcpos;
	shortOpr   = bc-BC.I2L;
	switch (shortOpr) {
	    // i2<x>
	case 0: datatype = BCBasicDatatype.LONG;break;
	case 1: datatype = BCBasicDatatype.FLOAT;break;
	case 2: datatype = BCBasicDatatype.DOUBLE;break;
	    // l2<x>
	case 3: datatype = BCBasicDatatype.INT;break;
	case 4: datatype = BCBasicDatatype.FLOAT;break;
	case 5: datatype = BCBasicDatatype.DOUBLE;break;
	    // f2<x>
	case 6: datatype = BCBasicDatatype.INT;break;
	case 7: datatype = BCBasicDatatype.LONG;break;
	case 8: datatype = BCBasicDatatype.DOUBLE;break;
	    // d2<x>
	case 9: datatype = BCBasicDatatype.INT;break;
	case 10: datatype = BCBasicDatatype.LONG;break;
	case 11: datatype = BCBasicDatatype.FLOAT;break;
	    // i2<x>
	case 12: datatype = BCBasicDatatype.BYTE;break;
	case 13: datatype = BCBasicDatatype.CHAR;break;
	case 14: datatype = BCBasicDatatype.SHORT;break;
	}
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) {
	operant = stack.pop();
	stack.push(this);
	return null;
    }

    public String toReadableString() {
	switch (datatype) {
	case 0: return "(int)"+operant.toReadableString();
	case 1: return "(long)"+operant.toReadableString();
	case 2: return "(float)"+operant.toReadableString();
	case 3: return "(double)"+operant.toReadableString();
	case 5: return "(byte)"+operant.toReadableString();
	case 6: return "(char)"+operant.toReadableString();
	case 7: return "(short)"+operant.toReadableString();
	default: return "<error>";
	}
    }

    // IMCast
    public void translate(Reg result) throws CompileException {
	switch (shortOpr) {
	    // i2<x>
	case 0: // LONG
	case 1: // FLOAT
	case 2: // DOUBLE
	    execEnv.codeThrow(this,-11,bcPosition);
	    //Debug.out.println(toReadableString());
	    //throw new CompileException(getLineInfo()+": wrong cast called!");
	    // l2<x>
	case 3: // l2i
	    if (!opts.isOption("long")) {
		Debug.out.println("warn: long cast to int not supported use -x:long");
		regs.allocIntRegister(result,BCBasicDatatype.INT);
		code.xorl(result,result);
		execEnv.codeThrow(this,-11,bcPosition);
		return;
	    }

	    if (result.any()) { regs.setAnyIntRegister(result,Reg.eax); }

	    Reg64 reg64 = Reg64.extendLowRegister(result);

	    if (reg64!=null) {
		operant.translate(reg64);
		regs.freeIntRegister(reg64.high);
		/* result == reg64.low */
	    } else {
		reg64 = regs.chooseLongRegister();
		operant.translate(reg64);
		regs.freeIntRegister(reg64.high);
		regs.allocIntRegister(result,BCBasicDatatype.INT);
		code.movl(reg64.low,result);
		regs.freeIntRegister(reg64.low);
	    }
	    break;
	case 6: // f2i 
	case 9: // d2i
	    execEnv.codeThrow(this,-11,bcPosition);
	    //throw new CompileException("cast d2i and f2i -- not implemented yet!");
	    // i2<x>
	case 12: // i2b
	    if (result.value!=0) {
		Reg regEAX = regs.getIntRegister(Reg.eax);
		operant.translate(regEAX);
		code.cbw();
		code.cwde();
		regs.allocIntRegister(result,BCBasicDatatype.INT);
		code.movl(regEAX,result);
		regs.freeIntRegister(regEAX);
	    } else {
		operant.translate(result);
		code.cbw();
		code.cwde();
	    }	    
	    break;
	case 13: // i2c
	    operant.translate(result);
	    regs.writeIntRegister(result);
	    code.andl(0x0000ffff,result);
	    break;
	case 14: // i2s
	    if (result.value!=0) {
		Reg regEAX = regs.getIntRegister(Reg.eax);
		operant.translate(regEAX);
		code.cwde();
		regs.allocIntRegister(result,BCBasicDatatype.INT);
		code.movl(regEAX,result);
		regs.freeIntRegister(regEAX);
	    } else {
		operant.translate(result);
		code.cwde();
	    }
	    break;
	}
    }

    // IMCast
    public void translate(Reg64 result) throws CompileException {
 	switch (shortOpr) {
	case 0: // i2l
	    if (!opts.isOption("long")) {
		Debug.out.println("warn: int cast to long not supported use -x:long");
		execEnv.codeThrow(this,-11,bcPosition);
		return;
	    }	    
 	    if (result.equals(Reg64.eax)) {
		operant.translate(result.low);
		regs.allocIntRegister(result.high,BCBasicDatatype.INT);
		code.cdq();
	    } else {
		Reg64 tmp = regs.getLongRegister(Reg64.eax);
		operant.translate(tmp.low);

		regs.allocIntRegister(tmp.high,BCBasicDatatype.INT);
		code.cdq();

		regs.allocLongRegister(result);
		code.movl(tmp.low,result.low);
		code.movl(tmp.high,result.high);

		regs.freeLongRegister(tmp);
	    }
	    break;
	case 7: // f2l
	    execEnv.codeThrow(this,-11,bcPosition);
	    break;
	case 10: //d2l
	    execEnv.codeThrow(this,-11,bcPosition);
	    break;
	default:
	    throw new CompileException(getLineInfo()+": unknown cast!");
	}
    }
}
