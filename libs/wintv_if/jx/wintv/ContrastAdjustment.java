package jx.wintv;

public class ContrastAdjustment extends FloatVideoAdjustment {
   static {
      minHard  = 0x000;
      maxHard  = 0x1ff;
      minHuman = 0;
      maxHuman = 23657;
      humanMult = 100;
      humanUnit = "%";
   }
   
   public ContrastAdjustment(int hardwareValue){
      this.hardwareValue = hardwareValue;
   }
}

