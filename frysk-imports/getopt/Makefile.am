
getoptsrc=$(srcdir)/src/getopt/gnu/classpath/tools/getopt/

all-local: getopt.jar

EXTRA_DIST= $(getoptsrc)/*.java \
		$(getoptsrc)/*.properties

CLEANFILES = getopt.jar gnu/classpath/tools/getopt/*

getopt.jar: $(getoptsrc)/*.java
	$(JAVAC) $(JAVACFLAGS) -g -d . $(getoptsrc)/*.java
	cp $(getoptsrc)/*.properties ./gnu/classpath/tools/getopt
	$(JAR) cf getopt.jar gnu/classpath/tools/getopt/*
