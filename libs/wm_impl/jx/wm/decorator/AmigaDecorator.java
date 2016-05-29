package jx.wm.decorator;

import jx.zero.*;
import jx.wm.decorator.WindowDecorator;
import jx.wm.WFont;
import jx.wm.WFontHeight;
import jx.wm.WWindowImpl;
import jx.wm.WView;
import jx.wm.WindowFlags;
import jx.devices.fb.*;

public class AmigaDecorator extends WindowDecorator
{
	static PixelColor s_cSelWinCol = new PixelColor (255, 0, 0, 0);
	static PixelColor s_cWinCol = new PixelColor (0, 0, 255, 0);

 	WFontHeight 		m_sFontHeight;
	PixelRect 			m_cBounds;
	String			m_cTitle;
	WindowFlags			m_nFlags;

	PixelRect 			m_cCloseRect;
	PixelRect 			m_cZoomRect;
	PixelRect 			m_cToggleRect;
	PixelRect 			m_cDragRect;

	int 			m_nLeftBorder;
	int	 		m_nTopBorder;
	int 			m_nRightBorder;
	int 			m_nBottomBorder;

	boolean 		m_bHasFocus;
	boolean		 	m_bCloseState;
	boolean 		m_bZoomState;
	boolean 		m_bDepthState;
	public AmigaDecorator (WView cSrvView, WindowFlags nWndFlags)
	{
		super (cSrvView);
		m_nFlags = new WindowFlags (nWndFlags);

		m_bHasFocus   = false;
		m_bCloseState = false;
		m_bZoomState  = false;
		m_bDepthState = false;
		m_sFontHeight 	= new WFontHeight();
		m_cCloseRect 	= new PixelRect ();
		m_cZoomRect	= new PixelRect ();
		m_cToggleRect	= new PixelRect ();
		m_cDragRect	= new PixelRect ();
		m_cBounds	= new PixelRect ();

		/*TODOm_sFontHeight = pcSrvView.GetFontheight();*/
		cSrvView.getFontHeight (m_sFontHeight);

		calculateBorderSizes ();
	}
	void calculateBorderSizes ()
	{	
		if ((m_nFlags.getValue() & WindowFlags.WND_NO_BORDER) != 0)
		{
			m_nLeftBorder = 0;
			m_nRightBorder = 0;
			m_nTopBorder = 0;
			m_nBottomBorder = 0;
		}
		else
		{
			if ((m_nFlags.getValue() &
				 (WindowFlags.WND_NOT_MOVABLE | WindowFlags.WND_NOT_CLOSABLE | WindowFlags.WND_NOT_ZOOMABLE | WindowFlags.WND_NOT_MINIMIZABLE
					| WindowFlags.WND_NOT_MOVABLE)) ==
				(WindowFlags.WND_NOT_MOVABLE | WindowFlags.WND_NOT_CLOSABLE | WindowFlags.WND_NOT_ZOOMABLE | WindowFlags.WND_NOT_MINIMIZABLE
				 | WindowFlags.WND_NOT_MOVABLE))
			{
				m_nTopBorder = 4;
			}
			else
			{
				m_nTopBorder =	m_sFontHeight.nAscender + m_sFontHeight.nDescender + 6;
			}
			m_nLeftBorder = 4;
			m_nRightBorder = 4;
			m_nBottomBorder = 4;
		}
		//Debug.out.println (m_cTitle + ": AmigaDecorator::calculateBorderSizes [" + m_nLeftBorder + "," + m_nTopBorder +
		//"," + m_nRightBorder + "," + m_nBottomBorder + "]");	
	}
	public int hitTest (PixelPoint cPosition)
	{
		if (cPosition.m_nX < 4)
		{
			if (cPosition.m_nY < 4)
			{
				return (HIT_SIZE_LT);
			}
			else if (cPosition.m_nY > m_cBounds.m_nY1 - 4)
			{
				return (HIT_SIZE_LB);
			}
			else
			{
				return (HIT_SIZE_LEFT);
			}
		}	
		else if (cPosition.m_nX > m_cBounds.m_nX1 - 4)
		{
			if (cPosition.m_nY < 4)
			{
				return (HIT_SIZE_RT);
			}
			else if (cPosition.m_nY > m_cBounds.m_nY1 - 4)
			{
				return (HIT_SIZE_RB);
			}
			else
			{
				return (HIT_SIZE_RIGHT);
			}
		}
		else if (cPosition.m_nY < 4)
		{
			return (HIT_SIZE_TOP);
		}
		else if (cPosition.m_nY > m_cBounds.m_nY1 - 4)
		{
			return (HIT_SIZE_BOTTOM);
		}
		if (m_cCloseRect.intersects (cPosition))
		{
			return (HIT_CLOSE);
		}	
		else if (m_cZoomRect.intersects (cPosition))
		{
			return (HIT_ZOOM);
		}
		else if (m_cToggleRect.intersects (cPosition))
		{
			return (HIT_DEPTH);
		}
		else if (m_cDragRect.intersects (cPosition))
		{
			return (HIT_DRAG);
		}
		return HIT_NONE;
	}
	public void frameSized (PixelRect cFrame)
	{
		WView cView = getView ();
		PixelPoint cDelta = new PixelPoint (cFrame.width() - m_cBounds.width(), cFrame.height() - m_cBounds.height());
		m_cBounds.setTo (cFrame.bounds ());
		
		/*Debug.out.println (m_cTitle + ": AmigaDecorator::frameSized(" + cFrame + ")");*/

		layout ();
		if (cDelta.m_nX != 0)
		{
			PixelRect cDamage = new PixelRect (m_cBounds);

			cDamage.m_nX0 = m_cZoomRect.m_nX0 - cDelta.m_nX - 2;
			cView.invalidate (cDamage);
		}
		if (cDelta.m_nY != 0)
		{
			PixelRect cDamage = new PixelRect (m_cBounds);
	
			cDamage.m_nY0 = cDamage.m_nY1 - __max__ (m_nBottomBorder, m_nBottomBorder + cDelta.m_nY) - 1;
			cView.invalidate (cDamage);
		}
	}
	public PixelRect getBorderSize ()
	{
		return new PixelRect (m_nLeftBorder, m_nTopBorder, m_nRightBorder, m_nBottomBorder);
	}
	public PixelPoint getMinimumSize ()
	{
		PixelPoint cMinSize = new PixelPoint (0, m_nTopBorder + m_nBottomBorder);

		if ((m_nFlags.getValue() & WindowFlags.WND_NOT_CLOSABLE) == 0)
		{
			cMinSize.m_nX += m_cCloseRect.width();
		}
		if ((m_nFlags.getValue() & WindowFlags.WND_NOT_ZOOMABLE) == 0)
		{
			cMinSize.m_nX += m_cZoomRect.width();
		}
		if ((m_nFlags.getValue() & WindowFlags.WND_NOT_MINIMIZABLE) == 0)
		{
			cMinSize.m_nX += m_cToggleRect.width();
		}
		if (cMinSize.m_nX < m_nLeftBorder + m_nRightBorder)
		{
			cMinSize.m_nX = m_nLeftBorder + m_nRightBorder;
		}
		return (cMinSize);
	}
	public void setTitle (String cTitle)
	{
		m_cTitle = cTitle;
		render (m_cBounds);
	}
	public void setFlags (WindowFlags nFlags)
	{
		m_nFlags.setValue (nFlags.getValue());
		calculateBorderSizes ();
		getView().invalidate ();
		layout ();
		render (m_cBounds);
	}
	public void fontChanged ()
	{
		WView cView = getView ();

		cView.getFontHeight (m_sFontHeight);
		calculateBorderSizes ();
		cView.invalidate ();
		layout ();
	}
	public void setWindowFlags (WindowFlags nFlags)
	{
		m_nFlags.setValue (nFlags.getValue());
	}
	public void setFocusState (boolean bHasFocus)
	{
		m_bHasFocus = bHasFocus;
		render (m_cBounds);
	}
	public void setCloseButtonState (boolean bPushed)
	{
		m_bCloseState = bPushed;
		render (m_cCloseRect);
	}	
	public void setZoomButtonState (boolean bPushed)
	{
		m_bZoomState = bPushed;
		render (m_cZoomRect);
	}
	public void setDepthButtonState (boolean bPushed)
	{
		m_bDepthState = bPushed;
		render (m_cToggleRect);
	}
	public void render (PixelRect cUpdateRect)
	{
		/*Debug.out.println (m_cTitle + ": AmigaDecorator::render() " + cUpdateRect);*/
/*		
		getView().fillRect (cUpdateRect, s_cSelWinCol);
		return;
*/
		if ((m_nFlags.getValue() & WindowFlags.WND_NO_BORDER) != 0)
			return;

		PixelColor sFillColor = m_bHasFocus ? s_cSelWinCol : s_cWinCol;

		WView cView = getView ();	
		
		cView.fillRect (cUpdateRect, sFillColor);

		PixelRect cOBounds = cView.getBounds ();
		PixelRect cIBounds = new PixelRect (cOBounds);

		cIBounds.m_nX0 += m_nLeftBorder - 1;
		cIBounds.m_nX1 -= m_nRightBorder - 1;
		cIBounds.m_nY0 += m_nTopBorder - 1;
		cIBounds.m_nY1 -= m_nBottomBorder - 1;

		cView.drawFrame (cOBounds, WView.FRAME_RAISED | WView.FRAME_THIN | WView.FRAME_TRANSPARENT);
		cView.drawFrame (cIBounds, WView.FRAME_RECESSED | WView.FRAME_THIN | WView.FRAME_TRANSPARENT);

		// Bottom
		cView.fillRect (new PixelRect (cOBounds.m_nX0 + 1, cIBounds.m_nY1 + 1, 
			cOBounds.m_nX1 - 1, cOBounds.m_nY1 - 1), sFillColor);
		// Left
		cView.fillRect (new PixelRect (cOBounds.m_nX0 + 1, cOBounds.m_nY0 + m_nTopBorder, 
			cIBounds.m_nX0 - 1, cIBounds.m_nY1), sFillColor);
		// Right
		cView.fillRect (new PixelRect (cIBounds.m_nX1 + 1, cOBounds.m_nY0 + m_nTopBorder, 
			cOBounds.m_nX1 - 1, cIBounds.m_nY1), sFillColor);

		if ((m_nFlags.getValue() & WindowFlags.WND_NOT_CLOSABLE) == 0)
		{
			drawClose (m_cCloseRect, sFillColor, m_bCloseState == true);
		}

		if ((m_nFlags.getValue() & WindowFlags.WND_NOT_MOVABLE) == 0)
		{
			cView.fillRect (m_cDragRect, sFillColor);
			cView.drawFrame (m_cDragRect, WView.FRAME_RAISED | WView.FRAME_TRANSPARENT);
			
			//cView.setFgColor (255, 255, 255, 0);
			cView.setFgColor (0, 0, 0);
			cView.setBgColor (sFillColor);
/*			
			cView.movePenTo (m_cDragRect.m_nX0 + 5, 
					(m_cDragRect.height() + 1) / 2 -
					(m_sFontHeight.nAscender +
					m_sFontHeight.nDescender) / 2 +
					 m_sFontHeight.nAscender +
					 m_sFontHeight.nLineGap / 2);
*/
			cView.movePenTo (m_cDragRect.m_nX0 + 5, 2);
			cView.drawString (m_cTitle, -1);
			
		}
		else
		{
			cView.fillRect (new PixelRect (cOBounds.m_nX0 + 1, cOBounds.m_nY0 - 1, 
				cOBounds.m_nX1 - 1, cIBounds.m_nY0 + 1), sFillColor);
		}

		if ((m_nFlags.getValue() & WindowFlags.WND_NOT_ZOOMABLE) == 0)
		{
			drawZoom (m_cZoomRect, sFillColor, m_bZoomState == true);
		}
		if ((m_nFlags.getValue() & WindowFlags.WND_NOT_MINIMIZABLE) == 0)
		{
			drawDepth (m_cToggleRect, sFillColor, m_bDepthState == true);
		}		
	}

