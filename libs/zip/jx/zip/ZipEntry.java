package jx.zip;

import jx.zero.ReadOnlyMemory;

public class ZipEntry {
    String filename;
    int uncompressed_size;
    int compression_method;
    ReadOnlyMemory data;
    boolean isDirectory;

    public boolean isDirectory() {
	return isDirectory;
    }
    public String getName() {
	return filename;
    }
    public int getSize() {
	return uncompressed_size;
    }
    public ReadOnlyMemory getData() {
	return data;
    }
}
