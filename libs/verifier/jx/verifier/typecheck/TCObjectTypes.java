package jx.verifier.typecheck;

import jx.verifier.VerifyException;
import jx.verifier.bytecode.*;
import jx.classstore.ClassFinder;
import jx.classfile.ClassData;
import jx.classfile.MethodData;
import jx.classfile.FieldData;
import jx.classfile.constantpool.*;

import java.util.Vector;
import java.util.Enumeration;

/* Class for Objects
*/
public class TCObjectTypes extends TCTypes {
    protected  String className=null;
    private static ClassFinder classFinder=null;
    private TCInterfaceTypes interfaces = null;

    public static final String objectString = "java/lang/Object";
    public static final String nullString = "(null)";

    public static final String[][] oMethods = {
	{"getClass", "()Ljava/lang/Class;"},
	{"hashCode", "()I"},
	{"clone", "()Ljava/lang/Object;"},
	{"wait", "()V"},
	{"wait", "(J)V"},
	{"wait", "(JI)V"},
	{"notify", "()V"},
	{"notifyAll", "()V"},
	{"toString", "()Ljava/lang/String;"},
	{"equals", "(Ljava/lang/Object;)Z"},
	{"finalize", "()V"}
    };

    public TCInterfaceTypes getInterfaces() {return interfaces;}

    public static void setClassFinder(ClassFinder classFinder) {
	TCObjectTypes.classFinder = classFinder;
    }
    public static ClassFinder getClassFinder() {
	return classFinder;
    }

    public String getClassName() { return className;}
    
    public String toString() {
	return "L" + className + "*" + interfaces.toString() +";";
    }
    
    public TCObjectTypes(String className) {
	super(CLASS_REF);
	this.className = className;
	if (className.charAt(0)=='[') {
	    throw new Error("Internal Error: Trying to create a TCObjectTypes object for an array!\nType: " + className);
	}
	interfaces = this.toInterfaces();
    }
    public TCObjectTypes(String className, TCInterfaceTypes interfaces) {
	super(CLASS_REF);
	this.className = className;
	if (className.charAt(0)=='[') {
	    throw new Error("Internal Error: Trying to create a TCObjectTypes object for an array!\nType: " + className);
	}
	this.interfaces = interfaces;
    }
    protected TCObjectTypes() { //For Arrays
	super(ARRAY_REF);
	interfaces = null;
    }

    /* FOUND.consistentWith(EXPECTED) 
       classFinder must be set before using consistentWith (setClassFinder())
       NOTE: null values are treated like ANY_REF - they are consistent with all other
       Object types. The reason is that we are doing a TYPE-SAFETY check and a null
       reference can have any Type.
     */
    public void consistentWith(TCTypes otherType) throws VerifyException {
	//Called only if 'this' is a Object Type!
	if (otherType instanceof TCObjectTypes) {
	    if (classFinder == null) {
		throw new Error("Internal Error: consistentWith called with classFinder==null");
	    }
	    TCObjectTypes other = (TCObjectTypes) otherType;
	    if (
		other == this ||
		other.getType() == ANY_REF ||
		other.getType() == ANY ||
		this.className.equals(nullString) ||
		other.getClassName().equals(nullString) ||
		this.descendantOf(other)) {
		return;
	    }
	    else if (other.getClassName().equals(this.className) ||
		     other.getClassName().equals(objectString)) {
		this.interfaces.consistentWith(other.getInterfaces());
		return;
	    }
	    
	} else { // other is not instanceof TCObjectTypes
	    //FEHLER das mit RETURN_ADDR sieht komisch aus!
	    if(otherType.getType() == ANY || 
	       otherType.getType() == ANY_REF ||
	       (otherType.getType() == ARRAY_REF &&
		this.className.equals(nullString)) ||
	       (otherType.getType() == RETURN_ADDR && //FEHLER ? 
		this.className.equals(objectString)))
		return ;
	}
	
	throw  new VerifyException("  Inconsistent Types: Found " + 
				   this + 
				   ", Expected " + 
				   otherType);
    }
    
