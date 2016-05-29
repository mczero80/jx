package test.wm;

import jx.zero.*;
import jx.zero.debug.*;
import jx.wm.*;
import jx.wm.message.*;
import jx.devices.fb.*;

class Button
{
	WWindow		m_cOwner = null;
	PixelRect	m_cFrame = new PixelRect ();
	boolean		m_bPressed = false;
	String		m_cTitle = null;
	public Button (WWindow cOwner, PixelRect cFrame, String cTitle)
	{
		m_cOwner = cOwner;
		m_cFrame.setTo (cFrame);
		m_cTitle = new String (cTitle);
	}
	public void paint (PixelRect cUpdateRect)
	{
		if (m_bPressed)
			m_cOwner.setFgColor (200, 200, 200);
		else
			m_cOwner.setFgColor (255, 255, 255);
		m_cOwner.fillRect (m_cFrame);
		if (m_bPressed)
			m_cOwner.setFgColor (0, 0, 0);
		else
			m_cOwner.setFgColor (128, 128, 128);
		m_cOwner.movePenTo (m_cFrame.m_nX0, m_cFrame.m_nY1);
		m_cOwner.drawLine (m_cFrame.m_nX0, m_cFrame.m_nY0);
		m_cOwner.drawLine (m_cFrame.m_nX1, m_cFrame.m_nY0);
		if (m_bPressed)
			m_cOwner.setFgColor (128, 128, 128);
		else
			m_cOwner.setFgColor (0, 0, 0);
		m_cOwner.drawLine (m_cFrame.m_nX1, m_cFrame.m_nY1);
		m_cOwner.drawLine (m_cFrame.m_nX0, m_cFrame.m_nY1);
		m_cOwner.movePenTo (m_cFrame.m_nX0 + 2, m_cFrame.m_nY0 + 2);
		m_cOwner.drawString (m_cTitle);
	}
	public boolean mouseDown (PixelPoint cMousePos, int nButton)
	{	
		boolean f = false;
		//Debug.out.println ("MainWindow::MouseDown(" + cMousePos.m_nX + ", " + cMousePos.m_nY + ")");
		if (m_cFrame.contains (cMousePos))
		{
			f = m_bPressed = true;
			m_cOwner.invalidate (m_cFrame);
		}
		return f;
	}
	public boolean mouseUp (PixelPoint cMousePos, int nButton)
	{
		boolean f = m_bPressed;
		if (m_bPressed)
		{
			m_bPressed = false;
			m_cOwner.invalidate (m_cFrame);
		}
		return f;
	}
	public void setValue (boolean bPressed)
	{
		m_bPressed = bPressed;
	}
	public boolean getValue()
	{
		return m_bPressed;
	}
}

class TestWindow extends WWindow
{
	int m_nColumns = 80;
	int m_nLines = 5;
	int m_nCursorX = 0;
	int m_nCursorY = 0;
	DrawingMode m_eDrawingMode = new DrawingMode();
	PixelRect m_cCursorRect = new PixelRect();
	StringBuffer m_cScreen[] = new StringBuffer[m_nLines];
	
