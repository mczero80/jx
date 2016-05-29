setenv JXROOT    /home/golm/jx 

setenv JDKVERSION /home/golm/tools-linux/IBMJava2-13
setenv JAVACCLASSES ${JDKVERSION}/lib/tools.jar
setenv JDKCLASSES ${JDKVERSION}/jre/lib/rt.jar

#setenv JDKVERSION /home/golm/tools-linux/jdk118_v1
#setenv JAVACCLASSES ${JDKVERSION}/lib/classes.zip
#setenv JDKCLASSES ${JDKVERSION}/lib/classes.zip
#setenv JDK -jdk_sun_118

######################
 
setenv JXLIBS    ${JXROOT}/libs
setenv JXEDOMAINS ${JXROOT}/edomains
setenv JXEMULATION ${JXROOT}/emulation
 
set path=( ${JDKVERSION}/bin ${JXROOT}/tools $path)
 
setenv EMULATIONCLASSPATH ${JXLIBS}/zero:${JXLIBS}/zero_misc:${JXLIBS}/bio:${JXEMULATION}
 
 
unsetenv JAVA_HOME
unsetenv CLASSPATH
 
setenv SHELL /bin/sh
 
setenv ZIPPROG zip
 
 
setenv PERL perl
setenv GREP grep
setenv CMP cmp
setenv JIT_GCC g++
setenv INDENT indent
 
setenv MAKE make
setenv TAR tar
setenv CC gcc
setenv AS gcc
 
setenv LD  /usr/bin/ld
 
setenv A2PS a2ps
setenv UNIFDEF "rmsif -k"

setenv JAVA  java
setenv JAVAC javac
 
setenv MKSYMTAB "${PERL} ${JXROOT}/tools/mksymtab.perl"
 
setenv JAVAC_FLAGS
