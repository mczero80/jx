package jx.verifier.typecheck;

import jx.verifier.bytecode.*;
import jx.verifier.VerifyException;
import jx.verifier.CharIter;
import java.util.Vector;
import jx.classfile.ClassData;
import jx.classstore.ClassFinder;

public class TCTypes {

    //ANY and ANY_REF are used for Bytecodes that operate on values of any type, like pop
    //they must not be pushed onto the stack or saved in the loval variables!!!
    public static final int UNINITIALIZED = 0; //used for local variables that have never been initialized
    public static final int INT = 3;
    public static final int LONG_U = 4; // _U is towards the top of the stack
    public static final int LONG_L = 5;
    public static final int FLOAT = 7;
    public static final int DOUBLE_U = 8;
    public static final int DOUBLE_L = 9;
    public static final int CLASS_REF = 10;
    public static final int INTERFACE_REF = 11;
    public static final int ARRAY_REF = 12;
    public static final int RETURN_ADDR = 13;
    public static final int ANY = 14; //should never be on the Op-stack!
    public static final int ANY_REF = 15; //should never be on the Op-stack!
    public static final int UNKNOWN = 16; //used when merging two local vars of different types

    //the following are only used as base types int arrays! should never be on the stack or in lvars
    public static final int BYTE = 1;
    public static final int SHORT = 2;
    public static final int CHAR = 6;
    
    public static final TCTypes T_UNINITIALIZED = new TCTypes(UNINITIALIZED);
    public static final TCTypes T_BYTE = new TCTypes(BYTE);
    public static final TCTypes T_SHORT = new TCTypes(SHORT);
    public static final TCTypes T_INT = new TCTypes(INT);
    public static final TCTypes T_LONG_U = new TCTypes(LONG_U);
    public static final TCTypes T_LONG_L = new TCTypes(LONG_L);
    public static final TCTypes T_CHAR = new TCTypes(CHAR);
    public static final TCTypes T_FLOAT = new TCTypes(FLOAT);
    public static final TCTypes T_DOUBLE_U = new TCTypes(DOUBLE_U);
    public static final TCTypes T_DOUBLE_L = new TCTypes(DOUBLE_L);
    public static final TCTypes T_CLASS_REF = new TCTypes(CLASS_REF);
    public static final TCTypes T_INTERFACE_REF = new TCTypes(INTERFACE_REF);
    public static final TCTypes T_ARRAY_REF = new TCTypes(ARRAY_REF);
    public static final TCTypes T_RETURN_ADDR  = new TCTypes(RETURN_ADDR);
    public static final TCTypes T_ANY = new TCTypes(ANY); 
    public static final TCTypes T_ANY_REF = new TCTypes(ANY_REF);
    public static final TCTypes T_UNKNOWN = new TCTypes(UNKNOWN);

    public static final TCObjectTypes T_NULL = 
	new TCObjectTypes(TCObjectTypes.nullString);
    public static final TCObjectTypes T_OBJECT = 
	new TCObjectTypes(TCObjectTypes.objectString);

    protected int type;

    public String toString() {
	switch(type) {
	case UNINITIALIZED:
	    return "UNINITIALIZED";
	case BYTE:
	    return "BYTE";
	case SHORT:
	    return "SHORT";
	case INT:
	    return "INT";
	case LONG_U:
	    return "LONG_U";
	case LONG_L:
	    return "LONG_L";
	case CHAR:
	    return "CHAR";
	case FLOAT:
	    return "FLOAT";
	case DOUBLE_U:
	    return "DOUBLE_U";
	case DOUBLE_L:
	    return "DOUBLE_L";
	case CLASS_REF:
	    return "CLASS_REF";
	case INTERFACE_REF:
	    return "INTERFACE_REF";
	case ARRAY_REF:
	    return "ARRAY_REF";
	case RETURN_ADDR:
	    return "RETURN_ADDR";
	case ANY:  
	    return "ANY";
	case ANY_REF: 
	    return "ANY_REF";
	    //	case BOOLEAN:
	    //return "BOOLEAN";
	default:
	    return "Unknown";
	}
    }
    public TCTypes(int type) { this.type = type;}
    public int getType(){return type;}

