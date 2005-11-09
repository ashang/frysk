INSTALL       = install
INSTALLOPTS   =
INSTALLDIR    = /opt/frysk
LIBINSTALLDIR = $(INSTALLDIR)/lib
JARINSTALLDIR = $(INSTALLDIR)/share/java


   all::libftkjava.so src/java/com/redhat/ftk/Stripchart.class ftk.jar


src/java/com/redhat/ftk/Stripchart.class: src/java/com/redhat/ftk/Stripchart.java
	gcj -C \
            -classpath /opt/frysk/share/java/glib0.2-0.2.jar:/opt/frysk/share/java/gtk2.8.jar:src/java:./src/java \
            -d src/java $<

ftk.jar: src/java/com/redhat/ftk/Stripchart.class
	jar cf ftk.jar -C src/java com/redhat/ftk/Stripchart.class
	
src/java/com/redhat/ftk/Stripchart.o: src/java/com/redhat/ftk/Stripchart.java
	gcj -fjni -I./src/java -Isrc/java -I/opt/frysk/share/java/gtk2.8.jar \
            --classpath=/opt/frysk/share/java/glib0.2-0.2.jar:./src/java \
            -g -O2 -c $< -o $@

libftkjava.so: src/java/com/redhat/ftk/Stripchart.o
	gcj -shared $^ \
            -Wl,--rpath -Wl,/opt/frysk/lib -L/opt/frysk/lib /opt/frysk/lib/libglibjava.so  \
            -Wl,-soname -Wl,libftkjava.so -o $@

install-so: libftkjava.so
	$(INSTALL) $(INSTALLOPTS) $^ $(LIBINSTALLDIR)

install-jar: ftk.jar
	$(INSTALL) $(INSTALLOPTS) $^ $(JARINSTALLDIR)

install-lib:
	$(MAKE) -C src/widgets install

install: install-lib install-so install-jar

clean:
	$(MAKE) -C src/widgets veryclean
	rm -f \
	src/java/com/redhat/ftk/Stripchart.o libftkjava.so \
	src/java/com/redhat/ftk/Stripchart.class ftk.jar


