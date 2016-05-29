package jx.awt;

import java.awt.*;
import java.awt.peer.*;
import java.awt.image.*;
import java.net.URL;

import java.awt.datatransfer.Clipboard;
import java.util.Properties;

/*
import java.awt.LightweightPeer;
import java.awt.Frame;
import java.awt.Window;
import java.awt.Container;
import java.awt.Component;
*/

public class Toolkit extends java.awt.Toolkit
{

/*
 * Constructors
 */

/**
  * Default constructor for subclasses.
  */
public Toolkit()
{
}

/*************************************************************************/

/*
 * Instance Methods
 */

/**
  * Creates a peer object for the specified <code>Button</code>.
  *
  * @param target The <code>Button</code> to create the peer for.
  *
  * @return The peer for the specified <code>Button</code> object.
  */
protected  ButtonPeer createButton(java.awt.Button target) { 
    return new Button(this);
}

/*************************************************************************/

/**
  * Creates a peer object for the specified <code>TextField</code>.
  *
  * @param target The <code>TextField</code> to create the peer for.
  *
  * @return The peer for the specified <code>TextField</code> object.
  */
protected  TextFieldPeer
createTextField(TextField target) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Creates a peer object for the specified <code>Label</code>.
  *
  * @param target The <code>Label</code> to create the peer for.
  *
  * @return The peer for the specified <code>Label</code> object.
  */
protected  LabelPeer
createLabel(Label target) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Creates a peer object for the specified <code>List</code>.
  *
  * @param target The <code>List</code> to create the peer for.
  *
  * @return The peer for the specified <code>List</code> object.
  */
protected  ListPeer
createList(List target) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Creates a peer object for the specified <code>Checkbox</code>.
  *
  * @param target The <code>Checkbox</code> to create the peer for.
  *
  * @return The peer for the specified <code>Checkbox</code> object.
  */
protected  CheckboxPeer
createCheckbox(Checkbox target) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Creates a peer object for the specified <code>Scrollbar</code>.
  *
  * @param target The <code>Scrollbar</code> to create the peer for.
  *
  * @return The peer for the specified <code>Scrollbar</code> object.
  */
protected  ScrollbarPeer
createScrollbar(Scrollbar target) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Creates a peer object for the specified <code>ScrollPane</code>.
  *
  * @param target The <code>ScrollPane</code> to create the peer for.
  *
  * @return The peer for the specified <code>ScrollPane</code> object.
  */
protected  ScrollPanePeer
createScrollPane(ScrollPane target) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Creates a peer object for the specified <code>TextArea</code>.
  *
  * @param target The <code>TextArea</code> to create the peer for.
  *
  * @return The peer for the specified <code>TextArea</code> object.
  */
protected  TextAreaPeer
createTextArea(TextArea target) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Creates a peer object for the specified <code>Choice</code>.
  *
  * @param target The <code>Choice</code> to create the peer for.
  *
  * @return The peer for the specified <code>Choice</code> object.
  */
protected  ChoicePeer
createChoice(Choice target) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Creates a peer object for the specified <code>Frame</code>.
  *
  * @param target The <code>Frame</code> to create the peer for.
  *
  * @return The peer for the specified <code>Frame</code> object.
  */
protected  FramePeer createFrame(java.awt.Frame target) {
    return new Frame(this);
}

/*************************************************************************/

/**
  * Creates a peer object for the specified <code>Canvas</code>.
  *
  * @param target The <code>Canvas</code> to create the peer for.
  *
  * @return The peer for the specified <code>Canvas</code> object.
  */
protected  CanvasPeer
createCanvas(Canvas target) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Creates a peer object for the specified <code>Panel</code>.
  *
  * @param target The <code>Panel</code> to create the peer for.
  *
  * @return The peer for the specified <code>Panel</code> object.
  */
protected  PanelPeer
createPanel(Panel target) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Creates a peer object for the specified <code>Window</code>.
  *
  * @param target The <code>Window</code> to create the peer for.
  *
  * @return The peer for the specified <code>Window</code> object.
  */
protected  WindowPeer
createWindow(java.awt.Window target) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Creates a peer object for the specified <code>Dialog</code>.
  *
  * @param target The dialog to create the peer for
  *
  * @return The peer for the specified font name.
  */
protected  DialogPeer
createDialog(Dialog target) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Creates a peer object for the specified <code>MenuBar</code>.
  *
  * @param target The <code>MenuBar</code> to create the peer for.
  *
  * @return The peer for the specified <code>MenuBar</code> object.
  */
protected  MenuBarPeer
createMenuBar(MenuBar target) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Creates a peer object for the specified <code>Menu</code>.
  *
  * @param target The <code>Menu</code> to create the peer for.
  *
  * @return The peer for the specified <code>Menu</code> object.
  */
protected  MenuPeer
createMenu(Menu target) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Creates a peer object for the specified <code>PopupMenu</code>.
  *
  * @param target The <code>PopupMenu</code> to create the peer for.
  *
  * @return The peer for the specified <code>PopupMenu</code> object.
  */
protected  PopupMenuPeer
createPopupMenu(PopupMenu target) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Creates a peer object for the specified <code>MenuItem</code>.
  *
  * @param target The <code>MenuItem</code> to create the peer for.
  *
  * @return The peer for the specified <code>MenuItem</code> object.
  */
protected  MenuItemPeer
createMenuItem(MenuItem target) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Creates a peer object for the specified <code>FileDialog</code>.
  *
  * @param target The <code>FileDialog</code> to create the peer for.
  *
  * @return The peer for the specified <code>FileDialog</code> object.
  */
protected  FileDialogPeer
createFileDialog(FileDialog target) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Creates a peer object for the specified <code>CheckboxMenuItem</code>.
  *
  * @param target The <code>CheckboxMenuItem</code> to create the peer for.
  *
  * @return The peer for the specified <code>CheckboxMenuItem</code> object.
  */
protected  CheckboxMenuItemPeer
createCheckboxMenuItem(CheckboxMenuItem target) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Creates a peer object for the specified <code>Component</code>.  The
  * peer returned by this method is not a native windowing system peer
  * with its own native window.  Instead, this method allows the component
  * to draw on its parent window as a "lightweight" widget.
  *
  * XXX: FIXME
  *
  * @param target The <code>Component</code> to create the peer for.
  *
  * @return The peer for the specified <code>Component</code> object.
  */
protected LightweightPeer
createComponent(Component target)
{
  return null;
}

/*************************************************************************/

/**
  * Creates a peer object for the specified font name.
  *
  * @param name The font to create the peer for.
  * @param style The font style to create the peer for.
  *
  * @return The peer for the specified font name.
  */
protected  FontPeer
getFontPeer(String name, int style) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Copies the current system colors into the specified array.  This is
  * the interface used by the <code>SystemColors</code> class.
  *
  * @param colors The array to copy the system colors into.
  */
protected void
loadSystemColors(int systemColors[])
{
}

/*************************************************************************/

/**
  * Returns the dimensions of the screen in pixels.
  *
  * @return The dimensions of the screen in pixels.
  */
public  Dimension
getScreenSize() { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Returns the screen resolution in dots per square inch.
  *
  * @return The screen resolution in dots per square inch.
  */
public  int
getScreenResolution() { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Returns the color model of the screen.
  *
  * @return The color model of the screen.
  */
public  ColorModel
getColorModel() { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Returns the names of the available fonts.
  *
  * @return The names of the available fonts.
  */
public  String[]
getFontList() { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Return the font metrics for the specified font
  *
  * @param name The name of the font to return metrics for.
  *
  * @return The requested font metrics.
  */
public  FontMetrics
getFontMetrics(Font name) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Flushes any buffered data to the screen so that it is in sync with 
  * what the AWT system has drawn to it.
  */
public  void
sync() { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Returns an image from the specified file, which must be in a 
  * recognized format.  Supported formats vary from toolkit to toolkit.
  *
  * @return name The name of the file to read the image from.
  */
public  Image
getImage(String name) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Returns an image from the specified URL, which must be in a 
  * recognized format.  Supported formats vary from toolkit to toolkit.
  *
  * @return url The URl to read the image from.
  */
public  Image
getImage(URL url) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Readies an image to be rendered on the screen.  The width and height
  * values can be set to the default sizes for the image by passing -1
  * in those parameters.
  *
  * @param image The image to prepare for rendering.
  * @param width The width of the image.
  * @param height The height of the image.
  * @param observer The observer to receive events about the preparation
  * process.
  *
  * @return <code>true</code> if the image is already prepared for rendering,
  * <code>false</code> otherwise.
  */
public  boolean
prepareImage(Image image, int width, int height, ImageObserver observer) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Checks the status of specified image as it is being readied for 
  * rendering.
  *
  * @param image The image to prepare for rendering.
  * @param width The width of the image.
  * @param height The height of the image.
  * @param observer The observer to receive events about the preparation
  * process.
  *
  * @return A union of the bitmasks from 
  * <code>java.awt.image.ImageObserver</code> that indicates the current
  * state of the imaging readying process.
  */
public  int
checkImage(Image image, int width, int height, ImageObserver observer) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Creates an image using the specified <code>ImageProducer</code>
  *
  * @param producer The <code>ImageProducer</code> to create the image from.
  *
  * @return The created image.
  */
public  java.awt.Image
createImage(ImageProducer producer) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Creates an image from the specified portion of the byte array passed.
  * The array must be in a recognized format.  Supported formats vary from
  * toolkit to toolkit.
  *
  * @param data The raw image data.
  * @param offset The offset into the data where the image data starts.
  * @param len The length of the image data.
  *
  * @return The created image.
  */
public  Image
createImage(byte[] date, int offset, int len) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Creates an image from the specified byte array. The array must be in
  * a recognized format.  Supported formats vary from toolkit to toolkit.
  *
  * @param data The raw image data.
  *
  * @return The created image.
  */
public Image
createImage(byte[] data)
{
  return(createImage(data, 0, data.length));
}

public java.awt.Image createImage(java.net.URL u){throw new Error();}
public java.awt.Image createImage(java.lang.String s){throw new Error();}

/*************************************************************************/

/**
  * Returns a instance of <code>PrintJob</code> for the specified 
  * arguments.
  *
  * @param frame The window initiating the print job.
  * @param title The print job title.
  * @param props The print job properties.
  *
  * @return The requested print job, or <code>null</code> if the job
  * was cancelled.
  */
public  PrintJob getPrintJob(java.awt.Frame frame, String title, Properties props) { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Returns the system clipboard.
  *
  * @return THe system clipboard.
  */
public  Clipboard
getSystemClipboard() { throw new Error("not implemented"); }

/*************************************************************************/

/**
  * Returns the accelerator key mask for menu shortcuts. The default is
  * <code>Event.CTRL_MASK</code>.  A toolkit must override this method
  * to change the default.
  *
  * @return The key mask for the menu accelerator key.
  */
public int
getMenuShortcutKeyMask()
{
  return(Event.CTRL_MASK);
}

/*************************************************************************/

/*************************************************************************/

/**
  * // FIXME: What does this do?
  */
protected  EventQueue getSystemEventQueueImpl() {
    Dbg.msg("Toolkit.getSystemEventQueueImpl");
    throw new Error();
}

/*************************************************************************/

/**
  * Causes a "beep" tone to be generated.
  */
public  void
beep() { throw new Error("not implemented"); }

public java.util.Map mapInputMethodHighlight(java.awt.im.InputMethodHighlight i) {throw new Error();
}
public java.awt.dnd.peer.DragSourceContextPeer createDragSourceContextPeer(java.awt.dnd.DragGestureEvent d) {throw new Error();}

} 
