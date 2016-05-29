package jx.wintv;

import jx.zero.Debug;

class BT878PLL {
   final static int fsc8NTSC = TVNorms.fsc8NTSC;	/* 8*Fsc(NTSC) */
   final static int fsc8PAL  = TVNorms.fsc8PAL;		/* 8*Fsc(PAL)  */
   
   boolean pllX = false;
   int pllI = 0;
   int pllF = 0;
   boolean pllC = false;
   
   BT878PLL(boolean x, int i, int f, boolean c){
      this.pllX = x;
      this.pllI = i;
      this.pllF = f;
      this.pllC = c;
      checkPllI();
   }
   
   BT878PLL(int fi, int fo, int divider){
      setDividerBits(divider);
      int prediv  = pllX ? 2 : 1;
      int postdiv = pllC ? 4 : 6;
      
      fo *= prediv * postdiv;
      
      pllI = fo/fi;
      pllF = 0;
      int r = fo%fi;
      for(int i=0; i<4; ++i){
	 r <<= 4;
	 pllF <<= 4;
	 pllF |= r/fi;
	 r %= fi;
      }
      
      checkPllI();
   }
   
   public int toInteger(){
      return toInteger(fsc8NTSC);
   }
   
   int toInteger(int fi){
      int prediv  = pllX ? 2 : 1;
      int postdiv = pllC ? 4 : 6;
      
      fi /= prediv;
      
      int fo = fi * pllI; 
      int digit;
      int pllF = this.pllF;
      Debug.assert(pllF >= 0 && pllF < 0x10000, "pllF out of range: "+pllF);
      for(int div=0x10000; pllF != 0; div>>=4, pllF>>=4){
	 digit = pllF & 0x0f;
	 fo += fi*digit/div;
      }
      
      fo /= postdiv;
      
      return fo;
   }
   
   public String toString(){
      return Integer.toString(toInteger());
   }
   
   public String toDebugString(){
      return "PLL(" + pllX + 
	", 0x" + Hex.toHexString((byte)pllI) +
	", 0x" + Hex.toHexString((short)pllF) + 
	", " +pllC + ") = " + toInteger();
   }
   
   /********************************************************************/
   
   void setDividerBits(int divider){
      switch(divider){
       case 4:
	 pllX = false;
	 pllC = true;
	 break;
       case 6:
	 pllX = false;
	 pllC = false;
	 break;
       case 8:
	 pllX = true;
	 pllC = true;
	 break;
       case 12:
	 pllX = true;
	 pllC = false;
	 break;
       default:
	 throw new Error("unsupported divider: "+divider);
      }
   }
   
   void checkPllI(){
//      Debug.out.println(toDebugString());
      if( pllI != 0 && (pllI < 6 || pllI > 63) )
	throw new Error("PLL-I out of allowed range: "+pllI);
      return;
   }
}


