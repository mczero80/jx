package jx.wm.message;

import jx.wm.message.WMessage;

public class WShowMessage extends WMessage
{
	public boolean m_bShow;
		
	public WShowMessage (boolean bShow)
	{
		super (WMessage.WINDOW_SHOW);
		m_bShow = bShow;
	}
	public String toString ()
	{
		return new String ("MSG(WINDOW_SHOW)");
	}
}
