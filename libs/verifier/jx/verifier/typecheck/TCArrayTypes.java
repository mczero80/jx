package jx.verifier.typecheck;

import jx.verifier.bytecode.*;
import jx.verifier.VerifyException;
import java.util.Vector;

/* Class for Arrays
   NOTE: Basetype for LONGs is TCTypes.LONG_U, for DOUBLEs TCTypes.DOUBLE_U !!!
*/

public class TCArrayTypes extends TCObjectTypes {
    private TCTypes baseType;
    public TCTypes getBaseType() {return baseType;}
    public String toString() {
	return className;
    }
    public String getClassName() {
	return className;
    }

    public TCArrayTypes(int intBaseType) {
	super();
	baseType = new TCTypes(intBaseType);
	fillClassName();
    }

    
    public TCArrayTypes(TCTypes baseType) {
	super();
	this.baseType = baseType;
	fillClassName();
    }

    public TCArrayTypes(byte aType) {  
	super();
	switch (aType) {
	case 4:
	    //baseType = new TCTypes(TCTypes.BOOLEAN);
	    baseType = new TCTypes(TCTypes.INT);
	    break;
	case 5:
	    baseType = new TCTypes(TCTypes.CHAR);
	    break;
	case 6:
	    baseType = new TCTypes(TCTypes.FLOAT);
	    break;
	case 7:
	    baseType = new TCTypes(TCTypes.DOUBLE_U);
	    break;
	case 8:
	    baseType = new TCTypes(TCTypes.BYTE);
	    break;
	case 9:
	    baseType = new TCTypes(TCTypes.SHORT);
	    break;
	case 10:
	    baseType = new TCTypes(TCTypes.INT);
	    break;
	case 11:
	    baseType = new TCTypes(TCTypes.LONG_U);
	    break;
	default:
	    baseType = new TCArrayTypes(TCTypes.UNKNOWN);
	    break;
	}
	fillClassName();
    }

    private void fillClassName() {
	className = "[";
	if (baseType instanceof TCObjectTypes) {
	    className += ((TCObjectTypes)baseType).getClassName();
	} else {
	    switch(baseType.getType()) {
	    case TCTypes.BYTE:
		className +="B";
		break;
	    case TCTypes.SHORT:
		className +="S";
		break;
	    case TCTypes.INT:
		className +="I";
		break;
	    case TCTypes.LONG_U:
		className +="L";
		break;
	    case TCTypes.CHAR:
		className +="C";
		break;
	    case TCTypes.FLOAT:
		className +="F";
		break;
	    case TCTypes.DOUBLE_U:
		className +="D";
		break;
	    case TCTypes.UNKNOWN:
		className +="UNKNOWN";
		break;
	    default:
		throw new Error("Internal Error: Array with unknown type - " + baseType.getType() + " (" + baseType + ")!");
	    }
	}
    }

    /* FOUND.consistentWith(EXPECTED) */
    public void consistentWith(TCTypes otherType) throws VerifyException {
	//Called only if this is a Array Type!
	if (otherType instanceof TCArrayTypes) {
	    TCArrayTypes other = (TCArrayTypes) otherType;
	    if (
		other == this ||
		other.getType() == TCTypes.ARRAY_REF
		)
		return;
	    try {
		//FEHLER (?) ist consistent in Ordnung? muesstn die nicht GLEICH sein?
		this.getBaseType().consistentWith(other.getBaseType());
		return;
	    } catch (VerifyException e) {
		//If BaseTypes are incompatible throw own exception
		throw  new VerifyException("  Inconsistent Types: " + 
					   this + 
					   " and " + 
					   otherType);
	    }

	} else if (otherType instanceof TCObjectTypes) {
	    TCObjectTypes otherObjType = (TCObjectTypes) otherType;
	    if (
		otherObjType.getClassName().equals(this.getClassName()) ||
		otherObjType.getClassName().equals("java/lang/Object") ||
		otherObjType.getType() == ANY_REF ||
		otherObjType.getType() == ANY ||
		otherObjType.getType() == ARRAY_REF		
		)
		return;
	} else {
	    if (otherType.getType() == ANY ||
		otherType.getType() == ANY_REF ||
		otherType.getType() == ARRAY_REF )
		return;
	}
	throw  new VerifyException("  Inconsistent Types: " + 
				   this + 
				   " and " + 
				   otherType);
	
    }
    

    //returns new type if there were changes, else this.
    // throws exception if not mergeable
    public TCTypes merge(TCTypes other) throws VerifyException {
	TCTypes mergedType;
	if (other instanceof TCArrayTypes) {
	    TCArrayTypes otherArray = (TCArrayTypes) other;
	    TCTypes tmp = baseType.merge(otherArray.getBaseType());
	    if (tmp != baseType) {
		mergedType = new TCArrayTypes(tmp);
	    } else {
		mergedType = this;
	    }
	} else if (other instanceof TCObjectTypes) {
	    if (((TCObjectTypes)other).getClassName().equals(TCObjectTypes.nullString)) {
		//null means unknown type, so merging with this results in this.
		mergedType = this;
	    } else {
		//merging a object and an array results in java/lang/Object! 
		mergedType = TCTypes.T_OBJECT;
	    }
	} else {
	    /*
	    throw new VerifyException("Inconsistent types: can't merge " +
				      other +
				      " into " +
				      this);
	    */
	    mergedType = TCTypes.T_UNKNOWN;
	}
	
	return mergedType;
    }
    
    //public boolean equals(TCTypes other)  not necessary, the one of TCObjectTypes is ok!


}

