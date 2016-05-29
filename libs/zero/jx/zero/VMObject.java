package jx.zero;

/** reference to an object of another domain
 * the other domain must be frozen
 * if it is melted this object gets invalid
 */
public interface VMObject extends Portal {
    VMClass getVMClass();
    int getPrimitiveData();
    String getString();
    boolean getFirstSubObject(VMObject result);
    boolean getNextSubObject(VMObject result);
}
