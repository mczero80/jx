package jx.wm;

import jx.collections.*;
import jx.zero.*;
import jx.zero.debug.*;
import jx.devices.fb.*;
import java.util.Vector;
import jx.wm.WRegion;
import jx.wm.WWindowImpl;
import jx.wm.WBitmap;
import jx.wm.message.*;
import jx.wm.WFont;

class ClippingRect
{
	public int	m_nX0;
	public int	m_nY0;	
	public int	m_nX1;
	public int	m_nY1;	
	public int	m_nMoveX;
	public int	m_nMoveY;

	public ClippingRect ()
	{
	}
	public ClippingRect (PixelRect cRect)
	{
		m_nX0 = cRect.m_nX0;
		m_nY0 = cRect.m_nY0;
		m_nX1 = cRect.m_nX1;
		m_nY1 = cRect.m_nY1;
	}
	public void setTo (PixelRect cRect)
	{
		m_nX0 = cRect.m_nX0;
		m_nY0 = cRect.m_nY0;
		m_nX1 = cRect.m_nX1;
		m_nY1 = cRect.m_nY1;
	}
}

class SortCmp implements Comparator
{
	public int compare (Object __a, Object __b)
	{
		ClippingRect cClip1 = (ClippingRect)__a;
		ClippingRect cClip2 = (ClippingRect)__b;
		if (cClip1.m_nX0 > cClip2.m_nX1 && cClip1.m_nX1 < cClip2.m_nX0)
		{	
			if (cClip1.m_nMoveX < 0)
			{
				return (cClip1.m_nX0 - cClip2.m_nX0);
			}
			else
			{
				return (cClip2.m_nX0 - cClip1.m_nX0);
			}
		}
		else
		{
			if (cClip1.m_nMoveY < 0)
			{
				return (cClip1.m_nY0 - cClip2.m_nY0);
			}
			else
			{
				return (cClip2.m_nY0 - cClip1.m_nY0);
			}
		}
	}
}

public class WView
{
	final static boolean fast = true;
 	/*static*/ private PixelRect cDraw = new PixelRect ();	
	/*static*/ private PixelRect cClip = new PixelRect ();
	
	static WView	s_cTopView;
	
	WWindowImpl		m_cOwner;
	WView		m_cParent;
	WView		m_cTopChild;
	WView		m_cBottomChild;
	WView		m_cLowerSibling;
	WView		m_cHigherSibling;
	PixelRect		m_cFrame;
	PixelPoint		m_cScrollOffset;
	String		m_cName;
	int		m_nFlags;

	WBitmap		m_cBitmap;
	
	boolean		m_bHasInvalidRegs;
	boolean		m_bBackdrop;
	boolean		m_bFontPaletteValid;
	boolean		m_bIsUpdating;
	boolean		m_bIsAddedToFont;
	boolean     m_bIsAdded = false;

	WRegion		m_cVisibleReg;
	WRegion		m_cDamageReg;
	WRegion		m_cActiveDamageReg;
	WRegion         m_cFullReg;
	WRegion         m_cDrawReg;
	WRegion         m_cPrevVisibleReg;
	WRegion         m_cPrevFullReg;
	WRegion			m_cUserClipReg;
	
	PixelPoint		m_cDeltaMove;
	PixelPoint          m_cDeltaSize;
	PixelPoint          m_cPenPos;

	int		m_nHideCount;
	int             m_nLevel;
	int		m_anFontPalette[];
	
	PixelColor		m_cFgColor;
	PixelColor		m_cBgColor;
	PixelColor		m_cEraseColor;	
	DrawingMode		m_nDrawingMode;
	WFont			m_cFont;
	
	public static final int	WID_CLEAR_BACKGROUND 	= 0x01;
	public static final int WID_DRAW_ON_CHILDREN 	= 0x02;
	public static final int WID_TRANSPARENT		= 0x04;
	public static final int WID_FULL_UPDATE_ON_H_RESIZE = 0x08;
	public static final int WID_FULL_UPDATE_ON_V_RESIZE = 0x10;

    	public static final int	FRAME_RECESSED    	= 0x000008;
    	public static final int	FRAME_RAISED      	= 0x000010;
    	public static final int	FRAME_THIN		= 0x000020;
    	public static final int	FRAME_WHIDE	      	= 0x000040;
    	public static final int	FRAME_ETCHED      	= 0x000080;
    	public static final int	FRAME_FLAT	      	= 0x000100;
    	public static final int	FRAME_DISABLED    	= 0x000200;
    	public static final int	FRAME_TRANSPARENT 	= 0x010000;
		

    	public static final int COL_NORMAL		= 0;
    	public static final int COL_SHINE		= 1;
    	public static final int COL_SHADOW		= 2;
    	public static final int COL_SEL_WND_BORDER	= 3;
    	public static final int COL_NORMAL_WND_BORDER	= 4;
    	public static final int COL_MENU_TEXT		= 5;
    	public static final int COL_SEL_MENU_TEXT	= 6;
    	public static final int COL_MENU_BACKGROUND	= 7;
    	public static final int COL_SEL_MENU_BACKGROUND	= 8;
    	public static final int COL_SCROLLBAR_BG	= 9;
    	public static final int COL_SCROLLBAR_KNOB	= 10;
    	public static final int COL_LISTVIEW_TAB	= 11;
    	public static final int COL_LISTVIEW_TAB_TEXT	= 12;
	public static final int COL_COUNT		= 13;
	
	static PixelColor s_asDefaultColors[] = 
	{
		//{0xaa, 0xaa, 0xaa, 0xff), // COL_NORMAL
		new PixelColor (0xaa, 0xaa, 0xaa, 0xff),
		new PixelColor (0xff, 0xff, 0xff, 0xff), // COL_SHINE
		new PixelColor (128, 128, 128, 0xff), // COL_SHADOW
  		new PixelColor (0x66, 0x88, 0xbb, 0xff),	// COL_SEL_WND_BORDER
  		new PixelColor (0x78, 0x78, 0x78, 0xff),	// COL_NORMAL_WND_BORDER
  		new PixelColor (0x00, 0x00, 0x00, 0xff),	// COL_MENU_TEXT
  		new PixelColor (0xff, 0xff, 0xff, 0xff),	// COL_SEL_MENU_TEXT
  		new PixelColor (0xd0, 0xd0, 0xd0, 0xd0),	// COL_MENU_BACKGROUND
  		new PixelColor (0x00, 0x00, 0x00, 0xff),	// COL_SEL_MENU_BACKGROUND
  		new PixelColor (0x78, 0x78, 0x78, 0xff),	// COL_SCROLLBAR_BG
  		new PixelColor (0xaa, 0xaa, 0xaa, 0xff),	// COL_SCROLLBAR_KNOB
  		new PixelColor (0x78, 0x78, 0x78, 0xff),	// COL_LISTVIEW_TAB
  		new PixelColor (0xff, 0xff, 0xff, 0xff)		// COL_LISTVIEW_TAB_TEXT
	};
	static PixelColor getDefaultColor (int nIndex)
	{
		if (nIndex >= COL_COUNT)
			return null;
		return new PixelColor (s_asDefaultColors[nIndex]);
	}


