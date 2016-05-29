package jx.wintv;

class ChromaUAdjustment extends FloatVideoAdjustment {
   static {
      minHard  = 0x000;
      maxHard  = 0x1ff;
      minHuman = 0;
      maxHuman = 20118;
      humanMult = 100;
      humanUnit = "%";
   }
   
   ChromaUAdjustment(int hardwareValue){
      this.hardwareValue = hardwareValue;
   }
}
