package jx.wm.bitmap;

import jx.zero.*;
import jx.devices.fb.*;
import jx.wm.*;

public class WBitmapOrg extends WBitmapMemory
{
	void init (int nWidth, int nHeight, ColorSpace eColorSpace, int nBytesPerLine) {
  		int nCpp = 0;  
		Naming cNaming;
		
		Debug.out.println ("WBitmap::init(" + nWidth + ", " + nHeight + ", " + eColorSpace + ", " + nBytesPerLine + ")");

		if ((cNaming = InitialNaming.getInitialNaming()) == null)
			throw new Error ("WBitmap::WBitmap() unable to obtain Naming!");
      		MemoryManager cMemMgr = (MemoryManager)cNaming.lookup("MemoryManager");
      		if (cMemMgr == null)
			throw new Error("WBitmap::WBitmap() no MemoryManager found");
  
		m_cDisplayDriver = null;
  		switch (eColorSpace.getValue())
  		{
    			case ColorSpace.CS_RGB32:
    			case ColorSpace.CS_RGBA32:
      				nCpp = 8;
      				break;
    			case ColorSpace.CS_RGB24:
      				nCpp = 6;
      				break;
   	 		case ColorSpace.CS_RGB16:
    			case ColorSpace.CS_RGBA15:
    			case ColorSpace.CS_RGB15:
      				nCpp = 4;
      				break;
   	 		case ColorSpace.CS_CMAP8: 
    			case ColorSpace.CS_GRAY8:
      				nCpp = 2;
      				break;
    			case ColorSpace.CS_CMAP4:
    			case ColorSpace.CS_GRAY4:
      				nCpp = 1;
      				break;
    			default:
      				throw new Error ("WBitmap::WBitmap() unsupported color space!"+eColorSpace);      
  		}
  		m_nBytesPerLine = (nWidth * nCpp) >> 1;
  		if ((m_nBytesPerLine << 1) != (nWidth * nCpp))
    			m_nBytesPerLine += 1;
	
		if (nBytesPerLine != -1)
		{
			if (m_nBytesPerLine > nBytesPerLine)
			{
				m_nBytesPerLine = -1;
				return;			
			}
			m_nBytesPerLine = nBytesPerLine;			
  		}
		m_nSize = nHeight * m_nBytesPerLine;
		if ((m_cMemory = cMemMgr.alloc (m_nSize)) == null)
			throw new Error ("WBitmap::WBitmap() unable to allocate memory!");	
		m_nOffset = 0;
  		m_eColorSpace.setValue (eColorSpace.getValue());
  		m_nWidth = nWidth;
  		m_nHeight = nHeight;
		m_cBounds = new PixelRect (0, 0, m_nWidth - 1, m_nHeight - 1);
		m_bIsVideoMemory = false;
	}

	public WBitmapOrg ()
	{
		m_eColorSpace.setValue (ColorSpace.CS_NO_COLOR_SPACE);
		m_nWidth = m_nHeight = m_nBytesPerLine = m_nOffset = 0;
		m_cMemory = null;
		m_cBounds = null;
		m_cDisplayDriver = null;
		m_bIsVideoMemory = false;
	}

	public WBitmapOrg (int nWidth, int nHeight, ColorSpace eColorSpace, int nBytesPerLine)
	{
		init (nWidth, nHeight, eColorSpace, nBytesPerLine);
	}

	public WBitmapOrg (int nWidth, int nHeight, ColorSpace eColorSpace)
	{
		init (nWidth, nHeight, eColorSpace, -1);
	}

	public WBitmapOrg (FramebufferDevice cDisplayDriver, int nWidth, int nHeight, ColorSpace eColorSpace, int nBytesPerLine, Memory cMemory, int nOffset)
	{
		Debug.out.println ("WBitmap::init(" + nWidth + ", " + nHeight + ", " + eColorSpace + ", " + nBytesPerLine + ")");
		m_cDisplayDriver = cDisplayDriver;
		m_nWidth = nWidth;
		m_nHeight = nHeight;
		m_eColorSpace.setValue (eColorSpace.getValue());
		m_nBytesPerLine = nBytesPerLine;
		m_cMemory = cMemory;
		m_nOffset = nOffset;
		m_nSize = cMemory.size ();
		m_cBounds = new PixelRect (0, 0, m_nWidth - 1, m_nHeight - 1);
		m_bIsVideoMemory = true;
	}

	public void finalize ()
	{
		Debug.out.println ("WBitmap::finalize(" + this + ")");
	}

	public boolean IsVideoMemory ()
	{
		return m_bIsVideoMemory;
	}
	public PixelRect bounds()
	{
		return m_cBounds;
	}
	public PixelRect getBounds ()
	{
		return m_cBounds;
	}
	public int width ()
	{
		return m_nWidth;
	}
	public int height()
	{
		return m_nHeight;
	}
	public int getWidth ()
	{
		return m_nWidth;
	}
	public int getHeight()
	{
		return m_nHeight;
	}
	public ColorSpace getColorSpace()
	{
		return m_eColorSpace;
	}
	public int bytesPerLine()
	{
		return m_nBytesPerLine;
	}
	public void set8 (int nOffset, byte nValue)
	{
		m_cMemory.set8 (m_nOffset + nOffset, nValue);		
	}
	public byte get8 (int nOffset)
	{
		return m_cMemory.get8 (m_nOffset + nOffset);
	}
	public void set16 (int nOffset, short nValue)
	{
		m_cMemory.set16 ((m_nOffset + nOffset) >> 1, nValue);		
	}
	public short get16 (int nOffset)
	{
		return m_cMemory.get16 ((m_nOffset + nOffset) >> 1);
	}
	public void set32 (int nOffset, int nValue)
	{
		m_cMemory.set32 ((m_nOffset + nOffset) >> 2, nValue);		
	}
	public int get32 (int nOffset)
	{
		return m_cMemory.get32 ((m_nOffset + nOffset) >> 2);
	}
	public void fill16 (int nOffset, int nLen, short nValue)
	{
		m_cMemory.fill16 (nValue, (m_nOffset + nOffset) >> 1, nLen);
	}
	public void fill32 (int nOffset, int nLen, int nValue)
	{
		m_cMemory.fill32 (nValue, (m_nOffset + nOffset) >> 2, nLen);
	}
	void startFrameBufferUpdate()
	{
		if (m_cDisplayDriver != null)
			m_cDisplayDriver.startFrameBufferUpdate ();
	}
	void endFrameBufferUpdate()
	{
		if (m_cDisplayDriver != null)
			m_cDisplayDriver.endFrameBufferUpdate ();
	}

