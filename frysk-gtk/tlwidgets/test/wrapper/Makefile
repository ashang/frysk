#installed
#   CLASSPATH=.:.:/usr/lib/jvm/java-1.4.2-gcj-1.4.2.0/jre/lib/rt.jar:/usr/local/share/java/gtk2.8-2.8.0.jar
#
#    LD_LIBRARY_PATH=/usr/local/qt/lib:/usr/local/lib

#local
#   CLASSPATH=.:.:/usr/lib/jvm/java-1.4.2-gcj-1.4.2.0/jre/lib/rt.jar:/usr/local/share/java/gtk2.8-2.8.0.jar:../../ftk.jar
   CLASSPATH=.:/usr/lib/jvm/java-1.4.2-gcj-1.4.2.0/jre/lib/rt.jar:/usr/share/frysk/java/gtk2.8.jar:/usr/share/frysk/java/glib0.2.jar:../../../../../frysk-build/frysk-gtk/ftk.jar

#   LD_LIBRARY_PATH=/usr/local/qt/lib:/usr/local/lib:../..
   LD_LIBRARY_PATH=/usr/local/qt/lib:/usr/local/lib

all::run

StripchartTest.class: StripchartTest.java
	CLASSPATH=$(CLASSPATH) gcj -C $<

StripchartXTest.class: StripchartXTest.java
	CLASSPATH=$(CLASSPATH) gcj -C $<

run: StripchartXTest.class
	LD_LIBRARY_PATH=$(LD_LIBRARY_PATH) gij -cp $(CLASSPATH) StripchartXTest

clean:
	rm -f *.class