	public WView (WWindowImpl cOwner, WView cParent, String cName,
		PixelRect cFrame, int nFlags)
	{
		//Debug.out.println (cName + ": WView::WView(" + cOwner.m_cTitle + ", " + cFrame + ", " + nFlags + ")");
		init (cName);
		//m_nStatus       = 0;
  		m_cOwner        = cOwner;
  		m_cParent       = cParent;
  		m_cFrame        = cFrame;
		m_nFlags        = nFlags;
  		m_cBitmap = cParent != null ? cParent.m_cBitmap : null;
 
  		if (null != cParent) 
  			cParent.addChild (this, true);
	}	
	WView (WBitmap cBitmap, String cName, PixelRect cFrame)
	{	
		//dbprintf ("SrvView::SrvView() %s ",pzName);
		//cFrame.PrintToStream (getlog());
		//dbprintf ("\n");
		init (cName);
		//m_nStatus       = 0;
  		m_cOwner        = null;
  		m_cParent       = null;
  		m_cFrame        = cFrame;
		m_nFlags        = WID_CLEAR_BACKGROUND;

		setBitmap (cBitmap); 
	}
	public PixelRect getBounds () 
	{ 
		return new PixelRect (0, 0, m_cFrame.m_nX1 - m_cFrame.m_nX0, m_cFrame.m_nY1 - m_cFrame.m_nY0);
	}
	public String getName ()
	{
		return m_cName;
	}
	private void init (String cName)
	{
		m_cTopChild        	= null;
		m_cBottomChild     	= null;
		m_cHigherSibling   	= null;
		m_cLowerSibling    	= null;
		m_cFont            	= new WFont();
		m_nHideCount        	= 0;
		m_bFontPaletteValid 	= false;
		m_bIsUpdating       	= false;
		m_bHasInvalidRegs   	= true;
		m_nLevel            	= 0;
		m_bBackdrop         	= false;
		m_bIsAddedToFont    	= false;
		m_nDrawingMode		= new DrawingMode (DrawingMode.DM_COPY);
		m_cDeltaMove        	= new PixelPoint (0,0);
		m_cDeltaSize        	= new PixelPoint (0,0);
		m_cScrollOffset     	= new PixelPoint (0,0);
		m_cPenPos           	= new PixelPoint (0,0);
		
		m_cFgColor		= new PixelColor (0, 0, 0);
		m_cBgColor		= new PixelColor (255, 255, 255);
		m_cEraseColor		= new PixelColor (m_cBgColor);
	
		m_cName = new String (cName);
		m_anFontPalette		= new int[WFont.NUM_FONT_GRAYS];
		for (int i = 0; i < WFont.NUM_FONT_GRAYS; i++)
			m_anFontPalette[i] = 0;
		
		m_cVisibleReg 		= new WRegion ();
		m_cDamageReg		= new WRegion ();
		m_cActiveDamageReg	= new WRegion ();
		m_cFullReg		= new WRegion ();
		m_cDrawReg		= new WRegion ();
		m_cPrevVisibleReg	= new WRegion ();
		m_cPrevFullReg		= new WRegion ();
		m_cUserClipReg		= new WRegion ();
	}	
	public void finalize ()
	{
		//Debug.out.println (m_cName + "WView::finalize ()");
		setBitmap (null);
		m_cFont = null;
		m_nDrawingMode = null;
		m_cDeltaMove = null;
		m_cDeltaSize = null;
		m_cScrollOffset = null;
		m_cPenPos = null;
		m_cFgColor = null;
		m_cBgColor = null;
		m_cEraseColor = null;
		m_cName = null;
		m_anFontPalette = null;
		m_cVisibleReg = null;
		m_cDamageReg = null;
		m_cActiveDamageReg = null;
		m_cFullReg = null;
		m_cDrawReg = null;
		m_cPrevVisibleReg = null;
		m_cPrevFullReg = null;
		m_cUserClipReg = null;
	}

	static void setTopView (WView cTopView)
	{
		s_cTopView = cTopView;
	}

	static WView getTopView ()
	{
		return s_cTopView;
	}

	public void dumpScreen() {
		if (this==s_cTopView) {
			m_cBitmap.dumpMemory();
		} else {
			s_cTopView.dumpScreen();
		}
	}

