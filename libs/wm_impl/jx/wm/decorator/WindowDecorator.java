package jx.wm.decorator;

import jx.devices.fb.*;
import jx.wm.WView;
import jx.wm.WindowFlags;
import jx.wm.decorator.AmigaDecorator;

public abstract class WindowDecorator
{
	WView	m_cView;

	public final static int HIT_NONE 	= 0x00;
	public final static int HIT_CLOSE	= 0x01;
	public final static int HIT_ZOOM	= 0x02;
	public final static int HIT_DEPTH	= 0x03;
	public final static int HIT_DRAG	= 0x04;
	public final static int HIT_SIZE_TOP	= 0x05;
	public final static int HIT_SIZE_BOTTOM = 0x06;
	public final static int HIT_SIZE_LEFT	= 0x07;
	public final static int HIT_SIZE_RIGHT	= 0x08;
	public final static int HIT_SIZE_LT	= 0x09;
	public final static int HIT_SIZE_RT	= 0x0a;
	public final static int HIT_SIZE_LB	= 0x0b;
	public final static int HIT_SIZE_RB	= 0x0c;
			
	public WindowDecorator (WView cView)
	{
		m_cView = cView;
	}
	public WView getView ()
	{
		return m_cView;
	}

	public abstract int hitTest (PixelPoint cPosition);
	public abstract void frameSized (PixelRect cNewFrame) ;
	public abstract PixelRect getBorderSize () ;
	public abstract PixelPoint getMinimumSize () ;
	public abstract void setTitle (String cTitle) ;
	public abstract void setFlags (WindowFlags nFlags) ;
	public abstract void fontChanged () ;
	public abstract void setWindowFlags (WindowFlags nFlags) ;
	public abstract void setFocusState (boolean bHasFocus) ;
	public abstract void setCloseButtonState (boolean bPushed) ;
	public abstract void setZoomButtonState (boolean bPushed) ;
	public abstract void setDepthButtonState (boolean bPushed) ;
	public abstract void render (PixelRect cUpdateRect) ;
	
	public static WindowDecorator create (WView cBorder, WindowFlags nFlags)
	{
/*
		return new AmigaDecorator (cBorder, nFlags);
		return new SimpleDecorator (cBorder, nFlags);
*/
		return new AmigaDecorator2 (cBorder, nFlags);
	}
}
