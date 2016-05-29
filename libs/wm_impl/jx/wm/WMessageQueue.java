package jx.wm;

import jx.wm.message.WMessage;

public class WMessageQueue
{
	WMessage	m_acMsgs[] = null;
	int		m_nMsgCount = 0;
	public WMessageQueue ()
	{
	}
	public void addMessage (jx.wm.message.WMessage cMsg)
	{
	    WMessage acMsgs[] = new WMessage[m_nMsgCount+1];
	    for (int i = 0; i < m_nMsgCount; i++)
		acMsgs[i] = m_acMsgs[i];
	    acMsgs[m_nMsgCount++] = cMsg;
	    m_acMsgs = acMsgs;
	}
	public jx.wm.message.WMessage nextMessage ()
	{
	    if (m_nMsgCount == 0)
		return null;
	    WMessage acMsgs[];
	    WMessage cMsg = m_acMsgs[0];
		if (m_nMsgCount == 1)
		{
			m_acMsgs = null;
			m_nMsgCount = 0;
		}
		else
		{
			acMsgs = new WMessage[m_nMsgCount-1];
			for (int i = 1; i < m_nMsgCount; i++)
				acMsgs[i-1] = m_acMsgs[i];
			--m_nMsgCount;
			m_acMsgs = acMsgs;
		}
	    return cMsg;
	}
	public jx.wm.message.WMessage lastMessage ()
	{
		if (m_nMsgCount == 0)
			return null;
		return m_acMsgs[m_nMsgCount-1];
	}
	public int getMessageCount ()
	{
		return m_nMsgCount;
	}
	public boolean isEmpty ()
	{
		return m_nMsgCount == 0;
	}
}
