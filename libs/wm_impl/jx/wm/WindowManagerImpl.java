package jx.wm;

import java.lang.*;
import jx.zero.*;
import jx.zero.debug.*;
import jx.devices.fb.*;
import jx.wm.WDisplay;
import jx.wm.WView;
import jx.wm.WWindowInterface;
import jx.wm.WWindowImpl;
import jx.wm.WindowFlags;
import jx.wm.message.*;
import jx.wm.WindowManager;
import jx.wm.WFont;
import jx.wm.Keymap;
import jx.wm.Keycode;
import jx.wm.Qualifiers;

import jx.devices.Keyboard;
/*
import jx.keyboard.KeyboardImpl;
import jx.keyboard.KeyboardListener;
import jx.keyboard.MouseListener;
*/
public class WindowManagerImpl implements WindowManager
{
	Naming			m_cNaming;
	WDisplay		m_cDisplay = null;
	Keymap			m_cKeymap = new Keymap();
	Keycode			m_eRawCode = new Keycode();
	Keycode			m_eKeyCode = new Keycode();
	Qualifiers 		m_eQual    = new Qualifiers();

    int nPlugins = 0;
    int maxPlugins = 5;
    HotkeyPlugin[] plugins = new HotkeyPlugin[maxPlugins];
    int [] hotkeys = new int[maxPlugins];

	void testBitBlt()
	{
		PixelRect cOld[] = new PixelRect[1];
		PixelRect cNew[] = new PixelRect[1];
		WBitmap cScreen = m_cDisplay.getScreenBitmap();
		Clock clock = (Clock)m_cNaming.lookup("Clock");
		int nWidth = 400;
		int nHeight = 350;
		int nLoops = 100;
		int nBpp = 2;
	
		CycleTime starttimec = new CycleTime();
		CycleTime endtimec = new CycleTime();
		CycleTime diff = new CycleTime();
		
		cOld[0] = new PixelRect();
		cNew[0] = new PixelRect();
		
		clock.getCycles(starttimec);
		for (int i = 0; i < 100; i++)
		{
			cOld[0].setTo (0 + i, 50 + i, 400 + i, 350 + i);
			cNew[0].setTo (1 + i, 51 + i, 401 + i, 351 + i);
			cScreen.bitBlt (cOld, cNew, 1);	
		}
		clock.getCycles(endtimec);
		clock.subtract(diff, endtimec, starttimec);
		int time =  clock.toMilliSec(diff);
		int nSize = nWidth * nHeight * nBpp * nLoops;
		int nThroughput = nSize / (time * 1000);
		Debug.out.println ("testBitBlt() copied " + nSize + " Bytes, " + time + " milliseconds, " + nThroughput + " MB/s"); 
	}
	
	public WindowManagerImpl (String [] args)
	{
	    String wmName = args[0];
	    String fbName = args[1];
	    Naming cNaming = InitialNaming.getInitialNaming();
	    WDisplay cDisplay;
		m_cNaming = cNaming;
		Debug.out.print ("Starting window manager...\n");

		BootFS cBootFS = (BootFS)cNaming.lookup ("BootFS");
		if (cBootFS == null)
		{
			Debug.out.println ("unable to find BootFS");
			return;
		}
		ReadOnlyMemory cStdKeymap = cBootFS.getFile ("std.keymap");
		if (cStdKeymap == null)
		{
			Debug.out.println ("unable to load standard keymap");
			return;
		}
		setKeymap (cStdKeymap);

		FramebufferDevice fb = (FramebufferDevice)LookupHelper.waitUntilPortalAvailable(InitialNaming.getInitialNaming(), fbName);
		cDisplay = new WDisplay (fb);
		
		WWindowImpl.setDisplay (cDisplay);
	
		WFont.scanFonts ();

		cDisplay.startUpdate ();
		PixelRect cFrame = new PixelRect (cDisplay.getScreenBitmap().getBounds());
		WView cTopView = new WView (cDisplay.getScreenBitmap(), "desktop", cFrame);
		WView.setTopView (cTopView);
		cTopView.setEraseColor (64, 64, 64);
		cTopView.setBgColor (64, 64, 64);
		cTopView.invalidate ();	
		cTopView.updateRegions (true);				
		cDisplay.initMouseSprite ();
		cDisplay.endUpdate ();

		Debug.out.println ("Window manager is now ready to accept requests.");
		m_cDisplay = cDisplay;

	      	cNaming.registerPortal(this, wmName);
	}
	public void setKeymap (ReadOnlyMemory cKeymap)
	{
		m_cKeymap.m_nCapsLock 		= cKeymap.get32 (0);
		m_cKeymap.m_nScrollLock		= cKeymap.get32 (1);
		m_cKeymap.m_nNumLock		= cKeymap.get32 (2);
	    	m_cKeymap.m_nLShift		= cKeymap.get32 (3);
    		m_cKeymap.m_nRShift		= cKeymap.get32 (4);
	    	m_cKeymap.m_nLCommand		= cKeymap.get32 (5);
    		m_cKeymap.m_nRCommand		= cKeymap.get32 (6);
    		m_cKeymap.m_nLControl		= cKeymap.get32 (7);
    		m_cKeymap.m_nRControl		= cKeymap.get32 (8);
    		m_cKeymap.m_nLOption		= cKeymap.get32 (9);
    		m_cKeymap.m_nROption		= cKeymap.get32 (10);
    		m_cKeymap.m_nMenu		= cKeymap.get32 (11);
    		m_cKeymap.m_nLockSetting	= cKeymap.get32 (12);
		int nIndex = 13;
		for (int nChar = 0; nChar < 128; nChar++)
		{
			for (int nTable = 0; nTable < 9; nTable++)
				m_cKeymap.m_anMap[nChar][nTable] = cKeymap.get32 (nIndex++);
		}			
	}
	public WWindowInterface createWindow (String cName, PixelRect cFrame, WindowFlags nFlags)
	{
	    while (m_cDisplay == null)
		Thread.yield();
		Debug.out.println ("WindowManager::createWindow()");
		WWindowImpl cWnd = new WWindowImpl (cName, cFrame, nFlags, m_cDisplay);
		return (WWindowInterface)cWnd;
	}
	public void postInputEvent (jx.wm.message.WMessage cEvent)
	{
		Debug.out.println ("WindowManager.postInputEvent(" + cEvent + ")");
		switch (cEvent.getCode())
		{
			case WMessage.KEY_DOWN:
				WWindowImpl.KeyDown ((WKeyDownMessage)cEvent);
				break;
			case WMessage.KEY_UP:
				WWindowImpl.KeyUp ((WKeyUpMessage)cEvent);
				break;
		}
	}
	public void handleKeyUp (int nKeyCode)
	{
		setQualifiers (nKeyCode|0x80);
		m_eRawCode.setValue (nKeyCode);		
		m_eKeyCode.setValue (convertKeyCode (nKeyCode, m_eQual));
	    if (m_cDisplay != null)
			WWindowImpl.handleKeyUp (m_eKeyCode, m_eRawCode, m_eQual);
	}
	public void handleKeyDown (int nKeyCode)
	{
	    Debug.out.println("CODE:"+nKeyCode);
	    for (int i=0; i<nPlugins; i++) {
		if (nKeyCode == hotkeys[i]) {
		    final int n = i;
		    new Thread() {
			    public void run() {
				plugins[n].keyPressed();
			    }
			}.start();
		}
	    }
		setQualifiers (nKeyCode);
		m_eRawCode.setValue (nKeyCode);		
		m_eKeyCode.setValue (convertKeyCode (nKeyCode, m_eQual));
	    if (m_cDisplay != null)
			WWindowImpl.handleKeyDown (m_eKeyCode, m_eRawCode, m_eQual);
	  
	}
	public void handleMouseDown (int nButton)
	{
		//Debug.out.println ("handleMouseDown (" + nButton + ")");
	    if (m_cDisplay != null)
		WWindowImpl.handleMouseDown (nButton);
	}
	public void handleMouseUp (int nButton)
	{
	    if (m_cDisplay != null)
		WWindowImpl.handleMouseUp (nButton);
	}
	public void handleMouseMoved (int nDeltaX, int nDeltaY)
	{
	    if (m_cDisplay != null)
		WWindowImpl.handleMouseMoved (nDeltaX, nDeltaY);
	}
	public void handleMousePosition (int nPosX, int nPosY)
	{
	    if (m_cDisplay != null)
		WWindowImpl.handleMousePosition (nPosX, nPosY);
	}
    public int getDisplayWidth() {
	return m_cDisplay.getWidth();
    }

