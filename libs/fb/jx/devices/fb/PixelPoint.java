package jx.devices.fb;

import java.util.*;

public class PixelPoint
{
	public int m_nX, m_nY;

	public PixelPoint ()
	{
		m_nX = m_nY = -1;
	}

	public PixelPoint (int nX, int nY)
	{
		m_nX = nX;
		m_nY = nY;
	}

	public PixelPoint (PixelPoint cPoint)
	{
		m_nX = cPoint.m_nX;
		m_nY = cPoint.m_nY;
	}

	public int X () { return m_nX; }

	public int Y () { return m_nY; }

	static public PixelPoint add (PixelPoint cPoint1, PixelPoint cPoint2) 
	{
		return new PixelPoint (cPoint1.m_nX + cPoint2.m_nX, cPoint1.m_nY + cPoint2.m_nY);
	}

	static public PixelPoint sub (PixelPoint cPoint1, PixelPoint cPoint2) 
	{
		return new PixelPoint (cPoint1.m_nX - cPoint2.m_nX, cPoint1.m_nY - cPoint2.m_nY);
	}

	public void add (PixelPoint cPoint)
	{
		m_nX += cPoint.m_nX;
		m_nY += cPoint.m_nY;
	}

	public void add (int x, int y)
	{
		m_nX += x;
		m_nY += y;
	}

	public void sub (PixelPoint cPoint)
	{
		m_nX -= cPoint.m_nX;
		m_nY -= cPoint.m_nY;
	}

	public void sub (int x, int y)
	{
		m_nX -= x;
		m_nY -= y;
	}

	public void setTo (PixelPoint cPoint)
	{
		m_nX = cPoint.m_nX;
		m_nY = cPoint.m_nY;
	}

	public void setTo (int nX, int nY)
	{
		m_nX = nX;
		m_nY = nY;
	}

	public boolean isEqual (PixelPoint cPoint)
	{
		return m_nX == cPoint.m_nX && m_nY == cPoint.m_nY;
	}

	public boolean isNotEqual (PixelPoint cPoint)
	{
		return m_nX != cPoint.m_nX || m_nY == cPoint.m_nY;
	}  

	public String toString ()
	{
		if (this == null)
			return "<null>";
		return new String ("[" + m_nX + "," + m_nY + "]");
	}
}

