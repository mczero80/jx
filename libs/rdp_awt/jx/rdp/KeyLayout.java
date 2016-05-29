package jx.rdp;

public class KeyLayout {
    
    private static final String[] main_key_US = {
	"`~", "1!", "2@", "3#", "4$", "5%", "6^", "7&", "8*", "9(", "0)", "-_",
	"=+",
	"qQ", "wW", "eE", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "[{", "]}",
	"aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", ";:", "'\"",
	"\\|",
	"zZ", "xX", "cC", "vV", "bB", "nN", "mM", ",<", ".>", "/?"
    };
    
    /*** United States keyboard layout (phantom key version) */
    /* (XFree86 reports the <> key even if it's not physically there) */
    private static final String[] main_key_US_phantom = {
	"`~", "1!", "2@", "3#", "4$", "5%", "6^", "7&", "8*", "9(", "0)", "-_",
	"=+",
	"qQ", "wW", "eE", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "[{", "]}",
	"aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", ";:", "'\"",
	"\\|",
	"zZ", "xX", "cC", "vV", "bB", "nN", "mM", ",<", ".>", "/?",
	"<>"			/* the phantom key */
    };
    
    /*** British keyboard layout */
    private static final String[] main_key_UK = {
	"`", "1!", "2\"", "3£", "4$", "5%", "6^", "7&", "8*", "9(", "0)", "-_",
	"=+",
	"qQ", "wW", "eE", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "[{", "]}",
	"aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", ";:", "'@", "#~",
	"zZ", "xX", "cC", "vV", "bB", "nN", "mM", ",<", ".>", "/?",
	"\\|"
    };
    
    /*** French keyboard layout (contributed by Eric Pouech) */
    private static final String[] main_key_FR = {
	"²", "&1", "é2~", "\"3#", "'4{", "(5[", "-6|", "è7", "_8\\", "ç9^±",
	"à0@", ")°]", "=+}",
	"aA", "zZ", "eE", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "^¨", "$£¤",
	"qQ", "sSß", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", "mM", "ù%", "*µ",
	"wW", "xX", "cC", "vV", "bB", "nN", ",?", ";.", ":/", "!§",
	"<>"
    };

    /*** Icelandic keyboard layout (contributed by Ríkharður Egilsson) */
    private static final String[] main_key_IS = {
	"°", "1!", "2\"", "3#", "4$", "5%", "6&", "7/{", "8([", "9)]", "0=}",
	"öÖ\\", "-_",
	"qQ@", "wW", "eE", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "ðÐ",
	"'?~",
	"aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", "æÆ", "´^", "+*`",
	"zZ", "xX", "cC", "vV", "bB", "nN", "mM", ",;", ".:", "þÞ",
	"<>|"
    };
    
    /*** German keyboard layout (contributed by Ulrich Weigand) */
    private static final String[] main_key_DE = {
	"^°", "1!", "2\"²", "3§³", "4$", "5%", "6&", "7/{", "8([", "9)]", "0=}",
	"ß?\\", "'`",
	"qQ@", "wW", "eE€", "rR", "tT", "zZ", "uU", "iI", "oO", "pP", "üÜ",
	"+*~",
	"aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", "öÖ", "äÄ", "#´",
	"yY", "xX", "cC", "vV", "bB", "nN", "mMµ", ",;", ".:", "-_",
	"<>|"
    };

    /*** German keyboard layout without dead keys */
    private static final String[] main_key_DE_nodead = {
	"^°", "1!", "2\"", "3§", "4$", "5%", "6&", "7/{", "8([", "9)]", "0=}",
	"ß?\\", "´",
	"qQ", "wW", "eE", "rR", "tT", "zZ", "uU", "iI", "oO", "pP", "üÜ", "+*~",
	"aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", "öÖ", "äÄ", "#'",
	"yY", "xX", "cC", "vV", "bB", "nN", "mM", ",;", ".:", "-_",
	"<>"
    };

