package jx.wm.message;

import jx.wm.message.WMessage;

public class WQuitMessage extends WMessage
{
	public WQuitMessage ()
	{
		super (WMessage.QUIT);
	}
	public String toString ()
	{
		return new String ("MSG(QUIT)");
	}
}
