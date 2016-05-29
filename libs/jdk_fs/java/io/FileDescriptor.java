package java.io;

public final class FileDescriptor
{ 
  public final static FileDescriptor in  = new FileDescriptor(0);
  public final static FileDescriptor out = new FileDescriptor(1);
  public final static FileDescriptor err = new FileDescriptor(2);
  
  int fd = -1; // keep this to be compatible with native io (e.g., sockets, console)

  public FileDescriptor() {}
  private FileDescriptor(int fd) {this.fd = fd;}
  public boolean valid() {return (fd >= 0);}
}

