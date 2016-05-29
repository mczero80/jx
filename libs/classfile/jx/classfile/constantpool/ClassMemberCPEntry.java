
package jx.classfile.constantpool; 
import java.io.*; 
import jx.classfile.datatypes.*; 
import jx.zero.Debug; 
abstract public class ClassMemberCPEntry extends ConstantPoolEntry {
  protected int classCPIndex;
  protected int nameAndTypeCPIndex;

  protected ClassCPEntry classCPEntry;
  protected NameAndTypeCPEntry nameAndTypeCPEntry; 

  public ClassMemberCPEntry() {}

  ClassMemberCPEntry(int classCPIndex, int nameAndTypeCPIndex) {
    this.classCPIndex = classCPIndex; 
    this.nameAndTypeCPIndex = nameAndTypeCPIndex;
  }

  // for metaXa interface, incomplete constructor (indices not initialized)   
  public ClassMemberCPEntry(ClassCPEntry classCPEntry, 
			    NameAndTypeCPEntry nameAndTypeCPEntry) {
    this.classCPEntry = classCPEntry; 
    this.nameAndTypeCPEntry = nameAndTypeCPEntry; 
    classCPIndex = -1; 
    nameAndTypeCPIndex = -1; 
  }

  void readFromClassFile(DataInput input) throws IOException {
    classCPIndex = input.readUnsignedShort();
    nameAndTypeCPIndex = input.readUnsignedShort(); 
  }

  public void linkCPEntries(ConstantPool cPool) {
    classCPEntry = (ClassCPEntry)cPool.entryAt(classCPIndex); 
    nameAndTypeCPEntry = (NameAndTypeCPEntry)cPool.entryAt(nameAndTypeCPIndex); 
  }

  public String getSimpleDescription() {
    return String.valueOf(classCPIndex)+", "+
      String.valueOf(nameAndTypeCPIndex); 
  }

  public String getClassName() {
    return classCPEntry.getClassName(); 
  }

  public ClassCPEntry getClassCPEntry() {
    return classCPEntry; 
  }

  public String getMemberName() {
    return nameAndTypeCPEntry.getName(); 
  }
  
  public String getMemberTypeDesc() {
    return nameAndTypeCPEntry.getTypeDesc();
  }

  public boolean isMember(String className,String memberName,String typeDesc) {
      return 
	  (classCPEntry.getClassName().equals(className) &&
	   nameAndTypeCPEntry.getName().equals(memberName) &&
	   nameAndTypeCPEntry.getTypeDesc().equals(typeDesc));	  
  }

  public String getDescription(ConstantPool cPool, boolean withIndex) {
    return "class=" + getIndexDescString(cPool, classCPIndex) + ", " + 
      "name_and_type=(" + nameAndTypeCPIndex + "), " + 
      nameAndTypeCPEntry.getDescription(cPool, true); 
  }

  public String getDescription() {
    return getClassName() + "." + getMemberName() + " (" + getMemberTypeDesc() + ") "; 
  }

  int getClassNameAddress() {return classCPEntry.getClassNameAddress();}
  int getTypeDescAddress() {return nameAndTypeCPEntry.getTypeDescAddress();}
  int getNameAddress() {return nameAndTypeCPEntry.getNameAddress();}
  public final int getNameAndTypeIndex() { return nameAndTypeCPIndex; }
  public final int getClassIndex() { return classCPIndex; }
}
