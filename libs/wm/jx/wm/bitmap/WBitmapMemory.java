/********************************************************************************
 * Super Class of Memory Based Bitmaps
 * Copyright 2002- Christian Wawersich
 *******************************************************************************/

package jx.wm.bitmap;

import jx.wm.*;
import jx.devices.fb.FramebufferDevice;
import jx.devices.fb.PixelRect;
import jx.devices.fb.DrawingMode;
import jx.devices.fb.ColorSpace;
import jx.zero.*;

public abstract class WBitmapMemory extends WBitmap
{
	public Memory                  m_cMemory;
        public boolean                 m_bIsVideoMemory;
        public FramebufferDevice       m_cDisplayDriver;
	protected int		       m_nOffset;

	public boolean isMemory() { return true; }
	public WBitmapMemory castMemory() { return this; }

	public void dumpMemory() {
		DebugSupport 
			deb=(DebugSupport) InitialNaming.getInitialNaming().lookup("DebugSupport");
		deb.sendBinary("screenshot", m_cMemory, m_cMemory.size());
	}

	public static Memory allocMemory(int nSize)
	{
		Memory cMemory;
		Naming cNaming;
		MemoryManager cMemMgr;

		if ((cNaming = InitialNaming.getInitialNaming()) == null)
                        throw new Error ("WBitmap::WBitmap() unable to obtain Naming!");

		cMemMgr = (MemoryManager)cNaming.lookup("MemoryManager");
                if (cMemMgr == null)
                        throw new Error("WBitmap::WBitmap() no MemoryManager found");

		if ((cMemory = cMemMgr.alloc (nSize)) == null)
			throw new Error ("WBitmap::WBitmap() unable to allocate memory!");

		return cMemory;
	}

	public void drawBitmap (WBitmap cBitmap, PixelRect cDst, PixelRect cSrc, PixelRect cClp)
	{
		drawBitmap (cBitmap, cDst, cSrc, cClp, DrawingMode.COPY);
	}

	public void drawBitmap (WBitmap cBitmap, PixelRect cDst, PixelRect cSrc, PixelRect cClp, DrawingMode nMode)
	{		
		int yscale, xscale, xstart, ystart;
		PixelRect cDraw;
		PixelRect cTmpSrc;

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

		int xs = cDraw.left() - cDst.left() + cTmpSrc.left();
		int ys = cDraw.top() - cDst.top() + cTmpSrc.top();

		xstart = xs * xscale;
		ystart = ys * yscale;

		startFrameBufferUpdate ();
		try {
			if (cBitmap.m_eColorSpace.getValue()==ColorSpace.CS_CMAP8) {
				drawBitmapCMAP8(cBitmap, cDraw, xscale, yscale, xstart, ystart, nMode);
			} else if (cBitmap.m_eColorSpace.getValue()==ColorSpace.CS_RGB16) {
				drawBitmapRGB16(cBitmap, cDraw, xscale, yscale, xstart, ystart, nMode); 
			} else if (cBitmap.m_eColorSpace.getValue()==ColorSpace.CS_RGB32) {
				drawBitmapRGB32(cBitmap, cDraw, xscale, yscale, xstart, ystart, nMode); 
			}
		} catch (RuntimeException ex) {
			Debug.out.println("exception in drawBitmap");
			Debug.out.println("cSrc "+cSrc+cTmpSrc+" cDst "+cDst+" m_cBounds "+m_cBounds+" cDraw "+cDraw);
			//	ex.printStackTrace(System.out);
			//ex.printStackTrace();
		}
		endFrameBufferUpdate ();
	}

	protected void drawBitmapCMAP8 (WBitmap cBitmap, PixelRect cDraw, 
  		int xscale, int yscale, int xstart, int ystart, DrawingMode nMode) { throw new Error("not impl"); }

	protected void drawBitmapRGB16 (WBitmap cBitmap, PixelRect cDraw, 
  		int xscale, int yscale, int xstart, int ystart, DrawingMode nMode) { throw new Error("not impl"); }

	protected void drawBitmapRGB32 (WBitmap cBitmap, PixelRect cDraw, 
  		int xscale, int yscale, int xstart, int ystart, DrawingMode nMode) { throw new Error("not impl"); }

	public boolean IsVideoMemory ()
	{
		return m_bIsVideoMemory;
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
}