    /*** Swiss German keyboard layout (contributed by Jonathan Naylor) */
    private static final String[] main_key_SG = {
	"§°", "1+|", "2\"@", "3*#", "4ç", "5%", "6&¬", "7/¦", "8(¢", "9)", "0=",
	"'?´", "^`~",
	"qQ", "wW", "eE", "rR", "tT", "zZ", "uU", "iI", "oO", "pP", "üè[",
	"¨!]",
	"aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", "öé", "äà{",
	"$£}",
	"yY", "xX", "cC", "vV", "bB", "nN", "mM", ",;", ".:", "-_",
	"<>\\"
    };

    /*** Swiss French keyboard layout (contributed by Philippe Froidevaux) */
    private static final String[] main_key_SF = {
	"§°", "1+|", "2\"@", "3*#", "4ç", "5%", "6&¬", "7/¦", "8(¢", "9)", "0=",
	"'?´", "^`~",
	"qQ", "wW", "eE", "rR", "tT", "zZ", "uU", "iI", "oO", "pP", "èü[",
	"¨!]",
	"aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", "éö", "àä{",
	"$£}",
	"yY", "xX", "cC", "vV", "bB", "nN", "mM", ",;", ".:", "-_",
	"<>\\"
    };

    /*** Norwegian keyboard layout (contributed by Ove Kåven) */
    private static final String[] main_key_NO = {
	"|§", "1!", "2\"@", "3#£", "4¤$", "5%", "6&", "7/{", "8([", "9)]",
	"0=}", "+?", "\\`´",
	"qQ", "wW", "eE", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "åÅ", "¨^~",
	"aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", "øØ", "æÆ", "'*",
	"zZ", "xX", "cC", "vV", "bB", "nN", "mM", ",;", ".:", "-_",
	"<>"
    };
    
    /*** Danish keyboard layout (contributed by Bertho Stultiens) */
    private static final String[] main_key_DA = {
	"½§", "1!", "2\"@", "3#£", "4¤$", "5%", "6&", "7/{", "8([", "9)]",
	"0=}", "+?", "´`|",
	"qQ", "wW", "eE", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "åÅ", "¨^~",
	"aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", "æÆ", "øØ", "'*",
	"zZ", "xX", "cC", "vV", "bB", "nN", "mM", ",;", ".:", "-_",
	"<>\\"
    };
    
    /*** Swedish keyboard layout (contributed by Peter Bortas) */
    private static final String[] main_key_SE = {
	"§½", "1!", "2\"@", "3#£", "4¤$", "5%", "6&", "7/{", "8([", "9)]",
	"0=}", "+?\\", "´`",
	"qQ", "wW", "eE", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "åÅ", "¨^~",
	"aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", "öÖ", "äÄ", "'*",
	"zZ", "xX", "cC", "vV", "bB", "nN", "mM", ",;", ".:", "-_",
	"<>|"
    };

    /*** Canadian French keyboard layout */
    private static final String[] main_key_CF = {
	"#|\\", "1!±", "2\"@", "3/£", "4$¢", "5%¤", "6?¬", "7&¦", "8*²", "9(³",
	"0)¼", "-_½", "=+¾",
	"qQ", "wW", "eE", "rR", "tT", "yY", "uU", "iI", "oO§", "pP¶", "^^[",
	"¸¨]",
	"aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", ";:~", "``{",
	"<>}",
	"zZ", "xX", "cC", "vV", "bB", "nN", "mM", ",'-", ".", "éÉ",
	"«»°"
    };
    
    /*** Portuguese keyboard layout */
    private static final String[] main_key_PT = {
	"\\¦", "1!", "2\"@", "3#£", "4$§", "5%", "6&", "7/{", "8([", "9)]",
	"0=}", "'?", "«»",
	"qQ", "wW", "eE", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "+*\\¨",
	"\\'\\`",
	"aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", "çÇ", "ºª",
	"\\~\\^",
	"zZ", "xX", "cC", "vV", "bB", "nN", "mM", ",;", ".:", "-_",
	"<>"
    };

