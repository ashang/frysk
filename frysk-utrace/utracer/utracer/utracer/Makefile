#  TARGET = libutracer.a libutracer.so
  TARGET = libutracer.a

 LDFLAGS =
  CFLAGS = -g
    INCS = -I../include
    LIBS =
      CC = gcc

all::$(TARGET)

%.o:%.c
	$(CC) $(CFLAGS) $(INCS) -c $<

libutracer.a: utracer.o
	ar cr $@ $^

libutracer.so: utracer.o
	$(CC) -shared -o $@ $^

utracer.o: utracer.c ../include/utracer.h

clean:
	rm -f *.o *.so *.a

veryclean: clean
