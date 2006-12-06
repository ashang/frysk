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

public class ElfNhdr
{
  private long namesz = 4;
  private long descsz = 0;
  private int type = ElfNhdrType.NT_INVALID.getValue();
  
  private String name = "CORE";
  private ElfNoteSectionEntry desc = null;
  
  public static abstract class ElfNoteSectionEntry
  {
    public abstract long getEntrySize();
    public abstract long fillMemRegion(byte[] buffer, long startAddress);
  }
  
  //XXX: no ElfNhdr struct in elfutils package now(2006-10-18).
  //private long pointer;
  
  public ElfNhdr()
  {

  }
  
  public String getName()
  {
    return this.name;
  }
  public long getNameSize()
  {
    return this.namesz;
  }
  public void setName(String nhdrName)
  {
    if (nhdrName == null)
      return;
    
    this.name = nhdrName;
    this.namesz = nhdrName.length();
  }
  
  public ElfNhdrType getNhdrType()
  {
    return ElfNhdrType.intern(this.type);
  }
  public ElfNoteSectionEntry getNhdrDesc()
  {
    return this.desc;
  }
  public long getDescSize()
  {
    return this.descsz;
  }
  
  public void setNhdrDesc(ElfNhdrType nhdrType, ElfNoteSectionEntry nhdrDesc)
  {
    this.type = nhdrType.getValue();
    this.desc = nhdrDesc;
    this.descsz = nhdrDesc.getEntrySize();
  }
 
  /**
   * Get the whole size of Nhdr (incluing the namesz and descsz).
   * 
   * @return
   */
  public long getNhdrEntrySize()
  {
    long size = 0;
    
    int nhdrSize = 0;
    
    nhdrSize = getNhdrSize();
    if ((nhdrSize <= 0) ||
        (namesz <= 0) || (descsz <= 0))
      {
        //Invalid object.
        return size;
      }
    
    size = nhdrSize + namesz + descsz;
    
    return size;
  }
 
  /**
   * Just get the size of Nhdr struct.
   * 
   * @return
   */
  public native int getNhdrSize();
  
  protected native long fillNhdr(byte[] buffer, long startAddress);
  protected native long fillNhdrName(byte[] buffer, long startAddress);
  
  /**
   * Fill the region starting from startAddress in buffer according to this ElfNhdr object.
   * 
   * @param noteSecBuffer
   * @param startAddress
   * @return
   */
  public long fillMemRegion(byte[] buffer, long startAddress)
  {
    long nhdrEntrySize = 0;
    
    long fillSize = 0;
    
    fillSize = fillNhdr(buffer, startAddress);
    if (fillSize != getNhdrSize())
      {
        //XXX: error occurred. throw excetpion?
      }
    
    nhdrEntrySize += fillSize;
    startAddress += fillSize;
    fillSize = fillNhdrName(buffer, startAddress);
    if (fillSize != this.namesz)
      {
        //XXX: error occurred. Throw exception?
      }
    
    nhdrEntrySize += fillSize;
    startAddress += fillSize;
    fillSize = this.desc.fillMemRegion(buffer, startAddress);
    if (fillSize != this.descsz)
      {
        //XXX: error occurred. Throw exception?
      }
    
    nhdrEntrySize += fillSize;
    return fillSize;
  }
  
}
