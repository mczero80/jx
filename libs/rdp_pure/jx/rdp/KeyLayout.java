package jx.rdp;

public class KeyLayout {

    /*** Layout table. Add your keyboard mappings to this list */
    
    /**
       main_key_JX speichert die Windows Scancodes
       und nutzt dabei die rohen Scancodes des Keyboard PC Tastaturtreibers
       Die JX Scancodes verweisen auf den Array Index an dem der entsprechende Windows Scancode
       steht
    */

    private static final int[] main_key_JX = {
	/*       Taste; Nummer(Decimal) ; JX Scancode */
	0x00, // No Code; 000; 0x00
	0x01, // ESC ; 001 ; 0x01
	0x3b, // F1 ; 002 ; 0x02
	0x3c, // F2 ; 003 ; 0x03
	0x3d, // F3 ; 004 ; 0x04
	0x3e, // F4 ; 005 ; 0x05
	0x3f, // F5 ; 006 ; 0x06
	0x40, // F6 ; 007 ; 0x07
	0x41, // F7 ; 008 ; 0x08
	0x42, // F8 ; 009 ; 0x09
	0x43, // F9 ; 010 ; 0x0a
	0x44, // F10 ; 011 ; 0x0b
	0x57, // F11 ; 012 ; 0x0c
	0x58, // F12 ; 013 ; 0x0d
	0xb7, // PRINT SCREEN ; 014 ; 0x0e
	0x46, // SCROLL LOCK ; 015 ; 0x0f !!NOT USED!!
	0x00, // No Code (Pause?) ; 016 ; 0x10
	0x29, // ~` ; 017, 0x11
	0x02, // 1 ; 018 ; 0x12
	0x03, // 2 ; 019 ; 0x13
	0x04, // 3 ; 020 ; 0x14
	0x05, // 4 ; 021 ; 0x15
	0x06, // 5 ; 022 ; 0x16
	0x07, // 6 ; 023 ; 0x17
	0x08, // 7 ; 024 ; 0x18
	0x09, // 8 ; 025 ; 0x19
	0x0a, // 9 ; 026 ; 0x1a
	0x0b, // 0 ; 027 ; 0x1b
	0x0c, // - ; 028 ; 0x1c
	0x0d, // = ; 029 ; 0x1d
	0x0e, // BACKSPACE ; 030 ; 0x1e
	0xd2, // INS ; 031 ; 0x1f
	0xc7, // HOME ; 032 ; 0x20	
	0xc9, // PG UP ; 033 ; 0x21
	0x45, // NUM LOCK ; 034 ; 0x22
	0xb5, // Keypad / ; 035 ; 0x23
	0x37, // Keypad * ; 036 ; 0x24
	0x4a, // Keypad - ; 037 ; 0x25
	0x0f, // TAB ; 038; 0x26
	0x10, // q ; 039 ; 0x27
	0x11, // w ; 040 ; 0x28
	0x12, // e ; 041 ; 0x29
	0x13, // r ; 042 ; 0x2a
	0x14, // t ; 043 ; 0x2b
	0x15, // y ; 044 ; 0x2c
	0x16, // u ; 045 ; 0x2d
	0x17, // i ; 046 ; 0x2e
	0x18, // o ; 047 ; 0x2f
	0x19, // p ; 048 ; 0x30
	0x1A, // [ ; 049 ; 0x31
	0x1B, // ] ; 050 ; 0x32
	0x00, // NOT DEFINED ; 051 ; 0x33
	0xd3, // DEL ; 052 ; 0x34 
	0xcf, // END ; 053 ; 0x35
	0xd1, // PG DOWN ; 054 ; 0x36
	0x47, // Keypad 7 ; 055 ; 0x37
	0x48, // Keypad 8 ; 056 ; 0x38
	0x49, // Keypad 9 ; 057 ; 0x39
	0x4e, // Keypad + ; 058 ; 0x3a
	0x3a, // CAPS LOCK ; 059 ; 0x3b
	0x1E, // a ; 060 ; 0x3c
	0x1F, // s ; 061 ; 0x3d
	0x20, // d ; 062 ; 0x3e
	0x21, // f ; 063 ; 0x3f
	0x22, // g ; 064 ; 0x40
	0x23, // h ; 065 ; 0x41
	0x24, // j ; 066 ; 0x42
	0x25, // k ; 067 ; 0x43
	0x26, // l ; 068 ; 0x44
	0x27, // ; ; 069 ; 0x45
	0x28, // ' ; 070 ; 0x46
	0x1c, // RETURN ; 071 ; 0x47
	0x4b, // Keypad 4 ; 072 ; 0x48
	0x4c, // Keypad 5 ; 073 ; 0x49
	0x4d, // Keypad 6 ; 074 ; 0x4a
	0x2a, // LSHIFT ; 075 ; 0x4b
	0x2C, // z ; 076 ; 0x4c
	0x2D, // x ; 077 ; 0x4d
	0x2E, // c ; 078 ; 0x4e
	0x2F, // v ; 079 ; 0x4f
	0x30, // b ; 080 ; 0x50
	0x31, // n ; 081 ; 0x51
	0x32, // m ; 082 ; 0x52
	0x33, // , ; 083 ; 0x53
	0x34, // . ; 084 ; 0x54
	0x35, // / ; 085 ; 0x55
	0x36, // RSHIFT ; 085 ; 0x56
	0xc8, // UP ARROW ; 086 ; 0x57
	0x4f, // Keypad 1 ; 087 ; 0x58
	0x50, // Keypad 2 ; 088 ; 0x59
	0x51, // Keypad 3 ; 089 ; 0x5a
	0x9c, // Keypad ENTER ; 090 ; 0x5b
	0x1d, // LCONTROL ; 091 ; 0x5c
	0x38, // LALT ; 092 ; 0x5d
	0x39, // SPACE ; 093 ; 0x5e
	0xb8, // RALT ; 094 ; 0x5f
	0x9d, // RCONTROL ; 095 ; 0x60
	0xcb, // LEFT ARROW ; 096 ; 0x61
	0xc0, // DOWN ARROW ; 097 ; 0x62
	0xcd, // RIGHT ARROW ; 098 ; 0x63
	0x52, // Keypad 0 ; 099 ; 0x64
	0x53, // Keypad . ; 100 ; 0x65
	0x00, // NOT DEFINED ; 101 ; 0x66
	0x00, // NOT DEFINED ; 102 ; 0x67
	0x00, // NOT DEFINED ; 103 ; 0x68
	0x00, // NOT DEFINED ; 104 ; 0x69
	0x00, // NOT DEFINED ; 105 ; 0x70
	0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	0,0,0,0,0,0,0,0,0,0  //Fill to 256 Bytes (for future use)
    };
    
    public static final int JX_INTEL = 0x01;

    public KeyLayout() {
    }
    
    public static KeyCode getLayout(int layout) {
	
	switch(layout) {
	    
	case(1):
	    return new KeyCode(main_key_JX);

	default:
	    return new KeyCode(main_key_JX);
	}
    }
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



