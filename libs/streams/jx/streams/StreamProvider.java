package jx.streams;

import jx.zero.Portal;

public interface StreamProvider extends Portal {
    InputStreamPortal getInputStream();
    OutputStreamPortal getOutputStream();
    OutputStreamPortal getErrorStream();
    void close();
}
