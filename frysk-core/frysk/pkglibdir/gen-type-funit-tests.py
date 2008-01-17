#!/usr/bin/python
# This file is part of the program FRYSK.
#
# Copyright 2007, 2008, Red Hat Inc.
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

import sys

########################################################################
# Manage creation of the C file
########################################################################

class c:
    def open(self):
        self.c_file = sys.stdout
    def add(self, type, var, initial, type_arr=""):
        if (type_arr == ""):
            var_suffix = "_var"
        else:
            var_suffix = ""
        if (type.endswith("*")):
            self.c_file.write(type + var + var_suffix + type_arr)
        else:
            self.c_file.write(type + " " + var + var_suffix + type_arr)
        self.c_file.write(" unused")
        if initial != "":
            self.write(" = " + initial)
        self.c_file.write(";\n")
        self.c_file.write("// Name: " + var + var_suffix + "\n")
        # Escape quotes and new lines, remove struct member "."
        self.c_file.write("// Value: " + initial.replace('"','\\"').replace('\n',"\n// Value: ").replace('{.','{').replace(' .',' ') + "\n")
        self.c_file.write("// Type: ")
        type += type_arr
        self.c_file.write(type.replace("\n", "\n// Type: "))
        self.c_file.write("\n")
    def write(self,str):
        self.c_file.write(str)
    def add_decl(self,str):
        self.c_file.write("static " + str)
    def prologue(self,):
        self.c_file.write('''// Generated by gen-type-funit-tests.py

// This file is part of the program FRYSK.
//
// Copyright 2007, 2008 Red Hat Inc.
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

#include <stdint.h>
#include <values.h>
#define unused __attribute__((unused))

static void
crash () {
    char *a = 0;
    a[0] = 0;
}

// Naming convention examples:
// ptr_const_char_var - Pointer to a const char
// char_ptr_const_var - Const pointer to a char
// arr_ptr_arr_arr_char - Array of pointers to 2 dimension arrays of char
// struct_arr_arr_int_var - Struct of 2 dimension arrays of int

''')
    def epilogue(self):
        self.c_file.write("\n")
        self.c_file.write("int\n")
        self.c_file.write("main (int argc, char **argv) {\n")
        self.c_file.write("    crash ();\n    return 0;\n}\n")


########################################################################
# main
########################################################################

# base types we generate variables for.  used to index into limits map
base_types=('char','short int','int','long int','long long int','float','double')
# Used for variable initialization
limits={'char' : {'min' : "'!'", 'max' : "'~'"},
        'short int' : {'min' : '-32767', 'max' : '32767'},
        'int' : {'min' : '-65536', 'max' : '65536'},
        'long int' : {'min' : '-65536', 'max' : '65536'},
        'long long int' : {'min' : '-65536', 
                           'max' :  '65536'},
        'float' : {'min' : '1.1754939E-38', 'max' : '3.402823E38'},
        'double' : {'min' : '2.225074E-308', 'max' : '1.797693E308'}
}
type_modifiers=('const','volatile')

c_file = c()
c_file.open()
c_file.prologue()

tm = te = tme = tms = ""
for m in type_modifiers:
    tm = tm + m + " "
    te = " " + te + m
# gcc puts multiple types in the opposite order of source so ignore these
tm = tm.rstrip()
te = te.lstrip()
tms = tm.rstrip().replace(" ","_")
for t in base_types:
    try:
        ts = t.replace(" ","_")
        min = limits[t]['min']
        max = limits[t]['max']
        c_file.add(t, ts, max)
        for m in type_modifiers:
            c_file.add("%s %s" % (m,t), "%s_%s" % (m,ts), min)
#        c_file.addexpected("%s %s" % (tm,t), "%s %s" % (te,t), "%s_%s" % (tms,ts), "", max)
        c_file.add("%s *" % t, "ptr_" + ts, "&%s_var" % ts)
        for m in type_modifiers:
            c_file.add("%s %s *" % (m,t), "ptr_%s_%s" % (m,ts),
                       "&%s_var" % ts)
            c_file.add("%s * %s" % (t,m), "%s_ptr_%s" % (ts,m),
                       "&%s_var" % ts)
#       c_file.add("%s %s *" % (tm,t), "ptr_%s_%s" % (tms,ts),
#                  "&%s_var" % ts)
#       c_file.add("%s * %s" % (t,tm), "%s_ptr_%s" % (ts,tms),
#                  "&%s_var" % ts)
#        if (t != "float" and t != "double"):
#            c_file.addexpected("unsigned %s" % t, "unsigned %s" % t, "unsigned_%s" % ts, "", max)
    except KeyError:
        continue

