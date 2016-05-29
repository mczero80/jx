default:
	cd ../../../.. ; $(MAKE)
compile:
	@echo "Files to compile: "
	@if $(PERL) $(LISTNEW) *.java; then \
	$(JAVAC) `$(PERL) $(LISTNEW) *.java`; fi;  

clean:
	rm *.class
