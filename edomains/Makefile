all:
	cd compiler; $(MAKE)
#	cd rpcgen; $(MAKE)
	cd verifier; $(MAKE)
	cd compspec; $(MAKE)

clean:
	rm -f *.zip
	find . -name "*.class" -exec rm -f {} \;
