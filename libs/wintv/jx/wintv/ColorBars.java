package jx.wintv;

import jx.zero.*;

public class ColorBars implements InputSource, Service {
   BT878Video bt;
   
   ColorBars(BT878Video bt){
      this.bt = bt;
   }
   
   public void activate(){
      bt.setColorBars(true);
   }
   public void deactivate(){
      bt.setColorBars(false);
   }
   public boolean isActive(){
      return bt.getColorBars();
   }
}
