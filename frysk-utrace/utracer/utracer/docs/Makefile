DOCBOOKMAN = docbook2man
DOCBOOKPDF = docbook2pdf
DOCBOOKTXT = docbook2txt

TARGET: pages

BODIES = utracer_open_body.sgml \
	 utracer_unregister_body.sgml

%.2:%.sgml
	$(DOCBOOKMAN) $<

pages: utracer.sgml $(BODIES)
	$(DOCBOOKMAN) $<

pdf: utracer.sgml $(BODIES)
	$(DOCBOOKPDF) $<
txt: utracer.sgml $(BODIES)
	$(DOCBOOKTXT) $<

utracer_open.2:		utracer_open.sgml utracer_open_body.sgml
utracer_unregister.2:	utracer_unregister.sgml utracer_unregister_body.sgml

clean:
	rm -f *.2 *.pdf *.txt