    public boolean equals(TCTypes other) {
	//only called if "this" is a simple type, so just compare this.type with other.type
	if (other.getType() == this.type) {
	    return true; 
	} else {
	    return false;
	}
    }

    /* FOUND.consistentWith(EXPECTED) */
    public void consistentWith(TCTypes other) throws VerifyException {
	//Called only if this is a Simple Type!
	if (other.getType() == this.type || this.type == ANY || other.getType() == ANY ||
	    ((other.getType()== BYTE || other.getType() == CHAR || other.getType() == SHORT) && this.type==INT)) {
	    return;
	} else {
	    throw  new VerifyException("  Inconsistent Types: Found " + 
				       this + 
				       ", expected " + 
				       other);
	}
	
    }

    // useless for normal Types. Necessary for Object Types
    public TCTypes merge(TCTypes other) throws VerifyException {
	if (other.getType() == this.type || this.type == ANY || other.getType() == ANY ) {
	    return this; // there has been no change to this.
	} else {
	    /*throw  new VerifyException("  Inconsistent Types: Found " + 
				       this + 
				       ", expected " + 
				       other);
	    */
	    return T_UNKNOWN;
	}
    }

    public static TCTypes nextTypeFromTypeDesc(CharIter typeDesc) {

	//FEHLER: Haesslich; ausserdem VerifyException werfen statt error!
	if (typeDesc.current() == CharIter.DONE) {
	    System.err.println("Invalid Type Descriptor!");
	    System.exit(1);
	}
	switch (typeDesc.current()) {
	case 'B':
	    return new TCTypes(TCTypes.INT);
	case 'C':
	    return new TCTypes(TCTypes.INT);
	case 'D':
	    return new TCTypes(TCTypes.DOUBLE_L);
	case 'F':
	    return new TCTypes(TCTypes.FLOAT);
	case 'I':
	    return new TCTypes(TCTypes.INT);
	case 'J':
	    return new TCTypes(TCTypes.LONG_L);
	case 'S':
	    return new TCTypes(TCTypes.INT);
	case 'Z':
	    return new TCTypes(TCTypes.INT);
	case 'V':
	    return null;
	case '[':
	    typeDesc.next();
	    return new TCArrayTypes(nextTypeFromTypeDesc(typeDesc));
	case 'L':
	    StringBuffer className = new StringBuffer();
	    while (typeDesc.next() != ';') {
		className.append(typeDesc.current());
	    }
	    if (className.toString().equals(TCObjectTypes.objectString))
		return T_OBJECT;
	    ClassData actClass = TCObjectTypes.getClassFinder().findClass(className.toString());
	    if (actClass == null || !actClass.isInterface()) 
		return new TCObjectTypes(className.toString());
	    else 
		return new TCObjectTypes(TCObjectTypes.objectString, new TCInterfaceTypes(className.toString()));
	default:
	    return T_UNKNOWN;
	}
	

	
    }

    public static TCTypes[] typeFromTypeDesc(String typeDesc) {
	Vector types = new Vector();
	TCTypes tmpType;
	if (typeDesc.length() == 0) {
	    return new TCTypes[0];
	}
	CharIter td = new CharIter(typeDesc);
	do {
	    tmpType = nextTypeFromTypeDesc(td);
	    if (tmpType == null) 
		continue;
	    types.addElement(tmpType);
	    if (tmpType.getType() == TCTypes.LONG_L)
		types.addElement(new TCTypes(TCTypes.LONG_U));
	    if (tmpType.getType() == TCTypes.DOUBLE_L)
		types.addElement(new TCTypes(TCTypes.DOUBLE_U));

	} while (td.next() != CharIter.DONE);
	TCTypes ret[] = new TCTypes[types.size()];
	types.copyInto(ret);
	return ret;
    }

    public static TCTypes[] returnTypeFromMethod(String mType) {
	return typeFromTypeDesc(mType.substring(mType.indexOf(')')+1));
    }

    public static TCTypes[] argTypeFromMethod (String mType) {
	return typeFromTypeDesc(mType.substring(1,mType.indexOf(')')));
	
    }

}