	public void show (boolean bFlag)
	{
		//Debug.out.println (m_cName + ": WView::show(" + bFlag + ") hideCount: " + m_nHideCount);
  		if (m_cParent == null || m_cOwner == null) 
	 	{
  			Debug.out.print ( "Error: WView::show() attempt to hide root layer\n" );
	  		return;
	  	}
		if (bFlag) 
  			m_nHideCount--;
		else 
  			m_nHideCount++;

		m_cParent.m_bHasInvalidRegs = true;
		setDirtyRegFlags ();
	
		WView cSibling;
		PixelRect cRect = new PixelRect ();
		for (cSibling = m_cParent.m_cBottomChild; cSibling != null; cSibling = cSibling.m_cHigherSibling)
		{
			if (cSibling.m_cFrame.intersects (m_cFrame))
			{
				cRect.m_nX0 = m_cFrame.m_nX0 - cSibling.m_cFrame.m_nX0;
				cRect.m_nY0 = m_cFrame.m_nY0 - cSibling.m_cFrame.m_nY0;
				cRect.m_nX1 = m_cFrame.m_nX1 - cSibling.m_cFrame.m_nX0;
				cRect.m_nY1 = m_cFrame.m_nY1 - cSibling.m_cFrame.m_nY0;
				cSibling.markModified (cRect);
			}				
		}	
		WView cChild;
		for (cChild = m_cTopChild; cChild != null; cChild = cChild.m_cLowerSibling)
			cChild.show (bFlag);	
	}
	public PixelRect getFrame ()
	{
		return new PixelRect (m_cFrame);
	}
	public void setFrame (PixelRect cFrame)
	{
		//Debug.out.println (m_cName + ": WView::setFrame(" + cFrame + " / " + m_cFrame + ")");
		if (m_nHideCount == 0)
		{
			if (m_cFrame.isEqual (cFrame))
				return;


/*
			m_cDeltaMove += PixelPoint (cFrame.LeftTop()) - PixelPoint (m_cFrame.LeftTop());
			m_cDeltaSize += PixelPoint (cFrame.width(), cFrame.height()) -
			                PixelPoint (m_cFrame.width(), m_cFrame.height());
*/
			m_cDeltaMove.m_nX += cFrame.m_nX0 - m_cFrame.m_nX0;
			m_cDeltaMove.m_nY += cFrame.m_nY0 - m_cFrame.m_nY0;
			m_cDeltaSize.m_nX += cFrame.width() - m_cFrame.width();
			m_cDeltaSize.m_nY += cFrame.height() - m_cFrame.height ();
			
			if (m_cParent != null)
				m_cParent.m_bHasInvalidRegs = true;

			setDirtyRegFlags ();
			WView cSibling;
			PixelRect cRect = new PixelRect ();
	
			for (cSibling = m_cLowerSibling; cSibling != null; cSibling = cSibling.m_cLowerSibling)
			{
				if (cSibling.m_cFrame.intersects (m_cFrame) || cSibling.m_cFrame.intersects (cFrame))
				{
/*				
					cSibling.markModified (m_cFrame - cSibling.m_cFrame.LeftTop ());
					cSibling.markModified (cFrame - cSibling.m_cFrame.LeftTop ());
*/
					cRect.m_nX0 = m_cFrame.m_nX0 - cSibling.m_cFrame.m_nX0;					
					cRect.m_nY0 = m_cFrame.m_nY0 - cSibling.m_cFrame.m_nY0;					
					cRect.m_nX1 = m_cFrame.m_nX1 - cSibling.m_cFrame.m_nX0;					
					cRect.m_nY1 = m_cFrame.m_nY1 - cSibling.m_cFrame.m_nY0;					
					cSibling.markModified (cRect);
					cRect.m_nX0 = cFrame.m_nX0 - cSibling.m_cFrame.m_nX0;					
					cRect.m_nY0 = cFrame.m_nY0 - cSibling.m_cFrame.m_nY0;					
					cRect.m_nX1 = cFrame.m_nX1 - cSibling.m_cFrame.m_nX0;					
					cRect.m_nY1 = cFrame.m_nY1 - cSibling.m_cFrame.m_nY0;					
					cSibling.markModified (cRect);
				}
			}
		}
		m_cFrame = cFrame;
		if (m_cParent == null)
			invalidate ();
	}
	public void setClip (int x, int y, int w, int h)
	{
		m_cUserClipReg.makeEmpty ();
		m_cDrawReg.makeEmpty ();
		m_cUserClipReg.include (new PixelRect (x, y, x + w - 1, y + h - 1));
	}
	public void setFgColor (PixelColor cColor)
	{
		m_cFgColor.setTo (cColor);
		m_bFontPaletteValid = false;
	}
	public void setBgColor (PixelColor cColor)
	{	
		m_cBgColor.setTo (cColor);
		m_bFontPaletteValid = false;
	}
	public void setEraseColor (PixelColor cColor)
	{
		m_cEraseColor.setTo (cColor);
	}
	public void setFgColor (int r, int g, int b, int a)
	{
		m_cFgColor.setTo (r, g, b, a);
		m_bFontPaletteValid = false;
	}
	public void setBgColor (int r, int g, int b, int a)
	{
		m_cBgColor.setTo (r, g, b, a);
		m_bFontPaletteValid = false;
	}
	public void setEraseColor (int r, int g, int b, int a)
	{
		m_cEraseColor.setTo (r, g, b, a);
	}
	public void setFgColor (int r, int g, int b)
	{
		m_cFgColor.setTo (r, g, b, 0);
		m_bFontPaletteValid = false;
	}
	public void setBgColor (int r, int g, int b)
	{
		m_cBgColor.setTo (r, g, b, 0);
		m_bFontPaletteValid = false;
	}
	public void setEraseColor (int r, int g, int b)
	{
		m_cEraseColor.setTo (r, g, b, 0);
	}
	public void movePenTo (int x, int y)		
	{ 
		m_cPenPos.m_nX = x; 
		m_cPenPos.m_nY = y;  
	}
	public void movePenTo (PixelPoint cPos)	
	{ 
		m_cPenPos.setTo (cPos);  
	}
	public void movePenBy (PixelPoint cPos)	
	{ 
		m_cPenPos.add (cPos); 
	}
	void setFontPaletteEntry (int nIndex, PixelColor cCol)
	{
		switch (m_cBitmap.getColorSpace ().getValue())
  		{
    			case ColorSpace.CS_CMAP8:	    
    			case ColorSpace.CS_GRAY8:
      				m_anFontPalette[nIndex] = (int)cCol.toCMAP8();
      				break;
    			case ColorSpace.CS_RGB15:
    			case ColorSpace.CS_RGBA15:
      				m_anFontPalette[nIndex] = (int)cCol.toRGB15();
      				break;
    			case ColorSpace.CS_RGB16:
      				m_anFontPalette[nIndex] = (int)cCol.toRGB16();
      				break;
    			case ColorSpace.CS_RGB24:
      				m_anFontPalette[nIndex] = (int)cCol.toRGB24();
      				break;
    			case ColorSpace.CS_RGB32:
      				m_anFontPalette[nIndex] = (int)cCol.toRGB32();
      				break;
	  		default:
     				Debug.out.println ("WView::setFontPaletteEntry() unknown colorspace " + m_cBitmap.getColorSpace());
     				break;
  		}      
	}
	public void drawString (String cString, int nLength)
	{
		//Debug.out.println ("drawString(" + cString + ", " + nLength + "/" + cString.length() + ")");
		//Debug.out.println ("colors: " + m_cFgColor + ", " + m_cBgColor);
  		if (m_cBitmap == null || m_cFont == null) 
  			return;
  		WRegion cReg = getRegion();

  		if (cReg == null) 
  			return;

  		if (nLength == -1) 
  			nLength = cString.length();

  		PixelPoint cTopLeft = convertToRoot (new PixelPoint(0,0));    
  		PixelPoint cPos	= new PixelPoint (m_cPenPos);

		if (m_cParent != null && m_cBitmap != m_cParent.m_cBitmap)
			cTopLeft.setTo (0, 0);
			
		cPos.add (cTopLeft);
		cPos.add (m_cScrollOffset);
  		int	   i;
/*TODO
  		if (m_cOwner == null || m_cOwner->isOffScreen() == false) 
  			SrvSprite::Hide (GetBounds() + cTopLeft);
*/    
  		if (m_bFontPaletteValid == false)
  		{
			PixelColor cCol = new PixelColor();
    
    			setFontPaletteEntry (0, m_cBgColor);
    			setFontPaletteEntry (WFont.NUM_FONT_GRAYS-1, m_cFgColor);
  			for (i = 1 ; i < WFont.NUM_FONT_GRAYS - 1 ; ++i) 
    			{
				cCol.setTo (
					m_cBgColor.m_nRed   + (m_cFgColor.m_nRed   - m_cBgColor.m_nRed)   * i / (WFont.NUM_FONT_GRAYS-1),
					m_cBgColor.m_nGreen + (m_cFgColor.m_nGreen - m_cBgColor.m_nGreen) * i / (WFont.NUM_FONT_GRAYS-1),
					m_cBgColor.m_nBlue  + (m_cFgColor.m_nBlue  - m_cBgColor.m_nBlue)  * i / (WFont.NUM_FONT_GRAYS-1),
					0);
      				setFontPaletteEntry (i, cCol); 
 	  		}
  			m_bFontPaletteValid = true;
			//Debug.out.println ("palette: [" + Integer.toHexString(m_anFontPalette[0]) + ", " +
			//	Integer.toHexString (m_anFontPalette[WFont.NUM_FONT_GRAYS-1]) + "]");
  		}
		for (i = 0; i < nLength; ++i)
    	{
   			char	nChar = cString.charAt(i);
   			Glyph cGlyph = m_cFont.getGlyph(((int)nChar) & 0xffff);
			PixelRect cClip = new PixelRect ();

  			if (cGlyph == null) 
   	 		{
 				Debug.out.println ("Error: WView::drawString() failed to load glyph");
  	    		continue;
	    	}      
      
			for (int j = 0; j < cReg.countRects(); j++)
  			{
				cClip.setTo (cReg.rectAt(j));
				cClip.add (cTopLeft);
	    		m_cBitmap.renderGlyph (cGlyph, cPos.m_nX, cPos.m_nY, cClip, m_anFontPalette);
	    	}
   			cPos.m_nX += cGlyph.m_nAdvance;
   			m_cPenPos.m_nX += cGlyph.m_nAdvance;
    	}
/*TODO
  		if (m_pcOwner == NULL || m_pcOwner->IsOffScreen() == false) 
  			SrvSprite::Unhide();
*/			
	}
	public void drawFrame (PixelRect cRect, int nStyle)
	{
		boolean Sunken = false;
		
		//Debug.out.println (m_cName + "WView::drawFrame(" + cRect + ", " + nStyle + ")");

		if (((nStyle & FRAME_RAISED) == 0) && (nStyle & (FRAME_RECESSED)) != 0)
		{
			Sunken = true;
		}

		if ((nStyle & FRAME_FLAT) != 0)
		{
			if ((nStyle & FRAME_TRANSPARENT) == 0)
			{
				fillRect (new PixelRect (cRect.m_nX0 + 2, cRect.m_nY0 + 2, 
					cRect.m_nX1 - 2, cRect.m_nY1 - 2), m_cEraseColor,
					new DrawingMode (DrawingMode.DM_COPY));
			}

			setFgColor (PixelColor.BLACK);

			movePenTo (cRect.m_nX0, cRect.m_nY1);
			drawLine (cRect.m_nX0, cRect.m_nY0);
			drawLine (cRect.m_nX1, cRect.m_nY0);
			drawLine (cRect.m_nX1, cRect.m_nY1);
			drawLine (cRect.m_nX0, cRect.m_nY1);

		}	
		else
		{
			PixelColor sShinePen  = getDefaultColor (COL_SHINE);
			PixelColor sShadowPen = getDefaultColor (COL_SHADOW);

			if ((nStyle & FRAME_TRANSPARENT) == 0)
			{
				fillRect (new PixelRect (cRect.m_nX0 + 2, cRect.m_nY0 + 2, 
					cRect.m_nX1 - 2, cRect.m_nY1 - 2), m_cEraseColor,
					new DrawingMode (DrawingMode.DM_COPY));
			}

			setFgColor ((Sunken) ? sShadowPen : sShinePen);

			movePenTo (cRect.m_nX0, cRect.m_nY1);
			drawLine (cRect.m_nX0, cRect.m_nY0);
			drawLine (cRect.m_nX1, cRect.m_nY0);

			setFgColor ((Sunken) ? sShinePen : sShadowPen);

			drawLine (cRect.m_nX1, cRect.m_nY1);
			drawLine (cRect.m_nX0, cRect.m_nY1);


			if ((nStyle & FRAME_THIN) == 0)
			{
				if ((nStyle & FRAME_ETCHED) != 0)
				{
					setFgColor ((Sunken) ? sShadowPen : sShinePen);

					movePenTo (cRect.m_nX0 + 1, cRect.m_nY1 - 1);

					drawLine (cRect.m_nX0 + 1, cRect.m_nY0 + 1);
					drawLine (cRect.m_nX1 - 1, cRect.m_nY0 + 1);

					setFgColor ((Sunken) ? sShinePen : sShadowPen);

					drawLine (cRect.m_nX1 - 1, cRect.m_nY1 - 1);
					drawLine (cRect.m_nX0 + 1, cRect.m_nY1 - 1);
				}
				else
				{
					PixelColor sBrightPen = getDefaultColor (COL_SHINE);
					PixelColor sDarkPen   = getDefaultColor (COL_SHADOW);

					setFgColor ((Sunken) ? sDarkPen : sBrightPen);

					movePenTo (cRect.m_nX0 + 1, cRect.m_nY1 - 1);

					drawLine (cRect.m_nX0 + 1, cRect.m_nY0 + 1);
					drawLine (cRect.m_nX1 - 1, cRect.m_nY0 + 1);

					setFgColor ((Sunken) ? sBrightPen : sDarkPen);
	
					drawLine (cRect.m_nX1 - 1, cRect.m_nY1 - 1);
					drawLine (cRect.m_nX0 + 1, cRect.m_nY1 - 1);
				}
			}
			else
			{
				if ((nStyle & FRAME_TRANSPARENT) == 0)
				{
					fillRect (new PixelRect (cRect.m_nX0 + 1, cRect.m_nY0 + 1, cRect.m_nX1 - 1,
									 cRect.m_nY1 - 1), m_cEraseColor,
									 new DrawingMode (DrawingMode.DM_COPY));
				}
			}
		}
	}
	public void getFontHeight (WFontHeight cFontHeight)
	{
		m_cFont.getFontHeight (cFontHeight);
		/*
		cFontHeight.nAscender = 7;
		cFontHeight.nDescender = 2;
		cFontHeight.nLineGap = 1;
		*/
	}

