package jx.classfile; 

import jx.classfile.constantpool.ConstantPool; 

/** 
    Adapter for the sources of 
    class data. 
    See subclasses. 
*/ 

abstract public class ClassSource {

    public static final int ACC_PUBLIC       = 0x0001; 
    public static final int ACC_PRIVATE      = 0x0002; 
    public static final int ACC_PROTECTED    = 0x0004; 
    public static final int ACC_STATIC       = 0x0008; 
    public static final int ACC_FINAL        = 0x0010; 
    public static final int ACC_SYNCHRONIZED = 0x0020;
    public static final int ACC_VOLATILE     = 0x0040; 
    public static final int ACC_TRANSIENT    = 0x0080; 
    public static final int ACC_NATIVE       = 0x0100; 
    public static final int ACC_INTERFACE    = 0x0200;   
    public static final int ACC_ABSTRACT     = 0x0400; 
  
    public static boolean isPublic(int accessFlags) {return (accessFlags & ACC_PUBLIC) != 0;}
    public static boolean isPrivate(int accessFlags) {return (accessFlags & ACC_PRIVATE) != 0;}
    public static boolean isProtected(int accessFlags) {return (accessFlags & ACC_PROTECTED) != 0;}
    public static boolean isStatic(int accessFlags) {return (accessFlags & ACC_STATIC) != 0;}
    public static boolean isFinal(int accessFlags) {return (accessFlags & ACC_FINAL) != 0;}
    public static boolean isSynchronized(int accessFlags) {return (accessFlags & ACC_SYNCHRONIZED) != 0;}
    public static boolean isVolatile(int accessFlags) {return (accessFlags & ACC_VOLATILE) != 0;}
    public static boolean isTransient(int accessFlags) {return (accessFlags & ACC_TRANSIENT) != 0;}
    public static boolean isNative(int accessFlags) {return (accessFlags & ACC_NATIVE) != 0;}
    public static boolean isInterface(int accessFlags) {return (accessFlags & ACC_INTERFACE) != 0;}
    public static boolean isAbstract(int accessFlags) {return (accessFlags & ACC_ABSTRACT) != 0;}

    abstract public boolean isPublic();
    abstract public boolean isPrivate();
    abstract public boolean isProtected();
    abstract public boolean isStatic();
    abstract public boolean isFinal();
    abstract public boolean isAbstract();
    abstract public boolean isNative();
    abstract public boolean isSynchronized();
    abstract public boolean isInterface();
    abstract public boolean isVolatile();
    abstract public boolean isTransient();

    // abstract public MethodSource[] getCompilableMethods();
    abstract public MethodSource[] getMethods();
    abstract public ConstantPool getConstantPool(); 
    abstract public MethodSource getMethod(String methodName, String methodType); 
    abstract public String getClassName();
    abstract public String getSuperClassName();
    //    abstract public boolean isInterface();
    abstract public String[] getInterfaceNames();
    abstract public FieldData[] getFields();
    abstract public String getSourceFileAttribute();
}