	private static PixelRect cClipped = new PixelRect();
	public void drawLine_Unsafe(PixelRect cDraw, PixelRect cClip, PixelColor cColor, DrawingMode nDrawingMode)
	{	
		cClipped.setTo(cDraw);
		
		cClip.clip (m_cBounds);		
		if (cClip.isValid() == false) return;
		if (clipLine (cClip, cClipped) == false)  return; 
		
		if (m_cDisplayDriver != null && m_cDisplayDriver.drawLine (cDraw, cClipped, cColor, nDrawingMode) == 0)
			return;			
			
		startFrameBufferUpdate (); 			
		switch (m_eColorSpace.getValue())
		{
 			case ColorSpace.CS_RGB16:
				drawLineRGB16 (cDraw, cClipped, cColor.toRGB16(), nDrawingMode);
				break;
 			case ColorSpace.CS_RGB32:
				drawLineRGB32 (cDraw, cClipped, cColor.toRGB32(), nDrawingMode);
				break;
			default:
				break;
		}
		endFrameBufferUpdate ();
	}

	public void drawLine (PixelRect cDraw, PixelRect cTmpClip, PixelColor cColor, DrawingMode nDrawingMode)
	{	
		PixelRect cClipped = new PixelRect (cDraw);
		PixelRect cClip = new PixelRect (cTmpClip);

		//Debug.out.println ("drawLine (" + cDraw + ", " + cTmpClip + ", " + cColor + ") ");			
		
		cClip.clip (m_cBounds);		
		if (cClip.isValid() == false)
		{
			//Debug.out.println ("drawLine(" + cDraw + ", " + cTmpClip + ") has been clipped1 (" + cClip + ")");
			return;
		}
		if (clipLine (cClip, cClipped) == false)
		{
			//Debug.out.println ("drawLine(" + cDraw + ", " + cTmpClip + ") has been clipped2 (" + cClipped + ", " + cClip + ")");
			return;
		}
		if (cClipped.m_nX0 < m_cBounds.m_nX0 || cClipped.m_nX0 > m_cBounds.m_nX1 ||
		    cClipped.m_nX1 < m_cBounds.m_nX0 || cClipped.m_nX1 > m_cBounds.m_nX1 ||
		    cClipped.m_nY0 < m_cBounds.m_nY0 || cClipped.m_nY0 > m_cBounds.m_nY1 ||
		    cClipped.m_nY1 < m_cBounds.m_nY0 || cClipped.m_nY1 > m_cBounds.m_nY1)
		{
			Debug.out.println ("BUG() in drawLine " + cClipped + " / " + m_cBounds);
		}

		
		if (m_cDisplayDriver != null && m_cDisplayDriver.drawLine (cDraw, cClipped, cColor, nDrawingMode) == 0)
			return;			
			
		startFrameBufferUpdate (); 			
		switch (m_eColorSpace.getValue())
		{
 			case ColorSpace.CS_RGB16:
				drawLineRGB16 (cDraw, cClipped, cColor.toRGB16(), nDrawingMode);
				break;
 			case ColorSpace.CS_RGB32:
				drawLineRGB32 (cDraw, cClipped, cColor.toRGB32(), nDrawingMode);
				break;
			default:
				break;
		}
		endFrameBufferUpdate ();
	}

	public void fillRect (PixelRect cRect[], int nCount, PixelColor cColor, DrawingMode nMode)
	{
		if (m_cDisplayDriver != null && m_cDisplayDriver.fillRect (cRect, nCount, cColor, nMode) == 0)
			return;
			
		startFrameBufferUpdate (); 			
		switch (m_eColorSpace.getValue())
		{
			case ColorSpace.CS_RGB16:
				fillRectRGB16 (cRect, nCount, cColor.toRGB16(), nMode);
				break;
			case ColorSpace.CS_RGB15:
				fillRectRGB16 (cRect, nCount, cColor.toRGB15(), nMode);
				break;
			case ColorSpace.CS_RGB32:
				fillRectRGB32 (cRect, nCount, cColor.toRGB32(), nMode);
				break;
			default:
				break;				
		}			
		endFrameBufferUpdate ();
	}
	public void fillRect (PixelRect cRect, PixelColor cColor, DrawingMode nMode)
	{
		PixelRect cRects[] = new PixelRect[1];
		cRects[0] = cRect;
		fillRect (cRects, 1, cColor, nMode);
	}
	

