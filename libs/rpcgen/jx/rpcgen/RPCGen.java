package jx.rpcgen;

import java.io.PrintStream;
import java.util.*;

import jx.rpc.*;
import jx.classstore.*;
import jx.classfile.*;
import jx.classfile.datatypes.*;

import jx.zero.Debug;

/**
 * RPC stub and skeleton generator.
 * @author Michael Golm
 */
public class RPCGen {
    static final boolean dumpAll = false;
    public static final boolean debug = false; // create debug output statements
    final static boolean debugInterface = false;

    Formats formats;
    ClassFinder classFinder;
    FileSys fileSys;
    ClassInformation classInfo;
    ClassData rpcProcClass, rpcDataClass;

    static final boolean createDebugCode = true;

    public RPCGen(ClassFinder classFinder, FileSys fileSys) {
	this.classFinder = classFinder;
	this.fileSys = fileSys;
	this.classInfo = new ClassInformation(classFinder);
	rpcProcClass = classFinder.findClass("jx.rpc.RPCProc");
	rpcDataClass = classFinder.findClass("jx.rpc.RPCData");
	formats = new Formats(this, classFinder, classInfo);
    }

    public void generate(String classname) throws Exception {
	ClassData cl = classFinder.findClass(classname);
	if (cl == null) throw new Error("Class "+classname+" not found.");
	String packageName = Helper.classnamePackage(cl.getJavaLanguageName());
	String implClassName = Helper.classnameName(cl.getJavaLanguageName());
	ClassData[] interfaces = classInfo.getInterfaces(cl);
	MethodData[] methods = (MethodData[]) cl.getMethods();
	FieldData[] fields =  cl.getFields();
    
	if (! classInfo.doesImplement(cl, rpcProcClass)) {
	    if (! classInfo.doesImplement(cl, rpcDataClass)) {
		throw new Error(implClassName + " must implement the RPCProc or RPCData interface");
	    } else {
		/*
		 * write data parser
		 */
		formats.addRPCFormatType(cl,"public");

		Hashtable f = formats.getFormats();
		for(Enumeration e=f.keys();e.hasMoreElements();) {
		    ClassData c = (ClassData)e.nextElement();
		    if (dumpAll) Debug.out.println("Generating RPCFormat class for "+c.getName());
		    PrintStream stub = new PrintStream(fileSys.openFile("RPCFormatter"+Helper.classnameName(c.getName())+".java"));
		    stub.println("package " + Helper.classnamePackage(c.getName()) + ";");
		    stub.println("import jx.rpc.*;");
		    stub.println("import jx.zero.debug.Dump;");
		    stub.println("import jx.xdr.Format;");
		    stub.println((String)f.get(c));
		    stub.close();
		}
		System.exit(0);
	    }
	}
	if (dumpAll) Debug.out.println("Generating stub class for "+cl.getName());
	String classModifier = "static";

	PrintStream stub = new PrintStream(fileSys.openFile(implClassName+"_Stub.java"));
	PrintStream skel = new PrintStream(fileSys.openFile(implClassName+"_Skel.java"));
	String hdr = "package " + packageName + ";\n";
	hdr += "import jx.rpc.*;\n";
	hdr += "import jx.zero.debug.Dump;\n";
	hdr += "import jx.xdr.Format;\n";
	stub.print(hdr);
	skel.print(hdr);

	stub.print("public class " + implClassName + "_Stub implements ");
	skel.print("public class " + implClassName + "_Skel  ");

	for (int i=0; i<interfaces.length; i++) {
	}
	stub.print(implClassName);
	stub.println(" {");
	skel.println(" {");

	skel.println("  public "+implClassName+"_Skel(jx.rpc.RPC rpc, int port, "+classname+" obj)  { ");
	skel.println("    jx.net.UDPReceiver receiveSocket = rpc.createReceiver(port);");
	skel.println("    RPCBuffer storedReply = rpc.getNewBuffer();");
	skel.println("    RPCBuffer request = rpc.getNewBuffer();");
	skel.println("    RPCBuffer reply = rpc.getNewBuffer();");
	skel.println("    int prevXID = -1;");
	skel.println("   for(;;) {");
	skel.println("    //RPCBuffer reply = rpc.getNewBuffer();");
	skel.println("    reply.init();");
	skel.println("    request = rpc.receive(receiveSocket, request);");
	skel.println("    RPCMessage m = RPCFormatterRPCMessage.read(request);");
	skel.println("    /*if (m.xid == prevXID) { ");
	skel.println("       jx.zero.Debug.out.println(\"Repeated XID \"+m.xid); ");
	skel.println("       storedReply = rpc.replyOnly(port, request.getSourceAddress(), request.getSourcePort(), storedReply);");
	skel.println("       continue;");
	skel.println("    }*/");
	skel.println("    if (! (m instanceof RPCCall)) { throw new Error(); }");
	skel.println("    RPCCall c = (RPCCall)m; ");
	if (debug) {
	    skel.println("    jx.zero.Debug.out.println(\"Unprocessed:\");");
	    skel.println("    request.dumpUnprocessed();");
	}
	skel.println("    if (c.prog != "+ implClassName+".PROGRAM) {");
	skel.println("       jx.zero.Debug.out.println(\"PROG NOT FOUND:\"+c.prog); ");
	skel.println("       RPCFormatterRPCMsgProgUnavailable.write(reply, new RPCMsgProgUnavailable(m.xid, new AuthNone()));");
	skel.println("       reply = rpc.replyOnly(port, request.getSourceAddress(), request.getSourcePort(), reply);");
	skel.println("       continue;");
	skel.println("    }");
	skel.println("    if (c.progVersion != "+ implClassName+".VERSION) throw new Error();");
	skel.println("    RPCFormatterRPCMsgSuccess.write(reply, new RPCMsgSuccess(m.xid, new AuthNone()));");
	skel.println("    prevXID = m.xid;");
	skel.println("    switch(c.proc) {");

	stub.println("  int port;");
	stub.println("  RPC rpc;");
	stub.println("  jx.net.IPAddress rpcHost;");
	stub.println("  Auth a = new AuthNone();");
	stub.println("  Auth c = new AuthNone();");
    
	stub.println("  public " + implClassName + "_Stub(jx.rpc.RPC r, jx.net.IPAddress rpcHost, int remotePort)  { ");
	stub.println("    this.rpcHost = rpcHost;");
	//stub.println("    this.rpc = rpc==null?new RPC(0):rpc;");
	stub.println("    if(r==null) throw new Error(\"RPC NOT INITIALIZED\");");
	stub.println("    this.rpc = r;");
	stub.println("    this.port = remotePort;");
	stub.println("  }");

	stub.println("  public " + implClassName + "_Stub(RPC rpc, jx.net.IPAddress rpcHost)  {");
	stub.println("    this.rpcHost = rpcHost;");
	//stub.println("    this.rpc = rpc==null?new RPC(0):rpc;");
	stub.println("    if(rpc==null) throw new Error(\"RPC NOT INITIALIZED\");");
	stub.println("    this.rpc = rpc;");
	stub.println("    this.port = new jx.rpcsvc.bind2.RPCBind_Stub(rpc,rpcHost,111).getaddr(new jx.rpcsvc.bind2.RPCB(" + implClassName + ".PROGRAM, "  + implClassName + ".VERSION));");
	stub.println("  }");

	stub.println("  public void setAuth(Auth a, Auth c) {");
	stub.println("    this.a = a; this.c = c;;");
	stub.println("  }");

	int mid=-1;
	for(int i=0; i<methods.length; i++) {
	    MethodData method = methods[i];
	    BasicTypeDescriptor[] params = method.getParameterTypes();
	    Hashtable fixlens = new Hashtable(); //dummy
	    mid++;

	    mid = -1;
	    for(int j=0; j<fields.length; j++) {
		String fname = fields[j].getName();
		String fprefix = "MID_"+method.getName()+"_";
		if (fname.startsWith(fprefix)) {
		    String numStr = fname.substring(fprefix.length());
		    mid = Integer.parseInt(numStr);
		}
	    }
		if (mid == -1) throw new Error("No MID_"+method.getName()+"_[number] variable found in interface "+classname+".");

	    if (debugInterface) Debug.out.println("MID="+mid+", method="+ method.getName());

	    // skel
	    skel.println("       case "+mid+": { /* "+ method.getName() +"*/");
	    if (debug) skel.println("          jx.zero.Debug.out.println(\"Method: "+ method.getName() +"\");");
	    String skeltxt = "obj."+method.getName()+"(";
	    for(int j=0; j<params.length; j++) {
		if (params[j].isPrimitive()) {
		    if (params[j].isBoolean()) {
			skeltxt += "jx.xdr.Format.readBoolean(request)";
		    } else if (params[j].isInteger()) {
			skeltxt += "jx.xdr.Format.readInt(request)";
		    } else {
			throw new Error("only bool/int supported:"+params[j]);
		    }
		} else {
		    skeltxt += "("+params[j].getJavaLanguageType()+")"+implClassName + "_Stub.RPCFormatter"+Helper.classnameName(params[j].getJavaLanguageType())+".read(request)";
		}
		if (j<params.length-1) skeltxt += ", ";
		
	    }
	    skeltxt += ");";

	    BasicTypeDescriptor returns = method.getReturnType();
	    if (! returns.isVoid()) {
		if (returns.isPrimitive()) {
		    if (returns.isBoolean()) {
			skel.println("            boolean ret = "+skeltxt);
		    } else if (returns.isInteger()) {
			skel.println("            int ret = "+skeltxt);
			//skel.println("            if (ret == -1) {");
			//skel.println("             rpc.replyError(port, request.getSourceAddress(), request.getSourcePort(), reply);");
			//skel.println("             break; }");
		    } else {
			throw new Error("only bool/int supported:"+returns);
		    }
		} else if (returns.getJavaLanguageType().equals("java.lang.String")) {
		    skel.println("      String ret = "+skeltxt);
		} else {
		    skel.println("            "+returns.getJavaLanguageType()+" ret = "+skeltxt);
		}
		genDataWriter(skel, null, returns,  "ret", fixlens, null,  classModifier,"reply", "     "+implClassName + "_Stub.");
	    } else {
		skel.println("             "+skeltxt);
	    }
	    skel.println("             rpc.replyOnly(port, request.getSourceAddress(), request.getSourcePort(), reply);");
	    skel.println("             break; }");

	    // stub
	    stub.print("  public " + method.getReturnType().getJavaLanguageType() + " " +  method.getName() + "("); 
	    for(int j=0; j<params.length; j++) {
		stub.print(params[j].getJavaLanguageType());
		stub.print(" param"+j);
		if (j<params.length-1) stub.print(",");
	    }
	    stub.print(")");
	    stub.println(" {");

	    if (createDebugCode) {
		stub.println("    jx.zero.Debug.out.println(\"RPC Stub-Method called:  "+method.getName() +"\");");
		stub.println("    if(rpc==null) jx.zero.Debug.out.println(\"RPC NOT INITIALIZED\");");
	    }

	    stub.println("    RPCBuffer outbuf = rpc.getNewBuffer();");
	    for(int j=0; j<params.length; j++) {	
		genDataWriter(stub, null, params[j],  "param"+j, fixlens, null,  classModifier,"outbuf","");
		/*
		  formats.addRPCFormatType(params[j],classModifier);
		  String fullname = Helper.classnameName(params[j].getName());
		  stub.println("    RPCFormatter"+fullname+".write(outbuf, param"+j+");");
		*/
	    }
	    stub.println("    RPCContinuation co = rpc.call(this.rpcHost, this.port, "+
			 implClassName + ".PROGRAM,"+
			 implClassName + ".VERSION,"+
			 mid+","+
			 " outbuf, a, c);");
	    //stub.println("    RPCBINDReplyFormat bindReply = new RPCBINDReplyFormat(rbuf, 0);");
	    //stub.println("    return bindReply.getPortNumber();");

	    if (! returns.isVoid()) {
		stub.println("    RPCBuffer rpcbuf = rpc.initFrom(co.buf,co);");
		//stub.println("    Dump.xdump(co.buf,128);");
		if (returns.isPrimitive()) {
		    if (returns.isBoolean()) {
			stub.println("    return Format.readBoolean(rpcbuf);");
		    } else if (returns.isInteger()) {
			stub.println("    return Format.readInt(rpcbuf);");
		    } else {
			throw new Error("only bool/int supported:"+returns);
		    }
		} else if (returns.getJavaLanguageType().equals("java.lang.String")) {
		    stub.println("    return Format.readString(rpcbuf);");
		} else {
		    formats.addRPCFormatType(returns,classModifier);
		    stub.println("    return ("+returns.getJavaLanguageType()+")RPCFormatter"+Helper.classnameName(returns.getJavaLanguageType())+".read(rpcbuf);");
		}
	    }
	    stub.println("  }");
	}
	generate(stub);
	stub.println("}");
	stub.close();
	skel.println("    }");
	skel.println("    RPCBuffer tmp = storedReply;");
	skel.println("    storedReply = reply;");
	skel.println("    reply = tmp;");
	skel.println("  }");
	skel.println("  }");
	skel.println("}");
	skel.close();
    }
    public void generate(PrintStream out) {
	Hashtable f = formats.getFormats();
	for(Enumeration e=f.elements();e.hasMoreElements();) {
	    out.println((String)e.nextElement());
	}
    }



