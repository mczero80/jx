package java.lang;

/**
 *  Character
 */

public final class Character extends Object
{
	public static final char MIN_VALUE = '\u0000';
	public static final char MAX_VALUE = '\uffff';
	public static final int MIN_RADIX = 2;
	public static final int MAX_RADIX = 36;

    public static final Class TYPE = Class.getPrimitiveClass("char");

	private char value;

	public Character(char value)
	{
		this.value = value;
	}

	public String toString()
	{
		return String.valueOf(value);
	}

	public boolean equals(Object obj)
	{
		if (obj == null || !(obj instanceof Character))
			return false;
		return ((Character)obj).value == value;
	}

	public int hashCode()
	{
		return (int) value;
	}

	public char charValue()
	{
		return value;
	}

	public static boolean isDefined(char ch)
	{
		// TODO:
		return true;
	}

    public static boolean isWhitespace(char ch) {
	throw new Error("NOT IMPLEMENTED");
    }

       
  private  int fassela = 1 ;
  private static int fasela = 1 ;
  private static int[] fasel = { 1 };
	private static char[] isLowerCaseTable = {
		'\u0183', '\u0185', '\u0188', '\u0192',
		'\u0195', '\u019E', '\u01A8', '\u01AB',
		'\u01AD', '\u01B0', '\u01B4', '\u01B6',
		'\u01BD', '\u01C6', '\u01C9', '\u01F0',
		'\u01F3', '\u01F5', '\u0275', '\u029A',
		'\u02A0', '\u0390', '\u04C2', '\u04C4',
		'\u04C8', '\u04CC', '\u04F9'
	};

	private static char[][] isLowerCaseRangeTable = {
		{'\u0061', '\u007A'}, {'\u00DF', '\u00F6'},
		{'\u00F8', '\u00FF'}, {'\u017F', '\u0180'},
		{'\u018C', '\u018D'}, {'\u0199', '\u019B'},
		{'\u01B9', '\u01BA'}, {'\u0250', '\u0261'},
		{'\u0263', '\u0269'}, {'\u026B', '\u0273'},
		{'\u0277', '\u027F'}, {'\u0282', '\u028E'},
		{'\u0290', '\u0293'}, {'\u029D', '\u029E'},
		{'\u02A3', '\u02A8'}, {'\u03AC', '\u03CE'},
		{'\u03D0', '\u03D1'}, {'\u03D5', '\u03D6'},
		{'\u03F0', '\u03F1'}, {'\u0430', '\u044F'},
		{'\u0451', '\u045C'}, {'\u045E', '\u045F'},
		{'\u0561', '\u0587'}, {'\u1E96', '\u1E9A'},
		{'\u1F00', '\u1F07'}, {'\u1F10', '\u1F15'},
		{'\u1F20', '\u1F27'}, {'\u1F30', '\u1F37'},
		{'\u1F40', '\u1F45'}, {'\u1F50', '\u1F57'},
		{'\u1F60', '\u1F67'}, {'\u1F70', '\u1F7D'},
		{'\u1F80', '\u1F87'}, {'\u1F90', '\u1F97'},
		{'\u1FA0', '\u1FA7'}, {'\u1FB0', '\u1FB4'},
		{'\u1FB6', '\u1FB7'}, {'\u1FC2', '\u1FC4'},
		{'\u1FC6', '\u1FC7'}, {'\u1FD0', '\u1FD3'},
		{'\u1FD6', '\u1FD7'}, {'\u1FE0', '\u1FE7'},
		{'\u1FF2', '\u1FF4'}, {'\u1FF6', '\u1FF7'},
		{'\uFB00', '\uFB06'}, {'\uFB13', '\uFB17'},
		{'\uFF41', '\uFF5A'}
	};

	private static char[][] isLowerCaseRangeOddTable = {
		{'\u0101', '\u0137'}, {'\u0149', '\u0177'},
		{'\u01A1', '\u01A5'}, {'\u01DD', '\u01EF'},
		{'\u01FB', '\u0217'}, {'\u03E3', '\u03EF'},
		{'\u0461', '\u0481'}, {'\u0491', '\u04BF'},
		{'\u04D1', '\u04EB'}, {'\u04EF', '\u04F5'},
		{'\u1E01', '\u1E95'}, {'\u1EA1', '\u1EF9'}
	};