	private void drawLineRGB16 (PixelRect cDraw, PixelRect cClipped, short nColor, DrawingMode nDrawingMode)
	{
  		int nODeltaX = abs (cDraw.m_nX1 - cDraw.m_nX0);
  		int nODeltaY = abs (cDraw.m_nY1 - cDraw.m_nY0);
  		int ox1 = cDraw.m_nX0;
  		int oy1 = cDraw.m_nY0;
 	 	int nModulo = m_nBytesPerLine;
		int nOffset = 0;
  		int nDeltaX = abs (cClipped.m_nX1 - cClipped.m_nX0);
  		int nDeltaY = abs (cClipped.m_nY1 - cClipped.m_nY0);
		int nStep, nInc, nDelta, d, dinc1, dinc2;
		
		//Debug.out.println ("drawLineRGB16(" + cDraw + ", " + cClipped + ", " + Integer.toHexString(nColor) + ", " + nDrawingMode + ")");

  		if (nODeltaX > nODeltaY)
  		{
  			int nYStep;
  			
			dinc1 = nODeltaY << 1;
  			dinc2 = (nODeltaY - nODeltaX) << 1;
	  		d = dinc1 - nODeltaX;

  			if (ox1 != cClipped.m_nX0 || oy1 != cClipped.m_nY0) 
    			{
	    			int nClipDeltaX = abs(cClipped.m_nX0 - ox1);
	    			int nClipDeltaY = abs(cClipped.m_nY0 - oy1);
	    			d += ((nClipDeltaY*dinc2) + ((nClipDeltaX-nClipDeltaY)*dinc1));
  			}    
  			if (cClipped.m_nY0 > cClipped.m_nY1) 
	    			nYStep = -nModulo;
	  		else
	    			nYStep = nModulo;
  			if (cClipped.m_nX0 > cClipped.m_nX1) 
    			{
	    			nYStep = -nYStep;
	   	 		nOffset = cClipped.m_nX1 * 2 + nModulo * cClipped.m_nY1;
	  		} 
    			else 
    			{
	    			nOffset = cClipped.m_nX0 * 2 + nModulo * cClipped.m_nY0;
  			}
			nDelta = nDeltaX;
			nStep  = nYStep;
			nInc   = 2;
	
  		}
  		else
  		{
	  		int nXStep;
  			
			dinc1 = nODeltaX << 1;
	  		d = dinc1 - nODeltaY;
  			dinc2 = (nODeltaX - nODeltaY) << 1;
    
  			if (ox1 != cClipped.m_nX0 || oy1 != cClipped.m_nY0) 
    			{
	    			int nClipDeltaX = abs(cClipped.m_nX0 - ox1);
	    			int nClipDeltaY = abs(cClipped.m_nY0 - oy1);
	    			d += ((nClipDeltaX*dinc2) + ((nClipDeltaY-nClipDeltaX)*dinc1));
  			}   
  			if (cClipped.m_nX0 > cClipped.m_nX1) 
	    			nXStep = -2;
  			else
	    			nXStep = 2;
  	
  			if (cClipped.m_nY0 > cClipped.m_nY1) 
    			{
				nXStep = -nXStep;
	    			nOffset = cClipped.m_nX1 * 2 + nModulo * cClipped.m_nY1;
  			} 
    			else 
    			{
	    			nOffset = cClipped.m_nX0 * 2 + nModulo * cClipped.m_nY0;
  			}
			nDelta = nDeltaY;
			nStep  = nXStep;
			nInc   = nModulo;    
  		}
		if (nDrawingMode.getValue() == DrawingMode.DM_COPY || nDrawingMode.getValue() == DrawingMode.DM_OVER)
		{
  			for (int i = 0; i <= nDelta; ++i)
	  		{
				set16 (nOffset, nColor);
    				if (d < 0) 
      				{
 	  				d += dinc1;
  	  			} 
     				else 
      				{
 	  				d += dinc2;
					nOffset += nStep;
    				}
				nOffset += nInc;
	  		}
		}
		else if (nDrawingMode.getValue() == DrawingMode.DM_INVERT)
		{
  			for (int i = 0; i <= nDelta; ++i)
	  		{
				set16 (nOffset, PixelColor.invertRGB16 (get16 (nOffset)));
     
	    			if (d < 0) 
				{
		    			d += dinc1;
  	  			} 
				else 
				{
   					d += dinc2;
					nOffset += nStep;
	    			}
				nOffset += nInc;
  			}
		}
		//Debug.out.println ("drawLineRGB16(" + cDraw + ", " + cClipped + ", " + Integer.toHexString(nColor) + ", " + nDrawingMode + ")");
	}
	private void drawLineRGB32 (PixelRect cDraw, PixelRect cClipped, int nColor, DrawingMode nDrawingMode)
	{
  		int nODeltaX = abs (cDraw.m_nX1 - cDraw.m_nX0);
  		int nODeltaY = abs (cDraw.m_nY1 - cDraw.m_nY0);
  		int ox1 = cDraw.m_nX0;
  		int oy1 = cDraw.m_nY0;
 	 	int nModulo = m_nBytesPerLine;
		int nOffset = 0;
  		int nDeltaX = abs (cClipped.m_nX1 - cClipped.m_nX0);
  		int nDeltaY = abs (cClipped.m_nY1 - cClipped.m_nY0);
		int nStep, nInc, nDelta, d, dinc1, dinc2;
		
		//Debug.out.println ("drawLineRGB16(" + cDraw + ", " + cClipped + ", " + Integer.toHexString(nColor) + ", " + nDrawingMode + ")");

  		if (nODeltaX > nODeltaY)
  		{
  			int nYStep;
  			
			dinc1 = nODeltaY << 2;
  			dinc2 = (nODeltaY - nODeltaX) << 2;
	  		d = dinc1 - nODeltaX;

  			if (ox1 != cClipped.m_nX0 || oy1 != cClipped.m_nY0) 
    			{
	    			int nClipDeltaX = abs(cClipped.m_nX0 - ox1);
	    			int nClipDeltaY = abs(cClipped.m_nY0 - oy1);
	    			d += ((nClipDeltaY*dinc2) + ((nClipDeltaX-nClipDeltaY)*dinc1));
  			}    
  			if (cClipped.m_nY0 > cClipped.m_nY1) 
	    			nYStep = -nModulo;
	  		else
	    			nYStep = nModulo;
  			if (cClipped.m_nX0 > cClipped.m_nX1) 
    			{
	    			nYStep = -nYStep;
	   	 		nOffset = cClipped.m_nX1 * 4 + nModulo * cClipped.m_nY1;
	  		} 
    			else 
    			{
	    			nOffset = cClipped.m_nX0 * 4 + nModulo * cClipped.m_nY0;
  			}
			nDelta = nDeltaX;
			nStep  = nYStep;
			nInc   = 4;
	
  		}
  		else
  		{
	  		int nXStep;
  			
			dinc1 = nODeltaX << 2;
	  		d = dinc1 - nODeltaY;
  			dinc2 = (nODeltaX - nODeltaY) << 2;
    
  			if (ox1 != cClipped.m_nX0 || oy1 != cClipped.m_nY0) 
    			{
	    			int nClipDeltaX = abs(cClipped.m_nX0 - ox1);
	    			int nClipDeltaY = abs(cClipped.m_nY0 - oy1);
	    			d += ((nClipDeltaX*dinc2) + ((nClipDeltaY-nClipDeltaX)*dinc1));
  			}   
  			if (cClipped.m_nX0 > cClipped.m_nX1) 
	    			nXStep = -4;
  			else
	    			nXStep = 4;
  	
  			if (cClipped.m_nY0 > cClipped.m_nY1) 
    			{
				nXStep = -nXStep;
	    			nOffset = cClipped.m_nX1 * 4 + nModulo * cClipped.m_nY1;
  			} 
    			else 
    			{
	    			nOffset = cClipped.m_nX0 * 4 + nModulo * cClipped.m_nY0;
  			}
			nDelta = nDeltaY;
			nStep  = nXStep;
			nInc   = nModulo;    
  		}
		if (nDrawingMode.getValue() == DrawingMode.DM_COPY || nDrawingMode.getValue() == DrawingMode.DM_OVER)
		{
  			for (int i = 0; i <= nDelta; ++i)
	  		{
				set32 (nOffset, nColor);
    				if (d < 0) 
      				{
 	  				d += dinc1;
  	  			} 
     				else 
      				{
 	  				d += dinc2;
					nOffset += nStep;
    				}
				nOffset += nInc;
	  		}
		}
		else if (nDrawingMode.getValue() == DrawingMode.DM_INVERT)
		{
  			for (int i = 0; i <= nDelta; ++i)
	  		{
				set32 (nOffset, PixelColor.invertRGB32 (get32 (nOffset)));
     
	    			if (d < 0) 
				{
		    			d += dinc1;
  	  			} 
				else 
				{
   					d += dinc2;
					nOffset += nStep;
	    			}
				nOffset += nInc;
  			}
		}
		//Debug.out.println ("drawLineRGB16(" + cDraw + ", " + cClipped + ", " + Integer.toHexString(nColor) + ", " + nDrawingMode + ")");
	}
	private void fillRectRGB16 (PixelRect cRect[], int nCount, short nColor, DrawingMode eMode)
	{
		if (eMode.getValue() == DrawingMode.DM_INVERT)
		{
			for (int i = 0; i < nCount; i++)
			{
				for (int y = cRect[i].top (); y <= cRect[i].bottom (); y++)
				{
					int nOffset = y * m_nBytesPerLine + cRect[i].left() * 2;
					for (int x = cRect[i].left(); x <= cRect[i].right(); x++)
					{
						set16 (nOffset, PixelColor.invertRGB16 (get16 (nOffset)));
						nOffset += 2;
					}
				}
			}
		}
		else
		{
			for (int i = 0; i < nCount; i++)
			{
				for (int y = cRect[i].top (); y <= cRect[i].bottom (); y++)
				{
					int nOffset = y * m_nBytesPerLine + cRect[i].left() * 2;
					fill16 (nOffset, cRect[i].width() + 1, nColor);
				}		
			}			
		}
	}
	private void fillRectRGB32 (PixelRect cRect[], int nCount, int nColor, DrawingMode eMode)
	{
	
		if (eMode.getValue() == DrawingMode.DM_INVERT)
		{
			for (int i = 0; i < nCount; i++)
			{
				for (int y = cRect[i].top (); y <= cRect[i].bottom (); y++)
				{
					int nOffset = y * m_nBytesPerLine + cRect[i].left() * 4;
					for (int x = cRect[i].left(); x <= cRect[i].right(); x++)
					{
						set32 (nOffset, PixelColor.invertRGB32 (get32 (nOffset)));
						nOffset += 4;
					}
				}
			}
		}
		else
		{
			for (int i = 0; i < nCount; i++)
			{
				for (int y = cRect[i].top (); y <= cRect[i].bottom (); y++)
				{
					int nOffset = y * m_nBytesPerLine + cRect[i].left() * 4;
					fill32 (nOffset, cRect[i].width() + 1, nColor);
				}		
			}			
		}
	}

