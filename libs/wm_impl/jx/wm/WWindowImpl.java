package jx.wm;

import jx.devices.fb.*;
import jx.wm.WRegion;
import jx.wm.WindowBorder;
import jx.wm.decorator.WindowDecorator;
import jx.wm.message.*;
import jx.wm.WDisplay;
import jx.wm.WWindowInterface;
import jx.wm.WindowFlags;
import jx.zero.*;
import jx.zero.debug.*;
import jx.wm.Qualifiers;
import jx.wm.Keycode;
import jx.wm.WMessageQueue;
import jx.wm.WSprite;

public class WWindowImpl implements WWindowInterface
{
	static final boolean    m_bBackBuffer = false;

	static WWindowImpl	s_cWindows	= null;
	static WWindowImpl	s_cActiveWindow = null;
	static WWindowImpl	s_cLastMouseWindow = null;
	static WDisplay		s_cDisplay	= null;
	static WWindowImpl	s_cDragWindow	= null;
		
	WWindowImpl		m_cPrev;
	WWindowImpl		m_cNext;
	String			m_cTitle;
	WMessageQueue		m_cPostedMsgs;
	CPUManager		m_cCPUManager;
	CPUState		m_cWaitingThread;
	WindowBorder		m_cWndBorder;
	WindowDecorator 	m_cDecorator;
	WindowFlags		m_nFlags;
	WDisplay		m_cDisplay;
	boolean			m_bHasFocus;
	WView			m_cTopView;
	boolean			m_bBorderHit;
	DrawingMode		m_nDrawingMode = new DrawingMode();
	PixelRect		m_cTmpRect = new PixelRect ();

	WBitmap         m_cBackBuffer = null;
/*	
	public static final int WND_NO_BORDER 		= 0x01;
	public static final int WND_NOT_MOVABLE 	= 0x02;
	public static final int WND_NOT_ZOOMABLE 	= 0x04;
	public static final int WND_NOT_CLOSABLE	= 0x08;
	public static final int WND_NOT_MINIMIZABLE	= 0x10;
	public static final int WND_BACKMOST		= 0x20;
*/
	public static final int MOUSE_INSIDE		= 0x01;
	public static final int MOUSE_OUTSIDE		= 0x02;
	public static final int MOUSE_ENTERED		= 0x03;
	public static final int MOUSE_EXITED		= 0x04;
		
