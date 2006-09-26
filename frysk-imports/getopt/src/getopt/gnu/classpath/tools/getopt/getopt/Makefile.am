
getoptsrc=$(srcdir)/src/getopt/gnu/classpath/tools/getopt/

all-local: getopt.jar

EXTRA_DIST= $(getoptsrc)/*.java

CLEANFILES = getopt.jar gnu/classpath/tools/getopt/*

getopt.jar: $(getoptsrc)/*.java
	$(JAVAC) -g -d . $(getoptsrc)/*.java
	$(JAR) cf getopt.jar gnu/classpath/tools/getopt/*
