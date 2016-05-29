
package jx.classfile.constantpool; 
import java.io.*; 
import jx.classfile.datatypes.*; 
import jx.zero.Debug; 
public class FieldRefCPEntry extends ClassMemberCPEntry {

  private int fieldOffset; // currently useless 

  public FieldRefCPEntry() {
  }
  
  FieldRefCPEntry(int classCPIndex, int nameAndTypeCPIndex, 
			 int fieldOffset) {
    super(classCPIndex, nameAndTypeCPIndex);
    this.fieldOffset = fieldOffset; 
  } 

  // for metaXa interface, incomplete constructor (indices not initialized)   
  public FieldRefCPEntry(ClassCPEntry classCPEntry, 
			 NameAndTypeCPEntry nameAndTypeCPEntry) {
    super(classCPEntry, nameAndTypeCPEntry); 
  }
  
 
  public int getTag() {return CONSTANT_FIELDREF;}

  int getFieldOffset() {return fieldOffset;}
}