    //returns true if other is ancestor of 'this'
    private boolean descendantOf(TCObjectTypes other) {
	if (other.getClassName().equals(nullString) ||
	    this.getClassName().equals(nullString)) {
	    return false;
	}
	if (other.getClassName().equals(objectString)) {
	    return true;
	}
	if (other instanceof TCArrayTypes) {
	    return false;
	}
	//other is not java/lang/Object
	if (this.className.equals(objectString)) {
	    return false;
	}
	String cName = this.className;
	ClassData actClass = classFinder.findClass(this.className);
	if (actClass == null) {
	    throw new Error ("Class " + cName + " needed by Verifier not found!");
	}
	while(!(actClass.getClassName().equals(other.getClassName()) ||
		actClass.getSuperClassName().equals(objectString))) {
	    cName = actClass.getSuperClassName();
	    actClass = classFinder.findClass(cName);
	    if (actClass == null) {
		throw new Error ("Class " + cName + " needed by Verifier not found!");
	    }
	}
	// actClass is now either direct subclass of Object or of type 'other' 
	if (actClass.getClassName().equals(other.getClassName()) ||
	    other.getClassName().equals(objectString)) {
	    return true;
	} else {
	    return false;
	}
    }

    //returns true if this implements interface other
    // else false
    public  boolean hasInterface(TCInterfaceTypes other) {
	if (className.equals(TCObjectTypes.nullString) ||
	    className.equals(TCObjectTypes.objectString))
	    return false;
	//Interface might be one of the interfaces implemented by this class, or
	//any of their superInterfaces, or an interface or SuperInterface implemented
	//by any Superclass!
	String cName = this.className;
	String[] interfaces = null;
	ClassData actClass = null;
    INTERFACE_FOR:
	for (Enumeration enum = other.getInterfaces().elements();
	     enum.hasMoreElements();
	     ) {
	    String iName = (String)enum.nextElement();
	    do {
		actClass = classFinder.findClass(cName);
		if (actClass == null) {
		    throw new Error ("Class " + cName + " needed by Verifier not found!");
		}
		//check all interfaces of current class
		if (searchInterface(actClass, iName)) 
		    continue INTERFACE_FOR; //this interface was found --> continue with next one.
		cName = actClass.getSuperClassName();
	    } while(!(actClass.getSuperClassName().equals(objectString))); 
	    //all classes and interfaces have been searched, but iName was not found....
	    return false;
	}	    
	
	return true;
    }

    
    // returns true if any interfaces or superinterfaces of cData (which might itself be
    // an interface) have the name iName
    private boolean searchInterface(ClassData cData, String iName) {
	//check all interfaces of current class
	String[] interfaces = cData.getInterfaceNames();
	for (int i = 0; i < interfaces.length; i++) {
	    if (iName.equals(interfaces[i]))
		return true;
	}
	ClassData subInterface = null;
	for (int i = 0; i < interfaces.length; i++) {
	    subInterface = classFinder.findClass(interfaces[i]);
	    if (subInterface == null) {
		throw new Error("Interface " + interfaces[i] + " not found!");
	    }
	    if (searchInterface(subInterface, iName))
		return true;
	}
	return false;
    }


    //returns new type if there were changes, else this.
    // throws exception if not mergeable
    public TCTypes merge(TCTypes other) throws VerifyException {
	if (classFinder == null) {
	    throw new Error("Internal Error: consistentWith called with classFinder==null");
	}
	TCTypes mergedType = null;
	if (other instanceof TCArrayTypes) {
	    //'this' is not an array, but arrays are Objects!
	    if (this.className.equals(nullString)) {
		mergedType = other;
	    } else if(!className.equals(objectString)) {
		mergedType = T_OBJECT;
	    } else {
		mergedType = this;
	    }
	} else if (other instanceof TCObjectTypes) {
	    //null value has no type, so it can be merged into any other objectType
	    if (className.equals(nullString)) {
		mergedType = other;
	    } else if (((TCObjectTypes)other).getClassName().equals(nullString)) {
		//no changes
		mergedType = this;
	    } else {
		//merge two classes: find first common superclass
		String commonSC = findCommonSuperClass((TCObjectTypes)other);
		if (!className.equals(commonSC)) {
		    mergedType = new TCObjectTypes(commonSC, interfaces.merge(((TCObjectTypes)other).getInterfaces()));
		} else {
		    mergedType = this;
		}
		
	    }
	} else {
	    /*throw new VerifyException("Inconsistent types: trying to merge " +
				      other + " into " +
				      this);
	    */
	    mergedType =  T_UNKNOWN;

	}
	return mergedType;
	
    }

