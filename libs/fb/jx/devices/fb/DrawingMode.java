package jx.devices.fb;

import jx.zero.Debug;

final public class DrawingMode
{
	public static final int DM_UNKNOWN		= 0;
    	public static final int DM_COPY			= 1;
    	public static final int DM_INVERT		= 2;
    	public static final int DM_OVER			= 3;
    	public static final int DM_DEBUG		= 4;
	public static final int DM_SCALE                = 8;

	public static final int DM_SCALE_MASK	        = DM_SCALE;
	public static final int DM_DBG_MASK	        = DM_DEBUG;

	public static final DrawingMode OVER   = new DrawingMode(DM_OVER);
	public static final DrawingMode COPY   = new DrawingMode(DM_COPY);
	public static final DrawingMode INVERT = new DrawingMode(DM_INVERT);
	public static final DrawingMode DEBUG  = new DrawingMode(DM_DEBUG);
	public static final DrawingMode SCALEDCOPY   = new DrawingMode(DM_COPY|DM_SCALE);
    
    
    	private int value;
    
    	public DrawingMode ()
    	{
    		value = DM_UNKNOWN;
    	}

	public DrawingMode (int value)
    	{
	    int mvalue = getValue();
    		if (mvalue < 0 || mvalue > DM_DEBUG)
			throw new Error ("DrawingMode::DrawingMode() unknown drawing mode!");
		this.value = value;			
    	}

	public DrawingMode (DrawingMode eMode)
	{
		this.value = eMode.value;
	}

	public int getValue ()
	{
		return ((0xffffffff ^ (DM_SCALE_MASK | DM_DBG_MASK)) & value);
	}

	public void setValue (int value)
	{
    		if (value < 0 || value > DM_OVER)
			throw new Error ("DrawingMode::DrawingMode() unknown drawing mode!");
		this.value = value;			
	}

	public boolean isSet(int nMode) {
		return getValue()==nMode;
	}

	public boolean isSet(DrawingMode eMode) {
		return getValue()==eMode.getValue();
	}

	public boolean isScaleable() {
		return (value & DM_SCALE_MASK)==DM_SCALE;
	}

	public boolean isDebug() {
		return (value & DM_DBG_MASK)==DM_DEBUG;
	}

	public String toString ()
	{
		String ret;
		if (this == null)
			return "<null>";
		switch (getValue())
		{
			case DM_COPY:
				ret = "DM_COPY";
				break;
			case DM_INVERT:
				ret = "DM_INVERT";
				break;
			case DM_OVER:
				ret = "DM_OVER";
				break;
			default:
				ret = "DM_UNKNOWN(" + value + ")";	
		}
		if (isScaleable()) ret += "& DM_SCALE";
		if (isDebug()) ret += "& DM_DEBUG";
		return ret;			
	}			
}
