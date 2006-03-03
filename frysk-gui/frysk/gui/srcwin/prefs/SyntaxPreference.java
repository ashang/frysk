package frysk.gui.srcwin.prefs;

import java.util.Iterator;
import java.util.Vector;
import java.util.prefs.Preferences;

import org.gnu.gdk.Color;
import org.gnu.pango.Style;
import org.gnu.pango.Weight;

import frysk.gui.common.prefs.ColorPreference;

public class SyntaxPreference extends ColorPreference {

	public interface SyntaxPreferenceListener{
		void preferenceChanged(String name, Color newColor, Weight newWeight, Style newStyle);
	}

	protected Weight currentWeight;
	protected Style currentStyle;
	
	protected Weight defaultWeight;
	protected Style defaultStyle;

	protected Vector syntaxListeners;
	
	public SyntaxPreference(String name, Color fallback, Weight defaultWeight, Style defaultStyle) {
		super(name, fallback);
		
		this.defaultStyle = defaultStyle;
		this.defaultWeight = defaultWeight;
		this.syntaxListeners = new Vector();
	}

	public Style getCurrentStyle() {
		return currentStyle;
	}

	public void toggleItalics() {
		if (this.currentStyle.equals(Style.ITALIC))
			this.currentStyle = Style.NORMAL;
		else
			this.currentStyle = Style.ITALIC;
	}

	public Weight getCurrentWeight() {
		return currentWeight;
	}

	public void toggleBold() {
		if (this.currentWeight.equals(Weight.BOLD))
			this.currentWeight = Weight.NORMAL;
		else
			this.currentWeight = Weight.BOLD;
	}

	public void save(Preferences prefs) {
		super.save(prefs);

		this.model.putInt(name + "_weight", this.currentWeight.getValue());
		this.model.putInt(name + "_style", this.currentStyle.getValue());
		
		Iterator it = this.syntaxListeners.iterator();
		while(it.hasNext())
			((SyntaxPreferenceListener) it.next()).preferenceChanged(name, currentColor, currentWeight, currentStyle);
	}
	
	public void addListener(SyntaxPreferenceListener listener){
		this.syntaxListeners.add(listener);
		listener.preferenceChanged(this.getName(), this.currentColor, this.currentWeight, this.currentStyle);
	}
	
	public void revert(){
		super.revert();
		this.currentWeight = Weight.intern(this.model.getInt(name + "_weight",
				this.defaultWeight.getValue()));

		this.currentStyle = Style.intern(this.model.getInt(name + "_style",
				this.defaultStyle.getValue()));
	}
}
