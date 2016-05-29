package jx.wm.bitmap;

import jx.zero.*;
import jx.devices.fb.*;
import jx.wm.*;

/**
 * ColorMap 8 Bit Bitmap
 * @author Christian Wawersich
 */
public class WBitmapCMAP8Memory extends WBitmapMemory
{
	void init (int nWidth, int nHeight, int nBytesPerLine, ColorSpace eColorSpace) {
		
		Debug.out.println ("WBitmap::init(" + nWidth + ", " + nHeight + ", " + nBytesPerLine + ")");

		m_cDisplayDriver = null;
  		m_nBytesPerLine = nWidth;
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
		m_cMemory = allocMemory(m_nSize);
		m_nOffset = 0;
  		m_eColorSpace = eColorSpace;
  		m_nWidth = nWidth;
  		m_nHeight = nHeight;
		m_cBounds = new PixelRect (0, 0, m_nWidth - 1, m_nHeight - 1);
		m_bIsVideoMemory = false;
	}

	public WBitmapCMAP8Memory ()
	{
		m_eColorSpace.setValue (ColorSpace.CS_CMAP8);
		m_nWidth = m_nHeight = m_nBytesPerLine = m_nOffset = 0;
		m_cMemory = null;
		m_cBounds = null;
		m_cDisplayDriver = null;
		m_bIsVideoMemory = false;
	}

	public WBitmapCMAP8Memory (int nWidth, int nHeight, ColorSpace eColorSpace, int nBytesPerLine)
	{
		init (nWidth, nHeight, nBytesPerLine, eColorSpace);
	}

	public WBitmapCMAP8Memory (int nWidth, int nHeight)
	{
		init (nWidth, nHeight, -1, new ColorSpace(ColorSpace.CS_CMAP8));
	}

	public WBitmapCMAP8Memory (FramebufferDevice cDisplayDriver, int nWidth, int nHeight, ColorSpace eColorSpace, int nBytesPerLine, Memory cMemory, int nOffset)
	{
		Debug.out.println ("WBitmap::init(" + nWidth + ", " + nHeight + ", " + nBytesPerLine + ")");
		m_cDisplayDriver = cDisplayDriver;
		m_nWidth = nWidth;
		m_nHeight = nHeight;
		m_eColorSpace=eColorSpace;
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
		drawLineCMAP8 (cDraw, cClipped, cColor.toRGB16(), nDrawingMode);
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
			return;
		}
		if (clipLine (cClip, cClipped) == false)
		{
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
		drawLineCMAP8 (cDraw, cClipped, cColor.toRGB16(), nDrawingMode);
		endFrameBufferUpdate ();
	}

	public void fillRect (PixelRect cRect[], int nCount, PixelColor cColor, DrawingMode nMode)
	{
		if (m_cDisplayDriver != null && m_cDisplayDriver.fillRect (cRect, nCount, cColor, nMode) == 0)
			return;
			
		startFrameBufferUpdate (); 			
		//fillRectCMAP8 (cRect, nCount, cColor.toRGB32(), nMode);
		endFrameBufferUpdate ();
	}

	public void fillRect (PixelRect cRect, PixelColor cColor, DrawingMode nMode)
	{
		PixelRect cRects[] = new PixelRect[1];
		cRects[0] = cRect;
		fillRect (cRects, 1, cColor, nMode);
	}
	

	private void drawLineCMAP8 (PixelRect cDraw, PixelRect cClipped, short nColor, DrawingMode nDrawingMode) 
	{ throw new Error("not impl."); }

	private void fillRectCMAP8 (PixelRect cRect[], int nCount, short nColor, DrawingMode eMode)
	{ throw new Error("not impl."); }

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
			bitBlt8 (acOldPos[j], acNewPos[j]);
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
				m_cMemory.move (dst, src, cNewPos.right() - cNewPos.left());
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
/*		
		Debug.out.println ("WBitmap::drawBitmap(" + cBitmap + "," + cDst + "," + cSrc + "," + cClp + "," + nMode + ")");
		Debug.out.println ("this: (" + m_nWidth + ", " + m_nHeight + ", " + m_nBytesPerLine + ", " + m_eColorSpace + ")");
*/	
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

		if (cTmpSrc.width() < cDraw.width())
			cDraw.m_nX1 = cDraw.m_nX0 + cTmpSrc.width ();
		if (cTmpSrc.height() < cDraw.height())
			cDraw.m_nY1 = cDraw.m_nY0 + cTmpSrc.height ();
		
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
		drawBitmapCMAP8 (cBitmap, cDraw, xscale, yscale, xstart, ystart, nMode);
		endFrameBufferUpdate ();
	}

	protected void drawBitmapCMAP8 (WBitmap cBitmap, PixelRect cDraw, 
  		int xscale, int yscale, int xstart, int ystart, DrawingMode nMode)
	{ throw new Error("not impl."); }

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

	public void renderGlyph (Glyph cGlyph, int x, int y, PixelRect cTmpClip, int anPalette[]) 
	{
		PixelRect cClip = new PixelRect (cTmpClip);
	
		cClip.clip (m_cBounds);
		renderGlyph8 (cGlyph, x, y, cClip, anPalette);
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
		drawBitmapCMAP8 (cBitmap, cDraw, scale, scale, xstart*scale, ystart*scale, DrawingMode.COPY);
		endFrameBufferUpdate ();
	}

	public String toString ()
	{
		return new String ("WBitmapCMAP8(" + m_nWidth + "," + m_nHeight + "," + m_nBytesPerLine + "," + m_eColorSpace + ")");
	}
}
