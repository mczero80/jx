package jx.wm;

public class Keymap
{
    public static final int CM_NORMAL 		= 0;
    public static final int CS_SHFT		= 1;
    public static final int CS_CTRL		= 2;
    public static final int CS_OPT		= 3;
    public static final int CS_SHFT_OPT		= 4;
    public static final int CS_CAPSL		= 5;
    public static final int CS_SHFT_CAPSL	= 6;
    public static final int CS_CAPSL_OPT	= 7;
    public static final int CS_SHFT_CAPSL_OPT	= 8;

    public static final int KLOCK_CAPSLOCK   = 0x0001;
    public static final int KLOCK_SCROLLLOCK = 0x0002;
    public static final int KLOCK_NUMLOCK    = 0x0004;

    public int  m_nCapsLock;
    public int  m_nScrollLock;
    public int  m_nNumLock;
    public int  m_nLShift;
    public int  m_nRShift;
    public int  m_nLCommand;
    public int  m_nRCommand;
    public int  m_nLControl;
    public int  m_nRControl;
    public int  m_nLOption;
    public int  m_nROption;
    public int  m_nMenu;
    public int  m_nLockSetting;
    
    public int  m_anMap[][] = new int[128][9];
    
    public Keymap ()
    {
    }	
}