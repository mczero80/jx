package jx.wm;

import jx.zero.*;
import jx.devices.fb.*;
import jx.wm.WBitmap;

class WSprite
{

	final private static boolean DBG_DISPLAY_BACKBUFFER = false;

	private static WSprite s_cHead = null;
	private static WSprite s_cTail 	= null;
	private static int s_nHideCount = 0;

	private WSprite m_cPrev = null;
	private WSprite m_cNext	= null;

	private PixelPoint m_cPosition;
	private PixelPoint m_cHotSpot;
	private WBitmap m_cImage;
	private WBitmap m_cTarget;

	private WBitmap m_cBackground;
	private PixelRect m_cBounds;
	private boolean m_bVisible;

	private DrawingMode	m_nMode = new DrawingMode();

	public WSprite (PixelRect cBounds, PixelPoint cPos, PixelPoint cHotSpot,
			WBitmap cTarget, WBitmap cImage)
	{
		m_cImage = cImage;
		m_cTarget = cTarget;

		m_nMode = DrawingMode.OVER;
		
		//Debug.out.println ("WSprite::WSprite(" + cTarget + ", " + cImage + ")");

		if (cImage != null)
			m_cBackground =
				WBitmap.createWBitmap (cImage.getWidth (), cImage.getHeight (),
										 cTarget.getColorSpace ());
		else
			m_cBackground = null;

		m_cPosition = new PixelPoint (cPos);
		m_cHotSpot  = new PixelPoint (cHotSpot);
		m_bVisible  = true;

		if (cImage != null)
			m_cBounds =
				new PixelRect (0, 0, cImage.getWidth () - 1, cImage.getHeight () - 1);
		else
			m_cBounds = new PixelRect (cBounds);
		/*
		 * if (cTarget.Bits () != null) 
		 */
		{
			WSprite cSprite;
			PixelPoint cTmpPos = new PixelPoint ();

			for (cSprite = s_cTail; cSprite != null; cSprite = cSprite.m_cPrev)
			{
				if (cSprite.m_cImage != null)
				{
					cTmpPos.setTo (cSprite.m_cPosition.m_nX - cSprite.m_cHotSpot.m_nX,
											cSprite.m_cPosition.m_nY - cSprite.m_cHotSpot.m_nY);
					cSprite.erase (cSprite.m_cTarget, cTmpPos);
				}
			}
			cTmpPos.setTo (m_cPosition.m_nX - m_cHotSpot.m_nX,

									m_cPosition.m_nY - m_cHotSpot.m_nY);
			capture (m_cTarget, cTmpPos);
			draw (m_cTarget, cTmpPos);

			for (cSprite = s_cHead; cSprite != null; cSprite = cSprite.m_cNext)
			{
				if (cSprite.m_cImage != null)
				{
					cTmpPos.setTo (cSprite.m_cPosition.m_nX - cSprite.m_cHotSpot.m_nX,
											cSprite.m_cPosition.m_nY - cSprite.m_cHotSpot.m_nY);
					cSprite.capture (cSprite.m_cTarget, cTmpPos);
					cSprite.draw (cSprite.m_cTarget, cTmpPos);
				}
			}
		}
		if ((m_cNext = s_cHead) != null)
			s_cHead.m_cPrev = this;
		m_cPrev = null;
		s_cHead = this;
		if (s_cTail == null)
			s_cTail = this;

	}
	public static void colorSpaceChanged ()
	{
		WSprite cSprite;

		for (cSprite = s_cHead; cSprite != null; cSprite = cSprite.m_cNext)
		{
			if (cSprite.m_cImage != null)
			{
				cSprite.m_cBackground = WBitmap.createWBitmap (cSprite.m_cImage.getWidth (),
					cSprite.m_cImage.getHeight (),
					cSprite.m_cTarget.getColorSpace ());
			}
		}
	}

	public static void hide (PixelRect cFrame)
	{
		boolean bDoHide = false;
		WSprite cSprite;
		PixelRect cRect = new PixelRect ();

		 s_nHideCount++;

		for (cSprite = s_cHead; cSprite != null; cSprite = cSprite.m_cNext)
		{
			cRect.setTo (cSprite.m_cBounds);
			cRect.m_nX0 += cSprite.m_cPosition.m_nX - cSprite.m_cHotSpot.m_nX;
			cRect.m_nY0 += cSprite.m_cPosition.m_nY - cSprite.m_cHotSpot.m_nY;
			cRect.m_nX1 += cSprite.m_cPosition.m_nX - cSprite.m_cHotSpot.m_nX;
			cRect.m_nY1 += cSprite.m_cPosition.m_nY - cSprite.m_cHotSpot.m_nY;
			if (cFrame.intersects (cRect))
			{
				bDoHide = true;
				//Debug.out.println ("Hiding sprite " + cRect + ", " + cFrame);
				break;
			}
		}
		if (bDoHide == true)
		{
			for (cSprite = s_cHead; cSprite != null; cSprite = cSprite.m_cNext)
			{
				if (cSprite.m_bVisible)
					cSprite.erase ();
			}
		}
	}

	public static void hide ()
	{
		WSprite cSprite;

		 s_nHideCount++;
		for (cSprite = s_cHead; cSprite != null; cSprite = cSprite.m_cNext)
		{
			if (cSprite.m_bVisible)
				cSprite.erase ();
		}
	}

	public static void unhide ()
	{
		WSprite cSprite;

		if (s_nHideCount-- != 1)
			 return;
		for (cSprite = s_cHead; cSprite != null; cSprite = cSprite.m_cNext)
		{
			if (cSprite.m_bVisible == false)
				cSprite.draw ();
		}
	}

