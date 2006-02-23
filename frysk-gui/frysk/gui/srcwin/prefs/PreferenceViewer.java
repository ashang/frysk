package frysk.gui.srcwin.prefs;

import frysk.gui.common.prefs.ColorPreference;

public interface PreferenceViewer {
    
    void showPreferenceEditor(ColorPreference toShow);
    
    void saveAll();
}
