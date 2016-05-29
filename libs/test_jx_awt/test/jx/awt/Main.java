package test.jx.awt;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import jx.awt.*;
import jx.zero.*;

public class Main    implements ActionListener {
    static final boolean extendedTest = false;
    static final boolean rdpTest = true;


    ListenerDisplay ld;



    public static void main(String args[]) {
	Main m = new Main();
	m.doit();
    }

    public void doit() {
	// start background picture
	ScreenBackground sbg = new ScreenBackground();
	sbg.show();

	JXTaskBar tb = new JXTaskBar();
	ld = new ListenerDisplay();

	MenuItem infoItem = new MenuItem("about...");
	infoItem.setActionCommand("info");
	infoItem.addActionListener(this);
	MenuItem rebootItem = new MenuItem("reboot!");
	rebootItem.setActionCommand("reboot");
	rebootItem.addActionListener(this);
	Menu demosItem = new Menu("AWT demos");
	MenuItem d1 = new MenuItem("Label Demo");
	d1.setActionCommand("Label");
	d1.addActionListener(this);
	MenuItem d2 = new MenuItem("Button Demo");
	d2.setActionCommand("Button");
	d2.addActionListener(this);
	MenuItem d3 = new MenuItem("Checkbox Demo");
	d3.setActionCommand("Checkbox");
	d3.addActionListener(this);
	MenuItem d4 = new MenuItem("Scrollbar Demo");
	d4.setActionCommand("Scrollbar");
	d4.addActionListener(this);
	MenuItem d5 = new MenuItem("Choice Demo");
	d5.setActionCommand("Choice");
	d5.addActionListener(this);
	MenuItem d6 = new MenuItem("TextField Demo");
	d6.setActionCommand("TextField");
	d6.addActionListener(this);
	MenuItem d7 = new MenuItem("TextArea Demo");
	d7.setActionCommand("TextArea");
	d7.addActionListener(this);
	MenuItem d8 = new MenuItem("Canvas Demo");
	d8.setActionCommand("Canvas");
	d8.addActionListener(this);
	MenuItem d9 = new MenuItem("List Demo");
	d9.setActionCommand("List");
	d9.addActionListener(this);
	MenuItem d10 = new MenuItem("Menu Demo");
	d10.setActionCommand("Menu");
	d10.addActionListener(this);
	MenuItem d11 = new MenuItem("PopupMenu Demo");
	d11.setActionCommand("Popup");
	d11.addActionListener(this);
	MenuItem d12 = new MenuItem("ScrollPane Demo");
	d12.setActionCommand("ScrollPane");
	d12.addActionListener(this);
	MenuItem d13 = new MenuItem("Image Demo");
	d13.setActionCommand("Image");
	d13.addActionListener(this);
	MenuItem d14 = new MenuItem("Font Demo");
	d14.setActionCommand("Font");
	d14.addActionListener(this);
	demosItem.add(d1);
	demosItem.add(d2);
	demosItem.add(d3);
	demosItem.add(d4);
	demosItem.add(d5);
	demosItem.add(d6);
	demosItem.add(d7);
	demosItem.add(d8);
	demosItem.add(d9);
	demosItem.add(d10);
	demosItem.add(d11);
	demosItem.add(d12);
	demosItem.addSeparator();
	demosItem.add(d13);
	demosItem.add(d14);
	final Menu demosExtItem = new Menu("AWT(X) demos");
	demosExtItem.setEnabled(false);
	MenuItem x1 = new MenuItem("ExtendedLabel demo");
	x1.setActionCommand("ExtendedLabel");
	x1.addActionListener(this);
	MenuItem x2 = new MenuItem("ExtendedPanel demo");
	x2.setActionCommand("ExtendedPanel");
	x2.addActionListener(this);
	demosExtItem.add(x1);
	demosExtItem.add(x2);
	Menu applItem = new Menu("application demos");
	MenuItem a1 = new MenuItem("MSweep");
	a1.setActionCommand("MSweep");
	a1.addActionListener(this);
	applItem.add(a1);

	MenuItem a2 = new MenuItem("MSweep/Domain");
	a2.setActionCommand("MSweep/Domain");
	a2.addActionListener(this);
	applItem.add(a2);

	if (extendedTest) {
	    MenuItem a3 = new MenuItem("MSweep/Domain/Console");
	    a3.setActionCommand("MSweep/Domain/Console");
	    a3.addActionListener(this);
	    applItem.add(a3);
	}
	if (rdpTest) {
	    MenuItem a4 = new MenuItem("RDP");
	    a4.setActionCommand("RDP");
	    a4.addActionListener(this);
	    applItem.add(a4);

	}
	
	CheckboxMenuItem enableExtItem = new CheckboxMenuItem("enable X");
	enableExtItem.setState(false);
	enableExtItem.addItemListener(new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
		    demosExtItem.setEnabled((e.getStateChange() == ItemEvent.SELECTED));
		}
	    });
	
	tb.add(infoItem);
	tb.addSeparator();
	tb.add(demosItem);
	tb.add(demosExtItem);
	tb.add(applItem);
	tb.add(enableExtItem);
	tb.addSeparator();
	tb.add(rebootItem);

	//JXToolkit tk = (JXToolkit) Toolkit.getDefaultToolkit();
	//tk.loadAlternateColors();

	tb.show();
	ld.show();
	aboutDialog();
    }

    public void actionPerformed(ActionEvent e) {
	DemoFrame f;
	String s = e.getActionCommand();
	
	//toolkit.message("got command " + s);
	if (s.equals("info")) {
	    aboutDialog();
	}
	if (s.equals("reboot")) {
	    // get CPUManager portal
	    Naming naming = InitialNaming.getInitialNaming();
	    CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	    // reboot machine!
	    cpuManager.reboot();
	}
	if (s.equals("Label")) {
	    f = newLabelFrame(ld);
	    f.pack();
	    f.show();
	}
	if (s.equals("Button")) {
	    f = newButtonFrame(ld);
	    f.pack();
	    f.show();
	}
	if (s.equals("Checkbox")) {
	    f = newCheckboxFrame(ld);
	    f.pack();
	    f.show();
	}
	if (s.equals("Scrollbar")) {
	    f = newScrollbarFrame(ld);
	    f.pack();
	    f.show();
	}
	if (s.equals("Choice")) {
	    f = newChoiceFrame(ld);
	    f.pack();
	    f.show();
	}
	if (s.equals("TextField")) {
	    f = newTextFieldFrame(ld);
	    f.pack();
	    f.show();
	}
	if (s.equals("TextArea")) {
	    f = newTextAreaFrame(ld);
	    f.pack();
	    f.show();
	}
	if (s.equals("Canvas")) {
	    f = newCanvasFrame(ld);
	    f.pack();
	    f.show();
	}
	if (s.equals("List")) {
	    f = newListFrame(ld);
	    f.pack();
	    f.show();
	}
	if (s.equals("Menu")) {
	    f = newMenuFrame(ld);
	    f.pack();
	    f.show();
	}
	if (s.equals("Popup")) {
	    f = newPopupMenuFrame(ld);
	    f.pack();
	    f.show();
	}
	if (s.equals("ScrollPane")) {
	    f = newScrollPaneFrame(ld);
	    f.pack();
	    f.show();
	}
	if (s.equals("Image")) {
	    f = newImageFrame(ld);
	    f.pack();
	    f.show();
	}
	if (s.equals("Font")) {
	    f = newFontFrame(ld);
	    f.pack();
	    f.show();
	}
	if (s.equals("ExtendedLabel")) {
	    f = newExtendedLabelFrame(ld);
	    f.pack();
	    f.show();
	}
	if (s.equals("ExtendedPanel")) {
	    f = newExtendedPanelFrame(ld);
	    f.pack();
	    f.show();
	}

	if (s.equals("MSweep")) {
	    /*Thread t = new Thread() {
	      public void run() {*/
			MSweep m = new MSweep();
			m.doInit();
			/*    }
			      };
			      t.start();*/
	}

	if (s.equals("MSweep/Domain")) {
	    DomainStarter.createDomain("MSweep", "test_jx_awt.jll", "jx/start/ApplicationStarter", 500000, 50000,
				       new String[] {"stream", "test/jx/awt/MSweep", "test_jx_awt.jll"});
	}
	if (s.equals("MSweep/Domain/Console")) {
	    DomainStarter.createDomain("MSweep", "test_jx_awt.jll", "jx/start/WindowApplicationStarter", 500000, 100000,
				       new String[] {"stream", "test/jx/awt/MSweep", "test_jx_awt.jll"});
	}
	if (s.equals("RDP")) {
	    DomainStarter.createDomain("RDP", "test_jx_awt.jll", "jx/start/ApplicationStarter",3000000, 100000,
				       new String[] {"bla", "jx/init/Main", "init2.jll",  "boot.rc.rdp.embed"});
	}
    }



    private void aboutDialog() {
	MessageDialog.infoMessageDialog("About JX AWT Demo",
					"This litte demo shows some of the implementations that\n" +
					"were made to bring an AWT compatible GUI to the JX\n" +
					"platform.",
					"Have fun!");
    }


    
    private DemoFrame newLabelFrame(ListenerDisplay ld) {
	DemoFrame f = new DemoFrame("Label demo", ld);
	Label l1 = new Label("This is a Label.");
	Label l2 = new Label("This Label is disabled.");
	l2.setEnabled(false);

	l1.addComponentListener(ld);
	l2.addComponentListener(ld);

	f.add(l1, BorderLayout.NORTH);
	f.add(l2, BorderLayout.SOUTH);

	return f;
    }

    private DemoFrame newButtonFrame(ListenerDisplay ld) {
	DemoFrame f = new DemoFrame("Button demo", ld);
	Panel p1 = new Panel();
	Panel p2 = new Panel();
	Panel p3 = new Panel();
	p1.setLayout(new BorderLayout());
	p2.setLayout(new BorderLayout());
	p3.setLayout(new BorderLayout());
	Label l1 = new Label("This is a Button -->");
	Button b1 = new Button("Button 1");
	b1.addActionListener(ld);
	Label l2 = new Label("This Button is disabled -->");
	Button b2 = new Button("Button 2");
	b2.setEnabled(false);
	Label l3 = new Label("This Button is invisible -->");
	Button b3 = new Button("Button 3");
	b3.setVisible(false);
	Label l3a = new Label("press the button to see an action event occur.");

	p1.add(l1, BorderLayout.WEST);
	p1.add(b1, BorderLayout.EAST);
	p2.add(l2, BorderLayout.WEST);
	p2.add(b2, BorderLayout.EAST);
	p3.add(l3, BorderLayout.WEST);
	p3.add(b3, BorderLayout.EAST);
	p3.add(l3a, BorderLayout.SOUTH);
	f.add(p1, BorderLayout.NORTH);
	f.add(p2, BorderLayout.CENTER);
	f.add(p3, BorderLayout.SOUTH);

	return f;
    }

    private DemoFrame newCheckboxFrame(ListenerDisplay ld) {
	DemoFrame f = new DemoFrame("Checkbox demo", ld);
	f.setLayout(new GridLayout(0, 1));
	Panel p1 = new Panel();
	Panel p2 = new Panel();
	Panel p3 = new Panel();
	p1.setLayout(new BorderLayout());
	p2.setLayout(new BorderLayout());
	p3.setLayout(new BorderLayout());
	Label l1 = new Label("These are some ungrouped Checkboxes:");
	Checkbox c1 = new Checkbox("Checkbox 1", false);
	Checkbox c2 = new Checkbox("Checkbox 2", false);
	Checkbox c3 = new Checkbox("Checkbox 3", false);
	p1.add(l1, BorderLayout.NORTH);
	p1.add(c1, BorderLayout.WEST);
	p1.add(c2, BorderLayout.CENTER);
	p1.add(c3, BorderLayout.EAST);
	Label l2 = new Label("These are some grouped Checkboxes:");
	CheckboxGroup cbg = new CheckboxGroup();
	Checkbox c4 = new Checkbox("Checkbox 4", true, cbg);
	Checkbox c5 = new Checkbox("Checkbox 5", false, cbg);
	Checkbox c6 = new Checkbox("Checkbox 6", false, cbg);
	p2.add(l2, BorderLayout.NORTH);
	p2.add(c4, BorderLayout.WEST);
	p2.add(c5, BorderLayout.CENTER);
	p2.add(c6, BorderLayout.EAST);
	Label l3 = new Label("These are disabled Checkboxes:");
	CheckboxGroup cbg2 = new CheckboxGroup();
	Checkbox c7 = new Checkbox("Checkbox 7", true);
	Checkbox c8 = new Checkbox("Checkbox 8", false);
	Checkbox c9 = new Checkbox("Checkbox 9", true, cbg2);
	Checkbox c10 = new Checkbox("Checkbox 10", false, cbg2);
	c7.setEnabled(false);
	c8.setEnabled(false);
	c9.setEnabled(false);
	c10.setEnabled(false);
	Panel p4 = new Panel();
	p4.setLayout(new GridLayout(0, 2));

	p4.add(c7);
	p4.add(c9);
	p4.add(c8);
	p4.add(c10);
	p3.add(l3, BorderLayout.NORTH);
	p3.add(p4, BorderLayout.SOUTH);

	c1.addItemListener(ld);
	c2.addItemListener(ld);
	c3.addItemListener(ld);
	c4.addItemListener(ld);
	c5.addItemListener(ld);
	c6.addItemListener(ld);

	f.add(p1);
	f.add(p2);
	f.add(p3);
	f.add(new Label("press checkboxes to let some"));
	f.add(new Label("item events occur."));

	return f;
    }

    private DemoFrame newScrollbarFrame(ListenerDisplay ld) {
	DemoFrame f = new DemoFrame("Scrollbar demo", ld);

	Panel p1 = new Panel();
	p1.setLayout(new GridLayout(0, 1));
	p1.add(new Label("Here are four scrollbars, two vertical and two"));
	p1.add(new Label("horizontal ones. The bars on the left and on top"));
	p1.add(new Label("are disabled. You can use the mouse to change"));
	p1.add(new Label("the values of the remaining bars and look for"));
	p1.add(new Label("occuring adjustment events."));
	Scrollbar s1 = new Scrollbar(Scrollbar.VERTICAL, 0, 10, 0, 80);
	s1.addAdjustmentListener(ld);
	Scrollbar s2 = new Scrollbar(Scrollbar.HORIZONTAL, 0, 10, 0, 80);
	s2.addAdjustmentListener(ld);
	Scrollbar s3 = new Scrollbar(Scrollbar.VERTICAL, 0, 10, 0, 80);
	s3.setEnabled(false);
	Scrollbar s4 = new Scrollbar(Scrollbar.HORIZONTAL, 0, 10, 0, 80);
	s4.setEnabled(false);

	f.add(p1, BorderLayout.CENTER);
	f.add(s1, BorderLayout.EAST);
	f.add(s2, BorderLayout.SOUTH);
	f.add(s3, BorderLayout.WEST);
	f.add(s4, BorderLayout.NORTH);

	return f;
    }
	
    private DemoFrame newChoiceFrame(ListenerDisplay ld) {
	DemoFrame f = new DemoFrame("Choice demo", ld);

	f.setLayout(new FlowLayout());
	Panel p1 = new Panel();
	Panel p2 = new Panel();
	p1.setLayout(new GridLayout(0, 1));
	p2.setLayout(new GridLayout(0, 1));
	
	Choice c = new Choice();
	c.add("Line 1");
	c.add("Line 2");
	c.add("Line 3");
	c.add("Line 4");
	c.add("Line 5");
	c.add("Line 6");
	c.add("Line 7");
	c.addItemListener(ld);
	Choice c2 = new Choice();
	c2.add("Line 1");
	c2.add("Line 2");
	c2.add("Line 3");
	c2.add("Line 4");
	c2.add("Line 5");
	c2.add("Line 6");
	c2.add("Line 7");
	c2.add("Line 8");
	c2.add("Line 9");
	c2.add("Line 10");
	c2.add("Line 11");
	c2.add("Line 12");
	c2.add("Line 13");
	c2.add("Line 14");
	c2.add("Line 15");
	c2.add("Line 16");
	c2.add("Line 17");
	c2.add("Line 18");
	c2.addItemListener(ld);
	Choice c3 = new Choice();
	c3.add("disabled");
	c3.setEnabled(false);

	p1.add(new Label("This is a Choice"));
	p1.add(new Label("This Choice has more items"));
	p1.add(new Label("This Choice is disabled"));
	p2.add(c);
	p2.add(c2);
	p2.add(c3);
	f.add(p1);
	f.add(p2);

	return f;
    }

    private DemoFrame newPopupMenuFrame(ListenerDisplay ld) {
	DemoFrame f = new DemoFrame("PopupMenu demo", ld);
	
	Label l = new Label("Popup Label");
	final PopupMenu m = new PopupMenu("");
	m.add(new MenuItem("Test 1"));
	m.add(new MenuItem("Test 2"));
	m.addSeparator();
	m.add(new MenuItem("Test 3"));
	m.add(new CheckboxMenuItem("Test 4", true));
	l.add(m);
	l.addMouseListener(new MouseAdapter() {
		public void mouseReleased(MouseEvent e) {
		    if (e.isPopupTrigger()) {
			m.show(e.getComponent(),
			       e.getX(), e.getY());
		    }
		}
	    });

	f.add(new Label("This Label has a PopupMenu."), BorderLayout.NORTH);
	f.add(new Label("Use the right mouse button -->"), BorderLayout.WEST);
	f.add(new Label("to activate it"), BorderLayout.SOUTH);
	f.add(l, BorderLayout.EAST);

	return f;
    }

    private DemoFrame newTextAreaFrame(ListenerDisplay ld) {
	DemoFrame f = new DemoFrame("TextArea demo", ld);
	f.setLayout(new GridLayout(0, 1));
	TextArea ta1 = new TextArea("This is a TextArea with 2 scroll bars.\n" +
				    "Use them to read the whole text. Try\n" +
				    "to use the \"bubbles\" to scroll in both\n" +
				    "directions, or just type in whatever\n" +
				    "you want.", 5, 20, TextArea.SCROLLBARS_BOTH);
	TextArea ta2 = new TextArea("This TextArea has only\n" +
				    "one bar on the right.\n" +
				    "You can only scroll\n" +
				    "up and down.", 5, 20, TextArea.SCROLLBARS_VERTICAL_ONLY);
	TextArea ta3 = new TextArea("This Area is not editable and has\n" +
				    "one bar to scroll horizontally.", 5, 20, TextArea.SCROLLBARS_HORIZONTAL_ONLY);
	TextArea ta4 = new TextArea("This Area is disabled.\n" +
				    "There are no scroll bars.", 5, 20, TextArea.SCROLLBARS_NONE);
	ta3.setEditable(false);
	ta4.setEnabled(false);
	ta1.addKeyListener(ld);
	ta2.addKeyListener(ld);
	ta3.addKeyListener(ld);
	ta4.addKeyListener(ld);
	ta1.addTextListener(ld);
	ta2.addTextListener(ld);
	ta3.addTextListener(ld);
	ta4.addTextListener(ld);

	f.add(ta1);
	f.add(ta2);
	f.add(ta3);
	f.add(ta4);

	return f;
    }

    private DemoFrame newTextFieldFrame(ListenerDisplay ld) {
	DemoFrame f = new DemoFrame("TextField demo", ld);
	f.setLayout(new GridLayout(0, 1));
	TextField tf1 = new TextField("Field 1", 20);
	TextField tf2 = new TextField("Field 2", 20);
	TextField tf3 = new TextField("Field 3", 20);
	TextField tf4 = new TextField("Field 4", 20);
	tf2.setEchoChar('*');
	tf3.setEditable(false);
	tf4.setEnabled(false);
	tf1.addTextListener(ld);
	tf2.addTextListener(ld);
	tf3.addTextListener(ld);
	tf4.addTextListener(ld);
	tf1.addKeyListener(ld);
	tf2.addKeyListener(ld);
	tf3.addKeyListener(ld);
	tf4.addKeyListener(ld);

	Panel p1 = new Panel();
	Panel p2 = new Panel();
	Panel p3 = new Panel();
	Panel p4 = new Panel();
	p1.setLayout(new BorderLayout());
	p2.setLayout(new BorderLayout());
	p3.setLayout(new BorderLayout());
	p4.setLayout(new BorderLayout());

	p1.add(new Label("This is a TextField"), BorderLayout.WEST);
	p1.add(tf1, BorderLayout.EAST);
	p2.add(new Label("This TextField is secure"), BorderLayout.WEST);
	p2.add(tf2, BorderLayout.EAST);
	p3.add(new Label("This TextField is not editable"), BorderLayout.WEST);
	p3.add(tf3, BorderLayout.EAST);
	p4.add(new Label("This TextField is not enabled"), BorderLayout.WEST);
	p4.add(tf4, BorderLayout.EAST);

	f.add(p1);
	f.add(p2);
	f.add(p3);
	f.add(p4);
	return f;
    }

    private DemoFrame newCanvasFrame(ListenerDisplay ld) {
	DemoFrame f = new DemoFrame("Canvas demo", ld);
	final MyCanvas m = new MyCanvas();
	m.setColor(Color.white);
	m.setType(MyCanvas.LINE);
	Panel p1 = new Panel();
	p1.setLayout(new GridLayout(0, 6));
	CheckboxGroup cbg1 = new CheckboxGroup();
	final Checkbox c1 = new Checkbox("red", false, cbg1);
	final Checkbox c2 = new Checkbox("blue", false, cbg1);
	final Checkbox c3 = new Checkbox("green", false, cbg1);
	final Checkbox c4 = new Checkbox("yellow", false, cbg1);
	final Checkbox c5 = new Checkbox("black", false, cbg1);
	ItemListener il1 = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
		    Component c = (Component) e.getSource();
		    if (c == c1)
			m.setColor(Color.red);
		    if (c == c2)
			m.setColor(Color.blue);
		    if (c == c3)
			m.setColor(Color.green);
		    if (c == c4)
			m.setColor(Color.yellow);
		    if (c == c5)
			m.setColor(Color.black);
		}
	    };
	c1.addItemListener(il1);
	c2.addItemListener(il1);
	c3.addItemListener(il1);
	c4.addItemListener(il1);
	c5.addItemListener(il1);

	CheckboxGroup cbg2 = new CheckboxGroup();
	final Checkbox t1 = new Checkbox("Line", false, cbg2);
	final Checkbox t2 = new Checkbox("Rectangle", false, cbg2);
	ItemListener il2 = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
		    Component c = (Component) e.getSource();
		    if (c == t1)
			m.setType(MyCanvas.LINE);
		    if (c == t2)
			m.setType(MyCanvas.RECTANGLE);
		}
	    };
	t1.addItemListener(il2);
	t2.addItemListener(il2);

	p1.add(new Label("Color:"));
	p1.add(c1);
	p1.add(c2);
	p1.add(c3);
	p1.add(c4);
	p1.add(c5);
	p1.add(new Label("Type:"));
	p1.add(t1);
	p1.add(t2);

	f.add(p1, BorderLayout.NORTH);
	f.add(m, BorderLayout.CENTER);
	f.add(new Label("This is a drawing Canvas. Choose color and type, and draw."), BorderLayout.SOUTH);

	return f;
    }

    private DemoFrame newListFrame(ListenerDisplay ld) {
	DemoFrame f = new DemoFrame("List demo", ld);

	Panel p1 = new Panel();
	Panel p2 = new Panel();
	p1.setLayout(new GridLayout(0, 1));
	p2.setLayout(new GridLayout(0, 1));

	List l = new List(5, false);
	l.add("Line 1");
	l.add("Line 2");
	l.add("Line 3");
	l.add("Line 4");
	l.add("Line 5");
	l.add("Line 6");
	l.add("Line 7");
	l.add("Line 8");
	l.add("Line 9");
	l.addActionListener(ld);
	l.addItemListener(ld);
	List l2 = new List(5, true);
	l2.add("Line 1");
	l2.add("Line 2");
	l2.add("Line 3");
	l2.add("Line 4");
	l2.add("Line 5");
	l2.add("Line 6");
	l2.add("Line 7");
	l2.add("Line 8");
	l2.add("Line 9");
	l2.addActionListener(ld);
	l2.addItemListener(ld);
	List l3 = new List(5, true);
	l3.add("Line 1");
	l3.add("Line 2");
	l3.add("Line 3");
	l3.add("Line 4");
	l3.add("Line 5");
	l3.add("Line 6");
	l3.add("Line 7");
	l3.add("Line 8");
	l3.add("Line 9");
	l3.setEnabled(false);

	p1.add(new Label("This is a List that allows only one selection:"), BorderLayout.WEST);
	p1.add(new Label("This is a List that allows multiple selections:"), BorderLayout.WEST);
	p1.add(new Label("This List is disabled:"), BorderLayout.WEST);
	p2.add(l);
	p2.add(l2);
	p2.add(l3);
	f.add(p1, BorderLayout.WEST);
	f.add(p2, BorderLayout.EAST);
	
	return f;
    }

    private DemoFrame newMenuFrame(ListenerDisplay ld) {
	DemoFrame f = new DemoFrame("Menu demo", ld);
	
	MenuBar mb = new MenuBar();
	Menu m1 = new Menu("Menu 1");
	Menu m2 = new Menu("Menu 2");
	Menu m3 = new Menu("Disabled Menu");
	m3.setEnabled(false);
	Menu m4 = new Menu("Help");

	MenuItem m11 = new MenuItem("MenuItem 1");
	m11.addActionListener(ld);
	MenuItem m12 = new MenuItem("MenuItem 2");
	m12.addActionListener(ld);
	Menu m13 = new Menu("Sub menu");
	MenuItem m14 = new MenuItem("Disabled MenuItem");
	m14.setEnabled(false);

	MenuItem m131 = new MenuItem("MenuItem 3");
	m131.addActionListener(ld);
	MenuItem m132 = new MenuItem("MenuItem 4");
	m132.addActionListener(ld);
	m13.add(m131);
	m13.add(m132);

	m1.add(m11);
	m1.add(m12);
	m1.addSeparator();
	m1.add(m13);
	m1.add(m14);

	CheckboxMenuItem m21 = new CheckboxMenuItem("CheckboxMenuItem 1", true);
	m21.addItemListener(ld);
	CheckboxMenuItem m22 = new CheckboxMenuItem("CheckboxMenuItem 2", false);
	m22.addItemListener(ld);
	CheckboxMenuItem m23 = new CheckboxMenuItem("CheckboxMenuItem 3", true);
	m23.setEnabled(false);
	CheckboxMenuItem m24 = new CheckboxMenuItem("CheckboxMenuItem 4", false);
	m24.setEnabled(false);

	m2.add(m21);
	m2.add(m22);
	m2.addSeparator();
	m2.add(m23);
	m2.add(m24);

	MenuItem m41 = new MenuItem("About...");
	m41.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    aboutDialog();
		}
	    });

	m4.add(m41);

	mb.add(m1);
	mb.add(m2);
	mb.add(m3);
	mb.setHelpMenu(m4);
	f.setMenuBar(mb);

	f.setLayout(new GridLayout(0, 1));
	f.add(new Label("This is a menu test. You should see a menu bar"));
	f.add(new Label("with some menus. Use your mouse to discover the"));
	f.add(new Label("bar."));

	return f;
    }

    private DemoFrame newScrollPaneFrame(ListenerDisplay ld) {
	DemoFrame f = new DemoFrame("ScrollPane demo", ld);
	
	Panel p1 = new Panel();
	Panel p2 = new Panel();
	p1.setLayout(new GridLayout(0, 1));
	p2.setLayout(new GridLayout(0, 1));
	
	ScrollPane s1 = new ScrollPane(ScrollPane.SCROLLBARS_ALWAYS);
	ScrollPane s2 = new ScrollPane(ScrollPane.SCROLLBARS_NEVER);
	ScrollPane s3 = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
	s1.add(new ExtendedLabel("This is a test for a ScrollPane\n" +
				 "class. This text is embedded in-\n" +
				 "side a ScrollPane and , if the\n" +
				 "ScrollPane is enabled to do, can\n" +
				 "be scrolled. Just try it by using\n" +
				 "the scroll bars."));
	s2.add(new ExtendedLabel("This is a test for a ScrollPane\n" +
				 "class. This text is embedded in-\n" +
				 "side a ScrollPane and , if the\n" +
				 "ScrollPane is enabled to do, can\n" +
				 "be scrolled. Just try it by using\n" +
				 "the scroll bars."));
	s3.add(new ExtendedLabel("This is a test for a ScrollPane\n" +
				 "class. This text is embedded in-\n" +
				 "side a ScrollPane and , if the\n" +
				 "ScrollPane is enabled to do, can\n" +
				 "be scrolled. Just try it by using\n" +
				 "the scroll bars."));

	p1.add(new Label("This is a ScrollPane with always-shown scroll bars:"), BorderLayout.WEST);
	p1.add(new Label("This is a ScrollPane without scroll bars:"), BorderLayout.WEST);
	p1.add(new Label("This is a ScrollPane with scroll bar shown when needed:"), BorderLayout.WEST);
	p2.add(s1);
	p2.add(s2);
	p2.add(s3);
	f.add(p1, BorderLayout.WEST);
	f.add(p2);
	
	return f;
    }

    private DemoFrame newImageFrame(ListenerDisplay ld) {
	DemoFrame f = new DemoFrame("Image demo", ld);

	Toolkit tk = Toolkit.getDefaultToolkit();
	Image img1 = tk.getImage("vision01.ppm");

	int w = 255;
	int h = 255;
	int[] pix = new int[w * h];
	int index = 0;
	for (int y = 0; y < h; y++) {
	    int red = (y * 255) / (h - 1);
	    for (int x = 0; x < w; x++) {
		int blue = (x * 255) / (w - 1);
		pix[index++] = (255 << 24) | (red << 16) | blue;
	    }
	}
	Image img2 = tk.createImage(new MemoryImageSource(w, h, null /* ColorModel*/, pix, 0, w));

	ImageCanvas ic1 = new ImageCanvas(img1);
	ImageCanvas ic2 = new ImageCanvas(img2);
	ImageCanvas ic3 = new ImageCanvas(img2, 200, 100);
	Panel p1 = new Panel();
	Panel p2 = new Panel();
	Panel p3 = new Panel();
	p1.setLayout(new BorderLayout());
	p2.setLayout(new BorderLayout());
	p3.setLayout(new BorderLayout());
	p1.add(ic1);
	p1.add(new Label("This is an Image loaded from a file:"), BorderLayout.NORTH);
	p2.add(ic2);
	p2.add(new Label("This is an Image created from memory:"), BorderLayout.NORTH);
	p3.add(ic3);
	p3.add(new Label("This is a resized Image:"), BorderLayout.NORTH);
	f.setLayout(new FlowLayout());
	f.add(p1, BorderLayout.NORTH);
	f.add(p2, BorderLayout.WEST);
	f.add(p3, BorderLayout.SOUTH);

	return f;
    }

    private DemoFrame newFontFrame(ListenerDisplay ld) {
	DemoFrame f = new DemoFrame("Font demo", ld);

	f.add(new Label("Below you see all characters of the current default font:"), BorderLayout.NORTH);
	f.add(new FontCanvas(), BorderLayout.SOUTH);

	return f;
    }

    private DemoFrame newExtendedLabelFrame(ListenerDisplay ld) {
	DemoFrame f = new DemoFrame("ExtendedLabel demo", ld);

	f.add(new ExtendedLabel("This is an ExtendedLabel. Unlike a normal AWT Label,\n" +
				"it is able to show more than one line of text. This\n" +
				"is especially useful to provide some kind of info\n" +
				"boxes."));

	return f;
    }
    
    private DemoFrame newExtendedPanelFrame(ListenerDisplay ld) {
	DemoFrame f = new DemoFrame("ExtendedPanel demo", ld);

	Label epl1 = new Label("ExtendedPanel normal");
	Label epl2 = new Label("ExtendedPanel lined");
	Label epl3 = new Label("ExtendedPanel raised");
	Label epl4 = new Label("ExtendedPanel lowered");
	Label epl5 = new Label("ExtendedPanel etched");
	Label epl6 = new Label("ExtendedPanel normal");
	Label epl7 = new Label("ExtendedPanel lined");
	Label epl8 = new Label("ExtendedPanel raised");
	Label epl9 = new Label("ExtendedPanel lowered");
	Label epl10 = new Label("ExtendedPanel etched");
	ExtendedPanel ep1 = new ExtendedPanel(ExtendedPanel.BORDER_NONE, null);
	ExtendedPanel ep2 = new ExtendedPanel(ExtendedPanel.BORDER_LINE, null);
	ExtendedPanel ep3 = new ExtendedPanel(ExtendedPanel.BORDER_RAISED, null);
	ExtendedPanel ep4 = new ExtendedPanel(ExtendedPanel.BORDER_LOWERED, null);
	ExtendedPanel ep5 = new ExtendedPanel(ExtendedPanel.BORDER_ETCHED, null);
	ExtendedPanel ep6 = new ExtendedPanel(ExtendedPanel.BORDER_NONE, "title");
	ExtendedPanel ep7 = new ExtendedPanel(ExtendedPanel.BORDER_LINE, "title");
	ExtendedPanel ep8 = new ExtendedPanel(ExtendedPanel.BORDER_RAISED, "title");
	ExtendedPanel ep9 = new ExtendedPanel(ExtendedPanel.BORDER_LOWERED, "title");
	ExtendedPanel ep10 = new ExtendedPanel(ExtendedPanel.BORDER_ETCHED, "title");
	ep1.add(epl1);
	ep2.add(epl2);
	ep3.add(epl3);
	ep4.add(epl4);
	ep5.add(epl5);
	ep6.add(epl6);
	ep7.add(epl7);
	ep8.add(epl8);
	ep9.add(epl9);
	ep10.add(epl10);
	Panel p1 = new Panel();
	Panel p2 = new Panel();
	p1.setLayout(new GridLayout(0, 2));
	p2.setLayout(new GridLayout(0, 2));
	p1.add(ep1);
	p1.add(ep2);
	p1.add(ep3);
	p1.add(ep4);
	p1.add(ep5);
	p2.add(ep6);
	p2.add(ep7);
	p2.add(ep8);
	p2.add(ep9);
	p2.add(ep10);
	f.add(p1, BorderLayout.NORTH);
	f.add(p2, BorderLayout.SOUTH);

	return f;
    }
    
}
