// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.
#include <libdwfl.h>
#include <stdio.h>
#include <stdlib.h>
#include <gcj/cni.h>

#include <gnu/gcj/RawDataManaged.h>

#include "lib/dw/Dwfl.h"

#define DWFL_POINTER (::Dwfl *) this->pointer

void
lib::dw::Dwfl::dwfl_begin(jint pid){
	
	char * flags = "-:.debug:/usr/bin/debug";
	
	::Dwfl_Callbacks *cbs = (::Dwfl_Callbacks*) JvAllocBytes(sizeof(::Dwfl_Callbacks));
	
	cbs->find_elf = ::dwfl_linux_proc_find_elf;
	cbs->find_debuginfo = ::dwfl_standard_find_debuginfo;
	cbs->debuginfo_path = &flags;
	cbs->section_address = NULL;
	
	this->callbacks = (gnu::gcj::RawDataManaged*) cbs;
	
	::Dwfl* dwfl = ::dwfl_begin(cbs);
	
	::dwfl_report_begin(dwfl);
	::dwfl_linux_proc_report(dwfl, (pid_t) pid);
	::dwfl_report_end(dwfl, NULL, NULL);
	
	this->pointer = (jlong) dwfl;
}

void
lib::dw::Dwfl::dwfl_end(){
	::dwfl_end(DWFL_POINTER);
}


//jlong[]
//lib::dw::Dwfl::dwfl_get_modules(){
//	
//}

//jlong[]
//lib::dw::Dwfl::dwfl_getdwarf(){
//	
//}

jlong
lib::dw::Dwfl::dwfl_getsrc(jlong addr){
	return (jlong) ::dwfl_getsrc(DWFL_POINTER, (::Dwarf_Addr) addr);
}

jlong
lib::dw::Dwfl::dwfl_addrdie(jlong addr){
	Dwarf_Addr bias;
	return (jlong) ::dwfl_addrdie(DWFL_POINTER, (::Dwarf_Addr) addr, &bias);	
}
