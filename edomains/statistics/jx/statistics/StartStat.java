package jx.statistics;

import jx.emulation.MemoryManagerImpl;
import jx.zero.MemoryManager;
import jx.zero.Memory;
import jx.zero.Debug;
import jx.classstore.ClassManager;
import jx.classstore.ClassFinder;
import jx.classfile.ClassData;
import jx.classfile.MethodData;
import jx.classfile.FieldData;

import java.io.*;
import java.util.Vector;
import java.util.Hashtable;
import java.util.StringTokenizer;

import jx.collections.Iterator;

import jx.compiler.execenv.BCClass;
import jx.compiler.execenv.BCMethod;
import jx.compiler.execenv.BCMethodWithCode;

import jx.classfile.datatypes.*; 
import jx.classfile.datatypes.DataType; 


import jx.classfile.constantpool.*; 

/**
 * Code statistics, dependency checker and compatibility checker.
 * NOT FINISHED YET!!!!
 * @author Michael Golm
 */
public class StartStat {
    static MemoryManager memMgr = new MemoryManagerImpl();

    private static final boolean doDebug = true;
    public static void readZipFile(String filename, MemoryManager memMgr) {
	try {
	RandomAccessFile file = new RandomAccessFile(filename, "r");
	byte [] data = new byte[(int)file.length()];
	file.readFully(data);
	Memory m = memMgr.alloc(data.length);
	m.copyFromByteArray(data, 0, 0, data.length);
	} catch(IOException e) {
	    e.printStackTrace();
	}
    }

    public static Memory getZIP(MemoryManager memMgr, String filename) {
	try {
	    if (doDebug) Debug.out.println("read classes.zip file: "+filename);
	    RandomAccessFile file = new RandomAccessFile(filename, "r");
	    byte [] data = new byte[(int)file.length()];
	    file.readFully(data);
	    Memory m = memMgr.alloc(data.length);
	    m.copyFromByteArray(data, 0, 0, data.length);
	    return m;
	} catch(IOException e) {
	    Debug.throwError("could not read classes.zip file: "+filename);
	    return null;
	}
    }   

    static Vector parsePath(String path) {
	if (path.equals("-")) return null;
	Vector paths = new Vector();
	StringTokenizer tk = new StringTokenizer(path, ":");
	while (tk.hasMoreTokens()) {
	    paths.addElement(tk.nextToken());
	}
	return paths;
    }

    public static void main(String[] args) throws Exception {

       jx.emulation.Init.init();	

       System.out.println("JX Classfile Statistics");

       if (args.length < 1) {
	   System.out.println("Parameters:");
	   System.out.println("     (-stat|-dep|-impldep|-comp|-externals|-createexternals)");
	   System.out.println("     zipfiles");
	   System.out.println("     ");
	   System.out.println("     -createxternals creates files in the current working directory!");
	   System.out.println("Example: -dep classes.zip java/lang/Object");
	   System.out.println("Example: -comp jx/libs/jdk0.zip classes.zip");
	   System.out.println("Example: -externals jx/libs/jdk0.zip");
	   System.out.println("Example: -externals jx/libs/jdk1.zip jx/libs/zero.zip");
	   return;
       }

       Memory[] libClasses;

       if (args[0].equals("-stat") || args[0].equals("-externals")|| args[0].equals("-createexternals")) {
	   libClasses = new Memory[args.length-1];
	   for(int i=0; i<args.length-1; i++) {
	       libClasses[i] = getZIP(memMgr, args[i+1]);
	   }
       } else {
	   libClasses = new Memory[args.length-2];
	   for(int i=0; i<args.length-2; i++) {
	       libClasses[i] = getZIP(memMgr, args[i+1]);
	   }
       }
       
       ClassManager classManager = new ClassManager();
       classManager.addFromZip(null, libClasses);

       if (args[0].equals("-dep")) {
	   Vector v = new Vector();
	   interfaceDependencies(v, classManager, args[args.length-1]);
	   print(v);
	   return;
       }

       if (args[0].equals("-impldep")) {
	   Vector v = new Vector();
	   implementationDependencies(v, classManager, args[args.length-1]);
	   print(v);
	   return;
       }

       if (args[0].equals("-comp")) {
	   Vector v = new Vector();
	   compatibility(v, classManager, args[args.length-1]);
	   return;
       }

       if (args[0].equals("-stat")) {
	   statistics(classManager);
	   return;
       }


       if (args[0].equals("-externals")) {
	   externals(classManager);
	   return;
       }

       if (args[0].equals("-createexternals")) {
	   createexternals(classManager);
	   return;
       }


       
       throw new Error("Wrong parameters");
       
       
    }
    
