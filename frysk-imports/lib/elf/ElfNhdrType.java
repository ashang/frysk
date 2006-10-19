//This file is part of the program FRYSK.

//Copyright 2006, IBM Inc.

//FRYSK is free software; you can redistribute it and/or modify it
//under the terms of the GNU General Public License as published by
//the Free Software Foundation; version 2 of the License.

//FRYSK is distributed in the hope that it will be useful, but
//WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//General Public License for more details.

//You should have received a copy of the GNU General Public License
//along with FRYSK; if not, write to the Free Software Foundation,
//Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.

//In addition, as a special exception, Red Hat, Inc. gives You the
//additional right to link the code of FRYSK with code not covered
//under the GNU General Public License ("Non-GPL Code") and to
//distribute linked combinations including the two, subject to the
//limitations in this paragraph. Non-GPL Code permitted under this
//exception must only link to the code of FRYSK through those well
//defined interfaces identified in the file named EXCEPTION found in
//the source code files (the "Approved Interfaces"). The files of
//Non-GPL Code may instantiate templates or use macros or inline
//functions from the Approved Interfaces without causing the
//resulting work to be covered by the GNU General Public
//License. Only Red Hat, Inc. may make changes or additions to the
//list of Approved Interfaces. You must obey the GNU General Public
//License in all respects for all of the FRYSK code and other code
//used in conjunction with FRYSK except the Non-GPL Code covered by
//this exception. If you modify this file, you may extend this
//exception to your version of the file, but you are not obligated to
//do so. If you do not wish to provide this exception without
//modification, you must delete this exception statement from your
//version and license this file solely under the GPL without
//exception.

package lib.elf;

public class ElfNhdrType
{
    public static final ElfNhdrType NT_INVALID = new ElfNhdrType(0, "NT_INVALID");
    public static final ElfNhdrType NT_PRSTATUS = new ElfNhdrType(1, "NT_PRSTATUS");
    public static final ElfNhdrType NT_FPREGSET = new ElfNhdrType(2, "NT_FPREGSET");
    public static final ElfNhdrType NT_PRPSINFO = new ElfNhdrType(3, "NT_PRPSINFO");
    public static final ElfNhdrType NT_PRXREG = new ElfNhdrType(4, "NT_PRXREG");
    
    public static final ElfNhdrType NT_TASKSTRUCT = new ElfNhdrType(4, "NT_TASKSTRUCT");
    public static final ElfNhdrType NT_PLATFORM = new ElfNhdrType(5, "NT_PLATFORM");
    public static final ElfNhdrType NT_AUXV = new ElfNhdrType(6, "NT_AUXV");
    public static final ElfNhdrType NT_GWINDOWS = new ElfNhdrType(7, "NT_GWINDOWS");
    public static final ElfNhdrType NT_ASRS = new ElfNhdrType(8, "NT_ASRS");
    
    public static final ElfNhdrType NT_PSTATUS = new ElfNhdrType(10, "NT_PSTATUS");
    public static final ElfNhdrType NT_PSINFO = new ElfNhdrType(13, "NT_PSINFO");
    public static final ElfNhdrType NT_PRCRED = new ElfNhdrType(14, "NT_PRCRED");
    
    public static final ElfNhdrType NT_UTSNAME = new ElfNhdrType(15, "NT_UTSNAME");
    public static final ElfNhdrType NT_LWPSTATUS = new ElfNhdrType(16, "NT_LWPSTATUS");
    public static final ElfNhdrType NT_LWPSINFO = new ElfNhdrType(17, "NT_LWPSINFO");
    public static final ElfNhdrType NT_PRFPXREG = new ElfNhdrType(20, "NT_PRFPXREG");
    
    private static ElfNhdrType[] types = {NT_INVALID, 
        NT_PRSTATUS, NT_FPREGSET, NT_PRPSINFO, NT_PRXREG, NT_PLATFORM,
        NT_AUXV, NT_GWINDOWS, NT_ASRS, NT_INVALID, NT_PSTATUS,
        NT_INVALID, NT_INVALID, NT_PSINFO, NT_PRCRED, NT_UTSNAME,
        NT_LWPSTATUS, NT_LWPSINFO, NT_INVALID, NT_INVALID, NT_PRFPXREG
    };
    
    private int value = 0;
    private String name = null;
    
    private ElfNhdrType(int value, String name)
    {
      this.value = value;
      this.name = name;
    }
    
    /**
     * @return true iff the object is an ElfType and equal to this object
     */
    public boolean equals(Object obj)
    {
        if(!(obj instanceof ElfNhdrType))
            return false;
        
        return ((ElfNhdrType)obj).value == this.value;
    }
    
    public int getValue()
    {
        return this.value;
    }
    
    public static ElfNhdrType intern(int type)
    {
      if ((type <= 0) || (type > (types.length - 1)))
        return NT_INVALID;
      else
        return types[type];
    }
    
    public String toString()
    {
        return this.name + "(" + this.value + ")";
    }
}
