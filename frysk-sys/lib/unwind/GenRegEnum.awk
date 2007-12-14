# This file is part of the program FRYSK.
#
# Copyright (C) 2006-2007 IBM
#
# Contributed by
# Jose Flavio Aguilar Paulino <jflavio@br.ibm.com> <joseflavio@gmail.com>
#
# FRYSK is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License as published by
# the Free Software Foundation; version 2 of the License.
#
# FRYSK is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with FRYSK; if not, write to the Free Software Foundation,
# Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
# 
# In addition, as a special exception, Red Hat, Inc. gives You the
# additional right to link the code of FRYSK with code not covered
# under the GNU General Public License ("Non-GPL Code") and to
# distribute linked combinations including the two, subject to the
# limitations in this paragraph. Non-GPL Code permitted under this
# exception must only link to the code of FRYSK through those well
# defined interfaces identified in the file named EXCEPTION found in
# the source code files (the "Approved Interfaces"). The files of
# Non-GPL Code may instantiate templates or use macros or inline
# functions from the Approved Interfaces without causing the
# resulting work to be covered by the GNU General Public
# License. Only Red Hat, Inc. may make changes or additions to the
# list of Approved Interfaces. You must obey the GNU General Public
# License in all respects for all of the FRYSK code and other code
# used in conjunction with FRYSK except the Non-GPL Code covered by
# this exception. If you modify this file, you may extend this
# exception to your version of the file, but you are not obligated to
# do so. If you do not wish to provide this exception without
# modification, you must delete this exception statement from your
# version and license this file solely under the GPL without
# exception.

BEGIN {
	if ( ARGC != 3 ) {
		print "This script ('gen_reg_list.awk') requires 2 command line args, not " (ARGC-1);
		print "The first the arch prefix, ex: PPC64 or X86;";
		print "The second the registers unwind prefix, ex: UNW.";
		print "To be used to compose the registers DEFINE such as: PPC32_UNW_XXXXX";
		exit 1;
	}
	arch_prefix = ARGV[1];
	unwind_reg_prefix = ARGV[2];
	#cleaning the argv (to avoid it to be read as files by awk)
	ARGV[1] = "";
	ARGV[2] = "";
	reg_prefix = unwind_reg_prefix "_" arch_prefix "_";
	reg_num = 0;
	error_parsing = 0;
	all_reg_num_list = ""; 
}

#Main program block
#for each line input
{
        #Only lines started with UNW_PPC64 will be processed
	line_begin_regexp = "^[ \\t]*" reg_prefix;
        if($0 ~ line_begin_regexp) {

	   valid_line = 0;
	   second_field_start = "";
	   	
	   if(NF > 1) {
	      second_field_start = substr($2, 1);
	      if(second_field_start == "=") {
                 if($3 != "") {
	            reg_equals_start = substr($3, 4);
                    if(reg_equals_start != "UNW_") {
                       valid_line = 1;
		       gsub(",","",$3);
                       reg_num = $3;
	            }
	            else {
		       #for this kind of line:
		       #UNW_TDEP_LAST_REG = UNW_PPC64_NIP,
		       #thats not a real valid line for creating a Java Reg
                       valid_line = 0;
                    }
	         }
	         else {
	            error_parsing = 1;
	         }
	      }
	      else
	      if( (second_field_start == "//") || 
	 	  (second_field_start == "/*") ) {
	         valid_line = 1;
	      }
	      else {
	         error_parsing = 1;
	      }
	   }
	   else {
	      valid_line = 1;
	   }
	   
	   if(error_parsing == 0) {
	      if(valid_line == 1) {
		  
		 #Check if the current register number
		 #has been assigned to other registers
		 regnum_exp = "reg" reg_num;
		 if(all_reg_num_list ~ regnum_exp) {
		    print "ERROR, THERE ARE TWO REGISTERS WITH THE SAME NUMBER: " reg_num " (" $1 ")";
		    error_parsing = 1;
		 }
		 else {
		    gsub(",","",$1)
                    gsub(reg_prefix, "", $1)
	            printf "%s %s\n", $1, reg_num;
		    #Put the printed REG number in a string (a list)
		    all_reg_num_list = all_reg_num_list " reg" reg_num;
	            reg_num++;
		 }
	      }
	   }
	} #UNW_PPC64 lines
    }
    
    END {
       if(error_parsing == 1) {
          print "ERROR, COULD NOT CONVERT libunwind HEADER to java\n";
	  exit 1;
       }
    }
