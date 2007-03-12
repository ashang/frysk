// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

package lib.unwind;

import java.util.logging.Logger;

import gnu.gcj.RawDataManaged;
import gnu.gcj.RawData;

public abstract class Unwind
{
  volatile Logger logger = Logger.getLogger("frysk");
  
  abstract RawDataManaged initRemote(RawData addressSpace, 
                          Accessors accessors);
     
  abstract RawData createAddressSpace (ByteOrder byteOrder);
  
  abstract void destroyAddressSpace (RawData addressSpace);
  
  abstract void setCachingPolicy(RawData addressSpace, 
                                 CachingPolicy cachingPolicy);
  
  abstract int isSignalFrame (RawDataManaged cursor);
  
  abstract int step (RawDataManaged cursor);
  
  abstract ProcName getProcName(RawDataManaged cursor, int maxNameSize);
  
  abstract int getRegister(RawDataManaged cursor, int regNum,
                           RawDataManaged word);
  
  abstract RawDataManaged copyCursor(RawDataManaged cursor);
 /*
  int unw_getcontext(unw_context_t *);
  int unw_init_local(unw_cursor_t *, unw_context_t *);
  +int unw_init_remote(unw_cursor_t *, unw_addr_space_t, void *);
  int unw_step(unw_cursor_t *);
  +int unw_get_reg(unw_cursor_t *, unw_regnum_t, unw_word_t *);
  +int unw_get_fpreg(unw_cursor_t *, unw_regnum_t, unw_fpreg_t *);
  +int unw_set_reg(unw_cursor_t *, unw_regnum_t, unw_word_t);
  +int unw_set_fpreg(unw_cursor_t *, unw_regnum_t, unw_fpreg_t);
  int unw_resume(unw_cursor_t *);

  unw_addr_space_t unw_local_addr_space;
  +unw_addr_space_t unw_create_addr_space(unw_accessors_t, int);
  +void unw_destroy_addr_space(unw_addr_space_t);
  unw_accessors_t unw_get_accessors(unw_addr_space_t);
  void unw_flush_cache(unw_addr_space_t, unw_word_t, unw_word_t);
  int unw_set_caching_policy(unw_addr_space_t, unw_caching_policy_t);

  const char *unw_regname(unw_regnum_t);
  int unw_get_proc_info(unw_cursor_t *, unw_proc_info_t *);
  int unw_get_save_loc(unw_cursor_t *, int, unw_save_loc_t *);
  int unw_is_fpreg(unw_regnum_t);
  int unw_is_signal_frame(unw_cursor_t *);
  int unw_get_proc_name(unw_cursor_t *, char *, size_t, unw_word_t *);

  void _U_dyn_register(unw_dyn_info_t *);
  void _U_dyn_cancel(unw_dyn_info_t *);
 */
 abstract int getContext(RawDataManaged context);
 
}