    static void externals(ClassManager classManager) {
	ClassFinder classFinder = classManager; // avoid accessing classManager directly
	Iterator iter = classManager.getAllClasses();
	while(iter.hasNext()) {
	    ClassData c = (ClassData) iter.next();
	    ConstantPool cp = c.getConstantPool();
	    int n = cp.getNumberOfEntries();
	    cploop:
	    for (int i=0; i<n; i++) {
		ConstantPoolEntry e = cp.entryAt(i);
		if (e.getTag() == ConstantPoolEntry.CONSTANT_CLASS){
		    // found external; try to resolve it
		    ClassCPEntry ccp = (ClassCPEntry)e;
		    String className = ccp.getClassName();
		    while (className.charAt(0)=='[') {
			className = className.substring(1);
			if (className.charAt(0)=='[') continue;
			if (className.charAt(0)=='L') {
			    int c0 = className.indexOf(';');
			    className = className.substring(1, c0);
			} else {
			    // primitive type   
			    continue cploop;
			}
		    }
		    ClassData cd = classFinder.findClass(className);
		    if (cd == null) {
			System.out.println("EXTERNAL: "+className);
		    }
		}
	    }
	}
    }

    static void createexternals(ClassManager classManager) throws Exception {
	Vector classes = new Vector();
	Hashtable findclass = new Hashtable();
	ClassFinder classFinder = classManager; // avoid accessing classManager directly
	Iterator iter = classManager.getAllClasses();
	while(iter.hasNext()) {
	    ClassData c = (ClassData) iter.next();
	    // superclass and interfaces
	    String[] ifn = c.getInterfaceNames();
	    for(int i=0; i<ifn.length; i++) {
		String className = ifn[i];
		ClassData cd = classFinder.findClass(className);
		if (cd == null) {
		    MyClass cla = (MyClass) findclass.get(className);
		    if (cla==null) {
			cla = new MyClass();
			cla.name = className;
			findclass.put(className, cla);
			classes.addElement(cla);
		    }
		}
	    }

	    // all from constantpool
	    ConstantPool cp = c.getConstantPool();
	    int n = cp.getNumberOfEntries();
	    cploop:
	    for (int i=0; i<n; i++) {
		ConstantPoolEntry e = cp.entryAt(i);
		if (e.getTag() == ConstantPoolEntry.CONSTANT_METHODREF || e.getTag() == ConstantPoolEntry.CONSTANT_INTERFACEMETHODREF){
		    String className;
		    String mname;
		    String mtype;
		    if (e.getTag() == ConstantPoolEntry.CONSTANT_METHODREF) {
			MethodRefCPEntry mcp = (MethodRefCPEntry)e;
			className = mcp.getClassName();
			mname = mcp.getMemberName();
			mtype = mcp.getMemberTypeDesc();
		    } else {
			InterfaceMethodRefCPEntry mcp = (InterfaceMethodRefCPEntry)e;
			className = mcp.getClassName();
			mname = mcp.getMemberName();
			mtype = mcp.getMemberTypeDesc();
		    }

		    //System.out.println(mname+"::"+mtype);
		    while (className.charAt(0)=='[') {
			className = className.substring(1);
			if (className.charAt(0)=='[') continue;
			if (className.charAt(0)=='L') {
			    int c0 = className.indexOf(';');
			    className = className.substring(1, c0);
			} else {
			    // primitive type   
			    continue cploop;
			}
		    }
		    ClassData cd = classFinder.findClass(className);
		    if (cd == null) {
			MyClass cla = (MyClass) findclass.get(className);
			if (cla==null) {
			    cla = new MyClass();
			    cla.name = className;
			    findclass.put(className, cla);
			    classes.addElement(cla);
			}
			if (cla.findmethods.get(mname+mtype)==null) {
			    MyMethod me = new MyMethod();
			    me.name = mname;
			    me.type = mtype;
			    //if () me.isStatic = true;
			    cla.methods.addElement(me);
			    cla.findmethods.put(mname+mtype, me);
			}
		    }
		} else if (e.getTag() == ConstantPoolEntry.CONSTANT_FIELDREF){
		    FieldRefCPEntry mcp = (FieldRefCPEntry)e;
		    String mtype = mcp.getMemberTypeDesc();
		    DataType dt = new DataType(mtype);
		    String className = dt.getReferenceType();
		    if (className!=null) {
			ClassData cd = classFinder.findClass(className);
			if (cd == null) {
			    MyClass cla = (MyClass) findclass.get(className);
			    if (cla==null) {
				cla = new MyClass();
				cla.name = className;
				findclass.put(className, cla);
				classes.addElement(cla);
			    }
			}
		    }
		} else if (e.getTag() == ConstantPoolEntry.CONSTANT_CLASS){
		    ClassCPEntry ccp = (ClassCPEntry)e;
		    String mtype = ccp.getClassName();
		    DataType dt = new DataType(mtype);
		    String className = dt.getReferenceType();
		    if (className!=null) {
			ClassData cd = classFinder.findClass(className);
			if (cd == null) {
			    MyClass cla = (MyClass) findclass.get(className);
			    if (cla==null) {
				cla = new MyClass();
				cla.name = className;
				findclass.put(className, cla);
				classes.addElement(cla);
			    }
			}
		    }
		}
	    }
	}

	// find argument and return classes
	for(int i=0; i<classes.size(); i++) {
	    MyClass cl = (MyClass)classes.elementAt(i);	    
	    for(int j=0; j<cl.methods.size(); j++) {
		MyMethod m = (MyMethod)cl.methods.elementAt(j);
		Vector types=new Vector();
		MethodTypeDescriptor mt = new MethodTypeDescriptor(m.type);
		types.addElement(mt.getReturnType());
		DataType args[] = mt.getArguments();
		for(int k=0; k<args.length; k++) types.addElement(args[k]);
		for(int k=0; k<types.size(); k++) {
		    DataType dt = (DataType)types.elementAt(k);
		    String className = dt.getReferenceType();
		    if (className==null) continue;
		    ClassData cd = classFinder.findClass(className);
		    if (cd == null) {
			MyClass cla = (MyClass) findclass.get(className);
			if (cla==null) {
			    cla = new MyClass();
			    cla.name = className;
			    findclass.put(className, cla);
			    classes.addElement(cla);
			}
			
		    }
		}
	    }
	}

	// create class files
	boolean opt_n = false;
	for(int i=0; i<classes.size(); i++) {
	    MyClass cl = (MyClass)classes.elementAt(i);
	    cl.dirName = ".";
	    boolean last=false;
	    String name=cl.name;
	    while(!last) {
		int i0 = name.indexOf('/');
		String p=null;
		if (i0==-1) {
		    last = true;
		    cl.className = name;
		} else {
		    p = name.substring(0, i0);
		    name = name.substring(i0+1);
		}
		if (! last) {
		    cl.dirName += "/"+p;
		    if (! opt_n) {
			File file = new File(cl.dirName);
			if (! file.exists()) {
			    System.out.print(" "+cl.dirName);
			    file.mkdir();
			}
		    }
		    cl.pack.addElement(p);
		}
	    }
	    try {
		PrintStream out = new PrintStream(new FileOutputStream(cl.dirName+"/"+cl.className+".java"));
		out.print("package ");
		for(int j=0; j<cl.pack.size(); j++) {
		    out.print(((String)cl.pack.elementAt(j)));
		    if (j < cl.pack.size()-1) out.print(".");
		}
		out.println(";");
		out.println("public class "+cl.className+" {");
		for(int j=0; j<cl.methods.size(); j++) {
		    MyMethod m = (MyMethod)cl.methods.elementAt(j);
		    if (m.name.equals("<init>")) m.name = cl.className;
		    MethodTypeDescriptor mt = new MethodTypeDescriptor(m.type);
		    out.print("   ");
		    if (m.isStatic) out.print("static ");
		    out.print("public "+mt.getJavaReturnType()+" "+m.name+"(");
		    out.print(mt.getJavaArgumentList());
		    out.println(") { throw new Error(\"NOT IMPLEMENTED\"); }");
		}
		out.println("}");
		out.close();
	    } catch(FileNotFoundException ex) {
		throw new Error("FILE NOT FOUND "+ex);
	    }
	}

    }

