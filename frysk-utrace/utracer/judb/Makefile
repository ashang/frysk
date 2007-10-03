UDB_INCLUDES = -I../utracer/include

UDB_LIBS = -L../utracer/utracer -lutracer

TARGETS = \
Judb

all::$(TARGETS)

%:%.o
	gcj --main=$@ -o $@ $^ $(UDB_LIBS)

%.o:%.java
	gcj -c $<

Judb: Judb.o Utrace.o  cni/Utrace.o

Judb.o:		Judb.java Utrace.class
Utrace.o:	Utrace.java

cni/Utrace.o: 	cni/Utrace.cxx cni/Utrace.h
	gcc -o $@ -c $(UDB_INCLUDES) $<

cni/Utrace.h:   Utrace.class
	gcjh  Utrace -o ./cni/Utrace.h

Utrace.class:   Utrace.java
	javac Utrace.java

clean:
	rm -f *.o *.class cni/*.o cni/Utrace.h

veryclean: clean
	rm -f $(TARGETS) *~
