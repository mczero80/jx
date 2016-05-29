package jx.wm;

import jx.zero.*;
import jx.devices.fb.*;

public class WRegion
{
	private PixelRect m_acClips[];
	private PixelRect m_cBounds;
	private int m_nNumClipRect;

	public WRegion ()
	{
		m_cBounds = new PixelRect ();
		m_cBounds.m_nX0 = 99999999;
		m_cBounds.m_nY0 = 99999999;
		m_cBounds.m_nX1 = -99999999;
		m_cBounds.m_nY1 = -99999999;
		m_nNumClipRect = 0;
	}
	public WRegion(PixelRect cRect)
	{
		m_nNumClipRect = 1;
		m_acClips = new PixelRect[1];
		m_acClips[0] = new PixelRect (cRect);
		m_cBounds = new PixelRect ();
		m_cBounds.m_nX0 = cRect.m_nX0;
		m_cBounds.m_nY0 = cRect.m_nY0;
		m_cBounds.m_nX1 = cRect.m_nX1;
		m_cBounds.m_nY1 = cRect.m_nY1;
	}
	public WRegion (WRegion cRegion)
	{
		m_acClips = null;
		m_cBounds = new PixelRect ();
		m_cBounds.m_nX0 = 99999999;
		m_cBounds.m_nY0 = 99999999;
		m_cBounds.m_nX1 = -99999999;
		m_cBounds.m_nY1 = -99999999;
		m_nNumClipRect  = 0;
		include (cRegion);
	}
	public void makeEmpty ()
	{
		m_acClips = null;
		m_nNumClipRect = 0;
		m_cBounds.m_nX0 = 99999999;
		m_cBounds.m_nY0 = 99999999;
		m_cBounds.m_nX1 = -99999999;
		m_cBounds.m_nY1 = -99999999;
	}
	private void AddClipRect (PixelRect cClip)
	{
		PixelRect acClips[] = m_acClips;
		m_acClips = new PixelRect[m_nNumClipRect + 1];
		for (int i = 0; i < m_nNumClipRect; i++)
			 m_acClips[i] = acClips[i];
		 m_acClips[m_nNumClipRect++] = cClip;
	}
	private void RemClipRect (int nPos)
	{
		if (nPos >= m_nNumClipRect)
			return;
		PixelRect acClips[] = m_acClips;
		m_acClips = new PixelRect[m_nNumClipRect - 1];
		for (int i = 0; i < nPos; i++)
			 m_acClips[i] = acClips[i];
		for (int i = nPos + 1; i < m_nNumClipRect; i++)
			 m_acClips[i - 1] = acClips[i];
		m_nNumClipRect--;
	}
	private void RemClipRect (PixelRect cClip)
	{
		int nPos;

		for (nPos = 0; nPos < m_nNumClipRect; nPos++)
			if (m_acClips[nPos] == cClip)
				 break;
		if (nPos == m_nNumClipRect)
			 return;
		 RemClipRect (nPos);
	}
	private void Optimize ()
	{
		for (int i = 0; i < m_nNumClipRect; i++)
		{
			PixelRect cClip = m_acClips[i];
			for (int j = i + 1; j < m_nNumClipRect; )
			{
				PixelRect cTmp = m_acClips[j];
				if (cClip.m_nY0 == cTmp.m_nY1 + 1 &&
						cClip.m_nX0 == cTmp.m_nX0 && cClip.m_nX1 == cTmp.m_nX1)
				{
					cClip.m_nY0 = cTmp.m_nY0;
					RemClipRect (j);
					continue;
				}
				if (cClip.m_nY1 == cTmp.m_nY0 - 1 &&
						cClip.m_nX0 == cTmp.m_nX0 && cClip.m_nX1 == cTmp.m_nX1)
				{
					cClip.m_nY1 = cTmp.m_nY1;
					RemClipRect (j);
					continue;
				}
				if (cClip.m_nX0 == cTmp.m_nX1 + 1 &&
						cClip.m_nY0 == cTmp.m_nY0 && cClip.m_nY1 == cTmp.m_nY1)
				{
					cClip.m_nX0 = cTmp.m_nX0;
					RemClipRect (j);
					continue;
				}
				if (cClip.m_nX1 == cTmp.m_nX0 - 1 &&
						cClip.m_nY0 == cTmp.m_nY0 && cClip.m_nY1 == cTmp.m_nY1)
				{
					cClip.m_nX1 = cTmp.m_nX1;
					RemClipRect (j);
					continue;
				}
				j++;
			}
		}
	}
	private void CalculateBounds ()
	{
		/*
	 	* Bounds neu berechnen
	 	*/
		m_cBounds.m_nX0 = 9999999;
		m_cBounds.m_nY0 = 9999999;
		m_cBounds.m_nX1 = -9999999;
		m_cBounds.m_nY1 = -9999999;
	
		for (int i = 0; i < m_nNumClipRect; i++)
			m_cBounds.include (m_acClips[i]);
	}
	public void exclude (PixelRect cRect)
	{
		int i = 0;
		
		//for (i = 0; i < m_nNumClipRect; i++)
		while (i < m_nNumClipRect)
		{
			PixelRect cClip = m_acClips[i];
			PixelRect cOutRect;
			int code = 0, p0, p1, p2, p3;
			if (cRect.m_nX1 < cClip.m_nX0 || cRect.m_nX0 > cClip.m_nX1 ||
					 cRect.m_nY1 < cClip.m_nY0 || cRect.m_nY0 > cClip.m_nY1)
			{
				i++;					 
				 continue;
			}
			/*
			 * Ueberpruefen, ob sich die Rechtecke schneiden 
			 */ 
			if (cRect.m_nX0 <= cClip.m_nX0)
				 p0 = 0;
			
			else
				 p0 = 0x1000;
			if (cRect.m_nX1 < cClip.m_nX1)
				 p1 = 0;
			
			else
				 p1 = 0x100;
			if (cRect.m_nY0 <= cClip.m_nY0)
				 p2 = 0;
			
			else
				 p2 = 0x10;
			if (cRect.m_nY1 < cClip.m_nY1)
				 p3 = 0;
			
			else
				 p3 = 0x1;
			code = p0 + p1 + p2 + p3;
			
			switch (code)
			{
				
					/*
					 * Nur ein Rechteck entsteht 
					 * +---------------+
					 * | +-----------+ |
					 * +---------------+
					 * |           |
					 * +-----------+
					 */ 
			case 0x0100:
				{
					cClip.m_nY0 = cRect.m_nY1 + 1;
					i++;
					continue;
				}
				
					/*
					 * Nur ein Rechteck entsteht 
					 * +---+
					 * +---------|-+ |
					 * |         | | |
					 * |         | | |
					 * +---------|-+ |
					 * +---+
					 */ 
					/*
					 * if (code == 0x1101)
					 */
				case 0x1101:
				{
					cClip.m_nX1 = cRect.m_nX0 - 1;
					i++;
					continue;
				}
				
					/*
					 * Nur ein Rechteck entsteht 
					 * +-----------+ 
					 * |           |
					 * +---------------+
					 * | +-----------+ |
					 * +---------------+
					 */ 
			case 0x0111:
				{
					cClip.m_nY1 = cRect.m_nY0 - 1;
					i++;
					continue;
				}
				
					/*
					 * Nur ein Rechteck entsteht 
					 * +---+           
					 * | +-|---------+
					 * | | |         | 
					 * | | |         | 
					 * | +-|---------+
					 * +---+
					 */ 
			case 0x0001:
				{
					cClip.m_nX0 = cRect.m_nX1 + 1;
					i++;
					continue;
				}
				
					/*
					 * Zwei Rechtecke entstehen
					 * +---+
					 * +-----------+ 
					 * |   |   |   |
					 * |   |   |   |
					 * +-----------+ 
					 * +---+
					 */ 
			case 0x0110:
				{
					cOutRect = new PixelRect ();
					cOutRect.m_nX0 = cClip.m_nX0;
					cOutRect.m_nY0 = cRect.m_nY1 + 1;
					cOutRect.m_nX1 = cClip.m_nX1;
					cOutRect.m_nY1 = cClip.m_nY1;
					cClip.m_nY1 = cRect.m_nY0 - 1;
					AddClipRect (cOutRect);
					i++;
					continue;
				}
				
					/*
					 * Zwei Rechtecke entstehen 
					 * +-----------+ 
					 * +-|-----------|-+
					 * | |           | |
					 * +-|-----------|-+
					 * +-----------+ 
					 */ 
			case 0x1001:
				{
					cOutRect = new PixelRect ();
					cOutRect.m_nX0 = cRect.m_nX1 + 1;
					cOutRect.m_nY0 = cClip.m_nY0;
					cOutRect.m_nX1 = cClip.m_nX1;
					cOutRect.m_nY1 = cClip.m_nY1;
					cClip.m_nX1 = cRect.m_nX0 - 1;
					AddClipRect (cOutRect);
					i++;
					continue;
				}
				
					/*
					 * Zwei Rechtecke entstehen
					 * +---+
					 * | +-|---------+
					 * +---+         |
					 * |           |
					 * +-----------+
					 */ 
			case 0x0000:
				{
					cOutRect = new PixelRect ();
					cOutRect.m_nX0 = cRect.m_nX1 + 1;
					cOutRect.m_nY0 = cClip.m_nY0;
					cOutRect.m_nX1 = cClip.m_nX1;
					cOutRect.m_nY1 = cRect.m_nY1;
					AddClipRect (cOutRect);
					cClip.m_nY0 = cRect.m_nY1 + 1;
					i++;
					continue;
				}
				
					/*
					 * 
					 * +---+
					 * +---------|-+ |
					 * |         +---+
					 * |           |
					 * +-----------+
					 */ 
			case 0x1100:
				{
					cOutRect = new PixelRect ();
					cOutRect.m_nX0 = cClip.m_nX0;
					cOutRect.m_nY0 = cClip.m_nY0;
					cOutRect.m_nX1 = cRect.m_nX0 - 1;
					cOutRect.m_nY1 = cRect.m_nY1;
					AddClipRect (cOutRect);
					cClip.m_nY0 = cRect.m_nY1 + 1;
					i++;
					continue;
				}
				
					/*
					 * 
					 * +-----------+ 
					 * |           |
					 * |         +---+
					 * +---------|-+ |
					 * +---+
					 */ 
			case 0x1111:
				{
					cOutRect = new PixelRect ();
					cOutRect.m_nX0 = cClip.m_nX0;
					cOutRect.m_nY0 = cRect.m_nY0;
					cOutRect.m_nX1 = cRect.m_nX0 - 1;
					cOutRect.m_nY1 = cClip.m_nY1;
					AddClipRect (cOutRect);
					cClip.m_nY1 = cRect.m_nY0 - 1;
					i++;
					continue;
				}
				
					/*
					 * 
					 * +-----------+ 
					 * |           |
					 * +---+         |         
					 * | +-|---------+ 
					 * +---+            
					 */ 
			case 0x0011:
				{
					cOutRect = new PixelRect ();
					cOutRect.m_nX0 = cRect.m_nX1 + 1;
					cOutRect.m_nY0 = cRect.m_nY0;
					cOutRect.m_nX1 = cClip.m_nX1;
					cOutRect.m_nY1 = cClip.m_nY1;
					AddClipRect (cOutRect);
					cClip.m_nY1 = cRect.m_nY0 - 1;
					i++;
					continue;
				}
				
					/*
					 * Drei Rechtecke entstehen 
					 * +---+
					 * +---|---|---+
					 * |   +---+   |
					 * |           |
					 * +-----------+
					 */ 
			case 0x1000:
				{
					cOutRect = new PixelRect ();
					cOutRect.m_nX0 = cClip.m_nX0;
					cOutRect.m_nY0 = cClip.m_nY0;
					cOutRect.m_nX1 = cRect.m_nX0 - 1;
					cOutRect.m_nY1 = cRect.m_nY1;
					AddClipRect (cOutRect);

					cOutRect = new PixelRect ();
					cOutRect.m_nX0 = cRect.m_nX1 + 1;
					cOutRect.m_nY0 = cClip.m_nY0;
					cOutRect.m_nX1 = cClip.m_nX1;
					cOutRect.m_nY1 = cRect.m_nY1;
					AddClipRect (cOutRect);

					cClip.m_nY0 = cRect.m_nY1 + 1;
					i++;
					continue;
				}
				
					/*
					 * Drei Rechtecke entstehen           
					 * +-----------+
					 * |         +---+
					 * |         | | |
					 * |         +---+ 
					 * +-----------+
					 */ 
			case 0x1110:
				{
					cOutRect = new PixelRect ();
					cOutRect.m_nX0 = cClip.m_nX0;
					cOutRect.m_nY0 = cRect.m_nY0;
					cOutRect.m_nX1 = cRect.m_nX0 - 1;
					cOutRect.m_nY1 = cRect.m_nY1;
					AddClipRect (cOutRect);

					cOutRect = new PixelRect ();
					cOutRect.m_nX0 = cClip.m_nX0;
					cOutRect.m_nY0 = cRect.m_nY1 + 1;
					cOutRect.m_nX1 = cClip.m_nX1;
					cOutRect.m_nY1 = cClip.m_nY1;
					AddClipRect (cOutRect);

					cClip.m_nY1 = cRect.m_nY0 - 1;
					i++;
					continue;
				}
				
					/*
					 * Drei Rechtecke entstehen           
					 * +-----------+
					 * |           |
					 * |   +---+   |
					 * +---|---|---+
					 * +---+
					 */ 
			case 0x1011:
				{
					cOutRect = new PixelRect ();
					cOutRect.m_nX0 = cClip.m_nX0;
					cOutRect.m_nY0 = cRect.m_nY0;
					cOutRect.m_nX1 = cRect.m_nX0 - 1;
					cOutRect.m_nY1 = cClip.m_nY1;
					AddClipRect (cOutRect);

					cOutRect = new PixelRect ();
					cOutRect.m_nX0 = cRect.m_nX1 + 1;
					cOutRect.m_nY0 = cRect.m_nY0;
					cOutRect.m_nX1 = cClip.m_nX1;
					cOutRect.m_nY1 = cClip.m_nY1;
					AddClipRect (cOutRect);

					cClip.m_nY1 = cRect.m_nY0 - 1;
					i++;
					continue;
				}
				
					/*
					 * Drei Rechtecke entstehen           
					 * +-----------+
					 * +---+         |         
					 * | | |         |         
					 * +---+         |          
					 * +-----------+
					 */ 
			case 0x0010:
				{
					cOutRect = new PixelRect ();
					cOutRect.m_nX0 = cRect.m_nX1 + 1;
					cOutRect.m_nY0 = cRect.m_nY0;
					cOutRect.m_nX1 = cClip.m_nX1;
					cOutRect.m_nY1 = cRect.m_nY1;
					AddClipRect (cOutRect);

					cOutRect = new PixelRect ();
					cOutRect.m_nX0 = cClip.m_nX0;
					cOutRect.m_nY0 = cRect.m_nY1 + 1;
					cOutRect.m_nX1 = cClip.m_nX1;
					cOutRect.m_nY1 = cClip.m_nY1;
					AddClipRect (cOutRect);

					cClip.m_nY1 = cRect.m_nY0 - 1;
					i++;
					continue;
				}
				
					/*
					 * Vier Rechtecke entstehen           
					 * +-----------+
					 * |   +---+   |         
					 * |   |   |   |         
					 * |   +---+   |         
					 * +-----------+
					 */ 
			case 0x1010:
				{
					cOutRect = new PixelRect ();
					cOutRect.m_nX0 = cClip.m_nX0;
					cOutRect.m_nY0 = cRect.m_nY0;
					cOutRect.m_nX1 = cRect.m_nX0 - 1;
					cOutRect.m_nY1 = cRect.m_nY1;
					AddClipRect (cOutRect);

					cOutRect = new PixelRect ();
					cOutRect.m_nX0 = cRect.m_nX1 + 1;
					cOutRect.m_nY0 = cRect.m_nY0;
					cOutRect.m_nX1 = cClip.m_nX1;
					cOutRect.m_nY1 = cRect.m_nY1;
					AddClipRect (cOutRect);

					cOutRect = new PixelRect ();
					cOutRect.m_nX0 = cClip.m_nX0;
					cOutRect.m_nY0 = cRect.m_nY1 + 1;
					cOutRect.m_nX1 = cClip.m_nX1;
					cOutRect.m_nY1 = cClip.m_nY1;
					AddClipRect (cOutRect);

					cClip.m_nY1 = cRect.m_nY0 - 1;
					i++;
					continue;
				}
				
					/*
					 * Rechteck wird komplett berdeckt 
					 * +---------------+
					 * | +-----------+ |
					 * | |           | |        
					 * | |           | |        
					 * | |           | |         
					 * | +-----------+ |
					 * +---------------+
					 */ 
			case 0x0101:
				{
					RemClipRect (i);
					continue;
				}
			}
		}
		Optimize ();
		CalculateBounds ();
	}
	public void exclude (WRegion cRegion)
	{
		for (int i = 0; i < cRegion.m_nNumClipRect; i++)
			exclude (cRegion.m_acClips[i]);
	}
	public void include (PixelRect cRect)
	{
		PixelRect cBounds = new PixelRect (m_cBounds);
		cBounds.include (cRect);
		WRegion cRegion = new WRegion (cBounds);
		PixelRect cClip;

		/*
		 * "Inverse" Menge ermitteln
		 */	
		for (int i = 0; i < m_nNumClipRect; i++)
			cRegion.exclude (m_acClips[i]);
		cRegion.exclude (cRect);
		makeEmpty ();
		cClip = new PixelRect (cBounds.m_nX0, cBounds.m_nY0, cBounds.m_nX1, cBounds.m_nY1);
		AddClipRect (cClip);		
		for (int i = 0; i < cRegion.m_nNumClipRect; i++)
			exclude (cRegion.m_acClips[i]);
		CalculateBounds ();		
	}
	public void include (WRegion cRegion)
	{
		for (int i = 0; i < cRegion.m_nNumClipRect; i++)
			include (cRegion.m_acClips[i]);
	}
	public String toString ()
	{
		String cString = new String ("WRegion " + m_nNumClipRect + " [");
		for (int i = 0; i < m_nNumClipRect; i++)
		{
			if (i == m_nNumClipRect - 1)
				cString = cString.concat (m_acClips[i].toString ());			
			else
				cString = cString.concat (m_acClips[i].toString () + ",");			
		}				
		cString = cString.concat ("]");			
		return cString;			
	}
	static public void exchange (WRegion cReg1, WRegion cReg2)
	{
		cReg1.makeEmpty ();
	
		cReg1.m_acClips       = cReg2.m_acClips;
		cReg1.m_cBounds.m_nX0 = cReg2.m_cBounds.m_nX0;
		cReg1.m_cBounds.m_nY0 = cReg2.m_cBounds.m_nY0;
		cReg1.m_cBounds.m_nX1 = cReg2.m_cBounds.m_nX1;
		cReg1.m_cBounds.m_nY1 = cReg2.m_cBounds.m_nY1;
		cReg1.m_nNumClipRect  = cReg2.m_nNumClipRect;
	
		cReg2.m_acClips       = null;
		cReg2.m_cBounds.m_nX0 = 9999999;
		cReg2.m_cBounds.m_nY0 = 9999999;
		cReg2.m_cBounds.m_nX1 = -9999999;
		cReg2.m_cBounds.m_nY1 = -9999999;
		cReg2.m_nNumClipRect  = 0;
	}
	public boolean isEmpty ()
	{
		return m_nNumClipRect == 0;
	}
	public int countRects ()
	{
		return m_nNumClipRect;
	}
	public PixelRect rectAt (int nIndex)
	{
		//Debug.out.println ("WRegion::rectAt() " + nIndex + "/" + m_nNumClipRect);
		if (nIndex >= m_nNumClipRect)
			return null;
		return m_acClips[nIndex];
	}
	public void includeIntersection (WRegion cRegion, PixelRect cRectangle, boolean bNormalize)
	{
		PixelRect cOldClip;
		PixelRect cRect = new PixelRect ();
		/*PixelPoint cLefTop = cRectangle.LeftTop ();*/
		
		for (int i = 0; i < cRegion.m_nNumClipRect; i++)
		{			
			cOldClip = cRegion.m_acClips[i];
			
			cRect.m_nX0 = cOldClip.m_nX0;
			cRect.m_nY0 = cOldClip.m_nY0;
			cRect.m_nX1 = cOldClip.m_nX1;
			cRect.m_nY1 = cOldClip.m_nY1;

			cRect.clip (cRectangle);

			if (cRect.isValid ())
			{
				if (bNormalize)
				{
					/*cRect -= cLefTop;*/
					cRect.m_nX0 -= cRectangle.m_nX0;
					cRect.m_nY0 -= cRectangle.m_nY0;
					cRect.m_nX1 -= cRectangle.m_nX0;
					cRect.m_nY1 -= cRectangle.m_nY0;
				}					
				include (cRect);
			}
		}
		Optimize ();
		CalculateBounds ();
	}
	public PixelRect getBounds()
	{
		return new PixelRect (m_cBounds);
	}
	public void intersect (WRegion cRegion)
	{
		WRegion cTmp = new WRegion (this);
		cTmp.exclude (cRegion);
		exclude (cTmp);
	/*
		PixelRect cVClip;
		PixelRect cRect = new PixelRect ();
		WRegion cTmp = new WRegion ();

		for (int i = 0; i < cRegion.countRects(); i++)
		{					
			cVClip = cRegion.rectAt (i);										// add all currently visible
			for (int j = 0; j < m_nNumClipRect; j++)	// remove all previously visible
			{	
				cRect.setTo (cVClip);
				cRect.clip (m_acClips[j]);					

				if (cRect.isValid ())
					cTmp.include (cRect);
			}
		}
		exchange (this, cTmp);
	*/
	}
}