	private static char[][] isLowerCaseRangeEvenTable = {
		{'\u0138', '\u0148'}, {'\u017A', '\u017E'},
		{'\u01CC', '\u01DC'}
	};

	public static boolean isLowerCase(char ch)
	{
		// single characters
		for (int i = 0; i < isLowerCaseTable.length; i++)
			if (ch == isLowerCaseTable[i])
				return true;

		// ranges
		for (int i = 0; i < isLowerCaseRangeTable.length; i++)
			if (ch >= isLowerCaseRangeTable[i][0] &&
			    ch <= isLowerCaseRangeTable[i][1])
				return true;

		// even ranges
		if ((ch % 2) == 0)
		{
			for (int i = 0; i < isLowerCaseRangeEvenTable.length; i++)
				if (ch >= isLowerCaseRangeEvenTable[i][0] &&
				    ch <= isLowerCaseRangeEvenTable[i][1])
					return true;
		}
		// odd ranges
		else
		{
			for (int i = 0; i < isLowerCaseRangeOddTable.length; i++)
				if (ch >= isLowerCaseRangeOddTable[i][0] &&
				    ch <= isLowerCaseRangeOddTable[i][1])
					return true;
		}

		return false;
	}

	private static char[] isUpperCaseTable = {
		'\u0184', '\u0186', '\u0187', '\u01A2',
		'\u01A4', '\u01A7', '\u01A9', '\u01AC',
		'\u01AE', '\u01AF', '\u01B5', '\u01B7',
		'\u01B8', '\u01BC', '\u01C4', '\u01C7',
		'\u01CA', '\u01F1', '\u01F4', '\u0386',
		'\u038C', '\u038E', '\u038F', '\u04C1',
		'\u04C3', '\u04C7', '\u04CB', '\u04F8'
	};

	private static char[][] isUpperCaseRangeTable = {
		{'\u0041', '\u005A'}, {'\u00C0', '\u00D6'},
		{'\u00D8', '\u00DE'}, {'\u0181', '\u0182'},
		{'\u0189', '\u018B'}, {'\u018E', '\u0191'},
		{'\u0193', '\u0194'}, {'\u0196', '\u0198'},
		{'\u019C', '\u019D'}, {'\u019F', '\u01A0'},
		{'\u01B1', '\u01B3'}, {'\u0388', '\u038A'},
		{'\u0391', '\u03A1'}, {'\u03A3', '\u03AB'},
		{'\u0401', '\u040C'}, {'\u040E', '\u042F'},
		{'\u0531', '\u0556'}, {'\u10A0', '\u10C5'},
		{'\u1F08', '\u1F0F'}, {'\u1F18', '\u1F1D'},
		{'\u1F28', '\u1F2F'}, {'\u1F38', '\u1F3F'},
		{'\u1F48', '\u1F4D'}, {'\u1F68', '\u1F6F'},
		{'\u1F88', '\u1F8F'}, {'\u1F98', '\u1F9F'},
		{'\u1FA8', '\u1FAF'}, {'\u1FB8', '\u1FBC'},
		{'\u1FC8', '\u1FCC'}, {'\u1FD8', '\u1FDB'},
		{'\u1FE8', '\u1FEC'}, {'\u1FF8', '\u1FFC'},
		{'\uFF21', '\uFF3A'}
	};

	private static char[][] isUpperCaseRangeOddTable = {
		{'\u0139', '\u0147'}, {'\u0179', '\u017D'},
		{'\u01CD', '\u01DB'}, {'\u1F59', '\u1F5F'}
	};

	private static char[][] isUpperCaseRangeEvenTable = {
		{'\u0100', '\u0136'}, {'\u014A', '\u0178'},
		{'\u01DE', '\u01EE'}, {'\u01FA', '\u0216'},
		{'\u03E2', '\u03EE'}, {'\u0460', '\u0480'},
		{'\u0490', '\u04BE'}, {'\u04D0', '\u04EA'},
		{'\u04EE', '\u04F4'}, {'\u1E00', '\u1E94'},
		{'\u1EA0', '\u1EF8'}
	};

