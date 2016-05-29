package jx.awt;


import java.awt.*;
import java.awt.peer.*;
import java.awt.image.*;
import jx.awt.peer.*;
import jx.awt.image.*;
//import java.awt.datatransfer.*;


/**
 * This class implements the class Toolkit from the AWT. It provides
 * access to some general resources like the screen's size as well as
 * to the JX AWT peer classes. It also has interface methods to access
 * some AWT-wide unique resources used by the JX implementation, like
 * the event queue or the different JX handlers.
 */
public class JXToolkit
    extends java.awt.Toolkit {
    
    
    private EventQueue eventQueue;
    private JXMenuThread menuThread;
    private MenuHandler menuHandler;
    private FocusHandler focusHandler;
    private SlaveWindowHandler slaveWindowHandler;
    
    /** The screen's width */
    private int screenX;
    /** The screen's height */
    private int screenY;
    


    /**
     * Default contructor for the toolkit. It initializes all used resources
     * if called the first time. DO NOT CALL IT DIRECTLY! The AWT will call
     * it itself.
     */
    public JXToolkit() {
	if (eventQueue == null) {
	    //System.out.println("*** JX Toolkit has been called the first time...");
	    eventQueue = new EventQueue();
	    menuThread = new JXMenuThread(this);
	    menuHandler = new MenuHandler(this);
	    focusHandler = new FocusHandler(this);
	    slaveWindowHandler = new SlaveWindowHandler();
	    //System.out.println("*** everything initialized.");
	} else {
	    //System.out.println("--- JX Toolkit called another time!!!");
	}
    }
    
    /*************************************************************************/
    
    /**
     * Gets the currently working menu thread.
     */
    public JXMenuThread getMenuThread() {
	return menuThread;
    }

    /**
     * Gets the menu handler.
     */
    public MenuHandler getMenuHandler() {
	return menuHandler;
    }
    
    /**
     * Gets the focus handler.
     */
    public FocusHandler getFocusHandler() {
	return focusHandler;
    }
    
    /**
     * Gets the slave window handler.
     */
    public SlaveWindowHandler getSlaveWindowHandler() {
	return slaveWindowHandler;
    }

    /**
     * This method loads the alternate color scheme which must be
     * specified in the file "jxcolors.ini" packed into JX.
     */
    public void loadAlternateColors() {    
	JXColors colors = new JXColors();
	colors.loadColorInformation();
    }

    /**
     * Creates a peer object for the specified <code>Button</code>.
     *
     * @param target The <code>Button</code> to create the peer for.
     *
     * @return The peer for the specified <code>Button</code> object.
     */
    protected  ButtonPeer createButton(java.awt.Button target) { 
	return new JXButtonPeer(target, this);
    }
    
    /*************************************************************************/
    
    /**
     * Creates a peer object for the specified <code>TextField</code>.
     *
     * @param target The <code>TextField</code> to create the peer for.
     *
     * @return The peer for the specified <code>TextField</code> object.
     */
    protected  TextFieldPeer createTextField(TextField target) {
	return new JXTextFieldPeer(target, this);
    }
    
    /*************************************************************************/
    
    /**
     * Creates a peer object for the specified <code>Label</code>.
     *
     * @param target The <code>Label</code> to create the peer for.
     *
     * @return The peer for the specified <code>Label</code> object.
     */
    protected  LabelPeer createLabel(java.awt.Label target) { 
	return new JXLabelPeer(target, this);
    }
    
    /*************************************************************************/
    
    /**
     * Creates a peer object for the specified <code>List</code>.
     *
     * @param target The <code>List</code> to create the peer for.
     *
     * @return The peer for the specified <code>List</code> object.
     */
    protected  ListPeer createList(List target) {
	return new JXListPeer(target, this);
    }
    
    /*************************************************************************/
    
    /**
     * Creates a peer object for the specified <code>Checkbox</code>.
     *
     * @param target The <code>Checkbox</code> to create the peer for.
     *
     * @return The peer for the specified <code>Checkbox</code> object.
     */
    protected  CheckboxPeer createCheckbox(Checkbox target) {
	return new JXCheckboxPeer(target, this);
    }
    
    /*************************************************************************/
    
    /**
     * Creates a peer object for the specified <code>Scrollbar</code>.
     *
     * @param target The <code>Scrollbar</code> to create the peer for.
     *
     * @return The peer for the specified <code>Scrollbar</code> object.
     */
    protected  ScrollbarPeer createScrollbar(Scrollbar target) {
	return new JXScrollbarPeer(target, this);
    }
    
    /*************************************************************************/
    
    /**
     * Creates a peer object for the specified <code>ScrollPane</code>.
     *
     * @param target The <code>ScrollPane</code> to create the peer for.
     *
     * @return The peer for the specified <code>ScrollPane</code> object.
     */
    protected  ScrollPanePeer createScrollPane(ScrollPane target) {
	return new JXScrollPanePeer(target, this);
    }
    
    /*************************************************************************/
    
    /**
     * Creates a peer object for the specified <code>TextArea</code>.
     *
     * @param target The <code>TextArea</code> to create the peer for.
     *
     * @return The peer for the specified <code>TextArea</code> object.
     */
    protected  TextAreaPeer createTextArea(TextArea target) {
	return new JXTextAreaPeer(target, this);
    }
    
    /*************************************************************************/
    
    /**
     * Creates a peer object for the specified <code>Choice</code>.
     *
     * @param target The <code>Choice</code> to create the peer for.
     *
     * @return The peer for the specified <code>Choice</code> object.
     */
    protected  ChoicePeer createChoice(Choice target) {
	return new JXChoicePeer(target, this);
    }
    
    /*************************************************************************/
    
    /**
     * Creates a peer object for the specified <code>Frame</code>.
     *
     * @param target The <code>Frame</code> to create the peer for.
     *
     * @return The peer for the specified <code>Frame</code> object.
     */
    protected  FramePeer createFrame(java.awt.Frame target) {
	return new JXFramePeer(target, this);
    }
    
    /*************************************************************************/
    
    /**
     * Creates a peer object for the specified <code>Canvas</code>.
     *
     * @param target The <code>Canvas</code> to create the peer for.
     *
     * @return The peer for the specified <code>Canvas</code> object.
     */
    protected  CanvasPeer createCanvas(Canvas target) {
	return new JXCanvasPeer(target, this);
    }
    
    /*************************************************************************/
    
    /**
     * Creates a peer object for the specified <code>Panel</code>.
     *
     * @param target The <code>Panel</code> to create the peer for.
     *
     * @return The peer for the specified <code>Panel</code> object.
     */
    protected  PanelPeer createPanel(Panel target) {
	return new JXPanelPeer(target, this);
    }
    
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
    /*protected  DialogPeer
      createDialog(Dialog target) { throw new Error("not implemented"); }
    */
    /*************************************************************************/
    
    /**
     * Creates a peer object for the specified <code>MenuBar</code>.
     *
     * @param target The <code>MenuBar</code> to create the peer for.
     *
     * @return The peer for the specified <code>MenuBar</code> object.
     */
    protected  MenuBarPeer createMenuBar(MenuBar target) {
	return new JXMenuBarPeer(target, this);
    }
    
    /*************************************************************************/
    
    /**
     * Creates a peer object for the specified <code>Menu</code>.
     *
     * @param target The <code>Menu</code> to create the peer for.
     *
     * @return The peer for the specified <code>Menu</code> object.
     */
    protected  MenuPeer createMenu(Menu target) {
	return new JXMenuPeer(target, this);
    }
    
    /*************************************************************************/
    
    /**
     * Creates a peer object for the specified <code>PopupMenu</code>.
     *
     * @param target The <code>PopupMenu</code> to create the peer for.
     *
     * @return The peer for the specified <code>PopupMenu</code> object.
     */
    protected  PopupMenuPeer createPopupMenu(PopupMenu target) {
	return new JXPopupMenuPeer(target, this);
    }
    
    
    
    /*************************************************************************/
    
    /**
     * Creates a peer object for the specified <code>MenuItem</code>.
     *
     * @param target The <code>MenuItem</code> to create the peer for.
     *
     * @return The peer for the specified <code>MenuItem</code> object.
     */
    protected  MenuItemPeer createMenuItem(MenuItem target) {
	return new JXMenuItemPeer(target, this);
    }
    
    /*************************************************************************/
    
    /**
     * Creates a peer object for the specified <code>FileDialog</code>.
     *
     * @param target The <code>FileDialog</code> to create the peer for.
     *
     * @return The peer for the specified <code>FileDialog</code> object.
     */
    /*protected  FileDialogPeer
      createFileDialog(FileDialog target) { throw new Error("not implemented"); }
    */
    /*************************************************************************/
    
    /**
     * Creates a peer object for the specified <code>CheckboxMenuItem</code>.
     *
     * @param target The <code>CheckboxMenuItem</code> to create the peer for.
     *
     * @return The peer for the specified <code>CheckboxMenuItem</code> object.
     */
    protected  CheckboxMenuItemPeer createCheckboxMenuItem(CheckboxMenuItem target) {
	return new JXCheckboxMenuItemPeer(target, this);
    }
    
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
	throw new Error("createComponent: not yet implemented.");
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
	throw new Error("loadSystemColors: not implemented!");
    }
    
    /*************************************************************************/
    
    /**
     * Returns the dimensions of the screen in pixels.
     *
     * @return The dimensions of the screen in pixels.
     */
    public  Dimension
	getScreenSize() {
	if (screenX == 0 || screenY == 0) {
	    // find out screen resolution
	    GeneralConnector tempConnector = new JXMenuConnector();
	    screenX = tempConnector.getDisplayWidth();
	    screenY = tempConnector.getDisplayHeight();
	}
	return new Dimension(screenX, screenY);
    }
    
    /*************************************************************************/
    
    /**
     * Returns the screen resolution in dots per square inch.
     *
     * @return The screen resolution in dots per square inch.
     */
    public  int
	getScreenResolution() {
	return 72 * 72;
    }
    
    /*************************************************************************/
    
    /**
     * Returns the color model of the screen.
     *
     * @return The color model of the screen.
     */
    /*public  ColorModel
      getColorModel() { throw new Error("not implemented"); }
    */
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
    public void sync() {
	throw new Error("not implemented");
    }
    
    /*************************************************************************/
    
    /**
     * Returns an image from the specified file, which must be in a 
     * recognized format.  Supported formats vary from toolkit to toolkit.
     *
     * @return name The name of the file to read the image from.
     */
    public Image getImage(String name) {
	JXImageLoader l = new JXImageLoader();
	return l.loadImage(name);
    }
    
    /*************************************************************************/
    
    /**
     * Returns an image from the specified URL, which must be in a 
     * recognized format.  Supported formats vary from toolkit to toolkit.
     *
     * @return url The URl to read the image from.
     */
    /*public Image getImage(URL url) {
      throw new Error("not implemented");
      }*/
    
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
    /*public  boolean
      prepareImage(Image image, int width, int height, ImageObserver observer) { throw new Error("not implemented"); }
    */
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
    /*public  int
      checkImage(Image image, int width, int height, ImageObserver observer) { throw new Error("not implemented"); }
    */
    /*************************************************************************/
    
    /**
     * Creates an image using the specified <code>ImageProducer</code>
     *
     * @param producer The <code>ImageProducer</code> to create the image from.
     *
     * @return The created image.
     */
    public java.awt.Image createImage(ImageProducer producer) {
	JXImageCreator c = new JXImageCreator();
	return c.createImage(producer);
    }
    
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
    /*public  Image
      createImage(byte[] date, int offset, int len) { throw new Error("not implemented"); }
    */
    /*************************************************************************/
    
    /**
     * Creates an image from the specified byte array. The array must be in
     * a recognized format.  Supported formats vary from toolkit to toolkit.
     *
     * @param data The raw image data.
     *
     * @return The created image.
     */
    /*public Image
      createImage(byte[] data)
      {
      return(createImage(data, 0, data.length));
      }
      
      public java.awt.Image createImage(java.net.URL u){throw new Error();}
      public java.awt.Image createImage(java.lang.String s){throw new Error();}
    */
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
    /*public java.awt.PrintJob getPrintJob(java.awt.Frame frame, String title, java.util.Properties props) 
    //public PrintJob
    //getPrintJob(Frame frame, String title, Properties props) 
    { 
    throw new Error("not implemented");
    }
    */
    /*************************************************************************/
    
    /**
     * Returns the system clipboard.
     *
     * @return THe system clipboard.
     */
    //    public  Clipboard
    //	getSystemClipboard() { return null; /*throw new Error("not implemented");*/ }
    
    /*************************************************************************/


    /**
     * Returns the accelerator key mask for menu shortcuts. The default is
     * <code>Event.CTRL_MASK</code>.  A toolkit must override this method
     * to change the default.
     *
     * @return The key mask for the menu accelerator key.
     */
    /*public int
      getMenuShortcutKeyMask()
      {
      return(Event.CTRL_MASK);
      }*/
    
    /*************************************************************************/
    
    /*************************************************************************/
    
    /**
     * Returns the current event queue. As the JX implementation uses only one
     * AWT-wide event queue, this queue is returned.
     */
    protected  EventQueue getSystemEventQueueImpl() {
	return eventQueue;
    }
    
    /*************************************************************************/
    
    /**
     * Causes a "beep" tone to be generated.
     */
    public  void
	beep() { throw new Error("not implemented"); }
    
    /*public java.util.Map mapInputMethodHighlight(java.awt.im.InputMethodHighlight i) {throw new Error();
      }
      public java.awt.dnd.peer.DragSourceContextPeer createDragSourceContextPeer(java.awt.dnd.DragGestureEvent d) {throw new Error();}
    */
} 
