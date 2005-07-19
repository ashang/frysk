jlinesrc=$(srcdir)/src/jline/

all-local: jline

jline: $(jlinesrc)/*.java
	gcj -c --classpath=$(srcdir)/src/ $(jlinesrc)/*.java
	ar r libjline.a *.o
	javac -d . $(jlinesrc)/*.java
	cp $(jlinesrc)/*.properties ./jline
	jar cf jline.jar ./jline/*
