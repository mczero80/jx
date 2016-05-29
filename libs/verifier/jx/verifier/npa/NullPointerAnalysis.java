package jx.verifier.npa;

import jx.classfile.*;
import jx.classfile.constantpool.*;
import jx.classstore.ClassFinder;
import jx.verifier.bytecode.*;
import jx.verifier.*;

public class NullPointerAnalysis {
    static public void verifyMethod(MethodSource method, 
			      String className, 
			      ConstantPool cPool) 
	throws VerifyException {
	MethodVerifier m = new MethodVerifier(method,
					      className,
					      cPool);
	m.setInitialState(new NPAState(m.getCode().getFirst(), 
				       m.getMethod().getNumLocalVariables(),
				       m.getMethod().getNumStackSlots(),
				       m));
	
	m.runChecks();

	NPAResult result = new NPAResult(m.getMethod().getNumInstr() /16 ); //initial size: 1/16 of bytecodelength
	int totalChecks=0, val = -1;;
	for(ByteCode actBC = m.getCode().getFirst();
	    actBC != null;
	    actBC = actBC.next) {
	    val = -1;

	    switch(actBC.getOpCode()) {
	    case ByteCode.IALOAD:
	    case ByteCode.LALOAD:
	    case ByteCode.FALOAD:
	    case ByteCode.DALOAD:
	    case ByteCode.AALOAD:
	    case ByteCode.BALOAD:
	    case ByteCode.CALOAD:
	    case ByteCode.SALOAD:
		totalChecks++;
		if (actBC.beforeState == null) {val = -2; break;}
		val = ((NPAState)actBC.beforeState).NPAgetStack().NPApeek(1).getValue();
		break;
	    case ByteCode.IASTORE:
	    case ByteCode.FASTORE:
	    case ByteCode.AASTORE:
	    case ByteCode.BASTORE:
	    case ByteCode.CASTORE:
	    case ByteCode.SASTORE:
		totalChecks++;
		if (actBC.beforeState == null) {val = -2; break;}
		val = ((NPAState)actBC.beforeState).NPAgetStack().NPApeek(2).getValue();
		break;
	    case ByteCode.LASTORE:
	    case ByteCode.DASTORE:
		totalChecks++;
		if (actBC.beforeState == null) {val = -2; break;}
		val = ((NPAState)actBC.beforeState).NPAgetStack().NPApeek(3).getValue();
		break;
	    case ByteCode.GETFIELD:
		totalChecks++;
		if (actBC.beforeState == null) {val = -2; break;}
		val = ((NPAState)actBC.beforeState).NPAgetStack().NPApeek().getValue();
		break;
	    case ByteCode.PUTFIELD:
		totalChecks++;
		BCCPArgOp bc = (BCCPArgOp)actBC;
		int cPoolIndex = ((bc.getByteArgs()[0] <<8)&0xff00) |
		    (((int) bc.getByteArgs()[1])&0xff);
		
		String typeDesc = bc.getCP().
		    fieldRefEntryAt(cPoolIndex).getMemberTypeDesc();
		int count = NPAValue.typeFromTypeDesc(typeDesc).length;
		if (actBC.beforeState == null) {val = -2; break;}
		val = ((NPAState)actBC.beforeState).NPAgetStack().NPApeek(count).getValue();
		break;
	    case ByteCode.INVOKESPECIAL:
	    case ByteCode.INVOKEINTERFACE:
	    case ByteCode.INVOKEVIRTUAL:
		totalChecks++;
		BCCPArgOp bCode = (BCCPArgOp) actBC;
		int opCode = bCode.getOpCode();
		cPoolIndex = (bCode.getByteArgs()[0]<<8)|
		    (((int)bCode.getByteArgs()[1])&0xff);
		//get typedesc for method
		if (opCode !=ByteCode.INVOKEINTERFACE) {
		    typeDesc = bCode.getCP().methodRefEntryAt(cPoolIndex).getMemberTypeDesc();
		} else { //invokeinterface
		    typeDesc = bCode.getCP().InterfaceMethodRefEntryAt(cPoolIndex).
			getMemberTypeDesc();
		}
		NPAValue[] args = NPAValue.argTypeFromMethod(typeDesc);
		
		if (actBC.beforeState == null) {val = -2; break;}
		val = ((NPAState)actBC.beforeState).NPAgetStack().NPApeek(args.length).getValue();
		break;
	    case ByteCode.ARRAYLENGTH:
		totalChecks++;
		if (actBC.beforeState == null) {val = -2; break;}
		val = ((NPAState)actBC.beforeState).NPAgetStack().NPApeek().getValue();
		break;
	    case ByteCode.ATHROW:
		totalChecks++;
		if (actBC.beforeState == null) {val = -2; break;}
		val = ((NPAState)actBC.beforeState).NPAgetStack().NPApeek().getValue();
		break;
	    case ByteCode.MONITORENTER:
		totalChecks++;
		if (actBC.beforeState == null) {val = -2; break;}
		val = ((NPAState)actBC.beforeState).NPAgetStack().NPApeek().getValue();
		break;
	    case ByteCode.MONITOREXIT:
		totalChecks++;
		if (actBC.beforeState == null) {val = -2; break;}
		val = ((NPAState)actBC.beforeState).NPAgetStack().NPApeek().getValue();
	    break;
	    default:
		val = -1;
	    }
	    if (val == NPAValue.NONNULL)
		result.setNotNull(actBC.getAddress());
	    else if (val == NPAValue.NULL)
		Verifier.stdPrintln(1,"Warning - op on null reference: " + actBC);
	}
	method.setVerifyResult(result);

	if (Verifier.debugMode >1) {
	    if (totalChecks<= 0) {
		System.err.println("no checks");
	    } else {
		int ratio = (result.getCount()*100) / totalChecks;
		System.err.println("checks Removed: " + result.getCount() +"/" + totalChecks +
				   " - " + ratio+"%");
	    }
	    if (totalChecks >0) {
		for (ByteCode actBC = m.getCode().getFirst();
		     actBC != null;
		     actBC = actBC.next) {
		    if (result.notNull(actBC.getAddress())) 
			System.out.print("**** ");
		    else 
			System.out.print("     ");
		    System.out.println(actBC);
		    
		}
		System.out.println("Checks removed: " + result);
	    }
	}
    }
    
    
    public void test(ByteCode te) {
	
	System.out.println(te.next);
	System.out.println(te.prev);
    }
}