	private void layout ()
	{
		/*Debug.out.println (m_cTitle + ": AmigaDecorator::layout() " + m_cBounds);*/
		if ((m_nFlags.getValue() & WindowFlags.WND_NOT_CLOSABLE) != 0)
		{
			m_cCloseRect.m_nX0 = 0;
			m_cCloseRect.m_nX1 = 0;
			m_cCloseRect.m_nY0 = 0;
			m_cCloseRect.m_nY1 = 0;
		}
		else
		{
			m_cCloseRect.m_nX0 = 0;
			m_cCloseRect.m_nX1 = m_nTopBorder - 1;
			m_cCloseRect.m_nY0 = 0;
			m_cCloseRect.m_nY1 = m_nTopBorder - 1;
		}

		m_cToggleRect.m_nX1 = m_cBounds.m_nX1;
		if ((m_nFlags.getValue() & WindowFlags.WND_NOT_MINIMIZABLE) != 0)
		{
			m_cToggleRect.m_nX0 = m_cToggleRect.m_nX1;
		}
		else
		{
			//m_cToggleRect.m_nX0 = ceil (m_cToggleRect.m_nX1 - m_nTopBorder * 1.5f);
			m_cToggleRect.m_nX0 = m_cToggleRect.m_nX1 - (m_nTopBorder + m_nTopBorder / 2);
		}
		m_cToggleRect.m_nY0 = 0;
		m_cToggleRect.m_nY1 = m_nTopBorder - 1;


		if ((m_nFlags.getValue() & WindowFlags.WND_NOT_ZOOMABLE) != 0)
		{
			m_cZoomRect.m_nX0 = m_cToggleRect.m_nX0;
			m_cZoomRect.m_nX1 = m_cToggleRect.m_nX0;
		}
		else
		{
			if ((m_nFlags.getValue() & WindowFlags.WND_NOT_MINIMIZABLE) != 0)
			{
				m_cZoomRect.m_nX1 = m_cBounds.m_nX1;
			}
			else
			{
				m_cZoomRect.m_nX1 = m_cToggleRect.m_nX0 - 1;
			}
			//m_cZoomRect.m_nX0 = ceil (m_cZoomRect.m_nX1 - m_nTopBorder * 1.5f);
			m_cZoomRect.m_nX0 = m_cZoomRect.m_nX1 - (m_nTopBorder + m_nTopBorder / 2);
		}
		m_cZoomRect.m_nY0 = 0;
		m_cZoomRect.m_nY1 = m_nTopBorder - 1;
	
		if ((m_nFlags.getValue() & WindowFlags.WND_NOT_CLOSABLE) != 0)
		{
			m_cDragRect.m_nX0 = 0;
		}
		else
		{
			m_cDragRect.m_nX0 = m_cCloseRect.m_nX1 + 1;
		}
		if ((m_nFlags.getValue() & WindowFlags.WND_NOT_ZOOMABLE) != 0)
		{
			if ((m_nFlags.getValue() & WindowFlags.WND_NOT_MINIMIZABLE) != 0)
			{
				m_cDragRect.m_nX1 = m_cBounds.m_nX1;
			}
			else
			{
				m_cDragRect.m_nX1 = m_cToggleRect.m_nX0 - 1;
			}	
		}
		else
		{
			m_cDragRect.m_nX1 = m_cZoomRect.m_nX0 - 1;
		}
		m_cDragRect.m_nY0 = 0;
		m_cDragRect.m_nY1 = m_nTopBorder - 1;
	}	
	private int __max__ (int a, int b)
	{
		return a > b ? a : b;
	}
	void drawDepth (PixelRect cRect, PixelColor sFillColor, boolean bRecessed)
	{
		PixelRect L = new PixelRect (), R = new PixelRect ();
		
		/*Debug.out.println (m_cTitle + ": AmigaDecorator::drawDepth() " + cRect);*/

		L.m_nX0 = cRect.m_nX0 + ((cRect.width() + 1) / 7);
		L.m_nY0 = cRect.m_nY0 + ((cRect.height() + 1) / 7);

		L.m_nX1 = L.m_nX0 + ((cRect.width() + 1) * 4 / 7);
		L.m_nY1 = L.m_nY0 + ((cRect.height() + 1) / 2);

		R.m_nX1 = cRect.m_nX1 - ((cRect.width() + 1) / 7);
		R.m_nY1 = cRect.m_nY1 - ((cRect.height() + 1) / 7);

		R.m_nX0 = R.m_nX1 - ((cRect.width() + 1) * 4 / 7);
		R.m_nY0 = R.m_nY1 - ((cRect.height() + 1) / 2);

		WView cView = getView ();

		if (bRecessed)
		{
			cView.fillRect (cRect, sFillColor);
			cView.drawFrame (cRect, WView.FRAME_RECESSED | WView.FRAME_TRANSPARENT);
			cView.drawFrame (L, WView.FRAME_RAISED | WView.FRAME_TRANSPARENT);
			cView.fillRect (R, sFillColor);
			cView.drawFrame (R, WView.FRAME_RAISED | WView.FRAME_TRANSPARENT);
		}
		else
		{
			cView.fillRect (cRect, sFillColor);
			cView.drawFrame (cRect, WView.FRAME_RAISED | WView.FRAME_TRANSPARENT);
			cView.drawFrame (L, WView.FRAME_RECESSED | WView.FRAME_TRANSPARENT);
			cView.fillRect (R, sFillColor);
			cView.drawFrame (R, WView.FRAME_RAISED | WView.FRAME_TRANSPARENT);
		}
	}
	void drawZoom (PixelRect cRect, PixelColor sFillColor, boolean bRecessed)
	{
		PixelRect L = new PixelRect (), R = new PixelRect ();

		/*Debug.out.println (m_cTitle + ": AmigaDecorator::drawZoom() " + cRect);*/

		L.m_nX0 = cRect.m_nX0 + ((cRect.width() + 1) / 6);
		L.m_nY0 = cRect.m_nY0 + ((cRect.height() + 1) / 6);

		L.m_nX1 = cRect.m_nX1 - ((cRect.width() + 1) / 6);
		L.m_nY1 = cRect.m_nY1 - ((cRect.height() + 1) / 6);

		R.m_nX0 = L.m_nX0 + 1;
		R.m_nY0 = L.m_nY0 + 1;

		R.m_nX1 = R.m_nX0 + ((cRect.width() + 1) / 3);
		R.m_nY1 = R.m_nY0 + ((cRect.height() + 1) / 3);

		WView cView = getView ();

		if (bRecessed)
		{
			cView.fillRect (cRect, sFillColor);
			cView.drawFrame (cRect, WView.FRAME_RECESSED | WView.FRAME_TRANSPARENT);
			cView.drawFrame (R, WView.FRAME_RAISED | WView.FRAME_TRANSPARENT);
			cView.drawFrame (L, WView.FRAME_RECESSED | WView.FRAME_TRANSPARENT);
	
			cView.fillRect (new PixelRect (L.m_nX0 + 1, L.m_nY0 + 1, L.m_nX1 - 1, L.m_nY1 - 1),
											sFillColor);
		}
		else
		{
			cView.fillRect (cRect, sFillColor);
			cView.drawFrame (cRect, WView.FRAME_RAISED | WView.FRAME_TRANSPARENT);
			cView.drawFrame (L, WView.FRAME_RAISED | WView.FRAME_TRANSPARENT);
			cView.drawFrame (R, WView.FRAME_RECESSED | WView.FRAME_TRANSPARENT);
		}
	}
	void drawClose (PixelRect cRect, PixelColor sFillColor, boolean bRecessed)
	{
		PixelRect L = new PixelRect ();

		/*Debug.out.println (m_cTitle + ": AmigaDecorator::drawClose() " + cRect);*/

		L.m_nX0 = cRect.m_nX0 + ((cRect.width() + 1) / 3);
		L.m_nY0 = cRect.m_nY0 + ((cRect.height() + 1) / 3);

		L.m_nX1 = cRect.m_nX1 - ((cRect.width() + 1) / 3);
		L.m_nY1 = cRect.m_nY1 - ((cRect.height() + 1) / 3);

		WView cView = getView ();
	
		if (bRecessed)
		{
			cView.fillRect (cRect, sFillColor);
			cView.drawFrame (cRect, WView.FRAME_RECESSED | WView.FRAME_TRANSPARENT);
			cView.drawFrame (L, WView.FRAME_RAISED | WView.FRAME_TRANSPARENT);
		}
		else
		{
			cView.fillRect (cRect, sFillColor);
			cView.drawFrame (cRect, WView.FRAME_RAISED | WView.FRAME_TRANSPARENT);
			cView.drawFrame (L, WView.FRAME_RECESSED | WView.FRAME_TRANSPARENT);
		}
	}
}
