package jx.verifier.typecheck;

import jx.verifier.bytecode.*;
import jx.verifier.VerifyException;
import jx.verifier.JVMOPStackElement;
import java.util.Vector;

public class OPStackElement extends JVMOPStackElement{
    private TCTypes type;
    public TCTypes getType() {return type;}
    public OPStackElement(TCTypes type, JVMOPStackElement prev) {
	super(prev);
	this.type = type;
    }
    public OPStackElement(TCTypes type, JVMOPStackElement prev, int bcAddr) {
	super(prev, bcAddr);
	this.type = type;
    }

    //must return true, if the data of other equals the data of this.
    public boolean dataEquals(JVMOPStackElement other){
	return (other != null) ?
	    (getType().equals(((OPStackElement)other).getType())) :
	    false ;

    }

    //merges the two StackElements. Has to return a NEW(!) JVMOPStackElement (even if this
    //and other were the same!), whose prev pointer must be null! 
    //throws Exception if not mergeable
    public JVMOPStackElement merge(JVMOPStackElement otherElm) throws VerifyException {
	OPStackElement other = (OPStackElement) otherElm;
	TCTypes mergedType =null;
	try {
	    mergedType =  getType().merge(other.getType()); 
	} catch (VerifyException e) {
	    e.append(" while comparing Stacks:\n");
	    throw e;
	    //FEHLER das ganze nach oben verschieben (in OPStack)+
	    //"  this :" + this.toString() + 
	    //"\n  other:" + otherStack.toString());
	}
	return new OPStackElement(mergedType, null, JVMOPStackElement.ADDR_MERGE);
    }

    
    public String toString() {
	return (getPrev() != null)? 
	    getPrev().toString() + ": <"+ type.toString()+ " ("  + addressString() +")>" :
	    "<"+type.toString()+" ("  + addressString() + ")>";
    }
}
