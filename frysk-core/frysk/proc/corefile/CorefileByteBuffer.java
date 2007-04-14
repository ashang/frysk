// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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

package frysk.proc.corefile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;

import lib.elf.Elf;
import lib.elf.ElfCommand;
import lib.elf.ElfEHeader;
import lib.elf.ElfException;
import lib.elf.ElfFileException;
import lib.elf.ElfPHeader;


import inua.eio.ByteBuffer;

public class CorefileByteBuffer 
	extends ByteBuffer

{

  
  // Private class to hold 
  private class MapAddress
  {
    long offsetStart = 0;
    long offsetEnd = 0;
    long fileSize = 0;
    long vaddr = 0;
    
    public MapAddress(long vaddr, long start, long end, long fileSize)
    {
      this.vaddr = vaddr;
      this.offsetStart = start;
      this.offsetEnd = end;
      this.fileSize = fileSize;
    }
  }
  
  ArrayList offsetList = new ArrayList();
  File coreFile = null;
  RandomAccessFile coreFileRaw = null;
  boolean fileOpen = false;

  public CorefileByteBuffer(File file) throws ElfException
  {
    // Don't know the size of the highWater mark
    // Map -1 to 0xFFFFn (Long max size)
    super(0,-1);
    this.coreFile = file;
    buildElfMaps();
    fileOpen = openFile();
  }

  protected void poke(long arg0, int arg1) 
  {
    throw new RuntimeException("Cannot poke into a corefile!");
  }
  
  protected int peek(long address) 
  {

    int peekValue = 0;
    if (isFileOpen())
      {
	long offset = convetAddressToOffset(address);

	try
	{
	  this.coreFileRaw.seek(offset);
	  peekValue = this.coreFileRaw.readByte();
	}
	catch (IOException e)
	{
	  throw new RuntimeException("Cannot seek to offset " + address);
	}

	return peekValue;
      }
    else
      throw new RuntimeException("Cannot access corefile backing ByteBuffer");
  }


  protected void finalize()
  {
    try
      {
	this.coreFileRaw.close();
      }
    catch (IOException e)
      {
	e.printStackTrace();
      }
  }
  

 
  
  private boolean openFile() 
  {
    try
    {
      this.coreFileRaw = new RandomAccessFile(this.coreFile,"r");
    }
    catch (FileNotFoundException e)
    {
      throw new RuntimeException(e);
    }
    
    return true;
  }
  
  private void buildElfMaps() throws ElfException  
  {
    if (isFileSane())
      {
	try
	{
	  Elf elf = new Elf (this.coreFile.getPath(), ElfCommand.ELF_C_READ);
	  ElfEHeader eHeader = elf.getEHeader();
	  for (int i=0; i<eHeader.phnum; i++)
	    {
	      // Test if pheader is of types LOAD. If so add to list
	      ElfPHeader pHeader = elf.getPHeader(i);
	      if (pHeader.type == ElfPHeader.PTYPE_LOAD)
		{

		  offsetList.add(new MapAddress(pHeader.vaddr,
		                                pHeader.offset, 
		                                pHeader.offset + 
		                                pHeader.filesz, 
		                                pHeader.filesz));
		}
	    }  
	  elf.close();

	}
	catch (ElfFileException e)
	{
	  throw(e);
	}
	catch (ElfException e)
	{
	  throw(e);
	}


      }
    else
      throw new RuntimeException("Cannot IO access " + this.coreFile.getPath());

  }


  private boolean isFileSane()
  {
    if (this.coreFile.exists() && this.coreFile.canRead())
      return true;

    return false;
  }
  
  private boolean isFileOpen()
  {
    return fileOpen;
  }

  private long convetAddressToOffset (long address)
  {
    MapAddress pair;
    long offset = 0;
    boolean foundOffset = false;
    
    Iterator i = offsetList.iterator();
    while (i.hasNext())
      {
        pair = (MapAddress) i.next();
        if ((address >= pair.vaddr) && 
            (address <= (pair.vaddr + pair.fileSize)))
          {
            offset = pair.offsetStart + (address - pair.vaddr);
            foundOffset = true;
            break;
          }
            
      }
        
    //XXX: We can't return -1 here if offset is not found as that will 
    //XXX: render a positive hex offset. Instead throw an exception if 
    //XXX: the boolean gate indicates offset not found.
    
    if (!foundOffset)
      throw new RuntimeException("Cannot find file offset for given address 0x"
                                 + Long.toHexString(address));
    return offset;
  }

   
}