	private int convertXToRoot (int x)
	{
		if (m_cParent != null) {
			x += m_cFrame.m_nX0;
			return m_cParent.convertXToRoot (x);
		}			
		return x;
	}

	private int convertYToRoot (int y)
	{
		if (m_cParent != null) {
			y += m_cFrame.m_nY0;
			return m_cParent.convertYToRoot (y);
		}			
		return y;
	}

	public PixelPoint convertToRoot (PixelPoint cPoint)
	{
  		if (m_cParent != null) 
		{
			cPoint.m_nX += m_cFrame.m_nX0;
			cPoint.m_nY += m_cFrame.m_nY0;
  			return m_cParent.convertToRoot (cPoint);
		}			
		return cPoint;
	}

	public PixelRect convertToRoot (PixelRect cRect)
	{
		if (m_cParent != null) 
		{
			cRect.m_nX0 += m_cFrame.m_nX0;
			cRect.m_nY0 += m_cFrame.m_nY0;
			cRect.m_nX1 += m_cFrame.m_nX0;
			cRect.m_nY1 += m_cFrame.m_nY0;
  			return m_cParent.convertToRoot (cRect);
		}			
  		return cRect;
	}

	public void drawLine (PixelPoint cToPos, DrawingMode eMode)
	{	
		if (!fast) {
			if (null == m_cBitmap) 
				return;
			WRegion cReg = getRegion();

			if (null == cReg) 
				return;

			PixelPoint cTopLeft = convertToRoot (new PixelPoint (0,0));
			if (m_cBitmap != m_cParent.m_cBitmap)
				cTopLeft.setTo (0, 0);

			PixelPoint cMin = new PixelPoint (m_cPenPos);
			PixelPoint cMax = new PixelPoint (cToPos);

			cMin.add (cTopLeft);
			cMin.add (m_cScrollOffset);
			cMax.add (cTopLeft);
			cMax.add (m_cScrollOffset);

			m_cPenPos.setTo (cToPos);

			PixelRect cDraw = new PixelRect (cMin, cMax);	
			PixelRect cClip = new PixelRect ();
			PixelRect cBounds = getBounds();
			cBounds.add (cTopLeft);

			if (m_cOwner == null)
				WSprite.hide (cBounds);

			for (int i = 0; i < cReg.countRects(); i++)
			{
				cClip.setTo (cReg.rectAt(i));
				cClip.add (cTopLeft);
				m_cBitmap.drawLine (cDraw, cClip, m_cFgColor, eMode);
			}
			if (m_cOwner == null) 
				WSprite.unhide();
		} else {
			drawLineFast(cToPos.m_nX,cToPos.m_nY,eMode);
		}
	}

	/**
	 * deC++ style ;-( 
	 *
	 * 3 new less
	 *
	 * @author Christian Wawersich
	 */

	private void drawLineFast (int x, int y, DrawingMode eMode)
	{	
		int minX, minY;
		int maxX, maxY;
		int topX, topY;

		if (null == m_cBitmap) return;

  		WRegion cReg = getRegion();

  		if (null == cReg) return;
/*  
 	 	PixelPoint cTopLeft = convertToRoot (new PixelPoint (0,0));
		if (m_cParent != null && m_cBitmap != m_cParent.m_cBitmap)
			cTopLeft.setTo (0, 0);
		PixelPoint cMin = new PixelPoint (m_cPenPos);
		PixelPoint cMax = new PixelPoint (cToPos);
		
		cMin.add (cTopLeft);
		cMin.add (m_cScrollOffset);
		cMax.add (cTopLeft);
		cMax.add (m_cScrollOffset);
*/		
		if (m_cParent != null && m_cBitmap != m_cParent.m_cBitmap) {
			topX = 0;
			topY = 0;
		} else {
			topX = convertXToRoot (0);
			topY = convertYToRoot (0);
		}

		minX = m_cPenPos.m_nX + topX + m_cScrollOffset.m_nX;
		minY = m_cPenPos.m_nY + topY + m_cScrollOffset.m_nY; 

		maxX = x + topX + m_cScrollOffset.m_nX;
		maxY = y + topY + m_cScrollOffset.m_nY;
		
  		m_cPenPos.setTo (x, y);

/*
		PixelRect cDraw = new PixelRect (minX, minY, maxX, maxY);	
		PixelRect cClip = new PixelRect ();
*/
		cDraw.setTo(minX, minY, maxX, maxY);	

		PixelRect cBounds = getBounds();
		cBounds.add (topX, topY);

  		if (m_cOwner == null)
  			WSprite.hide (cBounds);
			
		for (int i = 0; i < cReg.countRects(); i++)
  		{
			cClip.setTo (cReg.rectAt(i));
			cClip.add (topX, topY);
			//m_cBitmap.drawLine (cDraw, cClip, m_cFgColor, eMode);
			m_cBitmap.drawLine_Unsafe(cDraw, cClip, m_cFgColor, eMode);
  		}

  		if (m_cOwner == null) 
	  		WSprite.unhide();
	}

	public void drawLine (PixelPoint p)
	{
		if (!fast) {	
			drawLine (p, m_nDrawingMode);
		} else {
			drawLineFast(p.m_nX, p.m_nY, m_nDrawingMode);
		}
	}

	public void drawLine (int x, int y)
	{
		if (!fast) {	
			PixelPoint cToPos = new PixelPoint (x, y);
			drawLine (cToPos);
		} else { 
			drawLineFast(x,y,m_nDrawingMode);
		}
	}

	public void drawLine (int x, int y, DrawingMode nMode)
	{
		if (!fast) {	
			PixelPoint cToPos = new PixelPoint (x, y);
			drawLine (cToPos);
		} else {
			drawLineFast(x,y,nMode);
		}
	}

	public void drawRect(PixelRect cRect, PixelColor cColor) 
	{
		drawRect(cRect,cColor,m_nDrawingMode);
	}


	public void drawRect(PixelRect cRect, PixelColor cColor, DrawingMode nMode)
	{
		setFgColor (cColor);
		movePenTo (cRect.m_nX0, cRect.m_nY1);
		drawLine(cRect.m_nX0, cRect.m_nY0, nMode);
		drawLine(cRect.m_nX1, cRect.m_nY0, nMode);
		drawLine(cRect.m_nX1, cRect.m_nY1, nMode);
		drawLine(cRect.m_nX0, cRect.m_nY1, nMode);
	}	

	public void fillRect (PixelRect cRect)
	{
		fillRect (cRect, m_cFgColor);
	}
	public void fillRect (PixelRect cRect, DrawingMode nMode)
	{
		fillRect (cRect, m_cFgColor, nMode);
	}

	public void fillRect (PixelRect cRect, PixelColor cColor)
	{
		if (cColor==null) cColor=m_cFgColor;
		fillRect (cRect, cColor, m_nDrawingMode);
	}

