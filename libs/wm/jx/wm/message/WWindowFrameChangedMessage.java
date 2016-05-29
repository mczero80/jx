package jx.wm.message;

import jx.wm.message.WMessage;
import jx.devices.fb.PixelRect;

public class WWindowFrameChangedMessage extends WMessage
{
	public PixelRect m_cFrame;
		
	public WWindowFrameChangedMessage (PixelRect cFrame)
	{
		super (WMessage.WINDOW_FRAME_CHANGED);
		m_cFrame = new PixelRect (cFrame);
	}
	public String toString ()
	{
		return new String ("MSG(WINDOW_FRAME_CHANGED)");
	}
}
