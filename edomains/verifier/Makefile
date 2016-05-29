CLASSPATH=${JDKZIP}:${JXLIBS}/zero:${JXLIBS}/zero_misc:${JXLIBS}/classfile:${JXLIBS}/collections:${JXLIBS}/classstore:${JXLIBS}/zip:${JXLIBS}/verifier:${JXEDOMAINS}/verifier:${JXEMULATION}

all:
	cd jx/verifier; $(MAKE) CLASSPATH=$(CLASSPATH) compile

clean:
	cd jx/verifier; $(MAKE) clean