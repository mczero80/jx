package jx.devices.fb;

public class PixelColor
{

	final public static PixelColor BLACK  = new PixelColor(0,0,0);
	final public static PixelColor WHITE  = new PixelColor(255,255,255);
	final public static PixelColor RED    = new PixelColor(255,0,0);
	final public static PixelColor GREEN  = new PixelColor(0,255,0);
	final public static PixelColor BLUE   = new PixelColor(0,0,255);
	final public static PixelColor YELLOW = new PixelColor(0,255,255);

	public byte m_nRed;
	public byte m_nGreen;
	public byte m_nBlue;
	public byte m_nAlpha;

	public static final int TRANSPARENT_CMAP8	= 0xff;
	public static final int TRANSPARENT_RGB15	= 0xffff;
	public static final int TRANSPARENT_RGB32	= 0xffffffff;	

	public PixelColor ()
	{
		m_nRed = m_nGreen = m_nBlue = m_nAlpha = 0;
	}

	public PixelColor (PixelColor cColor)
	{		
		m_nRed 		= cColor.m_nRed;
		m_nGreen 	= cColor.m_nGreen;
		m_nBlue 	= cColor.m_nBlue;
		m_nAlpha 	= cColor.m_nAlpha;
	}

	public PixelColor (byte nRed, byte nGreen, byte nBlue)
	{
		m_nRed = nRed;
		m_nGreen = nGreen;
		m_nBlue = nBlue;
		m_nAlpha = 0;
	}

	public PixelColor (byte nRed, byte nGreen, byte nBlue, byte nAlpha)
	{
		m_nRed 		= nRed;
		m_nGreen 	= nGreen;
		m_nBlue 	= nBlue;
		m_nAlpha 	= nAlpha;
	}

	public PixelColor (int nRed, int nGreen, int nBlue)
	{
		m_nRed   = (byte)nRed;
		m_nGreen = (byte)nGreen;
		m_nBlue  = (byte)nBlue;
		m_nAlpha = 0;
	}

	public PixelColor (int nRed, int nGreen, int nBlue, int nAlpha)
	{
		m_nRed   = (byte)nRed;
		m_nGreen = (byte)nGreen;
		m_nBlue  = (byte)nBlue;
		m_nAlpha = (byte)nAlpha;
	}

	public void setTo (PixelColor cColor)
	{		
		m_nRed 		= cColor.m_nRed;
		m_nGreen 	= cColor.m_nGreen;
		m_nBlue 	= cColor.m_nBlue;
		m_nAlpha 	= cColor.m_nAlpha;
	}
	public void setTo (byte nRed, byte nGreen, byte nBlue, byte nAlpha)
	{
		m_nRed = nRed;
		m_nGreen = nGreen;
		m_nBlue = nBlue;
		m_nAlpha = nAlpha;
	}
	public void setTo (int nRed, int nGreen, int nBlue, int nAlpha)
	{
		m_nRed   = (byte)nRed;
		m_nGreen = (byte)nGreen;
		m_nBlue  = (byte)nBlue;
		m_nAlpha = (byte)nAlpha;
	}
	public byte red ()
	{
		return m_nRed;
	}
	public byte green()
	{
		return m_nGreen;
	}
	public byte blue ()
	{
		return m_nBlue;
	}
	public byte alpha ()
	{
		return m_nAlpha;
	}
	public byte toCMAP8()
	{	
		/* Currently not implemented */
		return 0;
	}
	public short toRGB16 ()
	{
		return  (short)((int)(((((int)m_nRed & 0xff) >> 3) << 11)|((((int)m_nGreen &0xff) >> 2) << 5)|(((int)m_nBlue & 0xff) >> 3)) & 0xffff);  
	}
	public static PixelColor fromRGB16 (short value)
	{
	    int m_nBlue = (value & 0x1f) << 3;
	    int m_nGreen = ((value >>> 5) & 0x3f) << 2;
	    int m_nRed = ((value >>> 11) & 0x1f) << 3;
	    return  new PixelColor(m_nRed, m_nGreen, m_nBlue); 
	}
	public short toRGB15 ()
	{
		return  (short)(((m_nRed >> 3) << 10)|((m_nGreen >> 3) << 5)|(m_nBlue >> 3));  
	}
	public int toRGB24 ()
	{
		return (int)(((int)m_nRed << 16)|((int)m_nBlue << 8)|(int)m_nBlue);
	}
	public int toRGB32()
	{
		return (int)(((int)m_nAlpha << 24)|((int)m_nRed << 16)|((int)m_nBlue << 8)|(int)m_nBlue);
	}
	public static short invertRGB16 (short nColor)
	{
		short nInvert;
		
		nInvert = (short)((31 - (nColor >> 11)) << 11);
		nInvert|= (short)((63 - ((nColor >> 5) & 63)) << 5);
		nInvert|= (short)(31 - (nColor & 31));
		return nInvert;		
	}
    public static int invertRGB32 (int nColor)
    {
	int nInvert;
	nInvert = (int)(255 - (nColor & 0xff));
	nInvert|= (int)((255 - ((nColor >> 8) & 0xff)) << 8);
	nInvert|= (int)((255 - ((nColor >> 16) & 0xff)) << 16);
	nInvert|= (int)((255 - ((nColor >> 24) & 0xff)) << 24);
	return nInvert;
    }

    public static short RGB32toRGB16 (int nColor)
    {	
	return (short)((((((nColor) >> 16) & 255) >> 3) << 11) | (((((nColor) >> 8) & 255) >> 2) << 5) | (((nColor) & 255) >> 3));
    }
    public static int RGB16toRGB32 (short nColor)
    {
	return (((nColor & 31) << 3)|(((nColor >> 5) & 63) << 10)|(((nColor >> 11) & 31) << 19));
    }
    public String toString ()
    {
		if (this == null)
			return "<null>";
		return new String ("[" + m_nRed + "," + m_nGreen + "," + m_nBlue + "," + m_nAlpha + "]");
	}

};
