package jx.emulation;

import jx.zero.*;
import jx.zero.scheduler.HighLevelScheduler;
import java.util.*;

public class ComponentManagerImpl implements ComponentManager {
    public void registerLib(String name,Memory libcode) {
	throw new Error("registerLib not emulated");
    }
    public int load(String name){throw new Error();}
}
