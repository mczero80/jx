/**
 * X86-Register Class
 */

package jx.compiler.nativecode;

import jx.compiler.imcode.*;

public interface RegObj {
    public int getDatatype();
    public void push(MethodStackFrame frame);
}
