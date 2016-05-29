package jx.classfile.datatypes; 

/** 
    only for the testing of this package  
*/ 

class TestTypeDesc {
    /*
  static void decompField(String typeDesc) {
    // System.out.print(typeDesc + ":"); 
    BasicTypeDescriptor desc = new BasicTypeDescriptor(typeDesc); 
    if (desc.isPrimitive())
      System.out.print("P("+desc.toBasicDatatype()+")"); 
    else if (desc.isClass()) 
      System.out.print("C("+desc.getClassName()+")"); 
    else if (desc.isArray()) {
      System.out.print("A("); 
      decompField(desc.getArrayTypeDesc()); 
      System.out.print(", "+ desc.getArrayDimension() + ")"); 
    }
  }
   

  static void decompMethod(String typeDesc) {
    MethodTypeDescriptor desc = new MethodTypeDescriptor(typeDesc); 
    System.out.println("Method: " + desc.getNumArguments()+ ":"); 
    int[] args = desc.getBasicArgumentTypes(); 
    for(int i=0;i<args.length;i++) 
      System.out.print(", " + args[i]); 
    System.out.println("" + desc.getBasicReturnType()); 

    String arg; 
    desc.initReadArguments(); 
    do {
      arg = desc.readArgumentTypeDesc(); 
      if (arg != null) {
	decompField(arg); 
	System.out.println("");
      } 
    } while (arg != null); 
    arg = desc.getReturnTypeDesc(); 
    System.out.print("Ret:"); 
    decompField(arg); 
    System.out.println(""); 
  }


  public static void main(String[] argv) {
    decompMethod(argv[0]); 
  }
    */
}
    
