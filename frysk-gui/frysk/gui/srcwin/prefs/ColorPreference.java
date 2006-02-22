package frysk.gui.srcwin.prefs;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.prefs.Preferences;

import org.gnu.gdk.Color;
import org.gnu.gtk.TextTag;

import frysk.gui.srcwin.ColorConverter;

public class ColorPreference extends FryskPreference {

	protected static HashMap defaultColors;

	public static final String[] NAMES = { "Text", "Background", "Margin",
			"Line Number Color", "Current Line", "Search Results", "Executable Marks"};

    public static final int TEXT = 0;
    public static final int BACKGROUND = 1;
    public static final int MARGIN = 2;
    public static final int LINE_NUMBER = 3;
    public static final int CURRENT_LINE = 4;
    public static final int SEARCH = 5;
    public static final int EXEC_MARKS = 6;
    
	static {
		defaultColors = new HashMap();
		
		defaultColors.put(NAMES[TEXT], Color.BLACK);
		defaultColors.put(NAMES[BACKGROUND], Color.WHITE);
		defaultColors.put(NAMES[MARGIN], new Color(47031, 42662, 52685));
		defaultColors.put(NAMES[LINE_NUMBER], Color.BLACK);
		defaultColors.put(NAMES[CURRENT_LINE], Color.GREEN);
		defaultColors.put(NAMES[SEARCH], Color.ORANGE);
		defaultColors.put(NAMES[EXEC_MARKS], Color.BLACK);
	}

	
	
	private static Color getDefaultColor(String name) {
		return (Color) defaultColors.get(name);
	}

	protected Color currentColor;

	protected boolean foreground;

	protected Vector tags;

	protected ColorPreference(String name, boolean foreground,
			TextTag tag) {
		this.tags = new Vector();
		if(tag != null)
			this.tags.add(tag);
		this.foreground = foreground;
		this.name = name;
	}
	
	public ColorPreference(int name, boolean foreground, TextTag tag){
		this(NAMES[name], foreground, tag);
	}
	
	public ColorPreference(int name){
		this(NAMES[name], false, null);
	}

	protected void setModel(Preferences prefs){
		this.model = prefs;
		
		Color fallback = getDefaultColor(name);
		int r = model.getInt(name + "_R", fallback.getRed());
		int g = model.getInt(name + "_G", fallback.getGreen());
		int b = model.getInt(name + "_B", fallback.getBlue());
		this.currentColor = new Color(r, g, b);
		
        /*
         * there may or may not be a text tag associated with this item
         */
        if (tags != null) {
        	Iterator it = this.tags.iterator();
        	while(it.hasNext()){
        		TextTag tag = (TextTag) it.next();
	            if (foreground)
	                tag.setForeground(ColorConverter
	                        .colorToHexString(this.currentColor));
	            else
	                tag.setBackground(ColorConverter
	                        .colorToHexString(this.currentColor));
        	}
        }
	}
	
	public void setCurrentColor(Color currentColor) {
		this.currentColor = currentColor;
	}

	public Color getCurrentColor() {
		return this.currentColor;
	}

	public void appendTags(ColorPreference pref2){
		Iterator it = pref2.tags.iterator();
		
		while(it.hasNext()){
			TextTag tag = (TextTag) it.next();
			if(!this.tags.contains(tag))
				this.tags.add(tag);
		}
	}
	
	public void removeTag(TextTag tag){
		this.tags.remove(tag);
	}
	
	public void saveValues() {
		this.model.putInt(name + "_R", this.currentColor.getRed());
		this.model.putInt(name + "_G", this.currentColor.getGreen());
		this.model.putInt(name + "_B", this.currentColor.getBlue());

		/*
		 * there may or may not be a text tag associated with this item
		 */
		if (tags != null) {
			Iterator it = this.tags.iterator();
        	while(it.hasNext()){
        		TextTag tag = (TextTag) it.next();
	            if (foreground)
	                tag.setForeground(ColorConverter
	                        .colorToHexString(this.currentColor));
	            else
	                tag.setBackground(ColorConverter
	                        .colorToHexString(this.currentColor));
        	}
		}
	}

}