    /*** Italian keyboard layout */
    private static final String[] main_key_IT = {
	"\\|", "1!¹", "2\"²", "3£³", "4$¼", "5%½", "6&¾", "7/{", "8([", "9)]",
	"0=}", "'?`", "ì^~",
	"qQ@", "wW", "eE", "rR", "tT", "yY", "uU", "iI", "oOø", "pPþ", "èé[",
	"+*]",
	"aA", "sSß", "dDð", "fF", "gG", "hH", "jJ", "kK", "lL", "òç@", "à°#",
	"ù§",
	"zZ", "xX", "cC", "vV", "bB", "nN", "mMµ", ",;", ".:·", "-_",
	"<>|"
    };

    /*** Finnish keyboard layout */
    private static final String[] main_key_FI = {
	"", "1!", "2\"@", "3#", "4$", "5%", "6&", "7/{", "8([", "9)]", "0=}",
	"+?\\", "\'`",
	"qQ", "wW", "eE", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "", "\"^~",
	"aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", "", "", "'*",
	"zZ", "xX", "cC", "vV", "bB", "nN", "mM", ",;", ".:", "-_",
	"<>|"
    };
    
    /*** Russian keyboard layout (contributed by Pavel Roskin) */
    private static final String[] main_key_RU = {
	"`~", "1!", "2@", "3#", "4$", "5%", "6^", "7&", "8*", "9(", "0)", "-_",
	"=+",
	"qQÊê", "wWÃã", "eEÕõ", "rRËë", "tTÅå", "yYÎî", "uUÇç", "iIÛû", "oOÝý",
	"pPÚú", "[{Èè", "]}ßÿ",
	"aAÆæ", "sSÙù", "dD×÷", "fFÁá", "gGÐð", "hHÒò", "jJÏï", "kKÌì", "lLÄä",
	";:Öö", "'\"Üü", "\\|",
	"zZÑñ", "xXÞþ", "cCÓó", "vVÍí", "bBÉé", "nNÔô", "mMØø", ",<Ââ", ".>Àà",
	"/?"
    };
    
    /*** Russian keyboard layout KOI8-R */
    private static final String[] main_key_RU_koi8r = {
	"()", "1!", "2\"", "3/", "4$", "5:", "6,", "7.", "8;", "9?", "0%", "-_",
	"=+",
	"Êê", "Ãã", "Õõ", "Ëë", "Åå", "Îî", "Çç", "Ûû", "Ýý", "Úú", "Èè", "ßÿ",
	"Ææ", "Ùù", "×÷", "Áá", "Ðð", "Òò", "Ïï", "Ìì", "Ää", "Öö", "Üü", "\\|",
	"Ññ", "Þþ", "Óó", "Íí", "Éé", "Ôô", "Øø", "Ââ", "Àà", "/?",
	"<>"			/* the phantom key */
    };
    
    /*** Spanish keyboard layout (contributed by José Marcos López) */
    private static final String[] main_key_ES = {
	"ºª\\", "1!|", "2\"@", "3·#", "4$", "5%", "6&¬", "7/", "8(", "9)", "0=",
	"'?", "¡¿",
	"qQ", "wW", "eE", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "`^[",
	"+*]",
	"aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", "ñÑ", "'¨{",
	"çÇ}",
	"zZ", "xX", "cC", "vV", "bB", "nN", "mM", ",;", ".:", "-_",
	"<>"
    };

    /*** Belgian keyboard layout ***/
    private static final String[] main_key_BE = {
	"", "&1|", "é2@", "\"3#", "'4", "(5", "§6^", "è7", "!8", "ç9{", "à0}",
	")°", "-_",
	"aA", "zZ", "eE¤", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "^¨[",
	"$*]",
	"qQ", "sSß", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", "mM", "ù%´",
	"µ£`",
	"wW", "xX", "cC", "vV", "bB", "nN", ",?", ";.", ":/", "=+~",
	"<>\\"
    };
    
    /*** Hungarian keyboard layout (contributed by Zoltán Kovács) */
    private static final String[] main_key_HU = {
	"0§", "1'~", "2\"·", "3+^", "4!¢", "5%°", "6/²", "7=`", "8(ÿ", "9)´",
	"öÖ½", "üÜ¨", "óÓ¸",
	"qQ\\", "wW|", "eE", "rR", "tT", "zZ", "uU", "iIÍ", "oOø", "pP", "õÕ÷",
	"úÚ×",
	"aA", "sSð", "dDÐ", "fF[", "gG]", "hH", "jJí", "kK³", "lL£", "éÉ$",
	"áÁß", "ûÛ¤",
	"yY>", "xX#", "cC&", "vV@", "bB{", "nN}", "mM", ",?;", ".:·", "-_*",
	"íÍ<"
    };

