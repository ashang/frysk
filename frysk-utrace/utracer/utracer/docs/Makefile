DOCBOOKMAN = docbook2man
DOCBOOKPDF = docbook2pdf
DOCBOOKTXT = docbook2txt

TARGET: pages

BODIES = utracer_open_body.sgml \
	 utracer_unregister_body.sgml

%.7:%.sgml
	$(DOCBOOKMAN) $<

%.2:%.sgml
	$(DOCBOOKMAN) $<

pages: utracer.sgml $(BODIES)
	$(DOCBOOKMAN) $<

utracer.pdf: utracer.sgml $(BODIES)
	$(DOCBOOKPDF) $<

pdf: utracer.pdf

utracer.txt: utracer.sgml $(BODIES)
	$(DOCBOOKTXT) $<

txt: utracer.txt

utracer_open.2:		utracer_open.sgml utracer_open_body.sgml
utracer_unregister.2:	utracer_unregister.sgml utracer_unregister_body.sgml
utracer.7:		utracer_intro.sgml utracer_intro_body.sgml

clean:
	rm -f *.2 *.pdf *.txt manpage.*
