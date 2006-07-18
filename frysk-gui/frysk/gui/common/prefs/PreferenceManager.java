

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
      
      addPreferenceGroup(sourceWinGroup);
      addPreferenceGroup(syntaxHighlightingGroup);
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
