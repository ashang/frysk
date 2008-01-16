jlinesrc=$(srcdir)/src/src/main/java/jline
jlinepropsrc=$(srcdir)/src/src/main/resources/jline

all-local: jline.jar

EXTRA_DIST  = $(jlinesrc)/*.java \
			   $(jlinepropsrc)/*.properties

CLEANFILES = jline.jar jline/*

jline.jar: $(jlinesrc)/*.java
	$(JAVAC) $(JAVACFLAGS) -g -d . $(jlinesrc)/*.java
	cp $(jlinepropsrc)/*.properties ./jline
	$(JAR) cf jline.jar jline/*
