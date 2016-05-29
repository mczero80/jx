package jx.classstore; 

import java.io.*; 
import jx.zero.Debug; 
import jx.zero.Memory; 
import jx.zero.debug.Dump; 
import jx.classfile.constantpool.*; 
import jx.classfile.datatypes.*; 
import jx.classfile.ClassData; 
import jx.classfile.ClassSource; 
import jx.classfile.MethodSource;
import jx.classfile.MethodData;
import jx.classfile.FieldData;
import jx.classfile.NativeMethodException;

import jx.zero.memory.MemoryInputStream;

public class MemoryClassSource extends ClassData {

  public MemoryClassSource(Memory input) throws IOException {
      super(new DataInputStream(new MemoryInputStream(input))); 
  }

  public MemoryClassSource(Memory input, boolean allowNative) throws IOException {
      super(new DataInputStream(new MemoryInputStream(input)), allowNative); 
  }
}


