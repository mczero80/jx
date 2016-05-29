
package jx.classfile.constantpool; 
import java.io.*; 
import jx.classfile.datatypes.*; 
import jx.zero.Debug; 

public class DummyCPEntry extends ConstantPoolEntry {
  
  public int getTag() {return -1;}
  
  void readFromClassFile(DataInput input) throws IOException {
  }

  public String getSimpleDescription() { return "NoValue";}
}
