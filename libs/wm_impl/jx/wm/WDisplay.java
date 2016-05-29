package jx.wm;

import jx.zero.*;
import jx.devices.*;
import jx.devices.fb.*;
import jx.wm.WSprite;
import jx.wm.WBitmap;

class WDisplay
{
	FramebufferDevice 	m_cDisplayDriver;
	WBitmap			m_cScreen;
	WSprite			m_cMouseSprite = null;
	PixelPoint		m_cMousePos = new PixelPoint (0, 0);
	
    int nWidth;
    int nHeight;
	
	public WDisplay (FramebufferDevice fb)
	{
	    m_cDisplayDriver = fb;
	    DeviceConfigurationTemplate[] cModes = m_cDisplayDriver.getSupportedConfigurations();
	    m_cScreen = null;
	    if (cModes == null)
		throw new Error ("WDisplay::WDisplay() unable to find a appropriate video mode!");
	    FramebufferConfigurationTemplate cMode = (FramebufferConfigurationTemplate)cModes[0];
	    if (cMode == null)
		throw new Error ("WDisplay::WDisplay() unable to find a appropriate video mode!");
	    
	    setMode (cMode.xresolution, cMode.yresolution, new ColorSpace (cMode.colorSpace));
	    /*PixelColor.setCMAP (s_acCMAP);*/
	}
	public void initMouseSprite ()
	{		
		WBitmap cBitmap = WBitmap.createWBitmap (8, 8, ColorSpace.CMAP8);
		for (int y = 0; y < 8; y++)
		{
			for (int x = 0; x < 8; x++)
				cBitmap.set8 (y * cBitmap.bytesPerLine() + x, s_anMouseShape[y*8 + x]);
		}
		m_cMouseSprite = new WSprite (cBitmap.getBounds(), new PixelPoint (0, 0), new PixelPoint (0, 0),
			m_cScreen, cBitmap);
	}
	public WBitmap getScreenBitmap ()
	{
		return m_cScreen;
	}
	public PixelRect getScreenResolution ()
	{
		return new PixelRect (m_cScreen.getBounds());
	}
	public int setMode (int nWidth, int nHeight, ColorSpace eColorSpace)
	{
		int nError;	
		
		Debug.out.println ("WDisplay::SetMode() " + nWidth + "/" + nHeight + "/" + eColorSpace);
		/*
		FramebufferConfigurationTemplate cConf[] = (FramebufferConfigurationTemplate[])m_cDisplayDriver.getSupportedConfigurations();
		if (cConf == null)
			throw new Error ("WDisplay::WDisplay() unable to get a suitable video mode!");
		FramebufferConfiguration mode = new FramebufferConfiguration (cConf[0].xresolution, cConf[0].yresolution,
			cConf[0].colorSpace);			
		*/
		FramebufferConfiguration mode = new FramebufferConfiguration (nWidth, nHeight, eColorSpace);			
		m_cDisplayDriver.open ((DeviceConfiguration)mode);			
		m_cScreen = WBitmap.createWBitmap (m_cDisplayDriver, 
			nWidth, nHeight, eColorSpace, 
			m_cDisplayDriver.getBytesPerLine(),
			m_cDisplayDriver.getFrameBuffer (),
			m_cDisplayDriver.getFrameBufferOffset ());
		this.nWidth = nWidth;
		this.nHeight = nHeight;
		return 0;					
	}
    public int getWidth() {
	return nWidth;
    }
    public int getHeight() {
	return nHeight;
    }
	public void startUpdate ()
	{
		m_cMouseSprite.hide();
		m_cDisplayDriver.startUpdate ();
	}
	public void endUpdate ()
	{
		m_cDisplayDriver.endUpdate ();
		m_cMouseSprite.unhide();
	}
	public PixelPoint getMousePos ()
	{
		return new PixelPoint (m_cMousePos);
	}
	void moveMouseBy (int nDeltaX, int nDeltaY)
	{
		m_cMousePos.m_nX += nDeltaX;
		m_cMousePos.m_nY += nDeltaY;
		if (m_cMousePos.m_nX < 0)
			m_cMousePos.m_nX = 0;
		if (m_cMousePos.m_nY < 0)
			m_cMousePos.m_nY = 0;
		if (m_cMousePos.m_nX >= m_cScreen.width())
			m_cMousePos.m_nX = m_cScreen.width() - 1;
		if (m_cMousePos.m_nY >= m_cScreen.height())
			m_cMousePos.m_nY = m_cScreen.height() - 1;
		startUpdate ();			
		m_cMouseSprite.moveTo (m_cMousePos);
		endUpdate ();
	}
    void moveMouseTo (int nPosX, int nPosY)
    {
		m_cMousePos.m_nX = nPosX;
		m_cMousePos.m_nY = nPosY;
		if (m_cMousePos.m_nX < 0)
			m_cMousePos.m_nX = 0;
		if (m_cMousePos.m_nY < 0)
			m_cMousePos.m_nY = 0;
		if (m_cMousePos.m_nX >= m_cScreen.width())
			m_cMousePos.m_nX = m_cScreen.width() - 1;
		if (m_cMousePos.m_nY >= m_cScreen.height())
			m_cMousePos.m_nY = m_cScreen.height() - 1;
		startUpdate ();			
		m_cMouseSprite.moveTo (m_cMousePos);
		endUpdate ();
    }

	static byte s_anMouseShape[] = {
		(byte)0xfe, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
		(byte)0xfe, (byte)0xfe, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
		(byte)0xfe, (byte)0x03, (byte)0xfe, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
		(byte)0xfe, (byte)0x03, (byte)0x03, (byte)0xfe, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
		(byte)0xfe, (byte)0x03, (byte)0x03, (byte)0x03, (byte)0xfe, (byte)0xff, (byte)0xff, (byte)0xff,
		(byte)0xfe, (byte)0x03, (byte)0x03, (byte)0x03, (byte)0x03, (byte)0xfe, (byte)0xff, (byte)0xff,
		(byte)0xfe, (byte)0x03, (byte)0x03, (byte)0x03, (byte)0x03, (byte)0x03, (byte)0xfe, (byte)0xff,
		(byte)0xfe, (byte)0xfe, (byte)0xfe, (byte)0xfe, (byte)0xfe, (byte)0xfe, (byte)0xfe, (byte)0xfe
	};
};