for t in base_types:
    ts = t.replace(" ","_")
    min = limits[t]['min']
    max = limits[t]['max']
    if (t == "char"):
        char1 = min.strip("'")
        char2 = max.strip("'")
        c_file.add(t, "arr_%s" % ts, '"%s%s"' % (char1,char2), " [2]")
        c_file.add(t, "arr_arr_%s" % ts, '{{"%s%s"},{"%s%s"}}' % (char1,char2,char1,char2), " [2][2]")
        c_file.add(t, "arr_arr_arr_%s" % ts, '{{{"%s%s"},{"%s%s"}},{{"%s%s"},{"%s%s"}}}' % (char1,char2,char1,char2,char1,char2,char1,char2), " [2][2][2]")
        c_file.add("%s *" % t, "arr_ptr_arr_arr_%s" % ts, "{arr_arr_%s[0],arr_arr_%s[1]}" % (ts,ts), " [2]")
        c_file.add("%s (*" % t, "ptr_arr_%s" % ts, "&arr_%s" % ts, ")[2]")
    else:
        c_file.add(t, "arr_%s" % ts, "{%s,%s}" % (min,max), " [2]")
        c_file.add(t, "arr_arr_%s" % ts, "{{%s,%s},{%s,%s}}" % (min,max,min,max), " [2][2]")
        c_file.add(t, "arr_arr_arr_%s" % ts, "{{{%s,%s},{%s,%s}},{{%s,%s},{%s,%s}}}" % (min,max,min,max,min,max,min,max), " [2][2][2]")
        c_file.add(t, "arr_arr_arr_arr_%s" % ts, 
                   "{{{{%s,%s},{%s,%s},{%s,%s}},{{%s,%s},{%s,%s},{%s,%s}}},{{{%s,%s},{%s,%s},{%s,%s}},{{%s,%s},{%s,%s},{%s,%s}}},{{{%s,%s},{%s,%s},{%s,%s}},{{%s,%s},{%s,%s},{%s,%s}}}}" % (min,max,min,max,min,max,min,max,min,max,min,max,min,max,min,max,min,max,min,max,min,max,min,max,min,max,min,max,min,max,min,max,min,max,min,max), " [3][2][3][2]")
        c_file.add("%s *" % t, "arr_ptr_arr_arr_%s" % ts, "{arr_arr_%s[0],arr_arr_%s[1]}" % (ts,ts), " [2]")
        c_file.add("%s (*" % t, "ptr_arr_%s" % ts, "&arr_%s" % ts, ")[2]")

c_file.write("\nstatic int one = 1, two = 2, three = 3, four = 4;\n")

