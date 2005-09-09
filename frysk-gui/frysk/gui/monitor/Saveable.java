package frysk.gui.monitor;

import java.util.prefs.Preferences;

/*
 * Created on 19-Jul-05
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

public interface Saveable {
	void save(Preferences prefs);
	void load(Preferences prefs);
}