    //find first class that is superclass of both 'this' and 'other'
    public String findCommonSuperClass(TCObjectTypes other) {
	Vector thisSuperClasses = new Vector(8);
	Vector otherSuperClasses = new Vector(8);
	
	if (other.getClassName().equals(objectString) ||
	    className.equals(objectString) ||
	    other.getClassName().equals(nullString) ||
	    className.equals(nullString)) {
	    return objectString;
	}

	//find all ancestors for both classes
	String cName = this.className;
	ClassData actClass = classFinder.findClass(cName);
	if (actClass == null) {
	    throw new Error ("Class " + cName + " needed by Verifier not found!");
	}
	// java/lang/Object cannot be found by classfinder
	while(!(actClass.getSuperClassName().equals(objectString))) {
	    thisSuperClasses.addElement(actClass);
	    cName = actClass.getSuperClassName();
	    actClass = classFinder.findClass(cName);
	    if (actClass == null) {
		throw new Error ("Class " + cName + " needed by Verifier not found!");
	    }
	}
	thisSuperClasses.addElement(actClass); 

	actClass = classFinder.findClass(other.getClassName());
	while(!(actClass.getSuperClassName().equals(objectString))) {
	    otherSuperClasses.addElement(actClass);
	    cName = actClass.getSuperClassName();
	    actClass = classFinder.findClass(cName);
	    if (actClass == null) {
		throw new Error ("Class " + cName + " needed by Verifier not found!");
	    }
	}
	otherSuperClasses.addElement(actClass);
	

	//find last common superclass, beginning with java.lang.Object (which is not
	// part of thisSuperClasses or otherSuperclasses)
	ClassData thisSC=null, otherSC=null;
	while(thisSuperClasses.size() > 0 && otherSuperClasses.size() > 0){
	    thisSC = (ClassData) thisSuperClasses.lastElement();
	    otherSC = (ClassData) otherSuperClasses.lastElement();
	    if (!thisSC.getClassName().equals(otherSC.getClassName())) {
		return thisSC.getSuperClassName();
	    }
	    thisSuperClasses.removeElementAt(thisSuperClasses.size()-1);
	    otherSuperClasses.removeElementAt(otherSuperClasses.size()-1);
	}

	if (thisSC == null) {   
	    //no entries in thisSuperClasses or otherSuperClasses i.e.
	    // one of them is of type 'Object'
	    return objectString;
	} else  {
	    //classname were the same during the whole while-loop
	    return thisSC.getClassName();
	}
    }
    
    //returns methodData if method found, null if method is a method of java/lang/Object
    //or if clName is 'null',
    // throws verifyException else
    public static MethodData findMethod(String clName, String mName, String typeDesc) 
	throws VerifyException {
	/* -- method invokation on null will either result in runtime-error, 
	   or this invokation will never occur because of a ifnonnull/ifnull! --
	  if (clName.equals(nullString)) {
	  throw new VerifyException(nullString + " has no methods!");
	  }
	*/

	if (clName.equals(nullString)) {
	    return null;
	}
	String cName = clName;
	if (!cName.equals(objectString)) {
	    ClassData cData = classFinder.findClass(cName);
	    if (cData == null) {
		throw new Error("Class " + cName + 
				" needed by Verifier not found!");
	    }

	    //search class, superclass and interfaces for method.
	    while(true) {
		MethodData mData = cData.getMethodDataNE(mName, typeDesc);
		if (mData != null)
		    return mData; //method found!

		//ONLY if cData is an interface, search all interfaces for method.
		//if cData is not an interface, it or its superclasses must implement the 
		//method. but if we do not know what kind of class cData is, only its 
		//interface, all "superinterfaces" must be searched too, as they are
		//also implemented by all objects with this interface.
		if (cData.isInterface()) {
		    //All Classes are subclasses of java/lang/Object. so search Objects's
		    //methods first!
		    for (int i = 0; i < oMethods.length; i++) {
			if (mName.equals(oMethods[i][0]) &&
			    typeDesc.equals(oMethods[i][1]) )
			    return null;
		    }
		    String[] interfaces = cData.getInterfaceNames();
		    for (int i = 0; i< interfaces.length; i++) {
			mData = findInterfaceMethod(interfaces[i], mName, typeDesc);
			if(mData != null)
			    return mData;
		    }
		    throw new VerifyException("Method " + mName + typeDesc + 
					      " not found in interface " + clName);
		} else if (!cData.getSuperClassName().equals(objectString)) {
		    //if method not in this class then probably in superclass!
		    String tmpName = cData.getSuperClassName();
		    cData = classFinder.findClass(cData.getSuperClassName());
		    if (cData == null) {
			throw new Error("Class " + tmpName + 
					" needed by Verifier not found!");
		    }
		    
		} else {
		    //superclass is java/lang/Object
		    cName = objectString;
		    break; //while
		}
	    }
	} //end if not "java/lang/object"

	//at this point, the class is java/lang/Object!!
	//if method not found until now it must be one of the java lang object methods!
	for (int i = 0; i < oMethods.length; i++) {
	    if (mName.equals(oMethods[i][0]) &&
		typeDesc.equals(oMethods[i][1]) )
		return null;
	}
	throw new VerifyException("Method " + mName + 
				  typeDesc + " not found in class " + 
				  clName);
	
    }