	public void bitBlt (PixelRect acOldPos[], PixelRect acNewPos[], int nCount)
	{
	  	int h1,h2;
  		PixelRect cOldPos, cNewPos;
  		int i = 0;
  
		while (i < nCount)
  		{
  			cOldPos = acOldPos[i];
      			cNewPos = acNewPos[i];
      
			if (cOldPos.m_nX1 < cOldPos.m_nX0)
			{
				++i;
			      	continue;
			}
      			if (cOldPos.m_nX0 < 0)
      			{
        			cNewPos.m_nX0-= cOldPos.m_nX0;
        			cOldPos.m_nX0 = 0;
      			}  
      			if (cNewPos.m_nX0 < 0)
      			{
        			cOldPos.m_nX0-= cNewPos.m_nX0;
        			cNewPos.m_nX0 = 0;
     			}
      			if (cOldPos.m_nY0 < 0)
      			{
        			cNewPos.m_nY0-= cOldPos.m_nY0;
        			cOldPos.m_nY0 = 0;
      			}  
      			if (cNewPos.m_nY0 < 0)
      			{
        			cOldPos.m_nY0-= cNewPos.m_nY0;
        			cNewPos.m_nY0 = 0;
      			}
      			if (cOldPos.m_nX1 >= (int)m_nWidth)
      			{
        			cNewPos.m_nX1-= cOldPos.m_nX1 - m_nWidth + 1;
        			cOldPos.m_nX1 = m_nWidth - 1;    
      			}
      			if (cNewPos.m_nX1 >= (int)m_nWidth)
      			{
        			cOldPos.m_nX1-= cNewPos.m_nX1 - m_nWidth + 1;
        			cNewPos.m_nX1 = m_nWidth - 1;
      			}
      			if (cOldPos.m_nY1 >= (int)m_nHeight)
      			{
        			cNewPos.m_nY1-= cOldPos.m_nY1 - m_nHeight + 1;
        			cOldPos.m_nY1 = m_nHeight - 1;    
      			}
      			if (cNewPos.m_nY1 >= (int)m_nHeight)
      			{
        			cOldPos.m_nY1-= cNewPos.m_nY1 - m_nHeight + 1;
        			cNewPos.m_nY1 = m_nHeight - 1;
      			}
      			if (cNewPos.m_nX1 < cNewPos.m_nX0 || cNewPos.m_nY1 < cNewPos.m_nY0 || 
          		    cOldPos.m_nX1 < cOldPos.m_nX0 || cOldPos.m_nY1 < cOldPos.m_nY0)
      			{
        			++i;  
        			continue;
      			}
      
      			h1 = cOldPos.m_nX1 - cOldPos.m_nX0;
      			h2 = cNewPos.m_nX1 - cNewPos.m_nX0;    
      			if (h1 < h2) cNewPos.m_nX1 = cNewPos.m_nX0 + h1;
      			if (h2 < h1) cOldPos.m_nX1 = cOldPos.m_nX0 + h2;
      			h1 = cOldPos.m_nY1 - cOldPos.m_nY0;
      			h2 = cNewPos.m_nY1 - cNewPos.m_nY0;    
      			if (h1 < h2) cNewPos.m_nY1 = cNewPos.m_nY0 + h1;
      			if (h2 < h1) cOldPos.m_nY1 = cOldPos.m_nY0 + h2;
  			++i;
    		}
		if (m_cDisplayDriver != null && m_cDisplayDriver.bitBlt (acOldPos, acNewPos, nCount) == 0)
			return;
		startFrameBufferUpdate ();
 		for (int j = 0; j < nCount; j++)
    		{
      			switch (m_eColorSpace.getValue())
      			{
        			case ColorSpace.CS_CMAP8:
        			case ColorSpace.CS_GRAY8:      
          				bitBlt8 (acOldPos[j], acNewPos[j]);
          				break;
        			case ColorSpace.CS_RGB15:
        			case ColorSpace.CS_RGBA15:
        			case ColorSpace.CS_RGB16:
          				bitBlt16 (acOldPos[j], acNewPos[j]);
          				break;      				       
        			case ColorSpace.CS_RGB32:
          				bitBlt32 (acOldPos[j], acNewPos[j]);
          				break;      				       
				default:
					break;						
      			}
    		}      
		endFrameBufferUpdate ();
  	}    
	private void bitBlt8 (PixelRect cOldPos, PixelRect cNewPos)
	{
  		int y,src,dst;

  		if (cOldPos.top () < cNewPos.top ())
  		{
    			src = ((cNewPos.bottom () - cNewPos.top ()) + cOldPos.top ()) * m_nBytesPerLine + cOldPos.left ();
    			dst = cNewPos.bottom () * m_nBytesPerLine + cNewPos.left ();
    			for (y = cNewPos.bottom (); y >= cNewPos.top (); y--)
    			{
				m_cMemory.move (dst, src, cNewPos.right() - cNewPos.left());
      				//fast_memmove ((void *)dst, (void *)src, cNewPos.right () - cNewPos.left () + 1);
      				//__mmx_memmove ((void *)dst, (void *)src, cNewPos.right () - cNewPos.left () + 1);
      				src-= m_nBytesPerLine;
      				dst-= m_nBytesPerLine;
    			} 
  		}
  		else
  		{
    			src = cOldPos.top () * m_nBytesPerLine + cOldPos.left ();
    			dst = cNewPos.top () * m_nBytesPerLine + cNewPos.left ();
    			for (y = cNewPos.top (); y <= cNewPos.bottom (); y++)
    			{
      				//fast_memmove ((void *)dst, (void *)src, cNewPos.right () - cNewPos.left () + 1);
				m_cMemory.move (dst, src, cNewPos.right() - cNewPos.left());
      				src+= m_nBytesPerLine;
      				dst+= m_nBytesPerLine;
    			} 
  		}   
	}		
	private void bitBlt16 (PixelRect cOldPos, PixelRect cNewPos)
	{
  		int y,src,dst;
		
		//Debug.out.println ("bitBlt16 " + cOldPos + " -> " + cNewPos + ", m_nOffset: " + m_nOffset);

  		if (cOldPos.top () < cNewPos.top ())
  		{
    			src = ((cNewPos.bottom () - cNewPos.top ()) + cOldPos.top ()) * m_nBytesPerLine + cOldPos.left () * 2;
    			dst = cNewPos.bottom () * m_nBytesPerLine + cNewPos.left () * 2;
    			for (y = cNewPos.bottom (); y >= cNewPos.top (); y--)
    			{
					m_cMemory.move (m_nOffset + dst, m_nOffset + src, (cNewPos.right() - cNewPos.left() + 1) * 2);
      				src-= m_nBytesPerLine;
      				dst-= m_nBytesPerLine;
    			} 
  		}
  		else
  		{
    			src = cOldPos.top () * m_nBytesPerLine + cOldPos.left () * 2;
    			dst = cNewPos.top () * m_nBytesPerLine + cNewPos.left () * 2;
    			for (y = cNewPos.top (); y <= cNewPos.bottom (); y++)
    			{
					m_cMemory.move (m_nOffset + dst, m_nOffset + src, (cNewPos.right() - cNewPos.left() + 1) * 2);
      				src+= m_nBytesPerLine;
      				dst+= m_nBytesPerLine;
    			} 
  		}   
	}		
	private void bitBlt32 (PixelRect cOldPos, PixelRect cNewPos)
	{
  		int y,src,dst;
/*		
		Debug.out.println ("bitBlt16 " + cOldPos + " -> " + cNewPos + ", m_nOffset: " + m_nOffset);
*/
  		if (cOldPos.top () < cNewPos.top ())
  		{
    			src = ((cNewPos.bottom () - cNewPos.top ()) + cOldPos.top ()) * m_nBytesPerLine + cOldPos.left () * 4;
    			dst = cNewPos.bottom () * m_nBytesPerLine + cNewPos.left () * 4;
    			for (y = cNewPos.bottom (); y >= cNewPos.top (); y--)
    			{
				m_cMemory.move (m_nOffset + dst, m_nOffset + src, (cNewPos.right() - cNewPos.left() + 1) * 4);
      				src-= m_nBytesPerLine;
      				dst-= m_nBytesPerLine;
    			} 
  		}
  		else
  		{
    			src = cOldPos.top () * m_nBytesPerLine + cOldPos.left () * 4;
    			dst = cNewPos.top () * m_nBytesPerLine + cNewPos.left () * 4;
    			for (y = cNewPos.top (); y <= cNewPos.bottom (); y++)
    			{
				m_cMemory.move (m_nOffset + dst, m_nOffset + src, (cNewPos.right() - cNewPos.left() + 1) * 4);
      				src+= m_nBytesPerLine;
      				dst+= m_nBytesPerLine;
    			} 
  		}   
	}		

