/********************************************************************************
 * RGB 16 Bitmap
 * Copyright 2002- Christian Wawersich
 *******************************************************************************/

package jx.wm.bitmap;

import jx.zero.*;
import jx.devices.fb.*;
import jx.wm.*;

/**
 * RGB 16 Bitmap
 * @author Christian Wawersich
 */
public class WBitmapRGB16Memory extends WBitmapMemory
{
	void init (int nWidth, int nHeight, int nBytesPerLine) {
		Debug.out.println ("WBitmap::init(" + nWidth + ", " + nHeight + ", " + nBytesPerLine + ")");

		m_cDisplayDriver = null;

		m_nBytesPerLine = nWidth * 2;
		if (nBytesPerLine != -1)
		{
			if (m_nBytesPerLine > nBytesPerLine)
			{
				m_nBytesPerLine = -1;
				return;			
			}
			m_nBytesPerLine = nBytesPerLine;			
  		}

		m_nSize   = nHeight * m_nBytesPerLine;
		m_cMemory = allocMemory(m_nSize);
		m_nOffset = 0;
  		m_eColorSpace.setValue (ColorSpace.CS_RGB16);
  		m_nWidth = nWidth;
  		m_nHeight = nHeight;
		m_cBounds = new PixelRect (0, 0, m_nWidth - 1, m_nHeight - 1);
		m_bIsVideoMemory = false;
	}

	public WBitmapRGB16Memory ()
	{
		m_eColorSpace.setValue (ColorSpace.CS_RGB16);
		m_nWidth = m_nHeight = m_nBytesPerLine = m_nOffset = 0;
		m_cMemory = null;
		m_cBounds = null;
		m_cDisplayDriver = null;
		m_bIsVideoMemory = false;
	}

	public WBitmapRGB16Memory (int nWidth, int nHeight, int nBytesPerLine)
	{
		init (nWidth, nHeight, nBytesPerLine);
	}

	public WBitmapRGB16Memory (int nWidth, int nHeight)
	{
		init (nWidth, nHeight, -1);
	}

	public WBitmapRGB16Memory (FramebufferDevice cDisplayDriver, int nWidth, int nHeight, int nBytesPerLine, Memory cMemory, int nOffset)
	{
		Debug.out.println ("WBitmap::init(" + nWidth + ", " + nHeight + ", " + nBytesPerLine + ")");
		m_cDisplayDriver = cDisplayDriver;
		m_nWidth = nWidth;
		m_nHeight = nHeight;
		m_eColorSpace.setValue (ColorSpace.CS_RGB16);
		m_nBytesPerLine = nBytesPerLine;
		m_cMemory = cMemory;
		m_nOffset = nOffset;
		m_nSize   = nHeight * m_nBytesPerLine;
		//m_nSize = cMemory.size ();
		if (m_nSize > cMemory.size()) {
			throw new RuntimeException("memory too small for bitmap");
                }
		m_cBounds = new PixelRect (0, 0, m_nWidth - 1, m_nHeight - 1);
		m_bIsVideoMemory = true;
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
		drawLineRGB16 (cDraw, cClipped, cColor.toRGB16(), nDrawingMode);
		endFrameBufferUpdate ();
	}

	public void drawRect(PixelRect cRect, PixelColor cColor, DrawingMode nMode)
	{
		PixelRect clip = new PixelRect(cRect);
		PixelRect tmp  = new PixelRect(cRect);
		tmp.m_nX1 = cRect.m_nX0; 
		drawLine(tmp, clip, cColor, nMode);
		tmp.setTo(cRect);
		tmp.m_nY1 = cRect.m_nY0;
		drawLine(tmp, clip, cColor, nMode);
		tmp.setTo(cRect);
		tmp.m_nY0 = cRect.m_nY1;
		drawLine(tmp, clip, cColor, nMode);
		tmp.setTo(cRect);
		tmp.m_nX0 = cRect.m_nX1;
		drawLine(tmp, clip, cColor, nMode);
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
		drawLineRGB16 (cDraw, cClipped, cColor.toRGB16(), nDrawingMode);
		endFrameBufferUpdate ();
	}

