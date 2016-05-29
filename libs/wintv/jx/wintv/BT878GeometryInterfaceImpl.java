package jx.wintv;

/*
 * This class gives the user the ability to set the geometry of the video
 * on his own algorithms without giving him full access to the BT878 chip.
 * Whithout this class the user could "upcast" the BT878GeometryInterface.
 */
public class BT878GeometryInterfaceImpl implements BT878GeometryInterface {
   private BT878Video bt;
   
   BT878GeometryInterfaceImpl(BT878Video bt){
      if( bt == null )
	throw new Error("bt == null");
      this.bt = bt;
   }
   
   public void setGeometryOdd(int hscale, int hdelay, int hactive, 
			      int vscale, int vdelay, int vactive){
      bt.setGeometryOdd(hscale, hdelay, hactive,
			vscale, vdelay, vactive);
   }
   public void setGeometryEven(int hscale, int hdelay, int hactive, 
			       int vscale, int vdelay, int vactive){
      bt.setGeometryEven(hscale, hdelay, hactive,
			 vscale, vdelay, vactive);
   }
   public int getHScaleOdd(){
      return bt.getHScaleOdd();
   }
   public int getHDelayOdd(){
      return bt.getHDelayOdd();
   }
   public int getHActiveOdd(){
      return bt.getHActiveOdd();
   }
   public int getVScaleOdd(){
      return bt.getVScaleOdd();
   }
   public int getVDelayOdd(){
      return bt.getVDelayOdd();
   }
   public int getVActiveOdd(){
      return bt.getVActiveOdd();
   }
   public int getHScaleEven(){
      return bt.getHScaleEven();
   }
   public int getHDelayEven(){
      return bt.getHDelayEven();
   }
   public int getHActiveEven(){
      return bt.getHActiveEven();
   }
   public int getVScaleEven(){
      return bt.getVScaleEven();
   }
   public int getVDelayEven(){
      return bt.getVDelayEven();
   }
   public int getVActiveEven(){
      return bt.getVActiveEven();
   }
}
