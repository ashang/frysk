TARGET=udb

UDB_CC = gcc
UDB_CFLAGS = -g
UDB_INCLUDES = -I./utracer/include
#UDB_CFLAGS = -g -DENABLE_MODULE_OPS
UDB_LDFLAGS =
UDB_LIBS = -lreadline -lhistory -lcurses -lpthread

UDB_SOURCE_FILES = udb.c udb-text-ui.c udb-utrace-if.c udb-module.c \
                   udb-response.c udb-i386.c
UDB_OBJECT_FILES = $(UDB_SOURCE_FILES:.c=.o)

UDB_INCS     = udb.h  udb-i386.h
UTRACER_INCS = utracer/include/utracer.h  utracer/include/utracer-errmsgs.h

#all::lcl_install
all::$(TARGET)

%.o:%.c
	$(UDB_CC) $(UDB_CFLAGS) $(UDB_INCLUDES) -c $<

lcl_install: $(TARGET)
	sudo chown root:root $(TARGET)
	sudo chmod u+s $(TARGET)

$(TARGET): $(UDB_OBJECT_FILES)
	gcc -o $@ $(UDB_LDFLAGS) $^ $(UDB_LIBS)

#$(TARGET): $(UDB_OBJECT_FILES)
#	sudo rm -f $@
#	gcc -o $@ $(UDB_LDFLAGS) $^ $(UDB_LIBS)

udb.o:           udb.c          $(UDB_INCS) $(UTRACER_INCS)
udb-i386.o:      udb-i386.c     $(UDB_INCS)
udb-module.o:    udb-module.c   $(UDB_INCS) $(UTRACER_INCS)
udb-response.o:  udb-response.c $(UDB_INCS) $(UTRACER_INCS)
udb-text-ui.o:   udb-text-ui.c  $(UDB_INCS)
udb-utrace-if.o: udb-text-ui.c  $(UDB_INCS) $(UTRACER_INCS)

clean:
	rm -f *.o
	make -C utracer/module clean

veryclean: clean
	rm -f $(TARGET)
	make -C utracer/module veryclean

insert:
	sudo insmod utracer/utracer.ko

remove:
	sudo rmmod utracer/utracer.ko
