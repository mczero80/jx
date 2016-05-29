package jx.fs;

public interface Directory extends jx.fs.FSObject, jx.zero.Portal {

    public String[]   list() throws Exception;

    public FSObject    openRW(String name) throws Exception;
    public FSObject    openRO(String name) throws Exception;

    public RegularFile create(Permission perm, String filename) throws Exception;
    public boolean     link(Permission perm, String filename, FSObject file) throws Exception;
    public boolean     unlink(String filename) throws Exception;
    public boolean     mkdir(Permission perm, String dirname) throws Exception;
}