    //returns FieldData if field found, null if clName is 'null',
    // throws verifyException else
    public static FieldData findField(String clName, String fName)
	throws VerifyException {

	if (clName.equals(nullString)) {
	    return null;
	}
	String cName = clName;
	ClassData cData = classFinder.findClass(cName);
	if (cData == null) {
	    throw new Error("Class " + cName + 
			    " needed by Verifier not found!");
	}
	
	//search class, superclass and interfaces for field.
	while(true) {
	    FieldData fData = cData.getField(fName);
	    if (fData != null)
		return fData; //Field found
	    
	    //ONLY if cData is an interface, search all interfaces for method.
	    //if cData is not an interface, it or its superclasses must implement the 
	    //method. but if we do not know what kind of class cData is, only its 
	    //interface, all "superinterfaces" must be searched too, as they are
	    //also implemented by all objects with this interface.
	    if (cData.isInterface()) {
		
		String[] interfaces = cData.getInterfaceNames();
		for (int i = 0; i< interfaces.length; i++) {
		    fData = findInterfaceField(interfaces[i], fName);
		    if(fData != null)
			return fData;
		}
		throw new VerifyException("Field " + fName + 
					  " not found in interface " + clName);
	    } else if (!cData.getSuperClassName().equals(objectString)) {
		//if method not in this class then probably in superclass!
		String tmpName = cData.getSuperClassName();
		cData = classFinder.findClass(cData.getSuperClassName());
		if (cData == null) {
		    throw new Error("Class " + tmpName + 
				    " needed by Verifier not found!");
		}
		
	    } else {
		//superclass is java/lang/Object
		cName = objectString;
		break; //while
		}
	}
	throw new VerifyException("Field " + fName + 
				  " not found in class " + 
				  clName);
	
    }

	    
    //returns methodData if method is found in interface or one of its superinterfaces.
    //returns null, if method is not found
    //NOTE: java/lang/Object's methods are not searched!
    private static MethodData findInterfaceMethod(String iName, 
						  String mName, 
						  String typeDesc) {
	ClassData actInterface = classFinder.findClass(iName);
	if (actInterface == null) {
	    throw new Error("Interface " + iName + " not found!");
	}
	//check all interfaces of current class
	MethodData mData = actInterface.getMethodDataNE(mName, typeDesc);
	if (mData != null) 
	    return mData;
	//}
	//check all superinterfaces.
	String[] interfaces = actInterface.getInterfaceNames();
	for (int i = 0; i < interfaces.length; i++) {
	    mData = findInterfaceMethod(interfaces[i], mName, typeDesc);
	    if (mData != null) 
		return mData;
	}
	return null;
    }

    //returns FieldData if Field is found in interface or one of its superinterfaces.
    //returns null, if Field is not found
    private static FieldData findInterfaceField(String iName, 
						String fName) {
	ClassData actInterface = classFinder.findClass(iName);
	if (actInterface == null) {
	    throw new Error("Interface " + iName + " not found!");
	}
	
	FieldData fData = actInterface.getField(fName);
	if (fData != null) 
	    return fData;
	
	//check all superinterfaces.
	String[] interfaces = actInterface.getInterfaceNames();
	for (int i = 0; i < interfaces.length; i++) {
	    fData = findInterfaceField(interfaces[i], fName);
	    if (fData != null) 
		return fData;
	}
	return null;
    }
    

