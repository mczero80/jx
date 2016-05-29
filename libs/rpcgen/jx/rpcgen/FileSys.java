package jx.rpcgen;

import java.io.OutputStream;

/**
 * Used to allow RPCGen to create and write files
 */
public interface FileSys {
    OutputStream openFile(String filename);
}
