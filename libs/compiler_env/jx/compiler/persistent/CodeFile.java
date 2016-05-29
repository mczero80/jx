/*
 * representation of a file that contains the
 * compiled code of a domain
 *
 * Copyright 2000 Michael Golm
 */
package jx.compiler.persistent;

import java.util.*;

import jx.classfile.constantpool.*; 
import jx.classfile.*; 

import jx.zero.Debug;
import jx.zero.Memory;

import java.util.Vector;
import java.util.Enumeration;

import java.io.PrintStream;
import java.io.IOException;

import jx.compiler.*;
import jx.compiler.execenv.*;
import jx.compiler.symbols.*;

import jx.collections.Iterator;
import jx.collections.Collection;

import jx.compspec.MetaInfo;


    /**
     * Format of codefile:

     * FILE              ::= HEADER  code

     * HEADER            ::= FORMATVERSION PROCESSOR NUMBER_OF_OPTS OPT* ... NUMBER_OF_LIBS LIB*  METAINFO STRINGTABLE NUMBER_OF_CLASSES CLASSHEADER*
     * FORMATVERSION     ::= int (currently 1)
     * PROCESSOR         ::= [ "x86" "P6" "x86profile" ]
     * NUMBER_OF_OPTS    ::= int == 0
     * OPT               ::= !!! reserved for future work !!!
     * NUMBER_OF_LIBS    ::= int
     * LIB               ::= string
     * METAINFO          ::= SIZE_OF_META META
     * SIZE_OF_META      ::= int
     * META              ::= bytes
     * NUMBER_OF_CLASSES ::= int

     * CLASSHEADERx      ::= CLASSNAME SUPERCLASSNAME ISINTERFACE 
                             NUMBER_OF_IMPLEMENTED_INTERFACES INTERFACENAME*
			     NUMBER_OF_METHODS INSTANCE_SIZE INSTANCE_FIELDMAP FIELDLIST STATICFIELDS_SIZE BYTECODESIZE VTABLE METHODHEADER*
     * CLASSNAME         ::= string
     * ISINTERFACE        ::= int (0=class, 1=interface)
     * NUMBER_OF_IMPLEMENTED_INTERFACES ::= int
     * INTERFACENAME        ::= string
     * STATIC_FIELDS_SIZE ::= int
     * INSTANCE_SIZE     ::= int
     * INSTANCE_FIELDMAP ::= NUMBER_OF_FIELDMAPBYTES bytes 
     * NUMBER_OF_FIELDMAPBYTES ::= int 
     * FIELDLIST          ::= NUMBER_OF_FIELDS FIELD*
     * NUMBER_OF_FIELDS ::= int 
     * FIELD             ::= FIELDNAME FIELDTYPE FIELDOFFSET
     * FIELDNAME        ::= string
     * FIELDTYPE        ::= string
     * FIELDOFFSET        ::= int
     * NUMBER_OF_METHODS ::= int

     * METHODHEADER      ::= METHODNAME METHODSIGNATURE 
                             SIZE_LOCAL_VARS NUMBER_CODEBYTES ARGUMENTMAP RETURNTYPE FLAGS
                             NUMBER_OF_SYMBOLS SYMBOL* LINENUMBERINFO

     * ARGUMENTMAP       ::= NUMBER_OF_ARGUMENTMAPBYTES bytes
     * NUMBER_OF_ARGUMENTMAPBYTES ::= int 
     * RETURNTYPE        ::= int
     * FLAGS             ::= int

     * LINENUMBERINFO    ::= NUMBER_OF_LINES LINEINFO*
     * LINEINFO          ::= BCINDEX NATIVESTART NATIVEEND

     * METHODNAME        ::= string
     * METHODSIGNATURE   ::= string
     * NUMBER_CODEBYTES  ::= int
     * NUMBER_OF_SYMBOLS ::= int
     * SYMBOLx           ::= refer to SymbolTableEntryBase
     */


