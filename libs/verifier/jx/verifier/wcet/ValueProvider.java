package jx.verifier.wcet;

import jx.verifier.bytecode.*;

/**Interface to provide values for simulation of bytecodes that cannot be simulated, like getfield.
 */
public interface ValueProvider {
    /**Check if information for ByteCode bc is available.
     * @return true if available, else false
     */
    public boolean valueAvailable(ByteCode bc);

    /** Get Field Value
     * @return value if available, else null
     */
    public Integer getIntField(String className, String fieldName, String fieldType, ByteCode bc);

    /**returns the value that would be returned if the method was invoked.
     *note: should only be called during Simulation, as calls may return different values, depending on how often they were already called.
     * @return value if available, else null
     */
    public Integer invokeIntMethod(String className, 
			       String methodName, 
			       String methodType,
				   ByteCode bc) ;
    /** Get ExecutionTime of Method
     */
    public ExecutionTime getMethodWCET(String className, 
				       String methodName, 
				       String methodType,
				       ByteCode bc);

    /**returns true if the method should not be analyzed and the wcet is provided by this vp.
     * Note that this method might return false though <code>getMethodWCET</code> returns a valid ExecutionTime. This is the case, if the method should be analyzed, and only if it could not be analyzed, as a last resort, the wcet provided by the vp. should be used.
     * @return true if the wcet from the vp must be used.
     */
    public boolean providesMethodWCET(String className, 
				       String methodName, 
				       String methodType,
				       ByteCode bc);

    /**Set class, name and type of the method currently analyzed.
     */
    public void setContext(String className, 
			   String methodName, 
			   String methodType);
    
    /**returns the value that should be used as nth parameter.
     * @return value if available, else null
     */
    public Integer getMethodArgument(int argNum,
				 String className, 
				 String methodName, 
				     String methodType);
}
