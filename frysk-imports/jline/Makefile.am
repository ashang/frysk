jlinesrc=$(srcdir)/src/jline/

all-local: jline.jar

CLEANFILES = jline.jar jline/*

jline.jar: $(jlinesrc)/*.java
	javac -g -d . $(jlinesrc)/*.java
	cp $(jlinesrc)/*.properties ./jline
	jar cf jline.jar jline/*
