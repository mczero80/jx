package jx.wintv;
import jx.zero.Portal;

public interface BT878GeometryInterface extends Portal {
   void setGeometryOdd(int hscale, int hdelay, int hactive, 
		       int vscale, int vdelay, int vactive);
   void setGeometryEven(int hscale, int hdelay, int hactive, 
			int vscale, int vdelay, int vactive);
   
   int getHScaleOdd();
   int getHDelayOdd();
   int getHActiveOdd();
   
   int getVScaleOdd();
   int getVDelayOdd();
   int getVActiveOdd();

   int getHScaleEven();
   int getHDelayEven();
   int getHActiveEven();
   
   int getVScaleEven();
   int getVDelayEven();
   int getVActiveEven();
}