	public void draw ()
	{
		if (m_bVisible)
			return;

		m_bVisible = true;

		PixelRect cBitmapRect = new PixelRect (0, 0, m_cTarget.getWidth () - 1, m_cTarget.getHeight () - 1);
		if (m_cImage != null)
		{
			PixelRect cRect = new PixelRect (m_cBounds);

			cRect.m_nX0 += m_cPosition.m_nX - m_cHotSpot.m_nX;
			cRect.m_nY0 += m_cPosition.m_nY - m_cHotSpot.m_nY;
			cRect.m_nX1 += m_cPosition.m_nX - m_cHotSpot.m_nX;
			cRect.m_nY1 += m_cPosition.m_nY - m_cHotSpot.m_nY;
			
			m_cBackground.drawBitmap (m_cTarget, m_cBounds, cRect, m_cBounds);

			if (DBG_DISPLAY_BACKBUFFER) {
				PixelRect tmp = new PixelRect(2,2,m_cBounds.width()+2,m_cBounds.height()+2);
				m_cTarget.drawBitmap (m_cBackground, tmp, m_cBounds, m_cBounds);
				tmp.m_nX0--;
				tmp.m_nY0--;
				//tmp.m_nX1++;
				//tmp.m_nY1++;
				m_cTarget.drawRect(tmp,PixelColor.RED,DrawingMode.COPY);
			}

			m_cTarget.drawBitmap (m_cImage, cRect, m_cBounds, cBitmapRect, m_nMode);
		}
	}

	public void erase ()
	{
		if (m_bVisible == false)
			return;

		PixelRect cBitmapRect = new PixelRect (0, 0, m_cTarget.getWidth () - 1, m_cTarget.getHeight () - 1);
		if (m_cImage != null)
		{
			PixelRect cRect = new PixelRect (m_cBounds);

			cRect.m_nX0 += m_cPosition.m_nX - m_cHotSpot.m_nX;
			cRect.m_nY0 += m_cPosition.m_nY - m_cHotSpot.m_nY;
			cRect.m_nX1 += m_cPosition.m_nX - m_cHotSpot.m_nX;
			cRect.m_nY1 += m_cPosition.m_nY - m_cHotSpot.m_nY;
			
			m_cTarget.drawBitmap (m_cBackground, cRect, m_cBounds, cBitmapRect);
		}
		m_bVisible = false;
	}

	public void draw (WBitmap cTarget, PixelPoint cPos)
	{
		PixelRect cBitmapRect = new PixelRect (0, 0, cTarget.getWidth () - 1, cTarget.getHeight () - 1);
		if (m_cImage != null)
		{
			PixelRect cRect = new PixelRect (m_cBounds);
			cRect.add (cPos);
			cTarget.drawBitmap (m_cImage, cRect, m_cBounds, cBitmapRect, m_nMode);
		}
	}

	public void capture (WBitmap cTarget, PixelPoint cPos)
	{
		if (m_cImage != null)
		{
			PixelRect cRect = new PixelRect (m_cBounds);
			cRect.add (cPos);
			m_cBackground.drawBitmap (cTarget, m_cBounds, cRect, m_cBounds);
		}
	}

	public void erase (WBitmap cTarget, PixelPoint cPos)
	{
		PixelRect cBitmapRect = new PixelRect (0, 0, cTarget.getWidth () - 1, cTarget.getHeight () - 1);
		if (m_cImage != null)
		{
			PixelRect cRect = new PixelRect (m_cBounds);
			cRect.add (cPos);
			cTarget.drawBitmap (m_cBackground, cRect, m_cBounds, cBitmapRect);
		}
	}

	public void moveBy (PixelPoint cDelta)
	{
		WSprite cSprite;
		PixelPoint cPos = new PixelPoint ();

		if (s_nHideCount == 0)
		{
			for (cSprite = s_cTail; cSprite != null; cSprite = cSprite.m_cPrev)
			{
				cPos.setTo (cSprite.m_cPosition);
				cPos.sub (cSprite.m_cHotSpot);
				cSprite.erase (cSprite.m_cTarget, cPos);
			}				
		}
		for (cSprite = s_cHead; cSprite != null; cSprite = cSprite.m_cNext)
		{
			cSprite.m_cPosition.add (cDelta);
			if (s_nHideCount == 0)
			{
				cPos.setTo (cSprite.m_cPosition);
				cPos.sub (cSprite.m_cHotSpot);
				cSprite.capture (cSprite.m_cTarget, cPos);
				cSprite.draw (cSprite.m_cTarget, cPos);
			}
		}
	}

	public void moveTo (PixelPoint cNewPos)
	{
		WSprite cSprite;
		PixelPoint cPos = new PixelPoint ();

		if (s_nHideCount == 0)
		{
			for (cSprite = s_cTail; cSprite != null; cSprite = cSprite.m_cPrev)
			{
				cPos.setTo (cSprite.m_cPosition);
				cPos.sub (cSprite.m_cHotSpot);
				cSprite.erase (cSprite.m_cTarget, cPos);
			}				
		}
		for (cSprite = s_cHead; cSprite != null; cSprite = cSprite.m_cNext)
		{
			cSprite.m_cPosition.setTo (cNewPos);
			if (s_nHideCount == 0)
			{
				cPos.setTo (cSprite.m_cPosition);
				cPos.sub (cSprite.m_cHotSpot);
				cSprite.capture (cSprite.m_cTarget, cPos);
				cSprite.draw (cSprite.m_cTarget, cPos);
			}
		}
	}
}