	public static boolean isUpperCase(char ch)
	{
		// single characters
		for (int i = 0; i < isUpperCaseTable.length; i++)
			if (ch == isUpperCaseTable[i])
				return true;

		// ranges
		for (int i = 0; i < isUpperCaseRangeTable.length; i++)
			if (ch >= isUpperCaseRangeTable[i][0] &&
			    ch <= isUpperCaseRangeTable[i][1])
				return true;

		// even ranges
		if ((ch % 2) == 0)
		{
			for (int i = 0; i < isUpperCaseRangeEvenTable.length; i++)
				if (ch >= isUpperCaseRangeEvenTable[i][0] &&
				    ch <= isUpperCaseRangeEvenTable[i][1])
					return true;
		}
		// odd ranges
		else
		{
			for (int i = 0; i < isUpperCaseRangeOddTable.length; i++)
				if (ch >= isUpperCaseRangeOddTable[i][0] &&
				    ch <= isUpperCaseRangeOddTable[i][1])
					return true;
		}

		return false;
	}

	private static char[] isTitleCaseTable = {
		'\u01C5', '\u01C8', '\u01CB', '\u01F2'
	};

	public static boolean isTitleCase(char ch)
	{
		for (int i = 0; i < isTitleCaseTable.length; i++)
			if (ch == isTitleCaseTable[i])
				return true;
		return false;
	}

	private static char[][] isDigitRangeTable = {
		{'\u0030', '\u0039'}, {'\u0660', '\u0669'},
		{'\u06F0', '\u06F9'}, {'\u0966', '\u096F'},
		{'\u09E6', '\u09EF'}, {'\u0A66', '\u0A6F'},
		{'\u0AE6', '\u0AEF'}, {'\u0B66', '\u0B6F'},
		{'\u0BE7', '\u0BEF'}, {'\u0C66', '\u0C6F'},
		{'\u0CE6', '\u0CEF'}, {'\u0D66', '\u0D6F'},
		{'\u0E50', '\u0E59'}, {'\u0ED0', '\u0ED9'},
		{'\uFF10', '\uFF19'},
	};

	public static boolean isDigit(char ch)
	{
		// ranges
		for (int i = 0; i < isDigitRangeTable.length; i++)
		{
			if (ch >= isDigitRangeTable[i][0] &&
			    ch <= isDigitRangeTable[i][1])
				return true;
		}

		return false;
	}

	public static boolean isLetter(char ch)
	{
		return (isLetterOrDigit(ch) && !isDigit(ch));
	}

	private static char[] isLetterOrDigitTable = {
		'\u037A', '\u037E', '\u038C', '\u038E',
		'\u03DA', '\u03DC', '\u03DE', '\u03E0',
		'\u0589', '\u060C', '\u061B', '\u061F',
		'\u0621', '\u09B2', '\u09BC', '\u09BE',
		'\u09D7', '\u0A02', '\u0A3C', '\u0A3E',
		'\u0A5E', '\u0A8D', '\u0A8F', '\u0AD0',
		'\u0AE0', '\u0B9C', '\u0B9E', '\u0B9F',
		'\u0BD7', '\u0CDE', '\u0CE0', '\u0CE1',
		'\u0D57', '\u0E84', '\u0E8A', '\u0E8D',
		'\u0EA5', '\u0EA7', '\u0EC6', '\u0EC8',
		'\u10FB', '\u1F59', '\u1F5B', '\u1F5D',
		'\uFB3E', '\uFB40', '\uFB41', '\uFB43',
		'\uFB44', '\uFB46', '\uFE74', '\uFE76'
	};

