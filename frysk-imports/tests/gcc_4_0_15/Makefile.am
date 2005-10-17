# The idea is simple, validate that this fails

all-local: FAIL

FAIL:
	gcj -I$(srcdir)/.. -I$(GTK2_JAR) -I$(GLIB_JAVA_JAR) $(GTK_JAVA_LIBS) -I$(datadir)/glib0.2.jar --main=gcc_4_0_15.JNICrash $(srcdir)/JNICrash.java
	if LD_LIBRARY_PATH=/opt/frysk/lib ./a.out ; then touch FAIL ; else touch PASS ; fi
	touch TEST
