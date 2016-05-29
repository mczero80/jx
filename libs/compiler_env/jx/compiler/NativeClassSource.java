package jx.compiler;

import jx.classfile.ClassSource; 
import jx.classfile.MethodSource; 
import jx.classfile.FieldData; 
import jx.classfile.constantpool.ConstantPool; 

class NativeClassSource extends ClassSource {
    String className, superName;
    NativeClassSource(String className, String superName) {
	this.className = className;
	this.superName = superName;
    }
    // abstract public MethodSource[] getCompilableMethods();
    public MethodSource[] getMethods() {return new MethodSource[0];}
    public ConstantPool getConstantPool() {return null;}
    public MethodSource getMethod(String methodName, String methodType) {return null;}
    public String getClassName() {return className;}
    public String getSuperClassName() {return superName;}
    //public boolean isInterface() {return false;}
    public String[] getInterfaceNames() {return new String[0];}
    public FieldData[] getFields() {return new FieldData[0];}
    public String getSourceFileAttribute() {return null;}

    public boolean isPublic() {return true;}
    public boolean isPrivate() {return false;}
    public boolean isProtected() {return false;}
    public boolean isStatic() {return true;}
    public boolean isFinal() {return true;}
    public boolean isAbstract() {return false;}
    public boolean isNative() {return true;}
    public boolean isSynchronized() {return true;}
    public boolean isInterface() {return false;}
    public boolean isVolatile() {return false;}
    public boolean isTransient() {return false;}
}
