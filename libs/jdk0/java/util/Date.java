package java.util;

public class Date
{
	// Constants
  
  //  private final static String[] days = { "Sun", "Mon" };
	  //	"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
	  //};
  /*
	private final static String[] months = {
		"Jan", "Feb", "Mar", "Apr", "May", "Jun",
		"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
	};
	*/
  private final static String[] days = null;
	private final static String[] months = null;


	// Private Class Methods

	private static String fixed_int(int i)
	{
		return (i > 10) ? ("" + i) : ("0" + i);
	}

	// Static Fields

	private static int timezone;	// timezone offset (excluding DST)
	private static String[] tzname;	// timezone names

	static
	{
	  //	timezone = NativeUtil.getTZOffset();
	  //	tzname = new String[2];
	  //	NativeUtil.getTZNames(tzname);
	}

	// Fields

	private long millis;		// milliseconds since 1970-1-1
	private boolean isGMT;		// true if this Date represents GMT

	private int year;		// tm_year
	private int month;		// tm_mon
	private int date;		// tm_mday
	private int hrs;		// tm_hour
	private int min;		// tm_min
	private int sec;		// tm_sec
	private int day;		// tm_wday
	private int yday;		// tm_yday
	private boolean isdst;		// tm_isdst

	private boolean valid;		// broken-down time is valid
	private boolean mvalid;		// millisecond count is valid

	// Private Methods

	private final static int daysIn1970Years = 719528;
	private final static int daysIn400Years = 146097;
	private final static int daysIn100Years = 36524;
	private final static int daysIn4Years = 1460;
	private final static int daysInYear = 365;

	private final static int[] daysInMonth = null;
  /*
	private final static int[] daysInMonth = {
		31, 28, 31,  30, 31, 30,  31, 31, 30,  31, 30, 31
	};
	*/

	private void refresh_millis()
	{
		if (mvalid)
			return;

		int year = this.year + 1900;

		int c400 = year / 400;
		int c100 = (year % 400) / 100;
		int c4 = (year % 100) / 4;
		int c1 = year % 4;

		boolean isLeapCentury = (c100 == 0);
		boolean isLeapCycle = (isLeapCentury || c4 > 0);
		boolean isLeapYear = (isLeapCycle && c1 == 0);

		int days =
			c400 * daysIn400Years +
			c100 * daysIn100Years + (!isLeapCentury ? 1 : 0) +
			c4 * (daysIn4Years + 1) - ((!isLeapCentury && c4 > 0) ? 1 : 0) +
			c1 * daysInYear + ((isLeapCycle && c1 > 0) ? 1 : 0);

		for (int i = 0; i < this.month; i++)
			days += daysInMonth[i] + (isLeapYear && i == 1 ? 1 : 0);

		days += this.date - 1;

		this.yday = days;

		this.day = (days + 6) % 7;

		int hours = days * 24 + this.hrs;
		int minutes = hours * 60 + this.min;
		int seconds = minutes * 60 + this.sec;

		//this.isdst = !isGMT && NativeUtil.getTZIsDST(seconds);

		seconds -= timezone * 60 + (isdst ? 60 * 60 : 0);

		this.millis = seconds * 1000;

		mvalid = true;
	}

	private void refresh_components()
	{
		if (valid)
			return;

		refresh_millis();

		int n = (int)(this.millis / 1000) + (getTimezoneOffset() * 60);

		this.sec = n % 60;
		n /= 60;

		this.min = n % 60;
		n /= 60;

		this.hrs = n % 24;
		n /= 24;

		// n == days since 1970-01-01

		int days = n + daysIn1970Years;
		// days == days since 0000-01-01

		this.day = (days + 6) % 7;
		// 0000-01-01 is a Saturday => 6

		int c400 = days / daysIn400Years;
		// c400 == 400YrCycle

		days -= c400 * daysIn400Years;
		// days == days % 400 years ( == leap year cycle)

		int c100 = (days == 0) ? 0 : ((days - 1) / daysIn100Years);
		// c100 == century % 4

		boolean isLeapCentury = (c100 == 0);

		days -= (c100 * daysIn100Years + (isLeapCentury ? 0 : 1));
		// days == days % 100 years

		int c4 = (days + (isLeapCentury ? 0 : 1)) / (daysIn4Years + 1);
		// c4 == 4YrCycles % 25

		boolean isLeapCycle = (isLeapCentury || c4 > 0);

		days -= (c4 * (daysIn4Years + 1) - (isLeapCycle ? 0 : 1));
		// days == days % 4 years

		int c1 = (days == 0) ? 0 : ((days - (isLeapCycle ? 1 : 0)) / daysInYear);
		// c1 == Years % 4

		boolean isLeapYear = (isLeapCycle && c1 == 0);

		days -= (c1 * daysInYear + ((isLeapCycle && c1 > 0) ? 1 : 0));
		// days == days % 365/366

		this.year = (400 * c400) + (c100 * 100) + (c4 * 4) + c1 - 1900;

		this.yday = days;

		for (int i = 0; i < 12; i++)
		{
			n = daysInMonth[i];
			if (isLeapYear && i == 1)
				n++;
			if (days < n)
			{
				this.month = i;
				break;
			}
			days -= n;
		}

		this.date = ++days;

		valid = true;
	}

