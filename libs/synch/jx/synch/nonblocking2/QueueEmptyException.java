package jx.synch.nonblocking2;

class QueueEmptyException extends Exception {
    public QueueEmptyException() {}
    public QueueEmptyException(String msg) { super(msg); }
}
