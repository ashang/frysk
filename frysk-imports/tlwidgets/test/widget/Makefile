  LDFLAGS =
   CFLAGS = -g

    INCS = `pkg-config --cflags gtk+-2.0` \
           `pkg-config --cflags ftk`

     LIBS = `pkg-config --libs   gtk+-2.0` \
            `pkg-config --libs   ftk` \
            -lm

      CC = gcc

all::tstripchart

%.o:%.c
	$(CC) $(CFLAGS) -std=gnu99 $(INCS) -c $<

ttimeline: ttimeline.o
	$(CC) $(LDFLAGS) -o $@ $(LIBS) $^

ttimeline.o: ttimeline.c

tstripchart: tstripchart.o
	$(CC) $(LDFLAGS) -o $@ $(LIBS) $^

tstripchart.o: tstripchart.c

clean:
	rm -f *.o ttimeline tstripchart

veryclean: clean
	rm -f *~
