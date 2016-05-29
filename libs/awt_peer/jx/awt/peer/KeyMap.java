package jx.awt.peer;


import jx.wm.Keycode;
import java.awt.event.KeyEvent;


/**
 * This class provides a key mapping from the WindowManager's Keycode class
 * to the AWT's KeyEvent class.
 */
public class KeyMap {

    /**
     * Translates from Keycode to KeyEvent, where code is the keyValue from
     * the WindowManager.
     */
    public static int translate(int code) {
	switch (code) {
	case Keycode.VK_UP_ARROW:return KeyEvent.VK_UP;
	case Keycode.VK_DOWN_ARROW:return KeyEvent.VK_DOWN;
	case Keycode.VK_LEFT_ARROW:return KeyEvent.VK_LEFT;
	case Keycode.VK_RIGHT_ARROW:return KeyEvent.VK_RIGHT;
	case Keycode.VK_SPACE:return KeyEvent.VK_SPACE;
	case Keycode.VK_ENTER:return KeyEvent.VK_ENTER;
	case Keycode.VK_ESCAPE:return KeyEvent.VK_ESCAPE;
	case Keycode.VK_TAB:return KeyEvent.VK_TAB;
	case Keycode.VK_DELETE:return KeyEvent.VK_DELETE;
	case Keycode.VK_HOME:return KeyEvent.VK_HOME;
	case Keycode.VK_END:return KeyEvent.VK_END;
	case Keycode.VK_PAGE_UP:return KeyEvent.VK_PAGE_UP;
	case Keycode.VK_PAGE_DOWN:return KeyEvent.VK_PAGE_DOWN;
	default:
	    return code;
	}
    }

    /**
     * Translates from Keycode to KeyEvent, where code is the keyValue and rawcode
     * is the rawValue from the WindowManager.
     */
    public static int translate(int code, int rawcode) {
	switch (rawcode) {
	case 0x5d:return KeyEvent.VK_ALT;
	case 0x5c:return KeyEvent.VK_CONTROL;
	case 0x4b:return KeyEvent.VK_SHIFT;
	default:
	    return translate(code);
	}
    }

    /**
     * Tells whether the shift key has been pressed or not. modifier is the modifier
     * value from the WindowManager.
     */
    public static boolean shiftPressed(int modifier) {
	return ((modifier & 0x01) != 0);
    }

    /**
     * Tells whether the alt key has been pressed or not. modifier is the modifier
     * value from the WindowManager.
     */
    public static boolean altPressed(int modifier) {
	return ((modifier & 0x10) != 0);
    }

    /**
     * Tells whether the ctrl key has been pressed or not. modifier is the modifier
     * value from the WindowManager.
     */
    public static boolean ctrlPressed(int modifier) {
	return ((modifier & 0x04) != 0);
    }
}
