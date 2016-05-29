// -*-Java-*-

package jx.classfile.constantpool; 
// COMMON_HEADER: package jx.classfile.constantpool; 

import java.io.*; 
// COMMON_HEADER: import java.io.*; 

// COMMON_HEADER: import jx.classfile.datatypes.*; 
import jx.classfile.datatypes.*; 

import jx.zero.Debug; 
// COMMON_HEADER: import jx.zero.Debug; 

/** 
    All constant pool entries are organized in a hierarchy. 
    Links between constant pool entries are implemented as 
    java references. These are initialized by linkCPEntries(); 
    The entry objects are rather passive, they are only used to 
    access the contents of the constant pool. Nothing else. 
    This class has two ways to get the data for the constantpool: 
    Either from a class file, or from the structural reflection interface 
    of metaXa.  

    Initially it was intended to access the data from the constant pool 
    resolution (e,g, addresses of static fields ...) through these 
    objects. There are still some methods in the classes below for 
    this purpose. (currently they have default visibility and are not used anywhere). 
*/ 

abstract public class ConstantPoolEntry {
  
  // index of this entry in the constant pool 
  // possibly useful for the serialization of compiled code 
  // this field is not always initialized
  // in this case, cpIndex == -1 
  private int cpIndex; 

  public ConstantPoolEntry() {
    cpIndex = -1; 
  }

  void setCPIndex(int index) {this.cpIndex = index; }

  public int getCPIndex() {return cpIndex;}

  /** 
      @return one of the constants from ConstantPoolEntry.CONSTANT_*
  */ 
  abstract public int getTag(); 

  /** 
      read the entry from a class file
      (this method does _not_ initialize the references to other
      CP entries)
  */ 
  abstract void readFromClassFile(DataInput input) throws IOException; 

  /** 
      skips the entry from a class file 
      (this method does _not_ initialize the references to other
      CP entries)
  */ 
    void readDummyValue(DataInput input) throws IOException {
	throw new Error("wrong method called");
    }

  /** 
      initialize the references to other CP entries    
      (must be called after reading an entry from classfile)
  */
  void linkCPEntries(ConstantPool cPool) {}

  public static final int CONSTANT_UTF8 = 1; 
  public static final int CONSTANT_INTEGER = 3; 
  public static final int CONSTANT_FLOAT = 4; 
  public static final int CONSTANT_LONG = 5; 
  public static final int CONSTANT_DOUBLE = 6 ; 
  public static final int CONSTANT_CLASS = 7; 
  public static final int CONSTANT_STRING = 8 ; 
  public static final int CONSTANT_FIELDREF = 9; 
  public static final int CONSTANT_METHODREF = 10; 
  public static final int CONSTANT_INTERFACEMETHODREF = 11;
  public static final int CONSTANT_NAMEANDTYPE = 12; 

  public String toString() {
    return getTypeString() + ": " + getSimpleDescription(); 
  }

  /** 
      @return a textual description of the entries type 
  */ 
  public String getTypeString() {
    switch( getTag()) {
    case CONSTANT_UTF8: return "UTF8"; 
    case CONSTANT_INTEGER: return "Integer"; 
    case CONSTANT_FLOAT: return "Float";  
    case CONSTANT_LONG: return "Long"; 
    case CONSTANT_DOUBLE: return "Double"; 
    case CONSTANT_CLASS: return "Class"; 
    case CONSTANT_STRING: return "String"; 
    case CONSTANT_FIELDREF: return "FieldRef"; 
    case CONSTANT_METHODREF: return "MethodRef"; 
    case CONSTANT_INTERFACEMETHODREF: return "InterfaceMethodRef"; 
    case CONSTANT_NAMEANDTYPE: return "NameAndType"; 
    default: return "UnknownEntry"; 
    }
  }

  /**
     It is better to use one of the methods below if you want a 
     descriptive string (this one was only useful during 
     the implementation of this class) 
     @return a simple description 
  */ 
  abstract public String getSimpleDescription(); 

  /**
      Used by subclasses of bytecode.code.BCInstr to print the 
      bytecode. This versiom of the method needs the constant pool.
      @return a textual description of the entry (similar to the 
      output of javap (but not exactly equal))
      @see bytecode.code.BCInstr
  */ 
  public String getDescription(ConstantPool cPool, boolean withIndex) {
    return getSimpleDescription(); 
  }
    
  /**
     This method should be equivalent to the method above. 
     The difference is that it doesn't need a reference of the 
     constant pool. 
     @return a textual description of the entry (similar to the 
     output of javap (but not exactly equal))
  */       
  public String getDescription() {
    return getSimpleDescription(); 
  }

  /** 
      Yet another description method. Mainly used for debugging. 
      @return descriptive string with indices 
  */ 
  protected String getIndexDescString(ConstantPool cPool, int index) {
    return "("+index+")" + cPool.entryAt(index).getDescription(cPool, false); 
  }

  // currently unused 
  private boolean isResolved() {return false;}
}