    public int getDisplayHeight() {
	return m_cDisplay.getHeight();
    }

    public void registerHotkeyPlugin(HotkeyPlugin plugin, int keycode) throws TooManyPluginsException {
	if (nPlugins == maxPlugins) throw new TooManyPluginsException();
	plugins[nPlugins] = plugin;
	hotkeys[nPlugins] = keycode;
	nPlugins++;
    }

	void setQualifiers (int nKeyCode)
	{
		switch (nKeyCode)
		{
		case 0x4b:
			m_eQual.value |= Qualifiers.LSHIFT;
			break;
		case 0x4b | 0x80:
			m_eQual.value &= ~Qualifiers.LSHIFT;
			break;
			
		case 0x56:
			m_eQual.value |= Qualifiers.RSHIFT;
			break;
		case 0x56 | 0x80:
			m_eQual.value &= ~Qualifiers.RSHIFT;
			break;

		case 0x5c:
			m_eQual.value |= Qualifiers.LCTRL;
			break;
		case 0x5c | 0x80:
			m_eQual.value &= ~Qualifiers.LCTRL;
			break;

		case 0x60:
			m_eQual.value |= Qualifiers.RCTRL;
			break;
		case 0x60 | 0x80:
			m_eQual.value &= ~Qualifiers.RCTRL;
			break;

		case 0x5d:
			m_eQual.value |= Qualifiers.LALT;
			break;
		case 0x5d | 0x80:
			m_eQual.value &= ~Qualifiers.LALT;
			break;

		case 0x5f:
			m_eQual.value |= Qualifiers.RALT;
			break;
		case 0x5f | 0x80:
			m_eQual.value &= ~Qualifiers.RALT;
			break;
		}
	}
	int convertKeyCode (int nRawKey, Qualifiers nQual)
	{
		int nTable = 0;
		int nQ = 0;

		if (nRawKey < 0 || nRawKey >= 128)
			return 0;

		if ((nQual.value & Qualifiers.SHIFT) != 0)
			nQ |= 0x01;
		if ((nQual.value & Qualifiers.CTRL) != 0)
			nQ |= 0x02;
		if ((nQual.value & Qualifiers.ALT) != 0)
			nQ |= 0x04;

		switch (nQ)
		{
		case 0x0:
			nTable = 0;
			break;
		case 0x1:
			nTable = 1;
			break;
		case 0x2:
			nTable = 2;
			break;
		case 0x3:
			nTable = 6;
			break;
		case 0x4:
			nTable = 3;
			break;
		case 0x5:
			nTable = 4;
			break;
		case 0x6:
			nTable = 7;
			break;
		case 0x7:
			nTable = 8;
			break;
		}
		return m_cKeymap.m_anMap[nRawKey][nTable];
	}	



	public static void main(String [] args)
	{
		new WindowManagerImpl (args);
	}
}
