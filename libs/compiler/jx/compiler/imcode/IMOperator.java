
package jx.compiler.imcode; 
import jx.classfile.constantpool.*; 
import jx.classfile.datatypes.*; 
import jx.classfile.*;
import jx.zero.Debug; 
import jx.compiler.*;
import jx.compiler.nativecode.*;
import jx.compiler.symbols.*;
import jx.compiler.execenv.*;
import java.util.Vector;
// ***** IMOperator *****

public class IMOperator extends IMOperant {

    public IMOperator(CodeContainer container) {
	super(container);
	tag = tag | OPERATOR;
    }

    public String toReadableString() {
	return "<operator>";
    }
}
