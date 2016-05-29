package jx.classfile; 

import java.io.*; 
import jx.classfile.constantpool.*; 
import jx.classfile.datatypes.*; 
import jx.zero.Debug; 

/** 
    All data about a class, that can be found 
    in a class file. This is a rather passive class. 
    It only reads the data from the class file and 
    stores it. 
*/ 
public class ClassData extends ClassSource {

    final static boolean debugRead = false;

    ConstantPool constantPool; 

    int accessFlags; 
    int thisClassCPIndex; 
    int superClassCPIndex; 

    int numInterfaces; 
    int[] interfaceCPIndex; 

    int numFields; 
    FieldData[] field; 

    int numMethods; 
    MethodData[] method; 

    String sourceFile;

    private boolean allowNative = false;

      public int getThisClassCPIndex() {
	return thisClassCPIndex;
    }

    public ClassData(DataInput input) throws IOException,EOFException {
	readFromClassFile(input); 
    }

    public ClassData(DataInput input, boolean allowNative) throws IOException,EOFException {
	this.allowNative = allowNative;
	readFromClassFile(input); 
    }
  
    public ConstantPool getConstantPool() {return constantPool;}
  
    public MethodSource getMethod(String methodName, String methodType) {
	return getMethodData(methodName, methodType); 
    }


    public MethodData[] getMethodData() {
	return method;
    }

    public MethodData getMethodData(String methodName, String methodType) {
	int i=0; 
	while (i < numMethods) {
	    if (method[i].getMethodName().equals(methodName) && 
		method[i].getMethodType().equals(methodType)) 
		return method[i]; 
	    i++;
	}
	Debug.throwError("Wrong Method Mame or Type"); 
	return null; 
    }

    //same as getMethodData but without the throwError
    public MethodData getMethodDataNE(String methodName, String methodType) {
	int i=0; 
	while (i < numMethods) {
	    if (method[i].getMethodName().equals(methodName) && 
		method[i].getMethodType().equals(methodType)) 
		return method[i]; 
	    i++;
	}
	return null; 
    }


    //neu, fuer Verifier
    public void addMethod(MethodData newMethod) {
	if (newMethod == null)
	    Debug.throwError("Can't add null-Method to ClassData Object");
	numMethods++;
	MethodData oldMethod[] = method;
	method = new MethodData[numMethods];
	for (int i = 0; i < numMethods-1; i++) {
	    method[i] = oldMethod[i];
	}
	method[numMethods-1] = newMethod;
    }


    public void readFromClassFile(DataInput input) throws IOException, EOFException, NoMagicNumberException {
	int magicNumber = input.readInt(); 
	try {
	    if(magicNumber!=0xcafebabe) throw new NoMagicNumberException();
	    int minorVersion = input.readUnsignedShort();
	    int majorVersion = input.readUnsignedShort(); 

	    //Debug.out.println("version "+majorVersion+"."+minorVersion);
    
	    constantPool = new ConstantPool(); 
	    constantPool.readFromClassFile(input); 

	    //Debug.out.println(constantPool.toString()); 
    
	    accessFlags = input.readUnsignedShort(); 
	    thisClassCPIndex = input.readUnsignedShort(); 
	    superClassCPIndex = input.readUnsignedShort(); 
    
	    constantPool.setThisClassCPIndex(thisClassCPIndex); 

	    //Debug.out.println("** Class was "+constantPool.getClassName());
	    //Debug.out.println("");

	    numInterfaces = input.readUnsignedShort(); 
	    interfaceCPIndex = new int[numInterfaces]; 
	    for(int i=0; i<numInterfaces;i++) 
		interfaceCPIndex[i] = input.readUnsignedShort(); 

	    numFields = input.readUnsignedShort(); 
	    field = new FieldData[numFields]; 
	    for(int i=0; i<numFields; i++) {
		field[i] = new FieldData(this, input, constantPool); 
	    }

	    numMethods = input.readUnsignedShort(); 
	    method = new MethodData[numMethods];
	    if (debugRead) Debug.out.println("Class:"+constantPool.classEntryAt(thisClassCPIndex).getClassName());
	    for(int i=0; i<numMethods; i++) {
		try {
		    method[i] = new MethodData(this, input, constantPool, allowNative); 
		    if (debugRead) Debug.out.println("METHOD "+i+": "+method[i].getMethodName());
		} catch(NativeMethodException e) {
		    Debug.out.println("Class:"+constantPool.classEntryAt(thisClassCPIndex).getClassName());
		    throw e;
		}
	    }

	    int numAttributes = input.readUnsignedShort(); 
	    for(int i=0; i<numAttributes; i++) 
		readAttribute(input, constantPool); 
	} catch(EOFException ex) {
	    throw new Error("Unexpected EOF");
	}
    }

