default:
	cd ../..; $(MAKE)

compile:
	@echo CLASSPATH=$(CLASSPATH)
	@echo "Files to compile: "
	@if $(PERL) $(LISTNEW) *.java; then \
	$(JAVAC) $(JAVAC_FLAGS) `$(PERL) $(LISTNEW) *.java`; fi;  

clean:
	rm *.class