	public static void setDisplay (WDisplay cDisplay)
	{
		s_cDisplay = cDisplay;
		WindowBorder.setDisplay (cDisplay);
	}
	public WWindowImpl (String cTitle, PixelRect cFrame, WindowFlags nFlags, WDisplay cDisplay)
	{
		Naming cNaming = InitialNaming.getInitialNaming();
		m_cCPUManager = (CPUManager)cNaming.lookup("CPUManager");
		
		m_cTitle = new String (cTitle);
		m_cDisplay = cDisplay;
		m_bBorderHit = false;
		
		m_cPrev = null;
		if ((m_cNext = s_cWindows) != null)
			s_cWindows.m_cPrev = this;
		s_cWindows = this;
		m_cPostedMsgs = new WMessageQueue ();

		PixelRect cBorderFrame = new PixelRect (cFrame);
		
		m_nFlags = new WindowFlags (nFlags);
		m_cWndBorder = new WindowBorder (this, null, cTitle + "_wnd_border", nFlags);
		m_cDecorator =	WindowDecorator.create (m_cWndBorder, nFlags);
		m_cWndBorder.setDecorator (m_cDecorator);
		m_cTopView = m_cWndBorder.getClient();

		PixelRect cBorders = m_cDecorator.getBorderSize ();

		cBorderFrame.resize (-cBorders.m_nX0, -cBorders.m_nY0, cBorders.m_nX1, cBorders.m_nY1);
		m_cWndBorder.setFrame (cBorderFrame);
		m_cDecorator.setTitle (cTitle);
		WView.getTopView().addChild (m_cWndBorder);
		m_cWndBorder.show (false);
	}	
	public PixelRect getBorderSize ()
	{
		return m_cDecorator.getBorderSize();
	}
	public void setFlags (WindowFlags eFlags)
	{
		m_nFlags.setValue (eFlags.getValue());
		m_cDecorator.setFlags (m_nFlags);		
	}
	public void setTitle (String cString)
	{
		m_cDecorator.setTitle (cString);
	}
	public void finalize ()
	{
		//Debug.out.println ("WWindowImpl.finalize()");
		m_cTitle = null;
		m_cPostedMsgs = null;
		m_nFlags = null;
		m_cWndBorder = null;
		m_cTopView = null;
		m_cDecorator = null;
	}
	public WView getTopView()
	{
		return m_cWndBorder;
	}
	public void show (boolean bShow, boolean bPostMessage)
	{
		//Debug.out.println (m_cTitle + ": WWindow::show(" + bShow + ")");
		m_cDisplay.startUpdate ();
		WSprite.hide ();
		if (m_cWndBorder != null)
		{
			m_cWndBorder.show (bShow);
			m_cWndBorder.getParent().updateRegions (false);			
		}		
		WSprite.unhide ();
		m_cDisplay.endUpdate ();
		if (bPostMessage)
			postMessage (new jx.wm.message.WShowMessage (bShow));
	}	
	public void show (boolean bShow)
	{
		show (bShow, true);
	}
	public void moveToFront ()
	{
		//Debug.out.println (m_cTitle + ": WWindowImpl::moveToFront()");
		m_cDisplay.startUpdate ();
		WSprite.hide();
		m_cWndBorder.moveToFront ();
		m_cWndBorder.getParent().updateRegions (true);
		WSprite.unhide ();
		m_cDisplay.endUpdate ();
	}
	public void postMessage (jx.wm.message.WMessage cMsg)
	{
		//Debug.out.println (m_cTitle + ": WWindow::postMessage(" + cMsg + ")");
		m_cPostedMsgs.addMessage (cMsg);
		if (m_cWaitingThread != null)
		{
			m_cCPUManager.unblock (m_cWaitingThread);
			m_cWaitingThread = null;
		}			
	}
	public jx.wm.message.WMessage lastMessage ()
	{
		return m_cPostedMsgs.lastMessage ();
	}
	public jx.wm.message.WMessage peekMessage ()
	{
		if (m_cWaitingThread != null)
			return null;
		while (m_cPostedMsgs.isEmpty())
		{
			m_cWaitingThread = m_cCPUManager.getCPUState();
			m_cCPUManager.block ();		
		}			
		m_cWaitingThread = null;			
		jx.wm.message.WMessage cMsg = m_cPostedMsgs.nextMessage ();
		//Debug.out.println (m_cTitle + ": WWindow::peekMessage(" + cMsg + ")");
		return cMsg;
	}
	public void setFrame (PixelRect cFrame, boolean bBorderFrame)
	{
		//Debug.out.println (m_cTitle + ": WWindow::setFrame " + cFrame);
		m_cDisplay.startUpdate ();
		WSprite.hide ();
		if (null != m_cWndBorder)
		{
			if (bBorderFrame)
			{
				m_cWndBorder.setFrame (cFrame);
			}
			else			
			{
				PixelRect cBorders = m_cDecorator.getBorderSize ();
				m_cWndBorder.
					setFrame (new PixelRect
									(cFrame.m_nX0 - cBorders.m_nX0, 
									 cFrame.m_nY0 - cBorders.m_nY0,
									 cFrame.m_nX1 + cBorders.m_nX1,
									 cFrame.m_nY1 + cBorders.m_nY1));
			}									 	
		}
		if (null != m_cWndBorder.getParent())
			m_cWndBorder.getParent().updateRegions (false);
		WSprite.unhide ();
		m_cDisplay.endUpdate ();			
	}
	public void setFrame (PixelRect cFrame)
	{
		setFrame (cFrame, false);
	}
	public PixelRect getFrame (boolean bBorderFrame)
	{
		PixelRect cFrame = m_cWndBorder.getFrame();
		if (!bBorderFrame)
		{
			PixelRect cBorders = m_cDecorator.getBorderSize ();
			cFrame.m_nX0 += cBorders.m_nX0;				
			cFrame.m_nY0 += cBorders.m_nY0;				
			cFrame.m_nX1 -= cBorders.m_nX1;				
			cFrame.m_nY1 -= cBorders.m_nY1;				
		}
		return cFrame;
	}
	public String toString ()
	{
		return new String ("WWindow(" + m_cTitle + ")");
	}	
	public void movePenTo (int x, int y)
	{
		m_cWndBorder.getClient().movePenTo (x, y);
	}
	public void movePenTo (PixelPoint cPos)
	{
		m_cWndBorder.getClient().movePenTo (cPos);
	}
	public void movePenBy (PixelPoint cPos)
	{
		m_cWndBorder.getClient().movePenBy (cPos);
	}
	public void invertRect (int x, int y, int w, int h)
	{
		m_cDisplay.startUpdate ();
		m_nDrawingMode.setValue (DrawingMode.DM_INVERT);
		m_cWndBorder.getClient().fillRect (new PixelRect (x, y, x + w - 1, y + h -1), m_nDrawingMode);
		m_cDisplay.endUpdate ();
	}
	public void drawLine (int x, int y)
	{
		m_cDisplay.startUpdate ();
		m_cWndBorder.getClient().drawLine (x, y);
		m_cDisplay.endUpdate ();			
	}
	public void invertLine (int x, int y)
	{
		m_cDisplay.startUpdate ();
		m_nDrawingMode.setValue (DrawingMode.DM_INVERT);
		m_cWndBorder.getClient().drawLine (x, y, m_nDrawingMode);
		m_cDisplay.endUpdate ();
	}
	public void drawLine (PixelPoint cToPos)
	{
		m_cDisplay.startUpdate ();
		m_cWndBorder.getClient().drawLine (cToPos);
		m_cDisplay.endUpdate ();			
	}
	public void setFgColor (PixelColor cColor)
	{
		m_cWndBorder.getClient().setFgColor (cColor);
	}
	public void setBgColor (PixelColor cColor)
	{
		m_cWndBorder.getClient().setBgColor (cColor);
	}
	public void setEraseColor (PixelColor cColor)
	{
		m_cWndBorder.getClient().setEraseColor (cColor);
	}
	public void setFgColor (int r, int g, int b, int a)
	{
		m_cWndBorder.getClient().setFgColor (r, g, b, a);
	}
	public void setBgColor (int r, int g, int b, int a)
	{
		m_cWndBorder.getClient().setBgColor (r, g, b, a);
	}
	public void setEraseColor (int r, int g, int b, int a)
	{
		m_cWndBorder.getClient().setEraseColor (r, g, b, a);
	}
	public void setFgColor (int r, int g, int b)
	{
		m_cWndBorder.getClient().setFgColor (r, g, b);
	}
	public void setBgColor (int r, int g, int b)
	{
		m_cWndBorder.getClient().setBgColor (r, g, b);
	}
	public void setEraseColor (int r, int g, int b)
	{
		m_cWndBorder.getClient().setEraseColor (r, g, b);
	}
	public void fillRect (PixelRect cRect)
	{
		m_cDisplay.startUpdate();
		m_cWndBorder.getClient().fillRect (cRect);
		m_cDisplay.endUpdate();
	}
	public void fillRect (int x, int y, int w, int h)
	{
		m_cDisplay.startUpdate();
		m_cTmpRect.setTo (x, y, x + w - 1, y + h - 1);
		m_cWndBorder.getClient().fillRect (m_cTmpRect);
		m_cDisplay.endUpdate();
	}
	public void fillRect (PixelRect cRect, DrawingMode nMode)
	{
		m_cDisplay.startUpdate();
		m_cWndBorder.getClient().fillRect (cRect, nMode);
		m_cDisplay.endUpdate();
	}
	public void drawBitmap (WBitmap cBitmap, int x, int y)
	{
		m_cDisplay.startUpdate ();
		m_cWndBorder.getClient().drawBitmap (cBitmap, x, y);
		m_cDisplay.endUpdate ();
	}
	public void drawBitmap (WBitmap cBitmap, PixelPoint cPos, DrawingMode nMode)
	{
		m_cDisplay.startUpdate ();
		m_cWndBorder.getClient().drawBitmap (cBitmap, cPos, nMode);
		m_cDisplay.endUpdate ();
	}
	public void drawBitmap (WBitmap cBitmap, PixelRect cSrcRect, PixelRect cDstRect, DrawingMode nDrawingMode)
	{
		m_cDisplay.startUpdate ();
		m_cWndBorder.getClient().drawBitmap (cBitmap, cSrcRect, cDstRect, nDrawingMode);
		m_cDisplay.endUpdate ();
	}
	public void drawString (String cString)
	{
		m_cDisplay.startUpdate ();
		m_cWndBorder.getClient().drawString (cString, cString.length());
		m_cDisplay.endUpdate ();
	}
	public void drawString (String cString, int nLength)
	{
		m_cDisplay.startUpdate ();
		m_cWndBorder.getClient().drawString (cString, nLength);
		m_cDisplay.endUpdate ();
	}
	public void setBitmap (WBitmap pcBitmap)
	{
		if (pcBitmap == null)
			m_cWndBorder.getClient().setBitmap (m_cWndBorder.getBitmap());
		else
			m_cWndBorder.getClient().setBitmap (pcBitmap);
	}
	public void setClip (int x, int y, int w, int h)
	{
		m_cWndBorder.getClient().setClip (x, y, w, h);
	}
	public static void KeyUp (Keycode eKeyCode, Keycode eRawCode, Qualifiers eQualifiers)
	{
		if (s_cActiveWindow != null)
			s_cActiveWindow.postMessage (new jx.wm.message.WKeyUpMessage (eKeyCode, eRawCode, eQualifiers));
	}
	public static void KeyDown (Keycode eKeyCode, Keycode eRawCode, Qualifiers eQualifiers)
	{
		if (s_cActiveWindow != null)
			s_cActiveWindow.postMessage (new jx.wm.message.WKeyDownMessage (eKeyCode, eRawCode, eQualifiers));
	}
	public static void handleKeyUp (Keycode eKeyCode, Keycode eRawCode, Qualifiers eQualifiers)
	{
		if (s_cActiveWindow != null)
			s_cActiveWindow.postMessage (new jx.wm.message.WKeyUpMessage (eKeyCode, eRawCode, eQualifiers));
	}
	public static void handleKeyDown (Keycode eKeyCode, Keycode eRawCode, Qualifiers eQualifiers)
	{
		if (s_cActiveWindow != null)
			s_cActiveWindow.postMessage (new jx.wm.message.WKeyDownMessage (eKeyCode, eRawCode, eQualifiers));
	}
    public static void KeyUp (WKeyUpMessage cMsg)
    {
	if (s_cActiveWindow != null)
	    s_cActiveWindow.postMessage (cMsg);
    }
    public static void KeyDown (WKeyDownMessage cMsg)
    {
	if (s_cActiveWindow != null)
	    s_cActiveWindow.postMessage (cMsg);
    }
	public static void handleMouseDown (int nButton)
	{	
		PixelPoint cMousePos = s_cDisplay.getMousePos ();
		WWindowImpl cActiveWindow = getActiveWindow (false);
		WWindowImpl cMouseWnd = null;
	
		if (cActiveWindow == null || (cActiveWindow.m_nFlags.getValue() & (WindowFlags.WND_SYSTEM|WindowFlags.WND_NO_FOCUS)) == 0)
		{
			cMouseWnd = s_cLastMouseWindow;
		}
		if (cMouseWnd != null)
		{
			cMouseWnd.makeFocus (true);
			//cMouseWnd.moveToFront ();
		}
		else if (getActiveWindow (true) != null)
		{
			getActiveWindow (true).makeFocus (false);
		}
		cActiveWindow = getActiveWindow (false);
		if (cActiveWindow != null && cMouseWnd != cActiveWindow)
			cActiveWindow.mouseDown (cMousePos, nButton);			
		if (cMouseWnd != null)
		{
			cMouseWnd.mouseDown (cMousePos, nButton);
		}
	}
	public void mouseDown (PixelPoint cMousePos, int nButton)
	{
		PixelPoint cPos = new PixelPoint (cMousePos);
		cPos.sub (m_cWndBorder.getLeftTop());
//	Debug.out.println ("WWindowImpl::mouseDown (" + m_cTitle + ", " + cMousePos + ", " + cPos + ", " + nButton + ")" + " top: " + m_cTopView.m_cFrame);
		if (m_cTopView.m_cFrame.contains(cPos) == false && 
		    (m_nFlags.getValue() & (WindowFlags.WND_SYSTEM|WindowFlags.WND_NO_FOCUS)) == 0)
		{
//		Debug.out.println ("WWindowImpl::mouseDown (" + m_cTitle + ", " + cMousePos + ", " + cPos + ", " + nButton + ")" + " top: " + m_cTopView.m_cFrame);
			m_bBorderHit = true;
			if (m_cWndBorder.mouseDown (cPos, nButton) == true)
			{
				//Debug.out.println ("Setting dragWindow: " + m_cTitle);
				s_cDragWindow = this;
				cPos.sub (m_cTopView.getLeftTop()); // relative Koordinaten zum Client-Bereich
				postMessage (new WMouseDownMessage (cMousePos, cPos, nButton));	
				return;
			}				
		}	
		cPos.sub (m_cTopView.getLeftTop()); // relative Koordinaten zum Client-Bereich
	//Debug.out.println ("WWindowImpl::mouseDown (" + m_cTitle + ", " + cMousePos + ", " + cPos + ", " + nButton + ")" + " top: " + m_cTopView.m_cFrame);
		postMessage (new WMouseDownMessage (cMousePos, cPos, nButton));	
	}
	public static void handleMouseUp (int nButton)
	{
		PixelPoint cMousePos = s_cDisplay.getMousePos ();
		//Debug.out.println ("WWindowImpl::handleMouseUp(" + nButton + ")");
		WWindowImpl cActiveWindow = getActiveWindow (false);
		WWindowImpl cMouseWnd = null;
		
		if (s_cDragWindow != null)
		{
			//Debug.out.println ("WWindowImpl::handleMouseUp(" + nButton + ") --> s_cDragWindow found");
			PixelPoint cPos = new PixelPoint (cMousePos);
			cPos.sub (s_cDragWindow.m_cWndBorder.getLeftTop());
			if (s_cDragWindow.m_cWndBorder.mouseUp (cPos, nButton) == true)
			{
				cPos.sub (s_cDragWindow.m_cTopView.getLeftTop()); // relative Koordinaten zum Client-Bereich
				s_cDragWindow.postMessage (new WMouseUpMessage (cMousePos, cPos, nButton));
				//Debug.out.println ("WWindowImpl::handleMouseUp(" + nButton + ") --> setting s_cDragWindow to null");
				s_cDragWindow = null;
			}
			else
			{
				cPos.sub (s_cDragWindow.m_cTopView.getLeftTop()); // relative Koordinaten zum Client-Bereich
				s_cDragWindow.postMessage (new WMouseUpMessage (cMousePos, cPos, nButton));
			}
			return;
		}		
		if (cActiveWindow == null || (cActiveWindow.m_nFlags.getValue() & (WindowFlags.WND_SYSTEM|WindowFlags.WND_NO_FOCUS)) == 0)
		{
			cMouseWnd = s_cLastMouseWindow;
		}
		if (cMouseWnd != null)
		{
			cMouseWnd.mouseUp (cMousePos, nButton);
		}
		if (cActiveWindow != null && cMouseWnd != cActiveWindow)
		{
			cActiveWindow.mouseUp (cMousePos, nButton);
		}
	}
	void mouseUp (PixelPoint cMousePos, int nButton)
	{
		PixelPoint cPos = new PixelPoint (cMousePos);
		cPos.sub (m_cWndBorder.getLeftTop());
		cPos.sub (m_cTopView.getLeftTop()); // relative Koordinaten zum Client-Bereich
		//Debug.out.println ("mouseUp ("+m_cTitle+")");
		postMessage (new WMouseUpMessage (cMousePos, cPos, nButton));
	}
    public static void handleMousePosition (int nPosX, int nPosY)
    {
		PixelPoint cMousePos = s_cDisplay.getMousePos ();
		int nDeltaX = nPosX - cMousePos.m_nX;
		int nDeltaY = nPosY - cMousePos.m_nY;
		handleMouseMoved (nDeltaX, nDeltaY);	
    }
	public static void handleMouseMoved (int nDeltaX, int nDeltaY)
	{
		s_cDisplay.moveMouseBy (nDeltaX, nDeltaY);
		PixelPoint cMousePos = s_cDisplay.getMousePos ();
		WWindowImpl cActiveWindow = getActiveWindow (false);
		WWindowImpl cMouseWnd = null;
		
		//Debug.out.println ("WWindowImpl::HandleMouseMoved(" + nDeltaX + ", " + nDeltaY + ", " + cMousePos + ")");

		if (s_cDragWindow != null)
		{
			PixelPoint cPos = new PixelPoint (cMousePos);
			cPos.sub (s_cDragWindow.m_cWndBorder.getLeftTop());
			s_cDragWindow.m_cWndBorder.mouseMoved (cPos, MOUSE_INSIDE);
			return;
		}

		if (cActiveWindow == null || (cActiveWindow.m_nFlags.getValue() & (WindowFlags.WND_SYSTEM|WindowFlags.WND_NO_FOCUS)) == 0)
		{
			WView cView = WView.getTopView().getChildAt (cMousePos);

			if (cView != null)
				cMouseWnd = cView.getWindow ();
			else
				cMouseWnd = null;
		}
		if (s_cLastMouseWindow != cMouseWnd)
		{
			if (s_cLastMouseWindow != null)
			{
				// Give the window a chance to deliver a MOUSE_EXITED to it's views
				s_cLastMouseWindow.mouseMoved (cMousePos, MOUSE_EXITED);
				if (s_cLastMouseWindow == cActiveWindow)
					cActiveWindow = null;
			}
			if (cMouseWnd != null)
			{
				cMouseWnd.mouseMoved (cMousePos, MOUSE_ENTERED);
				if (cMouseWnd == cActiveWindow)
					cActiveWindow = null;
			}
			s_cLastMouseWindow = cMouseWnd;
		}
		else
		{
			if (cMouseWnd != null)
			{
				cMouseWnd.mouseMoved (cMousePos, MOUSE_INSIDE);
				if (cMouseWnd == cActiveWindow)
					cActiveWindow = null;
			}
		}
		if (cActiveWindow != null)
			cActiveWindow.mouseMoved (cMousePos, MOUSE_OUTSIDE);
		if (cMouseWnd != null && cMouseWnd != cActiveWindow)
		{	
			PixelPoint cPos = new PixelPoint (cMousePos);
			cPos.sub (cMouseWnd.m_cWndBorder.getLeftTop());
			cMouseWnd.m_cWndBorder.mouseMoved (cPos, MOUSE_INSIDE);
		}
	}
	void mouseMoved (PixelPoint cMousePos, int nTransit)
	{
		PixelPoint cPos = new PixelPoint (cMousePos);
		cPos.sub (m_cWndBorder.getLeftTop());
		/*TODO: m_bBorderHit */
		if ((m_nFlags.getValue() & (WindowFlags.WND_SYSTEM|WindowFlags.WND_NO_FOCUS)) == 0)
		{
			m_cWndBorder.mouseMoved (cPos, nTransit);
		}
		cPos.sub (m_cTopView.getLeftTop()); // relative Koordinaten zum Client-Bereich
		//Debug.out.println ("mouseMoved ("+m_cTitle+")");
		WMessage cMsg = lastMessage ();
		if (cMsg != null && cMsg.getCode() == WMessage.MOUSE_MOVED) { 
			WMouseMovedMessage cMoveMsg = (WMouseMovedMessage)cMsg;
			if (nTransit == cMoveMsg.m_nTransit)
			{
				cMoveMsg.m_cScreenPos.setTo (cMousePos);
				cMoveMsg.m_cPos.setTo (cPos);
				return;
			}
		}
		postMessage (new WMouseMovedMessage (cMousePos, cPos, nTransit));
	}
	public void makeFocus (boolean bFlag)
	{
		if ((m_nFlags.getValue() & WindowFlags.WND_NO_FOCUS) == 0)
		{
			if (bFlag)
				setActiveWindow (this);
			else
				setActiveWindow (null);
		}
		else
		{
		}
	}
	public void startUpdate()
	{
		WSprite.hide ();
	}
	public void endUpdate ()
	{
		WSprite.unhide ();
	}
	public WindowFlags getFlags()
	{
		return m_nFlags;
	}
	public static WWindowImpl getActiveWindow (boolean bIgnoreSystemWindows)
	{
		if (bIgnoreSystemWindows && s_cActiveWindow != null && 
			(s_cActiveWindow.m_nFlags.getValue() & (WindowFlags.WND_SYSTEM|WindowFlags.WND_NO_FOCUS)) != 0)
			return null;
		return s_cActiveWindow;
	}
	public static void setActiveWindow (WWindowImpl cWnd)
	{
		WWindowImpl cPrev;
		
//		if (cWnd != null)
//			Debug.out.println ("setActiveWindow(" + cWnd + ")");
//		else
//			Debug.out.println ("setActiveWindow(<null>)");
		
		// @ Marco Winter
		// a window should always get a notification
		// whenever its status is updated
		//if ((cPrev = s_cActiveWindow) == cWnd)
		//return;
		//if (cPrev != null)
		cPrev = s_cActiveWindow;
		if (cPrev != null && cPrev != cWnd)
			s_cActiveWindow.windowActivated (false);
		if ((s_cActiveWindow = cWnd) != null)		
			s_cActiveWindow.windowActivated (true);
	}
	void windowActivated (boolean bState)
	{
		postMessage (new WWindowActivatedMessage (bState));
		if (m_cWndBorder != null)
		{
			s_cDisplay.startUpdate ();
			WSprite.hide();
			m_cDecorator.setFocusState (bState);
			WSprite.unhide ();
			s_cDisplay.endUpdate ();
		}			
	}
	public void moveReply ()
	{
		if (m_cWndBorder != null)
			m_cWndBorder.wndMoveReply ();
	}
	public void close ()
	{
		//show (false);
		postMessage (new jx.wm.message.WRequestCloseMessage());
	}
	public PixelRect getScreenResolution ()
	{
		return m_cDisplay.getScreenResolution();
	}
	public void quit ()
	{
		//Debug.out.println (m_cTitle + ": WWindowImpl::quit()");
		show (false, false);
		postMessage (new jx.wm.message.WQuitMessage());
	}
	public void startRepaint ()
	{
		m_cWndBorder.getClient().beginUpdate ();
	}
	public void endRepaint ()
	{
		m_cWndBorder.getClient().endUpdate ();
	}

