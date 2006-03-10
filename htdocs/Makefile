all: ./index.html \
	build/index.html \
	documentation/index.html \
	getinvolved/index.html \
	links/index.html \
	people/index.html \
	questions/index.html \
	usecases/index.html \
	workflow/index.html
.PHONY: all

clean:
	rm -f *.new */*.new
.PHONY: clean

REBUILD = @\
	rm -f $@.tmp ; \
	echo "Verfying $@:" ; \
	for b in title logo text ; do \
	  for a in start end ; do \
	    if grep -e "-- $$a $$b --" $@ ; then \
	      : ; \
	    else \
	      echo "$@: missing -- $$a $$b --" ; \
	      exit 1 ; \
	    fi ; \
	  done ; \
	done ; \
	echo "Building $@.new:" ; \
	( \
	  sed -n '1,/-- start title --/ p' template.html ; \
	  sed -e '1,/-- start title --/ d' -e '/-- end title --/,$$ d' $@ ; \
	  sed -n '/-- end title --/,/-- start logo --/ p' template.html ; \
	  sed -e '1,/-- start logo --/ d' -e '/-- end logo --/,$$ d' $@ ; \
	  sed -n '/-- end logo --/,/-- start text --/ p' template.html ; \
	  sed -e '1,/-- start text --/ d' -e '/-- end text --/,$$ d' $@ ; \
	  sed -n '/-- end text --/,$$ p' template.html ; \
	) > $@.new ; \
	:

index.html: template.html
	$(REBUILD)
%/index.html: template.html
	$(REBUILD)
