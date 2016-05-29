This repo exists to preserve the JX project.

Here is what its all about:

JX is a Java operating system that focuses on a flexible and robust operating system architecture.

The JX system architecture consists of a set of Java components executing on the JX core that is responsible for system initialization, CPU context switching and low-level domain management. The Java code is organized in components which are loaded into domains, verified, and translated to native code.

Main-stream operating systems and traditional microkernels base their protection mechanisms on MMU-provided address space separation. Protection in JX is solely based on the type safety of the intermediate code, the Java bytecode.

The system runs either on off-the-shelf PC hardware (i486, Pentium, and embedded PCs, such as the DIMM-PC) or as a guest system on Linux. Many of the JX Java components, for example the file system, also run on an unmodified JVM. When running on the bare hardware, the system can access IDE disks , 3COM 3C905 NICs, and Matrox G200 video cards. The network code contains IP, TCP, UDP, NFS2 client and server, SUN RPC. Applications that run on top of JX include an Ext2 file system, a window manager, and a database system.

JX is unique in several aspects:

It is very secure. Compared to mainstream operating systems JX has a very small trusted computing base (TCB). This is discussed in more detail in the paper "A Java Operating System as the Foundation of a Secure Network Operating System". The unique protection domain model allows to run code with different trustworthiness at the same time in the same system.
It is free and open source. You can use JX as platform for your own open source or propriatary projects. We do not charge royalties or require you to disclose your source code. For details please see our license.
It conforms to standards. The core of the JX system is a cleanroom implementation of a Java Virtual Machine. This implementation conforms to the "Java Virtual Machine Specification", which allows you to use all tools and IDEs that produce bytecode compliant to this specification. There are several JX components that implement industry standards, such as the TCP communication protocol and the Ext2 file system.
It is flexible. You can configure the system according to your needs either as a very small embedded system with limited functionality or as a feature rich server system.
It is fast. Our benchmark results show, that a Java-based operating system does not need to be much slower than a traditional UNIK-like operating system, such as Linux.
The JX system is developed as an open source system by the University of Erlangen.

If you consider using JX in a commercial product you can contact info@jxos.com for consulting and development services.
