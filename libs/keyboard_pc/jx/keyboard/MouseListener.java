package jx.keyboard;

import jx.devices.Keyboard;
import jx.keyboard.KeyboardImpl;
import jx.zero.*;
import jx.zero.debug.*;
import java.lang.*;
import jx.wm.EventListener;
import jx.wm.Qualifiers;
import jx.wm.Keycode;
import jx.wm.Keymap;
import jx.wm.WindowManager;

public class MouseListener extends EventListener implements Runnable
{
    KeyboardImpl	m_cKeyboard = null;
    Thread 		m_cThread;
    int 		m_nLastButtons = 0; 
    int		m_nButtons = 0;
    
    final boolean debug = false; 
    
    public MouseListener (WindowManager wm, KeyboardImpl	m_cKeyboard)
    {
	super(wm);
	this.m_cKeyboard = m_cKeyboard;
	if (m_cKeyboard == null) throw new Error("NO KEYBOARD");
	Debug.out.println ("MouseListener::MouseListener()");
	m_cThread = new Thread (this, "MouseListener");
	m_cThread.start ();
    }
    public void run ()
	{
		int nIndex = 0;
		byte anBuf[] = {0,0,0,0};
		
		Debug.out.println ("starting to listen to mouse");
		m_cKeyboard.openAux ();
			
		while (true)
		{
		    int nByte = m_cKeyboard.readAux ();
		    
		    while (nByte != -1)
			{
			    nByte &= 0xff;
			    if (nIndex == 0)
				{
				    if ((nByte & 0x08) == 0 || (nByte & 0x04) != 0)
					{
					    nByte = m_cKeyboard.readAuxUnblocked();
					    continue;							// Out of sync.
					}
				}
			    anBuf[nIndex++] = (byte)nByte;
			    if (nIndex >= 3)
				{
				    int x = anBuf[1];
				    int y = anBuf[2];
				    
				    if ((anBuf[0] & 0x10) != 0)
					{
					    x |= 0xffffff00;
					}
				    if ((anBuf[0] & 0x20) != 0)
					{
					    y |= 0xffffff00;
					}
				    int nButtons = 0;
			    	    if ((anBuf[0] & 0x01) != 0)
					{
					    nButtons |= 0x01;
					}
				    if ((anBuf[0] & 0x02) != 0)
					{
					    nButtons |= 0x02;
					}
				    dispatchEvent (x, -y, nButtons);
				    nIndex = 0;
				}
			    nByte = m_cKeyboard.readAuxUnblocked();
			}
		    Thread.yield();
		}
		//		if (m_cKeyboard != null)
		//			m_cKeyboard.releaseAux ();
	}
    void dispatchEvent (int nDeltaX, int nDeltaY, int nButtons)
	{
	    int nButtonFlg;
	    
	    if (debug) Debug.out.println ("MouseListener.dispatchEvent()");
	    
	    nButtonFlg = nButtons ^ m_nLastButtons;
	    m_nLastButtons = nButtons;
	    if (nButtonFlg != 0)
		{
		    if ((nButtonFlg & 0x01) != 0)
			{
			    if ((nButtons & 0x01) != 0)
				{
				    m_nButtons |= 1;
				    handleMouseDown (1);
				}
			    else
				{
				    m_nButtons &= ~1;
				    handleMouseUp (1);
				}
			}
		    if ((nButtonFlg & 0x02) != 0)
			{
			    if ((nButtons & 0x02) != 0)
				{
				    m_nButtons |= 2;
				    handleMouseDown (2);
				}
			    else
				{
				    m_nButtons &= ~2;
				    handleMouseUp (2);
				}
			}
		}
	    if (nDeltaX != 0 || nDeltaY != 0)
		{
		    handleMouseMoved (nDeltaX, nDeltaY);
		}
	}
}
