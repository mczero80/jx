package jx.wintv;

abstract public class FloatVideoAdjustment implements VideoAdjustment {
   static int minHard;
   static int maxHard;
   
   static int minHuman;
   static int maxHuman;
   
   static int humanMult;	/* Since there is no floating point support, human values are mutliplied with this constant */
   static String humanUnit;

   int hardwareValue;			/* current value */
   
   int getHWValue(){
      return hardwareValue;
   }
   void setHWValue(int newValue){
      hardwareValue = newValue;
   }
   
   public String toString(){
      return toString(hardwareValue);
   }
   
   static public String toString(int hwValue){
      int human = (maxHuman-minHuman) * hwValue / (maxHard-minHard);
      return Integer.toString(human / humanMult) + "." + Integer.toString(human % humanMult) + humanUnit;
   }
}
