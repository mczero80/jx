package jx.wm;

import jx.zero.*;
import jx.devices.fb.*;
import jx.wm.bitmap.*;

public abstract class WBitmap
{
	
//	static public Memory m_cVideoMemory = null;
	
	public ColorSpace   	m_eColorSpace = new ColorSpace();
	public int	        m_nWidth;
	public int		m_nHeight;
	public int		m_nBytesPerLine;
	public int		m_nSize;
	protected PixelRect	m_cBounds;

	public static WBitmap createWBitmap(int nWidth, int nHeight, ColorSpace eColorSpace) {
		return createWBitmap(nWidth, nHeight, eColorSpace, -1);
	} 

	abstract public void dumpMemory(); 

	public static WBitmap createWBitmap(int nWidth, int nHeight, ColorSpace eColorSpace, int nBytesPerLine)
	{
		switch (eColorSpace.getValue())
		{
			case ColorSpace.CS_RGB16:
				return new WBitmapRGB16Memory(nWidth, nHeight, nBytesPerLine);
			case ColorSpace.CS_RGB32:
				return new WBitmapRGB32Memory(nWidth, nHeight, nBytesPerLine);
			case ColorSpace.CS_CMAP8: 
				return new WBitmapCMAP8Memory(nWidth, nHeight, eColorSpace, nBytesPerLine);
			case ColorSpace.CS_RGBA32:
			case ColorSpace.CS_RGB24:
			case ColorSpace.CS_RGBA15:
			case ColorSpace.CS_RGB15:
			case ColorSpace.CS_GRAY8:
			case ColorSpace.CS_CMAP4:
			case ColorSpace.CS_GRAY4:
			default:
				return new WBitmapOrg(nWidth,nHeight, eColorSpace, nBytesPerLine);
		}
	}

	public static WBitmap createWBitmap(FramebufferDevice cDisplayDriver, int nWidth, int nHeight,
			ColorSpace eColorSpace, int nBytesPerLine, Memory cMemory, int nOffset) {

		//m_cVideoMemory = cMemory;

		switch (eColorSpace.getValue())
		{
			case ColorSpace.CS_RGB16:
				return new WBitmapRGB16Memory(cDisplayDriver, nWidth, nHeight, nBytesPerLine, cMemory, nOffset);
			case ColorSpace.CS_RGB32:
				return new WBitmapRGB32Memory(cDisplayDriver, nWidth, nHeight, nBytesPerLine, cMemory, nOffset);
			case ColorSpace.CS_CMAP8: 
				return new WBitmapCMAP8Memory(cDisplayDriver, nWidth, nHeight, eColorSpace, nBytesPerLine, cMemory, nOffset);
			case ColorSpace.CS_RGBA32:
			case ColorSpace.CS_RGB24:
			case ColorSpace.CS_RGBA15:
			case ColorSpace.CS_RGB15:
			case ColorSpace.CS_GRAY8:
			case ColorSpace.CS_CMAP4:
			case ColorSpace.CS_GRAY4:
			default:
				return new WBitmapOrg(cDisplayDriver, nWidth, nHeight, eColorSpace, nBytesPerLine, cMemory, nOffset);
		}
	}

	public boolean IsVideoMemory () { return false; }

	public boolean isMemory() { return false; }

	public WBitmapMemory castMemory() { throw new Error(); }

	public PixelRect bounds()
	{
		return m_cBounds;
	}