	String m_cText = new String("");
	public TestWindow (String cTitle, PixelRect cFrame, WindowFlags eFlags)
	{
		super (cTitle, cFrame, eFlags);
		for (int i = 0; i < m_nLines; i++)
			m_cScreen[i] = new StringBuffer (m_nColumns);
	}
	void drawLine (int nLine)
	{
		setFgColor (255, 255, 255);
		m_cCursorRect.setTo (0, nLine*14, m_nColumns*9 - 1, nLine*14 + 13);
		fillRect (m_cCursorRect);
		movePenTo (0, nLine * 14);
		setFgColor (63, 63, 63);
		drawString (m_cScreen[nLine].toString());
		if (m_nCursorY == nLine)
		{
			m_cCursorRect.setTo (m_nCursorX*9, m_nCursorY*14, m_nCursorX*9 + 8, m_nCursorY*14 + 10);
			m_eDrawingMode.setValue (DrawingMode.DM_INVERT);
			fillRect (m_cCursorRect, m_eDrawingMode);
		}
	}
	void scrDown ()
	{
		for (int i = 1; i < m_nLines; i++)
		{
			m_cScreen[i-1] = m_cScreen[i];
		}
		m_cScreen[m_nLines-1] = new StringBuffer (m_nColumns);
	}
	public void paint (PixelRect cUpdateRect)
	{
	    //Debug.out.println ("TestWindow::paint(" + m_cText + ")");
		for (int i = 0; i < m_nLines; i++)
			drawLine (i);
   	}
	public void keyDown (Keycode eKeyCode, Keycode eRawCode, Qualifiers eQualifiers)
	{
		/*
		char buf[] = { (char)eKeyCode.value };
		m_cText = m_cText.concat (new String (buf));
		//Debug.out.println ("TestWindow::KeyDown (" + eKeyCode + ", " + eRawCode + ", " + eQualifiers + ")");
		*/
		switch (eKeyCode.value)
		{
		case Keycode.VK_BACKSPACE:
			break;
		case Keycode.VK_ENTER:
			m_nCursorX = 0;
			if (++m_nCursorY == m_nLines)
			{
				m_nCursorY = m_nLines - 1;
				scrDown ();
				for (int i = 0; i < m_nLines; i++)
					drawLine (i);
			}
			else
			{
				drawLine (m_nCursorY-1);
				drawLine (m_nCursorY);
			}
			break;
		case Keycode.VK_LEFT_ARROW:
			if (m_nCursorX > 0)
			{
				--m_nCursorX;
				drawLine (m_nCursorY);
				break;
			}
		case Keycode.VK_RIGHT_ARROW:
			if (m_nCursorX < m_nColumns)
			{
				++m_nCursorX;
				drawLine (m_nCursorY);
				break;
			}
		case Keycode.VK_UP_ARROW:
			if (m_nCursorY > 0)
			{
				--m_nCursorY;
				if (m_nCursorX > m_cScreen[m_nCursorY].length())
					m_nCursorX = m_cScreen[m_nCursorY].length();
				drawLine (m_nCursorY + 1);
				drawLine (m_nCursorY);
				break;
			}
		case Keycode.VK_DOWN_ARROW:
			if (m_nCursorY < m_nLines-1)
			{
				++m_nCursorY;
				if (m_nCursorX > m_cScreen[m_nCursorY].length())
					m_nCursorX = m_cScreen[m_nCursorY].length();
				drawLine (m_nCursorY - 1);
				drawLine (m_nCursorY);
				break;
			}
		default:
			if (m_nCursorX < m_nColumns)
			{
				m_cScreen[m_nCursorY].insert (m_nCursorX++, (char)eKeyCode.value);
				drawLine (m_nCursorY);
			}
			break;
		}
		//paint (new PixelRect ());
	}
	public void keyUp (Keycode eKeyCode, Keycode eRawCode, Qualifiers eQualifiers)
	{
	    //Debug.out.println ("TestWindow::KeyUp (" + eKeyCode + ", " + eRawCode + ", " + eQualifiers + ")");
	}
}

class DrawWindow extends WWindow
{
	int		m_nStartX;
	int		m_nStartY;
	int		m_nEndX;
	int		m_nEndY;
	int 	m_nMode = 0;
	boolean m_bDrawLine = false;
	boolean m_bDrawRect = false;
	Button	m_cLiner;
	Button  m_cRecter;
	Button  m_cFillTest;
	WBitmap m_cBackBuffer;
	
