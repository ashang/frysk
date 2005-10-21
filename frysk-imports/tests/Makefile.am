EXTRA_DIST =
# Gag all the output from the failing tests
TESTS_ENVIRONMENT = eval > log 2>&1
XFAIL_TESTS =
TESTS = $(XFAIL_TESTS)
all-local: $(TESTS)
$(TESTS) $(XFAIL_TESTS): Makefile
CLEANFILES = $(TESTS)
clean-local:
	rm -f */.class
	rm -f */*.o
	rm -f */a.out
	rm -f a.out

# This fails on gcj 4, but passes on gcj 4.1.
anoncall_FILES = anoncall/Anon.java
XFAIL_TESTS += anoncall/a.out
EXTRA_DIST += $(anoncall_FILES)
anoncall/a.out: $(anoncall_FILES)
	mkdir -p anoncall
	gcj -o anoncall/a.out --main=Anon $(srcdir)/anoncall/Anon.java

TESTS += cniinner/a.out
cniinner_FILES = \
	cniinner/native.cxx \
	cniinner/Parent.java \
	cniinner/Child.java
EXTRA_DIST += $(cniinner_FILES)
cniinner/a.out: $(cniinner_FILES)
	mkdir -p cniinner
	gcj -I$(srcdir) -d . -C $(srcdir)/cniinner/Parent.java
	gcj -I$(srcdir) -d . -C $(srcdir)/cniinner/Child.java
	gcjh -I. cniinner.Parent
	gcjh -I. cniinner.Child
	gcjh -I. 'cniinner.Child$$Nested'
	gcj -g -I. -c -o cniinner/native.o $(srcdir)/cniinner/native.cxx
	gcj -g -I. --main=cniinner.Child -o cniinner/a.out \
		$(srcdir)/cniinner/Parent.java \
		$(srcdir)/cniinner/Child.java \
		cniinner/native.o

XFAIL_TESTS += samename/a.out
samename_FILES = samename/SameName.java
EXTRA_DIST += $(samename_FILES)
samename/a.out: $(samename_FILES)
	mkdir -p samename
	gcj -g --main=SameName \
		$(srcdir)/samename/SameName.java \
		-o samename/a.out

# Compiling Werror with -Werror -Wall should get a warning and hence
# abort the compilation.  At least GCC 4.0.1 20050727 (Red Hat
# 4.0.1-5) does not abort, and consequently this test is marked as a
# FAIL.  The build system contains hacks to get around this.
EXTRA_DIST += $(srcdir)/Werror.java
CLEANFILES += werror/FAIL
all-local: werror/FAIL
werror/FAIL: werror/Werror.java Makefile
	mkdir -p werror
	if gcj -Werror -Wall -c -o /dev/null $(srcdir)/werror/Werror.java ; \
	then touch werror/FAIL ; \
	else touch werror/PASS ; \
	fi
	test -r werror/FAIL # expected to fail

TESTS += textiter/a.out
textiter_FILES = textiter/JNICrash.java
EXTRA_DIST += $(textiter_FILES)
textiter/a.out: $(textiter_FILES)
	mkdir -p textiter
	gcj -g \
		-I$(GTK2_JAR) -I$(GLIB_JAVA_JAR) $(GTK_JAVA_LIBS) \
		-Wl,-rpath,/opt/frysk/lib \
		-Djava.library.path=/opt/frysk/lib \
		-I$(datadir)/glib0.2.jar \
		--main=textiter.JNICrash \
		-o textiter/a.out \
		$(srcdir)/textiter/JNICrash.java

gcjtreeiter_FILES = gcjtreeiter/IterTest.java
EXTRA_DIST += $(gcjtreeiter_FILES)
XFAIL_TESTS += gcjtreeiter/a.out
gcjtreeiter/a.out: $(gcjtreeiter_FILES)
	mkdir -p gcjtreeiter
	gcj -g \
		-I$(GTK2_JAR) -I$(GLIB_JAVA_JAR) $(GTK_JAVA_LIBS) \
		-Wl,-rpath,/opt/frysk/lib \
		-Djava.library.path=/opt/frysk/lib \
		-I$(datadir)/glib0.2.jar \
		--main=gcjtreeiter.IterTest \
		$(srcdir)/gcjtreeiter/IterTest.java \
		-o gcjtreeiter/a.out
XFAIL_TESTS += gcjtreeiter/gij.sh
gcjtreeiter/gij.sh: $(gcjtreeiter_FILES)
	mkdir -p gcjtreeiter
	gcj -g -C -d . -I$(srcdir) -I$(GTK2_JAR) -I$(GLIB_JAVA_JAR) \
		-I$(datadir)/glib0.2.jar \
		$(srcdir)/gcjtreeiter/IterTest.java
	echo gij -classpath $(srcdir)/..:$(GTK2_JAR):$(GLIB_JAVA_JAR) \
		-Djava.library.path=/opt/frysk/lib gcjtreeiter.IterTest \
		> gcjtreeiter/gij.sh
	chmod a+x gcjtreeiter/gij.sh