	public PixelRect getBounds ()
	{
		if (m_cBounds==null) Debug.out.println(this+": m_cBounds==null");
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

	public void set8 (int nOffset, byte nValue) { throw new Error("not impl"); }
	public byte get8 (int nOffset) { throw new Error("not impl"); }
	public void set16 (int nOffset, short nValue) { throw new Error("not impl"); } 
	public short get16 (int nOffset) { throw new Error("not impl"); }
	public void set32 (int nOffset, int nValue) { throw new Error("not impl"); }
	public int get32 (int nOffset){ throw new Error("not impl"); }
	public void fill16 (int nOffset, int nLen, short nValue){ throw new Error("not impl"); }
	public void fill32 (int nOffset, int nLen, int nValue){ throw new Error("not impl"); }

	abstract public void drawLine_Unsafe(PixelRect cDraw, PixelRect cClip, PixelColor cColor, DrawingMode nDrawingMode);
	abstract public void drawLine (PixelRect cDraw, PixelRect cTmpClip, PixelColor cColor, DrawingMode nDrawingMode);
	abstract public void fillRect (PixelRect cRect[], int nCount, PixelColor cColor, DrawingMode nMode);
	abstract public void fillRect (PixelRect cRect, PixelColor cColor, DrawingMode nMode);
	abstract public void bitBlt (PixelRect acOldPos[], PixelRect acNewPos[], int nCount);

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

	abstract public void drawBitmap (WBitmap cBitmap, PixelRect cDst, PixelRect cSrc, PixelRect cClp, DrawingMode nMode);
	abstract public void drawBitmap (WBitmap cBitmap, PixelRect cDst, PixelRect cSrc, PixelRect cClp);

	abstract public void renderGlyph (Glyph cGlyph, int x, int y, PixelRect cTmpClip, int anPalette[]);
	abstract public void drawCloneMap (WBitmap cBitmap, PixelRect cDst, PixelRect cClp);

	public final static int abs (int n) { return n < 0 ? -n : n; }

	protected boolean clipLine (PixelRect cRect, PixelRect cClipped)
	{
		boolean point_1 = false;
		boolean point_2 = false;  // tracks if each end point is visible or invisible
		boolean clip_always = false;           // used for clipping override
		int xi=0,yi=0;                     // point of intersection
		boolean right_edge  = false;              // which edges are the endpoints beyond
		boolean left_edge   = false;
		boolean top_edge    = false;
		boolean bottom_edge = false;
		boolean success = false;               // was there a successfull clipping

		int dx,dy;                   // used to holds slope deltas

		// test if line is completely visible

		if ( (cClipped.m_nX0 >= cRect.m_nX0) && (cClipped.m_nX0<=cRect.m_nX1) && (cClipped.m_nY0>=cRect.m_nY0) && (cClipped.m_nY0<=cRect.m_nY1) ) {
			point_1 = true;
		}

		if ( (cClipped.m_nX1 >= cRect.m_nX0) && (cClipped.m_nX1 <= cRect.m_nX1) && (cClipped.m_nY1 >= cRect.m_nY0) && (cClipped.m_nY1 <= cRect.m_nY1) ) {
			point_2 = true;
		}


		// test endpoints
		if (point_1 && point_2) {
			return true;
		}

		// test if line is completely invisible
		if (point_1==false && point_2==false)
		{
			// must test to see if each endpoint is on the same side of one of
			// the bounding planes created by each clipping region boundary

			if ( ( (cClipped.m_nX0<cRect.m_nX0) && (cClipped.m_nX1<cRect.m_nX0) ) ||  // to the left
					( (cClipped.m_nX0>cRect.m_nX1) && (cClipped.m_nX1>cRect.m_nX1) ) ||  // to the right
					( (cClipped.m_nY0<cRect.m_nY0) && (cClipped.m_nY1<cRect.m_nY0) ) ||  // above
					( (cClipped.m_nY0>cRect.m_nY1) && (cClipped.m_nY1>cRect.m_nY1) ) ) { // below
				return false; // the entire line is otside the rectangle
			}

			// if we got here we have the special case where the line cuts into and
			// out of the clipping region
			clip_always = true;
		}

		// take care of case where either endpoint is in clipping region
		if ( point_1 || clip_always )
		{
			dx = cClipped.m_nX1 - cClipped.m_nX0; // compute deltas
			dy = cClipped.m_nY1 - cClipped.m_nY0;

			// compute what boundary line need to be clipped against
			if (cClipped.m_nX1 > cRect.m_nX1) 
			{
				// flag right edge
				right_edge = true;

				// compute intersection with right edge
				if (dx!=0)
					yi = (32768 + ((dy * 65536 * (cRect.m_nX1 - cClipped.m_nX0))/dx)  + (cClipped.m_nY0*65536)) / 65536;
				else
					yi = -1;  // invalidate intersection
			} 
			else if (cClipped.m_nX1 < cRect.m_nX0) 
			{
				// flag left edge
				left_edge = true;

				// compute intersection with left edge
				if (dx!=0) 
				{
					yi = (32768 + ((dy * 65536 * (cRect.m_nX0 - cClipped.m_nX0))/dx)  + (cClipped.m_nY0*65536)) / 65536;
				} 
				else 
				{
					yi = -1;  // invalidate intersection
				}
			}

			// horizontal intersections
			if (cClipped.m_nY1 > cRect.m_nY1) 
			{
				bottom_edge = true; // flag bottom edge

				// compute intersection with right edge
				if (dy!=0) {
					xi = (32768 + ((dx * 65536 * (cRect.m_nY1 - cClipped.m_nY0))/dy)  + (cClipped.m_nX0 * 65536)) / 65536;
				} else {
					xi = -1;  // invalidate inntersection
				}
			} 
			else if (cClipped.m_nY1 < cRect.m_nY0) 
			{
				// flag top edge
				top_edge = true;

				// compute intersection with top edge
				if (dy!=0) {
					xi = (32768 + ((dx*65536 * (cRect.m_nY0 - cClipped.m_nY0))/dy)  + (cClipped.m_nX0 * 65536)) / 65536;
				} else {
					xi = -1;  // invalidate intersection
				}
			}

			// now we know where the line passed thru
			// compute which edge is the proper intersection

			if ( right_edge == true && (yi >= cRect.m_nY0 && yi <= cRect.m_nY1) )
			{
				cClipped.m_nX1 = cRect.m_nX1;
				cClipped.m_nY1 = yi;
				success = true;
			} 
			else if (left_edge && (yi>=cRect.m_nY0 && yi<=cRect.m_nY1) ) 
			{
				cClipped.m_nX1 = cRect.m_nX0;
				cClipped.m_nY1 = yi;
				success = true;
			}

			if ( bottom_edge == true && (xi >= cRect.m_nX0 && xi <= cRect.m_nX1) )
			{
				cClipped.m_nX1 = xi;
				cClipped.m_nY1 = cRect.m_nY1;
				success = true;
			} 
			else if (top_edge && (xi>=cRect.m_nX0 && xi<=cRect.m_nX1) ) 
			{
				cClipped.m_nX1 = xi;
				cClipped.m_nY1 = cRect.m_nY0;
				success = true;
			}
		} // end if point_1 is visible

		// reset edge flags
		right_edge = left_edge = top_edge = bottom_edge = false;

		// test second endpoint
		if ( point_2 || clip_always )
		{
			dx = cClipped.m_nX0 - cClipped.m_nX1; // compute deltas
			dy = cClipped.m_nY0 - cClipped.m_nY1;

			// compute what boundary line need to be clipped against
			if ( cClipped.m_nX0 > cRect.m_nX1 ) 
			{
				// flag right edge
				right_edge = true;

				// compute intersection with right edge
				if (dx!=0) {
					yi = (32768 + ((dy * 65536 * (cRect.m_nX1 - cClipped.m_nX1))/dx) + (cClipped.m_nY1*65536))/65536;
				} else {
					yi = -1;  // invalidate inntersection
				}
			}
			else if (cClipped.m_nX0 < cRect.m_nX0) 
			{
				left_edge = true; // flag left edge

				// compute intersection with left edge
				if (dx!=0) {
					yi = (int)(32768 + ((dy * 65536 * (cRect.m_nX0 - cClipped.m_nX1))/dx) + (cClipped.m_nY1 * 65536)) / 65536;
				} else {
					yi = -1;  // invalidate intersection
				}
			}

			// horizontal intersections
			if (cClipped.m_nY0 > cRect.m_nY1) 
			{
				bottom_edge = true; // flag bottom edge

				// compute intersection with right edge
				if (dy!=0) {
					xi = (int)(32768 + ((dx * 65536 * (cRect.m_nY1 - cClipped.m_nY1))/dy) + (cClipped.m_nX1 * 65536)) / 65536;
				} else {
					xi = -1;  // invalidate inntersection
				}
			} 
			else if (cClipped.m_nY0 < cRect.m_nY0) 
			{
				top_edge = true; // flag top edge

				// compute intersection with top edge
				if (dy!=0) {
					xi = (int)(32768 + ((dx * 65536 * (cRect.m_nY0 - cClipped.m_nY1))/dy) + (cClipped.m_nX1 * 65536)) / 65536;
				} else {
					xi = -1;  // invalidate inntersection
				}
			}

			// now we know where the line passed thru
			// compute which edge is the proper intersection
			if ( right_edge && (yi >= cRect.m_nY0 && yi <= cRect.m_nY1) ) 
			{
				cClipped.m_nX0 = cRect.m_nX1;
				cClipped.m_nY0 = yi;
				success = true;
			} 
			else if ( left_edge && (yi >= cRect.m_nY0 && yi <= cRect.m_nY1) ) 
			{
				cClipped.m_nX0 = cRect.m_nX0;
				cClipped.m_nY0 = yi;
				success = true;
			}

			if (bottom_edge && (xi >= cRect.m_nX0 && xi <= cRect.m_nX1) ) 
			{
				cClipped.m_nX0 = xi;
				cClipped.m_nY0 = cRect.m_nY1;
				success = true;
			} 
			else if (top_edge==true && (xi>=cRect.m_nX0 && xi<=cRect.m_nX1) ) 
			{
				cClipped.m_nX0 = xi;
				cClipped.m_nY0 = cRect.m_nY0;
				success = true;
			}
		} // end if point_2 is visible

		return(success);
	}

	public boolean isCloneMap(WBitmap clone) {
		if (m_nWidth == clone.m_nWidth &&
				m_nHeight == clone.m_nHeight &&
				//	m_eColorSpace == clone.m_eColorSpace &&
				m_nBytesPerLine == clone.m_nBytesPerLine) {
			return true;
		} else {
			return false;
		}
	}

	public WBitmap getCloneMap() {
		return createWBitmap(m_nWidth, m_nHeight, m_eColorSpace, m_nBytesPerLine);
	}


	public String toString ()
	{
		return new String ("WBitmap(" + m_nWidth + "," + m_nHeight + "," + m_nBytesPerLine + "," + m_eColorSpace + ")");
	}
}
