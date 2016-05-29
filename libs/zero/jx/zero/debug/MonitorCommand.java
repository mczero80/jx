package jx.zero.debug;

import jx.zero.Portal;

public interface MonitorCommand extends Portal {
    void execCommand(String[] args);
    String getHelp();
}
