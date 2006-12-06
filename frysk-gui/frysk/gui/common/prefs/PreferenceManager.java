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

package frysk.gui.common.prefs;

import java.util.HashMap;
import java.util.Iterator;
import java.util.prefs.Preferences;

/**
 * PreferenceManager keeps track of the different
 * {@see frysk.gui.common.prefs.PreferenceGroup} in the system.
 */
public class PreferenceManager
{

  private static HashMap preferenceGroups;

  private static Preferences prefs;

  // Static preference groups
  public static PreferenceGroup sourceWinGroup = new PreferenceGroup(
                                                                     "Source Window",
                                                                     0);

  public static PreferenceGroup syntaxHighlightingGroup = new PreferenceGroup(
                                                                              "Syntax Highlighting",
                                                                              1);

  // Initialize the hashmap for groups and the default preferences model
  static
    {
	  
      preferenceGroups = new HashMap();
      
      prefs = Preferences.userRoot();
      
    }

  /**
   * Add a preference group to the internal list of groups.
   * 
   * @param newGroup The new preference group
   */
  public static void addPreferenceGroup (PreferenceGroup newGroup)
  {
    preferenceGroups.put(newGroup.getName(), newGroup);
    newGroup.load(prefs);
  }

  /**
   * @return An iterator over all the PreferenceGroups registered with the
   *         manager.
   */
  public static Iterator getPreferenceGroups ()
  {
    return preferenceGroups.values().iterator();
  }

  /**
   * Sets the preference model to use. This change is propagated down though all
   * registered preference groups.
   * 
   * @param newModel The new preference model to use.
   */
  public static void setPreferenceModel (Preferences newModel)
  {
    prefs = newModel;
    Iterator it = getPreferenceGroups();
    while (it.hasNext())
      ((PreferenceGroup) it.next()).load(newModel);
  }

  /**
   * Saves all changes into the preferences model. This call is propagated down
   * through all registered PreferenceGroups.
   */
  public static void saveAll ()
  {
    Iterator it = getPreferenceGroups();
    while (it.hasNext())
      ((PreferenceGroup) it.next()).save(prefs);
  }

  /**
   * Reverts the preferences in each of the groups back to the values in the
   * model.
   */
  public static void revertAll ()
  {
    Iterator it = getPreferenceGroups();
    while (it.hasNext())
      ((PreferenceGroup) it.next()).revertAll();
  }

  public static Preferences getPrefs ()
  {
    return prefs;
  }

}
