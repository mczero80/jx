package jx.wintv;

/* Exceptions must be caught, Errors not ... */
public class NotImpl extends Error {
   public NotImpl(){
      super("Not implemented");
   }
   public NotImpl(String reason){
      super(reason);
   }
}