	private static char[][] isLetterOrDigitRangeTable = {
		{'\u0030', '\u0039'}, {'\u0041', '\u005A'},
		{'\u0061', '\u007A'}, {'\u00C0', '\u00D6'},
		{'\u00D8', '\u00F6'}, {'\u00F8', '\u01F5'},
		{'\u01FA', '\u0217'}, {'\u0250', '\u02A8'},
		{'\u02B0', '\u02DE'}, {'\u02E0', '\u02E9'},
		{'\u0300', '\u0345'}, {'\u0360', '\u0361'},
		{'\u0374', '\u0375'}, {'\u0384', '\u038A'},
		{'\u038F', '\u03A1'}, {'\u03A3', '\u03CE'},
		{'\u03D0', '\u03D6'}, {'\u03DA', '\u03E2'},
		{'\u03E2', '\u03F3'}, {'\u0401', '\u040C'},
		{'\u040E', '\u044F'}, {'\u0451', '\u045C'},
		{'\u045E', '\u0486'}, {'\u0490', '\u04C4'},
		{'\u04C7', '\u04C8'}, {'\u04CB', '\u04CC'},
		{'\u04D0', '\u04EB'}, {'\u04EE', '\u04F5'},
		{'\u04F8', '\u04F9'}, {'\u0531', '\u0556'},
		{'\u0559', '\u055F'}, {'\u0561', '\u0587'},
		{'\u05B0', '\u05B9'}, {'\u05BB', '\u05C3'},
		{'\u05D0', '\u05EA'}, {'\u05F0', '\u05F4'},
		{'\u0622', '\u063A'}, {'\u0640', '\u0652'},
		{'\u0660', '\u066D'}, {'\u0670', '\u06B7'},
		{'\u06BA', '\u06BE'}, {'\u06C0', '\u06CE'},
		{'\u06D0', '\u06ED'}, {'\u06F0', '\u06F9'},
		{'\u0901', '\u0903'}, {'\u0905', '\u0939'},
		{'\u093C', '\u094D'}, {'\u0950', '\u0954'},
		{'\u0958', '\u0970'}, {'\u0981', '\u0983'},
		{'\u0985', '\u098C'}, {'\u098F', '\u0990'},
		{'\u0993', '\u09A8'}, {'\u09AA', '\u09B0'},
		{'\u09B6', '\u09B9'}, {'\u09BF', '\u09C4'},
		{'\u09C7', '\u09C8'}, {'\u09CB', '\u09CD'},
		{'\u09DC', '\u09DD'}, {'\u09DF', '\u09E3'},
		{'\u09E6', '\u09FA'}, {'\u0A05', '\u0A0A'},
		{'\u0A0F', '\u0A10'}, {'\u0A13', '\u0A28'},
		{'\u0A2A', '\u0A30'}, {'\u0A32', '\u0A33'},
		{'\u0A35', '\u0A36'}, {'\u0A38', '\u0A39'},
		{'\u0A3F', '\u0A42'}, {'\u0A47', '\u0A48'},
		{'\u0A4B', '\u0A4D'}, {'\u0A59', '\u0A5C'},
		{'\u0A66', '\u0A74'}, {'\u0A81', '\u0A83'},
		{'\u0A85', '\u0A8B'}, {'\u0A90', '\u0A91'},
		{'\u0A93', '\u0AA8'}, {'\u0AAA', '\u0AB0'},
		{'\u0AB2', '\u0AB3'}, {'\u0AB5', '\u0AB9'},
		{'\u0ABC', '\u0AC5'}, {'\u0AC7', '\u0AC9'},
		{'\u0ACB', '\u0ACD'}, {'\u0AE6', '\u0AEF'},
		{'\u0B01', '\u0B03'}, {'\u0B05', '\u0B0C'},
		{'\u0B0F', '\u0B10'}, {'\u0B13', '\u0B28'},
		{'\u0B2A', '\u0B30'}, {'\u0B32', '\u0B33'},
		{'\u0B36', '\u0B39'}, {'\u0B3C', '\u0B43'},
		{'\u0B47', '\u0B48'}, {'\u0B4B', '\u0B4D'},
		{'\u0B56', '\u0B57'}, {'\u0B5C', '\u0B5D'},
		{'\u0B5F', '\u0B61'}, {'\u0B66', '\u0B70'},
		{'\u0B82', '\u0B83'}, {'\u0B85', '\u0B8A'},
		{'\u0B8E', '\u0B90'}, {'\u0B92', '\u0B95'},
		{'\u0B99', '\u0B9A'}, {'\u0BA3', '\u0BA4'},
		{'\u0BA8', '\u0BAA'}, {'\u0BAE', '\u0BB5'},
		{'\u0BB7', '\u0BB9'}, {'\u0BBE', '\u0BC2'},
		{'\u0BC6', '\u0BC8'}, {'\u0BCA', '\u0BCD'},
		{'\u0BE7', '\u0BF2'}, {'\u0C01', '\u0C03'},
		{'\u0C05', '\u0C0C'}, {'\u0C0E', '\u0C10'},
		{'\u0C12', '\u0C28'}, {'\u0C2A', '\u0C33'},
		{'\u0C35', '\u0C39'}, {'\u0C3E', '\u0C44'},
		{'\u0C46', '\u0C48'}, {'\u0C4A', '\u0C4D'},
		{'\u0C55', '\u0C56'}, {'\u0C60', '\u0C61'},
		{'\u0C66', '\u0C6F'}, {'\u0C82', '\u0C83'},
		{'\u0C85', '\u0C8C'}, {'\u0C8E', '\u0C90'},
		{'\u0C92', '\u0CA8'}, {'\u0CAA', '\u0CB3'},
		{'\u0CB5', '\u0CB9'}, {'\u0CBE', '\u0CC4'},
		{'\u0CC6', '\u0CC8'}, {'\u0CCA', '\u0CCD'},
		{'\u0CD5', '\u0CD6'}, {'\u0CE6', '\u0CEF'},
		{'\u0D02', '\u0D03'}, {'\u0D05', '\u0D0C'},
		{'\u0D0E', '\u0D10'}, {'\u0D12', '\u0D28'},
		{'\u0D2A', '\u0D39'}, {'\u0D3E', '\u0D43'},
		{'\u0D46', '\u0D48'}, {'\u0D4A', '\u0D4D'},
		{'\u0D60', '\u0D61'}, {'\u0D66', '\u0D6F'},
		{'\u0E01', '\u0E3A'}, {'\u0E3F', '\u0E5B'},
		{'\u0E81', '\u0E82'}, {'\u0E87', '\u0E88'},
		{'\u0E94', '\u0E97'}, {'\u0E99', '\u0E9F'},
		{'\u0EA1', '\u0EA3'}, {'\u0EAA', '\u0EAB'},
		{'\u0EAD', '\u0EB9'}, {'\u0EBB', '\u0EBD'},
		{'\u0EC0', '\u0EC4'}, {'\u0EC9', '\u0ECD'},
		{'\u0ED0', '\u0ED9'}, {'\u0EDC', '\u0EDD'},
		{'\u10A0', '\u10C5'}, {'\u10D0', '\u10F6'},
		{'\u1100', '\u1159'}, {'\u115F', '\u11A2'},
		{'\u11A8', '\u11F9'}, {'\u1E00', '\u1E9A'},
		{'\u1EA0', '\u1EF9'}, {'\u1F00', '\u1F15'},
		{'\u1F18', '\u1F1D'}, {'\u1F20', '\u1F45'},
		{'\u1F48', '\u1F4D'}, {'\u1F50', '\u1F57'},
		{'\u1F5F', '\u1F7D'}, {'\u1F80', '\u1FB4'},
		{'\u1FB6', '\u1FC4'}, {'\u1FC6', '\u1FD3'},
		{'\u1FD6', '\u1FDB'}, {'\u1FDD', '\u1FEF'},
		{'\u1FF2', '\u1FF4'}, {'\u1FF6', '\u1FFE'},
		{'\u3041', '\u3094'}, {'\u3099', '\u309E'},
		{'\u30A1', '\u30FE'}, {'\u3105', '\u312C'},
		{'\u3131', '\u318E'}, {'\u3190', '\u319F'},
		{'\u3200', '\u321C'}, {'\u3220', '\u3243'},
		{'\u3260', '\u327B'}, {'\u327F', '\u32B0'},
		{'\u32C0', '\u32CB'}, {'\u32D0', '\u32FE'},
		{'\u3300', '\u3376'}, {'\u337B', '\u33DD'},
		{'\u33E0', '\u33FE'}, {'\u3400', '\u9FA5'},
		{'\uF900', '\uFA2D'}, {'\uFB00', '\uFB06'},
		{'\uFB13', '\uFB17'}, {'\uFB1E', '\uFB36'},
		{'\uFB38', '\uFB3C'}, {'\uFB47', '\uFBB1'},
		{'\uFBD3', '\uFD3F'}, {'\uFD50', '\uFD8F'},
		{'\uFD92', '\uFDC7'}, {'\uFDF0', '\uFDFB'},
		{'\uFE70', '\uFE72'}, {'\uFE77', '\uFEFC'},
		{'\uFF10', '\uFF19'}, {'\uFF21', '\uFF3A'},
		{'\uFF41', '\uFF5A'}, {'\uFF66', '\uFFBE'},
		{'\uFFC2', '\uFFC7'}, {'\uFFCA', '\uFFCF'},
		{'\uFFD2', '\uFFD7'}, {'\uFFDA', '\uFFDC'}
	};

