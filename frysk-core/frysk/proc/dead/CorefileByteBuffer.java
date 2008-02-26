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

package frysk.proc.dead;

import frysk.sys.StatelessFile;
import java.io.File;
import java.util.ArrayList;
import lib.dwfl.Elf;
import lib.dwfl.ElfCommand;
import lib.dwfl.ElfEHeader;
import lib.dwfl.ElfException;
import lib.dwfl.ElfPHeader;
import inua.eio.ByteBuffer;
import frysk.rsl.Log;

public class CorefileByteBuffer extends ByteBuffer {

  MapAddressHeader[] offsetList;
  //ArrayList offsetList = new ArrayList();
  File coreFile = null;
  File exeFile = null;
  StatelessFile coreFileRaw = null;
  boolean fileOpen = false;
    
  private static final Log finest = Log.finest(CorefileByteBuffer.class);

  private CorefileByteBuffer(File file, long lowerExtreem, 
			     long upperExtreem, 
			     MapAddressHeader[] metaData) throws ElfException

  {
    super(lowerExtreem, upperExtreem);
    this.coreFile = file;
    Elf elf = openCoreFileElf(file);
    setEndianWordSize(elf);
    offsetList = metaData;
    openFile();
    closeCoreFileElf(elf);
  }

  public CorefileByteBuffer(File file, MapAddressHeader[] metaData) throws ElfException
  {
    // XXX: Don't know the size of the highWater mark
    // Map -1 to 0xFFFFn (Long max size). Should build the
    // maps in advance.
    this(file,0,-1, metaData);
  }

  public CorefileByteBuffer(File file) throws ElfException
  {
    this(file,null);
    Elf elf = openCoreFileElf(file);
    offsetList = buildElfMaps(elf);
    closeCoreFileElf(elf);
  }

  protected void poke(long arg0, int arg1) 
  {
    throw new RuntimeException("Cannot poke into a corefile!");
  }
  
  protected int peek(long address) 
  {


    finest.log(this,"peek() at 0x",address);
    byte[] buffer = new byte[1];
    MapAddressHeader metaLine = findMetaData(address);

    if (metaLine != null)
      if (checkCorefileAddress(metaLine)) {
	  long offset = convertAddressToOffset(address);
	  this.coreFileRaw.pread(offset, buffer,0,1);
	}
      else
	{
	  if (!metaLine.name.equals("")) {
	        finest.log(this,"Attempting to read from non-corefile: ", metaLine.name);
		File fileCheck = new File(metaLine.name);
		if (((fileCheck.exists()) && fileCheck.canRead())) {
		    StatelessFile temp = new StatelessFile(new File(metaLine.name));	      
		    long offset = metaLine.solibOffset  + (address - metaLine.vaddr);
		    temp.pread(offset, buffer,0,1);
		} else {
		    throw new RuntimeException("CorefileByteBuffer: Cannot peek() at address 0x"+
					       Long.toHexString(address)+". Offset exists in file: " +
					       metaLine.name+" but that file cannot be accessed.");
		}
	    }
	}
	
    else
      throw new RuntimeException("CorefileByteBuffer: Cannot peek() " +
      				 "at address 0x" +
				 Long.toHexString(address)+"." +
				 " Address location is unknown " +
				 " (not in corefile, executable or "+
				 " mapped solibs).");

    return buffer[0];
  }

  protected ByteBuffer subBuffer (ByteBuffer parent, long lowerExtreem,
				  long upperExtreem)
  {
    CorefileByteBuffer up = (CorefileByteBuffer)parent;
    CorefileByteBuffer sub;
    try
      {
	sub =  new CorefileByteBuffer (up.coreFile,
				       lowerExtreem, 
				       upperExtreem,offsetList);
      }
    catch (ElfException e)
      {
	return null;
      }

    return sub;
  }

