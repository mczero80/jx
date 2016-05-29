package jx.wm;

public class WindowFlags
{
	/**
	 * Create a window without a border.
	 */
	public static final int WND_NO_BORDER 		= 0x001;
	/**
	 * The window will be not movable
	 */
	public static final int WND_NOT_MOVABLE 	= 0x002;
	/** 
	 * The window will be not zoomable
	 */ 
	public static final int WND_NOT_ZOOMABLE 	= 0x004;
	public static final int WND_NOT_CLOSABLE	= 0x008;
	public static final int WND_NOT_MINIMIZABLE	= 0x010;
	public static final int WND_BACKMOST		= 0x020;
	public static final int WND_NOT_H_RESIZABLE	= 0x040;
	public static final int WND_NOT_V_RESIZABLE	= 0x080;
	public static final int WND_NOT_RESIZABLE	= WND_NOT_H_RESIZABLE|WND_NOT_V_RESIZABLE;
	public static final int WND_SYSTEM			= 0x100;
	public static final int WND_FULL_UPDATE_ON_H_RESIZE = 0x800;
	public static final int WND_FULL_UPDATE_ON_V_RESIZE = 0x1000;
	public static final int WND_FULL_UPDATE_ON_RESIZE = WND_FULL_UPDATE_ON_H_RESIZE|WND_FULL_UPDATE_ON_V_RESIZE;
	/**
	 * Never let the window gain the input focus.
	 */
	public static final int WND_NO_FOCUS        = 0x200;
	/**
	 * Don't clear out of date regions of the window.
	 */
	public static final int WND_TRANSPARENT		= 0x400;
	
	private int m_nValue;
	
	public WindowFlags ()
	{
		m_nValue = 0;
	}
	public WindowFlags (int nValue)
	{
		m_nValue = nValue;
	}
	public WindowFlags (WindowFlags nFlags)
	{
		m_nValue = nFlags.m_nValue;
	}
	public int getValue ()
	{
		return m_nValue;
	}
	public void setValue (int nValue)
	{
		m_nValue = nValue;
	}
	public boolean isZoomable()
	{
		return (m_nValue & WND_NOT_ZOOMABLE) == 0;
	}
	public boolean isMovable()
	{
		return (m_nValue & WND_NOT_MOVABLE) == 0;
	}
	public boolean isClosable()
	{
		return (m_nValue & WND_NOT_CLOSABLE) == 0;
	}
	public boolean hasBorder()
	{
		return (m_nValue & WND_NO_BORDER) == 0;
	}
}
