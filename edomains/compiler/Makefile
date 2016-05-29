#CLASSPATH=${JDKZIP}:${JXLIBS}/zero:${JXLIBS}/zero_misc:${JXLIBS}/classfile:${JXLIBS}/collections:${JXLIBS}/compiler:${JXLIBS}/zip:${JXDOMAINS}/compiler:${JXEDOMAINS}/compiler:${JXEMULATION}
#CLASSPATH =  ${JDKZIP}:${JXLIBS}/zero:${JXLIBS}/zero_misc:${JXLIBS}/zip:${JXLIBS}/classfile:${JXLIBS}/collections:${JXLIBS}/compiler:${JXLIBS}/compiler_x86:${JXLIBS}/compiler_env:${JXEDOMAINS}/compiler:${JXEMULATION}
CLASSPATH =  ${JDKZIP}:${JXLIBS}/zero:${JXLIBS}/zero_misc:${JXLIBS}/zip:${JXLIBS}/classfile:${JXLIBS}/collections:${JXLIBS}/compspec:${JXLIBS}/compiler:${JXLIBS}/compiler_env:${JXEDOMAINS}/compiler:${JXEMULATION}

all:
	cd jx/compiler; $(MAKE) CLASSPATH=$(CLASSPATH) compile

clean:
	cd jx/compiler; $(MAKE) clean
