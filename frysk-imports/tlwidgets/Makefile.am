tlwidgetssrc=$(srcdir)/src
tlwidgetsdst=$(top_builddir)/$(subdir)

all-local: $(tlwidgetsdst)/libftk.so $(tlwidgetsdst)/libftkjava.so $(tlwidgetsdst)/libftkjni.so $(tlwidgetsdst)/ftk.jar

EXTRA_DIST= $(tlwidgetssrc)/java/com/redhat/ftk/*.java \
            $(tlwidgetssrc)/jni/*.c \
            $(tlwidgetssrc)/widgets/*.c \
            $(tlwidgetssrc)/widgets/*.h \
            $(tlwidgetssrc)/widgets/ftk.pc

CLEANFILES = $(tlwidgetsdst)/*.so \
             $(tlwidgetsdst)/*.jar \
             $(tlwidgetsdst)/*.o \
             $(tlwidgetsdst)/*.class

$(tlwidgetsdst)/com/redhat/ftk/Stripchart.class: $(tlwidgetssrc)/java/com/redhat/ftk/Stripchart.java
	gcj -C -d $(tlwidgetsdst) \
	    -classpath /opt/frysk/share/java/glib0.2-0.2.jar:/opt/frysk/share/java/gtk2.8.jar \
            $<

$(tlwidgetsdst)/ftk.jar: $(tlwidgetsdst)/com/redhat/ftk/Stripchart.class
	$(JAR) cf ftk.jar -C $(tlwidgetsdst) com/redhat/ftk/Stripchart.class

$(tlwidgetsdst)/Stripchart.o: $(tlwidgetssrc)/java/com/redhat/ftk/Stripchart.java
	gcj -fjni -I/opt/frysk/share/java/gtk2.8.jar \
            --classpath=/opt/frysk/share/java/glib0.2-0.2.jar:./src/java \
            -g -O2 -c $< -o $@

$(tlwidgetsdst)/libftkjava.so: $(tlwidgetsdst)/Stripchart.o
	gcj -shared $^ \
            -Wl,--rpath -Wl,/opt/frysk/lib -L/opt/frysk/lib /opt/frysk/lib/libglibjava.so  \
            -Wl,-soname -Wl,libftkjava.so -o $@

$(tlwidgetsdst)/com_redhat_ftk_Stripchart.o: $(tlwidgetssrc)/jni/com_redhat_ftk_Stripchart.c
	gcc -c $< -I$(tlwidgetssrc)/widgets/ \
	    -I/opt/frysk/include/gtk-2.0 \
	    -I/opt/frysk/include/glib-java \
	    -I/opt/frysk/include/cairo \
	    -I/opt/frysk/include/glib-2.0 \
	    -I/opt/frysk/include/pango-1.0 \
	    -I/opt/frysk/include/libgtk-java \
	    -I/opt/frysk/lib/glib-2.0/include \
	    -I/opt/frysk/lib/gtk-2.0/include \
	    `pkg-config --cflags atk` \
	    -fPIC -DPIC -o $@

$(tlwidgetsdst)/libftkjni.so: $(tlwidgetsdst)/com_redhat_ftk_Stripchart.o
	gcc -shared $^ \
	-Wl,--rpath \
	-Wl,/opt/frysk/lib -Wl,--rpath -Wl,/opt/frysk/lib -L/opt/frysk/lib \
	-lgthread-2.0 -lgtk-x11-2.0 -lgdk-x11-2.0 -latk-1.0 -lgdk_pixbuf-2.0 -lm \
	-lpangocairo-1.0 -lpango-1.0 -lgobject-2.0 -lgmodule-2.0 -ldl -lglib-2.0 \
	-lcairo /opt/frysk/lib/libglibjni.so  -pthread -Wl,-soname -Wl,libftkjni.so \
	-o $@

$(tlwidgetsdst)/ftkstripchart.o: $(tlwidgetssrc)/widgets/ftkstripchart.c  $(tlwidgetssrc)/widgets/ftkstripchart.h
	$(CC) $(CFLAGS) -std=gnu99 `pkg-config --cflags gtk+-2.0` -I$(tlwidgetssrc)/widgets -c -o $@ $<

$(tlwidgetsdst)/libftk.so: $(tlwidgetsdst)/ftkstripchart.o
	$(CC) -shared -o $@ $^ -lrt