	public void fillRect (PixelRect cRect,  PixelColor cColor, DrawingMode nMode)
	{
	  	PixelRect acRect[] = new PixelRect[16];
	  	int nCount;

  		if (null == m_cBitmap) return;

		WRegion cReg = getRegion ();			
		if (cReg == null)
			return;
/*		
	cReg.PrintToStream (getlog());		
*/
  		PixelPoint cTopLeft = convertToRoot (new PixelPoint (0,0));
		if (m_cParent != null && m_cBitmap != m_cParent.m_cBitmap)
			cTopLeft.setTo (0, 0);
		PixelRect cDstRect = new PixelRect (cRect);
  		/*PixelRect cDstRect (cRect + m_cScrollOffset);*/
		cDstRect.add (m_cScrollOffset);
		PixelRect cBounds = getBounds();
		cBounds.add (cTopLeft);

  		if (m_cOwner == null) 
  			WSprite.hide (cBounds);

	  	nCount = 0;
		for (int i = 0; i < cReg.countRects(); i++)
		{
			PixelRect cClip = cReg.rectAt (i);
    			/*ClippingRect cRect = cDstRect & *cClip;*/
			
			if (acRect[nCount] != null)
				acRect[nCount].setTo (cDstRect);
			else
				acRect[nCount] = new PixelRect (cDstRect);				
			acRect[nCount].clip (cClip);
			
   			if (acRect[nCount].isValid()) 
   			{
/*Debug.out.println (i + "/" + nCount + ": " + acRect[nCount] + "/" + cDstRect + "/" + cClip);*/
				acRect[nCount].add (cTopLeft);
				nCount++;
				if (nCount == 16)
		      	{
					m_cBitmap.fillRect (acRect, nCount, cColor, nMode);
		        		nCount = 0;
   				}        
   			}      
		}

  		if (nCount != 0)
			m_cBitmap.fillRect (acRect, nCount, cColor, nMode);
			
  		if (m_cOwner == null) 
  			WSprite.unhide();
	}

	public void drawBitmap (WBitmap cBitmap, PixelRect cSrcRect, PixelRect cTmpDstRect, DrawingMode nDrawingMode)
	{
		if (m_cBitmap == null)
			return;
		WRegion cReg = getRegion();		
  		if (cReg == null) 
  			return;

  		PixelPoint cTopLeft = convertToRoot (new PixelPoint (0,0));
		if (m_cParent != null && m_cBitmap != m_cParent.m_cBitmap)
			cTopLeft.setTo (0, 0);
		PixelRect cDstRect = new PixelRect (cTmpDstRect);
		PixelRect cBounds = getBounds();
		
  		cDstRect.add (m_cScrollOffset);
  		cDstRect.add (cTopLeft);
		cBounds.add (cTopLeft);

 		if (m_cOwner == null) 
  			WSprite.hide (cBounds);

  		if (nDrawingMode.getValue() == DrawingMode.DM_UNKNOWN)
    			nDrawingMode.setValue (m_nDrawingMode.getValue());    
/*		
		PixelRect cISrcRect = new PixelRect (cSrcRect);
		PixelRect cIDstRect = new PixelRect (cDstRect);		
*/
		for (int i = 0; i < cReg.countRects(); i++)
		{
			PixelRect cClip = new PixelRect (cReg.rectAt (i));
			cClip.add (cTopLeft);
	    		m_cBitmap.drawBitmap (cBitmap, cDstRect, cSrcRect, cClip, nDrawingMode);
		}
		if (m_cOwner == null)  
		  	WSprite.unhide();

	}

	public void drawBitmap (WBitmap cBitmap, PixelPoint cPos, DrawingMode nDrawingMode)
	{
		PixelRect cSrc = new PixelRect (cBitmap.getBounds ());
  		PixelRect cDst = new PixelRect (cSrc);
		cDst.add (cPos);
	  	drawBitmap (cBitmap, cSrc, cDst, nDrawingMode);
	}

	public void drawBitmap (WBitmap cBitmap, int x, int y)
	{
		PixelRect cSrc = new PixelRect (cBitmap.getBounds ());
  		PixelRect cDst = new PixelRect (cSrc);
		cDst.add (x, y);
	  	drawBitmap (cBitmap, cSrc, cDst, m_nDrawingMode);
	}

	public void drawCloneMap(WBitmap cClone, int x, int y) {

		if (m_cBitmap==null || cClone==null) return;

		WRegion cReg = getRegion();		
  		if (cReg == null) return;

		int x_off = 0;
                int y_off = 0;
		if (m_cParent != null && m_cBitmap == m_cParent.m_cBitmap) {
			x_off = convertXToRoot(x_off);
			y_off = convertYToRoot(y_off);
		}

		PixelRect cSrc = cClone.getBounds();
		PixelRect cDst = new PixelRect(cClone.getBounds());
		PixelRect cBounds = getBounds();
		
  		cDst.add (m_cScrollOffset);
  		cDst.add (x_off,y_off);
		cDst.add (x, y);
		cBounds.add (x_off,y_off);

 		if (m_cOwner == null) 
  			WSprite.hide (cBounds);

		for (int i = 0; i < cReg.countRects(); i++)
		{
			PixelRect cClip = new PixelRect (cReg.rectAt (i));
			cClip.add (x_off,y_off);
	    		//m_cBitmap.drawBitmap (cClone, cDst, cClone.getBounds(), cClip, m_nDrawingMode);
			m_cBitmap.drawCloneMap(cClone, cDst, cClip);
		}
		if (m_cOwner == null)  
		  	WSprite.unhide();
	}
	
