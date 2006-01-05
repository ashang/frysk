  LDFLAGS =
   CFLAGS = -g

#  for installed 
#    INCS = `pkg-config --cflags gtk+-2.0` \
#           `pkg-config --cflags ftk`
#
#    LIBS = `pkg-config --libs   gtk+-2.0` \
#            `pkg-config --libs   ftk` \
#            -lm
#
#    LD_LIBRARY_PATH=/usr/local/qt/lib:/usr/local/lib:/opt/frysk/lib

#  for local
    INCS = `pkg-config --cflags gtk+-2.0` \
           -I../../src/widgets

     LIBS = `pkg-config --libs   gtk+-2.0` \
            -L../../src/widgets -lftk \
            -lm

   LD_LIBRARY_PATH=../../src/widgets

      CC = gcc

all::run

%.o:%.c
	$(CC) $(CFLAGS) -std=gnu99 $(INCS) -c $<

ttimeline: ttimeline.o
	$(CC) $(LDFLAGS) -o $@ $(LIBS) $^

ttimeline.o: ttimeline.c

tstripchart: tstripchart.o
	$(CC) $(LDFLAGS) -o $@ $(LIBS) $^

tstripchart.o: tstripchart.c

tconsole: tconsole.o
	$(CC) $(LDFLAGS) -o $@ $(LIBS) $^

tconsole.o: tconsole.c

run: tstripchart
	LD_LIBRARY_PATH=$(LD_LIBRARY_PATH) ./tstripchart

clean:
	rm -f *.o ttimeline tstripchart

veryclean: clean
	rm -f *~
