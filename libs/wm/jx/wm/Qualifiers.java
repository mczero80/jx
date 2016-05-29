package jx.wm;

public class Qualifiers
{
    	public final static int	LSHIFT = 0x01;
    	public final static int RSHIFT = 0x02;
	public final static int SHIFT  = LSHIFT | RSHIFT;

	public final static int LCTRL  = 0x04;
	public final static int RCTRL  = 0x08;
	public final static int CTRL   = LCTRL | RCTRL;

	public final static int LALT   = 0x10;
	public final static int RALT   = 0x20;
	public final static int ALT    = LALT | RALT;

	public final static int REPEAT = 0x40;
	
	public int value;
	
	public Qualifiers ()
	{
		value = 0;
	}
	public Qualifiers (int value)
	{
		this.value = value;
	}
	public Qualifiers (Qualifiers q)
	{
		this.value = q.value;
	}
	public int getValue ()
	{
		return value;
	}
	public void setValue (int value)
	{
		this.value = value;
	}
	public String toString ()
	{
		return new String ("Qualifiers(" + Integer.toHexString (value) + ")");
	}
}