	public void addChild (WView cChild)
	{
		addChild (cChild, true);
	}
	public void addChild (WView cChild, boolean bTopmost)
	{
		//Debug.out.println (m_cName + ": WView::addChild(" + cChild.m_cName + ", " + bTopmost + ", " + m_nHideCount + ")");
		if (cChild.m_bIsAdded)
		{
			Debug.out.println (m_cName + ": WView::addChild() child already belonging to a view");
			return;
		}
		cChild.m_bIsAdded = true;
		if (null == m_cBottomChild && null == m_cTopChild)
		{
			m_cBottomChild = cChild;
			m_cTopChild = cChild;
			cChild.m_cLowerSibling = null;
			cChild.m_cHigherSibling = null;
		}
		else
		{
			if (bTopmost)
			{
				if (cChild.m_bBackdrop == false)
				{
					if (null != m_cTopChild)
						m_cTopChild.m_cHigherSibling = cChild;
					cChild.m_cLowerSibling = m_cTopChild;
					m_cTopChild = cChild;
					cChild.m_cHigherSibling = null;
				}
				else
				{
					WView cTmp;
	
					for (cTmp = m_cBottomChild; cTmp != null;
							 cTmp = cTmp.m_cHigherSibling)
					{
						if (cTmp.m_bBackdrop == false)
							break;
					}
					if (cTmp != null)
					{
						cChild.m_cHigherSibling = cTmp;
						cChild.m_cLowerSibling = cTmp.m_cLowerSibling;
						cTmp.m_cLowerSibling = cChild;
						if (cChild.m_cLowerSibling != null)
							cChild.m_cLowerSibling.m_cHigherSibling = cChild;
						else
							m_cBottomChild = cChild;
					}
					else
					{
						m_cTopChild.m_cHigherSibling = cChild;
						cChild.m_cLowerSibling = m_cTopChild;
						m_cTopChild = cChild;
						cChild.m_cHigherSibling = null;
					}
				}
			}	
			else
			{
				if (cChild.m_bBackdrop)
				{
					if (null != m_cBottomChild)
					{
						m_cBottomChild.m_cLowerSibling = cChild;
					}
					cChild.m_cHigherSibling = m_cBottomChild;
					m_cBottomChild = cChild;
					cChild.m_cLowerSibling = null;
				}
				else
				{
					WView cTmp;
	
					for (cTmp = m_cBottomChild; cTmp != null; cTmp = cTmp.m_cHigherSibling)
					{
						if (cTmp.m_bBackdrop == false)
							break;
					}
					if (cTmp != null)
					{
						cChild.m_cHigherSibling = cTmp;
						cChild.m_cLowerSibling = cTmp.m_cLowerSibling;
						cTmp.m_cLowerSibling = cChild;
						if (cChild.m_cLowerSibling != null)
							cChild.m_cLowerSibling.m_cHigherSibling = cChild;
						else
							m_cBottomChild = cChild;
					}
					else
					{
						m_cTopChild.m_cHigherSibling = cChild;
						cChild.m_cLowerSibling = m_cTopChild;
						m_cTopChild = cChild;
						cChild.m_cHigherSibling = null;
					}
				}
			}
		}
		cChild.setBitmap (m_cBitmap);
		cChild.m_cParent = this;
	
		cChild.added (m_nHideCount);	// Alloc clipping regions
	}	
	void removeChild (WView cChild)
	{
		cChild.m_bIsAdded = false;
		if (cChild.m_cHigherSibling != null)
			cChild.m_cHigherSibling.m_cLowerSibling = cChild.m_cLowerSibling;
		if (cChild.m_cLowerSibling != null)
			cChild.m_cLowerSibling.m_cHigherSibling = cChild.m_cHigherSibling;
		if (cChild == m_cTopChild)
			m_cTopChild = cChild.m_cLowerSibling;
		if (cChild == m_cBottomChild)
			m_cBottomChild = cChild.m_cHigherSibling;
		cChild.setBitmap (null);
		cChild.m_cParent = null;
		cChild.m_cLowerSibling = null;
		cChild.m_cHigherSibling = null;				
	}
	void removeThis()
	{
	  	if (m_cParent != null) 
  			m_cParent.removeChild (this);
	}
	void moveToFront ()
	{
		WView cParent;
		
		//Debug.out.println (m_cName + ": WView::moveToFront()");
		
		if ((cParent = m_cParent) == null)
			return;
		if (m_cParent.m_cTopChild == this)
			return;
		cParent.removeChild (this);
		cParent.addChild (this, true);
		cParent.m_bHasInvalidRegs = true;
		setDirtyRegFlags ();

		WView cSibling;
		PixelRect cRect = new PixelRect ();
		for (cSibling = cParent.m_cBottomChild; cSibling != null; cSibling = cSibling.m_cHigherSibling)
		{
			if (cSibling.m_cFrame.intersects (m_cFrame))
			{
				cRect.m_nX0 = m_cFrame.m_nX0 - cSibling.m_cFrame.m_nX0;
				cRect.m_nY0 = m_cFrame.m_nY0 - cSibling.m_cFrame.m_nY0;
				cRect.m_nX1 = m_cFrame.m_nX1 - cSibling.m_cFrame.m_nX0;
				cRect.m_nY1 = m_cFrame.m_nY1 - cSibling.m_cFrame.m_nY0;
				cSibling.markModified (cRect);
			}				
		}	
	}
	public void setBitmap (WBitmap cBitmap)
	{
			WView cChild;

		 	m_bFontPaletteValid = false;
		  	m_cBitmap = cBitmap;

	  		for (cChild = m_cTopChild; null != cChild; cChild = cChild.m_cLowerSibling) 
		  		cChild.setBitmap (cBitmap);
	}
	public WBitmap getBitmap ()
	{
		return m_cBitmap;
	}
	private void added (int nHideCount)
	{
		WView cChild;

		m_nHideCount += nHideCount;
		if (m_cParent != null)
			m_nLevel = m_cParent.m_nLevel + 1;
		for (cChild = m_cTopChild; null != cChild; cChild = cChild.m_cLowerSibling)
			cChild.added (nHideCount);
	}
	private void hided (int nHideCount)
	{
		WView cChild;

		m_nHideCount -= nHideCount;
		m_nLevel = 0;
		for (cChild = m_cTopChild; null != cChild; cChild = cChild.m_cLowerSibling)
			cChild.hided (nHideCount);
	}
	private void markModified (PixelRect cRect)
	{
		if (getBounds().intersects (cRect)) 
		{
			/*Debug.out.println (m_cName + ": WView::markModified(" + cRect + ")");*/
			m_bHasInvalidRegs = true;
    
			WView cChild;
			for (cChild = m_cBottomChild; null != cChild; cChild = cChild.m_cHigherSibling)
			{
				PixelRect cTmp = new PixelRect (cRect.m_nX0 - cChild.m_cFrame.m_nX0,
							cRect.m_nY0 - cChild.m_cFrame.m_nY0,
							cRect.m_nX1 - cChild.m_cFrame.m_nX0,
							cRect.m_nY1 - cChild.m_cFrame.m_nY0);
	    			cChild.markModified (cTmp);
			}				
  		}
	}
	private void setDirtyRegFlags()
	{
		/*Debug.out.println (m_cName + ": WView::setDirtyRegFlags()");*/
  		m_bHasInvalidRegs = true;
    
  		WView cChild;
  		for (cChild = m_cBottomChild; null != cChild; cChild = cChild.m_cHigherSibling) 
			cChild.setDirtyRegFlags();
	}
	private void clearDirtyRegFlags ()
	{
		/*Debug.out.println (m_cName + ": WView::clearDirtyRegFlags()");*/
		m_bHasInvalidRegs = false;
		for (WView cChild = m_cBottomChild; null != cChild; cChild = cChild.m_cHigherSibling)
			cChild.clearDirtyRegFlags ();
	}

	private void clearRegions ()
	{
		m_cVisibleReg.makeEmpty ();
		m_cFullReg.makeEmpty ();
		m_cDamageReg.makeEmpty ();
		m_cActiveDamageReg.makeEmpty ();
		m_cDrawReg.makeEmpty ();
		m_cPrevVisibleReg.makeEmpty ();
		m_cPrevFullReg.makeEmpty ();

		WView cChild;

		for (cChild = m_cTopChild; null != cChild; cChild = cChild.m_cLowerSibling)
			cChild.clearRegions ();
	}
	private void rebuildRegion (boolean bForce)
	{
	  	WView cSibling;
		WView cChild;
/*		
		Debug.out.println (m_cName + ": WView::rebuildRegion (" + bForce + ") frame: " + m_cFrame + 
		" hasInvalidRegs: " + m_bHasInvalidRegs);
*/		
/*
		if (m_cOwner != null && (m_cOwner.GetDesktopMask () & (1 << get_active_desktop ())) == 0)
			return;
*/

		if (m_nHideCount > 0)
		{
			if (m_cVisibleReg.isEmpty() == false)
				clearRegions ();
			return;
		}

		if (bForce)
			m_bHasInvalidRegs = true;

		if (m_bHasInvalidRegs)
		{
			m_cDrawReg.makeEmpty ();

			WRegion.exchange (m_cPrevVisibleReg, m_cVisibleReg);
			WRegion.exchange (m_cPrevFullReg, m_cFullReg);
		
			if (null == m_cParent)
			{
				m_cFullReg.include (m_cFrame);
			}
			else
			{
				if (m_cParent.m_cFullReg.isEmpty() == false)
				{
					m_cFullReg.includeIntersection (m_cParent.m_cFullReg, m_cFrame, true);
				}
			}
			/*PixelPoint cLeftTop (m_cFrame.LeftTop ());*/
			PixelRect cRect = new PixelRect ();

			for (cSibling = m_cHigherSibling; null != cSibling; cSibling = cSibling.m_cHigherSibling)
			{
				if (cSibling.m_nHideCount == 0)
				{
					if (cSibling.m_cFrame.intersects (m_cFrame))
					{
						cRect.m_nX0 = cSibling.m_cFrame.m_nX0 - m_cFrame.m_nX0;
						cRect.m_nY0 = cSibling.m_cFrame.m_nY0 - m_cFrame.m_nY0;
						cRect.m_nX1 = cSibling.m_cFrame.m_nX1 - m_cFrame.m_nX0;
						cRect.m_nY1 = cSibling.m_cFrame.m_nY1 - m_cFrame.m_nY0;
						/*m_cFullReg.exclude (cSibling.m_cFrame - cLeftTop);*/
						m_cFullReg.exclude (cRect);
					}						
				}
			}
			m_cVisibleReg.include (m_cFullReg);

			if ((m_nFlags & WID_DRAW_ON_CHILDREN) == 0)
			{
				for (cChild = m_cBottomChild; null != cChild; cChild = cChild.m_cHigherSibling)
				{
					/***	Remove children from child region	***/
					if (cChild.m_nHideCount == 0 && (cChild.m_nFlags & WID_TRANSPARENT) == 0)
						m_cVisibleReg.exclude (cChild.m_cFrame);
				}
			}
/*			
			Debug.out.println (m_cName + ": m_cFullReg: " + m_cFullReg);
			Debug.out.println (m_cName + ": m_cVisibleReg: " + m_cVisibleReg);
*/			
		}
		for (cChild = m_cBottomChild; null != cChild; cChild = cChild.m_cHigherSibling)
			cChild.rebuildRegion (bForce);
	}

