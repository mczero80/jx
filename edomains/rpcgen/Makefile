CLASSPATH=${JDKZIP}:${JXLIBS}/zero:${JXLIBS}/zero_misc:${JXLIBS}/classfile:${JXLIBS}/collections:${JXLIBS}/classstore:${JXLIBS}/zip:${JXLIBS}/rpcgen:${JXEDOMAINS}/rpcgen:${JXEMULATION}

all:
	cd jx/rpcgen; $(MAKE) CLASSPATH=$(CLASSPATH) compile

clean:
	cd jx/rpcgen; $(MAKE) clean