    static void statistics(ClassManager classManager) {
	ClassFinder classFinder = classManager; // avoid accessing classManager directly
	//       classFinder.dump();
	
	
	Iterator iter = classManager.getAllClasses();
	
	
	int allClasses=0;
	
	// methods
	int allMethods=0;
	int nativeMethods=0;
	int privateNativeMethods=0;
	   int protectedNativeMethods=0;
	   int publicNativeMethods=0;
	   int publicNativeMethodsInPublicClass=0;
	   int publicNativeStaticMethodsInPublicClass=0;
	   
	   // fields
	   int allFields=0;
	   int publicFields=0;
	   int publicStaticFields=0;
	   int publicStaticNonFinalFields=0;
	   
	   while(iter.hasNext()) {
	       ClassData c = (ClassData) iter.next();
	       allClasses++;
	       //	   c.dump();
	       //	   Debug.out.println(c.getClassName()); 
	       // METHODS
	       MethodData[] m = c.getMethodData();
	       for(int i=0; i<m.length; i++) {
		   allMethods++;
		   if (m[i].isNative()) {
		       //Debug.out.println(c.getClassName() + " " + m[i].getMethodName() + m[i].getMethodType()); 
		       nativeMethods++;
		       if (m[i].isPublic()) publicNativeMethods++;
		       if (m[i].isProtected()) protectedNativeMethods++;
		       if (m[i].isPrivate()) privateNativeMethods++;
		       if (m[i].isPublic() && c.isPublic()) {
			   //Debug.out.println(c.getClassName() + " " + m[i].getMethodName() + m[i].getMethodType()); 
			   publicNativeMethodsInPublicClass++;
		       }
		       if (m[i].isPublic() && c.isPublic() && m[i].isStatic()) {
			   Debug.out.println("M:"+c.getClassName() + " " + m[i].getMethodName() + m[i].getMethodType()); 
			   publicNativeStaticMethodsInPublicClass++;
		       }
		   }
	       }
	       // FIELDS
	       FieldData[] f = c.getFields();
	       for(int i=0; i<f.length; i++) {
		   allFields++;
		   
		   if (f[i].isPublic() && c.isPublic()) {
		       publicFields++;
		       if (f[i].isStatic()) {
			   publicStaticFields++;
			   if (!f[i].isFinal()) {
			       Debug.out.println("F:"+c.getClassName() + " " + f[i].getName() +"  "+ f[i].getType()); 
			       publicStaticNonFinalFields++;
			   }
		       }
		   }
	       }
	   }
	   
	   Debug.out.println("Total classes: " + allClasses);
	   Debug.out.println("Total methods: " + allMethods);
	   Debug.out.println("Native methods: " + nativeMethods + " ("+((nativeMethods*100.0) / allMethods)+" percent of all methods)");
	   Debug.out.println("Private Native methods: " + privateNativeMethods + " ("+((privateNativeMethods*100.0) / nativeMethods)+" percent of native methods)");
	   Debug.out.println("Protected Native methods: " + protectedNativeMethods + " ("+((protectedNativeMethods*100.0) / nativeMethods)+" percent of native methods)");
	   Debug.out.println("Public Native methods: " + publicNativeMethods + " ("+((publicNativeMethods*100.0) / nativeMethods)+" percent of native methods)");
	   Debug.out.println("Public Native methods in public classes: " + publicNativeMethodsInPublicClass + " ("+((publicNativeMethodsInPublicClass*100.0) / nativeMethods)+" percent of native methods)");
	   Debug.out.println("Public Native static methods in public classes: " + publicNativeStaticMethodsInPublicClass + " ("+((publicNativeStaticMethodsInPublicClass*100.0) / nativeMethods)+" percent of native methods)");
	   
	   Debug.out.println("Total fields: " + allFields);
	   Debug.out.println("Public fields in public classes: " + publicFields + " ("+((publicFields*100.0) / allFields)+" percent of all fields)");
	   Debug.out.println("Public static fields: " + publicStaticFields + " ("+((publicStaticFields*100.0) / publicFields)+" percent of public fields)");
	   Debug.out.println("Public static non-final fields: " + publicStaticNonFinalFields + " ("+((publicStaticNonFinalFields*100.0) / publicStaticFields)+" percent of public static fields)");
	   Debug.out.println("                              ("+((publicStaticNonFinalFields*100.0) / allFields)+" percent of all fields)");
       }