	public DrawWindow (String cTitle, PixelRect cFrame, WindowFlags eFlags)
	{
		super (cTitle, cFrame, eFlags);
		m_cLiner = new Button (this, new PixelRect (10, 10, 109, 29), "Linien");
		m_cRecter = new Button (this, new PixelRect (10, 35, 109, 54), "Rechtecke");
		m_cFillTest = new Button (this, new PixelRect (120, 10, 239, 29), "Filltest");
		m_cBackBuffer = WBitmap.createWBitmap (800, 600, new ColorSpace (ColorSpace.CS_RGB16));
		//setClip (50, 50, 100, 100);
	}
	public void paint (PixelRect cUpdateRect)
	{
		setBitmap (m_cBackBuffer);
		setFgColor (0, 0,  0);
/*		
		movePenTo (10, 10);
		drawString ("Dies ist ein ganz normaler Text");
		movePenTo (10, 30);
		drawLine (100, 40);
		fillRect (new PixelRect (10, 50, 100, 80));
*/		
		m_cLiner.paint (cUpdateRect);
		m_cRecter.paint (cUpdateRect);		 
		m_cFillTest.paint (cUpdateRect);
		setBitmap (null);
		drawBitmap (m_cBackBuffer, 0, 0);
	}
	public void mouseDown (PixelPoint cMousePos, int nButton)
	{
		if (!m_cLiner.mouseDown (cMousePos, nButton) && 
			!m_cRecter.mouseDown (cMousePos, nButton) &&
			!m_cFillTest.mouseDown (cMousePos, nButton))
		{
			if (m_nMode == 0 && !m_bDrawLine)
			{
				m_nStartX = m_nEndX = cMousePos.m_nX;
				m_nStartY = m_nEndY = cMousePos.m_nY;
				movePenTo (m_nStartX, m_nStartY);
				invertLine (m_nEndX, m_nEndY);
				m_bDrawLine = true;
			}
			if (m_nMode == 1 && !m_bDrawRect)
			{
				m_nStartX = m_nEndX = cMousePos.m_nX;
				m_nStartY = m_nEndY = cMousePos.m_nY;
				invertRect (m_nStartX, m_nStartY, 1, 1);
				m_bDrawRect = true;
			}
		}
	}
	public void mouseMoved (PixelPoint cMousePos, int nTransit)
	{
		if (m_bDrawLine)
		{
			movePenTo (m_nStartX, m_nStartY);
			invertLine (m_nEndX, m_nEndY);
			m_nEndX = cMousePos.m_nX;
			m_nEndY = cMousePos.m_nY;		
			movePenTo (m_nStartX, m_nStartY);
			invertLine (m_nEndX, m_nEndY);
		}
		if (m_bDrawRect)
		{
			invertRect (m_nStartX, m_nStartY, m_nEndX - m_nStartX + 1, m_nEndY - m_nStartY + 1);
			m_nEndX = cMousePos.m_nX;
			m_nEndY = cMousePos.m_nY;		
			invertRect (m_nStartX, m_nStartY, m_nEndX - m_nStartX + 1, m_nEndY - m_nStartY + 1);
		}
	}
	public void mouseUp (PixelPoint cMousePos, int nButton)
	{	
		if (m_cLiner.mouseUp (cMousePos, nButton))
			m_nMode = 0;
		if (m_cRecter.mouseUp (cMousePos, nButton))
			m_nMode = 1;
		if (m_cFillTest.mouseUp (cMousePos, nButton))
			testFillRect ();
		if (m_bDrawLine)
		{	
			movePenTo (m_nStartX, m_nStartY);
			invertLine (m_nEndX, m_nEndY);
			m_bDrawLine = false;
		}
		if (m_bDrawRect)
		{
			invertRect (m_nStartX, m_nStartY, m_nEndX - m_nStartX + 1, m_nEndY - m_nStartY + 1);
			m_bDrawRect = false;
		}
	}
	void testFillRect()
	{
		PixelRect cRect = new PixelRect();
		Naming naming = InitialNaming.getInitialNaming();
		Clock clock = (Clock)naming.lookup("Clock");
		int nWidth = 400;
		int nHeight = 350;
		int nLoops = 100;
		int nBpp = 2;
	
		CycleTime starttimec = new CycleTime();
		CycleTime endtimec = new CycleTime();
		CycleTime diff = new CycleTime();
		
		clock.getCycles(starttimec);
		for (int i = 0; i < 100; i++)
		{
			setFgColor (i, i, i);
			cRect.setTo (0 + i, 60 + i, 400 + i, 360 + i);
			fillRect (cRect);
		}
		clock.getCycles(endtimec);
		clock.subtract(diff, endtimec, starttimec);
		int time =  clock.toMilliSec(diff);
		int nSize = nWidth * nHeight * nBpp * nLoops;
		int nThroughput = nSize / (time * 1000);
		String cResults = "results: " + nSize + " Bytes, " + time + " milliseconds, " + nThroughput + " MB/s"; 
		setFgColor (255, 255, 255);
		cRect.setTo (0, 60, 600, 500);		
		fillRect (cRect);
		setFgColor (0, 0, 0);
		movePenTo (10, 60);
		drawString (cResults);
	}
}