	private void invalidate()
	{
		mvalid = false;
		valid = false;
	}

	// Public Class Methods

	public synchronized static long parse(String string)
	{
	  return 0;//new DateParser(string).parse();
	}

	public static long UTC(int year, int month, int date, int hrs, int min, int sec)
	{
		Date d = new Date(year, month, date, hrs, min, sec);
		d.isGMT = true;
		return d.getTime();
	}

	// Public Instance Methods

	public int hashCode()
	{
		refresh_millis();
		return (int)(millis ^ (millis >>> 32));
	}

	public int getTimezoneOffset()
	{
		if (isGMT)
			return 0;

		refresh_millis();

		return timezone + (isdst ? 60 : 0);
	}

	public String toString()
	{
		refresh_components();

		return	days[		day	] + " " +
			months[		month	] + " " +
			fixed_int(	date	) + " " +
			fixed_int(	hrs	) + ":" +
			fixed_int(	min	) + ":" +
			fixed_int(	sec	) + " " +
			tzname[	isdst ? 1 : 0	] + " " +
			(1900 +		year	);
	}

	public String toGMTString()
	{
		if (!isGMT)
		{
			Date d = new Date(this.millis);
			d.isGMT = true;
			return d.toGMTString();
		}

		refresh_components();

		return	(		date	) + " " +
			months[		month	] + " " +
			(1900 +		year	) + " " +
			fixed_int(	hrs	) + ":" +
			fixed_int(	min	) + ":" +
			fixed_int(	sec	) + " " +
			"GMT";
	}

	public String toLocaleString()
	{
		return toString();
	}

	public boolean after(Date when)
	{
		return getTime() > when.getTime();
	}

	public boolean before(Date when)
	{
		return getTime() < when.getTime();
	}

	public boolean equals(Object obj)
	{
		return getTime() == ((Date)obj).getTime();
	}


	public long getTime()
	{
		refresh_millis();
		return millis;
	}

	public int getYear()
	{
		refresh_components();
		return year;
	}

	public int getMonth()
	{
		refresh_components();
		return month;
	}

	public int getDate()
	{
		refresh_components();
		return date;
	}

	public int getHours()
	{
		refresh_components();
		return hrs;
	}

	public int getMinutes()
	{
		refresh_components();
		return min;
	}

	public int getSeconds()
	{
		refresh_components();
		return sec;
	}

	public int getDay()
	{
		refresh_components();
		return day;
	}


	public void setTime(long millis)
	{
		this.millis = millis;
		valid = false;
		mvalid = true;
	}

	public void setYear(int year)
	{
		this.year = year;
		invalidate();
	}

	public void setMonth(int month)
	{
		this.month = month;
		invalidate();
	}

	public void setDate(int date)
	{
		this.date = date;
		invalidate();
	}

	public void setHours(int hours)
	{
		this.hrs = hrs;
		invalidate();
	}

	public void setMinutes(int minutes)
	{
		this.min = min;
		invalidate();
	}

	public void setSeconds(int seconds)
	{
		this.sec = sec;
		invalidate();
	}

	// Constructors

	public Date(int year, int month, int date, int hrs, int min, int sec)
	{
		this.year = year;
		this.month = month;
		this.date = date;
		this.hrs = hrs;
		this.min = min;
		this.sec = sec;
	}

	public Date(int year, int month, int date, int hrs, int min)
	{
		this(year, month, date, hrs, min, 0);
	}

	public Date(int year, int month, int date)
	{
		this(year, month, date, 0, 0, 0);
	}

	public Date(long millis)
	{
		this.millis = millis;
		this.mvalid = true;
	}

	public Date(String s)
	{
		this(parse(s));
	}

	public Date()
	{
		this(System.currentTimeMillis());
	}
}

