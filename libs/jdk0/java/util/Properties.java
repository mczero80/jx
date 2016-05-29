package java.util;

import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;

class PropertiesEnumeration implements Enumeration
{
    // Fields

    private Properties source;
    private Enumeration enum;
    private Enumeration defaults;
    private Object next;

    // Private Methods

    private void next_element()
    {
	if (defaults != null)
	    {
		while (defaults.hasMoreElements())
		    {
			next = defaults.nextElement();
			if (!source.containsKey(next))
			    return;
		    }

		defaults = null;
	    }

	if (enum.hasMoreElements())
	    {
		next = enum.nextElement();
		return;
	    }

	next = null;
    }

    // Public Methods

    public boolean hasMoreElements()
    {
	return (next != null);
    }

    public Object nextElement() throws NoSuchElementException
    {
	if (next == null)
	    throw new NoSuchElementException();
	Object result = next;
	next_element();
	return next;
    }

    // Constructors

    public PropertiesEnumeration(Properties src)
    {
	source = src;
	enum = src.keys();
	if (src.defaults != null)
	    defaults = src.defaults.propertyNames();
	next_element();
    }
}

public class Properties extends Hashtable
{ 
    // Constants

    private final static char[] digits = {
	'0', '1', '2', '3',
	'4', '5', '6', '7',
	'8', '9', 'A', 'B',
	'C', 'D', 'E', 'F',
    };

    // Fields

    protected Properties defaults;

    // Private Methods

    private void skip_ws(PushbackInputStream is) throws IOException
    {
	int c;

	while (true)
	    {
		c = is.read();
		if (c < 0)
		    return;

		if (c != '\t' || c != ' ')
		    break;
	    }

	is.unread(c);
    }

    private void skip_comment(PushbackInputStream is) throws IOException
    {
	int c;

	while (true)
	    {
		c = is.read();
		if (c < 0)
		    return;

		if (c == '\r' || c == '\n')
		    return;
	    }
    }

    private String read_key(PushbackInputStream is) throws IOException
    {
	StringBuffer buff = new StringBuffer();
	int c;

    loop:
	while (true)
	    {
		c = is.read();
		if (c < 0)
		    return null;

		switch (c)
		    {
		    case '\r':
		    case '\n':
			return null;

		    case ':':
		    case '=':
			skip_ws(is);
			return buff.toString();

		    case '\t':
		    case ' ':
			break loop;

		    default:
			buff.append((char) c);
		    }
	    }

	skip_ws(is);

	c = is.read();
	if (c < 0)
	    return null;

	switch (c)
	    {
	    case '\r':
	    case '\n':
		return null;

	    case ':':
	    case '=':
		skip_ws(is);
		return buff.toString();

	    default:
		is.unread(c);
		return buff.toString();
	    }
    }

    private int read_hexcode(PushbackInputStream is) throws IOException
    {
	int code = 0;
	for (int i = 0; i < 4; i++)
	    {
		int c = is.read();
		if (c < 0)
		    return -1;
		int d = Character.digit((char) c, 16);
		if (d < 0)
		    return -1;
		code <<= 4;
		code += d;
	    }
	return code;
    }

    private int read_escape(PushbackInputStream is) throws IOException
    {
	int c = is.read();
	if (c < 0)
	    return -1;

	switch (c)
	    {
	    case '\r':
	    case '\n':
		return -1;

	    case 't':
		return '\t';

	    case 'n':
		return '\n';

	    case 'r':
		return '\r';

	    case 'u':
		return read_hexcode(is);

	    default:
		return c;
	    }
    }

    private String read_value(PushbackInputStream is) throws IOException
    {
	StringBuffer buff = new StringBuffer();
	int c;

	while (true)
	    {
		c = is.read();
		if (c < 0)
		    return null;

		switch (c)
		    {
		    case '\\':
			buff.append(read_escape(is));
			break;
					
		    case '\r':
		    case '\n':
			return buff.toString();

		    default:
			buff.append((char) c);
		    }
	    }
    }

    // Public Methods
    public Object setProperty(String key, String value) {
	throw new Error("NOT IMPLEMENTED");
    }
    
    public String getProperty(String key)
    {
	String value = (String) get(key);

	if (value != null)
	    return value;

	if (defaults != null)
	    value = defaults.getProperty(key);

	return value;
    }

    public String getProperty(String key, String defaultValue)
    {
	String value = getProperty(key);

	return (value == null) ? defaultValue : value;
    }

    public Enumeration propertyNames()
{
    return new PropertiesEnumeration(this);
}

public void load(InputStream in) throws IOException
{
    in = Runtime.getRuntime().getLocalizedInputStream(in);
    PushbackInputStream is = new PushbackInputStream(in);

 whileloop:
    while (true)
	{
	    skip_ws(is);

	    String key;

	    int c = is.read();
	    if (c < 0)
		return;

	    switch (c)
		{
		case '#':
		case '!':
		    skip_comment(is);
		    continue whileloop;

		case '\r':
		case '\n':
		    continue whileloop;

		default:
		    is.unread(c);
		    key = read_key(is);
		    if (key == null)
			continue whileloop;
		    break;
		}

	    String value = read_value(is);
	    if (value == null)
		continue;

	    put(key, value);
	}
}

public void save(OutputStream out, String header)
{
    OutputStream os = Runtime.getRuntime().getLocalizedOutputStream(out);
    PrintStream ps = new PrintStream(os);

    if (header != null)
	ps.print("#" + header + "\n"); // XXX what if header contains \n?
    ps.print("#" + new Date() + "\n");

    Enumeration e = keys();
    while (e.hasMoreElements())
	{
	    String key = (String) e.nextElement();
	    ps.print(key + "=");

	    String value = (String) get(key);
	    char[] chars = new char[value.length()];
	    value.getChars(0, value.length(), chars, 0);

	    boolean leading = true;

	    for (int i = 0 ; i < chars.length; i++)
		{
		    char c = chars[i];

		    switch (c)
			{
			case '\\':	ps.print("\\\\");	break;
			case '\t':	ps.print("\\t");	break;
			case '\n':	ps.print("\\n");	break;
			case '\r':	ps.print("\\r");	break;

			case ' ':
			    if (leading)
				ps.print("\\ ");
			    else
				ps.print(" ");
			    break;

			default:
			    if (c < '\u0020' || c > '\u007E')
				ps.print(
					 "\\u" +
					 Character.forDigit((c >> 12) & 0x000F, 16) +
					 Character.forDigit((c >> 8) & 0x000F, 16) +
					 Character.forDigit((c >> 4) & 0x000F, 16) +
					 Character.forDigit(c & 0x000F, 16)
					 );
			    else
				ps.print(c);
			    break;
			}

		    if (c != ' ')
			leading = false;
		}

	    ps.print("\n");
	}
}

public void list(PrintStream out)
{
    Enumeration e = keys();
    while (e.hasMoreElements())
	{
	    String key = (String) e.nextElement();
	    String value = (String) get(key);

	    if (key.length() > 40)
		key = key.substring(0, 36) + "...";
	    if (value.length() > 40)
		value = value.substring(0, 36) + "...";

	    out.println(key + "=" + value);
	}
}

// Constructors 

public Properties(Properties defaults)
{
    this.defaults = defaults;
}

public Properties()
{
    this.defaults = null;
}
}

