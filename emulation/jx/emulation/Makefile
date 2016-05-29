SUBDIRS = devices/ide

default:
	cd ../..; $(MAKE)

compile:
	@echo CLASSPATH=$(CLASSPATH)
	@echo "Files to compile: "
	@if $(PERL) $(LISTNEW) *.java; then \
	$(JAVAC) $(JAVAC_FLAGS) `$(PERL) $(LISTNEW) *.java`; fi;  
	@for i in $(SUBDIRS) ; do \
	    cd $$i; $(MAKE) $@ || exit 1; cd ..;  \
	done

clean:
	rm *.class
