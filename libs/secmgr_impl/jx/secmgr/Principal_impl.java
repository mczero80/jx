package jx.secmgr;

import jx.zero.Principal;

public class Principal_impl implements Principal {
    String name;
    int uid;
    public Principal_impl(String name, int uid) {
	this.name = name;
	this.uid = uid;
    }
    public String toString() { return name+"("+Integer.toString(uid)+")"; }

    public boolean equals( Principal p) { return ( name.equals(((Principal_impl)p).name) && uid==((Principal_impl)p).uid); }

}

