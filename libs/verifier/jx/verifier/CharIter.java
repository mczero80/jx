package jx.verifier;

/** Class to iterate through a String.*/
public class CharIter {
    private String string;
    private int actChar;
    /** indicates the end of the String*/
    //FEHLER: oh weh ist das haesslich!
    public static final char DONE = 3;
    /** returns the character at the actual position (i.e. the same character the last call to <code>next()</code> returned).
     * @return the character or <code>CharIter.DONE</code> if there are no more characters.
     */
    public char current() { return (actChar<string.length())?string.charAt(actChar):DONE;}
    
    /** returns the next character from the String.
     * @return the character or <code>CharIter.DONE</code> if there are no more characters.
     */
    public char next() { actChar = actChar+1; return current();}

    /** Create new Iterator.
     * @param string The string from which the characters should be taken.
     */
    public CharIter(String string) {
	actChar = 0;
	this.string = string;
    }

}