    static void interfaceDependencies(Vector v, ClassManager classManager, String classname) {

	ClassData rootClass = classManager.findClass(classname);

	MethodData[] m = rootClass.getMethodData();
	for(int i=0; i<m.length; i++) {
	    if (!(m[i].isPublic() || (m[i].isProtected() && rootClass.isPublic()))) continue;

	    BasicTypeDescriptor[] p = m[i].getParameterTypes();
	    BasicTypeDescriptor ret = m[i].getReturnType();

	    if (ret.isClass()) {
		String cn = ret.getClassName();
		ClassData retClass = classManager.findClass(cn);
		//Debug.out.println("*"+cn);
		if (retClass==null) {
		    throw new Error("Return type of method "+classname+"."+m[i].getName()+" not found: "+cn);
		}
		if (retClass.isPublic()) {
		    if (add(v,cn))
			interfaceDependencies(v, classManager, cn);
		}
	    }

	    for(int j=0; j<p.length; j++) {
		if (p[j].isClass()) {
		    String cn = p[j].getClassName();
		    //    Debug.out.println("*"+cn);
		    ClassData parClass = classManager.findClass(cn);
		    if (parClass.isPublic()) {
			if (add(v,cn))
			    interfaceDependencies(v, classManager, cn);
		    }
		}
	    }


	}


	// public fields and protected fields in public classes 
	FieldData[] f = rootClass.getFields();
	for(int i=0; i<f.length; i++) {
	    if (!(f[i].isPublic() || (f[i].isProtected() && rootClass.isPublic()))) continue;

	    String cn = f[i].getType();
	    if ((cn=addType(v,cn))!=null)
		interfaceDependencies(v, classManager, cn);

	}
    }

