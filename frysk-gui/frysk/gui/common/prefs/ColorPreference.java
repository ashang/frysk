package frysk.gui.common.prefs;

import java.util.Iterator;
import java.util.Vector;
import java.util.prefs.Preferences;

import org.gnu.gdk.Color;

public class ColorPreference extends FryskPreference {

	public interface ColorPreferenceListener{
		void preferenceChanged(String prefName, Color newColor);
	}

    protected Color currentColor;
	protected Color fallback;
	
	protected Vector listeners;
	
	public ColorPreference(String name, Color fallback) {
		this.name = name;
		this.fallback = fallback;
		this.listeners = new Vector();
	}
	
	public void setCurrentColor(Color currentColor) {
		this.currentColor = currentColor;
	}

	public Color getCurrentColor() {
		return this.currentColor;
	}

	public void save(Preferences prefs) {
		this.model.putInt(name + "_R", this.currentColor.getRed());
		this.model.putInt(name + "_G", this.currentColor.getGreen());
		this.model.putInt(name + "_B", this.currentColor.getBlue());
		
		Iterator it = this.listeners.iterator();
		while(it.hasNext())
			((ColorPreferenceListener) it.next()).preferenceChanged(this.name, this.currentColor);
	}

	public void load(Preferences prefs) {
		this.model = prefs;
		
		this.revert();
	}

	public void addListener(ColorPreferenceListener listener){
		this.listeners.add(listener);
		listener.preferenceChanged(this.name, this.currentColor);
	}

	public void revert() {
		int r = model.getInt(name + "_R", fallback.getRed());
		int g = model.getInt(name + "_G", fallback.getGreen());
		int b = model.getInt(name + "_B", fallback.getBlue());
		this.currentColor = new Color(r, g, b);
	}
}
