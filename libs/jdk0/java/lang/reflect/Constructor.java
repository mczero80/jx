package java.lang.reflect;
import jx.zero.VMClass;
import jx.zero.VMMethod;
public class Constructor {
    VMMethod method;
    VMClass vmclass;
    Class c;
    Class[] param;
    public Constructor(Class c, Class[] param) {
	this.c = c;
	this.param = param;
    }
   public java.lang.Object newInstance(java.lang.Object[] arg0) {
       
       throw new Error("NOT IMPLEMENTED");
   }
   public java.lang.Class getDeclaringClass() { throw new Error("NOT IMPLEMENTED"); }
   public java.lang.Class[] getExceptionTypes() { throw new Error("NOT IMPLEMENTED"); }
   public int getModifiers() { throw new Error("NOT IMPLEMENTED"); }
   public java.lang.Class[] getParameterTypes() { throw new Error("NOT IMPLEMENTED"); }
}
