default:
	cd ../.. ; $(MAKE)

compile:
	@echo "Files to compile: "
	@if $(PERL) $(LISTNEW) *.java; then \
	$(JAVAC) `$(PERL) $(LISTNEW) *.java`; fi;  
#	$(JAVAC) $(JAVAC_FLAGS) `$(PERL) $(LISTNEW) *.java`; fi;  

clean:
	@for i in $(SUBDIRS) ; do \
	    cd $$i; $(MAKE) $@ || exit 1; cd ..;  \
	done
	rm -f *.class *~

