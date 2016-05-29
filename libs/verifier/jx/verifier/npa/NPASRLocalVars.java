package jx.verifier.npa;

import jx.verifier.bytecode.*;
import jx.verifier.*;
import java.util.Vector;


/* Local Variables for verifying Subroutines (jsr ... ret)
   records every access to a local Variable in accessed array
*/
public class NPASRLocalVars extends JVMSRLocalVars implements NPALocalVarsInterface {


    ///// methods from NPALocalVars//////////////////
    protected void initVars(String className,
			    String methodTypeDesc,
			    boolean isStatic)
	throws VerifyException {
	int i = 0;
	if (isStatic) {
	    write(i, new NPALocalVarsElement(NPAValue.newNONNULL(), -1)); //this
	    i++;
	}
	for(;i < getNumVars(); i++) {
	    write(i, new NPALocalVarsElement(NPAValue.newOTHER(), -1));
	}
    }

    public JVMLocalVars copy() {return copySR();} 
  //find all local Vars with same id as value (must be a valid id!) and change their
    //value to newVal.
    public void setValue(NPAValue value, int newVal) {
	JVMLocalVarsElement[] lv = getLVars();
	for (int i = 0; i < lv.length; i++) {
	    if (lv[i] != null && 
		((NPALocalVarsElement)lv[i]).getType().getId() == value.getId()) {
		//the values are the same so update information
		((NPALocalVarsElement)lv[i]).setType(new NPAValue(newVal, value.getId()));
	    }
	}
    }
    
    ///////////////////////////

    public JVMSRLocalVars copySR() {
	return new NPASRLocalVars(this.getLVars(), this.getAccessed());
    }

    public NPASRLocalVars(JVMLocalVars otherLV) {
	super(otherLV);
    }

    protected NPASRLocalVars(JVMLocalVarsElement[] lVars, boolean[] accessed) {
	super(lVars, accessed);
    }

    public NPAValue NPAread(int index) throws VerifyException {
	return ((NPALocalVarsElement)super.read(index)).getType();
    }
    
    public void write(int index, NPAValue type, int bcAddr) throws VerifyException {
	super.write(index, new NPALocalVarsElement(type, bcAddr));
    }

}

