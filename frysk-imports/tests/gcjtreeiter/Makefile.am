# The idea is simple, validate that this fails

all-local: PASS1 PASS2

EXTRA_DIST = $(srcdir)/JNICrash.java

PASS1:
	gcj -I$(srcdir)/.. -I$(GTK2_JAR) -I$(GLIB_JAVA_JAR) $(GTK_JAVA_LIBS) \
	-I$(datadir)/glib0.2.jar --main=gcjtreeiter.IterTest $(srcdir)/IterTest.java
	if LD_LIBRARY_PATH=/opt/frysk/lib ./a.out; then touch FAIL1; else touch PASS1; fi

PASS2:
	gcj -C -I$(srcdir)/.. -I$(GTK2_JAR) -I$(GLIB_JAVA_JAR) \
	-I$(datadir)/glib0.2.jar $(srcdir)/IterTest.java
	if gij -classpath $(srcdir)/..:$(GTK2_JAR):$(GLIB_JAVA_JAR) \
	-Djava.library.path=/opt/frysk/lib gcjtreeiter.IterTest; \
	then touch FAIL2; \
	else touch PASS2; fi

clean-local:
	rm -f IterTest IterTest*.class FAIL* PASS*
