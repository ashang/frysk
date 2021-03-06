// This file is part of the Utracer kernel module and it's userspace
// interfaces. 
//
// Copyright 2007, Red Hat Inc.
//
// Utracer is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// Utracer is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Utracer; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
//
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of Utracer with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of Utracer through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the Utracer code and other code
// used in conjunction with Utracer except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

#ifndef UTRACER_ERRMSGS_H
#define UTRACER_ERRMSGS_H

#ifdef __cplusplus
extern "C" {
#endif
  
const char * utrace_emsg[] = {
  "No errors.",				  // UTRACER_EBASE         
  "Invalid engine.",			  // UTRACER_EENGINE,    
  "Invalid utracing structure.",	  // UTRACER_ETRACING,   
  "Invalid utraced structure.",		  // UTRACER_ETRACED,    
  "Register out of range.",		  // UTRACER_EREG,       
  "Syscall number out of range.",	  // UTRACER_ESYSRANGE,  
  "Process not quiesced.",		  // UTRACER_ESTATE,     
  "Error reading user pages.",		  // UTRACER_EPAGES,     
  "Null mm_struct, task probably died.",  // UTRACER_EMM,        
  "Invalid regset."			  // UTRACER_EREGSET,    
  "Wait failed."			  // UTRACER_EWAIT,    
};

#ifdef __cplusplus
}
#endif  

#endif  /* UTRACER_ERRMSGS_H */
