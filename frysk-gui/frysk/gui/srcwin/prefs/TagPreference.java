package frysk.gui.srcwin.prefs;

import java.util.Iterator;
import java.util.Vector;
import java.util.prefs.Preferences;

import org.gnu.gtk.TextTag;

import frysk.gui.common.prefs.ColorPreference;
import frysk.gui.srcwin.ColorConverter;

public class TagPreference extends ColorPreference {

	protected boolean foreground;

	protected Vector tags;
	
	public TagPreference(String name, boolean foreground, TextTag tag) {
		super(name);
		
		this.tags = new Vector();
		if(tag != null)
			this.tags.add(tag);
		this.foreground = foreground;
	}

	public TagPreference(int name, boolean foreground, TextTag tag) {
		this(ColorPreference.NAMES[name], foreground, tag);
	}

	public void appendTags(TagPreference pref2){
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
	
	public void save(Preferences prefs){
		super.save(prefs);
		
		/*
		 * there may or may not be a text tag associated with this item
		 */
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
	
	public void load(Preferences prefs){
		super.load(prefs);
		
		/*
         * there may or may not be a text tag associated with this item
         */
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
