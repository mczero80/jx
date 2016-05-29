package jx.net;


import jx.zero.Memory;

public interface UDPConsumer {
  Memory processUDP(UDPData data);
}
