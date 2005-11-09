tlwidgetssrc=$(srcdir)/src

all-local: libftk.so libftkjava.so ftk.jar

EXTRA_DIST= $(tlwidgetssrc)/java/com/redhat/ftk/*.java \
            $(tlwidgetssrc)/jni/*.c \
            $(tlwidgetssrc)/widgets/*.c \
            $(tlwidgetssrc)/widgets/*.h \
            $(tlwidgetssrc)/widgets/ftk.pc

CLEANFILES = libftk.so libftkjava.so ftk.jar \
             $(tlwidgetssrc)/java/com/redhat/ftk/*.o \
             $(tlwidgetssrc)/java/com/redhat/ftk/*.class \
             $(tlwidgetssrc)/widgets/*.o

$(tlwidgetssrc)/java/com/redhat/ftk/Stripchart.class: $(tlwidgetssrc)/java/com/redhat/ftk/Stripchart.java
	gcj -C \
	    -classpath /opt/frysk/share/java/glib0.2-0.2.jar:/opt/frysk/share/java/gtk2.8.jar:src/java:./src/java \
            -d src/java $<

ftk.jar: $(tlwidgetssrc)/java/com/redhat/ftk/Stripchart.class
	jar cf ftk.jar -C $(tlwidgetssrc)/java com/redhat/ftk/Stripchart.class

$(tlwidgetssrc)/java/com/redhat/ftk/Stripchart.o: $(tlwidgetssrc)/java/com/redhat/ftk/Stripchart.java
	gcj -fjni -I./src/java -Isrc/java -I/opt/frysk/share/java/gtk2.8.jar \
            --classpath=/opt/frysk/share/java/glib0.2-0.2.jar:./src/java \
            -g -O2 -c $< -o $@

libftkjava.so: $(tlwidgetssrc)/java/com/redhat/ftk/Stripchart.o
	gcj -shared $^ \
            -Wl,--rpath -Wl,/opt/frysk/lib -L/opt/frysk/lib /opt/frysk/lib/libglibjava.so  \
            -Wl,-soname -Wl,libftkjava.so -o $@

$(tlwidgetssrc)/widgets/ftkstripchart.o: $(tlwidgetssrc)/widgets/ftkstripchart.c  $(tlwidgetssrc)/widgets/ftkstripchart.h
	$(CC) $(CFLAGS) -std=gnu99 `pkg-config --cflags gtk+-2.0` -c -o $@ $<

libftk.so: $(tlwidgetssrc)/widgets/ftkstripchart.o
	$(CC) -shared -o $@ $^ -lrt

