package frysk.gui.common;

import org.gnu.gdk.Pixbuf;
import org.gnu.gtk.IconFactory;
import org.gnu.gtk.IconSet;
import org.gnu.gtk.IconSize;
import org.gnu.gtk.IconSource;

public class FryskIconSet {
	
	private String name;
	private IconSet data;
	
	public FryskIconSet(String name){
		this.name = name;
		this.data = new IconSet();
	}
	
	public String getName(){
		return this.name;
	}
	
	public IconSet getIconSet(){
		return this.data;
	}
	
	public void addIcon(Pixbuf icon, IconSize size){
		IconSource source = new IconSource();
		source.setPixbuf(icon);
		source.setSize(size);
		this.data.addSource(source);
	}
	
	public void addToFactory(IconFactory factory){
		factory.addIconSet(this.name, this.data);
	}
	
}
