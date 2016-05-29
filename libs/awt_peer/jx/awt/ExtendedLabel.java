package jx.awt;


import java.awt.Label;


/**
 * This class provides an extended label that is able
 * to show more than one line of text.
 */
public class ExtendedLabel
    extends Label {
    // this class has nothing special to do
    // it just extends Label, and the JXLabelPeer
    // will recognize whether it is an ExtendedLabel
    // and tread it accordingly.

    /**
     * Creates a new ExtendedLabel. The text may contain
     * several '\n', which will be used to cut the text
     * into several rows.
     */
    public ExtendedLabel(String text) {
	super(text);
    }

    /**
     * Creates a new ExtendedPanel with no text. Use setText()
     * to set a multiple-rows text.
     */
    public ExtendedLabel() {
	super();
    }
}
