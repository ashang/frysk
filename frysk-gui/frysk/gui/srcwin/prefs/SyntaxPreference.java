package frysk.gui.srcwin.prefs;

import java.util.HashMap;
import java.util.prefs.Preferences;

import org.gnu.gdk.Color;
import org.gnu.gtk.TextTag;
import org.gnu.pango.Style;
import org.gnu.pango.Weight;

public class SyntaxPreference extends ColorPreference {

	private static HashMap defaultWeights;

	private static HashMap defaultStyles;

	private Weight currentWeight;

	private Style currentStyle;

	public static String[] NAMES = { "Functions", "Variables",
			"Optimized Variables", "Out of Scope Variables", "Keywords",
			"Global Variables", "Classes" };

	public static final int FUNCTIONS = 0;
	public static final int VARIABLES = 1;
	public static final int OPTIMIZED = 2;
	public static final int OUT_OF_SCOPE = 3;
	public static final int KEYWORDS = 4;
	public static final int GLOBALS = 5;
	public static final int CLASSES = 6;
	
	static {
		defaultWeights = new HashMap();
		defaultStyles = new HashMap();

		// Funcitons
		defaultColors.put(NAMES[0], new Color(771, 21331, 65535));
		defaultWeights.put(NAMES[0], Weight.BOLD);
		defaultStyles.put(NAMES[0], Style.NORMAL);
		// Variables
		defaultColors.put(NAMES[1], new Color(14906, 36494, 13364));
		defaultWeights.put(NAMES[1], Weight.NORMAL);
		defaultStyles.put(NAMES[1], Style.NORMAL);
		// Optimized variables
		defaultColors.put(NAMES[2], new Color(36494, 36494, 36494));
		defaultWeights.put(NAMES[2], Weight.NORMAL);
		defaultStyles.put(NAMES[2], Style.NORMAL);
		// Out of scope variables
		defaultColors.put(NAMES[3], new Color(36494, 36494, 36494));
		defaultWeights.put(NAMES[3], Weight.NORMAL);
		defaultStyles.put(NAMES[3], Style.NORMAL);
		// Keywords
		defaultColors.put(NAMES[4], new Color(34438, 3341, 26214));
		defaultWeights.put(NAMES[4], Weight.BOLD);
		defaultStyles.put(NAMES[4], Style.NORMAL);
		// Global Variables
		defaultColors.put(NAMES[5], new Color(14906, 36494, 13364));
		defaultWeights.put(NAMES[5], Weight.NORMAL);
		defaultStyles.put(NAMES[5], Style.ITALIC);
		// Classes
		defaultColors.put(NAMES[6], new Color(55769, 4112, 5140));
		defaultWeights.put(NAMES[6], Weight.NORMAL);
		defaultStyles.put(NAMES[6], Style.NORMAL);
	}

	protected SyntaxPreference(String name, TextTag tag) {
		super(name, true, tag);
	}
	
	public SyntaxPreference(int name, TextTag tag){
		super(NAMES[name], true, tag);
	}

	public Style getCurrentStyle() {
		return currentStyle;
	}

	public void toggleItalics() {
		System.out.println("Toggling italic");
		if (this.currentStyle.equals(Style.ITALIC))
			this.currentStyle = Style.NORMAL;
		else
			this.currentStyle = Style.ITALIC;
	}

	public Weight getCurrentWeight() {
		return currentWeight;
	}

	public void toggleBold() {
		System.out.println("Toggling bold");
		if (this.currentWeight.equals(Weight.BOLD))
			this.currentWeight = Weight.NORMAL;
		else
			this.currentWeight = Weight.BOLD;
	}

	public void saveValues() {
		super.saveValues();

		this.model.putInt(name + "_weight", this.currentWeight.getValue());
		this.model.putInt(name + "_style", this.currentStyle.getValue());
		this.tag.setWeight(this.currentWeight);
		this.tag.setStyle(this.currentStyle);
	}

	protected void setModel(Preferences model){
		super.setModel(model);
		
		this.currentWeight = Weight.intern(this.model.getInt(name + "_weight",
				getDefaultWeight(name).getValue()));

		this.currentStyle = Style.intern(this.model.getInt(name + "_style",
				getDefaultStyle(name).getValue()));
	}
	
	private static Weight getDefaultWeight(String name) {
		return (Weight) defaultWeights.get(name);
	}

	private static Style getDefaultStyle(String name) {
		return (Style) defaultStyles.get(name);
	}
}