    static void implementationDependencies(Vector v, ClassManager classManager, String classname) {
	ClassData rootClass = classManager.findClass(classname);
	if (rootClass == null) {
	    throw new Error("Cannot find class "+classname);
	}

	ConstantPool cp = rootClass.getConstantPool();
	int n = cp.getNumberOfEntries();
	cploop:
	for(int i=0; i<n; i++) {
	    ConstantPoolEntry e = cp.constantEntryAt(i);
	    int t = e.getTag(); 
	    switch( t ) {
	    case -1:
	    case ConstantPoolEntry.CONSTANT_UTF8:
	    case ConstantPoolEntry.CONSTANT_INTEGER:
	    case ConstantPoolEntry.CONSTANT_FLOAT:
	    case ConstantPoolEntry.CONSTANT_LONG:
	    case ConstantPoolEntry.CONSTANT_DOUBLE:
	    case ConstantPoolEntry.CONSTANT_STRING: 
	    case ConstantPoolEntry.CONSTANT_NAMEANDTYPE: 
		break;

	    case ConstantPoolEntry.CONSTANT_FIELDREF: {
		ClassMemberCPEntry cp0 = (ClassMemberCPEntry)e;

		String cn = cp0.getClassName();
		if ((cn=addNameOrArray(v,cn))!=null)
		    implementationDependencies(v, classManager, cn);

		cn = cp0.getMemberTypeDesc();
		if ((cn=addType(v,cn))!=null)
		    implementationDependencies(v, classManager, cn);

		break;
	    }

	    case ConstantPoolEntry.CONSTANT_INTERFACEMETHODREF:
	    case ConstantPoolEntry.CONSTANT_METHODREF: {
		ClassMemberCPEntry cp0 = (ClassMemberCPEntry)e;

		String cn = cp0.getClassName();
		if ((cn=addNameOrArray(v,cn))!=null)
		    implementationDependencies(v, classManager, cn);

		MethodTypeDescriptor td = new MethodTypeDescriptor(cp0.getMemberTypeDesc());

		String[]types= td.getArgumentTypeDesc();
		for(int k=0; k<types.length; k++) {
		    if ((cn=addType(v,types[k]))!=null)
			implementationDependencies(v, classManager, cn);
		}

		String ret= td.getReturnTypeDesc();
		if ((cn=addType(v,ret))!=null)
		    implementationDependencies(v, classManager, cn);

		break;
	    }

	    case ConstantPoolEntry.CONSTANT_CLASS: {
		ClassCPEntry cp0 = (ClassCPEntry)e;
		String cn = cp0.getClassName();
		if ((cn=addNameOrArray(v,cn))!=null)
		    implementationDependencies(v, classManager, cn);
		break;
	    }
	    default: {
		Debug.out.println("UNKNOWN TAG: "+t);
		throw new Error();
	    }
	    }
	}
    }

