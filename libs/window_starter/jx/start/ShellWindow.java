package jx.start;

import jx.zero.*;
import jx.zero.debug.*;
import jx.wm.*;
import jx.wm.message.*;
import jx.devices.fb.*;

import java.io.IOException;

import jx.streams.StreamProvider;
import jx.streams.InputStreamPortal;
import jx.streams.OutputStreamPortal;

class ShellWindow extends WWindow {
    int m_nColumns = 80;
    int m_nLines = 25;
    int m_nCursorX = 0;
    int m_nCursorY = 0;
    DrawingMode m_eDrawingMode = new DrawingMode();
    PixelRect m_cCursorRect = new PixelRect();
    StringBuffer m_cScreen[] = new StringBuffer[m_nLines];
	
    String m_cText = new String("");

    public static void main(String[] args) {
	ShellWindow w = new ShellWindow (args[0], new PixelRect (10, 30, 750, 400), new WindowFlags());
	w.show (true);
    }

    public ShellWindow (String cTitle, PixelRect cFrame, WindowFlags eFlags)
    {
	super (cTitle, cFrame, eFlags);
	for (int i = 0; i < m_nLines; i++)
	    m_cScreen[i] = new StringBuffer (m_nColumns);


	StreamProvider mystreams = new WindowStreamProvider();
	    
	InitialNaming.getInitialNaming().registerPortal(mystreams, cTitle);
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

    


    class WindowInputStream implements InputStreamPortal {
	public int read() throws IOException {
	    return -1;
	}
    }

    class WindowOutputStream implements OutputStreamPortal {
	public void flush()  {}
	
	public void write(int b) throws IOException {
	    if (b == '\n') {
		m_nCursorX = 0;
		++m_nCursorY;
		if (m_nCursorY == m_nLines) {
		    m_nCursorY = m_nLines - 1;
		    scrDown ();
		    for (int i = 0; i < m_nLines; i++)
			drawLine (i);
		} else {
		    drawLine (m_nCursorY-1);
		    drawLine (m_nCursorY);
		}
	    } else {
		//Debug.out.println("cy="+m_nCursorY+", cx="+m_nCursorX+", cn="+m_nLines+", b="+b);
		//Debug.out.println(m_cScreen[m_nCursorY].toString());
		m_cScreen[m_nCursorY].insert (m_nCursorX, (char)b);
		m_nCursorX++; // FIXME: increment in previous line causes crash!!
		drawLine (m_nCursorY);
	    }
	}
    }

    class WindowStreamProvider implements  StreamProvider, Service {
	public InputStreamPortal getInputStream() {
	    return new WindowInputStream();
	}
	
	public OutputStreamPortal getOutputStream() {
	    return new WindowOutputStream();
	}
	
	public OutputStreamPortal getErrorStream() {
	    return new WindowOutputStream();
	}
	public void close() {}
    }
    
}
