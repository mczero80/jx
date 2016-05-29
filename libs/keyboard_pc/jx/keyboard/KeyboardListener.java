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

public class KeyboardListener extends EventListener implements Runnable
{
	KeyboardImpl	m_cKeyboard;
	Thread 		m_cThread;


	public KeyboardListener (WindowManager wm, KeyboardImpl m_cKeyboard)
	{
	    super(wm);
	    this.m_cKeyboard = m_cKeyboard;
	    Debug.out.println ("KeyboardListener::KeyboardListener() for native pc keyboard");
	    m_cThread = new Thread (this, "KeyboardListener (PC Keyboard)");
	    m_cThread.start ();
	}
	public void finalize ()
	{
		Debug.out.println ("KeyboardListener::finalize()");
	}
	public void run ()
	{
		Debug.out.println ("Starting to listen to keyboard");
		Keycode eRawCode = new Keycode ();
		Keycode eKeyCode = new Keycode ();
		while (true)
		{
			int nKeyCode = m_cKeyboard.getcode() & 0xff;
			if (nKeyCode != 0)
			{
				if ((nKeyCode & 0x80) != 0)
					handleKeyUp (nKeyCode & 0x7f);
				else
					handleKeyDown (nKeyCode & 0x7f);
			}
		}
	}
}
