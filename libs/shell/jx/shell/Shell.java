package jx.shell;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import jx.zero.*;
import jx.zero.debug.*;

class Parser {
    private String kommando;
    private String[] argv = new String[10];
    private int argc;

    public Parser(String zeile) {
	String tmp = zeile.trim();
	int cut = tmp.indexOf(" ", 0);
	argc = 0;

	if (cut == -1) {
	    kommando = tmp;
	    return;
	}
	kommando = tmp.substring(0, cut);
	tmp = tmp.substring(cut).trim();
	while ((tmp.length() > 0) && (argc < 10)) {
	    cut = tmp.indexOf(" ", 0);
	    if (cut == -1) {
		argv[argc++] = tmp;
		break;
	    } else {
		argv[argc++] = tmp.substring(0, cut);
		tmp = tmp.substring(cut).trim();
	    }
	}
    }

    public boolean isValid() {
	return (kommando.length() > 0);
    }

    public String getKommando() {
	return kommando;
    }

    public String[] getArgumente() {
	String[] retval = new String[argc];
	for (int i = 0; i < argc; i++)
	    retval[i] = argv[i];
	return retval;
    }
}

class HistoryElement {
    Command action;
    String[] param;
    String line;
    HistoryElement(Command action, String[] param, String line) {
	this.action=action; this.param = param;this.line=line;
    }
}

public class Shell {
    private DataInputStream in;
    private PrintStream uout; // user output
    private String prompt = "> ";
    private Hashtable commands = new Hashtable();
    private Vector history = new Vector();

    public Shell(Naming naming) {
    }
    public Shell(OutputStream out, InputStream in) {
	this.in = new DataInputStream(in);
	this.uout = new PrintStream(out);
    }
    public void mainloop()  throws IOException {
	String line;
	uout.println("Starting shell.");
	uout.println("'help' lists all commands.");
	uout.flush();
	
	while (true) {
	    uout.print(prompt);
	    uout.flush();
	    line = in.readLine();
	    Parser parser = new Parser(line);
	    if (parser.getKommando().equals("exit")) return;
	    
	    if (! parser.isValid()) continue;
	    
	    String command = parser.getKommando();
	    String [] param = parser.getArgumente();
	    
	    try {
		if (command.equals("!!")) {
		    HistoryElement h = (HistoryElement)history.lastElement();
		    uout.println(h.line);
		    h.action.command(uout, h.param);
		} else if (command.equals("history")) {
		    Enumeration e = history.elements();
		    while(e.hasMoreElements()) {
			HistoryElement h = (HistoryElement) e.nextElement();
			uout.println(h.line);
		    }
		    continue;
		} else if (command.equals("help")) {
		    Enumeration e = commands.keys();
		    uout.println("exit:  Exit shell");
		    uout.println("!!:    Execute previous command again");
		    while(e.hasMoreElements()) {
			String cmd = (String) e.nextElement();
			Command c = (Command)commands.get(cmd);
			uout.println(cmd+":  "+c.getInfo());
		    }
		    continue;
		} else {
		    Debug.out.println("EXEC COMMAND "+command);
		    Command action = (Command)commands.get(command);
		    if (action == null) {
			Debug.out.println("XX ");
			uout.println("unknown command '" + command + "'");
			continue;
		    }
		    Debug.out.println("YY ");
		    action.command(uout, param);
		    history.addElement(new HistoryElement(action,param,line));
		}
	    } catch(Throwable ex) {
		uout.println("Command has thrown exception "+ex);
	    }
	}
    }

    public void register(String cmdStr, Command cmd) {
	commands.put(cmdStr, cmd);
    }
}