    private void readAttribute(DataInput input, ConstantPool cPool) 
	throws IOException {
	int attrNameCPIndex = input.readUnsignedShort(); 
	int numBytes = input.readInt(); 
	String attrName = constantPool.getUTF8StringAt(attrNameCPIndex);
	if (attrName.equals("SourceFile")) {
	    int sourceFileCPIndex = input.readUnsignedShort();
	    sourceFile = constantPool.getUTF8StringAt(sourceFileCPIndex);
	} else {
	    input.skipBytes(numBytes); 
	}
    }

    private static final String[] accessFlagsString = {
	"public", "private", "protected", "static", 
	"final", "synchronized", "volatile", "transient", 
	"native", "interface", "abstract" 
    }; 

    public static String getAccessFlagsString(int accessFlags) {
	String s = ""; 
	boolean isFirstWord = true; 
	for( int i=0; i<accessFlagsString.length; i++) {
	    if ((accessFlags & (1 << i)) != 0) {
		if (!isFirstWord) s += ", "; 
		s += accessFlagsString[i]; 
		isFirstWord = false; 
	    }
	}
	return s; 
    }
    
    public int getAccessFlags() { return accessFlags;}
    //    public boolean isInterface() {return isInterface(accessFlags);}

    public String getClassName() {
	return constantPool.getClassName(); 
    }

    public String getSuperClassName() {
	if (superClassCPIndex == 0) { // Object's superclass
	    return null;
	}

	ClassCPEntry superClassCP = constantPool.classEntryAt(superClassCPIndex);
	return superClassCP.getClassName();
    }		

    public String[] getInterfaceNames() {
	String ifs[] = new String[numInterfaces];
	for(int j=0; j<numInterfaces; j++) {
	    ClassCPEntry ifCP = constantPool.classEntryAt(interfaceCPIndex[j]); 
	    ifs[j] = ifCP.getClassName();
	}
	return ifs;
    }  
  
    public FieldData[] getFields() {
	return field;
    }

    public String getSourceFileAttribute() {
	return sourceFile;
    }

    public void dump() {
	Debug.out.println("ClassData for class: "+getClassName());
	Debug.out.println("   Superclass: " + getSuperClassName());
	Debug.out.print("   Implements: ");
	String[] ifs = getInterfaceNames();
	for(int i=0; i<ifs.length; i++) Debug.out.print(ifs[i]+ " ");
	Debug.out.println();
    }

    public String getJavaLanguageName() {
	return getClassName().replace('/','.');
    }
    /* ------
     * Java Reflection "compatible" methods
     */
    public final FieldData[] getDeclaredFields() {
	return getFields();
    }
    public final String getName() {
	return getClassName();
    }
    public final MethodSource[] /* should be MethodData */  getMethods() {
	return method;
    }
    public final FieldData getField(String name) {
	for(int i=0; i<field.length; i++) {
	    if (field[i].getName().equals(name)) return field[i];
	} 
	return null;
    }

    public boolean isPublic()    {return ClassData.isPublic(accessFlags);} 
    public boolean isPrivate()   {return ClassData.isPrivate(accessFlags);}
    public boolean isProtected() {return ClassData.isProtected(accessFlags);}
    public boolean isStatic()    {return ClassData.isStatic(accessFlags);}
    public boolean isFinal()     {return ClassData.isFinal(accessFlags);}
    public boolean isAbstract()  {return ClassData.isAbstract(accessFlags);}
    public boolean isNative()    {return ClassData.isNative(accessFlags);}
    public boolean isSynchronized() {return ClassData.isSynchronized(accessFlags);}
    public boolean isInterface() {return ClassData.isInterface(accessFlags);}
    public boolean isVolatile()  {return ClassData.isVolatile(accessFlags);}
    public boolean isTransient() {return ClassData.isTransient(accessFlags);}
}