public class CodeFile {

    public static final int VERSION = 9;
    public static final boolean verbose = false;

    ExtendedDataInputStream in;
    ExtendedDataOutputStream out;

    CompilerOptions opts = null;

    MetaInfo meta;

    /**
     * Write all classes contained in the vector to
     * the file.
     */

    public CodeFile(CompilerOptions opts, MetaInfo meta) {
	this.opts = opts;
	this.meta = meta;
    }

    //public void write(ExtendedDataOutputStream out, ClassStore classStore, CompilerOptions opts) throws IOException {
    public void write(ExtendedDataOutputStream out, ClassStore classStore) throws IOException {    
	
	this.out = out;
        
        //
        // write header
        //

	out.writeInt(VERSION);
	out.writeString(opts.codeType());

	/*
	  symbol - base - table
	*/

	out.writeInt(0);

	//
	//  needed libs
	//
	
	String[] libs = opts.getNeededLibs();
	if (libs==null) {
	    //	    Debug.out.println("NO LIBS NEEDED");
	  out.writeInt(0);
	} else {
	    //	    Debug.out.println("NEEDED LIBS");
	  out.writeInt(libs.length);
	  for (int i=0;i<libs.length;i++) {
	    try {
		//		Debug.out.println("\""+libs[i]+"\"");
	      out.writeString(libs[i]);
	    } catch (Exception ex) {
	      throw new Error(ex.getClass().getName());
	    }
	  
	  }
	}

	//
	//  metainfo
	//
	String []vars = meta.getVars();
	out.writeInt(vars.length);
	for(int i=0; i<vars.length;i++) {
	    //Debug.out.println("  "+vars[i]+"   = "+meta.getVar(vars[i]));
	    out.writeString(vars[i]);	    
	    out.writeString(meta.getVar(vars[i]));	    
	}

	//
	// write string table
	//

	StringTable stringTable = new StringTable();
	Iterator iter = classStore.iterator();
	while (iter.hasNext()) {
	    BCClass c = (BCClass) iter.next();
	    collectStrings(c,stringTable);
	}
	stringTable.writeStringTable(out);

	//
	//  vmsupport-base-table
	//
	
	VMSupportSTEntry.writeSymTable(out);

	//
	// write header
	//
	  
	out.writeInt(classStore.size());
	iter = classStore.iterator();
	while(iter.hasNext()) {
	    BCClass c = (BCClass) iter.next();
	    saveToFile(c,stringTable,true);
	}

	//
        // write code
	//

	iter = classStore.iterator();
	while(iter.hasNext()) {
	    BCClass c = (BCClass) iter.next();
	    saveToFile(c,stringTable,false);
	}

	out.writeChecksum();

	if (verbose) Debug.out.println("**********Finished saving!");
    }

    /**
     * Reads a CodeFile form a input stream.
     */
    public Vector read(ExtendedDataInputStream in) throws Exception {

	this.in = in;

	Vector all = new Vector();
	
	// 
        // read header
	//

	if (in.readInt() != VERSION) {
	    Debug.throwError("Wrong version");
	}
	String codeType = in.readString();
	int size = in.readInt();
	for(int i=0; i<size; i++) {
	    CompiledClass compiledClass = readHeaderFromFile();
	    all.addElement(compiledClass);
	}

	//
	// read code
	//

	for(int i=0; i<size; i++) {
	    readCodeFromFile((CompiledClass)all.elementAt(i));
	}	
	return all;
    }

