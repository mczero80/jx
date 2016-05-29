package jx.zero;

public interface BootFS extends Portal {
    public boolean lookup(String filename);
    public ReadOnlyMemory getFile(String filename);
    public Memory getReadWriteFile(String filename);
}
