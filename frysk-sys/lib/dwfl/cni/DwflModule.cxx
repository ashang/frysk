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

#include <cstdlib>
#include <cstring>
#include "libdwfl.h"

#include <gcj/cni.h>

#include <gnu/gcj/RawData.h>

#include "lib/dwfl/DwflModule.h"
#include "lib/dwfl/DwflLine.h"
#include "lib/dwfl/ModuleElfBias.h"
#include "lib/dwfl/SymbolBuilder.h"
#include "lib/dwfl/Elf.h"

#define DWFL_MODULE_POINTER (Dwfl_Module *) this->pointer

jstring
lib::dwfl::DwflModule::getName()
{
  if (!name)
    name = JvNewStringUTF(dwfl_module_info(DWFL_MODULE_POINTER,
					   0, 0, 0, 0, 0, 0, 0));
  return name;
}

lib::dwfl::ModuleElfBias*
lib::dwfl::DwflModule::module_getelf()
{
	Dwarf_Addr bias = 0;
	::Elf *elf = dwfl_module_getelf(DWFL_MODULE_POINTER, &bias);
	if(elf == NULL)
		return NULL;
		
	lib::dwfl::ModuleElfBias *ret = new lib::dwfl::ModuleElfBias();
	ret->elf = new lib::dwfl::Elf((gnu::gcj::RawData*) elf);
	ret->bias = (jlong) bias;
		
	return ret;	
}

typedef JArray<lib::dwfl::DwflLine *> DwflLineArray;

DwflLineArray *
lib::dwfl::DwflModule::getLines(jstring filename, jint lineno, jint column)
{
  int fileNameLength =  JvGetStringUTFLength(filename);
  char fileName[fileNameLength + 1];
  JvGetStringUTFRegion(filename, 0, filename->length(), fileName);
  fileName[fileNameLength] = 0;
  ::Dwfl_Line **srcsp = 0;
  size_t nsrcs = 0;
  int result = ::dwfl_module_getsrc_file(DWFL_MODULE_POINTER, fileName, lineno,
					 column, &srcsp, &nsrcs);
  if (result >= 0)
    {
      DwflLineArray *array
	= (DwflLineArray *)JvNewObjectArray(nsrcs,
					    &lib::dwfl::DwflLine::class$,
					    0);
      for (size_t i = 0; i < nsrcs; i++)
	{
	  lib::dwfl::DwflLine *line = new lib::dwfl::DwflLine((jlong)srcsp[i],
							  getParent());
	  elements(array)[i] = line;
	}
      std::free(srcsp);
      return array;
    }
  return 0;
} 

void
lib::dwfl::DwflModule::getSymbol(jlong address, lib::dwfl::SymbolBuilder* symbolBuilder)
{
	Dwarf_Addr addr = (Dwarf_Addr) address;
	GElf_Sym closest_sym;
	
	const char* methName = dwfl_module_addrsym(DWFL_MODULE_POINTER, addr, 
                                                   &closest_sym, NULL);
	
	jstring jMethodName;
	if (methName == NULL)	
		jMethodName = NULL;
	else
		jMethodName = JvNewStringUTF(methName);	
	
	symbolBuilder->symbol(jMethodName,
			      closest_sym.st_value, 
			      closest_sym.st_size,
			      ELF64_ST_TYPE(closest_sym.st_info),
			      ELF64_ST_BIND(closest_sym.st_info),
			      closest_sym.st_other);
}

void
lib::dwfl::DwflModule::getSymbolByName(jstring name,
				     lib::dwfl::SymbolBuilder* symbolBuilder)
{
  int nameLength = JvGetStringUTFLength(name);
  char rawName[nameLength + 1];
  JvGetStringUTFRegion(name, 0, name->length(), rawName);
  rawName[nameLength] = 0;
  int numSymbols = dwfl_module_getsymtab(DWFL_MODULE_POINTER);
  for (int i = 0; i < numSymbols; i++)
    {
      GElf_Sym sym;
      const char *symName = dwfl_module_getsym(DWFL_MODULE_POINTER, i, &sym, 0);
      if (!::strcmp(rawName, symName))
	symbolBuilder->symbol(JvNewStringUTF(symName),
			      sym.st_value,
			      sym.st_size,
			      ELF64_ST_TYPE(sym.st_info),
			      ELF64_ST_BIND(sym.st_info),
			      sym.st_other);
    }
}

void
lib::dwfl::DwflModule::setUserData(jobject data)
{
  void **userdata = NULL;
  dwfl_module_info(DWFL_MODULE_POINTER, &userdata, NULL, NULL, NULL, NULL, NULL,
                   NULL);
                   
   *userdata = data;
  
}