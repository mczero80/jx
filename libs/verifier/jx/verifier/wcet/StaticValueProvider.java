package jx.verifier.wcet;

import jx.verifier.bytecode.*;
import jx.zero.*;
import jx.zero.debug.*;

/**Class to provide values for simulation of bytecodes that cannot be simulated, like getfield.
 */

public class StaticValueProvider implements ValueProvider{
    private String ctxClassName;
    private String ctxMethodName;
    private String ctxMethodType;

    public boolean valueAvailable(ByteCode bc) {
	return true;
    }

    public Integer getIntField(String className, String fieldName, String fieldType, ByteCode bc)  {
	Debug.out.println("StaticValueProvider asked");
	return new Integer(0);

    }

    /**returns the value that would be returned if the method was invoked.
     *note: should only be called during Simulation, as calls may return different values, depending on how often they were already called.
     */
    public Integer invokeIntMethod(String className, 
			       String methodName, 
			       String methodType,
				   ByteCode bc) {
	Debug.out.println("StaticValueProvider asked");
	return new Integer(0);
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
	Debug.out.println("StaticValueProvider asked");
	return new SimpleExecutionTime(2);
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
	
	Debug.out.println("StaticValueProvider asked");
	return new Integer(0);
   }
}
