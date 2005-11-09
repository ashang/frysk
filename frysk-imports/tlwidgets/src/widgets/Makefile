  LDFLAGS =
   CFLAGS = -g

INSTALL       = install
INSTALLOPTS   =
INSTALLDIR    = /opt/frysk
LIBINSTALLDIR = $(INSTALLDIR)/lib
INCINSTALLDIR = $(INSTALLDIR)/include/ftk
PKGINSTALLDIR = $(LIBINSTALLDIR)/pkgconfig

    INCS = `pkg-config --cflags gtk+-2.0`

     LIBS = `pkg-config --libs   gtk+-2.0` -lm

      CC = gcc

all::libftk.so

%.o:%.c
	$(CC) $(CFLAGS) -std=gnu99 $(INCS) -c $<

ftktimeline.o:   ftktimeline.c    ftktimeline.h
ftkstripchart.o: ftkstripchart.c  ftkstripchart.h

libftk.so: ftktimeline.o ftkstripchart.o
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

