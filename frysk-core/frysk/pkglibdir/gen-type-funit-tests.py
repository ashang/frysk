#!/usr/bin/python
import os,posix,sys
from subprocess import *
from os.path import *

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
        self.c_file.write("static " + type + " " + var + var_suffix + type_arr)
        self.c_file.write(" unused")
        if initial != "":
            self.write(" = " + initial)
        self.c_file.write(";\n")
        self.c_file.write("// Name: " + var + var_suffix + "\n")
        self.c_file.write("// Value: " + initial + "\n")
        self.c_file.write("// Type: ")
        type += type_arr
        self.c_file.write(type.replace("\n", "\n// Type: "))
        self.c_file.write("\n")
    def write(self,str):
        self.c_file.write(str)
    def add_decl(self,str):
        self.c_file.write("static " + str)
    def prologue(self,):
        self.c_file.write('''// Generated by gen-typetests.py

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
    def epilogue(self,debug):
        self.c_file.write("\n")
        if (debug):
            self.c_file.write("volatile int x = 1;\n")
        self.c_file.write("int\n")
        self.c_file.write("main (int argc, char **argv) {\n")
        if (debug):
            self.c_file.write("while (x);\n")
        self.c_file.write("    crash ();\n    return 0;\n}\n")


########################################################################
# main
########################################################################

def usage ():
    print "Usage " + sys.argv[0] + " -debug"
    sys.exit(1)

debug=0
for t in sys.argv:
    if (t == "-debug"):
        debug=1
    elif (t.startswith("-")):
        usage()

# base types we generate variables for.  used to index into limits map
base_types=('char','short int','int','long int','long long int','float','double')
# Used for variable initialization
limits={'char' : {'min' : '41', 'max' : '176'},
        'short int' : {'min' : '-32767', 'max' : '32767'},
        'int' : {'min' : '-2147483647', 'max' : '2147483647'},
        'long int' : {'min' : '-2147483647L', 'max' : '2147483647L'},
        'long long int' : {'min' : '-9223372036854775807LL', 
                           'max' :  '9223372036854775807LL'},
        'float' : {'min' : '1.175494E-38', 'max' : '3.402823E+38'},
        'double' : {'min' : '2.225074E-308', 'max' : '1.797693E+308'}
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
c_file.write("// Scalar ########################################\n")
c_file.write("// Test: Scalar\n")
for t in base_types:
    try:
        c_file.write("// %s\n" % t);
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

c_file.write("// Array ########################################\n")
c_file.write("// Test: Array\n")
for t in base_types:
    c_file.write("// array of %s\n" % (t))
    ts = t.replace(" ","_")
    min = limits[t]['min']
    max = limits[t]['max']
    c_file.add(t, "arr_%s" % ts, "{%s,%s}" % (min,max), " [2]")
    c_file.add(t, "arr_arr_%s" % ts, "{{%s,%s},{%s,%s}}" % (min,max,min,max), " [2][2]")
    c_file.add(t, "arr_arr_arr_%s" % ts, "{{{%s,%s},{%s,%s}},{{%s,%s},{%s,%s}}}" % (min,max,min,max,min,max,min,max), " [2][2][2]")
    c_file.add("%s *" % t, "arr_ptr_arr_arr_%s" % ts, "{arr_arr_%s[0],arr_arr_%s[1]}" % (ts,ts), " [2]")
    c_file.add("%s (*" % t, "ptr_arr_%s" % ts, "&arr_%s" % ts, ")[2]")

c_file.write("\nstatic int one = 1, two = 2, three = 3, four = 4;\n")

c_file.add("struct {\n  int int_var;\n}", "arr_struct", "{{1},{2}}", " [2]")
c_file.add("union {\n  int int_var;\n  float fl;\n}", "arr_union", "{{1},{2}}", " [2]")
c_file.add("struct {\n  int int_var;\n}", "arr_arr_struct", "{{{1},{2}},{{3},{4}}}", " [2][2]")
c_file.add("union {\n  int int_var;\n  float float_var;\n}", "arr_arr_union", "{{{1},{2}},{{3},{4}}}", " [2][2]")
c_file.add("int *", "arr_arr_ptr", "{{&one,&two},{&three,&four}}", " [2][2]")
c_file.add("struct {\n  int arr_int[2];\n}", "arr_struct_arr_int", "{{{1, 2}}, {{3, 4}}}", " [2]")
c_file.add("struct {\n  struct {\n    int int_var;\n  } struct_a;\n}", "arr_struct_struct", "{{{1}},{{2}}}", " [2]")
c_file.add("struct {\n  union {\n    int int_var;\n    float float_var;\n  } struct_a;\n}", "arr_struct_union", "{{{1}},{{2}}}", " [2]")
c_file.add("struct {\n  int * ptr;\n}", "arr_struct_ptr", "{{&one},{&two}}", " [2]")
c_file.add("union {\n  int arr_int[2];\n  float arr_float[2];\n}", "arr_union_arr_int", "{{{1, 2}}, {{3, 4}}}", " [2]")
c_file.add("union {\n  struct {\n    int int_var;\n  } struct_a;\n}", "arr_union_struct", "{{{1}}, {{2}}}", " [2]")
c_file.add("union {\n  union {\n    int int_var;\n  } union_a;\n}", "arr_union_union", "{{{1}}, {{2}}}", " [2]")
c_file.add("union {\n  int * ptr;\n}", "arr_union_ptr", "{{&one}, {&two}}", " [2]", )
# ??? fails
# c_file.add("int (*", "arr_ptr_arr", "{&arr_int, &arr_int}", " [2])[2]")
c_file.add("struct {\n  int int_var;\n} *", "arr_ptr_struct", "", " [2]")
c_file.add("union {\n  int int_var;\n  float float_var;\n} *", "arr_ptr_union", "", " [2]")
c_file.add("int * *", "arr_ptr_ptr", "", " [2]")
c_file.add("struct {\n  int int_var;\n} (*", "ptr_arr_struct", "", ")[2]")
c_file.add("union {\n  int int_var;\n} (*", "ptr_arr_union", "", ")[2]")
c_file.add("int * (*", "ptr_arr_ptr", "", ")[2]")

c_file.write("// Struct ########################################\n")
c_file.write("// Test: Struct\n")
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
c_file.add('''struct {\n  unsigned int bit1_0:1;\n  unsigned int bit1_1:1;\n  char char_2;\n  unsigned int bit1_6:1;\n  unsigned int bit1_7:1;\n  char char_8;\n  unsigned int bit1_9:1;\n  unsigned int bit1_10:1;\n}''', "bitfields_small", "{1, 0, 0x7f, 1, 0, 0x7f, 1, 0}")
c_file.add('''struct {\n  unsigned char char_0;\n  int bit1_4:1;\n  unsigned int bit1_5:1;\n  int bit2_6:2;\n  unsigned int bit2_8:2;\n  int bit3_10:3;\n  unsigned int bit3_13:3;\n  int bit9_16:9;\n  unsigned int bit9_25:9;\n  char char_34;\n}''', "bitfields_bit", "{0x7f, 1, 1, 1, 3, 3, 7, UINT8_MAX, 511, 0x7f}")
c_file.add('''struct {\n  short int arr_short[2];\n}''', "struct_arr_short", "{{1, 2}}")
c_file.add('''struct {\n  struct {\n    int int_var;\n  } struct_a;\n}''', "struct_struct", "{{1}}")
c_file.add("struct {\n  union {\n    int int_var;\n  } union_a;\n}", "struct_union", "{{1}}")
c_file.add('''struct {\n  int * ptr_int;\n}''', "struct_ptr", "{&one}")
c_file.add('''union {\n  int arr_int[4];\n  float arr_float[4];\n}''', "union_arr", "{{1, 2, 3, 4}}")
c_file.add('''union {\n  struct {\n    int int_var;\n  } struct_a;\n}''', "union_struct", "{{1}}")
c_file.add('''union {\n  union {\n    int int_var;\n    float float_var;\n  } union_a;\n}''', "union_union", "{{1}}")
c_file.add('''union {\n  int * ptr_int;\n}''', "union_ptr", "{&one}")
c_file.add('''struct {\n  int int_var;\n} *''', "ptr_struct", "")
c_file.add('''union {\n  int int_var;\n} *''', "ptr_union", "")
c_file.add('''struct {\n  int arr_arr_int[2][2];\n}''', "struct_arr_arr_int", "{{{1, 2}, {3, 4}}}")
# ??? improve indentation
c_file.add('''struct {\n  struct {\n  int int_var;\n} arr_struct[2];\n}''', "struct_arr_struct", "{{{1}, {2}}}")
c_file.add('''struct {\n  union {\n  int int_var;\n} arr_union[4];\n}''', "struct_arr_union", "{{{1}, {2}}}")
c_file.add('''struct {\n  int * arr_ptr[2];\n}''', "struct_arr_ptr", "{{&one, &two}}")
c_file.add('''struct {\n  struct {\n    int arr_int[2];\n  } struct_arr;\n}''', "struct_struct_arr_int", "{{{1, 2}}}")
c_file.add('''struct {\n  struct {\n    struct {\n      int int_var;\n    } struct_a;\n  } struct_struct;\n}''', "struct_struct_struct", "{{{1}}}")
c_file.add('''struct {\n  struct {\n    union {\n      char int_var;\n    } union_a;\n  } struct_union;\n}''', "struct_struct_union", "{{{1}}}")
c_file.add('''struct {\n  struct {\n    int * ptr_int;\n  } sp;\n}''', "struct_struct_ptr", "{{&three}}")
c_file.add('''struct {\n  union {\n    int arr_int[4];\n  } union_arr_int;\n}''', "struct_union_arr",  "{{{1, 2, 3, 4}}}")
c_file.add('''struct {\n  union {\n    struct {\n      int int_var;\n    } struct_a;\n  } union_struct;\n}''', "struct_union_struct", "{{{1}}}")
c_file.add('''struct {\n  union {\n    union {\n      long int int_var;\n      float float_var;\n    } union_a;\n  } union_union;\n}''', "struct_union_union", "{{{1.0}}}")
c_file.add('''struct {\n  union {\n    int * ptr_int;\n  } union_ptr;\n}''', "struct_union_ptr", "{{&four}}")
c_file.add('''struct {\n  int (* ptr_arr)[4];\n}''', "struct_ptr_arr", "")
# ??? improve indentation
c_file.add('''struct {\n  struct {\n  int int_var;\n} * ptr_struct;\n}''', "struct_ptr_struct", "")
c_file.add('''struct {\n  union {\n  int int_var;\n} * ptr_union;\n}''', "struct_ptr_union", "")
c_file.add('''struct {\n  int * * ptr_ptr;\n}''', "struct_ptr_ptr", "")
c_file.add('''union {\n  int arr_int[2][2];\n  float arr_float[2][2];\n}''', "union_arr_int", "{{{1, 2}, {3, 4}}}")
# ??? improve indentation
c_file.add('''union {\n  struct {\n  int int_var;\n} arr_struct[2];\n}''', "union_arr_struct", "{{{1}}}")
c_file.add('''union {\n  union {\n  int int_var;\n  float float_var;\n} arr_union[2];\n}''', "union_arr_union", "{{{1.1}}}")
c_file.add('''union {\n  int * arr_ptr[4];\n}''', "union_arr_ptr", "{{&one}}")
c_file.add('''union {\n  struct {\n    int arr_int[4];\n  } struct_arr;\n}''', "union_struct_arr_int", "{{{1, 2, 3, 4}}}")
c_file.add('''union {\n  struct {\n    struct {\n      int int_var;\n    } struct_a;\n  } struct_b;\n}''', "union_struct_struct", "{{{1}}}")
c_file.add('''union {\n  struct {\n    union {\n      int int_var;\n      float float_var;\n    } union_a;\n  } struct_a;\n}''', "union_struct_union", "{{{1.1}}}")
c_file.add('''union {\n  struct {\n    int * ptr_int;\n  } struct_ptr;\n}''', "union_struct_ptr", "{{&one}}")
c_file.add('''union {\n  union {\n    long long int arr_int[4];\n  } union_arr;\n}''', "union_union_arr_int", "{{{1, 2, 3, 4}}}")
c_file.add('''union {\n  union {\n    struct {\n      long long int int_var;\n    } struct_a;\n  } union_a;\n}''', "union_union_struct", "{{{1}}}")
c_file.add('''union {\n  union {\n    union {\n      int int_var;\n      float float_var;\n    } union_a;\n  } union_b;\n}''', "union_union_union", "{{{1.1}}}")
c_file.add('''union {\n  union {\n    int * ptr_int;\n    float * ptr_float;\n  } union_ptr;\n}''', "union_union_ptr", "{{&one}}")
c_file.add('''union {\n  int (* ptr_arr)[4];\n}''', "union_ptr_arr", "")
c_file.add('''union {\n  struct {\n  int int_var;\n} * ptr_struct;\n}''', "union_ptr_struct", "")
c_file.add('''union {\n  union {\n  int int_var;\n} * ptr_union;\n}''', "union_ptr_union", "")
c_file.add('''union {\n  int * * ptr_ptr;\n}''', "union_ptr_ptr", "")
c_file.add('''struct {\n  int arr_int[4];\n} *''', "ptr_struct_arr_int", "")
c_file.add('''struct {\n  struct {\n    int int_var;\n  } struct_a;\n} *''', "Ptr_struct_struct", "")
c_file.add('''struct {\n  union {\n    int int_var;\n  } union_a;\n} *''', "ptr_struct_union", "")
c_file.add('''struct {\n  int * ptr_int;\n} *''', "ptr_struct_ptr", "")
c_file.add('''union {\n  int arr_int[4];\n} *''',"ptr_union_arr_int", "")
c_file.add('''union {\n  struct {\n    int int_var;\n  } struct_a;\n} *''',"ptr_union_struct", "")
c_file.add('''union {\n  union {\n    int int_var;\n  } union_a;\n} *''', "ptr_union_union", "")
c_file.add('''union {\n  int * ptr_int;\n} *''', "ptr_union_ptr", "")
c_file.add('''struct {\n  int int_var;\n} * *''', "ptr_ptr_struct", "")
c_file.add('''union {\n  int int_var;\n} * *''', "ptr_ptr_union", "")

c_file.write("// Enum ########################################\n")
c_file.write("// Test: Enum\n")
c_file.add('''enum  {\n  red = 0,\n  green = 1,\n  blue = 2\n}''', "primary_colors", "")
c_file.add('''enum colors {\n  orange = 0,\n  yellow = 1,\n  violet = 2,\n  indigo = 3\n}''', "rainbow_colors", "")
c_file.add('''enum  {\n  chevy = 33,\n  dodge = 44,\n  ford = 55\n}''', "usa_cars", "")
c_file.add('''enum cars {\n  bmw = 0,\n  mercedes = 1,\n  porsche = 2\n}''', "sports_cars", "")

c_file.epilogue(debug)
