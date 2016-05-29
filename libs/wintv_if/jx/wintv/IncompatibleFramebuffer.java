package jx.wintv;

public class IncompatibleFramebuffer extends Error {
   public IncompatibleFramebuffer(){
   }
   public IncompatibleFramebuffer(String reason){
      super(reason);
   }
}
