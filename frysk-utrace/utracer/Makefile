TARGET=udb

UDB_CC = gcc
UDB_CFLAGS = -g
UDB_LDFLAGS =
UDB_LIBS = -lreadline -lhistory -lcurses -lpthread

UDB_SOURCE_FILES = udb.c udb-text-ui.c udb-utrace-if.c udb-module.c
UDB_OBJECT_FILES = $(UDB_SOURCE_FILES:.c=.o)

all::lcl_install

%.o:%.c
	$(UDB_CC) $(UDB_CFLAGS) -c $<

lcl_install: $(TARGET)
	sudo chown root:root $(TARGET)
	sudo chmod u+s $(TARGET)

udb: $(UDB_OBJECT_FILES)
	sudo rm -f $@
	gcc -o $@ $(UDB_LDFLAGS) $^ $(UDB_LIBS)

udb.o:         udb.c udb.h utracer/utracer.h
udb-text-ui.o: udb-text-ui.c udb.h
udb-utrace-if.o: udb-text-ui.c udb.h utracer/utracer.h

clean:
	rm -f *.o
	make -C utracer clean

veryclean: clean
	rm -f $(TARGET)
	make -C utracer veryclean

remove:
	sudo rmmod utracer/utracer.ko