    /**
     * @param out Writer for serialisation code
     * @param out Writer for length code
     * @param fixlens list of fixed len arrays
     * @param refname fieldname needed for fixlens
     */
    public void genDataWriter(PrintStream out, PrintStream lou, BasicTypeDescriptor cl, String varName, Hashtable fixlens, String refName, String classModifier, String bufname, String rpcFormatterPrefix) {
	if (cl.isPrimitive()) {
	    if (cl.isInteger()) {
		out.println("        Format.writeInt("+bufname+","+varName+");");
		if(lou!=null) lou.println("        _l += Format.lengthInt("+varName+");");
	    } else if (cl.isShort()) {
		out.println("        Format.writeShort("+bufname+","+varName+");");
		if(lou!=null) lou.println("        _l += Format.lengthShort("+varName+");");
	    } else {
		throw new Error("only int supported:"+cl);
	    }
	} else if (cl.getJavaLanguageType().equals("java.lang.String")) {
	    out.println("        Format.writeString("+bufname+","+varName+");");
	    if(lou!=null) lou.println("        _l += Format.lengthString("+varName+");");
	} else if (cl.getComponentType() != null) {
	    BasicTypeDescriptor arrayType = cl.getComponentType();
	    if (arrayType.isByte()) {
		FieldData flen = (FieldData) fixlens.get(refName.intern());
		if (flen != null) {
		    out.println("        Format.writeFixByteArray("+bufname+","+varName+","+flen.getDeclaringClass().getJavaLanguageName()+"."+flen.getName()+");");
		    if(lou!=null) lou.println("        _l += Format.lengthFixByteArray("+varName+","+flen.getDeclaringClass().getJavaLanguageName()+"."+flen.getName()+");");
		} else {
		    out.println("        Format.writeByteArray("+bufname+","+varName+");");
		    if(lou!=null) lou.println("        _l += Format.lengthByteArray("+varName+");");
		}
	    } else if (arrayType.isInteger()) {
		out.println("        Format.writeIntArray("+bufname+","+varName+");");
		if(lou!=null) lou.println("        _l += Format.lengthIntArray("+varName+");");
	    } else if (arrayType.isShort()) {
		out.println("        Format.writeShortArray("+bufname+","+varName+");");
		if(lou!=null) lou.println("        _l += Format.lengthShortArray("+varName+");");
	    } else {
		System.out.println("only int/byte/short arrays supported:"+arrayType);
		System.exit(1);
	    }
	} else {
	    formats.addRPCFormatType(cl,classModifier);
	    out.println("        "+rpcFormatterPrefix+"RPCFormatter"+Helper.classnameName(cl.getJavaLanguageType())+".write("+bufname+", "+varName+");");
	    if(lou!=null) lou.println("        _l += RPCFormatter"+Helper.classnameName(cl.getJavaLanguageType())+".length("+varName+");");
	}
    }


    void writeSkel(PrintStream out, String classname) {
	out.println("public "+classname+"_Skel(jx.rpc.RPC rpc, int port, "+classname+" obj)  { ");
	out.println("  jx.zero.Memory buf = rpc.receive(port);");
	out.println("}");
    }

}

