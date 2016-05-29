#CLASSPATH=${JDKZIP}:${JXLIBS}/zero:${JXLIBS}/zero_misc:${JXLIBS}/bio:${JXEMULATION}
CLASSPATH=${JDKZIP}:${EMULATIONCLASSPATH}

all:
	@echo EEECLASSPATH=$(EMULATIONCLASSPATH)
	@echo CLASSPATH=$(CLASSPATH)
	cd jx/emulation; $(MAKE) CLASSPATH=$(CLASSPATH) compile
	cd jx/zero; $(MAKE) CLASSPATH=$(CLASSPATH) compile


clean:
	rm -f *.zip
	find . -name "*.class" -exec rm -f {} \;