    /*** Polish (programmer's) keyboard layout ***/
    private static final String[] main_key_PL = {
	"`~", "1!", "2@", "3#", "4$", "5%", "6^", "7&§", "8*", "9(", "0)", "-_",
	"=+",
	"qQ", "wW", "eEêÊ", "rR", "tT", "yY", "uU", "iI", "oOóÓ", "pP", "[{",
	"]}",
	"aA±¡", "sS¶¦", "dD", "fF", "gG", "hH", "jJ", "kK", "lL³£", ";:", "'\"",
	"\\|",
	"zZ¿¯", "xX¼¬", "cCæÆ", "vV", "bB", "nNñÑ", "mM", ",<", ".>", "/?",
	"<>|"
    };
    
    /*** Croatian keyboard layout specific for me <jelly@srk.fer.hr> ***/
    private static final String[] main_key_HR_jelly = {
	"`~", "1!", "2@", "3#", "4$", "5%", "6^", "7&", "8*", "9(", "0)", "-_",
	"=+",
	"qQ", "wW", "eE", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "[{¹©",
	"]}ðÐ",
	"aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", ";:èÈ", "'\"æÆ",
	"\\|¾®",
	"zZ", "xX", "cC", "vV", "bB", "nN", "mM", ",<", ".>", "/?",
	"<>|"
    };

    /*** Croatian keyboard layout ***/
    private static final String[] main_key_HR = {
	"¸¨", "1!", "2\"·", "3#^", "4$¢", "5%°", "6&²", "7/`", "8(ÿ", "9)´",
	"0=½", "'?¨", "+*¸",
	"qQ\\", "wW|", "eE", "rR", "tT", "zZ", "uU", "iI", "oO", "pP", "¹©÷",
	"ðÐ×",
	"aA", "sS", "dD", "fF[", "gG]", "hH", "jJ", "kK³", "lL£", "èÈ", "æÆß",
	"¾®¤",
	"yY", "xX", "cC", "vV@", "bB{", "nN}", "mM§", ",;", ".:", "-_/",
	"<>"
    };
    
    /*** Japanese 106 keyboard layout ***/
    private static final String[] main_key_JA_jp106 = {
	"1!", "2\"", "3#", "4$", "5%", "6&", "7'", "8(", "9)", "0~", "-=", "^~",
	"\\|",
	"qQ", "wW", "eE", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "@`", "[{",
	"aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", ";+", ":*", "]}",
	"zZ", "xX", "cC", "vV", "bB", "nN", "mM", ",<", ".>", "/?",
	"\\_",
    };

    /*** Japanese pc98x1 keyboard layout ***/
    private static final String[] main_key_JA_pc98x1 = {
	"1!", "2\"", "3#", "4$", "5%", "6&", "7'", "8(", "9)", "0", "-=", "^`",
	"\\|",
	"qQ", "wW", "eE", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "@~", "[{",
	"aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", ";+", ":*", "]}",
	"zZ", "xX", "cC", "vV", "bB", "nN", "mM", ",<", ".>", "/?",
	"\\_",
    };
    
    /*** Brazilian ABNT-2 keyboard layout (contributed by Raul Gomes Fernandes) */
    private static final String[] main_key_PT_br = {
	"'\"", "1!", "2@", "3#", "4$", "5%", "6\"", "7&", "8*", "9(", "0)",
	"-_", "=+",
	"qQ", "wW", "eE", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "'`", "[{",
	"aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", "çÇ", "~^", "]}",
	"zZ", "xX", "cC", "vV", "bB", "nN", "mM", ",<", ".>", "/?"
    };