	public static boolean isLetterOrDigit(char ch)
	{
		// single characters
		for (int i = 0; i < isLetterOrDigitTable.length; i++)
			if (ch == isLetterOrDigitTable[i])
				return true;

		// ranges
		for (int i = 0; i < isLetterOrDigitRangeTable.length; i++)
			if (ch >= isLetterOrDigitRangeTable[i][0] &&
			    ch <= isLetterOrDigitRangeTable[i][1])
				return true;

		return false;
	}
    


	public static boolean isJavaLetter(char ch)
	{
		return (ch == '\u0024' || ch == '\u005F' || isLetter(ch));
	}

	public static boolean isJavaLetterOrDigit(char ch)
	{
		return (ch == '\u0024' || ch == '\u005F' || isLetterOrDigit(ch));
	}


	public static char toTitleCase(char ch)
	{
		// TODO: support non-ASCII
		return ch;
	}

	public static int digit(char ch, int radix)
	{
		// TODO: support non-ASCII
		if (radix < MIN_RADIX || radix > MAX_RADIX)
			return -1;

		if (isDigit(ch))
		{
			int d = (ch - '0');
			return (d < radix) ? d : -1;
		}

		if (ch >= 'A' && ch < (char)('A' + radix - 10))
			return (ch - 'A' + 10);
		if (ch >= 'a' && ch < (char)('a' + radix - 10))
			return (ch - 'a' + 10);
		return -1;
	}

