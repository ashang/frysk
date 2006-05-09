// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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


package frysk.sys.proc;


public class Status
{

    /**
     * frysk.sys.proc.Status
     * 
     * Partial wrapper for /proc/$$/status. 
     *
     * This class supplements frysk.sys.proc.Stat 
     * as that class  does not contain GID and UID. 
     *
     * This class extracts that information from either
     * /proc/$$/status or from a buffer that has been
     * been passed to it for parsing.
     * 
     */

    private native static byte[] statusSlurp (int pid);
   
    /**
     * Converts a byte[] data structure to a 
     * String[] data structure 
     * @param byteBuffer[] - source byte byffer
     * @return String[] - converted String buffer
     */
    private static String[] byteBuffertoStringbuffer(byte[] byteBuffer)
    {
        String byteString = new String(byteBuffer);
        return byteString.split("\n");
    }

    /**
     * Given a byte[] buffer from /proc/$$/status
     * find and return either GID or UID.
     * @param idType - Type to return. Accepts Gid or Uid 
     * @param byteidBuffer - buffer to search.
     * @return int either GID or UID of process, or -1 on error. 
     */
    private static int getID(String idType, byte[] byteidBuffer)
    {
		// As fetching a GID/UID are very similar
		// we just pass of the search code to a simple
		// lookup method
		
		String[] idBuffer = byteBuffertoStringbuffer(byteidBuffer);
		int idIndex = 5;
		int idIndexEnd = 0;
		for (int i=0; i<idBuffer.length; i++)
		{
			if (idBuffer[i].startsWith(idType))
			{
				idIndexEnd = idIndex;
				for (int j=idIndex; j<idBuffer[i].length(); j++)
				if (idBuffer[i].charAt(j)=='\t')
					break;
				else
			   		idIndexEnd++;
				if (idIndex == idIndexEnd)
					return -1;
				else 
					return Integer.parseInt(idBuffer[i].substring(idIndex,idIndexEnd));
			}
		}
	
		// if we get here, id not found in status
	
		return -1;
    }

    /**
     * Return the UID from a given buffer. Buffer
     * has to follow format of /proc/$$/status.
     * @param buffer - buffer search.
     * @return int - UID in buffer.
     */
    public static int getUID(byte[] buffer)
    {
	if (buffer != null)
		return getID("Uid", buffer);
	else
		return -1;
    }

    /**
     * Return the UID from the /proc/$$/status
     * file according to the PID passed.
     * @param spid - PID of process to search.
     * @return int - UID of process PID.
     */
    public static int getUID(int spid)
    {	
	byte[] buffer = statusSlurp(spid);
    	return getUID(buffer);
    }

    /**
     * Return the GID from a given buffer. Buffer
     * has to follow format of /proc/$$/status.
     * @param buffer - buffer search.
     * @return int - GID in buffer.
     */
    public static int getGID(byte[] buffer)
    {
    	if (buffer != null)
    		return getID("Gid", buffer);
    	else
    		return -1;
    }
    
    /**
     * Return the GID from the /proc/$$/status
     * file according to the PID passed.
     * @param spid - PID of process to search.
     * @return int - GID of process PID.
     */
    public static int getGID(int spid)
    {
    	byte[] buffer = statusSlurp(spid);
    	return getGID(buffer);
    }

}
