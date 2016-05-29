// -*-Java-*-

// COMMON_HEADER: package jx.compiler.nativecode; 
package jx.compiler.nativecode;

// COMMON_HEADER: import jx.classfile.datatypes.*; 
import jx.classfile.datatypes.*;

// COMMON_HEADER: import jx.compiler.CompileException; 
import jx.compiler.CompileException;

// COMMON_HEADER: import jx.compiler.symbols.*;
import jx.compiler.symbols.*;

// COMMON_HEADER: import jx.compiler.execenv.*;
import jx.compiler.execenv.*;

// COMMON_HEADER: import java.util.Vector;
import java.util.Vector;

// ***** IMNode *****

public class IA32Node {
    public int size() {return -1;}
    public int translate(int ip, byte[] code) {return ip;}
}
