package jx.wm;

import jx.wm.WRegion;
import jx.wm.message.*;
import jx.wm.WBitmap;
import jx.zero.*;
import jx.zero.debug.*;
import jx.devices.fb.*;

public interface WWindowInterface extends jx.zero.Portal {
	public static final int MOUSE_INSIDE		= 0x01;
	public static final int MOUSE_OUTSIDE		= 0x02;
	public static final int MOUSE_ENTERED		= 0x03;
	public static final int MOUSE_EXITED		= 0x04;

	public String toString ();
	public void show (boolean bShow);
	public void postMessage (jx.wm.message.WMessage cMsg);
	public jx.wm.message.WMessage peekMessage ();
	public PixelRect getFrame (boolean bBorderFrame);
	public void setFrame (PixelRect cFrame);
	public void movePenTo (int x, int y);		
	public void movePenTo (PixelPoint cPos);	
	public void movePenBy (PixelPoint cPos);	
	public void drawLine (int x, int y);
	public void invertLine (int x, int y);
	public void invertRect (int x, int y, int w, int h);
	public void drawLine (PixelPoint cToPos);
	public void setFgColor (PixelColor cColor);
	public void setBgColor (PixelColor cColor);
	public void setEraseColor (PixelColor cColor);
	public void setFgColor (int r, int g, int b, int a);
	public void setBgColor (int r, int g, int b, int a);
	public void setEraseColor (int r, int g, int b, int a);
	public void setFgColor (int r, int g, int b);
	public void setBgColor (int r, int g, int b);
	public void setClip (int x, int y, int w, int h);
	public void setEraseColor (int r, int g, int b);
	public void fillRect (PixelRect cRect);
	public void fillRect (PixelRect cRect, DrawingMode nMode);
	public void fillRect (int x, int y, int w, int h);
	public void drawBitmap (WBitmap cBitmap, int x, int y);
	public void drawBitmap (WBitmap cBitmap, PixelPoint cPos, DrawingMode nMode);
	public void drawBitmap (WBitmap cBitmap, PixelRect cSrcRect, PixelRect cDstRect, DrawingMode nDrawingMode);
	public void drawString (String cString);
	public void drawString (String cString, int nLength);
	public void startUpdate();
	public void endUpdate ();
	public void moveReply ();
	public void quit ();
	public void setTitle (String cString);
	public PixelRect getBorderSize ();
	public void setFlags (WindowFlags eFlags);
	public void makeFocus (boolean bFocus);
	public void moveToFront ();
	public void setBitmap (WBitmap pcBitmap);
	public void startRepaint ();
	public void endRepaint ();

	public void enableBackBuffer(boolean enable);
	public void resetBackBuffer(); 
	public void drawBackBuffer(); 
}
