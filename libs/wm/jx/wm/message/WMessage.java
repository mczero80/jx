package jx.wm.message;

import jx.zero.*;

public class WMessage
{
    //WMessage	m_cNext;
    //WMessage	m_cPrev;
	int		m_nCode;
	
	public static final int	PAINT 			= 0x01;
	public static final int KEY_DOWN 		= 0x02;
	public static final int KEY_UP   		= 0x03;
	public static final int WINDOW_FRAME_CHANGED 	= 0x04;
	public static final int WINDOW_ACTIVATED	= 0x05;
	public static final int MOUSE_DOWN		= 0x06;
	public static final int MOUSE_UP		= 0x07;
	public static final int MOUSE_MOVED		= 0x08;
	public static final int WINDOW_SHOW 	= 0x10;
	public static final int QUIT			= 0x20;
	public static final int REQUEST_CLOSE	= 0x40;
	
	public WMessage (int nCode)
	{
	    //	m_cNext = null;
	    //	m_cPrev = null;
		m_nCode = nCode;
	}
	public int getCode ()
	{
		return m_nCode;
	}
	public String toString ()
	{
		return new String ("MSG(" + m_nCode + ")");
	}
};