    private void collectStrings(BCClass aClass, StringTable strTable) {
	BinaryCode mc=null;
	BCMethod   method=null;

	BCClassInfo info = (BCClassInfo)aClass.getInfo();

	strTable.register(aClass.getClassName());
	if (info.superClass != null) {
	    strTable.register(info.superClass.getClassName());
	} else {
	    strTable.register("");
	}

	for(int i=0; i<info.interfaces.length; i++) {
	    strTable.register(info.interfaces[i].getClassName());
	}

	info.objectLayout.registerStrings(strTable);
	info.classLayout.registerStrings(strTable);
	info.methodTable.registerStrings(strTable);

	int len = info.nativeCode.length;
	for(int i=0; i<len; i++) {
	    method = info.methods[i];

	    strTable.register(method.getName());
	    strTable.register(method.getSignature());

	    if (method.isAbstract() || info.nativeCode[i]==null) { 
		continue;
	    } else {
		mc     = info.nativeCode[i].getMachineCode();
		Vector unresolvedAddresses = mc.getUnresolvedAddresses();
		Enumeration elements = unresolvedAddresses.elements(); 		
		while (elements.hasMoreElements()) {
		    SymbolTableEntryBase entry =(SymbolTableEntryBase)elements.nextElement();
		    entry.registerStrings(strTable);
		}
	    }
	}
    }

    private void saveToFile(BCClass aClass, StringTable strTable, boolean saveHeader) throws IOException {
	BCClassInfo info = (BCClassInfo)aClass.getInfo();
	if (saveHeader) { 
	    if (verbose) Debug.out.println("***** Saving header class: " + aClass.getClassName());
	    //out.writeString(aClass.getClassName());
	    strTable.writeStringID(out,aClass.getClassName());
	    if (info.superClass != null) {
		//out.writeString(info.superClass.getClassName());
		strTable.writeStringID(out,info.superClass.getClassName());
	    } else {
		//out.writeString("");
		strTable.writeStringID(out,"");
	    }
	    if (aClass.isInterface()) {
		out.writeInt(1); // isinterface
	    } else {
		out.writeInt(0); // isinterface
	    }
	    out.writeInt(info.interfaces.length); // number of implemented interfaces
	    for(int i=0; i<info.interfaces.length; i++) {
		//out.writeString(info.interfaces[i].getClassName());
		strTable.writeStringID(out,info.interfaces[i].getClassName());
	    }
	    out.writeInt(info.nativeCode.length);
	    out.writeInt(info.objectLayout.wordsNeeded());
	    // fieldmap
	    info.objectLayout.writeFieldMap(out);
	    // fieldlist
	    info.objectLayout.writeFieldList(out,strTable);
	    // statics
	    out.writeInt(info.classLayout.wordsNeeded());
	    info.classLayout.writeFieldMap(out);

	    /* accumulated bytecode size */
	    //Debug.out.println("BYTECODES-CLASS-START "+aClass.getClassName());
	    if (aClass.isInterface()) {
		out.writeInt(0); // no bytecode in an interface
	    } else {
		int bcsize=0;
		for(int i=0; i<info.methods.length; i++) {
		    if (info.methods[i] instanceof BCMethodWithCode) {
			BCMethodWithCode b = (BCMethodWithCode)info.methods[i];
			bcsize += b.getByteCodeSize();
			//Debug.out.println("BYTECODES of "+b.getName()+b.getSignature()+":"+b.getByteCodeSize());
		    }
		}
		//Debug.out.println("BYTECODES-CLASS "+aClass.getClassName()+"="+bcsize);
		out.writeInt(bcsize); 
	    }

	    info.methodTable.serialize(out,strTable);
	}
	int len = info.nativeCode.length;
	for(int i=0; i<len; i++) {
	    //for (int i=0;i<info.methods.length;i++) {
	    if (saveHeader) {		
	        if (verbose) Debug.out.println("**  Saving header method "+i+"/"+len+" "
					    +aClass.getClassName()+"."+info.methods[i].getName());
		saveHeaderToFile(strTable, aClass.getClassName(), info.methods[i].getName(), 
				 info.methods[i].getSignature(), info.nativeCode[i], info.methods[i], info);
	    } else {
	        if (verbose) Debug.out.println("**  Saving code method "+i+" "+info.methods[i].getName());
		saveCodeToFile(info.methods[i], info.nativeCode[i]);
	    }
	}
    }

