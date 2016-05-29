export JXROOT=/home/golm/jx 
export JDKVERSION=/home/golm/tools-linux/IBMJava2-13
export JAVACCLASSES=${JDKVERSION}/lib/tools.jar
export JDKCLASSES=${JDKVERSION}/jre/lib/rt.jar

######################
 
export JXLIBS=${JXROOT}/libs
export JXEDOMAINS=${JXROOT}/edomains
export JXEMULATION=${JXROOT}/emulation
 
export PATH=$JDKVERSION/bin:$JXROOT/tools:$PATH
 
export EMULATIONCLASSPATH=${JXLIBS}/zero:${JXLIBS}/zero_misc:${JXLIBS}/bio:${JXEMULATION}
 
 
unset JAVA_HOME
unset CLASSPATH
 
export SHELL=/bin/sh
 
export ZIPPROG=zip
 
 
export PERL=perl
export GREP=grep
export CMP=cmp
export JIT_GCC=g++
export INDENT=indent
 
export MAKE=make
export TAR=tar
export CC=gcc
export AS=gcc
 
export LD=/usr/bin/ld
 
export A2PS=a2ps
export UNIFDEF="rmsif -k"

export JAVA=java
export JAVAC=javac
 
export MKSYMTAB="${PERL} ${JXROOT}/tools/mksymtab.perl"
 
export JAVAC_FLAGS
