all: \
	cagney-k-startterminal.jpg \
	cagney-k-runsbuild.jpg \
	cagney-k-alert.jpg

FIG2DEV = fig2dev
.SUFFIXES: .fig .jpg
.fig.jpg:
	mkdir -p `dirname $@`
	$(FIG2DEV) -L jpeg $< $@
