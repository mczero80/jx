package jx.wintv;

class ChromaVAdjustment extends FloatVideoAdjustment {
   static {
      minHard  = 0x000;
      maxHard  = 0x1ff;
      minHuman = 0;
      maxHuman = 28389;
      humanMult = 100;
      humanUnit = "%";
   }
   
   ChromaVAdjustment(int hardwareValue){
      this.hardwareValue = hardwareValue;
   }
}
