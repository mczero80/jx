package jx.wm;

import jx.devices.fb.*;
import jx.zero.*;
import jx.wm.WView;
import jx.wm.WWindowImpl;
import jx.wm.WDisplay;
import jx.wm.decorator.WindowDecorator;
import jx.wm.message.WWindowFrameChangedMessage;

class WindowBorder extends WView
{
	static WDisplay		s_cDisplay = null;
	static boolean		s_bUseWireFrames = false;
	WindowDecorator 	m_cDecorator;
  	WView   		m_cClient;
	WWindowImpl		m_cOwner;
	
	int 			m_nCloseDown;
	int 			m_nZoomDown;
	int 			m_nToggleDown;
    int                     m_nPendingMoves = 0;
	int				m_nMouseDown = 0;
	boolean			m_bWndMovePending;
	boolean			m_bBackdrop;
	boolean			m_bWireFramePending = false;
	boolean			m_bZoomed = false;

  	PixelPoint    		m_cDeltaMove;
  	PixelPoint    		m_cDeltaSize;
	PixelPoint			m_cHitOffset;
	PixelPoint			m_cMinSize;
	PixelPoint			m_cMaxSize;
  	PixelPoint			m_cAlignSize;
  	PixelPoint			m_cAlignSizeOff;
  	PixelPoint			m_cAlignPos;
  	PixelPoint			m_cAlignPosOff;
	PixelRect			m_cRawFrame;
	PixelRect			m_cOldFrame = new PixelRect();
	PixelRect			m_cUnzoomedFrame = new PixelRect();
	
	int      		m_eHitItem;
	int       		m_eCursorHitItem;
	
