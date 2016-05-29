package jx.rpcgen;

import java.io.*;
import java.util.*;

import jx.rpc.*;
import jx.classstore.*;
import jx.classfile.*;
import jx.classfile.datatypes.*;

import jx.zero.Debug;

class Formats {
    static final boolean dumpAll = false;
    public static final boolean debug = false;
    Hashtable formats = new Hashtable();
    ClassFinder classFinder;
    ClassInformation classInfo;
    ClassData rpcProcClass, rpcDataClass, rpcOptionalClass, rpcOpaqueClass;
    RPCGen rpcGen;
    Formats(RPCGen rpcGen, ClassFinder classFinder, ClassInformation classInfo) {
	this.rpcGen = rpcGen;
	this.classFinder = classFinder;
	this.classInfo = classInfo;
	rpcProcClass = classFinder.findClass("jx.rpc.RPCProc");
	rpcDataClass = classFinder.findClass("jx.rpc.RPCData");
	rpcOptionalClass = classFinder.findClass("jx.rpc.RPCOptional");
	rpcOpaqueClass = classFinder.findClass("jx.rpc.RPCOpaque");
	if (rpcOpaqueClass == null) throw new Error("RPCOpaque not found");
    }

    public void addRPCFormatType(BasicTypeDescriptor cl, String classModifier) {
	ClassData cla = classFinder.findClass(cl.getClassName());
	if (cla == null) {
	    throw new Error("Can not find class for basic type "+cl);
	}
	addRPCFormatType(cla, classModifier);
    }
    public void addRPCFormatType(ClassData cl, String classModifier) {
	if (! classInfo.doesImplement(cl, rpcDataClass)) {
	    throw new Error(cl.getJavaLanguageName()+ " must implement the RPCData interface");
	}
	ByteArrayOutputStream str = new ByteArrayOutputStream();
	PrintStream out = new PrintStream(str);

	ByteArrayOutputStream lstr = new ByteArrayOutputStream();
	PrintStream lou = new PrintStream(lstr);

	try {
	    if (formats.get(cl) != null) return;
	    formats.put(cl, cl);
	    String classname = cl.getJavaLanguageName();
	    if (dumpAll) Debug.out.println("RPCFormatter for "+classname);

	    out.println(classModifier+" class RPCFormatter"+Helper.classnameName(classname)+ " {");

	    FieldData[] fields = cl.getDeclaredFields();      

	    FieldData switchDefault = null;
	    Vector switches = new Vector();
	    Hashtable fixlens = new Hashtable();
	    Vector consts = getDefinedConstants(cl);
	    for(int i=0; i<consts.size(); i++) {
		if (dumpAll) Debug.out.println((String)consts.elementAt(i));
		FieldData f = cl.getField((String)consts.elementAt(i));
		int m = f.getModifiers();
		if (Modifier.isFinal(m)) {
		    if (Modifier.isStatic(m)) {
			// constant
			if(f.getName().startsWith("SWITCH_")) {
			    switches.addElement(f);
			} else if(f.getName().startsWith("SWITCHDEFAULT_")) {
			    switchDefault = f;
			} else if(f.getName().startsWith("FIXLEN_")) {
			    String fieldName = f.getName();
			    int index = fieldName.lastIndexOf("_");
			    fieldName = fieldName.substring(index+1);
			    fixlens.put(fieldName.intern(),f);
			}
			//if (dumpAll) Debug.out.println("+switch:"+f);
		    } else {
			Debug.out.println("only static(!) finals are supported:"+fields[i].getType());
			System.exit(1);	      
		    }
		}
	    }


	    /*
	     * write
	     * 
	     */
	    out.println("     public static void write(RPCBuffer buf, "+classname+" obj) {");

	    if(classInfo.doesImplement(cl, rpcOptionalClass)) {
		// terminates a list of elements
		out.println("        if (obj == null) {");
		out.println("           Format.writeInt(buf, 0);");  
		out.println("           return;");  
		out.println("        } else {");  
		out.println("           Format.writeInt(buf, 1);");  
		out.println("        }");  
	    }

	    if(classInfo.doesImplement(cl, rpcOpaqueClass)) {
		throw new Error();
		//out.println("        Format.writeInt(buf,length(obj));");
	    }

	    for(int i=0; i<fields.length; i++) {
		int m = fields[i].getModifiers();
		if (Modifier.isFinal(m)) {
		    if (Modifier.isStatic(m)) {
			// constant
		    } else {
			throw new Error("Error: final without static");
		    }
		} else {
		    rpcGen.genDataWriter(out,lou,fields[i].getBasicType(), "obj."+fields[i].getName(),fixlens,fields[i].getName(),classModifier,"buf", "");
		    /*
		      if (fields[i].getType().isPrimitive()) {
		      if (fields[i].getType() == Integer.TYPE) {
		      out.println("        Format.writeInt(buf,obj."+fields[i].getName()+");");
		      lou.println("        _l += Format.lengthInt(obj."+fields[i].getName()+");");
		      } else if (fields[i].getType() == Short.TYPE) {
		      out.println("        Format.writeShort(buf,obj."+fields[i].getName()+");");
		      lou.println("        _l += Format.lengthShort(obj."+fields[i].getName()+");");
		      } else {
		      if (dumpAll) Debug.out.println("only int supported:"+fields[i].getType());
		      System.exit(1);
		      }
		      } else if (fields[i].getType() == String.class) {
		      out.println("        Format.writeString(buf,obj."+fields[i].getName()+");");
		      lou.println("        _l += Format.lengthString(obj."+fields[i].getName()+");");
		      } else if (fields[i].getType().getComponentType() != null) {
		      Class arrayType = fields[i].getType().getComponentType();
		      if (arrayType == Byte.TYPE) {
		      FieldData flen = (FieldData) fixlens.get(fields[i].getName().intern());
		      if (flen != null) {
		      out.println("        Format.writeFixByteArray(buf,obj."+fields[i].getName()+","+flen.getDeclaringClass().getName()+"."+flen.getName()+");");
		      lou.println("        _l += Format.lengthFixByteArray(obj."+fields[i].getName()+","+flen.getDeclaringClass().getName()+"."+flen.getName()+");");
		      } else {
		      out.println("        Format.writeByteArray(buf,obj."+fields[i].getName()+");");
		      lou.println("        _l += Format.lengthByteArray(obj."+fields[i].getName()+");");
		      }
		      } else if (arrayType == Integer.TYPE) {
		      out.println("        Format.writeIntArray(buf,obj."+fields[i].getName()+");");
		      lou.println("        _l += Format.lengthIntArray(obj."+fields[i].getName()+");");
		      } else if (arrayType == Short.TYPE) {
		      out.println("        Format.writeShortArray(buf,obj."+fields[i].getName()+");");
		      lou.println("        _l += Format.lengthShortArray(obj."+fields[i].getName()+");");
		      } else {
		      System.out.println("only int/byte/short arrays supported:"+arrayType);
		      System.exit(1);
		      }
		      } else {
		      addRPCFormatType(fields[i].getType(),classModifier);
		      out.println("        RPCFormatter"+Helper.classnameName(fields[i].getType().getName())+".write(buf, obj."+fields[i].getName()+");");
		      lou.println("        _l += RPCFormatter"+Helper.classnameName(fields[i].getType().getName())+".length(obj."+fields[i].getName()+");");
		      }
		    */
		}
	    }

	
	    if(switches.size() > 0) { // UNION
		// generate switch
		FieldData switchField = fields[fields.length-1];
		if (switchField.getBasicType().isPrimitive()) {
		    if (switchField.getBasicType().isInteger()) {
			out.println("        switch(obj."+switchField.getName()+") {");
			for(int j=0; j<switches.size(); j++) {
			    FieldData f = (FieldData)switches.elementAt(j);
			    String subclassName = f.getName();
			    int index = subclassName.lastIndexOf("_");
			    subclassName = subclassName.substring(index+1);
			    ClassData subClass = classFinder.findClass(Helper.classnamePackage(cl.getJavaLanguageName())+"."+subclassName);
			    if (classInfo.getSuperClass(subClass) != cl) {
				System.out.println("union type error: wrong superclass of "+subClass);
				System.out.println("                  superclass must be "+cl);
				throw new Error();	      		    
			    }
			    addRPCFormatType(subClass,classModifier);
			    out.println("          case "+f.getDeclaringClass().getJavaLanguageName()+"." + f.getName() + ": RPCFormatter"+Helper.classnameName(subclassName)+".write(buf, ("+subclassName+")obj); break;");
			}
			if (switchDefault != null) {
			    String subclassName = switchDefault.getName();
			    int index = subclassName.lastIndexOf("_");
			    subclassName = subclassName.substring(index+1);
			    ClassData subClass = classFinder.findClass(Helper.classnamePackage(cl.getJavaLanguageName())+"."+subclassName);
			    addRPCFormatType(subClass,classModifier);
			    out.println("          default: RPCFormatter"+Helper.classnameName(subclassName)+".write(buf, ("+subclassName+")obj); break;");
			} else {
			    out.println("          default: throw new Error();");
			}
			out.println("        }");
		    } else {
			throw new Error("only int in RPCUnion supported:"+switchField.getType());
		    }
		}
      

	    } // UNION


	    out.println("     }"); // closing write bracket
      
	    /*
	     * read
	     * 
	     */
	    out.println("     public static " +classname + " read(RPCBuffer buf) {");
	    if (debug) out.println("          jx.zero.Debug.out.println(\"  Reading: "+ classname +"\");");
	    out.println("        "+classname +" obj;");
	    out.println("        obj = new "+classname+"();");


	    if(classInfo.doesImplement(cl, rpcOptionalClass)) {
		out.println("        int opt = Format.readInt(buf);");
		out.println("        if (opt == 0) return obj;");
	    }
      
	    String copyFields = "";
	    for(int i=0; i<fields.length; i++) {
		if (dumpAll) Debug.out.println(fields[i]);
		int m = fields[i].getModifiers();
		if (Modifier.isFinal(m)) {
		    if (Modifier.isStatic(m)) {
			// constant
		    } else {
			System.out.println("Error: final without static");
		    }
		} else {
		    if (fields[i].getBasicType().isPrimitive()) {
			if (fields[i].getBasicType().isInteger()) {
			    out.println("        obj."+fields[i].getName()+"= Format.readInt(buf);");
			} else if (fields[i].getBasicType().isShort()) {
			    out.println("        obj."+fields[i].getName()+"= Format.readShort(buf);");
			} else {
			    throw new Error("only int supported:"+fields[i].getType());
			}
		    } else if (fields[i].getBasicType().getJavaLanguageType().equals("java.lang.String")) {
			out.println("        obj."+fields[i].getName()+"= Format.readString(buf);");
			if (debug) {
			    out.println("        jx.zero.Debug.out.println(\""+fields[i].getName()+"=\"+obj."+fields[i].getName()+");");
			}
		    } else if (fields[i].getBasicType().getComponentType() != null) {
			BasicTypeDescriptor arrayType = fields[i].getBasicType().getComponentType();
			if (arrayType.isByte()) {
			    FieldData flen = (FieldData) fixlens.get(fields[i].getName().intern());
			    if (flen != null) {
				out.println("        obj."+fields[i].getName()+"= Format.readFixByteArray(buf,"+flen.getDeclaringClass().getJavaLanguageName()+"."+flen.getName()+");");
			    } else {
				out.println("        obj."+fields[i].getName()+"= Format.readByteArray(buf);");
			    }
			} else if (arrayType.isInteger()) {
			    out.println("        obj."+fields[i].getName()+"= Format.readIntArray(buf);");
			} else if (arrayType.isShort()) {
			    out.println("        obj."+fields[i].getName()+"= Format.readShortArray(buf);");
			} else {
			    System.out.println("only int/byte/short arrays supported:"+arrayType);
			    System.exit(1);
			}
		    } else {
			addRPCFormatType(fields[i].getBasicType(),classModifier);
			String jtype = fields[i].getBasicType().getJavaLanguageType();
			out.println("        obj."+fields[i].getName()+" = ("+jtype+")RPCFormatter"+Helper.classnameName(jtype)+".read(buf);");	  
			if (debug) {
			    //out.println("        jx.zero.Debug.out.println(\"RPCDEBUG: "+fields[i].getName()+"=compound\");");
			}
		    }
		    copyFields += "_obj."+fields[i].getName()+"= obj."+fields[i].getName()+";";

		}
	    }




	    if(switches.size() > 0) { // UNION
		// generate switch
		out.println("        "+classname +" _obj = null;");
		FieldData switchField = fields[fields.length-1];
		if (switchField.getBasicType().isPrimitive()) {
		    if (switchField.getBasicType().isInteger()) {
			out.println("        switch(obj."+switchField.getName()+") {");
			for(int j=0; j<switches.size(); j++) {
			    FieldData f = (FieldData)switches.elementAt(j);
			    String subclassName = f.getName();
			    int index = subclassName.lastIndexOf("_");
			    subclassName = subclassName.substring(index+1);
			    ClassData subClass = classFinder.findClass(Helper.classnamePackage(cl.getJavaLanguageName())+"."+subclassName);
			    if (classInfo.getSuperClass(subClass) != cl) {
				System.out.println("union type error: wrong superclass of "+subClass);
				System.out.println("                  superclass must be "+cl);
				throw new Error();
			    }
			    addRPCFormatType(subClass,classModifier);
			    out.println("          case "+f.getDeclaringClass().getJavaLanguageName()+"." + f.getName() + ": _obj = RPCFormatter"+Helper.classnameName(subclassName)+".read(buf); break;");
			}
			if (switchDefault != null) {
			    String subclassName = switchDefault.getName();
			    int index = subclassName.lastIndexOf("_");
			    subclassName = subclassName.substring(index+1);
			    subclassName = Helper.classnamePackage(cl.getJavaLanguageName())+"."+subclassName;
			    out.println("          default: _obj = new "+subclassName+"();");
			} else {
			    // generate default "default:" switch
			    // print out error message and throw excetion
			    out.println("          default: System.err.println(\"Unknown switch in RPC message\" + " +"obj."+switchField.getName()+"); throw new RuntimeException();");
			}
			out.println("        }");
		    } else {
			throw new Error("only int in RPCUnion supported:"+switchField.getType());
		    }
		}
		out.println(copyFields);
	    } // UNION




	    if(switches.size() > 0) { // UNION
		out.println("        return _obj;");
	    } else {
		out.println("        return obj;");
	    }
	    out.println("     }");




	    /*
	     * length (can only be called when object contains data)
	     */
	    out.println("     public static int length("+classname+" obj) {");
	    out.println("       int _l = 0;");
	    out.print(lstr);
	    out.println("       return _l;");
	    out.println("     }");


	    out.println("  }");


	    formats.remove(cl);
	    formats.put(cl, str.toString());

	} catch(Exception e) {
	    e.printStackTrace();
	    throw new Error(e.toString());
	}
    }
    public static boolean doesImplement(Class cl, Class interf) {
	if (cl==null) return false;
	Class[] all = cl.getInterfaces();
	for(int i=0; i<all.length; i++) {
	    if( all[i] == interf) return true;
	}
	return doesImplement(cl.getSuperclass(), interf);
    }

    Hashtable getFormats() {
	return formats;
    }


    /**
     * Get constants that are defined by this class.
     * Ignore constants from superclasses.
     */
    private Vector getDefinedConstants(ClassData cl) {
	Vector consts = new Vector();
	FieldData[] fields = cl.getDeclaredFields();      
	for(int i=0; i<fields.length; i++) {
	    int m = fields[i].getModifiers();
	    if (Modifier.isFinal(m)) {
		if (Modifier.isStatic(m)) {
		    consts.addElement(fields[i].getName());
		}
	    }
	}
	removeConstants(classInfo.getSuperClass(cl), consts);
	return consts;
    }
    
    private void removeConstants(ClassData cl, Vector consts) {
	if (cl==null) return;
	FieldData[] fields = cl.getDeclaredFields();      
	for(int i=0; i<fields.length; i++) {
	    int m = fields[i].getModifiers();
	    if (Modifier.isFinal(m)) {
		if (Modifier.isStatic(m)) {
		    consts.removeElement(fields[i].getName());
		}
	    }
	}
    removeConstants(classInfo.getSuperClass(cl), consts);
    }
    
}
