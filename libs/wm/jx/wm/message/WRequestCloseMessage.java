package jx.wm.message;

import jx.wm.message.WMessage;

public class WRequestCloseMessage extends WMessage
{
	public WRequestCloseMessage ()
	{
		super (WMessage.REQUEST_CLOSE);
	}
	public String toString ()
	{
		return new String ("MSG(REQUEST_CLOSE)");
	}
}
