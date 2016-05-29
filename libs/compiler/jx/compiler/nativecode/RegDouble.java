/**
 * X86-Register for floating-point 
 */

package jx.compiler.nativecode;

import jx.compiler.symbols.SymbolTableEntryBase;
import jx.compiler.imcode.*;

final public class RegDouble implements Cloneable {

    public RegDouble() {
	throw new Error("wrong RegFloat constuctor");
    }

    public RegFloat getClone() {
	return null;
    }
}
