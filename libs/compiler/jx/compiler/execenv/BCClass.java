package jx.compiler.execenv; 

import jx.compiler.*;
//import jx.compiler.execenv.BCClassInterface;

//import jx.jit.bytecode.code.*; 
import jx.classfile.constantpool.*; 
import jx.classfile.*; 
//import jx.jit.bytecode.execenv.*; 
//import jx.jit.bytecode.translation.*;  
import jx.classfile.datatypes.*; 
//import jx.jit.debug.DebugConf; 
import jx.zero.Debug;
import java.util.Vector;

// BCClass stimmt nicht unbedingt mit ClassData ueberein -> subclassen etc ! 
// vorerst: Schnittstellen sind wichtig 
public class BCClass implements BCClassInterface {
    BCMethod[] method; 
    
    ClassSource classSource; 
    String className;
    Object info;

  public BCClass(ClassSource classSource, String className, Object info) {
    this.classSource = classSource; 
    this.className = className;
    this.info = info;
  }

  public BCClass(ClassSource classSource, String className) {
      this(classSource, className, null);
  }

  public BCClass(ClassSource classSource) {
    this(classSource, classSource.getClassName(), null);
  }

    public void setInfo(Object info) {
	this.info = info;
    }

    public Object getInfo() {
	return info;
    }

    public boolean isInterface() {
	return classSource.isInterface();
    }

    public ClassSource getClassSource() { return classSource; }

  public BCMethod getMethod(String name, String type) {
    return new BCMethod(classSource.getMethod(name, type)); 
  }

    public String getClassName() {
	return className;
    }

    public BCMethodWithCode getMethodWithCode(String name, String type) throws CompileException {
	return new BCMethodWithCode(classSource.getMethod(name, type), getConstantPool(), null); 
    }
    
    // get all methods of this class
    // returns also abstract methods
    // NO LONGER filters out native methods
  public BCMethod[] getAllMethodsWithCode(Vector replaceInterfaceWithClass) throws CompileException {
      //Debug.out.println("getAllMethodsWithCode from " + classSource);
      MethodSource[] m = classSource.getMethods();
      ConstantPool cp = getConstantPool(); 
      int numMethods = m.length;

      /*
      for(int i=0; i<m.length; i++) {
	  if (m[i].isNative()) {
	      throw new Error("native method not allowed");
	  }
      }
      */
      BCMethod[] bm = new BCMethod[numMethods];
      int j=0;
      for(int i=0; i<m.length; i++) {
	  /*
	  if (m[i].isNative()) {
	      throw new Error("native method not allowed");
	  }
	  */
	  //Debug.out.println("    "+m[i].getMethodName() + m[i].getMethodType()); 	  
	  if (m[i].isAbstract() || m[i].isNative() ) {
	      bm[j] = new BCMethod(m[i]); // cannot get code of abstract method
	  } else {
	      bm[j] = new BCMethodWithCode(m[i], cp, replaceInterfaceWithClass);
	  }
	  j++;
      }
      return bm;
  }

  public ConstantPool getConstantPool() {
    return classSource.getConstantPool(); 
  }

  public void invalidateMethod(BCMethod method) {
    // invalidate method, if cached 
  }

    public String toString() {
	return "BCClass("+className+")";
    }
}
