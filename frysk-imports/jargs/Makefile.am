jargssrc=$(srcdir)/src/jargs/

all-local: jargs.jar

EXTRA_DIST= $(jargssrc)/gnu/CmdLineParser.java \
			   $(jargssrc)/examples/gnu/*.java \
			   $(jargssrc)/test/gnu/*.java

CLEANFILES = jargs.jar jargs/gnu/*

jargs.jar: $(jargssrc)/gnu/*.java
	$(JAVAC) -g -d . $(jargssrc)/gnu/*.java
	$(JAR) cf jargs.jar jargs/*
