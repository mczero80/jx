package jx.classstore;


import jx.classfile.constantpool.*; 
import jx.classfile.*; 
import jx.classfile.datatypes.*; 

import jx.zero.Debug;
import jx.zero.Memory;

/** Helper class to get information about classes and their relation */
public class ClassInformation {
    ClassFinder classFinder;

    public ClassInformation(ClassFinder classFinder) {
	this.classFinder = classFinder;
    }

    /*
    public boolean isAssignableTo(String className, String superName) {
	BCClass c1 = findClass(className);
	if (c1 == null ) Debug.throwError("class "+className+" not found");
	BCClass c2 = findClass(superName);
	if (c2==null) Debug.throwError("class "+superName+" not found");
	return domClassStore.isAssignableTo(c1, c2);
    }

    public boolean isAssignableTo(ClassData c1, BCClass c2) {
	return domClassStore.isAssignableTo(c1, c2);
    }
    */

    public ClassData[] getInterfaces(ClassData cl) {
      return findClasses(cl.getInterfaceNames());
    }

    public boolean doesImplement(ClassData cl, ClassData interf) {
      if (cl==null) return false;
      ClassData[] all = findClasses(cl.getInterfaceNames());
      for(int i=0; i<all.length; i++) {
	if( all[i] == interf) return true;
      }
      return doesImplement(classFinder.findClass(cl.getSuperClassName()), interf);
    }
    

    public ClassData [] findClasses(String[] classnames) {
	ClassData result[] = new ClassData[classnames.length];
	for(int i=0; i<classnames.length; i++) {
	    result[i] = classFinder.findClass(classnames[i]);
	}
	return result;
    }

    public ClassData [] findClasses(BasicTypeDescriptor[] classnames) {
	ClassData result[] = new ClassData[classnames.length];
	for(int i=0; i<classnames.length; i++) {
	    result[i] = classFinder.findClass(classnames[i].getClassName());
	}
	return result;
    }

    public ClassData findClass(BasicTypeDescriptor classname) {
	return classFinder.findClass(classname.getClassName());
    }
    public ClassData getSuperClass(ClassData cl) {
	return classFinder.findClass(cl.getSuperClassName());
    }

}
