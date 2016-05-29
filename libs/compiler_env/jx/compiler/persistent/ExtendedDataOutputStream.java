package jx.compiler.persistent;

import java.io.*;
import jx.formats.*;

/**
 * Writes numbers in little endian format.
 * Writes strings by first writing the stringlength and
 * then the *bytes* of the string (no unicode support up to now)
 */
public class ExtendedDataOutputStream extends LittleEndianOutputStream implements jx.compiler.execenv.ExtendedDataOutputStream {
    public ExtendedDataOutputStream(OutputStream out) { super(out); }
}
