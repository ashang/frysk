# The idea is simple, validate that this fails

all-local: PASS

EXTRA_DIST = $(srcdir)/JNICrash.java

PASS:
	gcj -I$(srcdir)/.. -I$(GTK2_JAR) -I$(GLIB_JAVA_JAR) $(GTK_JAVA_LIBS) \
	-I$(datadir)/glib0.2.jar --main=textiter.JNICrash $(srcdir)/JNICrash.java
	LD_LIBRARY_PATH=/opt/frysk/lib ./a.out
	touch PASS

clean-local:
	rm -f a.out JNICrash*.class FAIL PASS
