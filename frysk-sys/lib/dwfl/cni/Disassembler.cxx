// This file is part of the program FRYSK.
//
// Copyright 2008 Red Hat Inc.
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

#include <stdlib.h>
#define __STDC_FORMAT_MACROS
#include <inttypes.h>

#include <cstring>
#include <gcj/cni.h>

#include <libdwfl.h>
#include <libasm.h>

#include <java/util/LinkedList.h>

#include "lib/dwfl/Elf.h"
#include "lib/dwfl/ElfException.h"
#include "lib/dwfl/Disassembler.h"
#include "lib/dwfl/DwflModule.h"
#include "lib/dwfl/Instruction.h"
#include "lib/dwfl/ModuleElfBias.h"
#include "frysk/sys/cni/Errno.hxx"

using namespace std;

// object passed into the disassembler output callback function, used
// for tracking the current address, pointers into the buffer that's
// being disassembled, and for accumulating the results of the
// disassembly.

struct disasm_info
{
  GElf_Addr addr;
  const uint8_t *cur;
  const uint8_t *last_end;
  java::util::LinkedList *list;
};

namespace
{
  extern "C" int
  disasm_output (char *buf, size_t buflen, void *arg)
  {
    struct disasm_info *info = (struct disasm_info *) arg;
    jstring instructionString = JvNewStringLatin1(buf, (jsize)buflen);
    jint length = info->cur - info->last_end;
    lib::dwfl::Instruction* inst
      = new lib::dwfl::Instruction((jlong)info->addr, length, instructionString);
    info->addr += length;
    info->last_end = info->cur;
    info->list->add(inst);
    return 0;
  }

// Look up a symbol when requested by the disassembler. Unfortunately
// the disassembler doesn't seem to use this as much as it could.
  extern "C" int symCallback(GElf_Addr, Elf32_Word, GElf_Addr addr, char **buf,
				 size_t *len, void *symcbarg)
  {
    lib::dwfl::Disassembler* disasm
      = reinterpret_cast<lib::dwfl::Disassembler*>(symcbarg);
    Dwfl_Module* module = (Dwfl_Module*)disasm->module->getPointer();
    GElf_Sym closest_sym;
    const char* symName = dwfl_module_addrsym(module, (Dwarf_Addr)addr,
                                              &closest_sym, 0);
    if (symName)
      {
        uint64_t offset = addr - closest_sym.st_value;
        int bufSize;
        if (offset == 0)
          bufSize = strlen(symName) + 1;
        else
          bufSize = strlen(symName) + 18; // '+' plus hex digits + '\0'
        if (!*buf)
          *buf = (char*)malloc(bufSize); // for '\0'
        else
          *buf = (char*)realloc(*buf, bufSize);
        if (!*buf)
            return -1;
        if (offset == 0)
          {
            memcpy(*buf, symName, bufSize);
            *len = bufSize - 1;
          }
        else
          {
            *len = snprintf(*buf, bufSize, "%s+%" PRIx64, symName, offset);
          }
        return 0;
      }
    else
      {
        return -1;
      }
  }
}

java::util::LinkedList*
lib::dwfl::Disassembler::disassemble_instructions(ModuleElfBias* bias,
                                                  jlong startAddress,
                                                  jbyteArray bytes)
{
  ::Elf *elf = (::Elf *)bias->elf->getPointer();
  ::Ebl *ebl = ebl_openbackend(elf);
  DisasmCtx_t *ctx = disasm_begin (ebl, elf, symCallback);
  if (ctx == NULL)
      throw new lib::dwfl::ElfException(JvNewStringUTF("no disassember available"));

  struct disasm_info info;
  info.addr = startAddress;
  info.last_end = info.cur = reinterpret_cast<uint8_t*>(elements(bytes));
  info.list = new java::util::LinkedList;

  // Magic format string copied from elfutils/src/objdump.c
  disasm_cb(ctx, &info.cur, info.cur + JvGetArrayLength(bytes), info.addr,
            "%7m %.1o,%.2o,%.3o%34a %l", disasm_output, &info,
            this);
  (void)disasm_end(ctx);
  ebl_closebackend(ebl);
  return info.list;
}
