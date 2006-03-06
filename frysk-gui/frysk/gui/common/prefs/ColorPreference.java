package frysk.gui.common.prefs;

import java.util.Iterator;
import java.util.Vector;
import java.util.prefs.Preferences;

import org.gnu.gdk.Color;

/**
 * ColorPreference models a color-values preference for Frysk
 * 
 */
public class ColorPreference extends FryskPreference {

	public interface ColorPreferenceListener{
		void preferenceChanged(String prefName, Color newColor);
	}

    protected Color currentColor;
	protected Color fallback;
	
	protected Vector listeners;
	
	/**
	 * Creates a new ColorPreference
	 * @param name The name of the preference
	 * @param fallback The default color to use
	 */
	public ColorPreference(String name, Color fallback) {
		this.name = name;
		this.fallback = fallback;
		this.listeners = new Vector();
	}
	
	/**
	 * Sets the current color for this preference. Note that this is
	 * not saved into the model until {@see #save(Preferences)} is called.
	 * @param currentColor The new color
	 */
	public void setCurrentColor(Color currentColor) {
		this.currentColor = currentColor;
	}

	/**
	 * 
	 * @return The current value of this preference
	 */
	public Color getCurrentColor() {
		return this.currentColor;
	}

	/**
	 * Saves the value of this preference into the preference model and
	 * notify all attached listeners
	 */
	public void save(Preferences prefs) {
		this.model.putInt(name + "_R", this.currentColor.getRed());
		this.model.putInt(name + "_G", this.currentColor.getGreen());
		this.model.putInt(name + "_B", this.currentColor.getBlue());
		
		Iterator it = this.listeners.iterator();
		while(it.hasNext())
			((ColorPreferenceListener) it.next()).preferenceChanged(this.name, this.currentColor);
	}

	/**
	 * Sets the preference to use the provided model and loads the values from
	 * it.
	 */
	public void load(Preferences prefs) {
		this.model = prefs;
		
		this.revert();
	}

	/**
	 * Adds a listener to this preference that will get notified whenever
	 * the value of this preference changes
	 * @param listener The object to notify when the preference changes.
	 */
	public void addListener(ColorPreferenceListener listener){
		this.listeners.add(listener);
		listener.preferenceChanged(this.name, this.currentColor);
	}

	/**
	 * Restores the current value of this preference from the model.
	 */
	public void revert() {
		int r = model.getInt(name + "_R", fallback.getRed());
		int g = model.getInt(name + "_G", fallback.getGreen());
		int b = model.getInt(name + "_B", fallback.getBlue());
		this.currentColor = new Color(r, g, b);
	}
}
