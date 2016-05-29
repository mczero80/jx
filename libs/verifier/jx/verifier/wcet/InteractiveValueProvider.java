package jx.verifier.wcet;

import jx.verifier.bytecode.*;
import jx.zero.*;
import jx.zero.debug.*;

/**Class to provide values for simulation of bytecodes that cannot be simulated, like getfield.
 */

public class InteractiveValueProvider implements ValueProvider{
    private String ctxClassName;
    private String ctxMethodName;
    private String ctxMethodType;

    public boolean valueAvailable(ByteCode bc) {
	return true;
    }

    public Integer getIntField(String className, String fieldName, String fieldType, ByteCode bc)  {
	
	Debug.out.print("Please enter value for field " +
			 className +"." + fieldName +" ("+fieldType+")\ncalled in " +
			 ctxClassName + "." + ctxMethodName +" ("+ ctxMethodType+") at " +
			 bc + ":");
	byte[] buffer = new byte[16];
	int bufferLen = 0;
	try {
	    bufferLen = Debug.in.read(buffer);
	} catch (java.io.IOException e) {
	    //ignore it
	}
	bufferLen--; //remove trailing newline
	if (bufferLen <=0) 
	    return null;
	
	String numberString = new String(buffer, 0, bufferLen);
	int value = Integer.parseInt(numberString);
	return new Integer(value);

    }

    /**returns the value that would be returned if the method was invoked.
     *note: should only be called during Simulation, as calls may return different values, depending on how often they were already called.
     */
    public Integer invokeIntMethod(String className, 
			       String methodName, 
			       String methodType,
				   ByteCode bc) {
	Debug.out.print("Please enter RETURN VALUE for method invokation of " +
			 className +"." + methodName +" ("+methodType+")\ncalled in " +
			 ctxClassName + "." + ctxMethodName +" ("+ ctxMethodType+") at " +
			 bc + ":");
	byte[] buffer = new byte[16];
	int bufferLen = 0;
	try {
	    bufferLen = Debug.in.read(buffer);
	} catch (java.io.IOException e) {
	    //ignore it
	}
	bufferLen--; //remove trailing newline
	if (bufferLen <=0) 
	    return null;

	String numberString = new String(buffer, 0, bufferLen);
	int value = Integer.parseInt(numberString);
	return new Integer(value);
    }

    public boolean providesMethodWCET(String className, 
				      String methodName, 
				      String methodType,
				      ByteCode bc) {
	return false; //try analyzing all methods first.
    }
    

    public ExecutionTime getMethodWCET(String className, 
				       String methodName, 
				       String methodType,
				       ByteCode bc) {

	
	Debug.out.print("Please enter WCET for method " +
			 className +"." + methodName +" ("+methodType+")\ncalled in " +
			 ctxClassName + "." + ctxMethodName +" ("+ ctxMethodType+") at " +
			 bc + ":");
	byte[] buffer = new byte[16];
	int bufferLen = 0;
	try {
	    bufferLen = Debug.in.read(buffer);
	} catch (java.io.IOException e) {
	    //ignore it
	    Debug.err.println("Warning IOException occured (InteractiveValueProvider)");
	}
	bufferLen--;
	if (bufferLen <=0) 
	    return null;

	String numberString = new String(buffer, 0, bufferLen);
	int value = Integer.parseInt(numberString);
	return new SimpleExecutionTime(value);
    }
    
    /**Set class, name and type of the method currently analyzed.
     */
    public void setContext(String className, 
			   String methodName, 
			   String methodType) {
	ctxClassName = className;
	ctxMethodName = methodName;
	ctxMethodType = methodType;
    }
    /**returns the value that should be used as nth parameter.
     */
    public Integer getMethodArgument(int argNum,
				     String className, 
				     String methodName, 
				     String methodType) {
	
	Debug.out.print("Please enter VALUE for Argument " + argNum + " of method " +
			 className +"." + methodName +" ("+methodType+")\ncalled from " +
			 ctxClassName + "." + ctxMethodName +" ("+ ctxMethodType+"):");
	byte[] buffer = new byte[16];
	int bufferLen = 0;
	try {
	    bufferLen = Debug.in.read(buffer);
	} catch (java.io.IOException e) {
	    //ignore it
	}
	bufferLen--; //remove trailing newline
	if (bufferLen <=0) 
	    return null;

	String numberString = new String(buffer, 0, bufferLen);
	int value = Integer.parseInt(numberString);
	return new Integer(value);
   }
}