c_file.add('''struct {
  int int_var;
}''', "arr_struct", 
'''{{
  .int_var=1,
},{
  .int_var=2,
}}''', " [2]")
c_file.add(
'''union {
  int int_var;
  float fl;
}''', "arr_union", 
'''{{
  .int_var=1,
  .fl=1.4012985E-45,
},{
  .int_var=1073741824,
  .fl=2.0,
}}''', " [2]")
c_file.add('''struct {
  int int_var;
}''', "arr_arr_struct", '''{{{
  .int_var=1,
},{
  .int_var=2,
}},{{
  .int_var=3,
},{
  .int_var=4,
}}}''', " [2][2]")
c_file.add('''union {
  int int_var;
  float float_var;
}''', "arr_arr_union", '''{{{
  .int_var=1,
  .float_var=1.4012985E-45,
},{
  .int_var=2,
  .float_var=2.802597E-45,
}},{{
  .int_var=3,
  .float_var=4.2038954E-45,
},{
  .int_var=4,
  .float_var=5.605194E-45,
}}}''', " [2][2]")
c_file.add("int *", "arr_arr_ptr", "{{&one,&two},{&three,&four}}", " [2][2]")
c_file.add('''struct {
  int arr_int[2];
}''', "arr_struct_arr_int", '''{{
  .arr_int={1,2},
},{
  .arr_int={3,4},
}}''', " [2]")
c_file.add('''struct {
  struct {
    int int_var;
  } struct_a;
}''', "arr_struct_struct", '''{{
  .struct_a={
    .int_var=1,
  },
},{
  .struct_a={
    .int_var=2,
  },
}}''', " [2]")
c_file.add('''struct {
  union {
    int int_var;
    float float_var;
  } struct_a;
}''', "arr_struct_union", '''{{
  .struct_a={
    .int_var=1,
    .float_var=1.4012985E-45,
  },
},{
  .struct_a={
    .int_var=2,
    .float_var=2.802597E-45,
  },
}}''', " [2]")
c_file.add("struct {\n  int *ptr;\n}", "arr_struct_ptr", "{{&one},{&two}}", " [2]")
c_file.add('''union {
  int arr_int[2];
  float arr_float[2];
}''', "arr_union_arr_int", '''{{
  .arr_int={1,2},
  .arr_float={1.4012985E-45,2.802597E-45},
},{
  .arr_int={3,4},
  .arr_float={4.2038954E-45,5.605194E-45},
}}''', " [2]")
c_file.add('''union {
  struct {
    int int_var;
  } struct_a;
}''', "arr_union_struct", '''{{
  .struct_a={
    .int_var=1,
  },
},{
  .struct_a={
    .int_var=2,
  },
}}''', " [2]")
c_file.add('''union {
  union {
    int int_var;
  } union_a;
}''', "arr_union_union", '''{{
  .union_a={
    .int_var=1,
  },
},{
  .union_a={
    .int_var=2,
  },
}}''', " [2]")
c_file.add("union {\n  int *ptr;\n}", "arr_union_ptr", "{{&one}, {&two}}", " [2]", )
# ??? fails
# c_file.add("int (*", "arr_ptr_arr", "{&arr_int, &arr_int}", " [2])[2]")
c_file.add("struct {\n  int int_var;\n} *", "arr_ptr_struct", "", " [2]")
c_file.add("union {\n  int int_var;\n  float float_var;\n} *", "arr_ptr_union", "", " [2]")
c_file.add("int **", "arr_ptr_ptr", "", " [2]")
c_file.add("struct {\n  int int_var;\n} (*", "ptr_arr_struct", "", ")[2]")
c_file.add("union {\n  int int_var;\n} (*", "ptr_arr_union", "", ")[2]")
c_file.add("int * (*", "ptr_arr_ptr", "", ")[2]")

