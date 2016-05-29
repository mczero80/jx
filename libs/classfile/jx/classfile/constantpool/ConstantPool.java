package jx.classfile.constantpool; 

import java.io.*; 
import jx.classfile.datatypes.*; 
import jx.zero.Debug; 

/** 
    This class represents a constant pool of a class. 
    It stores a array of entries, each of which contains the
    data of a constant pool entry. 
    It can be initialized either with a java class file, or with 
    a instance of MetaxaConstantPool, or with a ClassInterface. 
    The last way ist actually deprecated, and not used anymore. 
    
    When initialized with a MetaxaConstantPool, the entries are
    loaded on demand, i.e. an entry is loaded when it is referenced 
    using one of the entryAt()-Methods. Only entry types that are actually 
    referenced by bytecode can be loaded through MetaxaConstantPool 
    (e.g. NameAndTypeCPEntry's can not). 
*/ 
public class ConstantPool {

    static boolean noLongs = false;

  // the entries of the constant pool 
  private int numEntries; 
  private ConstantPoolEntry[] entry; 

  // deprecated 
    /*private ClassInterface classInterface; */

  /** 
      necessary to access string entries of this constantpool through 
      the interface in bytecode.execenv.ExecEnvironment
  */ 
  private String className, superClassName; 

  // deprecated 
    /*  ConstantPool(ClassInterface classInterface) {
   this.classInterface = classInterface; 
   }*/
  
  
  /**  
       constructors for reading from classFile 
       (call readFromClassFile() after using this constructor) 
  */   
  public ConstantPool() {
      //    throw new RuntimeException(); // this constructor is unsafe, does not create the entry array!
  }

  /**  
       constructors for reading from classFile 
  */ 
  public ConstantPool(DataInput input) throws IOException {
    readFromClassFile(input); 
    //throw new RuntimeException();
  }

  /** 
      set the index of the entry that contains the name of the 
      constantpools class. 
  */ 
  public void setThisClassCPIndex(int thisClassCPIndex) {
    className = ((ClassCPEntry)entryAt(thisClassCPIndex)).getClassName(); 
  }

  public void setSuperClassCPIndex(int cpIndex) {
      //Debug.out.println("ENTRY: "+entryAt(cpIndex));
      superClassName = ((ClassCPEntry)entryAt(cpIndex)).getClassName(); 
  }

  /** 
      this method is necessay to access the address of 
      strings in the constant pool through the interface int
      bytecode.execenv.ExecEnvironment
  */ 
  public String getClassName() {
    return className; 
  }

  public String getSuperClassName() {
    return superClassName; 
  }

  /** 
      read all entries from a class file 
  */
  public void readFromClassFile(DataInput input) throws IOException {
    numEntries = input.readUnsignedShort(); 
    entry = new ConstantPoolEntry[numEntries]; 
    entry[0] = new DummyCPEntry(); 
    int i = 1; 
 
    while (i < numEntries) {
      ConstantPoolEntry newEntry=null;     
      int tag = input.readUnsignedByte(); 

      switch (tag) {
      case ConstantPoolEntry.CONSTANT_UTF8: 
	newEntry = new UTF8CPEntry();
	break; 
      case ConstantPoolEntry.CONSTANT_INTEGER:
      case ConstantPoolEntry.CONSTANT_FLOAT:
      case ConstantPoolEntry.CONSTANT_LONG:
      case ConstantPoolEntry.CONSTANT_DOUBLE: 
	newEntry = new NumericCPEntry(tag);
	break; 
      case ConstantPoolEntry.CONSTANT_CLASS:	
	newEntry = new ClassCPEntry();
	break; 
      case ConstantPoolEntry.CONSTANT_STRING: 
	newEntry = new StringCPEntry();
	break; 
      case ConstantPoolEntry.CONSTANT_FIELDREF: 
	newEntry = new FieldRefCPEntry();
	break; 
      case ConstantPoolEntry.CONSTANT_METHODREF: 
	newEntry = new MethodRefCPEntry();
	break; 
      case ConstantPoolEntry.CONSTANT_INTERFACEMETHODREF: 
	newEntry = new InterfaceMethodRefCPEntry();
	break; 
      case ConstantPoolEntry.CONSTANT_NAMEANDTYPE: 
	newEntry = new NameAndTypeCPEntry();
	break; 
      default: 
	Debug.assert(false, "This must not happen "+tag); 
	break; 
      }
      
      // Long Hack !!
      if (noLongs &&
	  (tag == ConstantPoolEntry.CONSTANT_LONG ||
	   tag == ConstantPoolEntry.CONSTANT_DOUBLE)) {
	  //Debug.out.println("!! skip longs in jx/classfile/constantpool/ConstantPool.java !!");
	  newEntry.readDummyValue(input);
      } else {
	  newEntry.readFromClassFile(input); 
      }
      newEntry.setCPIndex(i);  

      this.entry[i++] = newEntry; 
   
      if (tag == ConstantPoolEntry.CONSTANT_LONG || 
	  tag == ConstantPoolEntry.CONSTANT_DOUBLE) 
	  this.entry[i++] = new DummyCPEntry();
    }
    
    // link the entries 
    for(int j=0;j<numEntries;j++) 
      entry[j].linkCPEntries(this); 
  }


