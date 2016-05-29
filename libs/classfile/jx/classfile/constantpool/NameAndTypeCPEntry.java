
package jx.classfile.constantpool; 
import java.io.*; 
import jx.classfile.datatypes.*; 
import jx.zero.Debug; 
public class NameAndTypeCPEntry extends ConstantPoolEntry {
  private int nameCPIndex;
  private int typeCPIndex; 

  private UTF8CPEntry nameCPEntry;
  private UTF8CPEntry typeCPEntry; 

  public NameAndTypeCPEntry() {}
  
  NameAndTypeCPEntry(int nameCPIndex, int typeCPIndex) {
    this.nameCPIndex = nameCPIndex; 
    this.typeCPIndex = typeCPIndex;
  }
  
  // for metaXa interface, incomplete constructor (indices not initialized)   
  public NameAndTypeCPEntry(UTF8CPEntry nameCPEntry, UTF8CPEntry typeCPEntry) {
    this.nameCPEntry = nameCPEntry; 
    this.typeCPEntry = typeCPEntry; 
    nameCPIndex = -1; 
    typeCPIndex = -1; 
  }

  public int getTag() {return CONSTANT_NAMEANDTYPE;}

  void readFromClassFile(DataInput input) throws IOException {
    nameCPIndex = input.readUnsignedShort();
    typeCPIndex = input.readUnsignedShort(); 
  }

  void linkCPEntries(ConstantPool cPool) {
    nameCPEntry = (UTF8CPEntry)cPool.entryAt(nameCPIndex); 
    typeCPEntry = (UTF8CPEntry)cPool.entryAt(typeCPIndex); 
  }

  public String getSimpleDescription() {
    return String.valueOf(nameCPIndex)+", "+
      String.valueOf(typeCPIndex); 
  }

  public String getName() {
    return nameCPEntry.value(); 
  }

  public String getTypeDesc() {
    return typeCPEntry.value(); 
  }

  public String getDescription(ConstantPool cPool, boolean withIndex) {
    return "name=" + getIndexDescString(cPool, nameCPIndex) + ", " + 
      "type=" + getIndexDescString(cPool, typeCPIndex); 
  }

  public String getDescription() {
    return getName() + " (" + getTypeDesc() + ") "; 
  }

  int getNameAddress() {return nameCPEntry.getStringAddress();}
  int getTypeDescAddress() {return typeCPEntry.getStringAddress();}
}
