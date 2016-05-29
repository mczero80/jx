package jx.devices.fb;

import jx.devices.fb.PixelPoint;

final public class PixelRect
{
  	public int m_nX0;
	public int m_nY0;
  	public int m_nX1;
	public int m_nY1;
	
	private static int __min__ (int a, int b)
	{
		return a < b ? a : b;
	}

	private static int __max__ (int a, int b)
	{
		return a > b ? a : b;
	}
	
  	public PixelRect ()
  	{
    		m_nX0 = m_nY0 = m_nX1 = m_nY1 = 99999999;
  	}

  	public PixelRect (int fX0, int fY0, int fX1, int fY1)
 	{
    		m_nX0 = fX0;
    		m_nY0 = fY0;
    		m_nX1 = fX1;
    		m_nY1 = fY1;
  	}

  	public PixelRect (PixelPoint cPoint1, PixelPoint cPoint2)
  	{
    		m_nX0 = cPoint1.X ();
    		m_nY0 = cPoint1.Y ();
    		m_nX1 = cPoint2.X ();    
    		m_nY1 = cPoint2.Y ();         
  	}

  	public PixelRect (PixelRect r)
  	{
    		m_nX0 = r.m_nX0;
    		m_nY0 = r.m_nY0;
    		m_nX1 = r.m_nX1;
    		m_nY1 = r.m_nY1;
  	}

  	public void setTo (int nX0, int nY0, int nX1, int nY1)
 	{ 
    		m_nX0 = nX0;
    		m_nY0 = nY0;
    		m_nX1 = nX1;
    		m_nY1 = nY1;
  	}

  	public void setTo (PixelRect r)
  	{
    		m_nX0 = r.m_nX0;
    		m_nY0 = r.m_nY0;
    		m_nX1 = r.m_nX1;
    		m_nY1 = r.m_nY1;
  	}

  	public boolean	isValid()
  	{ 
    		return (m_nX0 <= m_nX1 && m_nY0 <= m_nY1);	 
  	}

	public boolean isEqual (PixelRect cRect)
	{
		return (m_nX0 == cRect.m_nX0 &&
			m_nY0 == cRect.m_nY0 &&
			m_nX1 == cRect.m_nX1 &&
			m_nY1 == cRect.m_nY1);		       
	}

  	public void	invalidate () 
  	{ 
    		m_nX0 = m_nY0 = 999999; m_nX1 = m_nY1 = -999999;	
  	}

  	public boolean intersects (PixelPoint cPoint) 
  	{ 
    		return (!(cPoint.m_nX < m_nX0 || cPoint.m_nX > m_nX1|| 
              		cPoint.m_nY < m_nY0 || cPoint.m_nY > m_nY1)); 
  	}  

	public boolean contains (PixelPoint cPoint)
	{
		return (cPoint.m_nX >= m_nX0 && cPoint.m_nX <= m_nX1 &&
		        cPoint.m_nY >= m_nY0 && cPoint.m_nY <= m_nY1);
	}

  	public boolean intersects (PixelRect cPixelRect)
  	{ 
    		return (!(cPixelRect.m_nX1 < m_nX0 || cPixelRect.m_nX0 > m_nX1 || 
              		cPixelRect.m_nY1 < m_nY0 || cPixelRect.m_nY0 > m_nY1)); 
  	}

  	public void resize (int nLeft, int nTop, int nRight, int nBottom) 
  	{
		m_nX0 += nLeft; 
    		m_nY0 += nTop; 
    		m_nX1 += nRight; 
    		m_nY1 += nBottom;
  	}

	final public int x0()
	{
		return m_nX0;
	}
	final public int y0()
	{
		return m_nY0;
	}
	final public int x1()
	{
		return m_nX1;		
	}
	final public int y1()
	{
		return m_nY1;
	}

  	public int left () 
  	{ 
  		return m_nX0; 
	}
  	public int right () 
	{ 
		return m_nX1; 
	}
  	public int top () 
	{ 
		return m_nY0; 
	}
  	public int bottom () 
	{ 
		return m_nY1; 
	}
  	public int width () 
	{ 
		return m_nX1 - m_nX0; 
	}
  	public int height () 
	{ 
		return m_nY1 - m_nY0; 
	}
  	public PixelPoint leftTop () 
	{ 
		return new PixelPoint (m_nX0, m_nY0); 
	}
  	public PixelPoint leftBottom () 
	{ 
		return new PixelPoint (m_nX0, m_nY1); 
	}
  	public PixelPoint rightTop () 
	{ 
		return new PixelPoint (m_nX1, m_nY0); 
	}
  	public PixelPoint rightBottom () 
	{ 
		return new PixelPoint (m_nX1, m_nY1); 
	}
	public PixelRect bounds ()
	{
		return new PixelRect (0, 0, m_nX1 - m_nX0, m_nY1 - m_nY0);
	}
	public void add (PixelPoint cPos)
  	{
    		m_nX0 += cPos.X ();
    		m_nY0 += cPos.Y ();
    		m_nX1 += cPos.X ();
    		m_nY1 += cPos.Y ();
  	}
	public void add (int x, int y)
	{
		m_nX0 += x;
		m_nY0 += y;
		m_nX1 += x;
		m_nY1 += y;
	}
  	public void sub (PixelPoint cPos)
  	{
    		m_nX0 -= cPos.m_nX;
    		m_nY0 -= cPos.m_nY;
    		m_nX1 -= cPos.m_nX;
    		m_nY1 -= cPos.m_nY;
  	}
	public void sub (int x, int y)
	{
		m_nX0 -= x;
		m_nY0 -= y;
		m_nX1 -= x;
		m_nY1 -= y;
	}
	public void add (PixelRect cRect)
	{
		m_nX0 += cRect.m_nX0;
		m_nY0 += cRect.m_nY0;
		m_nX1 += cRect.m_nX1;
		m_nY1 += cRect.m_nY1;
	}
	public void sub (PixelRect cRect)
	{
		m_nX0 -= cRect.m_nX0;
		m_nY0 -= cRect.m_nY0;
		m_nX1 -= cRect.m_nX1;
		m_nY1 -= cRect.m_nY1;
	}
	public PixelRect or (PixelRect cRect)
	{
		return new
			 PixelRect (__min__ (m_nX0, cRect.m_nX0),
							__min__ (m_nY0, cRect.m_nY0),
							__max__ (m_nX1, cRect.m_nX1), __max__ (m_nY1, cRect.m_nY1));
	}
	public void include (PixelRect cRect)
	{
		m_nX0 = __min__ (m_nX0, cRect.m_nX0);
		m_nY0 = __min__ (m_nY0, cRect.m_nY0);
		m_nX1 = __max__ (m_nX1, cRect.m_nX1);
		m_nY1 = __max__ (m_nY1, cRect.m_nY1);
	}
	public void clip (PixelRect cRect)
	{
		m_nX0 = __max__ (m_nX0, cRect.m_nX0);
		m_nY0 = __max__ (m_nY0, cRect.m_nY0);
		m_nX1 = __min__ (m_nX1, cRect.m_nX1);
		m_nY1 = __min__ (m_nY1, cRect.m_nY1);
	}
	public String toString ()
	{
		if (this == null) return "<null>";
		return new String ("[" + m_nX0 + "/" + m_nY0 + " - " + m_nX1 + "/" + m_nY1 + "]");
	}
};
