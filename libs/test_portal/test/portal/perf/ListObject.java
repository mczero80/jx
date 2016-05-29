package test.portal.perf;

public class ListObject {
    ListObject next;
    ListObject() {}
    ListObject(int n) {
	ListObject o = this;
	for(int i=0; i<n; i++) {
	    o.next = new ListObject();
	    o = o.next;
	}
    }
}
