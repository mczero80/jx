package jx.wintv;

import jx.zero.*;

public class MUXPin implements InputSource, Service {
   BT878Video bt; 
   int muxId;
   
   MUXPin(BT878Video bt, int muxId){
      this.bt = bt;
      this.muxId = muxId;
   }
   
   public void activate(){
      bt.setMuxSel(muxId);
   }
   public void deactivate(){
   }
   public boolean isActive(){
      return (muxId == bt.getMuxSel()) && (bt.getColorBars() == false);
   }
}

