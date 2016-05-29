package jx.bootrc;

import jx.zero.*;
import java.util.*;

public class DomainSpec extends Spec {
    ComponentSpec[] comp;
    void setComponents(ComponentSpec[] c) {
	comp = c;
    }
    public ComponentSpec[] getComponents() {
	return comp;
    }
}