    /*** US international keyboard layout (contributed by Gustavo Noronha (kov@debian.org)) */
    private static final String[] main_key_US_intl = {
	"`~", "1!", "2@", "3#", "4$", "5%", "6^", "7&", "8*", "9(", "0)", "-_",
	"=+", "\\|",
	"qQ", "wW", "eE", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "[{", "]}",
	"aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", ";:", "'\"",
	"zZ", "xX", "cC", "vV", "bB", "nN", "mM", ",<", ".>", "/?"
    };

    /*** Slovak keyboard layout (see cssk_ibm(sk_qwerty) in xkbsel)
	 - dead_abovering replaced with degree - no symbol in iso8859-2
	 - brokenbar replaced with bar					*/
    private static final String[] main_key_SK = {
	";°`'", "+1", "µ2", "¹3", "è4", "»5", "¾6", "ý7", "á8", "í9", "é0)",
	"=%", "",
	"qQ\\", "wW|", "eE", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "ú/÷",
	"ä(×",
	"aA", "sSð", "dDÐ", "fF[", "gG]", "hH", "jJ", "kK³", "lL£", "ô\"$",
	"§!ß", "ò)¤",
	"zZ>", "xX#", "cC&", "vV@", "bB{", "nN}", "mM", ",?<", ".:>", "-_*",
	"<>\\|"
    };

    /*** Slovak and Czech (programmer's) keyboard layout (see cssk_dual(cs_sk_ucw)) */
    private static final String[] main_key_SK_prog = {
	"`~", "1!", "2@", "3#", "4$", "5%", "6^", "7&", "8*", "9(", "0)", "-_",
	"=+",
	"qQäÄ", "wWìÌ", "eEéÉ", "rRøØ", "tT»«", "yYýÝ", "uUùÙ", "iIíÍ", "oOóÓ",
	"pPöÖ", "[{", "]}",
	"aAáÁ", "sS¹©", "dDïÏ", "fFëË", "gGàÀ", "hHúÚ", "jJüÜ", "kKôÔ", "lLµ¥",
	";:", "'\"", "\\|",
	"zZ¾®", "xX¤", "cCèÈ", "vVçÇ", "bB", "nNòÒ", "mMåÅ", ",<", ".>", "/?",
	"<>"
    };
    
    /*** Czech keyboard layout (see cssk_ibm(cs_qwerty) in xkbsel) */
    private static final String[] main_key_CS = {
	";", "+1", "ì2", "¹3", "è4", "ø5", "¾6", "ý7", "á8", "í9", "é0½)", "=%",
	"",
	"qQ\\", "wW|", "eE", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "ú/[{",
	")(]}",
	"aA", "sSð", "dDÐ", "fF[", "gG]", "hH", "jJ", "kK³", "lL£", "ù\"$",
	"§!ß", "¨'",
	"zZ>", "xX#", "cC&", "vV@", "bB{", "nN}", "mM", ",?<", ".:>", "-_*",
	"<>\\|"
    };

    /*** Latin American keyboard layout (contributed by Gabriel Orlando Garcia) */
    private static final String[] main_key_LA = {
	"|°¬", "1!", "2\"", "3#", "4$", "5%", "6&", "7/", "8(", "9)", "0=",
	"'?\\", "¡¿",
	"qQ@", "wW", "eE", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "´¨",
	"+*~",
	"aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", "ñÑ", "{[^",
	"}]`",
	"zZ", "xX", "cC", "vV", "bB", "nN", "mM", ",;", ".:", "-_",
	"<>"
    };

    /*** Lithuanian (Baltic) keyboard layout (contributed by Nerijus Baliûnas) */
    private static final String[] main_key_LT_B = {
	"`~", "àÀ", "èÈ", "æÆ", "ëË", "áÁ", "ðÐ", "øØ", "ûÛ", "((", "))", "-_",
	"þÞ",
	"qQ", "wW", "eE", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "[{", "]}",
	"aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", ";:", "'\"",
	"\\|",
	"zZ", "xX", "cC", "vV", "bB", "nN", "mM", ",<", ".>", "/?"
    };

