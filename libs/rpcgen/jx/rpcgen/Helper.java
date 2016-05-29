package jx.rpcgen;

class Helper {
  public static String classnameName(String full) {
    int index = full.lastIndexOf('.');
    if (index == -1) {
      return full;
    }
    return full.substring(index+1);
  }
  public static String classnamePackage(String full) {
    int index = full.lastIndexOf('.');
    if (index == -1) {
      return null;
    }
    return full.substring(0,index);
  }
}