	public void drawBitmap (WBitmap cBitmap, PixelRect cDst, PixelRect cSrc, PixelRect cClp, DrawingMode nMode)
	{		
		int yscale, xscale, xstart, ystart;
		PixelRect cDraw;
		PixelRect cTmpSrc;

		Debug.out.println ("WBitmap::drawBitmap(" + cBitmap + "," + cDst + "," + cSrc + "," + cClp + "," + nMode + ")");
		Debug.out.println ("this: (" + m_nWidth + ", " + m_nHeight + ", " + m_nBytesPerLine + ", " + m_eColorSpace + ")");

		if (!cSrc.isValid())
			return;		
		if (!cDst.isValid())
			return;		
		
		cDraw = new PixelRect (cDst);
		cDraw.clip (m_cBounds);
  		cDraw.clip (cClp);
  
  		if (!cDraw.isValid())
    			return;
		
		cTmpSrc = new PixelRect (cSrc);
		cTmpSrc.clip (cBitmap.getBounds());
		if (!cTmpSrc.isValid())
			return;
		
		boolean bStretch = false;
		if (cTmpSrc.width() == 0 || cDst.width() == 0) {
			xscale = 1 << 16;
		} else {
			xscale = (cTmpSrc.width () << 16) / cDst.width ();
			if (cTmpSrc.width() != cDst.width()) bStretch = true;
		}
		if (cTmpSrc.height() == 0 || cDst.height() == 0) {
			yscale = 1 << 16;
		} else {
			yscale = (cTmpSrc.height() << 16) / cDst.height ();
			if (cTmpSrc.height() != cDst.height()) bStretch = true;
		}
  
		xstart = (cDraw.left() - cDst.left() + cTmpSrc.left()) * xscale;
		ystart = (cDraw.top() - cDst.top() + cTmpSrc.top()) * yscale;

		startFrameBufferUpdate ();
		switch (m_eColorSpace.getValue())
  		{
    			case ColorSpace.CS_CMAP8:
    			{
      				break;  
    			}            
		    	case ColorSpace.CS_RGB16:
    			{
				drawBitmapRGB16 (cBitmap, cDraw, xscale, yscale, xstart, ystart, nMode);
				break;
    			}      
		    	case ColorSpace.CS_RGB32:
    			{
				drawBitmapRGB32 (cBitmap, cDraw, xscale, yscale, xstart, ystart, nMode);
				break;
    			}      
			default:
				break;
  		}      
		endFrameBufferUpdate ();
	}

