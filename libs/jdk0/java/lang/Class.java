package java.lang;

import jx.zero.VMClass;
import jx.zero.VMMethod;

import jx.zero.CPUManager;
import jx.zero.InitialNaming;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

public final class Class
{
    static CPUManager cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");

    VMClass vmclass;
    VMMethod methods[];

    private Class(VMClass cl) {
	//System.out.println("");
	this.vmclass = cl;
    }
    
    public static Class forName(String className) throws ClassNotFoundException {
	VMClass cl = cpuManager.getClass(className);
	if (cl == null) throw new ClassNotFoundException();
	return new Class(cl);
    }
    
    public String toString()
    {
	return (isInterface() ? "interface " : "class ") + getName();
    }

    public Class getComponentType() { throw new Error(); }
    
    public String getName() { return vmclass.getName(); }
    
    public boolean isInterface() { return false; }

    public boolean isInstance(Object o) { throw new Error("NOT IMPLEMENTED"); }
    public Method[] getDeclaredMethods()  { throw new Error("NOT IMPLEMENTED"); }

    public Class getSuperclass() { return null; }
    
    public Class[] getInterfaces() { return null; }
    
    public ClassLoader getClassLoader() { throw new Error("NOT IMPLEMENTED"); }
    
    public Object newInstance() throws InstantiationException, IllegalAccessException{
	return vmclass.newInstance(); 
    }
    
    public static Class getPrimitiveClass(String cname) {
	return null ; 
    }

    public boolean isAssignableFrom(Class c) {
	throw new Error("NOT IMPLEMENTED");
    }

    public Constructor getConstructor(Class[] c) throws NoSuchMethodException {
	//	throw new Error("NOT IMPLEMENTED");
	return new Constructor(this, c);
    }

    /*
    public Method getMethod(String name, Class parameterTypes[]) throws NoSuchMethodException, SecurityException {
	if (methods == null) methods = vmclass.getMethods();
	for(int i=0; i<methods.length; i++) {
	    if (name.equals(methods[i].getName())) {
	    }
	}
	throw new NoSuchMethodException();
    }
    */

}

