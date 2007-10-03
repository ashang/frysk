// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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

package frysk.proc;

/**
 * Exception class for problems accessing a Task's executable file.
 */
class TaskFileException extends TaskSevereException 
{
  private static final long serialVersionUID = 200608040000L;

  private String fileName;
  
  /**
   * Class constructor.
   *
   * @param message the message for the exception.
   */
  public TaskFileException(String message)
  {
    super(message);
  }

  /**
   * Class constructor with cause.
   *
   * @param message the message for the exception.
   * @param cause the chained exception.
   */
  public TaskFileException(String message, Throwable cause)
  {
    super(message, cause);
  }

  /**
   * Class constructor with Task argument.
   *
   * @param message message for the exception
   * @param task the offending task
   * @see TaskException
   */
  public TaskFileException(String message, Task task)
  {
    super(message, task);
  }

  /**
   * Class constructor with Task and cause arguments.
   *
   * @param message message for the exception
   * @param task the offending task
   * @param cause the chained exception
   * @see TaskException
   */
  public TaskFileException(String message, Task task, Throwable cause)
  {
    super(message, task, cause);
  }

  /**
   * Class constructor with Task and file name arguments.
   *
   * @param message message for the exception
   * @param task the offending task
   * @param fileName name of the task's executable
   * @see TaskException
   */
  public TaskFileException(String message, Task task, String fileName) 
  {
    super(message, task);
    this.fileName = fileName;
  }

  /**
   * Class constructor with Task, file name and cause arguments.
   *
   * @param message message for the exception
   * @param task the offending task
   * @param fileName name of the task's executable
   * @param cause the chained exception
   * @see TaskException
   */
  public TaskFileException(String message, Task task, String fileName, 
			   Throwable cause) 
  {
    super(message, task, cause);
    this.fileName = fileName;
  }
  
  /**
   * Accessor for file name.
   *
   * @Returns the file name
   */
  public String getFileName()
  {
    return fileName;
  }
}