	protected void drawBitmapRGB16 (WBitmap cBitmap, PixelRect cDraw, 
  		int xscale, int yscale, int xstart, int ystart, DrawingMode nMode)
	{
		int nStartDst, nSrc, nDst, x, y, xcount, ycount;
		boolean bStretch = (xscale != 65536);
/*		
		Debug.out.println ("drawBitmapRGB16(" + cBitmap + "," + cDraw + "," + xscale + "," + yscale + "," + xstart +
			"," + ystart + "," + nMode + ")");
*/											
  		nStartDst = cDraw.m_nY0 * bytesPerLine() + cDraw.m_nX0 + cDraw.m_nX0;
  		for (y = cDraw.m_nY0, ycount = ystart; y <= cDraw.m_nY1; y++, ycount+= yscale)
  		{
    			nDst = nStartDst;
    			nSrc = (ycount >> 16) * cBitmap.bytesPerLine();
    			switch (nMode.getValue())
    			{
      				case DrawingMode.DM_COPY:
      				{
					switch (cBitmap.m_eColorSpace.getValue())
					{					
						case ColorSpace.CS_CMAP8:
						{
							byte c;
		        				for (x = cDraw.m_nX0, xcount = xstart; x <= cDraw.m_nX1; x++, xcount+= xscale)
        						{
								c = cBitmap.get8 (nSrc + (xcount >> 16));
								set16 (nDst, cBitmap.m_eColorSpace.CMAP8toRGB16 (c));
								nDst += 2;
        						}          
        						break;
						}
						case ColorSpace.CS_RGB16:
						{
							if (bStretch)
							{							
								short c;								
		        				for (x = cDraw.m_nX0, xcount = xstart; x <= cDraw.m_nX1; x++, xcount+= xscale)
       							{
									c = cBitmap.get16 (nSrc + (xcount >> 15));
									set16 (nDst, c);
									nDst += 2;
	       						}          
							}
							else
							{
								nSrc += xstart >> 15;
								WBitmapMemory memMap = cBitmap.castMemory();
								m_cMemory.copyFromMemory (memMap.m_cMemory, memMap.m_nOffset + nSrc, m_nOffset + nDst, (cDraw.m_nX1 - cDraw.m_nX0 + 1) * 2);
							}
       						break;
						}
						case ColorSpace.CS_RGB32:
						{
							int c;
		        				for (x = cDraw.m_nX0, xcount = xstart; x <= cDraw.m_nX1; x++, xcount+= xscale)
        						{
								c = cBitmap.get32 (nSrc + (xcount >> 14));
								set16 (nDst, PixelColor.RGB32toRGB16 (c));
								nDst += 2;
        						}          
        						break;
						}
					}		
					break;					
      				}       
      				case DrawingMode.DM_OVER:
      				{
					switch (cBitmap.m_eColorSpace.getValue())
					{
						case ColorSpace.CS_CMAP8:
						{
							byte c;
        						for (x = cDraw.m_nX0, xcount = xstart; x <= cDraw.m_nX1; x++, xcount+= xscale)
	        					{      
								c = cBitmap.get8 (nSrc + (xcount >> 16));
								if (c != (byte)PixelColor.TRANSPARENT_CMAP8)
									set16 (nDst, cBitmap.m_eColorSpace.CMAP8toRGB16 (c));
								nDst += 2;
        						}        
		        				break;
						}
						case ColorSpace.CS_RGB16:
						{
							short c;
        						for (x = cDraw.m_nX0, xcount = xstart; x <= cDraw.m_nX1; x++, xcount+= xscale)
	        					{      
								c = cBitmap.get16 (nSrc + (xcount >> 15));
								if (c != (short)PixelColor.TRANSPARENT_RGB15)
									set16 (nDst, c);
								nDst += 2;
        						}        
		        				break;
						}
						case ColorSpace.CS_RGB32:
						{
							int c;
        						for (x = cDraw.m_nX0, xcount = xstart; x <= cDraw.m_nX1; x++, xcount+= xscale)
	        					{      
								c = cBitmap.get32 (nSrc + (xcount >> 14));
								if (c != PixelColor.TRANSPARENT_RGB32)
									set16 (nDst, PixelColor.RGB32toRGB16 (c));
								nDst += 2;
        						}        
		        				break;
						}
					}
					break;						
      				}       
    			}       
    			nStartDst += bytesPerLine();
  		}    
	}		