  // currently not used 
    /*
  private void loadCPEntryFromInterpreterInterface(int index) {
    Debug.assert(entry[index] == null); 
    entry[index] = classInterface.getConstantPoolEntry(index); 
    entry[index].linkCPEntries(this); 
    }*/
  

  // ***** Methods to access the entries ***** 

  public ConstantPoolEntry constantEntryAt(int index) {
    if (entry[index]==null) {
	Debug.throwError();
    }
    return entry[index]; 
  }
   

  public ClassCPEntry classEntryAt(int cpIndex) {
    if (entry[cpIndex]==null) {
	Debug.throwError();
    }
    if (!(entry[cpIndex] instanceof ClassCPEntry)) {
	  Debug.out.println("cpIndex="+cpIndex+", entry="+getEntryStringAt(cpIndex));
	  Debug.throwError("wrong CP entry type (expect ClassCPEntry)");
    }
    return (ClassCPEntry)entry[cpIndex]; 
  }

  public MethodRefCPEntry methodRefEntryAt(int index) {
    if (entry[index]==null)  {
	Debug.throwError();
    }
    /*if (DebugConf.doPrintCPEntry) Debug.out.println("Entry["+index+"]:" + entry[index]);*/
    return (MethodRefCPEntry)entry[index]; 
  }

  public FieldRefCPEntry fieldRefEntryAt(int index) {
    if (entry[index]==null)  {
	Debug.throwError();
    }
    return (FieldRefCPEntry)entry[index]; 
  }
    
  public InterfaceMethodRefCPEntry InterfaceMethodRefEntryAt(int index) {
    if (entry[index]==null)  {
	Debug.throwError();
    }
    return (InterfaceMethodRefCPEntry)entry[index]; 
  }

  /** 
      only if the entries are loaded from a class file, 
      you can use this method to access them. Otherwise, this 
      class will throw an error. 
      It is better to use the methods above for this task. 
  */ 
  public ConstantPoolEntry entryAt(int index) {
      if (index == -1) {
	  throw new Error("Attempt to access a dynamically generated CPEntry via an index.");
      }
    if (entry[index]==null)
      Debug.throwError("Accessing of entries not allowed"); 

    // if (entry[index] == null) 
    // loadCPEntryFromInterpreterInterface(index); 

    return entry[index];
  }
      
  public String toString() {
    String string = ""; 
    for(int i=0;i<numEntries;i++) {
      string += ""+i+" : "+entry[i].getTypeString() + ": " + 
	entry[i].getDescription(this, true) + "\n"; 
    }
    return string;
  }

  public int getNumberOfEntries() { return numEntries; }

  public String getEntryStringAt(int cpIndex) {
    return "(" + cpIndex + ") " + entry[cpIndex].getDescription(this, true); 
  }

  public String getUTF8StringAt(int cpIndex) {
      if (! (entry[cpIndex] instanceof UTF8CPEntry)) {
	  Debug.out.println("cpIndex="+cpIndex+", entry="+getEntryStringAt(cpIndex));
	  Debug.throwError("wrong CP entry type");
      }
      return ((UTF8CPEntry)entry[cpIndex]).value();
  }  

  // the next two functions are not used 

  private  BCBasicDatatype getNumericValueAt(int cpIndex) {
    return ((NumericCPEntry)entry[cpIndex]).value(); 
  }

  private int getIntegerAt(int cpIndex) {
    return ((BCInteger)getNumericValueAt(cpIndex)).value(); 
  }



    /*************** MODIFICATION SUPPORT **********************/
    /* The following methods can be used to modify the 
     * constantpool, for example, to add new entries.
     * This feature is used for the dynamic bytecode generation.
     ***********************************************************/

    /** @return cp index of added entry */
    public int addEntry(ConstantPoolEntry newEntry) {
	int index = numEntries;
	newEntry.setCPIndex(index); 
	if (entry.length == numEntries) {
	    ConstantPoolEntry[] e = new ConstantPoolEntry[numEntries + 10];
	    System.arraycopy(entry, 0, e, 0, numEntries);
	    entry = e;
	}
	entry[index] = newEntry; 
	numEntries++;
	return index;
    }
} 

// ***** some private notes ***** 

// Constant Pool muss rekonstruierbar sein, d.h. man 
// sollte ihn wieder in das Format fuer die Ausfuehrung konvertieren koennen 

// Momentan wird auf die ConstantPoolEntries ueber ihre Indices verwiesen 
// effizienter waere es, man wuerde direkte Referenzen auf die CPoolEntries 
// speichern. Auch die CPoolEntries selbst muessten dann auf andere CPoolEntries 
// ueber Referenzen zugreifen. 
// (Linkphase nach Laden des Pools -> intern sind die Indices immer noch notw.) 
// Problem: Zeilennummern 



