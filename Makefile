all: builder jx realmode jxcore
	build
	@date

fast: BFILES
	touch JC_CONFIG
	@echo Compiling...
	@cat BFILES | xargs javac -d edomains/compspec/classes -classpath emulation:edomains/compspec/classes:$(CB0P)
	build
	@date

jx:
	cd jcore; $(MAKE) jx

jxcore:
	cd jcore; $(MAKE) jxcore

realmode:
	cd jcore; $(MAKE) realmode

rc:
	build

clean:
	rm -f *~ code.zip libs/*.zip libs/*.jln libs/*.jll libs/COMPONENTS.inc
	find libs -name "*.class" | xargs rm -f
	find libs -name "*.imcode" | xargs rm -f
	find libs -name "Makefile" | xargs rm -f

cvstest:
	cvs -nq update | grep -v "^?"

postcommit:
	cd /tmp; rm -rf jx-$(LOGNAME) ; mkdir jx-$(LOGNAME) ; cd jx-$(LOGNAME) ; cvs checkout jx ; cd jx ; $(MAKE) ; cvs release

cleanpost:
	cd /tmp; rm -rf jx

update: clean
	cvs -Pq update -d

commit: clean 
	cvs commit

comdocs:
	rm -rf docs
	mkdir docs
	@for i in `cat libs/ALLCOMPONENTS` ; do \
		( j=`/usr/bin/dirname $$i` ; k=`/bin/basename $$j` ; echo "* Processing component $$k" ; mkdir docs/$$k ; javadoc -author -version -d docs/$$k `find libs/$$k -name "*.java"` )  ; \
	done

comdocs1:
	rm -rf docs
	mkdir docs
	@for i in `cat libs/ALLCOMPONENTS` ; do \
		( echo "* Processing component $$i" ; mkdir docs/$$i ; javadoc -author -version -d docs/$$k `find libs/$$k -name "*.java"` )  ; \
	done

alldocs:
	rm -rf docs
	mkdir docs
	cd docs ; javadoc -author -version -public -windowtitle "JX Documentation" -d . `find ../libs -name "*.java"`

cleandocs:
	rm -rf docs

cleanall: clean cleanmakefiles rm-imcode cleandocs

COMPONENTS_BUILDER  = zero zero_misc bio classfile zip collections bootrc compspec classstore verifier formats compiler compiler_env 
COMPONENTS_BUILDER += xdr timer buffer devices net net_manager rpc rpcgen
EDOMAINS_BUILDER    = compiler compspec
EDOMAINS_BUILDER   += rpcgen

COMPONENTS_RPCGEN = zero zero_misc bio classfile zip collections bootrc compspec classstore xdr timer buffer devices net net_manager rpc rpcgen
EDOMAINS_RPCGEN=rpcgen

COMPONENTS_VERIFIER = $(COMPONENTS_BUILDER) verifier
EDOMAINS_VERIFIER= $(EDOMAINS_BUILDER) verifier

define SPACE
 
endef
CB0 = $(COMPONENTS_BUILDER:%=${JXLIBS}/%)
CB0P= $(subst $(SPACE),:,$(CB0))
CR0 = $(COMPONENTS_RPCGEN:%=${JXLIBS}/%)
CR0P= $(subst $(SPACE),:,$(CR0))
CV0 = $(COMPONENTS_VERIFIER:%=${JXLIBS}/%)
CV0P= $(subst $(SPACE),:,$(CV0))

updatebuilder:
	@rm -rf BFILES; touch BFILES
	@echo Compiling...
	javac -d edomains/compspec/classes -classpath emulation:edomains/compspec/classes:$(CB0P) edomains/compspec/jx/compspec/*.java libs/rpcgen/jx/rpcgen/*.java libs/compiler_env/jx/compiler/*.java libs/compiler_env/jx/compiler/vtable/*.java edomains/compiler/jx/compiler/*.java libs/compiler/jx/compiler/*/*.java

BFILES:
	@rm -rf BFILES; touch BFILES
	@echo Finding Java files...
	@for i in $(COMPONENTS_BUILDER) ; do find libs/$$i -name "*.java"  >> BFILES;	done
	@for i in $(EDOMAINS_BUILDER) ; do \
	    find edomains/$$i -name "*.java"  >> BFILES; \
	done

builder:
	@echo Removing old class files...
	@rm -rf edomains/compspec/classes; mkdir edomains/compspec/classes
	@rm -rf BFILES; touch BFILES
	@echo Finding Java files...
	@for i in $(COMPONENTS_BUILDER) ; do find libs/$$i -name "*.java"  >> BFILES;	done
	@for i in $(EDOMAINS_BUILDER) ; do \
	    find edomains/$$i -name "*.java"  >> BFILES; \
	done
	@echo Compiling...
	@cat BFILES | xargs javac -d edomains/compspec/classes -classpath emulation:edomains/compspec/classes:$(CB0P)

rpcgen:
	@echo Removing old class files...
	@rm -rf edomains/rpcgen/classes; mkdir edomains/rpcgen/classes
	@rm -rf BFILES; touch BFILES
	@echo Finding Java files...
	@for i in $(COMPONENTS_RPCGEN) ; do find libs/$$i -name "*.java"  >> BFILES;	done
	@for i in $(EDOMAINS_RPCGEN) ; do \
	    find edomains/$$i -name "*.java"  >> BFILES; \
	done
	@echo Compiling...
	@cat BFILES | xargs javac -d edomains/rpcgen/classes -classpath emulation:edomains/rpcgen/classes:$(CR0P)

verifier:
	@echo Removing old class files...
	@rm -rf edomains/verifier/classes; mkdir edomains/verifier/classes
	@rm -rf BFILES; touch BFILES
	@echo Finding Java files...
	@for i in $(COMPONENTS_VERIFIER) ; do find libs/$$i -name "*.java"  >> BFILES;	done
	@for i in $(EDOMAINS_VERIFIER) ; do \
	    find edomains/$$i -name "*.java"  >> BFILES; \
	done
	@echo Compiling...
	@cat BFILES | xargs javac -d edomains/verifier/classes -classpath emulation:edomains/verifier/classes:$(CV0P)

fd0:
	mount /dev/fd0 /mnt2
	rm -f /mnt2/code.zip /mnt2/jxcore
	cp code.zip /mnt2
	strip jcore/jxcore ; cat jcore/jxcore | gzip -9 -c > /mnt2/jxcore
	umount /mnt2

image:
	mount -o loop floppy /mnt2
	rm -f /mnt2/code.zip /mnt2/jxcore
	cp code.zip /mnt2
	strip jcore/jxcore ; cat jcore/jxcore | gzip -c > /mnt2/jxcore
	umount /mnt2