	protected void drawBitmapRGB32 (WBitmap cBitmap, PixelRect cDraw, 
  		int xscale, int yscale, int xstart, int ystart, DrawingMode nMode)
	{
		int nStartDst, nSrc, nDst, x, y, xcount, ycount;
		boolean bStretch = (xscale != 65536);
/*		
		Debug.out.println ("drawBitmapRGB16(" + cBitmap + "," + cDraw + "," + xscale + "," + yscale + "," + xstart +
			"," + ystart + "," + nMode + ")");
*/											
  		nStartDst = cDraw.m_nY0 * bytesPerLine() + cDraw.m_nX0 * 4;
  		for (y = cDraw.m_nY0, ycount = ystart; y <= cDraw.m_nY1; y++, ycount+= yscale)
  		{
    			nDst = nStartDst;
    			nSrc = (ycount >> 16) * cBitmap.bytesPerLine();
    			switch (nMode.getValue())
    			{
      				case DrawingMode.DM_COPY:
      				{
					switch (cBitmap.m_eColorSpace.getValue())
					{					
						case ColorSpace.CS_CMAP8:
						{
							byte c;
		        				for (x = cDraw.m_nX0, xcount = xstart; x <= cDraw.m_nX1; x++, xcount+= xscale)
        						{
								c = cBitmap.get8 (nSrc + (xcount >> 16));
								set32 (nDst, cBitmap.m_eColorSpace.CMAP8toRGB32 (c));
								nDst += 4;
        						}          
        						break;
						}
						case ColorSpace.CS_RGB16:
						{
							short c;
		        				for (x = cDraw.m_nX0, xcount = xstart; x <= cDraw.m_nX1; x++, xcount+= xscale)
        						{
								c = cBitmap.get16 (nSrc + (xcount >> 15));
								set32 (nDst, PixelColor.RGB16toRGB32 (c));
								nDst += 4;
        						}          
        						break;
						}
						case ColorSpace.CS_RGB32:
						{
							if (bStretch)
							{
								int c;
		        				for (x = cDraw.m_nX0, xcount = xstart; x <= cDraw.m_nX1; x++, xcount+= xscale)
        						{
									c = cBitmap.get32 (nSrc + (xcount >> 14));
									set32 (nDst, c);
									nDst += 4;
        						}          
							}
							else
							{
								nSrc += xstart >> 14;
								WBitmapMemory mem = cBitmap.castMemory();
								m_cMemory.copyFromMemory (mem.m_cMemory, mem.m_nOffset + nSrc, m_nOffset + nDst, (cDraw.m_nX1 - cDraw.m_nX0 + 1) * 4);
							}
        					break;
						}
					}		
					break;					
      				}       
      				case DrawingMode.DM_OVER:
      				{
					switch (cBitmap.m_eColorSpace.getValue())
					{
						case ColorSpace.CS_CMAP8:
						{
							byte c;
        						for (x = cDraw.m_nX0, xcount = xstart; x <= cDraw.m_nX1; x++, xcount+= xscale)
	        					{      
								c = cBitmap.get8 (nSrc + (xcount >> 16));
								if (c != (byte)PixelColor.TRANSPARENT_CMAP8)
									set32 (nDst, cBitmap.m_eColorSpace.CMAP8toRGB32 (c));
								nDst += 4;
        						}        
		        				break;
						}
						case ColorSpace.CS_RGB16:
						{
							short c;
        						for (x = cDraw.m_nX0, xcount = xstart; x <= cDraw.m_nX1; x++, xcount+= xscale)
	        					{      
								c = cBitmap.get16 (nSrc + (xcount >> 15));
								if (c != (short)PixelColor.TRANSPARENT_RGB15)
									set32 (nDst, PixelColor.RGB16toRGB32 (c));
								nDst += 4;
        						}        
		        				break;
						}
						case ColorSpace.CS_RGB32:
						{
							int c;
        						for (x = cDraw.m_nX0, xcount = xstart; x <= cDraw.m_nX1; x++, xcount+= xscale)
	        					{      
								c = cBitmap.get32 (nSrc + (xcount >> 14));
								if (c != PixelColor.TRANSPARENT_RGB32)
								    set32 (nDst, c);
								nDst += 4;
        						}        
		        				break;
						}
					}
					break;						
      				}       
    			}       
    			nStartDst += bytesPerLine();
  		}    
	}		

