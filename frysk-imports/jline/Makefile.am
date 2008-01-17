jlinesrc=$(srcdir)/src/jline/

all-local: jline.jar

EXTRA_DIST  = $(jlinesrc)/*.java \
			   $(jlinesrc)/*.properties

CLEANFILES = jline.jar jline/*

jline.jar: $(jlinesrc)/*.java
	$(JAVAC) $(JAVACFLAGS) -g -d . $(jlinesrc)/*.java
	cp $(jlinesrc)/*.properties ./jline
	$(JAR) cf jline.jar jline/*
