package jx.shell;

import java.io.PrintStream;

public interface Command {
    void command(PrintStream out, String[] args) throws Exception;
    String getInfo();
}
