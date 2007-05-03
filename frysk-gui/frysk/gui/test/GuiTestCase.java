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
// type filter text
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

package frysk.gui.test;

import java.io.File;

import frysk.Config;
import frysk.junit.TestCase;

public class GuiTestCase extends TestCase{
  public static final File TEST_DIR = Config.getFryskTestDir();
  public static final File OBSERVERS_TEST_DIR = new File(TEST_DIR.getPath() + "/Observers");
  public static final File SESSIONS_TEST_DIR = new File(TEST_DIR.getPath() + "/Sessions");
  public static final File TAGSETS_TEST_DIR = new File(TEST_DIR.getPath() + "/Tagsets");
  
  protected void setUp () throws Exception
  {
    super.setUp();
    
    TEST_DIR.mkdir();
    cleanDir(TEST_DIR);
    
    OBSERVERS_TEST_DIR.mkdir();
    cleanDir(OBSERVERS_TEST_DIR);
    
    SESSIONS_TEST_DIR.mkdir();
    cleanDir(SESSIONS_TEST_DIR);
    
    TAGSETS_TEST_DIR.mkdir();
    cleanDir(TAGSETS_TEST_DIR);
  }
	
  protected void tearDown () throws Exception
  {
    super.tearDown();
    
    cleanDir(TEST_DIR);
    TEST_DIR.delete();
    
    cleanDir(OBSERVERS_TEST_DIR);
    OBSERVERS_TEST_DIR.delete();
    
    cleanDir(SESSIONS_TEST_DIR);
    SESSIONS_TEST_DIR.delete();
    
    cleanDir(TAGSETS_TEST_DIR);
    TAGSETS_TEST_DIR.delete();
    
  }
	
  public void cleanDir(File dir){
    File[] files = dir.listFiles();
    if(files!=null){
      for (int i = 0; i < files.length; i++){
	files[i].delete();
      }
    }
  }
  
}
