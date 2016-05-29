package jx.wm.message;

import jx.wm.message.WMessage;
import jx.wm.Keycode;
import jx.wm.Qualifiers;

public class WKeyUpMessage extends WMessage
{
	public Keycode m_eKeyCode;
	public Keycode m_eRawCode;
	public Qualifiers m_eQual;
	
	public WKeyUpMessage (Keycode eKeyCode, Keycode eRawCode, Qualifiers eQual)
	{
		super (WMessage.KEY_UP);
		m_eKeyCode = new Keycode (eKeyCode);
		m_eRawCode = new Keycode (eRawCode);
		m_eQual	   = new Qualifiers (eQual);
	}
	public String toString ()
	{
		return new String ("MSG(KEY_UP)");
	}
}
