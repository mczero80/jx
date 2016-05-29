package jx.compiler.persistent;

import java.io.*;
import jx.formats.*;

public class ExtendedDataInputStream extends LittleEndianInputStream implements jx.compiler.execenv.ExtendedDataInputStream  {
    public ExtendedDataInputStream(InputStream in) { super(in); }
}