  private boolean openFile() 
  {
      this.coreFileRaw = new StatelessFile(this.coreFile);
      if (this.coreFileRaw != null)
	return true;

      System.out.println("Unable to open " + this.coreFile.getPath());
      return false;
  }
  
  private boolean isFileSane()
  {
    if (this.coreFileRaw != null)
      return true;

    return true;
  }
  

  private MapAddressHeader findMetaData(long address)
  {
    MapAddressHeader data;
    for (int i=0; i<offsetList.length; i++)
      {
        data = offsetList[i];
        
        if ((address >= data.vaddr) && 
            (address <= (data.vaddr_end))) {
	    finest.log(this, "findMetaData() memory location 0x" + 
		       Long.toHexString(address)+ " found at " +
		       "0x"+Long.toHexString(data.vaddr)+"-"+
		       "0x"+Long.toHexString(data.vaddr_end)+
		       " Name: "+data.name);
		       
	  return data;
	}
      }

    finest.log(this, "findMetaData() cannot find metadata for addr 0x"+Long.toHexString(address));
    return null;
  }
  private boolean checkCorefileAddress(MapAddressHeader data)
  {
    boolean isCoreFile = false;
    if (data.fileSize > 0)
      isCoreFile = true;

    finest.log(this,"checkCorefileAddress()= " + isCoreFile);
    return isCoreFile;
    
  }
	  
  private long convertAddressToOffset (long address)
  {
    MapAddressHeader pair;
    long offset = 0;
    boolean foundOffset = false;
    boolean presentInFile = false;

    for (int i=0; i<offsetList.length; i++)
      {
        pair = offsetList[i];
        if ((address >= pair.vaddr) && 
            (address <= (pair.vaddr_end)))
          {
            offset = pair.corefileOffset + (address - pair.vaddr);
            foundOffset = true;
	    if (pair.fileSize > 0)
	      presentInFile = true;
            break;
          }
            
      }
        
    //XXX: We can't return -1 here if offset is not found as that will 
    //XXX: render a positive hex offset. Instead throw an exception if 
    //XXX: the boolean gate indicates offset not found.
    
    if (!foundOffset)
      throw new RuntimeException("Cannot find file offset for given address 0x"
                                 + Long.toHexString(address));

    
    if (!presentInFile)
      throw new RuntimeException("Cannot read file offset for given address 0x"
                                 + Long.toHexString(address) + 
				 ". It is elided from the core file");
    return offset;
  }

  private Elf openCoreFileElf(File file) {
      return  new Elf(file, ElfCommand.ELF_C_READ);
  }

  private void closeCoreFileElf(Elf elf)
  {
    elf.close();
    elf = null;
  }


    private void setEndianWordSize(Elf elf) {
	ElfEHeader elfHeader = elf.getEHeader();
	order(elfHeader.getByteOrder());
	wordSize(elfHeader.getWordSize());
    }

  /** Build basic meta data for this Buffer. This allows conversion
   *  of offset to address, and also tells the buffer whether a read 
   *  is legal before that read happens.
   **/

  private MapAddressHeader[] buildElfMaps(Elf elf) throws ElfException
  {

    ArrayList localList = new ArrayList();
    if (isFileSane())
      {

        ElfEHeader eHeader = elf.getEHeader();
        for (int i=0; i<eHeader.phnum; i++)
          {
            // Test if pheader is of types LOAD. If so add to list
            ElfPHeader pHeader = elf.getPHeader(i);
            if (pHeader.type == ElfPHeader.PTYPE_LOAD)
              {

                localList.add(new MapAddressHeader(pHeader.vaddr,
						   pHeader.vaddr+pHeader.memsz,
						   false,
						   false, false,
						   pHeader.offset,
						   0,
						   pHeader.filesz,
						   pHeader.memsz,
						   "",0x1000));
              }
          }
      }
    else
      throw new RuntimeException("Cannot IO access " + this.coreFile.getPath());

    return (MapAddressHeader[]) localList.toArray(new MapAddressHeader[localList.size()]);
  }


}
