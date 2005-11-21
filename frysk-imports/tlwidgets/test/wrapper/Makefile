#installed
#   CLASSPATH=.:.:/usr/lib/jvm/java-1.4.2-gcj-1.4.2.0/jre/lib/rt.jar:/usr/local/share/java/gtk2.8-2.8.0.jar:/opt/frysk/share/java/cairo1.0-1.0.0.jar:/opt/frysk/share/java/glib0.2-0.2.jar:/opt/frysk/share/ftk.jar
#
#    LD_LIBRARY_PATH=/usr/local/qt/lib:/usr/local/lib:/opt/frysk/lib

#local
#   CLASSPATH=.:.:/opt/frysk/lib:/usr/lib/jvm/java-1.4.2-gcj-1.4.2.0/jre/lib/rt.jar:/usr/local/share/java/gtk2.8-2.8.0.jar:/opt/frysk/share/java/cairo1.0-1.0.0.jar:/opt/frysk/share/java/glib0.2-0.2.jar:../../ftk.jar
   CLASSPATH=.:.:/opt/frysk/lib:/usr/lib/jvm/java-1.4.2-gcj-1.4.2.0/jre/lib/rt.jar:/usr/local/share/java/gtk2.8-2.8.0.jar:/opt/frysk/share/java/cairo1.0-1.0.0.jar:/opt/frysk/share/java/glib0.2-0.2.jar:/opt/frysk/share/java/ftk.jar

#   LD_LIBRARY_PATH=/usr/local/qt/lib:/usr/local/lib:/opt/frysk/lib:../..
   LD_LIBRARY_PATH=/usr/local/qt/lib:/usr/local/lib:/opt/frysk/lib

all::run

StripchartTest.class: StripchartTest.java
	CLASSPATH=$(CLASSPATH) gcj -C $<

run: StripchartTest.class
	LD_LIBRARY_PATH=$(LD_LIBRARY_PATH) gij -cp $(CLASSPATH) StripchartTest

clean:
	rm -f *.class