class GDomain {
    public static void init(Naming naming, String[] args) {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));	
	MainWindow mw = new MainWindow ("main", new PixelRect (10, 100, 460, 300), new WindowFlags());
	mw.show (true);
    }
}

class GDomainTerm {
    public static void init(Naming naming, String[] args) {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));	
	MainWindow mw = new MainWindow ("main", new PixelRect (10, 100, 460, 300), new WindowFlags());
	mw.show (true);
	for(int i=0; i<1000; i++) Thread.yield();
	Debug.out.println("TERMINATE");
	DomainManager domainManager = (DomainManager) InitialNaming.getInitialNaming().lookup("DomainManager");
	domainManager.terminateCaller();
    }
}

class MDomain {
    public static void init(Naming naming, String[] args) {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));	
	DrawWindow w = new DrawWindow ("malicious zeichen-test", new PixelRect (250, 50, 700, 200), new WindowFlags(WindowFlags.WND_TRANSPARENT));
	w.show (true);
	for(;;);
    }
}

class MainWindow extends WWindow
{
	DrawingMode m_eDrawingMode = new DrawingMode();
	PixelRect m_cButton1 = new PixelRect (10, 10, 10 + 150 - 1, 10 + 20 - 1);
	PixelRect m_cButton2 = new PixelRect (10, 40, 10 + 150 - 1, 40 + 20 - 1);
	PixelRect m_cButton3 = new PixelRect (10, 70, 10 + 150 - 1, 70 + 20 - 1);
	PixelRect m_cButton4 = new PixelRect (10, 100, 10 + 200 - 1, 100 + 20 - 1);
	PixelRect m_cButton5 = new PixelRect (10, 130, 10 + 250 - 1, 130 + 20 - 1);
	boolean	m_bButton1 = false;
	boolean m_bButton2 = false;
	boolean m_bButton3 = false;
	boolean m_bButton4 = false;
	boolean m_bButton5 = false;
	public MainWindow (String cTitle, PixelRect cFrame, WindowFlags eFlags)
	{
		super (cTitle, cFrame, eFlags);
		//setClip (50, 50, 100, 100);
	}
	void drawButton (String cString, int x, int y, int w, int h, boolean bPressed)
	{		
		if (bPressed)
			setFgColor (200, 200, 200);
		else
			setFgColor (255, 255, 255);
		fillRect (new PixelRect (x, y, x + w - 1, y + h - 1));
		if (bPressed)
			setFgColor (0, 0, 0);
		else
			setFgColor (128, 128, 128);
		movePenTo (x, y+h);
		drawLine (x, y);
		drawLine (x+w, y);
		if (bPressed)
			setFgColor (128, 128, 128);
		else
			setFgColor (0, 0, 0);
		drawLine (x+w, y+h);
		drawLine (x, y+h);
		movePenTo (x + 2, y + 2);
		drawString (cString);
	}
	public void paint (PixelRect cUpdateRect)
	{
		drawButton ("keyboard test", 10, 10, 150, 20, m_bButton1);
		drawButton ("zeichen test", 10, 40, 150, 20, m_bButton2);
		drawButton ("domain test", 10, 70, 150, 20, m_bButton3);
		drawButton ("malicious domain test", 10, 100, 200, 20, m_bButton4);
		drawButton ("terminate current domain", 10, 130, 250, 20, m_bButton5);
		setFgColor (0, 0, 0);
		fillRect (10, 160, 150, 20);
	}
	public void mouseDown (PixelPoint cMousePos, int nButton)
	{	
		Debug.out.println ("MainWindow::MouseDown(" + cMousePos.m_nX + ", " + cMousePos.m_nY + ")");
		if (m_cButton1.contains (cMousePos))
		{
			m_bButton1 = true;
			invalidate (m_cButton1);
		}
		if (m_cButton2.contains (cMousePos))
		{
			m_bButton2 = true;
			invalidate (m_cButton2);
		}
		if (m_cButton3.contains (cMousePos))
		{
			m_bButton3 = true;
			invalidate (m_cButton3);
		}
		if (m_cButton4.contains (cMousePos))
		{
			m_bButton4 = true;
			invalidate (m_cButton4);
		}
		if (m_cButton5.contains (cMousePos))
		{
			m_bButton5 = true;
			invalidate (m_cButton5);
		}
	}
	public void mouseUp (PixelPoint cMousePos, int nButton)
	{
	    Debug.out.println ("MainWindow.mouseUp(" + cMousePos.m_nX + ", " + cMousePos.m_nY + ")");
		if (m_bButton1)
		{
			m_bButton1 = false;
			invalidate (m_cButton1);
			TestWindow w = new TestWindow ("keyboard-test", new PixelRect (100, 100, 400, 300), new WindowFlags());
			w.show (true);
		}
		if (m_bButton2)
		{
			m_bButton2 = false;
			invalidate (m_cButton2);
			DrawWindow w = new DrawWindow ("zeichen-test", new PixelRect (250, 50, 700, 200), new WindowFlags(WindowFlags.WND_TRANSPARENT));
			w.show (true);
		}
		if (m_bButton3)
		{
			m_bButton3 = false;
			invalidate (m_cButton3);
			DomainStarter.createDomain("Good", "test_wm.jll", "test/wm/GDomain", 300000, 100000, 1000000, 100000, 3);
		}
		if (m_bButton4)
		{
			m_bButton4 = false;
			invalidate (m_cButton4);
			DomainStarter.createDomain("Malicious", "test_wm.jll", "test/wm/MDomain", 300000, 100000, 1000000, 100000, 3);
		}
		if (m_bButton5)
		{
			m_bButton5 = false;
			invalidate (m_cButton5);
			Debug.out.println("TERMINATE");
			DomainManager domainManager = (DomainManager) InitialNaming.getInitialNaming().lookup("DomainManager");
			domainManager.terminateCaller();
		}
	}
}

public class Main {
	public static void main (String [] args)
	{
		Naming naming = InitialNaming.getInitialNaming();
		Debug.out.print ("Starting test domain for window manager...\n");
		MainWindow mw = new MainWindow ("main", new PixelRect (300, 500, 750, 700), new WindowFlags());
		mw.show (true);

		if (args.length == 1) {
		    if (args[0].equals("showAll")) {
			new TestWindow ("keyboard-test", new PixelRect (50, 250, 450, 600), new WindowFlags()).show (true);
			new DrawWindow ("zeichen-test", new PixelRect (250, 50, 700, 200), new WindowFlags()).show (true);
		    } else if (args[0].equals("term")) {
			new DrawWindow ("zeichen-test", new PixelRect (250, 50, 700, 200), new WindowFlags()).show (true);
			for(int i=0; i<10; i++) Thread.yield();
			DomainStarter.createDomain("Good", "test_wm.jll", "test/wm/GDomainTerm", 300000, 100000, 1000000, 100000, 3);
		    } else if (args[0].equals("gctest")) {
			for(;;) {
			    int[] x = new int[100000];
			    for(int i=0; i<100; i++) Thread.yield();
			}
		    }
		}
	}	
}

