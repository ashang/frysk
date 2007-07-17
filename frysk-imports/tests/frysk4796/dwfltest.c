#include <libdwfl.h>
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>

int main()
{
	static char *flags = "-:.debug:/usr/lib/debug";
	Dwfl_Callbacks *cbs = (Dwfl_Callbacks *) malloc(sizeof (Dwfl_Callbacks));
	cbs->find_elf = dwfl_linux_proc_find_elf;
	cbs->find_debuginfo = dwfl_standard_find_debuginfo;
	cbs->debuginfo_path = &flags; 

	Dwfl* dwfl = dwfl_begin(cbs);
	dwfl_report_begin(dwfl);
	dwfl_report_module(dwfl, "module1", 0, 10);
	dwfl_report_end(dwfl, NULL, NULL);
	dwfl_report_begin(dwfl);
	dwfl_report_module(dwfl, "module1", 0, 10);
	dwfl_report_end(dwfl, NULL, NULL);
	return 0;
}
