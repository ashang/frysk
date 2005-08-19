jargssrc=$(srcdir)/src/jargs/

all-local: jargs.jar

CLEANFILES = jargs.jar jargs/gnu/*

jargs.jar: $(jargssrc)/gnu/*.java
	javac -g -d . $(jargssrc)/gnu/*.java
	jar cf jargs.jar jargs/*