	public void fillRect (PixelRect cRect[], int nCount, PixelColor cColor, DrawingMode nMode)
	{
		if (m_cDisplayDriver != null && m_cDisplayDriver.fillRect (cRect, nCount, cColor, nMode) == 0)
			return;
			
		startFrameBufferUpdate (); 			
		fillRectRGB16 (cRect, nCount, cColor.toRGB16(), nMode);
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

		if (nDrawingMode.isSet(DrawingMode.DM_COPY) || nDrawingMode.isSet(DrawingMode.DM_OVER))
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
		else if (nDrawingMode.isSet(DrawingMode.DM_INVERT))
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
	}

	private void fillRectRGB16 (PixelRect cRect[], int nCount, short nColor, DrawingMode eMode)
	{
		if (eMode.isSet(DrawingMode.DM_INVERT))
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
			bitBlt16 (acOldPos[j], acNewPos[j]);
    		}      
		endFrameBufferUpdate ();
  	}    

	private void bitBlt16 (PixelRect cOldPos, PixelRect cNewPos)
	{
  		int y,src,dst;
		
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

	public void drawBitmap (WBitmap cBitmap, PixelRect cDst, PixelRect cSrc, PixelRect cClp, DrawingMode nMode)
	{		
		int yscale, xscale, xstart, ystart;
		PixelRect cDraw;
		PixelRect cTmpSrc;

		if (!cSrc.isValid()) {
			return;		
		}
		if (!cDst.isValid()) {
			return;		
		}

		cDraw = new PixelRect (cDst);
		cDraw.clip (m_cBounds);
		cDraw.clip (cClp);

		if (!cDraw.isValid()) {
			return;
		}

		cTmpSrc = new PixelRect (cSrc);
		cTmpSrc.clip (cBitmap.getBounds());
		if (!cTmpSrc.isValid()) {
			return;
		}

		if (cTmpSrc.width() < cDraw.width())
			cDraw.m_nX1 = cDraw.m_nX0 + cTmpSrc.width ();
		if (cTmpSrc.height() < cDraw.height())
			cDraw.m_nY1 = cDraw.m_nY0 + cTmpSrc.height ();

		boolean bStretch = false;
		if (nMode.isScaleable()) {
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
		} else {
			xscale = 1 << 16;
			yscale = 1 << 16;
		}

		int xs = cDraw.left() - cDst.left() + cTmpSrc.left();
		int ys = cDraw.top() - cDst.top() + cTmpSrc.top();

		xstart = xs * xscale;
		ystart = ys * yscale;

		startFrameBufferUpdate ();
		try {
			if (cBitmap.m_eColorSpace.getValue()==ColorSpace.CS_CMAP8) {
/*
				if (nMode.isSet(DrawingMode.DM_COPY) && (!bStretch)) {
					drawBitmapCMAP8Fast(cBitmap, cDraw, xs, ys);
				} else {
*/
					drawBitmapCMAP8(cBitmap, cDraw, xscale, yscale, xstart, ystart, nMode);
//				} 
			} else if (cBitmap.m_eColorSpace.getValue()==ColorSpace.CS_RGB16) {
				drawBitmapRGB16(cBitmap, cDraw, xscale, yscale, xstart, ystart, nMode); 
			} else if (cBitmap.m_eColorSpace.getValue()==ColorSpace.CS_RGB32) {
				drawBitmapRGB32(cBitmap, cDraw, xscale, yscale, xstart, ystart, nMode); 
			}
		} catch (RuntimeException ex) {
			Debug.out.println("exception in drawBitmap");
			Debug.out.println("cSrc "+cSrc+cTmpSrc+" cDst "+cDst+" m_cBounds "+m_cBounds+" cDraw "+cDraw);
		}
		endFrameBufferUpdate ();
	}

	private void drawBitmapCMAP8Fast (WBitmap cBitmap, PixelRect cDraw, int xstart, int ystart)
	{
		int nStartDst, nSrc, nDst, x1, x2, y, c;
		byte c1;

		ColorSpace cColor = cBitmap.m_eColorSpace;
		int nLine1 = bytesPerLine();
		int nLine2 = cBitmap.bytesPerLine();

		nStartDst = (cDraw.m_nY0 * nLine1) + cDraw.m_nX0 * 2;

		int h = (cDraw.m_nY1 - cDraw.m_nY0); //+ 1;
		int w = (cDraw.m_nX1 - cDraw.m_nX0); //+ 1;

		int w1 = w / 4;
		int w2 = w % 4;

		if (w>0 && ((nStartDst%4)!=0||(xstart%4)!=0)) {
			w1--;
			w2+=4;
		}

		for (y = 0; y < h; y++)
		{
			nDst = nStartDst;
			nSrc = ((y+ystart) * nLine2) + xstart;

			for (x2 = 0; ((x2 < w2) && ((nDst%4)!=0||(nSrc%4)!=0)); x2++)
			{
				c1 = cBitmap.get8 (nSrc);
				nSrc++;
				set16 (nDst, cColor.CMAP8toRGB16 (c1));
				nDst += 2;
			}          
			for (x1 = 0; x1 < w1; x1++)
			{
				c  = cBitmap.get32(nSrc);
				set32(nDst, (((cColor.CMAP8toRGB16((byte)((c>>>8) & 0xff)) & 0xffff) << 16 ) 
							| (cColor.CMAP8toRGB16 ((byte)(c & 0xff)) & 0xffff)));
				set32(nDst+4, (((cColor.CMAP8toRGB16((byte)((c >>> 24) & 0xff)) & 0xffff) << 16 ) 
							| (cColor.CMAP8toRGB16 ((byte)((c >>> 16) & 0xff)) & 0xffff)));
				nSrc += 4;
				nDst += 8;
			}          
			for (;x2<w2;x2++)
			{
				c1 = cBitmap.get8 (nSrc);
				nSrc++;
				set16 (nDst, cColor.CMAP8toRGB16 (c1));
				nDst += 2;
			}          
			nStartDst += nLine1;
		}    
	}       

	protected void drawBitmapCMAP8 (WBitmap cBitmap, PixelRect cDraw, 
  		int xscale, int yscale, int xstart, int ystart, DrawingMode nMode)
	{
		int nStartDst, nSrc, nDst, x, y, xcount, ycount;
		ColorSpace cColor = cBitmap.m_eColorSpace;
		int nLine2 = cBitmap.bytesPerLine();
	
		nStartDst = cDraw.m_nY0 * m_nBytesPerLine + cDraw.m_nX0 * 2;
		switch (nMode.getValue())
		{
			case DrawingMode.DM_COPY:
				{
					for (y = cDraw.m_nY0, ycount = ystart; y <= cDraw.m_nY1; y++, ycount+= yscale)
					{
						nDst = nStartDst;
						nSrc = (ycount >> 16) * nLine2;
						byte c;
						for (x = cDraw.m_nX0, xcount = xstart; x <= cDraw.m_nX1; x++, xcount+= xscale)
						{
							c = cBitmap.get8 (nSrc + (xcount >> 16));
							set16 (nDst, cColor.CMAP8toRGB16 (c));
							nDst += 2;
						}          
						nStartDst += m_nBytesPerLine;
					}    
					break;
				}       
			case DrawingMode.DM_OVER:
				{
					for (y = cDraw.m_nY0, ycount = ystart; y <= cDraw.m_nY1; y++, ycount+= yscale)
					{
						nDst = nStartDst;
						nSrc = (ycount >> 16) * nLine2;
						byte c;
						for (x = cDraw.m_nX0, xcount = xstart; x <= cDraw.m_nX1; x++, xcount+= xscale)
						{      
							c = cBitmap.get8 (nSrc + (xcount >> 16));
							if (c != (byte)PixelColor.TRANSPARENT_CMAP8)
								set16 (nDst, cColor.CMAP8toRGB16 (c));
							nDst += 2;
						}        
						nStartDst += m_nBytesPerLine;
					}    
				}
		}
	}	

	protected void drawBitmapRGB16 (WBitmap cBitmap, PixelRect cDraw, 
  		int xscale, int yscale, int xstart, int ystart, DrawingMode nMode)
	{
		int nStartDst, nSrc, nDst, x, y, xcount, ycount;
		boolean bStretch = (xscale != 65536);

		nStartDst = cDraw.m_nY0 * bytesPerLine() + cDraw.m_nX0 + cDraw.m_nX0;

		switch (nMode.getValue())
		{
			case DrawingMode.DM_COPY:
				{
					if (bStretch) {
						for (y = cDraw.m_nY0, ycount = ystart; y <= cDraw.m_nY1; y++, ycount+= yscale)
						{
							nDst = nStartDst;
							nSrc = (ycount >> 16) * cBitmap.bytesPerLine();
							short c;
							for (x = cDraw.m_nX0, xcount = xstart; x <= cDraw.m_nX1; x++, xcount+= xscale)
							{
								c = cBitmap.get16 (nSrc + (xcount >> 15));
								set16 (nDst, c);
								nDst += 2;
							}          
							nStartDst += bytesPerLine();
						}
					} 
					else
					{
						int nLen = (cDraw.m_nX1 - cDraw.m_nX0 + 1) * 2;
						WBitmapMemory memMap = cBitmap.castMemory();
						int line = cBitmap.bytesPerLine();
						int line2 = m_nBytesPerLine;
						nStartDst += m_nOffset;
						boolean flag = true;
						for (y = cDraw.m_nY0, ycount = ystart; y <= cDraw.m_nY1; y++, ycount+= yscale)
						{
							nSrc = (ycount >> 16) * line + (xstart >> 15) + memMap.m_nOffset;
							m_cMemory.copyFromMemory(memMap.m_cMemory,nSrc,nStartDst,nLen);
							nStartDst += bytesPerLine();
						}
					}       
					break;
				}
			case DrawingMode.DM_OVER:
				{
					for (y = cDraw.m_nY0, ycount = ystart; y <= cDraw.m_nY1; y++, ycount+= yscale)
					{
						nDst = nStartDst;
						nSrc = (ycount >> 16) * cBitmap.bytesPerLine();
						short c;
						for (x = cDraw.m_nX0, xcount = xstart; x <= cDraw.m_nX1; x++, xcount+= xscale)
						{      
							c = cBitmap.get16 (nSrc + (xcount >> 15));
							if (c != (short)PixelColor.TRANSPARENT_RGB15)
								set16 (nDst, c);
							nDst += 2;
						}        
						nStartDst += bytesPerLine();
					}
					break;
				}
		}       
	}

	protected void drawBitmapRGB32 (WBitmap cBitmap, PixelRect cDraw, 
  		int xscale, int yscale, int xstart, int ystart, DrawingMode nMode)
	{
		int nStartDst, nSrc, nDst, x, y, xcount, ycount;
		boolean bStretch = (xscale != 65536);
		nStartDst = cDraw.m_nY0 * bytesPerLine() + cDraw.m_nX0 * 4;
		for (y = cDraw.m_nY0, ycount = ystart; y <= cDraw.m_nY1; y++, ycount+= yscale)
		{
			nDst = nStartDst;
			nSrc = (ycount >> 16) * cBitmap.bytesPerLine();
			switch (nMode.getValue())
			{
				case DrawingMode.DM_COPY:
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
				case DrawingMode.DM_OVER:
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
			nStartDst += bytesPerLine();
		}    
	}		

	private void renderGlyph16 (Glyph cGlyph, int X, int Y, PixelRect cClip, int anPalette[]) 
	{
  		int x, y, nSrc, nDst, nSrcModulo, nDstModulo;
  		/*ClippingRect cBounds = ClippingRect (pcGlyph->m_cBounds + WPoint (X, Y));*/
/*
		PixelRect cBounds = new PixelRect (cGlyph.m_cBounds);
		cBounds.m_nX0 += X;
		cBounds.m_nY0 += Y;
  		cBounds.m_nX1 += X - 1;
  		cBounds.m_nY1 += Y - 1;
		PixelRect cDraw = new PixelRect (cBounds);
*/
	        PixelRect cBounds = cGlyph.m_cBounds; 
		int left = cBounds.m_nX0+X;
		int top  = cBounds.m_nY0+Y;
		PixelRect cDraw   = new PixelRect(left,top,cBounds.m_nX1+X-1,cBounds.m_nY1+Y-1);
		cDraw.clip (cClip);
  
  		if (cDraw.isValid () == false)
    			return;

  		nDst = cDraw.top() * m_nBytesPerLine + cDraw.left () + cDraw.left ();
  		//nSrc = (cDraw.left () - cBounds.left ()) + (cDraw.top() - cBounds.top ()) * cGlyph.m_nBytesPerLine;
  		nSrc = (cDraw.left()-left) + (cDraw.top()-top) * cGlyph.m_nBytesPerLine;

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

	public void renderGlyph (Glyph cGlyph, int x, int y, PixelRect cTmpClip, int anPalette[]) 
	{
		PixelRect cClip = new PixelRect (cTmpClip);
		cClip.clip (m_cBounds);
		renderGlyph16 (cGlyph, x, y, cClip, anPalette);
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
		drawCloneMapRGB16 (cBitmap, cDraw, xstart, ystart);
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
		return new String ("WBitmapRGB16Memory(" + m_nWidth + "," + m_nHeight + "," + m_nBytesPerLine + "," + m_eColorSpace + ")");
	}
}
