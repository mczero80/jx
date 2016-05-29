package jx.fs;

public interface ReadOnlyDirectory extends jx.fs.FSObject, jx.zero.Portal {
    public String[]   list() throws Exception;
    public FSObject   openRO(String name) throws Exception;
    public FSObject   openRW(String name) throws Exception;
}
