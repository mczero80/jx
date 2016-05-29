/* KeyEvent.java -- Event for key presses.
   Copyright (C) 1999 Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
02111-1307 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */


package java.awt.event;

import java.awt.Component;

/**
  * This event is generated when a key is pressed or released.
  *
  * @author Aaron M. Renn (arenn@urbanophile.com)
  */
public class KeyEvent extends InputEvent implements java.io.Serializable
{

/*
 * Static Variables
 */

/**
  * This is the first id in the range of event ids used by this class.
  */
public static final int KEY_FIRST = 400;

/**
  * This is the last id in the range of event ids used by this class.
  */
public static final int KEY_LAST = 402;

/**
  * This event id indicates a key was typed, which is a key press
  * followed by a key release.
  */
public static final int KEY_TYPED = 400;

/**
  * This event id indicates a key was pressed.
  */
public static final int KEY_PRESSED = 401;

/**
  * This event it indicates a key was released.
  */
public static final int KEY_RELEASED = 402;

// Virtual key constants, document later

public static final int VK_0 = '0';
public static final int VK_1 = '1';
public static final int VK_2 = '2';
public static final int VK_3 = '3';
public static final int VK_4 = '4';
public static final int VK_5 = '5';
public static final int VK_6 = '6';
public static final int VK_7 = '7';
public static final int VK_8 = '8';
public static final int VK_9 = '9';

public static final int VK_A = 'A';
public static final int VK_B = 'B';
public static final int VK_C = 'C';
public static final int VK_D = 'D';
public static final int VK_E = 'E';
public static final int VK_F = 'F';
public static final int VK_G = 'G';
public static final int VK_H = 'H';
public static final int VK_I = 'I';
public static final int VK_J = 'J';
public static final int VK_K = 'K';
public static final int VK_L = 'L';
public static final int VK_M = 'M';
public static final int VK_N = 'N';
public static final int VK_O = 'O';
public static final int VK_P = 'P';
public static final int VK_Q = 'Q';
public static final int VK_R = 'R';
public static final int VK_S = 'S';
public static final int VK_T = 'T';
public static final int VK_U = 'U';
public static final int VK_V = 'V';
public static final int VK_W = 'W';
public static final int VK_X = 'X';
public static final int VK_Y = 'Y';
public static final int VK_Z = 'Z';

public static final int VK_AMPERSAND = '&';
public static final int VK_ASTERISK = '*';
public static final int VK_AT = '@';
public static final int VK_BACK_QUOTE = '`';
public static final int VK_BACK_SLASH = '\\';
public static final int VK_BACK_SPACE = '\b';
public static final int VK_BRACELEFT = '{';
public static final int VK_BRACERIGHT = '}';
public static final int VK_CIRCUMFLEX = '^';
public static final int VK_OPEN_BRACKET = '[';
public static final int VK_CLOSE_BRACKET = ']';
public static final int VK_COLON = ':';
public static final int VK_COMMA = ',';
public static final int VK_DOLLAR = '$';
public static final int VK_ENTER = '\n';
public static final int VK_EQUALS = '=';
public static final int VK_EXCLAMATION_POINT = '!';
public static final int VK_GREATER = '>';
public static final int VK_LESS = '<';
public static final int VK_LEFT_PARENTHESIS = '(';
public static final int VK_RIGHT_PARENTHESIS = ')';
public static final int VK_MINUS = '-';
public static final int VK_NUMBER_SIGN = '#';
public static final int VK_PERIOD = '.';
public static final int VK_PLUS = '+';
public static final int VK_QUOTE = '"';
public static final int VK_SEMICOLON = ';';
public static final int VK_SLASH = '/';
public static final int VK_SPACE = ' ';
public static final int VK_TAB = '\t';
public static final int VK_UNDERSCORE = '_';

public static final int VK_F1 = 0x70;
public static final int VK_F2 = 0x71;
public static final int VK_F3 = 0x72;
public static final int VK_F4 = 0x73;
public static final int VK_F5 = 0x74;
public static final int VK_F6 = 0x75;
public static final int VK_F7 = 0x76;
public static final int VK_F8 = 0x77;
public static final int VK_F9 = 0x78;
public static final int VK_F10 = 0x79;
public static final int VK_F11 = 0x7A;
public static final int VK_F12 = 0x7B;

public static final int VK_NUMPAD0 = 0x60;
public static final int VK_NUMPAD1 = 0x61;
public static final int VK_NUMPAD2 = 0x62;
public static final int VK_NUMPAD3 = 0x63;
public static final int VK_NUMPAD4 = 0x64;
public static final int VK_NUMPAD5 = 0x65;
public static final int VK_NUMPAD6 = 0x66;
public static final int VK_NUMPAD7 = 0x67;
public static final int VK_NUMPAD8 = 0x68;
public static final int VK_NUMPAD9 = 0x69;

public static final int VK_ADD = 0x6B;
public static final int VK_SUBTRACT = 0x6D;
public static final int VK_MULTIPLY = 0x6A;
public static final int VK_DIVIDE = 0x6F;
public static final int VK_DECIMAL = 0x6E;
public static final int VK_SEPARATER = 0x6C;

public static final int VK_LEFT = 0x25;
public static final int VK_RIGHT = 0x27;
public static final int VK_UP = 0x26;
public static final int VK_DOWN = 0x38;

public static final int VK_HOME = 0x24;
public static final int VK_END = 0x23;
public static final int VK_PAGE_UP = 0x21;
public static final int VK_PAGE_DOWN = 0x22;

public static final int VK_ALT = 0x12;
public static final int VK_CANCEL = 0x03;
public static final int VK_CAPS_LOCK = 0x14;
public static final int VK_CLEAR = 0x0C;
public static final int VK_CONTROL = 0x11;
public static final int VK_DELETE = 0x7F;
public static final int VK_ESCAPE = 0x1B;
public static final int VK_HELP = 0x9C;
public static final int VK_INSERT = 0x9B;
public static final int VK_META = 0x9D;
public static final int VK_NUM_LOCK = 0x90;
public static final int VK_PAUSE = 0x13;
public static final int VK_PRINTSCREEN = 0x9A;
public static final int VK_SCROLL_LOCK = 0x91;
public static final int VK_SHIFT = 0x10;

public static final int VK_CONVERT = 0x1C;
public static final int VK_NONCONVERT = 0x1D;
public static final int VK_ACCEPT = 0x1E;
public static final int VK_FINAL = 0x18;
public static final int VK_MODECHANGE = 0x1F;

public static final int VK_KANA = 0x15;
public static final int VK_KANJI = 0x19;

public static final int VK_UNDEFINED = 0;
public static final char CHAR_UNDEFINED = 0;

// FIXME: Java 1.2 keys I don't know the code for yet.
/*
public static final int VK_ALL_CANDIDATES =
public static final int VK_ALPHANUMERIC =
public static final int VK_ALT_GRAPH =
public static final int VK_CODE_INPUT =
public static final int VK_COMPOSE =
public static final int VK_COPY =
public static final int VK_CUT = 
public static final int VK_EURO_SIGN =
public static final int VK_FIND =
public static final int VK_FULL_WIDTH =
public static final int VK_HALF_WIDTH
public static final int VK_HIRAGANA =
public static final int VK_INVERTED_EXCLAMATION_POINT =
public static final int VK_JAPANESE_HIRAGANA =
public static final int VK_JAPANESE_KATAKANA =
public static final int VK_JAPANESE_ROMAN =
public static final int VK_KATAKANA =
public static final int VK_PASTE = 
public static final int VK_PREVIOUS_CANDIDATE =
public static final int VK_QUOTEDBL =
public static final int VK_ROMAN_CHARACTERS =
public static final int VK_STOP =

public static final int VK_KP_UP =
public static final int VK_KP_DOWN =
public static final int VK_KP_LEFT =
public static final int VK_KP_RIGHT =

public static final int VK_DEAD_ABOVEDOT =
public static final int VK_DEAD_ABOVERING =
public static final int VK_DEAD_ACUTE =
public static final int VK_DEAD_BREVE =
public static final int VK_DEAD_CARON =
public static final int VK_DEAD_CEDILLA =
public static final int VK_DEAD_CIRCUMFLEX =
public static final int VK_DEAD_DIAERESIS =
public static final int VK_DEAD_DOUBLEACUTE =
public static final int VK_DEAD_GRAVE =
public static final int VK_DEAD_IOTA =
public static final int VK_DEAD_MACRON =
public static final int VK_DEAD_OGONEK =
public static final int VK_DEAD_SEMIVOICED_SOUND =
public static final int VK_DEAD_TILDE =
public static final int VK_DEAD_VOICED_SOUND =

public static final int VK_F13 =
public static final int VK_F14 =
public static final int VK_F15 =
public static final int VK_F16 =
public static final int VK_F17 =
public static final int VK_F18 =
public static final int VK_F19 =
public static final int VK_F20 =
public static final int VK_F21 =
public static final int VK_F22 =
public static final int VK_F23 =
public static final int VK_F24 =
*/

/*************************************************************************/

/*
 * Instance Variables
 */

/**
  * @serial The VK_ code for this key
  */
private int keyCode; 

/**
  * @serial  The Unicode value for this key
  */
private char keyChar;

/*************************************************************************/

/*
 * Static Methods
 */

/**
  * Returns the text name of the event key.
  *
  * @return The text name of the event key.
  */
public static String
getKeyText(int keyCode)
{
  // FIXME: Needs to read values from property file
  return("Unknown");
}

/*************************************************************************/

/**
  * Returns a string indicating the modifier keys in effect.
  *
  * @return A string describing the modifier keys in effect.
  */
public static String
getModifiersText(int modifiers)
{
  // FIXME: Needs to read values from property file
  return("Unknown");
}

/*************************************************************************/

/*
 * Constructors
 */

/**
  * Initializes a new instance of <code>KeyEvent</code> with the specified
  * information.
  *
  * @param source The component that generated this event.
  * @param id The event id.
  * @param when The timestamp when the even occurred.
  * @param modifiers The modifier keys pressed during the event.
  * @param keyCode The integer constant from this class identifying the key.
  * @param keyChar The Unicode value of the key, or 
  * <code>CHAR_UNDEFINED</code> if one does not exist.
  */
public 
    KeyEvent(Component source, int id, /*long when,*/ int modifiers,
         int keyCode, char keyChar)
{
    super(source, id, /*when,*/ modifiers);
  this.keyCode = keyCode;
  this.keyChar = keyChar;
}
  
/*************************************************************************/

/**
  * Initializes a new instance of <code>KeyEvent</code> with the specified
  * information.
  *
  * @param source The component that generated this event.
  * @param id The event id.
  * @param when The timestamp when the even occurred.
  * @param modifiers The modifier keys pressed during the event.
  * @param keyCode The integer constant from this class identifying the key.
  */
public 
    KeyEvent(Component source, int id, /*long when,*/ int modifiers, int keyCode)
{
    this(source, id, /*when,*/ modifiers, keyCode, CHAR_UNDEFINED);
}

/*************************************************************************/

/*
 * Instance Variables
 */

/**
  * Returns the key code for the event key.  This will be one of the VK_
  * constants defined in this class.
  *
  * @return The key code for this event.
  */
public int
getKeyCode()
{
  return(keyCode);
}

/*************************************************************************/

/**
  * Sets the key code for this event.  This must be one of the VK_
  * constants defined in this class.
  *
  * @param keyCode The new key code for this event.
  */
public void
setKeyCode(int keyCode)
{
  this.keyCode = keyCode;
}

/*************************************************************************/

/**
  * Returns the Unicode value for the event key.  This will be 
  * <code>CHAR_UNDEFINED</code> if there is no Unicode equivalent for
  * this key.
  *
  * @return The Unicode character for this event.
  */
public char
getKeyChar()
{
  return(keyChar);
}

/*************************************************************************/

/**
  * Sets the Unicode character for this event to the specified value.
  *
  * @param keyChar The new Unicode character for this event.
  */
public void
setKeyChar(char keyChar)
{
  this.keyChar = keyChar;
}

/*************************************************************************/

/**
  * Sets the modifier keys to the specified value.  This should be a union
  * of the bit mask constants from <code>InputEvent</code>.
  *
  * @param modifiers The new modifier value.
  */
public void
setModifiers(int modifiers)
{
  this.modifiers = modifiers;
}

/*************************************************************************/

/**
  * Tests whether or not this key is an action key as defined in
  * <code>java.awt.Event</code>
  *
  * @return <code>true</code> if this is an action key, or <code>false</code>
  * if it is not.
  */
public boolean
isActionKey()
{
  switch(keyCode)
    {
      case VK_F1:
      case VK_F2:
      case VK_F3:
      case VK_F4:
      case VK_F5:
      case VK_F6:
      case VK_F7:
      case VK_F8:
      case VK_F9:
      case VK_F10:
      case VK_F11:
      case VK_F12:
      case VK_PAGE_UP:
      case VK_PAGE_DOWN:
      case VK_HOME:
      case VK_END:
      case VK_PRINTSCREEN:
      case VK_SCROLL_LOCK:
      case VK_NUM_LOCK:
      case VK_PAUSE:
      case VK_INSERT:
        return(true);

      default:
        return(false);
    }
}

/*************************************************************************/

/**
  * Returns a string identifying the event.
  *
  * @return A string identifying the event.
  */
public String
paramString()
{
  return(getClass().getName() + " keyCode=" + getKeyCode() + " modifiers=" +
         getModifiers());
}

} // class KeyEvent 

