package jx.zero;

public interface ByteCodeTranslater extends Portal {
    public void translate(String zipname, String libname) throws Exception;
    //public void translate(String zip, String lib, String info) throws Exception;
}