	//public WindowBorder (WWindowImpl cOwner, WView cParent, String cName, boolean bBackdrop, boolean bTransparent)
	public WindowBorder (WWindowImpl cOwner, WView cParent, String cName, WindowFlags eFlags)
	{
		super (cOwner, cParent, cName, new PixelRect (0, 0, 0, 0), 0);
		
		boolean bBackdrop = (eFlags.getValue() & WindowFlags.WND_BACKMOST) != 0;
		boolean bTransparent = (eFlags.getValue() & WindowFlags.WND_TRANSPARENT) != 0;

		m_cOwner		= cOwner;
		m_eHitItem 		= WindowDecorator.HIT_NONE;
		m_eCursorHitItem 	= WindowDecorator.HIT_NONE;
		m_cDeltaMove		= new PixelPoint (0, 0);
		m_cDeltaSize		= new PixelPoint (0, 0);
		m_cHitOffset		= new PixelPoint (0, 0);

		m_cMinSize 		= new PixelPoint (1, 1);
		m_cMaxSize 		= new PixelPoint (9999999, 9999999);
		m_cAlignSize 		= new PixelPoint (1, 1);
		m_cAlignSizeOff 	= new PixelPoint (0, 0);
		m_cAlignPos 		= new PixelPoint (1, 1);
		m_cAlignPosOff 		= new PixelPoint (0, 0);
		m_cRawFrame		= new PixelRect ();

		m_nToggleDown    = 0;
		m_nZoomDown      = 0;
		m_nCloseDown     = 0;

		m_bWndMovePending = false;

		m_bBackdrop = bBackdrop;

		/*TODOSetFont (AppServer::GetInstance ()->GetWindowTitleFont ());*/

		m_cDecorator = null;
		
		int nViewFlags = 0;
		if (!bTransparent)
			nViewFlags |= WID_CLEAR_BACKGROUND;
		if ((eFlags.getValue() & WindowFlags.WND_FULL_UPDATE_ON_H_RESIZE) != 0)
			nViewFlags |= WID_FULL_UPDATE_ON_H_RESIZE;
		if ((eFlags.getValue() & WindowFlags.WND_FULL_UPDATE_ON_V_RESIZE) != 0)
			nViewFlags |= WID_FULL_UPDATE_ON_V_RESIZE;

		m_cClient = new WView (cOwner, this, cName + "_client", new PixelRect (0, 0, 0, 0), nViewFlags);
	}
	public void finalize ()
	{
		m_cOwner = null;
		m_cDeltaMove = null;
		m_cDeltaSize = null;
		m_cHitOffset = null;
		m_cMinSize = null;
		m_cMaxSize = null;
		m_cAlignSize = null;
		m_cAlignSizeOff = null;
		m_cAlignPos = null;
		m_cAlignPosOff = null;
		m_cRawFrame = null;
		m_cDecorator = null;
		removeChild (m_cClient);
		m_cClient = null;
	}
	public static void setDisplay (WDisplay cDisplay)
	{
		s_cDisplay = cDisplay;
	}
	public void setDecorator (WindowDecorator cDecorator)
	{
		m_cDecorator = cDecorator;
	}
	private PixelRect rectToClient (PixelRect cRect)
	{
		PixelRect cBorders = m_cDecorator.getBorderSize ();

		return (new PixelRect (cRect.m_nX0 + cBorders.m_nX0,
							 	 cRect.m_nY0 + cBorders.m_nY0,
								 cRect.m_nX1 - cBorders.m_nX1,
								 cRect.m_nY1 - cBorders.m_nY1));
	}
	private void doSetFrame (PixelRect cRect)
	{
		super.setFrame (cRect);
		PixelRect cBounds = getBounds ();
		m_cClient.setFrame (rectToClient (cBounds));
		m_cDecorator.frameSized (cBounds);
	}
	public void setFrame (PixelRect cRect)
	{
		/*Debug.out.println ("WindowBorder::setFrame() " + cRect);*/
		m_cRawFrame.setTo (cRect);
		doSetFrame (cRect);
	}
	protected void paint (WRegion cRegion, boolean bUpdate)
	{
		PixelRect cUpdateRect = cRegion.getBounds ();
		/*Debug.out.println ("WindowBorder::paint() " + cUpdateRect);*/
		if (bUpdate)
			beginUpdate ();
		m_cDecorator.render (cUpdateRect);
		if (bUpdate)
			endUpdate ();
	}
	public WView getClient ()
	{
		return m_cClient;
	}
	void getHitPointBase (PixelPoint cHitPos)
	{
		switch (m_eHitItem)
		{
			case WindowDecorator.HIT_SIZE_LEFT:
				cHitPos.setTo (0, 0);
				break;
			case WindowDecorator.HIT_SIZE_RIGHT:
				cHitPos.setTo (m_cRawFrame.width() + m_cDeltaSize.m_nX, 0);
				break;
			case WindowDecorator.HIT_SIZE_TOP:
				cHitPos.setTo (0, 0);
				break;
			case WindowDecorator.HIT_SIZE_BOTTOM:
				cHitPos.setTo (0, m_cRawFrame.height() + m_cDeltaSize.m_nY);
				break;
			case WindowDecorator.HIT_SIZE_LT:
				cHitPos.setTo (0, 0);
				break;
			case WindowDecorator.HIT_SIZE_RT:
				cHitPos.setTo (m_cRawFrame.width() + m_cDeltaSize.m_nX, 0);
				break;
			case WindowDecorator.HIT_SIZE_RB:
				cHitPos.setTo (m_cRawFrame.width() + m_cDeltaSize.m_nX,
					       m_cRawFrame.height() + m_cDeltaSize.m_nY);
				break;
			case WindowDecorator.HIT_SIZE_LB:
				cHitPos.setTo (0, m_cRawFrame.height() + m_cDeltaSize.m_nY);
				break;
			default:
				cHitPos.setTo (0, 0);
				break;
		}
	}
	boolean mouseDown (PixelPoint cPos, int nButton)
	{
		PixelPoint cHitPos = new PixelPoint();
		
		//Debug.out.println ("WindowBorder.mouseDown(" + cPos + ", " + m_nMouseDown + ")");
		
		//if (m_nMouseDown++ != 0)
		//	return (m_eHitItem != WindowDecorator.HIT_NONE);		
		if (m_eHitItem != WindowDecorator.HIT_NONE)
		{
			m_nMouseDown++;
			return true;
		}

		m_cDeltaMove.setTo (0, 0);
		m_cDeltaSize.setTo (0, 0);
		m_cRawFrame.setTo (m_cFrame);
		m_cOldFrame.setTo (m_cFrame);

		if (getBounds().intersects (cPos) && m_cClient.m_cFrame.intersects (cPos) == false)
		{
			m_eHitItem = m_cDecorator.hitTest (cPos);
			//Debug.out.println ("hitTest --> " + m_eHitItem);
		}
		else
		{
			m_eHitItem = WindowDecorator.HIT_NONE;
		}

		getHitPointBase (cHitPos);

		/*m_cHitOffset = cPos + GetLeftTop () - m_cRawFrame.LeftTop () - cHitPos;*/
		m_cHitOffset.setTo (cPos);
		m_cHitOffset.add (getLeftTop());
		m_cHitOffset.sub (m_cRawFrame.m_nX0, m_cRawFrame.m_nY0);
		m_cHitOffset.sub (cHitPos);
/*
		Debug.out.println ("WindowBorder::mouseDown(" + cPos + ", " + getLeftTop() + 
			", " + m_cRawFrame + ", " + cHitPos + ") -> " + m_cHitOffset);
*/
		if (m_eHitItem == WindowDecorator.HIT_CLOSE)
		{
			m_nCloseDown = 1;
			m_cDecorator.setCloseButtonState (true);
		}
		else if (m_eHitItem == WindowDecorator.HIT_ZOOM)
		{
			m_nZoomDown = 1;
			m_cDecorator.setZoomButtonState (true);
		}
		else if (m_eHitItem == WindowDecorator.HIT_DEPTH)
		{
			m_nToggleDown = 1;
			m_cDecorator.setDepthButtonState (true);
		}
		else if (m_eHitItem == WindowDecorator.HIT_DRAG)
		{
			m_cOwner.moveToFront();
			/*SrvApplication::SetCursor (WPoint (0, 0), 0, 0, CS_NO_COLOR_SPACE, 0, NULL);*/
		}
		if (m_eHitItem != WindowDecorator.HIT_NONE)
			m_nMouseDown++;
		return (m_eHitItem != WindowDecorator.HIT_NONE);
	}
	boolean mouseUp (PixelPoint cPos, int nButton)
	{
		if (--m_nMouseDown != 0)
			return false;
		if (m_nCloseDown == 1)
		{	
			m_cOwner.close ();
			/* TODO
			if (pcAppTarget != NULL)
			{
				WMessage cMsg (M_QUIT);

				if (pcAppTarget->SendMessage (&cMsg) < 0)
				{
					dbprintf
						("Error: WindowBorder::MouseUp() failed to send M_QUIT to window\n");
				}
			}
			*/
		}
		else if (m_nToggleDown == 1)
		{
			toggleDepth ();
			if (m_cParent != null)
			{
				s_cDisplay.startUpdate ();
				WSprite.hide ();
				m_cParent.updateRegions (false);
				WSprite.unhide ();
				s_cDisplay.endUpdate ();
			}
			/*TODO WWindowImpl.handleMouseTransaction ();*/
		}
		else if (m_nZoomDown == 1)
		{
		    int nFlags = m_cOwner.getFlags ().getValue();		    
		    if (((nFlags & WindowFlags.WND_NOT_RESIZABLE) == 0))
			{
			    if (m_bZoomed)
				{
				    m_bZoomed = false;
				    m_cOwner.setFrame (m_cUnzoomedFrame, true);
				}
			    else
				{
				    m_bZoomed = true;
				    m_cUnzoomedFrame = m_cOwner.getFrame (true);
				    m_cOwner.setFrame (m_cOwner.getScreenResolution(), true);
				}
			    s_cDisplay.startUpdate ();
			    WSprite.hide ();
			    m_cParent.updateRegions (false);
			    WSprite.unhide ();
			    s_cDisplay.endUpdate ();
			    PixelRect cBorders = m_cDecorator.getBorderSize();
			    PixelRect cAlignedFrame = alignRect (m_cRawFrame, cBorders);
			    m_cOwner.postMessage (new WWindowFrameChangedMessage (rectToClient (cAlignedFrame)));
			}
		}

		m_eHitItem = WindowDecorator.HIT_NONE;

		if (m_nToggleDown == 1)
		{
			m_cDecorator.setDepthButtonState (false);
		}
		if (m_nZoomDown == 1)
		{
			m_cDecorator.setZoomButtonState (false);
		}
		if (m_nCloseDown == 1)
		{
			m_cDecorator.setCloseButtonState (false);
		}

		m_nToggleDown = 0;
		m_nZoomDown = 0;
		m_nCloseDown = 0;
		m_cRawFrame.setTo (m_cFrame);
		
		if (s_bUseWireFrames && m_bWireFramePending && m_nMouseDown == 0)
		{
			m_cOwner.postMessage (new WWindowFrameChangedMessage (rectToClient (m_cOldFrame)));
		}	
		/*TODO?SrvApplication::RestoreCursor ();*/
		return true;
	}
	boolean mouseMoved (PixelPoint cNewPos, int nTransit)
	{
		PixelPoint cHitPos = new PixelPoint ();

		if (m_eHitItem == WindowDecorator.HIT_NONE && nTransit == WWindowImpl.MOUSE_EXITED)
		{
			m_eCursorHitItem = WindowDecorator.HIT_NONE;
			/*SrvApplication::RestoreCursor ();*/
			return (false);
		}

		// Caclculate the window borders
		PixelRect cBorders /*= new PixelRect (0, 0, 0, 0)*/;
		int nBorderWidth = 0;
		int nBorderHeight = 0;

		if (m_cDecorator != null)
		{
			cBorders      = m_cDecorator.getBorderSize ();
			nBorderWidth  = (int)(cBorders.m_nX0 + cBorders.m_nX1);
			nBorderHeight = (int)(cBorders.m_nY0 + cBorders.m_nY1);
		}
		else
		{
			return false;
		}

		// Figure out which edges the cursor is relative to
		getHitPointBase (cHitPos);

		// Calculate the delta movement relative to the hit edge/corner
		PixelPoint cDelta = new PixelPoint (cNewPos);
/*		
		PixelPoint cDelta ((cNewPos) - (cHitPos + m_cHitOffset) - m_cDeltaMove +
								 GetLeftTop () - m_cRawFrame.LeftTop ());
*/	
		cDelta.sub (cHitPos);
		cDelta.sub (m_cHitOffset);
		cDelta.sub (m_cDeltaMove);
		cDelta.add (getLeftTop ());
		cDelta.sub (m_cRawFrame.m_nX0, m_cRawFrame.m_nY0);								 
/*		
		Debug.out.println ("WindowBorder::mouseMoved(" + cNewPos + ", " + cHitPos + ", " + m_cHitOffset +
			", " + m_cDeltaMove + ", " + getLeftTop() + ", " + m_cRawFrame + ") -> " + cDelta);
*/
		int eHitItem;
		if (getBounds ().intersects (cNewPos) && m_cClient.m_cFrame.intersects (cNewPos) == false)
		{
			eHitItem = m_cDecorator.hitTest (cNewPos);
		}
		else
		{
			eHitItem = WindowDecorator.HIT_NONE;
		}

		// If we didnt hit anything interesting with the last mouse-click we are done by now.
		if (m_eHitItem == WindowDecorator.HIT_NONE)
			return (false);

		// Set the state of the various border buttons.
		if (m_nToggleDown > 0)
		{
			int nNewMode = (eHitItem == WindowDecorator.HIT_DEPTH) ? 1 : 2;

			if (nNewMode != m_nToggleDown)
			{
				m_nToggleDown = nNewMode;
				m_cDecorator.setDepthButtonState (m_nToggleDown == 1);
			}
		}
		else if (m_nZoomDown > 0)
		{
			int nNewMode = (eHitItem == WindowDecorator.HIT_ZOOM) ? 1 : 2;
	
			if (nNewMode != m_nZoomDown)
			{
				m_nZoomDown = nNewMode;
				m_cDecorator.setZoomButtonState (m_nZoomDown == 1);
			}
		}
		else if (m_nCloseDown > 0)
		{
			int nNewMode = (eHitItem == WindowDecorator.HIT_CLOSE) ? 1 : 2;
	
			if (nNewMode != m_nCloseDown)
			{
				m_nCloseDown = nNewMode;
				m_cDecorator.setCloseButtonState (m_nCloseDown == 1);
			}
		}
		else if (m_eHitItem == WindowDecorator.HIT_DRAG)
		{
			cDelta.m_nX = align (cDelta.m_nX, m_cAlignPos.m_nX);
			cDelta.m_nY = align (cDelta.m_nY, m_cAlignPos.m_nY);
			m_cDeltaMove.add (cDelta);
		}
	
		int nFlags = m_cOwner.getFlags ().getValue();

		if ((nFlags & WindowFlags.WND_NOT_RESIZABLE) != WindowFlags.WND_NOT_RESIZABLE)
		{
			if ((nFlags & WindowFlags.WND_NOT_H_RESIZABLE) != 0)
			{
				cDelta.m_nX = 0;
			}
			if ((nFlags & WindowFlags.WND_NOT_V_RESIZABLE) != 0)
			{
				cDelta.m_nY = 0;
			}
			PixelPoint cBorderMinSize  = m_cDecorator.getMinimumSize ();
			PixelPoint cMinSize = new PixelPoint (m_cMinSize);

			if (cMinSize.m_nX < cBorderMinSize.m_nX - nBorderWidth)
			{
				cMinSize.m_nX = cBorderMinSize.m_nX - nBorderWidth;
			}
			if (cMinSize.m_nY < cBorderMinSize.m_nY - nBorderHeight)
			{
				cMinSize.m_nY = cBorderMinSize.m_nY - nBorderHeight;
			}
	
			switch (m_eHitItem)
			{
			case WindowDecorator.HIT_SIZE_LEFT:
				cDelta.m_nX =
					-clamp (m_cRawFrame.width () + m_cDeltaSize.m_nX - nBorderWidth + 1,
									-cDelta.m_nX, cMinSize.m_nX, m_cMaxSize.m_nX);
				cDelta.m_nX = align (align (cDelta.m_nX, m_cAlignSize.m_nX), m_cAlignPos.m_nX);
				m_cDeltaMove.m_nX += cDelta.m_nX;
				m_cDeltaSize.m_nX -= cDelta.m_nX;
				break;
			case WindowDecorator.HIT_SIZE_RIGHT:
				cDelta.m_nX =
					clamp (m_cRawFrame.width () + m_cDeltaSize.m_nX - nBorderWidth + 1,
								 cDelta.m_nX, cMinSize.m_nX, m_cMaxSize.m_nX);
				cDelta.m_nX = align (cDelta.m_nX, m_cAlignSize.m_nX);
				m_cDeltaSize.m_nX += cDelta.m_nX;
				break;
			case WindowDecorator.HIT_SIZE_TOP:
				cDelta.m_nY =
					-clamp (m_cRawFrame.height () + m_cDeltaSize.m_nY - nBorderHeight + 1,
									-cDelta.m_nY, cMinSize.m_nY, m_cMaxSize.m_nY);
				cDelta.m_nY = align (align (cDelta.m_nY, m_cAlignSize.m_nY), m_cAlignPos.m_nY);
				m_cDeltaMove.m_nY += cDelta.m_nY;
				m_cDeltaSize.m_nY -= cDelta.m_nY;
				break;
			case WindowDecorator.HIT_SIZE_BOTTOM:
				cDelta.m_nY =
					clamp (m_cRawFrame.height () + m_cDeltaSize.m_nY - nBorderHeight + 1,
								 cDelta.m_nY, cMinSize.m_nY, m_cMaxSize.m_nY);
				cDelta.m_nY = align (cDelta.m_nY, m_cAlignSize.m_nY);
				m_cDeltaSize.m_nY += cDelta.m_nY;
				break;
			case WindowDecorator.HIT_SIZE_LT:
				cDelta.m_nX =
					-clamp (m_cRawFrame.width () + m_cDeltaSize.m_nX - nBorderWidth + 1,
									-cDelta.m_nX, cMinSize.m_nX, m_cMaxSize.m_nX);
				cDelta.m_nY =
					-clamp (m_cRawFrame.height () + m_cDeltaSize.m_nY - nBorderHeight + 1,
									-cDelta.m_nY, cMinSize.m_nY, m_cMaxSize.m_nY);
				cDelta.m_nX = align (align (cDelta.m_nX, m_cAlignSize.m_nX), m_cAlignPos.m_nX);
				cDelta.m_nY = align (align (cDelta.m_nY, m_cAlignSize.m_nY), m_cAlignPos.m_nY);
				m_cDeltaMove.add (cDelta);
				m_cDeltaSize.sub (cDelta);
				break;
			case WindowDecorator.HIT_SIZE_RT:
				cDelta.m_nX =
					clamp (m_cRawFrame.width () + m_cDeltaSize.m_nX - nBorderWidth + 1,
								 cDelta.m_nX, cMinSize.m_nX, m_cMaxSize.m_nX);
				cDelta.m_nY =
					-clamp (m_cRawFrame.height () + m_cDeltaSize.m_nY - nBorderHeight + 1,
									-cDelta.m_nY, cMinSize.m_nY, m_cMaxSize.m_nY);
				cDelta.m_nX = align (cDelta.m_nX, m_cAlignSize.m_nX);
				cDelta.m_nY = align (align (cDelta.m_nY, m_cAlignSize.m_nY), m_cAlignPos.m_nY);
				m_cDeltaSize.m_nX += cDelta.m_nX;
				m_cDeltaSize.m_nY -= cDelta.m_nY;
				m_cDeltaMove.m_nY += cDelta.m_nY;
				break;
			case WindowDecorator.HIT_SIZE_RB:
				cDelta.m_nX =
					clamp (m_cRawFrame.width () + m_cDeltaSize.m_nX - nBorderWidth + 1,
								 cDelta.m_nX, cMinSize.m_nX, m_cMaxSize.m_nX);
				cDelta.m_nY =
					clamp (m_cRawFrame.height () + m_cDeltaSize.m_nY - nBorderHeight + 1,
								 cDelta.m_nY, cMinSize.m_nY, m_cMaxSize.m_nY);
				cDelta.m_nX = align (cDelta.m_nX, m_cAlignSize.m_nX);
				cDelta.m_nY = align (cDelta.m_nY, m_cAlignSize.m_nY);
				m_cDeltaSize.add (cDelta);
				break;
			case WindowDecorator.HIT_SIZE_LB:
				cDelta.m_nX =
					-clamp (m_cRawFrame.width () + m_cDeltaSize.m_nX - nBorderWidth + 1,
									-cDelta.m_nX, cMinSize.m_nX, m_cMaxSize.m_nX);
				cDelta.m_nY =
					clamp (m_cRawFrame.height () + m_cDeltaSize.m_nY - nBorderHeight + 1,
								 cDelta.m_nY, cMinSize.m_nY, m_cMaxSize.m_nY);
				cDelta.m_nX = align (align (cDelta.m_nX, m_cAlignSize.m_nX), m_cAlignPos.m_nX);
				cDelta.m_nY = align (cDelta.m_nY, m_cAlignSize.m_nY);
				m_cDeltaMove.m_nX += cDelta.m_nX;
				m_cDeltaSize.m_nX -= cDelta.m_nX;
				m_cDeltaSize.m_nY += cDelta.m_nY;
				break;
			default:
				break;
			}
		}
/*		
		Debug.out.println ("WindowBorder::mouseMoved(" + m_cDeltaMove + ", " + m_bWndMovePending + ") -> " + cDelta);
*/		
		//Debug.out.println ("WindowBorder::mouseMoved(" + m_cDeltaMove + ", " + m_nPendingMoves + ") -> " + cDelta);
		if (m_bWndMovePending == false &&
			(m_cDeltaSize.m_nX != 0 || m_cDeltaSize.m_nY != 0 || m_cDeltaMove.m_nX != 0
				 || m_cDeltaMove.m_nY != 0))
		{
			m_bWndMovePending = true;
			m_cRawFrame.m_nX1 += m_cDeltaSize.m_nX + m_cDeltaMove.m_nX;
			m_cRawFrame.m_nY1 += m_cDeltaSize.m_nY + m_cDeltaMove.m_nY;
			m_cRawFrame.m_nX0 += m_cDeltaMove.m_nX;
			m_cRawFrame.m_nY0 += m_cDeltaMove.m_nY;
			m_cDeltaMove.setTo (0, 0);
			m_cDeltaSize.setTo (0, 0);

			PixelRect cAlignedFrame = alignRect (m_cRawFrame, cBorders);
			
			if (s_bUseWireFrames == true)
			{
				s_cDisplay.startUpdate ();
				WSprite.hide ();
				drawWireFrame (cAlignedFrame, false);
				WSprite.unhide ();
				s_cDisplay.endUpdate ();
			}
			else
			{
				doSetFrame (cAlignedFrame);
				s_cDisplay.startUpdate ();
				WSprite.hide ();
				m_cParent.updateRegions (false);
				WSprite.unhide ();
				s_cDisplay.endUpdate ();
			}
			m_cOwner.postMessage (new WWindowFrameChangedMessage (rectToClient (cAlignedFrame)));
		}
		return (m_eHitItem != WindowDecorator.HIT_NONE);
	}
	private void drawWireFrame (PixelRect cFrame, boolean bLastMove)
	{
		WBitmap cBitmap = s_cDisplay.getScreenBitmap();
		PixelRect cClip = cBitmap.getBounds();
		PixelRect cDraw = new PixelRect();
		PixelColor cColor = new PixelColor(255,255,255);
		DrawingMode eMode = new DrawingMode (DrawingMode.DM_INVERT);
		
		if (m_bWireFramePending == true)
		{
			cDraw.setTo (m_cOldFrame.m_nX0, m_cOldFrame.m_nY0, m_cOldFrame.m_nX1, m_cOldFrame.m_nY0);
			cBitmap.drawLine (cDraw, cClip, cColor, eMode);
			cDraw.setTo (m_cOldFrame.m_nX1, m_cOldFrame.m_nY0, m_cOldFrame.m_nX1, m_cOldFrame.m_nY1);
			cBitmap.drawLine (cDraw, cClip, cColor, eMode);
			cDraw.setTo (m_cOldFrame.m_nX1, m_cOldFrame.m_nY1, m_cOldFrame.m_nX0, m_cOldFrame.m_nY1);
			cBitmap.drawLine (cDraw, cClip, cColor, eMode);
			cDraw.setTo (m_cOldFrame.m_nX0, m_cOldFrame.m_nY1, m_cOldFrame.m_nX0, m_cOldFrame.m_nY0);
			cBitmap.drawLine (cDraw, cClip, cColor, eMode);
		}
		else
		{
			m_bWireFramePending = true;
		}
		if (bLastMove == true)
		{
			m_bWireFramePending = false;
		}
		else
		{
			cDraw.setTo (cFrame.m_nX0, cFrame.m_nY0, cFrame.m_nX1, cFrame.m_nY0);
			cBitmap.drawLine (cDraw, cClip, cColor, eMode);
			cDraw.setTo (cFrame.m_nX1, cFrame.m_nY0, cFrame.m_nX1, cFrame.m_nY1);
			cBitmap.drawLine (cDraw, cClip, cColor, eMode);
			cDraw.setTo (cFrame.m_nX1, cFrame.m_nY1, cFrame.m_nX0, cFrame.m_nY1);
			cBitmap.drawLine (cDraw, cClip, cColor, eMode);
			cDraw.setTo (cFrame.m_nX0, cFrame.m_nY1, cFrame.m_nX0, cFrame.m_nY0);
			cBitmap.drawLine (cDraw, cClip, cColor, eMode);
			m_cOldFrame.setTo (cFrame);
		}
	}
	int align (int nVal, int nAlign)
	{
		return (((int)nVal / (int)nAlign) * (int)nAlign);
	}
	int clamp (int nVal, int nDelta, int nMin, int nMax)
	{
		if (nVal + nDelta < nMin)
		{
			return (nMin - nVal);
		}
		else if (nVal + nDelta > nMax)
		{
			return (nMax - nVal);
		}
		else
		{
			return (nDelta);
		}	
	}
	PixelRect alignRect (PixelRect cRect, PixelRect cBorders)
	{
		PixelRect cAFrame = new PixelRect ();

		int nBorderWidth  = (int)(cBorders.m_nX0 + cBorders.m_nX1);
		int nBorderHeight = (int)(cBorders.m_nY0 + cBorders.m_nY1);

		cAFrame.m_nX0 =
			((cRect.m_nX0 + cBorders.m_nX0) / m_cAlignPos.m_nX) * m_cAlignPos.m_nX +
			m_cAlignPosOff.m_nX - cBorders.m_nX0;
		cAFrame.m_nY0 =
			((cRect.m_nY0 + cBorders.m_nY0) / m_cAlignPos.m_nY) * m_cAlignPos.m_nY +
			m_cAlignPosOff.m_nY - cBorders.m_nY0;

		cAFrame.m_nX1 =
			cAFrame.m_nX0 +
			((cRect.width () - nBorderWidth) / m_cAlignSize.m_nX) * m_cAlignSize.m_nX +
			m_cAlignSizeOff.m_nX + nBorderWidth;
		cAFrame.m_nY1 =
			cAFrame.m_nY0 +
			((cRect.height () - nBorderHeight) / m_cAlignSize.m_nY) * m_cAlignSize.m_nY +
			m_cAlignSizeOff.m_nY + nBorderHeight;
		return (cAFrame);
	}
	void wndMoveReply ()
	{
		//Debug.out.println ("WindowBorder::wndMoveReply(" + m_cDeltaMove + ", " + m_nPendingMoves + ")");
				
		if (m_cDeltaSize.m_nX != 0 || m_cDeltaSize.m_nY != 0 || m_cDeltaMove.m_nX != 0
			|| m_cDeltaMove.m_nY != 0)
		{
			if (m_cOwner != null)
			{
				PixelRect cBorders;
				
				if (m_cDecorator == null)
					return;					
				cBorders = m_cDecorator.getBorderSize ();

				m_cRawFrame.m_nX1 += m_cDeltaSize.m_nX + m_cDeltaMove.m_nX;
				m_cRawFrame.m_nY1 += m_cDeltaSize.m_nY + m_cDeltaMove.m_nY;
				m_cRawFrame.m_nX0 += m_cDeltaMove.m_nX;
				m_cRawFrame.m_nY0 += m_cDeltaMove.m_nY;
				m_cDeltaMove.setTo (0, 0);
				m_cDeltaSize.setTo (0, 0);
				m_bWndMovePending = true;
				PixelRect cAlignedFrame = alignRect (m_cRawFrame, cBorders);
				
				if (s_bUseWireFrames == true)
				{
					s_cDisplay.startUpdate ();
					WSprite.hide ();
					drawWireFrame (cAlignedFrame, false);
					WSprite.unhide ();
					s_cDisplay.endUpdate ();
				}
				else
				{
					doSetFrame (cAlignedFrame);
					s_cDisplay.startUpdate ();
					WSprite.hide ();
					m_cParent.updateRegions (false);
					WSprite.unhide ();
					s_cDisplay.endUpdate ();
				}
				m_cOwner.postMessage (new WWindowFrameChangedMessage (rectToClient (cAlignedFrame)));
			}
		}
		else
		{
			m_bWndMovePending = false;
			if (s_bUseWireFrames && m_bWireFramePending && m_nMouseDown == 0)
			{
				PixelRect cAlignedFrame = alignRect (m_cOldFrame, m_cDecorator.getBorderSize());
				s_cDisplay.startUpdate ();
				WSprite.hide ();
				drawWireFrame (null, true);
				doSetFrame (cAlignedFrame);
				m_cParent.updateRegions (false);
				WSprite.unhide ();
				s_cDisplay.endUpdate ();
			}
		}
	}
};