c_file.write('''typedef struct {
    char char_var;
    short short_var;
    int int_var;
    long long_var;
    float float_var;
    double double_var;
    char arr_char[4];
} type_struct;
typedef struct {
    type_struct type_struct_min;
    type_struct type_struct_max;
} type_type_struct;
type_type_struct type_minmax_struct =
  {{%s, %s, %s, %s, %s, %s, %s},
   {%s, %s, %s, %s, %s, %s, %s}
};\n''' % (limits['char']['min'],limits['short int']['min'],limits['int']['min'],limits['long int']['min'],limits['float']['min'],limits['double']['min'],'"ABC"',limits['char']['max'],limits['short int']['max'],limits['int']['max'],limits['long int']['max'],limits['float']['max'],limits['double']['max'],'"XYZ"'))
c_file.add(
'''struct {
  unsigned int bit1_0:1;
  unsigned int bit1_1:1;
  char char_2;
  unsigned int bit1_6:1;
  unsigned int bit1_7:1;
  char char_8;
  unsigned int bit1_9:1;
  unsigned int bit1_10:1;
}''', "bitfields_small", '''{
  .bit1_0=1,
  .bit1_1=0,
  .char_2='a',
  .bit1_6=1,
  .bit1_7=0,
  .char_8='z',
  .bit1_9=1,
  .bit1_10=0,
}''')
c_file.add('''struct {
  unsigned char char_0;
  int bit1_4:1;
  unsigned int bit1_5:1;
  int bit2_6:2;
  unsigned int bit2_8:2;
  int bit3_10:3;
  unsigned int bit3_13:3;
  int bit9_16:9;
  unsigned int bit9_25:9;
  char char_34;
}''', "bitfields_bit", '''{
  .char_0='A',
  .bit1_4=-1,
  .bit1_5=1,
  .bit2_6=1,
  .bit2_8=3,
  .bit3_10=3,
  .bit3_13=7,
  .bit9_16=255,
  .bit9_25=511,
  .char_34='Z',
}''')
c_file.add('''struct {
  short int arr_short[2];
}''', "struct_arr_short", '''{
  .arr_short={1,2},
}''')
c_file.add('''struct {
  struct {
    int int_var;
  } struct_a;
}''', "struct_struct", '''{
  .struct_a={
    .int_var=1,
  },
}''')
# HERE HERE -------------------------------------------------------
c_file.add('''struct {
  union {
    int int_var;
  } union_a;
}''', "struct_union", '''{
  .union_a={
    .int_var=1,
  },
}''')
c_file.add('''struct {
  int *ptr_int;
}''', "struct_ptr", "{&one}")
c_file.add('''union {
  int arr_int[4];
  float arr_float[4];
}''', "union_arr", '''{
  .arr_int={1,2,3,4},
  .arr_float={1.4012985E-45,2.802597E-45,4.2038954E-45,5.605194E-45},
}''')
c_file.add('''union {
  struct {
    int int_var;
  } struct_a;
}''', "union_struct", '''{
  .struct_a={
    .int_var=1,
  },
}''')
c_file.add('''union {
  union {
    int int_var;
    float float_var;
  } union_a;
}''', "union_union", '''{
  .union_a={
    .int_var=1,
    .float_var=1.4012985E-45,
  },
}''')
c_file.add('''union {
  int *ptr_int;
}''', "union_ptr", "{&one}")
c_file.add('''struct {
  int int_var;
} *''', "ptr_struct", "")
c_file.add('''union {
  int int_var;
} *''', "ptr_union", "")
c_file.add('''struct {
  int arr_arr_int[2][2];
}''', "struct_arr_arr_int", '''{
  .arr_arr_int={{1,2},{3,4}},
}''')
c_file.add('''struct {
  struct {
    int int_var;
  } arr_struct[2];
}''', "struct_arr_struct", '''{
  .arr_struct={{
    .int_var=1,
  },{
    .int_var=2,
  }},
}''')
c_file.add('''struct {
  union {
    int int_var;
  } arr_union[4];
}''', "struct_arr_union", '''{
  .arr_union={{
    .int_var=1,
  },{
    .int_var=2,
  },{
    .int_var=0,
  },{
    .int_var=0,
  }},
}''')
c_file.add('''struct {
  int * arr_ptr[2];
}''', "struct_arr_ptr", "{{&one, &two}}")
c_file.add('''struct {
  struct {
    int arr_int[2];
  } struct_arr;
}''', "struct_struct_arr_int", '''{
  .struct_arr={
    .arr_int={1,2},
  },
}''')
c_file.add('''struct {
  struct {
    struct {
      int int_var;
    } struct_a;
  } struct_struct;
}''', "struct_struct_struct", '''{
  .struct_struct={
    .struct_a={
      .int_var=1,
    },
  },
}''')
c_file.add('''struct {
  struct {
    union {
      char int_var;
    } union_a;
  } struct_union;
}''', "struct_struct_union", '''{
  .struct_union={
    .union_a={
      .int_var='~',
    },
  },
}''')
c_file.add('''struct {
  struct {
    int *ptr_int;
  } sp;
}''', "struct_struct_ptr", "{{&three}}")
c_file.add('''struct {
  union {
    int arr_int[4];
  } union_arr_int;
}''', "struct_union_arr",  '''{
  .union_arr_int={
    .arr_int={1,2,3,4},
  },
}''')
c_file.add('''struct {
  union {
    struct {
      int int_var;
    } struct_a;
  } union_struct;
}''', "struct_union_struct", '''{
  .union_struct={
    .struct_a={
      .int_var=1,
    },
  },
}''')
c_file.add('''struct {
  union {
    union {
      long int int_var;
      float float_var;
    } union_a;
  } union_union;
}''', "struct_union_union", '''{
  .union_union={
    .union_a={
      .int_var=1,
      .float_var=1.4012985E-45,
    },
  },
}''')
c_file.add('''struct {
  union {
    int *ptr_int;
  } union_ptr;
}''', "struct_union_ptr", "{{&four}}")
c_file.add('''struct {
  int (*ptr_arr)[4];
}''', "struct_ptr_arr", "")
c_file.add('''struct {
  struct {
    int int_var;
  } *ptr_struct;
}''', "struct_ptr_struct", "")
c_file.add('''struct {
  union {
    int int_var;
  } *ptr_union;
}''', "struct_ptr_union", "")
c_file.add('''struct {
  int **ptr_ptr;
}''', "struct_ptr_ptr", "")
c_file.add('''union {
  int arr_int[2][2];
  float arr_float[2][2];
}''', "union_arr_int", '''{
  .arr_int={{1,2},{3,4}},
  .arr_float={{1.4012985E-45,2.802597E-45},{4.2038954E-45,5.605194E-45}},
}''')
c_file.add('''union {
  struct {
    int int_var;
  } arr_struct[2];
}''', "union_arr_struct", '''{
  .arr_struct={{
    .int_var=1,
  },{
    .int_var=0,
  }},
}''')
c_file.add('''union {
  union {
    int int_var;
    float float_var;
  } arr_union[2];
}''', "union_arr_union", '''{
  .arr_union={{
    .int_var=1,
    .float_var=1.4012985E-45,
  },{
    .int_var=0,
    .float_var=0.0,
  }},
}''')
c_file.add('''union {
  int * arr_ptr[4];
}''', "union_arr_ptr", "{{&one}}")
c_file.add('''union {
  struct {
    int arr_int[4];
  } struct_arr;
}''', "union_struct_arr_int", '''{
  .struct_arr={
    .arr_int={1,2,3,4},
  },
}''')
c_file.add('''union {
  struct {
    struct {
      int int_var;
    } struct_a;
  } struct_b;
}''', "union_struct_struct", '''{
  .struct_b={
    .struct_a={
      .int_var=1,
    },
  },
}''')
c_file.add('''union {
  struct {
    union {
      int int_var;
      float float_var;
    } union_a;
  } struct_a;
}''', "union_struct_union", '''{
  .struct_a={
    .union_a={
      .int_var=1,
      .float_var=1.4012985E-45,
    },
  },
}''')
c_file.add('''union {
  struct {
    int *ptr_int;
  } struct_ptr;
}''', "union_struct_ptr", "{{&one}}")
c_file.add('''union {
  union {
    long long int arr_int[4];
  } union_arr;
}''', "union_union_arr_int", '''{
  .union_arr={
    .arr_int={1,2,3,4},
  },
}''')
c_file.add('''union {
  union {
    struct {
      long long int int_var;
    } struct_a;
  } union_a;
}''', "union_union_struct", '''{
  .union_a={
    .struct_a={
      .int_var=1,
    },
  },
}''')
c_file.add('''union {
  union {
    union {
      int int_var;
      float float_var;
    } union_a;
  } union_b;
}''', "union_union_union", '''{
  .union_b={
    .union_a={
      .int_var=1,
      .float_var=1.4012985E-45,
    },
  },
}''')
c_file.add('''union {
  union {
    int *ptr_int;
    float *ptr_float;
  } union_ptr;
}''', "union_union_ptr", "{{&one}}")
c_file.add('''union {
  int (*ptr_arr)[4];
}''', "union_ptr_arr", "")
c_file.add('''union {
  struct {
    int int_var;
  } *ptr_struct;
}''', "union_ptr_struct", "")
c_file.add('''union {
  union {
    int int_var;
  } *ptr_union;
}''', "union_ptr_union", "")
c_file.add('''union {
  int **ptr_ptr;
}''', "union_ptr_ptr", "")
c_file.add('''struct {
  int arr_int[4];
} *''', "ptr_struct_arr_int", "")
c_file.add('''struct {
  struct {
    int int_var;
  } struct_a;
} *''', "ptr_struct_struct", "")
c_file.add('''struct {
  union {
    int int_var;
  } union_a;
} *''', "ptr_struct_union", "")
c_file.add('''struct {
  int *ptr_int;
} *''', "ptr_struct_ptr", "")
c_file.add('''union {
  int arr_int[4];
} *''',"ptr_union_arr_int", "")
c_file.add('''union {
  struct {
    int int_var;
  } struct_a;
} *''',"ptr_union_struct", "")
c_file.add('''union {
  union {
    int int_var;
  } union_a;
} *''', "ptr_union_union", "")
c_file.add('''union {
  int *ptr_int;
} *''', "ptr_union_ptr", "")
c_file.add('''struct {
  int int_var;
} **''', "ptr_ptr_struct", "")
c_file.add('''union {
  int int_var;
} **''', "ptr_ptr_union", "")

c_file.add('''enum  {\n  red = 0,\n  green = 1,\n  blue = 2\n}''', "primary_colors", "red")
c_file.add('''enum colors {\n  orange = 0,\n  yellow = 1,\n  violet = 2,\n  indigo = 3\n}''', "rainbow_colors", "orange")
c_file.add('''enum  {\n  chevy = 0,\n  dodge = 44,\n  ford = 55\n}''', "usa_cars", "chevy")
c_file.add('''enum cars {\n  bmw = 0,\n  mercedes = 1,\n  porsche = 2\n}''', "sports_cars", "bmw")

c_file.epilogue()