	public static char forDigit(int digit, int radix)
	{
		if (radix < MIN_RADIX || radix > MAX_RADIX)
			return MIN_VALUE;
		if (digit < 0 || digit >= radix)
			return MIN_VALUE;
		if (digit < 10)
			return (char)('0' + digit);
		else
			return (char)('a' + digit - 10);
	}
	
    

	public static boolean isSpace(char ch)
	{
		switch (ch)
		{
		case '\t':
		case '\n':
		case '\f':
		case '\r':
		case ' ':
			return true;
		default:
			return false;
		}
	}

	public static char toLowerCase(char ch)
	{
		// TODO: support non-ASCII
		if (ch >= 'A' && ch <= 'Z')
			return (char)(ch + 'a' - 'A');
		return ch;
	}

	public static char toUpperCase(char ch)
	{
		// TODO: support non-ASCII
		if (ch >= 'a' && ch <= 'z')
			return (char)(ch - 'a' - 'A');
		return ch;
	}
    /*
  // DUMMY
  public static boolean isLowerCase(char ch) { return true; }
  public static boolean isUpperCase(char ch) { return false; }
  public static boolean isTitleCase(char ch) { return false; }
  public static boolean isDigit(char ch) { return false; }
  public static boolean isLetter(char ch) { return true; }
  public static boolean isLetterOrDigit(char ch) { return true; }
  public static boolean isJavaLetter(char ch) { return true; }
  public static boolean isJavaLetterOrDigit(char ch) { return true; }
  public static char toTitleCase(char ch) { return 'X'; }
  public static int digit(char ch, int radix) { return 0; }
  public static char forDigit(int digit, int radix) { return '0'; }
    */

    //    public static int getType(char ch) {
    // }

}