   /*public void enableBackBuffer() {
<       backBufferCounter++;
<       if (backBufferCounter != 1)
<           return;
<       PixelRect r = getFrame();
<       //System.out.println("Enabling backBuffer");
<       if (backBuffer == null ||
<           r.width() != backBuffer.width() - 1 ||
<           r.height() != backBuffer.height() - 1) {
<           if (backBuffer != null) {
<               System.out.println("old buffer was " + backBuffer.width() + "," + backBuffer.height());
<               System.out.println("new buffer is " + r.width() + "," + r.height());
<           } else
<               System.out.println("backBuffer is null");
<           int x = (r.width() > 0) ? r.width() : 0;
<           int y = (r.height() > 0) ? r.height() : 0;
<           backBuffer = new WBitmap(x + 1, y + 1, new ColorSpace(ColorSpace.CS_RGB16));
<           //backBuffer = new WBitmap(1024, 768, new ColorSpace(ColorSpace.CS_RGB16));
<       }
<       setBitmap(backBuffer);
---
>     public void setBufferCount() {
>       bufferCount++;
123,133c102,105
<       backBufferCounter--;
<       if (backBufferCounter != 0)
<           return;
<       //System.out.println("Drawing backBuffer");
<       PixelRect r = getFrame();
<       setClip(0, 0, r.width(), r.height());
< 
<       setBitmap(null);
<       drawBitmap(backBuffer, 0, 0);
<       }*/


