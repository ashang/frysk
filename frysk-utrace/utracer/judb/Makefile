UDB_INCLUDES = -I../utracer/include

UDB_LIBS = -L../utracer/utracer -lutracer

TARGETS = \
Judb

all::$(TARGETS)

JAVA_SRCS = Judb.java \
	Response.java \
	ResponseListener.java \
	Utrace.java

JAVA_OBJS = $(JAVA_SRCS:.java=.o)

%:%.o
	gcj --main=$@ -o $@ $^ $(UDB_LIBS)

%.o:%.java
	gcj -c $<

%.h:%.java
	javac $<
	gcjh  $(<:.java=)

Judb: $(JAVA_OBJS) cni/Utrace.o

Judb.o:		Judb.java Utrace.class
Utrace.o:	Utrace.java
ResponseListener.o: ResponseListener.java
Response.o:	Response.java

	gcjh  Response

cni/Utrace.o: 	cni/Utrace.cxx cni/Utrace.h
	gcc -o $@ -c $(UDB_INCLUDES) $<

cni/Utrace.h:   Utrace.class
	gcjh  Utrace -o ./cni/Utrace.h

Utrace.class:   Utrace.java
	javac Utrace.java

clean:
	rm -f *.o *.class cni/*.o cni/Utrace.h Response.h

veryclean: clean
	rm -f $(TARGETS) *~

insert:
	sudo /sbin/insmod i../utracer/module/utracer.ko

remove:
	sudo /sbin/rmmod ../utracer/module/utracer.ko
