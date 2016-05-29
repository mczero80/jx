package jx.wm;
import jx.wm.message.*;
import jx.wm.WindowManager;
import jx.zero.*;
import jx.devices.*;

public class EventListener
{	
	WindowManager m_cWindowManager = null;
			
	public EventListener (WindowManager m_cWindowManager)
	{
		Debug.out.println ("EventListener::EventListener()\n");
		this.m_cWindowManager = m_cWindowManager;
	}
	public void handleKeyUp (int nKeyCode)
	{
		m_cWindowManager.handleKeyUp (nKeyCode);
	}
	public void handleKeyDown (int nKeyCode)
	{
		m_cWindowManager.handleKeyDown (nKeyCode);
	}
	public void dispatchEvent (jx.wm.message.WMessage cEvent)
    	{
		m_cWindowManager.postInputEvent (cEvent);
    	}
	public void handleMouseDown (int nButton)
	{
		m_cWindowManager.handleMouseDown (nButton);
	}
	public void handleMouseUp (int nButton)
	{
		m_cWindowManager.handleMouseUp (nButton);
	}
	public void handleMouseMoved (int nDeltaX, int nDeltaY)
	{
		m_cWindowManager.handleMouseMoved (nDeltaX, nDeltaY);
	}
    public void handleMousePosition (int nPosX, int nPosY)
    {
	m_cWindowManager.handleMousePosition (nPosX, nPosY);
    }
}
