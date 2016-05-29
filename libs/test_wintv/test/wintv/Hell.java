package test.wintv;

public class Hell extends Error {
   public Hell(){
      super("WARNING: Hell is frozen!");
   }
   public Hell(String str){
      super("WARNING> Hell is frozen! "+str);
   }
}
