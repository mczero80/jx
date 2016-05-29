package jx.wintv;

public abstract class BooleanVideoAdjustment implements VideoAdjustment {
   boolean hardwareValue;			/* current value */
   
   public boolean getHWValue(){
      return hardwareValue;
   }
   public void setHWValue(boolean newHWValue){
      hardwareValue = newHWValue;
   }
   
   public void setValue(boolean value){
      hardwareValue = !value;
   }
   public boolean getValue(){
      return !hardwareValue;
   }
   
   public String toString(){
      return hardwareValue ? "true" : "false";
   }
}