	public void enableBackBuffer(boolean enable) {
		if (m_bBackBuffer) {
			PixelRect r = getFrame(false);

			if (m_cBackBuffer==null) {
				if (enable) {
					WBitmap fb = m_cWndBorder.getBitmap();
					m_cBackBuffer = fb.getCloneMap();
				} else {
					return;
				}
			}
			if (enable) {
				m_cWndBorder.getClient().setBitmap(m_cBackBuffer);
			} else {
				m_cWndBorder.getClient().setBitmap(m_cWndBorder.getBitmap());
			}
		}
	}

	public void resetBackBuffer() {
		if (m_bBackBuffer) {	
			//PixelRect r = getFrame(false);
			if (m_cBackBuffer!=null && m_cBackBuffer.isCloneMap(m_cWndBorder.getBitmap())) {
				return;
			} else {
				Debug.out.println("WWindowImpl::resetBackBuffer - do it");
				WBitmap fb = m_cWndBorder.getBitmap();
				m_cBackBuffer = fb.getCloneMap();
			}
		}
	}

	public void drawBackBuffer() {
		if (m_cBackBuffer!=null) {
			WView client = m_cWndBorder.getClient();

			PixelRect r = getFrame(false);
			setClip(0, 0, r.width()+1, r.height()+1);	

			setBitmap(m_cWndBorder.getBitmap());


			m_cDisplay.startUpdate ();
			m_cWndBorder.getClient().drawCloneMap (m_cBackBuffer, 0, 0);
			m_cWndBorder.getClient().drawBitmap (m_cBackBuffer, 0, 0);
			m_cDisplay.endUpdate ();

			setBitmap(m_cBackBuffer);
		}
	}


    private void serviceFinalizer() {
	Debug.out.println("*****  WWindowImpl: THIS SERVICE TERMINATES NOW *** WindowTitle: "+m_cTitle);
	quit();
    }
}
