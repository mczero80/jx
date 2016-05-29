package jx.wm;

import jx.wm.Glyph;
import jx.wm.WFontHeight;
import jx.zero.*;
import jx.devices.fb.*;

public class WFont
{
	int 	m_nAscender;
	int	m_nDescender;
	int	m_nLineGap;
	static Glyph	s_acGlyphs[] = new Glyph[256];
	public static final int	NUM_FONT_GRAYS = 2;
	
	public class FontHeight
	{
  		public int	nAscender;  // Pixels from baseline to top of glyph (positive)
  		public int	nDescender; // Pixels from baseline to bottom of glyph (negative)
  		public int	nLineGap;   // Space between lines (positive)
		
		public FontHeight ()
		{
		}
		public int ascender()
		{
			return nAscender;
		}
		public int descender()
		{
			return nDescender;
		}
		public int lineGap()
		{
			return nLineGap;
		}
	};
	public WFont ()
	{
		m_nAscender  = 9;
		m_nDescender = 4;
		m_nLineGap   = 1;
	}
	public int ascender ()
	{
		return m_nAscender;
	}
	public int descender ()
	{
		return m_nDescender;
	}
	public int lineGap ()
	{
		return m_nLineGap;
	}
	public void getFontHeight (WFontHeight cFontHeight)
	{
		cFontHeight.nAscender 	= m_nAscender;
		cFontHeight.nDescender 	= m_nDescender;
		cFontHeight.nLineGap 	= m_nLineGap;
	}
	public Glyph getGlyph (int nIndex)
	{
		nIndex &= 0x7fffffff;
		if (nIndex >= s_acGlyphs.length)
			return null;
		return s_acGlyphs[nIndex];
	}
	static public void scanFonts ()
	{	
		Naming naming = InitialNaming.getInitialNaming();
		Debug.out.println ("WFont::scanFonts()");
		BootFS cBootFS = (BootFS)naming.lookup ("BootFS");
		if (cBootFS == null)
		{
			Debug.out.println ("WFont::scanFont() unable to find BootFS");
			return;
		}
		ReadOnlyMemory cDefaultFont = cBootFS.getFile ("default.fon");
		if (cDefaultFont == null)
		{
			Debug.out.println ("WFont::scanFonts() unable to open 'default.fon'");
			return;
		}
		for (int i = 0; i < 256; i++)
		{
			s_acGlyphs[i] = new Glyph();
			s_acGlyphs[i].m_cBounds = new PixelRect (0, 0, 9, 14);
			s_acGlyphs[i].m_nAdvance = 9;
			s_acGlyphs[i].m_nBytesPerLine = 9;
			s_acGlyphs[i].m_anRaster = new byte[9 * 14];
			//Debug.out.println ("lineOffset for " + i + ": " + ((i / 16) * (14 * 9 * 16) + ((i % 16) * 9)));
			for (int y = 0; y < 14; y++)
			{
				int lineOffset = (i / 16) * (14 * 9 * 16) + ((i % 16) * 9) + (y * 9 * 16);				
				for (int x = 0; x < 9; x ++)
				{
					s_acGlyphs[i].m_anRaster[y*9 + x] =
						cDefaultFont.get8 (lineOffset + x);
				}
			}
		}
	}
	public String toString ()
	{
		return new String ("WFont(default.fon)");
	}
};
