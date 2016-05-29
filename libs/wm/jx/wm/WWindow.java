package jx.wm;

import jx.zero.*;
import jx.zero.debug.*;
import jx.wm.*;
import jx.wm.message.*;
import jx.devices.fb.*;
import java.lang.*;

/**
 * Creation and management of a window.
 * @author Juergen Obernolte
 */
public class WWindow implements Runnable
{
	static WindowManager s_cWindowManager = null;
	WWindowInterface m_cWindow;
	Thread m_cThread;
	PixelRect m_cFrame = new PixelRect ();

    /**
     * Create a window of specified size and attributes.
     *
     * @param cTitle   Title of the window
     * @param cFrame   Frame rectangle of the window
     * @param nFlags   Flags for the window (currently unused)
     */
	public WWindow (String cTitle, PixelRect cFrame, WindowFlags nFlags)
	{
		if (s_cWindowManager == null)
		{
			Naming naming = InitialNaming.getInitialNaming();
			s_cWindowManager = (WindowManager) LookupHelper.waitUntilPortalAvailable(naming, "WindowManager");
		}
		DomainManager domainManager = (DomainManager) InitialNaming.getInitialNaming().lookup("DomainManager");
		int domainID = domainManager.getCurrentDomain().getID();

		m_cWindow = s_cWindowManager.createWindow (cTitle+" (Domain "+domainID+")", cFrame, nFlags);
		m_cFrame.setTo (cFrame);
		m_cThread = new Thread (this, cTitle);
	}
	public void show (boolean bState)
	{
		if (bState == true)
		{
			m_cThread.start ();
			m_cWindow.show (true);
		}
		else
		{
			m_cWindow.show (false);
			m_cThread.stop ();
		}
	}
	public void run ()
	{
		boolean bRun = true;

		/* crash?
			Debug.out.println ("WWindow::run(" + m_cWindow + ") " +
												 m_cThread.getName ());
		*/

		while (bRun == true)
		{
			jx.wm.message.WMessage cMsg;
			cMsg = m_cWindow.peekMessage ();
			//Debug.out.println ("MyWindow::run() received message " + cMsg);
			switch (cMsg.getCode ())
			{
				case jx.wm.message.WMessage.PAINT:
				{
					jx.wm.message.WPaintMessage cPaintMsg =
						(jx.wm.message.WPaintMessage) cMsg;
					m_cWindow.startRepaint ();
					m_cWindow.startUpdate ();
					//for (int i = 0; i < cPaintMsg.m_cRegion.countRects (); i++)
					//{
					if (cPaintMsg.m_cRegion==null) {
					    throw new Error("No region info in paint message");
					}
					paint (cPaintMsg.m_cRegion.getBounds());
					//}						
					m_cWindow.endUpdate ();
					m_cWindow.endRepaint ();
					break;
				}
				case jx.wm.message.WMessage.KEY_DOWN:
				{
					jx.wm.message.WKeyDownMessage cKeyMsg = 
						(jx.wm.message.WKeyDownMessage) cMsg;
					keyDown (cKeyMsg.m_eKeyCode, cKeyMsg.m_eRawCode, cKeyMsg.m_eQual);
					break;
				}
				case jx.wm.message.WMessage.KEY_UP:
				{
					jx.wm.message.WKeyUpMessage cKeyMsg = 
						(jx.wm.message.WKeyUpMessage) cMsg;
					keyUp (cKeyMsg.m_eKeyCode, cKeyMsg.m_eRawCode, cKeyMsg.m_eQual);
					break;
				}
				case jx.wm.message.WMessage.WINDOW_FRAME_CHANGED:
				{
					jx.wm.message.WWindowFrameChangedMessage cFrameMsg =
						(jx.wm.message.WWindowFrameChangedMessage) cMsg;
					if (cFrameMsg.m_cFrame.width() != m_cFrame.width() ||
						cFrameMsg.m_cFrame.height() != m_cFrame.height())
						windowResized (cFrameMsg.m_cFrame);
					else
						windowMoved (cFrameMsg.m_cFrame);
					m_cFrame.setTo (cFrameMsg.m_cFrame);
					m_cWindow.moveReply ();
					break;
				}
				case jx.wm.message.WMessage.WINDOW_ACTIVATED:
				{
					jx.wm.message.WWindowActivatedMessage cActMsg =
						(jx.wm.message.WWindowActivatedMessage) cMsg;
					windowActivated (cActMsg.m_bActivated);
					break;
				}
				case jx.wm.message.WMessage.MOUSE_DOWN:
				{
					jx.wm.message.WMouseDownMessage cMouseMsg =
						(jx.wm.message.WMouseDownMessage) cMsg;
					mouseDown (cMouseMsg.m_cPos, cMouseMsg.m_nButton);
					break;
				}
				case jx.wm.message.WMessage.MOUSE_UP:
				{
					jx.wm.message.WMouseUpMessage cMouseMsg =
						(jx.wm.message.WMouseUpMessage) cMsg;
					mouseUp (cMouseMsg.m_cPos, cMouseMsg.m_nButton);
					break;
				}
				case jx.wm.message.WMessage.MOUSE_MOVED:
				{
					jx.wm.message.WMouseMovedMessage cMouseMsg =
						(jx.wm.message.WMouseMovedMessage) cMsg;
					mouseMoved (cMouseMsg.m_cPos, cMouseMsg.m_nTransit);
					break;
				}
				case jx.wm.message.WMessage.WINDOW_SHOW:
				{
					jx.wm.message.WShowMessage cActMsg =
						(jx.wm.message.WShowMessage) cMsg;
					if (cActMsg.m_bShow)
						windowShown ();
					else
						windowClosed ();
					break;
				}
				case jx.wm.message.WMessage.REQUEST_CLOSE:
				{
					closeRequested ();
					break;
				}
				case jx.wm.message.WMessage.QUIT:
				{
					bRun = false;
					break;
				}
				default:
				{
					handleMessage (cMsg);
					break;
				}
			}
		}
		Debug.out.println ("MyWindow::run(" + m_cWindow + ") stopping");
		m_cWindow = null;
	}
	/**
	 * Hook function called, when the window has to be redrawn. If you
	 * want to update part of the window by yourself, don't call this 
	 * method directly! Use "invalidate" instead.
	 */
	public void paint (PixelRect cUpdateRect)
	{
	}
	/**
	 * Handle a message.
	 */
	public void handleMessage (jx.wm.message.WMessage cMsg)
	{
	}
	/** 
	 * Hook function called, when the window gets resized 
	 */
	public void windowResized (PixelRect cRect)
	{
	}
	public void windowMoved (PixelRect cRect)
	{
	}
	public void keyDown (Keycode eKeyCode, Keycode eRawCode, Qualifiers eQualifiers)
	{
	}
	public void keyUp (Keycode eKeyCode, Keycode eRawCode, Qualifiers eQualifiers)
	{
	}
	public void mouseDown (PixelPoint cMousePos, int nButton)
	{
	}
	public void mouseUp (PixelPoint cMousePos, int nButton)
	{
	}
	public void mouseMoved (PixelPoint cMousePos, int nTransit)
	{
	}
	/**
	 * Hook function called when window is displayed on screen
	 */
	public void windowShown ()
	{
		Debug.out.println ("WWindow::windowShown()");
	}
	/**
	 * Hook function called when window has been hidden
	 */
	public void windowClosed ()
	{
		Debug.out.println ("WWindow::windowClosed()");
		quit ();
	}
	/**
	 * Hook function called when window is activated
	 */
	public void windowActivated (boolean bActivated)
	{
	}
	/*
	 * Hook function called when user has clicked the close button.
	 * Default behaviour is that the window is hidden.
	 */
	public void closeRequested ()
	{
		show (false);
	}
	/*
	 * Destroy the window.
	 */
	public void quit ()
	{
		m_cWindow.quit ();
	}
        // @ Marco Winter
	public void postMessage (jx.wm.message.WMessage cMsg)
        {
	    m_cWindow.postMessage(cMsg);
	}
	/**
	 * Invalidate a certain rectangular area of the window. This call will
	 * invoke the "paint" hook function.
	 */
	public void invalidate (PixelRect cRect)
	{
		m_cWindow.startUpdate ();
		paint (new PixelRect (cRect));
		m_cWindow.endUpdate ();
	}
        public  void startUpdate()
        {
	    m_cWindow.startUpdate();
	}
        public  void endUpdate()
        {
	    m_cWindow.endUpdate();
	}
	
