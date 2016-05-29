package jx.wm.message;

import jx.wm.message.WMessage;

public class WWindowActivatedMessage extends WMessage
{
	public boolean m_bActivated;
		
	public WWindowActivatedMessage (boolean bActivated)
	{
		super (WMessage.WINDOW_ACTIVATED);
		m_bActivated = bActivated;
	}
	public String toString ()
	{
		return new String ("MSG(WINDOW_ACTIVATED)");
	}
}
