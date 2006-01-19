package frysk.gui.srcwin.prefs;

import org.gnu.gtk.HBox;

public class SyntaxPreferenceViewer extends HBox implements PreferenceViewer{
	
	private ColorPreferenceEditor editor;
	private PreferenceList list;
	
	public SyntaxPreferenceViewer(){
		super(false, 5);
		this.setBorderWidth(10);
		
		this.editor = new ColorPreferenceEditor();
		this.list = new PreferenceList(this, SyntaxPreference.NAMES);
		
		this.packStart(this.list);
		this.packStart(this.editor);
	}
	
	public void showPreferenceEditor(ColorPreference selected){
		this.editor.setCurrentPref(selected);
	}
	
	public void saveAll(){
		this.list.saveAll();
	}

}
