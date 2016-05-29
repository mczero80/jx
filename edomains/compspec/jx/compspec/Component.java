package jx.compspec;

public class Component {
    String name;
    Component[] dependsOn;
    Component extendsOn;
    String [] subdirs;

    String getComponentName() { return name; }
    String[] getSubDirs() { return subdirs; }

    public String toString() {
	String s = name;
	for(Component c = this; c.extendsOn != null; c = c.extendsOn) s += "::"+c.extendsOn.name;
	if (dependsOn != null) {
	    s += " <- ";
	    for(int i=0; i<dependsOn.length; i++) {
		s += dependsOn[i].name;
		if (i<dependsOn.length-1) s += ", ";
	    }
	}
	return s;
    }
    // define lexical order on components 
    public boolean less(Component c) {
	return name.compareToIgnoreCase(c.name)>0;
    }
}
