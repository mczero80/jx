package jx.wintv;

public class BrightnessAdjustment extends FloatVideoAdjustment {
   static {
      minHard  = -128;
      maxHard  = +127;
      minHuman = -5000;
      maxHuman = +4960;
      humanMult = 100;
      humanUnit = "%";
   }
   
   public BrightnessAdjustment(int hardwareValue){
      this.hardwareValue = hardwareValue;
   }
}

