  LDFLAGS =
   CFLAGS = -g

INSTALL       = install
INSTALLOPTS   =
INSTALLDIR    = /usr/local
LIBINSTALLDIR = $(INSTALLDIR)/lib
INCINSTALLDIR = $(INSTALLDIR)/include/ftk
PKGINSTALLDIR = $(LIBINSTALLDIR)/pkgconfig

    INCS = `pkg-config --cflags gtk+-2.0`

     LIBS = `pkg-config --libs   gtk+-2.0` -lm

      CC = gcc


SOURCE_FILES = ftkconsole.c ftkstripchart.c ftktimeline.c
OBJ_FILES = $(SOURCE_FILES:.c=.o)

all::libftk.so

%.o:%.c
	$(CC) $(CFLAGS) -std=gnu99 $(INCS) -c $<

ftkconsole.o:    ftkconsole.c     ftkconsole.h
ftktimeline.o:   ftktimeline.c    ftktimeline.h
ftkstripchart.o: ftkstripchart.c  ftkstripchart.h

libftk.so: $(OBJ_FILES)
	$(CC) -shared -o $@ $^ -lrt

install-pkg: ftk.pc
	$(INSTALL) $(INSTALLOPTS) $^ $(PKGINSTALLDIR)

install-so: libftk.so
	$(INSTALL) $(INSTALLOPTS) $^ $(LIBINSTALLDIR)

install-header: ftkstripchart.h
	$(INSTALL) -d $(INCINSTALLDIR)
	$(INSTALL) $(INSTALLOPTS) $^ $(INCINSTALLDIR)

install: install-header install-so install-pkg

clean:
	rm -f *.o

veryclean: clean
	rm -f *.so