	private void clearRegion (WRegion cDamage)
	{
	  	PixelRect acRect[] = new PixelRect[16];
	  	int nCount;
  		PixelPoint	 cTopLeft = convertToRoot (new PixelPoint (0,0));

		if (m_cParent==null || m_cBitmap != m_cParent.m_cBitmap)
			cTopLeft.setTo (0, 0);
		WRegion cTmpReg = new WRegion (getBounds());
		WRegion cDrawReg = new WRegion (m_cVisibleReg);		
	
		cTmpReg.exclude (cDamage);
		cDrawReg.exclude (cTmpReg);
/*		
		Debug.out.println (m_cName + ": WView::clearRegion(" + cDamage + ") ");		
		Debug.out.println (m_cName + ": cDrawReg: " + cDrawReg);
*/
  		nCount = 0;
		for (int i = 0; i < cDrawReg.countRects(); i++)
		{	
			PixelRect cTmp = cDrawReg.rectAt (i);
			
			if (acRect[nCount] != null)									
	    			acRect[nCount].setTo (cTmp);
			else				
				acRect[nCount] = new PixelRect (cTmp);
			acRect[nCount].add (cTopLeft);
			nCount++;
    			if (nCount == 16)
    			{
      				m_cBitmap.fillRect (acRect, nCount, m_cEraseColor, new DrawingMode (DrawingMode.DM_COPY));
      				nCount = 0;
    			}        
		}
  		if (nCount != 0)
   			m_cBitmap.fillRect (acRect, nCount, m_cEraseColor, new DrawingMode (DrawingMode.DM_COPY));
	}
	private void invalidateNewAreas ()
	{
		WRegion cRegion = new WRegion ();
		
		/*Debug.out.println (m_cName + ": WView::invalidateNewAreas() hasInvalidRegs: " + m_bHasInvalidRegs);*/
		
		if (m_nHideCount > 0)
			return;
/*			
		if (m_cOwner != null && (m_cOwner.GetDesktopMask () & (1 << get_active_desktop ())) == 0)
			return;
*/
		
		if (m_bHasInvalidRegs)
		{
			if (((m_nFlags & WID_FULL_UPDATE_ON_H_RESIZE) != 0 && m_cDeltaSize.m_nX != 0) ||
				((m_nFlags & WID_FULL_UPDATE_ON_V_RESIZE) != 0 && m_cDeltaSize.m_nY != 0))
			{
				invalidate (false);
				if ((m_nFlags & WID_CLEAR_BACKGROUND) != 0)
					clearRegion (m_cDamageReg);
			}
			else
			{
				if (m_cVisibleReg.isEmpty() == false)
				{
					cRegion.include (m_cVisibleReg);
		
					if (m_cPrevVisibleReg.isEmpty() == false)
						cRegion.exclude (m_cPrevVisibleReg);
					
					/*Debug.out.println (m_cName + ": cRegion: " + cRegion);	*/

					if (m_cDamageReg.isEmpty() == true)
					{
						if (cRegion.isEmpty () == false)
							WRegion.exchange (m_cDamageReg, cRegion);
						else
							cRegion.makeEmpty ();
						if ((m_nFlags & WID_CLEAR_BACKGROUND) != 0)
							clearRegion (m_cDamageReg);
					}
					else
					{
						for (int i = 0; i < cRegion.countRects(); i++)
						{
							invalidate (cRegion.rectAt (i));
						}
						if ((m_nFlags & WID_CLEAR_BACKGROUND) != 0)
							clearRegion (cRegion);
						cRegion.makeEmpty ();
					}
				}
			}
			m_cPrevVisibleReg.makeEmpty ();
			m_cDeltaSize.setTo (0, 0);
			m_cDeltaMove.setTo (0, 0);
		}
		for (WView cChild = m_cBottomChild; null != cChild; cChild = cChild.m_cHigherSibling)
		{
			cChild.invalidateNewAreas ();
		}
	}
	protected void paint (WRegion cRegion, boolean bUpdate)
	{
		/*Debug.out.println (m_cName + ": WView::paint(" + cRegion + ")");*/
		if (m_nHideCount > 0 || m_bIsUpdating)
			return;
		if (m_cOwner != null && cRegion.countRects() != 0)
		{
			WPaintMessage cMsg = new WPaintMessage (cRegion);
			m_cOwner.postMessage (cMsg);
		}
	}
	public void invalidate (PixelRect cRect)
	{
		/*Debug.out.println (m_cName + ": WView::invalidate(" + cRect + ")");*/
		if (m_nHideCount == 0)
			m_cDamageReg.include (cRect);
	}
	public void invalidate (boolean bRecursive)
	{
		/*Debug.out.println (m_cName + ": WView::invalidate(" + bRecursive + ")");*/
		if (m_nHideCount == 0)
		{
			m_cDamageReg.makeEmpty ();
			m_cDamageReg.include (getBounds ());

			if (bRecursive)
			{
				for (WView cChild = m_cBottomChild; null != cChild; cChild = cChild.m_cHigherSibling)
				{
					cChild.invalidate (true);
				}
			}
		}
	}
	public void invalidate ()
	{
		invalidate (false);
	}
	private void updateIfNeeded ()
	{
		WView cChild;
		/*Debug.out.println (m_cName + ": WView::updateIfNeeded() " + m_nHideCount);*/
		if (m_nHideCount > 0)
			return;	
/*			
	  	if (m_cOwner != null && (m_cOwner.GetDesktopMask() & (1 << get_active_desktop())) == 0) 
			return;
*/			
  		if (!m_cDamageReg.isEmpty() && m_cActiveDamageReg.isEmpty())
  		{
			WRegion.exchange (m_cActiveDamageReg, m_cDamageReg);
			if (m_cActiveDamageReg.countRects() != 0)
				paint (m_cActiveDamageReg, true);
		    	/*Paint (m_cActiveDamageReg.Bounds(), true);*/
	  	}
		for (cChild = m_cBottomChild; null != cChild; cChild = cChild.m_cHigherSibling) 
  			cChild.updateIfNeeded ();
	}
	public void updateRegions (boolean bForce)
	{
		/*Debug.out.println (m_cName + ": WView::updateRegions() " + bForce);*/
		PixelRect cBounds = getBounds();
		cBounds.add (convertToRoot (new PixelPoint (0, 0)));
		if (m_cOwner == null)
			WSprite.hide (cBounds);
		rebuildRegion (bForce);
		moveChilds ();
		invalidateNewAreas ();
		if (m_cOwner == null)
			WSprite.unhide ();
		clearDirtyRegFlags ();
		updateIfNeeded ();
	}
	public void beginUpdate ()
	{
		if (!m_cVisibleReg.isEmpty())
			m_bIsUpdating = true;
	}
	public void endUpdate ()	
	{
		m_cActiveDamageReg.makeEmpty();
		m_cDrawReg.makeEmpty();
		m_bIsUpdating = false;
  
		if (!m_cDamageReg.isEmpty())
		{
			updateIfNeeded ();
			//m_pcActiveDamageReg = m_pcDamageReg;
			//m_pcDamageReg = NULL;
			//Paint( static_cast<Rect>(m_pcActiveDamageReg->GetBounds()), true );
	    }
	}
	private WRegion getRegion ()
	{
		/*
		if (m_cDrawReg.isEmpty() && !m_cVisibleReg.isEmpty())
		{
			if (m_cUserClipReg.isEmpty())
			{
				m_cDrawReg.include (m_cVisibleReg);
			}
			else
			{
				WRegion cTmp = new WRegion (m_cVisibleReg);
				cTmp.exclude (m_cUserClipReg);
				m_cDrawReg.include (m_cVisibleReg);
				m_cDrawReg.exclude (cTmp);
			}
		}
		return m_cDrawReg;
		*/
	    if (m_nHideCount > 0)
			return null;
	    if (m_bIsUpdating && m_cActiveDamageReg.isEmpty()) 
			return null;

    	if (!m_bIsUpdating) 
		{
			if (m_cUserClipReg.isEmpty()) 
			{
	    		return m_cVisibleReg;
			} 
			else 
			{
			    if (m_cDrawReg.isEmpty()) 
				{
					WRegion cTmp = new WRegion (m_cVisibleReg);
					cTmp.exclude (m_cUserClipReg);
					m_cDrawReg.include (m_cVisibleReg);
					m_cDrawReg.exclude (cTmp);
	    		}
			}
    	} 
		else if (m_cDrawReg.isEmpty() && !m_cVisibleReg.isEmpty()) 
		{
			m_cDrawReg.include (m_cVisibleReg);
			m_cDrawReg.intersect (m_cActiveDamageReg);
			if (!m_cUserClipReg.isEmpty())
				m_cDrawReg.intersect (m_cUserClipReg);
    	}
    	return m_cDrawReg;
	}
	void moveChilds ()
	{
		ClippingRect acClips[] = new ClippingRect[32];
		
		/*Debug.out.println (m_cName + ": WView::moveChilds() hasInvalidRegs: " + m_bHasInvalidRegs);*/
	
		if (m_nHideCount > 0 || null == m_cBitmap)
			return;
/*TODO
		if (m_cOwner != null && (m_cOwner.GetDesktopMask () & (1 << get_active_desktop ())) == 0)
			return;
*/
		WView cChild;
	
/*	dbprintf ("%s: MoveChilds\n",m_pzName);*/

		if (m_bHasInvalidRegs)
		{
			PixelRect cBounds = getBounds ();
			PixelPoint cTopLeft = new PixelPoint ();
			PixelPoint cChildOffset = new PixelPoint ();
			PixelPoint cChildMove = new PixelPoint ();

			for (cChild = m_cBottomChild; null != cChild; cChild = cChild.m_cHigherSibling)
			{
				if (cChild.m_cDeltaMove.m_nX == 0 && cChild.m_cDeltaMove.m_nY == 0)
					continue;
				if (cChild.m_cFullReg.isEmpty() == true || 
				    cChild.m_cPrevFullReg.isEmpty() == true)
					continue;

				WRegion cRegion = new WRegion (cChild.m_cPrevFullReg);

				cRegion.intersect (cChild.m_cFullReg);

				PixelRect cClip;
				int nCount = 0;
				
				cTopLeft.setTo (0, 0);
				cTopLeft = convertToRoot (cTopLeft);
				if (m_cParent==null || m_cBitmap != m_cParent.m_cBitmap)
					cTopLeft.setTo (0, 0);
				cChildOffset.setTo (cChild.m_cFrame.m_nX0 + cTopLeft.m_nX,
						    cChild.m_cFrame.m_nY0 + cTopLeft.m_nY);
				cChildMove.setTo (cChild.m_cDeltaMove);
/*										    
				PixelPoint cTopLeft = convertToRoot (new PixelPoint (0, 0));
				PixelPoint cChildOffset = new PixelPoint (cChild.m_cFrame.LeftTop() + cTopLeft);
				PixelPoint cChildMove = new PixelPoint (cChild.m_cDeltaMove);
*/
				for (int i = 0; i < cRegion.countRects(); i++)
				{
					cClip = cRegion.rectAt (i);
					if (acClips[nCount] != null)
						acClips[nCount].setTo (cClip);
					else
						acClips[nCount] = new ClippingRect (cClip);				
					acClips[nCount].m_nX0 += cChildOffset.m_nX;
					acClips[nCount].m_nY0 += cChildOffset.m_nY;				
					acClips[nCount].m_nX1 += cChildOffset.m_nX;
					acClips[nCount].m_nY1 += cChildOffset.m_nY;				
					acClips[nCount].m_nMoveX = cChildMove.m_nX;
					acClips[nCount].m_nMoveY = cChildMove.m_nY;
					nCount++;
				}
				if (nCount == 0)
				{
					cRegion.makeEmpty ();
					continue;
				}
				Vector cVector = new Vector (nCount);
				for (int i = 0; i < nCount; i++)
					cVector.addElement (acClips[i]);
				QuickSort qsort = new QuickSort (new SortCmp(), cVector);
				//qsort (acClips, nCount, sizeof (ClipRect), SortCmp);
				
				for (int i = 0; i < nCount; ++i)
				{
				    	int nBltCount = 0;
				    	/*ClippingRect cNewPos[MAX_BITBLT_CT], cOldPos[MAX_BITBLT_CT];*/
					PixelRect cNewPos[] = new PixelRect[16];
					PixelRect cOldPos[] = new PixelRect[16];
    
				    	while (i < nCount && nBltCount < 16)
				    	{
		    				ClippingRect cClipRect = acClips[i];
						
						if (cNewPos[nBltCount] == null)
							cNewPos[nBltCount] = new PixelRect ();
						if (cOldPos[nBltCount] == null)
							cOldPos[nBltCount] = new PixelRect ();
			
						cClipRect.m_nX0 += cTopLeft.m_nX;
						cClipRect.m_nY0 += cTopLeft.m_nY;
						cClipRect.m_nX1 += cTopLeft.m_nX;
						cClipRect.m_nY1 += cTopLeft.m_nY;
						cOldPos[nBltCount].m_nX0 = cClipRect.m_nX0 - cClipRect.m_nMoveX;
						cOldPos[nBltCount].m_nY0 = cClipRect.m_nY0 - cClipRect.m_nMoveY;
						cOldPos[nBltCount].m_nX1 = cClipRect.m_nX1 - cClipRect.m_nMoveX;
						cOldPos[nBltCount].m_nY1 = cClipRect.m_nY1 - cClipRect.m_nMoveY;
						cNewPos[nBltCount].m_nX0 = cClipRect.m_nX0;
						cNewPos[nBltCount].m_nY0 = cClipRect.m_nY0;
						cNewPos[nBltCount].m_nX1 = cClipRect.m_nX1;
						cNewPos[nBltCount].m_nY1 = cClipRect.m_nY1;
				      		nBltCount++;    
					      	i++;
		    			}  
		    			m_cBitmap.bitBlt (cOldPos, cNewPos, nBltCount);
				}
				cRegion.makeEmpty ();
			}
			/*
			 * Since the parent window is shrinked before the childs is moved
			 * *  we may need to redraw the right and bottom edges.
			 */
			if (null != m_cParent && (m_cDeltaMove.m_nX != 0 || m_cDeltaMove.m_nY != 0))
			{
				if (m_cParent.m_cDeltaSize.m_nX < 0)
				{
					PixelRect cRect = cBounds;

					cRect.m_nX0 =
						cRect.m_nX1 + (m_cParent.m_cDeltaSize.m_nX +
								 m_cParent.m_cFrame.m_nX1 -
								 m_cParent.m_cFrame.m_nX0 - m_cFrame.m_nX1);

					if (cRect.isValid ())
					{
						invalidate (cRect);
						if ((m_nFlags & WID_CLEAR_BACKGROUND) != 0)
							clearRegion (new WRegion (cRect));
					}					
				}
				if (m_cParent.m_cDeltaSize.m_nY < 0)
				{
					PixelRect cRect = cBounds;

					cRect.m_nY0 =
						cRect.m_nY1 + (m_cParent.m_cDeltaSize.m_nY +
								 m_cParent.m_cFrame.m_nY1 -
								 m_cParent.m_cFrame.m_nY0 - m_cFrame.m_nY1);

					if (cRect.isValid ())
					{
						invalidate (cRect);
						if ((m_nFlags & WID_CLEAR_BACKGROUND) != 0)
							clearRegion (new WRegion (cRect));
					}						
				}
			}
			m_cPrevFullReg.makeEmpty ();
		}
		for (cChild = m_cBottomChild; null != cChild; cChild = cChild.m_cHigherSibling)
			cChild.moveChilds ();
	}
	WView getParent ()
	{
		return m_cParent;
	}
	WWindowImpl getWindow ()
	{
		return m_cOwner;
	}
	WView getChildAt (PixelPoint cPos)
	{
		WView cChild;

		for (cChild = m_cTopChild; cChild != null; cChild = cChild.m_cLowerSibling)
		{
			if (cChild.m_nHideCount > 0)
				continue;
			if (cChild.m_cFrame.contains (cPos))
				return cChild;
		}
		return null;
	}
	PixelPoint getLeftTop ()
	{
		return new PixelPoint (m_cFrame.m_nX0, m_cFrame.m_nY0);
	}
	boolean toggleDepth ()
	{
		if (m_cParent != null)
		{
			WView cParent = m_cParent;

			if (cParent.m_cTopChild == this)
			{
				cParent.removeChild (this);
				cParent.addChild (this, false);
			}
			else
			{
				cParent.removeChild (this);
				cParent.addChild (this, true);
			}

			m_cParent.m_bHasInvalidRegs = true;
			setDirtyRegFlags ();

			WView cSibling;
			PixelRect cFrame = new PixelRect ();

			for (cSibling = m_cParent.m_cBottomChild; cSibling != null; cSibling = cSibling.m_cHigherSibling)
			{
				if (cSibling.m_cFrame.intersects (m_cFrame))
				{
					cFrame.setTo (m_cFrame);
					cFrame.sub (cSibling.m_cFrame.m_nX0, cSibling.m_cFrame.m_nY0);
					cSibling.markModified (cFrame);
				}
			}
		}
		return (false);
	}

}