	public void movePenTo (int x, int y)
	{
		m_cWindow.movePenTo (x, y);
	}
	public void drawLine (int x, int y)
	{
		m_cWindow.drawLine (x, y);
	}
	public void invertLine (int x, int y)
	{
		m_cWindow.invertLine (x, y);
	}
	public PixelRect getFrame ()
	{
		return m_cWindow.getFrame(false);
	}
	public PixelRect getFrame (boolean bBorderFrame)
	{
		return m_cWindow.getFrame(bBorderFrame);
	}
	public void setFrame (PixelRect cFrame)
	{
		m_cWindow.setFrame (cFrame);
	}
	public void setTitle (String cTitle)
	{
		m_cWindow.setTitle (cTitle);
	}
	public void movePenTo (PixelPoint cPos)
	{
		m_cWindow.movePenTo (cPos);
	}
	public void movePenBy (PixelPoint cPos)
	{
		m_cWindow.movePenBy (cPos);
	}
	public void drawLine (PixelPoint cToPos)
	{
		m_cWindow.drawLine (cToPos);
	}
	public void setFgColor (PixelColor cColor)
	{
		m_cWindow.setFgColor (cColor);
	}
	public void setBgColor (PixelColor cColor)
	{
		m_cWindow.setBgColor (cColor);
	}
	public void setEraseColor (PixelColor cColor)
	{
		m_cWindow.setEraseColor (cColor);
	}
	public void setFgColor (int r, int g, int b, int a)
	{
		m_cWindow.setFgColor (r, g, b, a);
	}
	public void setBgColor (int r, int g, int b, int a)
	{
		m_cWindow.setBgColor (r, g, b, a);
	}
	public void setEraseColor (int r, int g, int b, int a)
	{
		m_cWindow.setEraseColor (r, g, b, a);
	}
	public void setFgColor (int r, int g, int b)
	{
		m_cWindow.setFgColor (r, g, b);
	}
	public void setBgColor (int r, int g, int b)
	{
		m_cWindow.setBgColor (r, g, b);
	}
	public void setEraseColor (int r, int g, int b)
	{
		m_cWindow.setEraseColor (r, g, b);
	}
	public void fillRect (PixelRect cRect)
	{
		m_cWindow.fillRect (cRect);
	}
	public void fillRect (PixelRect cRect, DrawingMode nMode)
	{
		m_cWindow.fillRect (cRect, nMode);
	}
	public void fillRect (int x, int y, int w, int h)
	{
		m_cWindow.fillRect (x, y, w, h);
	}
	public void drawBitmap (WBitmap cBitmap, int x, int y)
	{
		m_cWindow.drawBitmap (cBitmap, x, y);
	}
	public void drawBitmap (WBitmap cBitmap, PixelPoint cPos, DrawingMode nMode)
	{
		m_cWindow.drawBitmap (cBitmap, cPos, nMode);
	}
	public void drawBitmap (WBitmap cBitmap, PixelRect cSrcRect, PixelRect cDstRect, DrawingMode nDrawingMode)
	{
		m_cWindow.drawBitmap (cBitmap, cSrcRect, cDstRect, nDrawingMode);
	}
	public void drawString (String cString)
	{
		m_cWindow.drawString (cString);
	}
	public void drawString (String cString, int nLength)
	{
		m_cWindow.drawString (cString, nLength);
	}
	public void setClip (int x, int y, int w, int h)
	{
		m_cWindow.setClip (x, y, w, h);
	}
	public PixelRect getBorderSize ()
	{
		return m_cWindow.getBorderSize();
	}
	public void setFlags (WindowFlags eFlags)
	{
		m_cWindow.setFlags (eFlags);
	}
    public void makeFocus (boolean bFocus)
    {
		m_cWindow.makeFocus (bFocus);
    }
	/**
	 * Move the window in front of all other windows.
	 */
	public void moveToFront ()
	{
		m_cWindow.moveToFront ();
	}

    public int getDisplayWidth() {
	return s_cWindowManager.getDisplayWidth();
    }
    public int getDisplayHeight() {
	return s_cWindowManager.getDisplayHeight();
    }

	public void invertRect (int x, int y, int w, int h)
	{
		m_cWindow.invertRect (x, y, w, h);
	}
	/**
	 * Sets the bitmap, where all drawing commands will go to.
	 * if pcBitmap == null the screen will used for drawing.
	 * Example usage for a backbuffer:
	 * 
	 * void paint (PixelRect cUpdateRect)
	 * {
	 *   setBitmap (myBackBuffer); // set the back buffer
	 *   do drawing stuff...       // all drawings will go to the back buffer
	 *   setBitmap (null);         // set screen
	 *   drawBitmap (myBackBuffer, 0, 0); // draw back buffer
	 * }
	 */
	public void setBitmap (WBitmap pcBitmap)
	{
		m_cWindow.setBitmap (pcBitmap);
	}

	final public void enableBackBuffer(boolean enable) {
		m_cWindow.enableBackBuffer(enable);
	}

	final public void resetBackBuffer() {
		m_cWindow.resetBackBuffer();
	}

	public void drawBackBuffer() {
		m_cWindow.drawBackBuffer();
	}

}