    static void compatibility(Vector v, ClassManager classManager, String zipname) {
	try {
	    Memory newerZip = getZIP(memMgr, zipname);
	    ClassManager newerClassManager = new ClassManager();
	    newerClassManager.addFromZip(null, new Memory []{newerZip});
	    Debug.out.println("Check if "+zipname+" is binary compatible");
	    Iterator iter = classManager.getAllClasses();
	    while(iter.hasNext()) {
		ClassData oldClass = (ClassData)iter.next();
		if (! oldClass.isPublic()) continue;  // we are only interested in public classes
		Debug.out.println(oldClass.getName());
		ClassData newClass = newerClassManager.findClass(oldClass.getName());
		
		// CLASS PROPERTIES
		// existance
		if (newClass==null) throw new IncompatibleException();
		// nonabstract -> abstract
		if (newClass.isAbstract() && ! oldClass.isAbstract()) throw new IncompatibleException();
		// final -> nonfinal
		if (! newClass.isFinal() &&  oldClass.isFinal()) throw new IncompatibleException();
		// public -> nonpublic
		if (! newClass.isPublic() &&  oldClass.isPublic()) throw new IncompatibleException();
		// superclasses: order is not important but removing is not allowed 
		// ...

		// FIELD PROPERTIES
		FieldData[] oldfields = oldClass.getFields();
		FieldData[] newfields = newClass.getFields();
		for(int i=0; i<oldfields.length; i++) {
		    if(oldfields[i].isPublic() || oldfields[i].isProtected()) {
			// field must exist in new class and be public or protected
			boolean found=false;
			for(int j=0; j<newfields.length; j++) {
			    if (oldfields[i].getName().equals(newfields[j].getName())) {
				if (oldfields[i].getType().equals(newfields[j].getType())) {
				    if(oldfields[i].isPublic() && ! newfields[j].isPublic())
					throw new IncompatibleException();
				    if(oldfields[i].isProtected() && ! (newfields[j].isPublic()||newfields[j].isProtected()))
				       throw new IncompatibleException("Field "+oldfields[i].getName()+" of class "+oldClass.getName()+" is protected in old class and private in new class");
				    if(newfields[j].isPrivate() && ! oldfields[i].isPrivate())
				       throw new IncompatibleException();
				    found=true;
				    break;
				} else {
				    throw new IncompatibleException();
				}
			    }
			}
			if (!found) {
			    Debug.out.println("Field "+oldfields[i].getName()+" of class "+oldClass.getName()+
					      " not found in new class");
			    throw new IncompatibleException();
			}
		    }
		}


		// METHOD PROPERTIES
		MethodData[] oldMethods = oldClass.getMethodData();
		MethodData[] newMethods = newClass.getMethodData();
		for(int i=0; i<oldMethods.length; i++) {
		    //if (!(oldMethods[i].isPublic() || (oldMethods[i].isProtected() && oldClass.isPublic()))) continue;
		    if (oldMethods[i].isPrivate()) continue;
		    if (!(oldMethods[i].isPublic() || oldMethods[i].isProtected())) continue;
		    if (oldMethods[i].getName().equals("<clinit>")) continue;
		    boolean found=false;
		    for(int j=0; j<newMethods.length; j++) {
			if (oldMethods[i].getName().equals(newMethods[j].getName())) {
			    found=true;
			    break;
			}
		    }
		    if (!found) {
			Debug.out.println("Method "+oldMethods[i].getName()+" of class "+oldClass.getName()+
					  " not found in new class");
			if(oldMethods[i].isProtected()) Debug.out.println("Method is protected.");
			else if(oldMethods[i].isPublic()) Debug.out.println("Method is public.");
			else if(oldMethods[i].isPrivate()) Debug.out.println("Method is private.");
			else Debug.out.println("Method has default access.");
			throw new IncompatibleException();
		    }

		}
	    }
	    


	} catch(Exception e) {
	    Debug.out.println(e);
	    e.printStackTrace();
	    throw new Error();
	}
	Debug.out.println("OK");
    }