    /*** Turkish keyboard Layout */
    private static final String[] main_key_TK = {
	"\"é", "1!", "2'", "3^#", "4+$", "5%", "6&", "7/{", "8([", "9)]", "0=}",
	"*?\\", "-_",
	"qQ@", "wW", "eE", "rR", "tT", "yY", "uU", "ýIî", "oO", "pP", "ðÐ",
	"üÜ~",
	"aAæ", "sSß", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", "þÞ", "iÝ",
	",;`",
	"zZ", "xX", "cC", "vV", "bB", "nN", "mM", "öÖ", "çÇ", ".:"
    };

    /*** Layout table. Add your keyboard mappings to this list */
    
    public KeyLayout() {
    }
    /*
    public static KeyCode getLayout(long layout) {
	
	switch((int)layout) {
	    
	case(0x0000080c):
	    return new KeyCode(main_key_BE);

	case(0x00000c0c):
	    return new KeyCode(main_key_CF);

	case(0x0000041A):
	    return new KeyCode(main_key_HR);

	case(0x00020405):
	    return new KeyCode(main_key_SK_prog);
		
	case(0x00000405):
	    return new KeyCode(main_key_CS);

	case(0x00000406):
	    return new KeyCode(main_key_DA);

	case(0x0000040b):
	    return new KeyCode(main_key_FI);

	case(0x0000040d):
	    return new KeyCode(main_key_FR);
	    
	case(0x00000407):
	    return new KeyCode(main_key_DE_nodead);

	case(0x0000040e):
	    return new KeyCode(main_key_HU);

	case(0x0000040f):
	    return new KeyCode(main_key_IS);

	case(0x00000410):
	    return new KeyCode(main_key_IT);

	case(0x00000411):
	    return new KeyCode(main_key_JA_jp106);

	case(0x0000080a):
	    return new KeyCode(main_key_LA);
	    
	case(0x00010427):
	    return new KeyCode(main_key_LT_B);

	case(0x00000414):
	    return new KeyCode(main_key_NO);

	case(0x00000415):
	    return new KeyCode(main_key_PL);

	case(0x00010416):
	    return new KeyCode(main_key_PT_br);

	case(0x00000816):
	    return new KeyCode(main_key_PT);

	case(0x00000419):
	    return new KeyCode(main_key_RU);

	case(0x0000041b):
	    return new KeyCode(main_key_SK);

	case(0x0000040a):
	    return new KeyCode(main_key_ES);

	case(0x0000041d):
	    return new KeyCode(main_key_SE);

	case(0x0000100c):
	    return new KeyCode(main_key_SF);

	case(0x00000807):
	    return new KeyCode(main_key_SG);

	case(0x0000041f):
	    return new KeyCode(main_key_TK);

	case(0x00000409):
	    return new KeyCode(main_key_US);

	case(0x00000809):
	    return new KeyCode(main_key_UK);
	    
	default:
	    return new KeyCode(main_key_US);
	    }
	    }*/
}

