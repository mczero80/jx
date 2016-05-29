package jx.wm;

public class Keycode
{
	public static final int VK_BACKSPACE	  	= 0x08;
	public static final int VK_ENTER	      	= 0x0a;
	public static final int VK_RETURN	      	= 0x0a;
	public static final int VK_SPACE	      	= 0x20;
	public static final int VK_TAB	        	= 0x09;
	public static final int VK_ESCAPE	      	= 0x1b;
	public static final int VK_LEFT_ARROW	  	= 0x1c;
	public static final int VK_RIGHT_ARROW  	= 0x1d;
	public static final int VK_UP_ARROW	    	= 0x1e;
	public static final int VK_DOWN_ARROW	  	= 0x1f;
	public static final int VK_INSERT	      	= 0x05;
	public static final int VK_DELETE	      	= 0x7f;
	public static final int VK_HOME	        	= 0x01;
	public static final int VK_END	        	= 0x04;
	public static final int VK_PAGE_UP	    	= 0x0b;
	public static final int VK_PAGE_DOWN	  	= 0x0c;
	public static final int VK_FUNCTION_KEY 	= 0x10;
	public static final int VK_F1			= 0x02;
	public static final int VK_F2			= 0x03;
	public static final int VK_F3			= 0x04;
	public static final int VK_F4			= 0x05;
	public static final int VK_F5			= 0x06;
	public static final int VK_F6			= 0x07;
	public static final int VK_F7			= 0x08;
	public static final int VK_F8			= 0x09;
	public static final int VK_F9			= 0x0a;
	public static final int VK_F10			= 0x0b;
	public static final int VK_F11			= 0x0c;
	public static final int VK_F12			= 0x0d;
	
	public int value;
	public Keycode ()
	{
		this.value = 0;
	}
	public Keycode (int value)
	{
		this.value = value;
	}
	public Keycode (Keycode c)
	{
		this.value = c.value;
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
		return new String ("Keycode(" + Integer.toHexString (value) + ")");
	}
}