    private void saveHeaderToFile(StringTable strTable, String className, String methodName, String methodType,
				  NativeCodeContainer nativeCode, BCMethod method, BCClassInfo info) {
	BinaryCode mc=null;

	try {
	    //out.writeString(methodName);
	    //out.writeString(methodType);
	    strTable.writeStringID(out,methodName);
	    strTable.writeStringID(out,methodType);

	    if (method.isAbstract()) {
		out.writeInt(0);
		out.writeInt(0);
	    } else {
		out.writeInt(nativeCode.getLocalVarSize());
		mc = nativeCode.getMachineCode(); 
		int numCodeBytes = mc.getNumCodeBytes();
		if (numCodeBytes <= 0) {
		    Debug.throwError("Fatal error: Number of code bytes <=0");
		}
		out.writeInt(numCodeBytes);
	    }

	    // write argument map
	    int[] argtypes = method.getArgumentTypes();
	    TypeMap.writeMap(out, argtypes);

	    // write return type (0=numeric; 1=reference)
	    if (method.returnsReference()) {
		out.writeInt(1);
	    } else {
		out.writeInt(0);
	    }

	    // write flags (static, etc.)
	    if (method.isStatic()) {
		out.writeInt(1);
	    } else {
		out.writeInt(0);
	    }

	    if (method.isAbstract() || nativeCode==null) { 
		out.writeInt(0); // number of symbols
		out.writeInt(0); // number of lineinfos
		out.writeInt(0); // number of lineinfos
	    } else {
		Vector unresolvedAddresses = mc.getUnresolvedAddresses();
		int numEntries=0;
		Enumeration elements = unresolvedAddresses.elements(); 
		while (elements.hasMoreElements()) {
		    SymbolTableEntryBase entry =(SymbolTableEntryBase)elements.nextElement(); 
		    //Debug.out.println("* SymTableEntry:");
		    //entry.dump();
		    if(! entry.isResolved()) {
			if ((entry instanceof UnresolvedJump)
			    && entry.isRelative()) {
			    Debug.out.println("ERROR: relative Jump Entry not resolved: Class: "+className+", Method: "+method);
			    entry.dump();
			    Debug.throwError("unresolved jump: "+entry);
			}
			numEntries++;
		    }
		}
		out.writeInt(numEntries);
		elements = unresolvedAddresses.elements(); 
		while (elements.hasMoreElements()) {
		    SymbolTableEntryBase entry =(SymbolTableEntryBase)elements.nextElement(); 
		    if(entry.isResolved()) {
			//Debug.out.println("Skipping resolved entry: "+entry);
		    } else {
			//Debug.out.println("Wrinting to file: "+entry);
			entry.writeEntry(out); 
		    }
		}
		// write  nativecode -> bytecode mapping
		if (! opts.isOption("noBytecodeNumbers")) {
		    Vector lineTable = nativeCode.getInstructionTable();
		    out.writeInt(lineTable.size());
		    for(int k=0; k<lineTable.size(); k++) {
			int[] l = (int[])lineTable.elementAt(k);
			out.writeInt(l[0]);
			out.writeInt(l[1]);
			out.writeInt(l[2]);
		    }
		} else { 
		    out.writeInt(0); // no lineinfos available
		}
		// write  bytecode -> sourcecode mapping
		LineAttributeData lineNumbers[] = method.getLineNumberTable();
		//Debug.out.println("LINENUMBERS: "+lineNumbers.length);
		if (lineNumbers != null && ! opts.isOption("noLineNumbers")) {
		    out.writeInt(lineNumbers.length); // number of lineinfos
		    for(int k=0; k<lineNumbers.length; k++) {
			out.writeInt(lineNumbers[k].startBytecodepos);
			out.writeInt(lineNumbers[k].lineNumber);
		    }
		} else {
		    out.writeInt(0); // no lineinfos available
		}
	    }
	} catch (Exception e) {
	  Debug.throwError("Exception "+e.getClass().getName()+" caught while writting code file.");
	}

    }
    private void saveCodeToFile(BCMethod method, 
				//IMCode nativeCode) {
				NativeCodeContainer nativeCode) {
	try {
	    if (method.isAbstract() || nativeCode==null) {
		return;
	    }
	    BinaryCode mc = nativeCode.getMachineCode();
	    byte[] code = mc.getCode();
	    int numCodeBytes = mc.getNumCodeBytes();
	    // apply the symbols that already are resolved
	    Vector unresolvedAddresses = mc.getUnresolvedAddresses();
	    Enumeration elements = unresolvedAddresses.elements(); 
	    while (elements.hasMoreElements()) {
		SymbolTableEntryBase entry =(SymbolTableEntryBase)elements.nextElement();
		if(entry.isRelative()) {
		    if(entry.isResolved()) {
			// this entry must be applied before the code is saved
			entry.apply(code, 0);
		    } 
		}
	    }

	    out.write(code,0,numCodeBytes);
	    //Disassembler disass = new Disassembler(code, 0, numCodeBytes);
	    //disass.disasm();
	} catch(IOException e) {
	    Debug.throwError("Error writing code file");
	}
    }