/*
 * These Values are copied directly from the Rdesktop source.
 * They resemble nearly all possible Windows Keyboard types
 * in the form: "ABRREV_NAME, LONG_NAME, KEYBOARDCODE (in unsigned int) and Pointer to
 * a Layout Structure
 
 {"SQ", "Albanian", 0x0000041c, NULL}
 {"AR", "Arabic (101)", 0x00000401, NULL},
 {NULL, "Arabic (102) AZERTY", 0x00020401, NULL},
 {NULL, "Arabic (102)", 0x00010401, NULL},
 {NULL, "Armenian Eastern", 0x0000042b, NULL},
 {NULL, "Armenian Western", 0x0001042b, NULL},
 {NULL, "Azeri Cyrillic", 0x0000082c, NULL},
 {NULL, "Azeri Latin", 0x0000042c, NULL},
 {"BE", "Belarusian", 0x00000423, NULL},
 {NULL, "Belgian (Comma)", 0x0001080c, NULL},
 {NULL, "Belgian Dutch", 0x00000813, NULL},
 {NULL, "Belgian French", 0x0000080c, &main_key_BE},
 {NULL, "Bulgarian (Latin)", 0x00010402, NULL},
 {"BG", "Bulgarian", 0x00000402, NULL},
 {"CF", "Canadian French (Legacy)", 0x00000c0c, &main_key_CF},
 {NULL, "Canadian French", 0x00001009, NULL},
 {NULL, "Canadian Multilingual Standard", 0x00011009, NULL},
 {NULL, "Chinese (Simplified) - MS-PinYin98", 0xE00E0804, NULL},
 {NULL, "Chinese (Simplified) - NeiMa", 0xE0050804, NULL},
 {NULL, "Chinese (Simplified) - QuanPin", 0xE0010804, NULL},
 {NULL, "Chinese (Simplified) - ShuangPin", 0xE0020804, NULL},
 {NULL, "Chinese (Simplified) - US Keyboard", 0x00000804, NULL},
 {NULL, "Chinese (Simplified) - ZhengMa", 0xE0030804, NULL},
 {NULL, "Chinese (Traditional) - Alphanumeric", 0xE01F0404, NULL},
 {NULL, "Chinese (Traditional) - Array", 0xE0050404, NULL},
 {NULL, "Chinese (Traditional) - Big5 Code", 0xE0040404, NULL},
 {NULL, "Chinese (Traditional) - ChangJie", 0xE0020404, NULL},
 {NULL, "Chinese (Traditional) - DaYi", 0xE0060404, NULL},
 {NULL, "Chinese (Traditional) - New ChangJie", 0xE0090404, NULL},
 {NULL, "Chinese (Traditional) - New Phonetic", 0xE0080404, NULL},
 {NULL, "Chinese (Traditional) - Phonetic", 0xE0010404, NULL},
 {NULL, "Chinese (Traditional) - Quick", 0xE0030404, NULL},
 {"ZH", "Chinese (Traditional) - US Keyboard", 0x00000404, NULL},
 {NULL, "Chinese (Traditional) - Unicode", 0xE0070404, NULL},
 {"HR", "Croatian", 0x0000041A, &main_key_HR},
 {NULL, "Croatian (specific)", 0x0000041A, &main_key_HR_jelly},
 {NULL, "Czech (QWERTY)", 0x00010405, NULL},
 {NULL, "Czech Programmers", 0x00020405, &main_key_SK_prog},
 {"CS", "Czech", 0x00000405, &main_key_CS},
 {"DA", "Danish", 0x00000406, &main_key_DA},
 {NULL, "Devanagari - INSCRIPT", 0x00000439, NULL},
 {"NL", "Dutch", 0x00000413, NULL},
 {"ET", "Estonian", 0x00000425, NULL},
 {"FO", "Faeroese", 0x00000438, NULL},
 {NULL, "Farsi", 0x00000429, NULL},
 {"FI", "Finnish", 0x0000040b, &main_key_FI},
 {"FR", "French", 0x0000040c, &main_key_FR},
 {"GD", "Gaelic", 0x00011809, NULL},
 {"KA", "Georgian", 0x00000437, NULL},
 {NULL, "German (IBM)", 0x00010407, NULL},
 {"DE", "German", 0x00000407, &main_key_DE},
 {NULL, "German (without dead keys)", 0x00000407, &main_key_DE_nodead},
 {NULL, "Greek (220) Latin", 0x00030408, NULL},
 {NULL, "Greek (220)", 0x00010408, NULL},
 {NULL, "Greek (319) Latin", 0x00040408, NULL},
 {NULL, "Greek (319)", 0x00020408, NULL},
 {NULL, "Greek Latin", 0x00050408, NULL},
 {NULL, "Greek Polytonic", 0x00060408, NULL},
 {"EL", "Greek", 0x00000408, NULL},
 {"HE", "Hebrew", 0x0000040d, NULL},
 {"HI", "Hindi Traditional", 0x00010439, NULL},
 {NULL, "Hungarian 101-key", 0x0001040e, NULL},
 {"HU", "Hungarian", 0x0000040e, &main_key_HU},
 {"IS", "Icelandic", 0x0000040f, &main_key_IS},
 {"GA", "Irish", 0x00001809, NULL},
 {NULL, "Italian (142)", 0x00010410, NULL},
 {"IT", "Italian", 0x00000410, &main_key_IT},
 {NULL, "Japanese Input System (MS-IME2000)", 0xE0010411, NULL},
 {"JA", "Japanese", 0x00000411, NULL},
 {NULL, "Japanese (106 keyboard)", 0x00000411, &main_key_JA_jp106},
 {NULL, "Japanese (pc98x1 keyboard)", 0x00000411, &main_key_JA_pc98x1},
 {"KK", "Kazakh", 0x0000043f, NULL},
 {NULL, "Korean(Hangul) (MS-IME98)", 0xE0010412, NULL},
 {"KO", "Korean(Hangul)", 0x00000412, NULL},
 {"LA", "Latin American", 0x0000080a, &main_key_LA},
 {NULL, "Latvian (QWERTY)", 0x00010426, NULL}
 {"LV", "Latvian", 0x00000426, NULL},
 {NULL, "Lithuanian IBM", 0x00000427, NULL},
 {"LT", "Lithuanian", 0x00010427, &main_key_LT_B},
 {"MK", "Macedonian (FYROM)", 0x0000042f, NULL},
 {"MR", "Marathi", 0x0000044e, NULL},
 {"NO", "Norwegian", 0x00000414, &main_key_NO},
 {NULL, "Polish (214)", 0x00010415, NULL},
 {"PL", "Polish (Programmers)", 0x00000415, &main_key_PL},
 {"BR", "Portuguese (Brazilian ABNT)", 0x00000416, NULL},
 {NULL, "Portuguese (Brazilian ABNT2)", 0x00010416, &main_key_PT_br},
 {"PT", "Portuguese", 0x00000816, &main_key_PT},
 {"RO", "Romanian", 0x00000418, NULL},
 {NULL, "Russian (Typewriter)", 0x00010419, NULL},
 {"RU", "Russian", 0x00000419, &main_key_RU},
 {NULL, "Russian (KOI8-R)", 0x00000419, &main_key_RU_koi8r},
 {NULL, "Serbian (Cyrillic)", 0x00000c1a, NULL},
 {"SR", "Serbian (Latin)", 0x0000081a, NULL},
 {NULL, "Slovak (QWERTY)", 0x0001041b, NULL},
 {"SK", "Slovak", 0x0000041b, &main_key_SK},
 {NULL, "Slovak (programers)", 0x0000041b, &main_key_SK_prog},
 {"SL", "Slovenian", 0x00000424, NULL},
 {NULL, "Spanish Variation", 0x0001040a, NULL},
 {"ES", "Spanish", 0x0000040a, &main_key_ES},
 {"SV", "Swedish", 0x0000041d, &main_key_SE},
 {"SF", "Swiss French", 0x0000100c, &main_key_SF},
 {"SG", "Swiss German", 0x00000807, &main_key_SG},
 {"TA", "Tamil", 0x00000449, NULL},
 {"TT", "Tatar", 0x00000444, NULL},
 {NULL, "Thai Kedmanee (non-ShiftLock)", 0x0002041e, NULL},
 {"TH", "Thai Kedmanee", 0x0000041e, NULL},
 {NULL, "Thai Pattachote (non-ShiftLock)", 0x0003041e, NULL},
 {NULL, "Thai Pattachote", 0x0001041e, NULL},
 {"TR", "Turkish F", 0x0001041f, NULL},
 {NULL, "Turkish Q", 0x0000041f, &main_key_TK},
 {NULL, "US English Table for IBM Arabic 238_L", 0x00050409, NULL},
 {"US", "US", 0x00000409, &main_key_US},
 {NULL, "US (phantom key version)", 0x00000409, &main_key_US_phantom},
 {"UK", "Ukrainian", 0x00000422, NULL},
 {"EN", "English", 0x00000809, &main_key_UK},
 {NULL, "United States-Dvorak for left hand", 0x00030409, NULL},
 {NULL, "United States-Dvorak for right hand", 0x00040409, NULL},
 {"DV", "United States-Dvorak", 0x00010409, NULL},
 {NULL, "United States-International", 0x00020409, &main_key_US_intl},
 {"UZ", "Uzbek Cyrillic", 0x00000843, NULL},
 {"VI", "Vietnamese", 0x0000042a, NULL},
*/