	public void renderGlyph8 (Glyph cGlyph, int X, int Y, PixelRect cClip, int anPalette[]) 
	{
  		int x, y, nSrc, nDst;
  		/*ClippingRect cBounds = ClippingRect (pcGlyph->m_cBounds + WPoint (X, Y));*/
		PixelRect cBounds = new PixelRect (cGlyph.m_cBounds);
		cBounds.m_nX0 += X;
		cBounds.m_nY0 += Y;
  		cBounds.m_nX1 += X - 1;
  		cBounds.m_nY1 += Y - 1;
		
		PixelRect cDraw = new PixelRect (cBounds);
		cDraw.clip (cClip);
  
  		if (cDraw.isValid () == false)
    			return;
  
  		for (y = cDraw.top (); y <= cDraw.bottom (); y++)
  		{
    			nDst = y * m_nBytesPerLine + cDraw.left ();
    			nSrc = (cDraw.left () - cBounds.left ()) + (y - cBounds.top ()) * cGlyph.m_nBytesPerLine;
    
    			for (x = cDraw.left (); x <= cDraw.right (); x++)
    			{
				int c;
      				if ((c = (int)cGlyph.m_anRaster[nSrc]) != 0)
				{
					set8 (nDst, (byte)anPalette[c & 0xff]);
				}					
      				nDst++;
      				nSrc++;
    			}
  		}
	}
	void renderGlyph16 (Glyph cGlyph, int X, int Y, PixelRect cClip, int anPalette[]) 
	{
  		int x, y, nSrc, nDst, nSrcModulo, nDstModulo;
  		/*ClippingRect cBounds = ClippingRect (pcGlyph->m_cBounds + WPoint (X, Y));*/
		PixelRect cBounds = new PixelRect (cGlyph.m_cBounds);
		cBounds.m_nX0 += X;
		cBounds.m_nY0 += Y;
  		cBounds.m_nX1 += X - 1;
  		cBounds.m_nY1 += Y - 1;
		
		PixelRect cDraw = new PixelRect (cBounds);
		cDraw.clip (cClip);
  
  		if (cDraw.isValid () == false)
    			return;

  		nDst = cDraw.top() * m_nBytesPerLine + cDraw.left () + cDraw.left ();
  		nSrc = (cDraw.left () - cBounds.left ()) + (cDraw.top() - cBounds.top ()) * cGlyph.m_nBytesPerLine;

  		nDstModulo = m_nBytesPerLine  - cDraw.width () - cDraw.width() - 2;      
  		nSrcModulo = cGlyph.m_nBytesPerLine - cDraw.width() - 1;
    
  		for (y = cDraw.top (); y <= cDraw.bottom (); y++)
  		{
    			for (x = cDraw.left (); x <= cDraw.right (); x++)
    			{
				int c;
      				if ((c = (int)cGlyph.m_anRaster[nSrc]) != 0)
				{
					set16 (nDst, (short)anPalette[c & 0xff]);
				}					
      				nDst += 2;
      				nSrc++;
    			}
    			nDst+= nDstModulo;
    			nSrc+= nSrcModulo;
  		}
	}	
	void renderGlyph32 (Glyph cGlyph, int X, int Y, PixelRect cClip, int anPalette[]) 
	{
  		int x, y, nSrc, nDst, nSrcModulo, nDstModulo;
  		/*ClippingRect cBounds = ClippingRect (pcGlyph->m_cBounds + WPoint (X, Y));*/
		PixelRect cBounds = new PixelRect (cGlyph.m_cBounds);
		cBounds.m_nX0 += X;
		cBounds.m_nY0 += Y;
  		cBounds.m_nX1 += X - 1;
  		cBounds.m_nY1 += Y - 1;
		
		PixelRect cDraw = new PixelRect (cBounds);
		cDraw.clip (cClip);
  
  		if (cDraw.isValid () == false)
    			return;

  		nDst = cDraw.top() * m_nBytesPerLine + cDraw.left () * 4;
  		nSrc = (cDraw.left () - cBounds.left ()) + (cDraw.top() - cBounds.top ()) * cGlyph.m_nBytesPerLine;

  		nDstModulo = m_nBytesPerLine  - (cDraw.width () * 4) - 4;      
  		nSrcModulo = cGlyph.m_nBytesPerLine - cDraw.width() - 1;
    
  		for (y = cDraw.top (); y <= cDraw.bottom (); y++)
  		{
    			for (x = cDraw.left (); x <= cDraw.right (); x++)
    			{
				int c;
      				if ((c = (int)cGlyph.m_anRaster[nSrc]) != 0)
				{
					set32 (nDst, (int)anPalette[c & 0xff]);
				}					
      				nDst += 4;
      				nSrc++;
    			}
    			nDst+= nDstModulo;
    			nSrc+= nSrcModulo;
  		}
	}	
	public void renderGlyph (Glyph cGlyph, int x, int y, PixelRect cTmpClip, int anPalette[]) 
	{
		PixelRect cClip = new PixelRect (cTmpClip);
	
		cClip.clip (m_cBounds);
  		switch (m_eColorSpace.getValue())
  		{
    			case ColorSpace.CS_CMAP8:
    			case ColorSpace.CS_GRAY8:      
      				renderGlyph8 (cGlyph, x, y, cClip, anPalette);
      				break;
    			case ColorSpace.CS_RGB15:
    			case ColorSpace.CS_RGBA15:
    			case ColorSpace.CS_RGB16:      
      				renderGlyph16 (cGlyph, x, y, cClip, anPalette);
      				break;
    			case ColorSpace.CS_RGB32:      
      				renderGlyph32 (cGlyph, x, y, cClip, anPalette);
      				break;
			default:
				break;			
 		}
	}  

	public void drawCloneMap (WBitmap cBitmap, PixelRect cDst, PixelRect cClp)
	{		
		int scale, xstart, ystart;
		PixelRect cDraw;
		PixelRect cTmpSrc;

		if (!cDst.isValid())
			return;		
		
		cDraw = new PixelRect (cDst);
		cDraw.clip (m_cBounds);
  		cDraw.clip (cClp);
  
  		if (!cDraw.isValid())
    			return;

		cTmpSrc = new PixelRect (cBitmap.getBounds());
		if (!cTmpSrc.isValid())
			return;

		if (!isCloneMap(cBitmap)) {
			Debug.out.println("!!!!!!! is not clone !!!!!!!!!!");
			return;
		}

		if (cTmpSrc.width() < cDraw.width())
			cDraw.m_nX1 = cDraw.m_nX0 + cTmpSrc.width ();
		if (cTmpSrc.height() < cDraw.height())
			cDraw.m_nY1 = cDraw.m_nY0 + cTmpSrc.height ();			

		scale = (1<<16);
		xstart = cDraw.left() - cDst.left() + cTmpSrc.left();
		ystart = cDraw.top() - cDst.top() + cTmpSrc.top(); 

		startFrameBufferUpdate ();
		switch (m_eColorSpace.getValue())
  		{
    			case ColorSpace.CS_CMAP8:
    			{
      				break;  
    			}            
		    	case ColorSpace.CS_RGB16:
    			{
				drawCloneMapRGB16 (cBitmap, cDraw, xstart, ystart);
				break;
    			}      
		    	case ColorSpace.CS_RGB32:
    			{
				drawBitmapRGB32 (cBitmap, cDraw, scale, scale, xstart*scale, ystart*scale, DrawingMode.COPY);
				break;
    			}      
			default:
				break;
  		}      
		endFrameBufferUpdate ();
	}

	private void drawCloneMapRGB16 (WBitmap cBitmap, PixelRect cDraw, int xstart, int ystart)
	{
		int nStartDst, nSrc, nDst, x, y, xcount, ycount;

		//Debug.out.println ("drawCloneMapRGB16(" + cBitmap + "," + cDraw + "," + xstart + "," + ystart + ")");
		WBitmapMemory mem = cBitmap.castMemory();

		nStartDst = cDraw.m_nY0 * bytesPerLine() + cDraw.m_nX0 + cDraw.m_nX0;
		for (y = cDraw.m_nY0, ycount = ystart; y <= cDraw.m_nY1; y++, ycount++)
		{
			nDst = nStartDst;
			nSrc = ycount * cBitmap.bytesPerLine();
			nSrc += xstart;
			m_cMemory.copyFromMemory (mem.m_cMemory, mem.m_nOffset + nSrc, m_nOffset + nDst, (cDraw.m_nX1 - cDraw.m_nX0 + 1) * 2);
			nStartDst += bytesPerLine();
		}    
	}		

	public String toString ()
	{
		return new String ("WBitmap(" + m_nWidth + "," + m_nHeight + "," + m_nBytesPerLine + "," + m_eColorSpace + ")");
	}
}