    //check if access to member 'member' of otherClass is allowed for thisClass. 
    // isStatic is true if it is a static access (getstatic, invokestatic), else false.
    public static void checkMemberAccess(String thisClass, 
					 TCObjectTypes otherClass,
					 ConstantPoolEntry cpEntry,
					 boolean isStatic) 
	throws VerifyException {

	if (!(cpEntry instanceof ClassMemberCPEntry)) {
	    throw new VerifyException("Invalid Constantpool-Entry for Memberaccess!");
	}
	
	ClassMemberCPEntry member = (ClassMemberCPEntry)cpEntry;

	if (otherClass.getClassName().equals(nullString)) {
	    //VerifyException e = new VerifyException();
	    //e.foundNull = true;
	    //throw e;
	    return;
	}
	if (!otherClass.getClassName().equals(objectString)) {
	    ClassData other = classFinder.findClass(otherClass.getClassName());
	    if (other == null) {
		throw new VerifyException("Class " + otherClass + " not found!");
	    }
	
	    
	    if (!(ClassData.isPublic(other.getAccessFlags()))) {
		//check if in same package
		//classnames must be the same until the last '/'
		int pNameLen = thisClass.lastIndexOf('/');
		if (!thisClass.regionMatches(0, otherClass.getClassName(), 0, pNameLen)) {
		    throw new VerifyException("Trying to access non-public class " + 
					      otherClass.getClassName() + 
					      " from Class " + thisClass + "!");
		}
	    }
	}
	
	//check if the member may be accessed.
	int accessFlags = 0;
	if (member instanceof FieldRefCPEntry) {
	    //field access
	    FieldData field = findField(otherClass.getClassName(),
					member.getMemberName());
	    if (field == null) {
		//either classname was null or the method was one of /java/lang/Object's
		//in both cases there is no further handling necessary here.
		return;
	    }
	    accessFlags = field.getModifiers();
	} else if (member instanceof MethodRefCPEntry ||
		   member instanceof InterfaceMethodRefCPEntry) {
	    //methods
	    MethodData method = findMethod(otherClass.getClassName(),
					   member.getMemberName(), 
					   member.getMemberTypeDesc());
	    if (method == null) {
		//either classname was null or the method was one of /java/lang/Object's
		//in both cases there is no further handling necessary here.
		return;
	    }
	    accessFlags = method.getModifiers();
	} else {
	    throw new Error("Internal Error: unknown CPEntry-type - " + member);
	}
	
	if (isStatic && !ClassData.isStatic(accessFlags)) {
	    throw new VerifyException("Trying to access non-static member " + 
				      member.getClassName() + "." +
				      member.getMemberName() +
				      "(" + member.getMemberTypeDesc() +
				      ") as static!");
	}
	if (ClassData.isPublic(accessFlags)) {
	    return;
	} else if (ClassData.isProtected(accessFlags)) {
	    TCObjectTypes thisType = new TCObjectTypes(thisClass);
	    if(thisType.descendantOf(otherClass)) {
		//if this is a descendant of other it may access its protected fields
		return;
	    } else {
		//package
		//check if in same package
		//classnames must be the same until the last '/'
		int pNameLen = thisClass.lastIndexOf('/');
		if (!thisClass.regionMatches(0, otherClass.getClassName(), 0, pNameLen)) {
		    throw new VerifyException("Trying to access protected member " + 
					      member.getClassName() + "." +
					      member.getMemberName() +
					      "(" + member.getMemberTypeDesc() +
					      ") from Class " + thisClass + "!");
		} else {
		    return;
		}
	    }
	} else if (ClassData.isPrivate(accessFlags)) {
	    if (thisClass.equals(otherClass.getClassName())) {
		return;
	    } else {
		throw new VerifyException("Trying to access private member " + 
					  member.getClassName() + "." +
					  member.getMemberName() +
					  "(" + member.getMemberTypeDesc() +
					  ") from Class " + thisClass + "!");
	    }
	} else {
	    //package
	    //check if in same package
	    //classnames must be the same until the last '/'
	    int pNameLen = thisClass.lastIndexOf('/');
	    if (!thisClass.regionMatches(0, otherClass.getClassName(), 0, pNameLen)) {
		throw new VerifyException("Trying to access member " + 
					  member.getClassName() + "." +
					  member.getMemberName() +
					  "(" + member.getMemberTypeDesc() +
					  ") from Class " + thisClass + 
					  " in different package!");
	    }
	}
	
	return;
    }

    /** converts this object to an interface reference which contains all interfaces implemented by this object.
     */
    private TCInterfaceTypes toInterfaces() {
	String ifaces[]= null;
	if (className.equals(objectString)) ifaces =  new String[0];
	else if (className.equals(nullString)) ifaces = new String[0];
	else {
	    ClassData cData = classFinder.findClass(className);
	    if (cData == null) {
		System.out.println("WARNING: Class " + className + " not found!");
		ifaces = new String[0]; //silently ignore it.
	    } else { 
		ifaces = cData.getInterfaceNames();
	    }
	}
	return new TCInterfaceTypes(ifaces);
    }

    public boolean equals(TCTypes other) {
	if (other instanceof TCArrayTypes)
	    return false;
	if (other instanceof TCObjectTypes &&
	    ((TCObjectTypes)other).getClassName().equals(this.className) &&
	    interfaces.equals(((TCObjectTypes)other).getInterfaces())) {
	    return true;
	} else {
	    return false;
	}
    }

}
