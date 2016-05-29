package jx.wintv;

public class SVideoImpl extends MUXPin implements SVideo {
   
   SVideoImpl(BT878Video bt, int muxId){
      super(bt, muxId);
   }
   
   public void activate(){
      super.activate();
      setChromaMode(false);
   }
   
   public void deactivate(){
      super.deactivate();
      setChromaMode(true);
   }
   
   public void setChromaMode(boolean composite){
      if( !isActive() )
	throw new Error("InputSource is not active");
      
      if( composite ){			/* composite mode: Y/C on MUX */
	 bt.setCSleep(true);
	 bt.setControlOdd(bt.CONTROL_COMP, 0);
	 bt.setControlEven(bt.CONTROL_COMP, 0);
      }
      else {				/* component mode: Y on MUX, C on CIN */
	 bt.setCSleep(false);
	 bt.setControlOdd(bt.CONTROL_COMP, bt.CONTROL_COMP);
	 bt.setControlEven(bt.CONTROL_COMP, bt.CONTROL_COMP);
      }
   }
   
   public boolean getChromaMode(){
      if( !isActive() )
	throw new Error("InputSource is not active");
      
      return (bt.getControlOdd(bt.CONTROL_COMP) == 0) && 
	(bt.getControlEven(bt.CONTROL_COMP) == 0);
   }
}

