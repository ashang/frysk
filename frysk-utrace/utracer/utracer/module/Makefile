obj-m += utracer.o
utracer-objs := utracer-base.o utracer-cf-io.o utracer-if-io.o

all:
	make -C /lib/modules/$(shell uname -r)/build M=$(PWD) modules

clean:
	make -C /lib/modules/$(shell uname -r)/build M=$(PWD) clean
	sudo rm -f  Module.symvers
	sudo rm -rf .tmp_versions

veryclean: clean
	rm -f utracer.ko

