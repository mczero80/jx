package jx.verifier.npa;

import jx.verifier.VerifyException;
import jx.verifier.CharIter;

import java.util.Vector;

public class NPAValue {
    //everything we don't know anything about
    static public final int OTHER = 0;
    //reference that is known to be null
    static public final int NULL = 1;
    //reference that is known to be non-null
    static public final int NONNULL = 2;
    //FEHLER nochmal richtig testen, ob OTHERREF wirklich weggeschmissen werden darf.
    /*//reference that we don't know anything about
      static public final int OTHERREF = 3;*/
    static public final NPAValue newOTHER() { return new NPAValue(OTHER);}
    static public final NPAValue newNULL() { return new NPAValue(NULL);}
    static public final NPAValue newNONNULL() { return new NPAValue(NONNULL);}
    //    static public final NPAValue newOTHERREF() { return new NPAValue(OTHERREF);}

    private int value;
    public int getValue() {return value;}

    static public final int INVALID_ID = Integer.MIN_VALUE;
    static private int uniqueId = Integer.MIN_VALUE+1;
    static public int getUniqueId() { return uniqueId++;}
    
    //if an element on the stack or in a local variable resulst from a copy-operation
    //on another element (e.g. aload), it should get the same id which must not be invalid!
    //if to values with different ids are merged the id must be set to INVALID_ID!
    private int id;
    public int getId() {return id;}
    public void setValidId() { if (id==INVALID_ID) id = getUniqueId();}
    public void setInvalidId() {id = INVALID_ID;}

    public NPAValue(int value) {
	this(value, INVALID_ID);
    }
    public NPAValue(int value, int id) {
	this.value = value;
	this.id = id;
    }
    public NPAValue copy() {
	return new NPAValue(value, id);
    }

    static private String[] valueNames = {
	"Other/Unknown",
	"Null",
	"NonNull",
	"Unknown Ref"};
    public String toString() { 
	return ((value >= 0 && value < valueNames.length)? valueNames[value] : 
		"InvalidValue") + 
	    "(Id " + ((id==INVALID_ID)? "INVALID" : "VALID") + ")";
    }

    // equals is used to see if merging changed something so compare both value and id,
    // although only the value is important for the results of the npa
    public boolean equals(NPAValue other) {
	return (other.getValue() == value &&
		other.getId() == id);
    }

    //merge two values:
    //returns:
    //types equal, values equal --> this
    //types equal, values diff. --> new NPAValue(this.value) (or this if this.id is invalid)
    //types diff., values equal --> new NPAValue(OTHER, this.id) (-"-)
    //types diff., values diff. --> new NPAValue(OTHER) (-"-)
    public NPAValue merge(NPAValue other) throws VerifyException{
	int otherValue = other.getValue();
	if (otherValue == this.value) {
	    //If the ids differ, the real values are probably not exactly the same
	    //-->this gets invalid id as the real value might be any of the two merged ones.
	    if (other.getId() != id && id != INVALID_ID)
		return new NPAValue(this.value);
	    else 
		return this;
	} else {
	    //types are not the same --> result is other!
	    if (other.getId() != id && id != INVALID_ID)
		return new NPAValue(OTHER);
	    else if (this.value == OTHER) return this;
	    else return new NPAValue(OTHER, id);
	}
    }


    public static NPAValue[] typeFromTypeDesc(String typeDesc) throws VerifyException {
	Vector types = new Vector();
	NPAValue tmpValue[];
	if (typeDesc.length() == 0) {
	    return new NPAValue[0];
	}
	CharIter td = new CharIter(typeDesc);
	do {
	    tmpValue = nextTypeFromTypeDesc(td);
	    if (tmpValue == null) 
		continue;
	    for(int i = 0; i < tmpValue.length;i++) types.addElement(tmpValue[i]);
	} while (td.next() != CharIter.DONE);
	NPAValue ret[] = new NPAValue[types.size()];
	types.copyInto(ret);
	return ret;
    }

    public static NPAValue[] nextTypeFromTypeDesc(CharIter typeDesc)
	throws VerifyException {
	    //Eigentlich kein error sondern eine VerifyException!!!
	if (typeDesc.current() == CharIter.DONE) {
	    throw new Error("Internal Error: Invalid Type Descriptor!");
	}
	switch (typeDesc.current()) {
	    case 'B': //Byte
	    {NPAValue[] ret = {newOTHER()}; return ret;}
	    case 'C': //Char
	    {NPAValue[] ret = {newOTHER()}; return ret;}
	    case 'D': //double
	    {NPAValue[] ret = {newOTHER(), newOTHER()}; return ret;}
	    case 'F': //float
	    {NPAValue[] ret = {newOTHER()}; return ret;}
	    case 'I': //int 
	    {NPAValue[] ret = {newOTHER()}; return ret;}
	    case 'J'://long
	    {NPAValue[] ret = {newOTHER(), newOTHER()}; return ret;}
	    case 'S'://short
	    {NPAValue[] ret = {newOTHER()}; return ret;}
	    case 'Z'://boolean
	    {NPAValue[] ret = {newOTHER()}; return ret;}
	    case 'V': //void
	    return null;
	    case '[':
	    typeDesc.next();
	    //return new NPAArrayValue(nextTypeFromTypeDesc(typeDesc));
	    nextTypeFromTypeDesc(typeDesc);
	    //{NPAValue[] ret = {newOTHERREF()}; return ret;}
	    {NPAValue[] ret = {newOTHER()}; return ret;}
	    case 'L':
	    StringBuffer className = new StringBuffer();
	    while (typeDesc.next() != ';') {
		className.append(typeDesc.current());
	    }
	    // return new NPAObjectValue(className.toString());
	    //{NPAValue[] ret = {newOTHERREF()}; return ret;}
	    {NPAValue[] ret = {newOTHER()}; return ret;}
	default:
	    throw new VerifyException("Unknown type : " + typeDesc.current());
	}
	

	
    }


    public static NPAValue[] returnTypeFromMethod(String mType) throws VerifyException {
	return typeFromTypeDesc(mType.substring(mType.indexOf(')')+1));
    }

    public static NPAValue[] argTypeFromMethod (String mType) throws VerifyException {
	return typeFromTypeDesc(mType.substring(1,mType.indexOf(')')));
	
    }

}
