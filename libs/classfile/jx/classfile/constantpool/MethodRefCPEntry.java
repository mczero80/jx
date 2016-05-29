
package jx.classfile.constantpool; 
import java.io.*; 
import jx.classfile.datatypes.*; 
import jx.zero.Debug; 
public class MethodRefCPEntry extends ClassMemberCPEntry {

  // index of this method in the methodtable 
  private int methodIndex; 

  public MethodRefCPEntry() {}
  
  public MethodRefCPEntry(int classCPIndex, int nameAndTypeCPIndex) {
    super(classCPIndex, nameAndTypeCPIndex);
  } 

  MethodRefCPEntry(int classCPIndex, int nameAndTypeCPIndex, 
			  int methodIndex) {
    super(classCPIndex, nameAndTypeCPIndex);
    this.methodIndex = methodIndex; 
  } 

  // for metaXa interface, incomplete constructor (indices not initialized)   
  public MethodRefCPEntry(ClassCPEntry classCPEntry, 
			  NameAndTypeCPEntry nameAndTypeCPEntry) {
    super(classCPEntry, nameAndTypeCPEntry); 
  }

  public int getTag() {return CONSTANT_METHODREF;}




  int getMethodIndex() {return 0;}
  int getStaticMethodAddress() {return 0;}
}