    static String addType(Vector v, String cn) {
	if (cn.charAt(0) == 'L') 
	    return addNameOrArray(v, cn.substring(1, cn.length()-1));
	if (cn.charAt(0) == '[') 
	    return addNameOrArray(v, cn);
	return null;
    }

    static String addNameOrArray(Vector v, String cn) {
	while (cn.charAt(0) == '[') {
	    if (cn.charAt(1) == 'L') {
		Debug.out.println("ARRAY "+cn);
		cn = cn.substring(2, cn.length()-1);
	    } else if (cn.charAt(1) == '[') {
		Debug.out.println("ARRAY "+cn);
		cn = cn.substring(1, cn.length());
	    } else {
		return null;
	    }
	}
	if (add(v,cn)) return cn;
	return null;
    }

    static boolean add(Vector v, String cn) {
	for(int i=0; i<v.size(); i++) {
	    String s = (String)v.elementAt(i);
	    if (s.equals(cn)) return false;
	}
	Debug.out.println(cn);
	v.addElement(cn);
	return true;
    }

    static String getPackage(String s) {
	char [] c = s.toCharArray();
	for(int i=c.length-1; i>0; i--) {
	    if (c[i]=='.' || c[i]=='/') {
		return new String(c, 0, i);
	    }
	}
	return "";
    }

    static void print(Vector v) {
	for(int i=0; i<v.size(); i++) {
	    String s = (String)v.elementAt(i);
	    Debug.out.println(" "+s);
	}
	Debug.out.println(" "+v.size());
	
	Vector p = new Vector();
	for(int i=0; i<v.size(); i++) {
	    String s = (String)v.elementAt(i);
	    String pa = getPackage(s);
	    boolean found=false;
	    for(int j=0; j<p.size(); j++) {
		PackData sx = (PackData)p.elementAt(j);	    
		if (sx.name.equals(pa)) {
		    found = true;
		    sx.num++;
		    break;
		}
	    }	    
	    if (! found) {
		p.addElement(new PackData(pa,1));
	    }
	}

	for(int i=0; i<p.size(); i++) {
	    PackData s = (PackData)p.elementAt(i);	    
	    Debug.out.println(s.num+" "+s.name);
	}

	
    }

    static class PackData { 
	String name; int num;
	PackData(String n, int nu) { name=n; num=nu;}
    }
}

class MyClass {
    String name;
    String subclassOf;
    Vector methods = new Vector();
    Hashtable findmethods=new Hashtable();
    Vector pack=new Vector();
    String className;
    String dirName;
}

class MyMethod {
    String name,type;
    boolean isStatic;
}

class IncompatibleException extends Exception {
    IncompatibleException(){}
    IncompatibleException(String m){super(m);}
}