    private CompiledClass readHeaderFromFile() throws Exception {
	String className = in.readString();
	String superName = in.readString();
	///Debug.out.println(className + " - " +superName);
	int isInterface = in.readInt();
	int numberOfMethods = in.readInt();
	int vtableSize = in.readInt();
	int objectSize = in.readInt();
	FieldLayout.readMap(in); // object map
	int classSize = in.readInt();
	FieldLayout.readMap(in); // statics map
	int numberOfBytecodes = in.readInt();
	//Debug.out.println("Ml " +numberOfMethods);	
	CompiledMethod[] methods = new CompiledMethod[numberOfMethods];
	for(int i=0; i<numberOfMethods; i++) {
	    methods[i] = readMethodHeaderFromFile();
	}
	CompiledClass compiledClass = new CompiledClass(className, superName, objectSize, classSize, methods);
	return compiledClass;
    }

    private CompiledMethod readMethodHeaderFromFile() throws Exception {
	String methodName = in.readString();
	String methodType = in.readString();
	//Debug.out.println("Method:"+methodName + " - " +methodType);
	int numCodeBytes = in.readInt();
	int vtableIndex = in.readInt(); // -1 means: no virtual method
	if (numCodeBytes > 0) { // not an abstract method
	}
	int numSymbols = in.readInt(); // number of symbols
	SymbolTableEntryBase[] symbols = new SymbolTableEntryBase[numSymbols];
	for(int i=0; i<numSymbols; i++) {
	    symbols[i] = SymbolTableEntryBase.readUnknownEntry(in);
	}
	int numLineInfo = in.readInt(); // number of lineinfos
	LineInfo[] lineTable = new LineInfo[numLineInfo];
	for(int k=0; k<numLineInfo; k++) {
	    LineInfo l = new LineInfo();
	    l.bytecodePos = in.readInt();
	    l.start = in.readInt();
	    l.end = in.readInt();
	    lineTable[k] = l;
	}
	return new CompiledMethod(methodName, methodType, numCodeBytes, vtableIndex, symbols, lineTable);
    }

    private void readCodeFromFile(CompiledClass compiledClass) throws IOException {
	CompiledMethod[] methods = compiledClass.getMethods();
	for(int i=0; i<methods.length; i++) {
	    readCodeFromFile(methods[i]);
	}
	
    }


    private void readCodeFromFile(CompiledMethod method) throws IOException {
	if (method.isAbstract()) {
	    return;
	}
	byte[] code = new byte[method.numCodeBytes];
	method.setCode(code);
	in.read(code,0,method.numCodeBytes);
    }


}
