package frysk.gui.srcwin.prefs;

import org.gnu.gtk.HBox;

public class SyntaxPreferenceViewer extends HBox{
	
	private ColorPreferenceEditor editor;
	private SyntaxPrefList list;
	
	public SyntaxPreferenceViewer(){
		super(false, 5);
		this.setBorderWidth(10);
		
		this.editor = new ColorPreferenceEditor();
		this.list = new SyntaxPrefList(this);
		
		this.packStart(this.list);
		this.packStart(this.editor);
	}
	
	public void showSyntaxPrefEditor(SyntaxPreference selected){
		this.editor.setCurrentPref(selected);
	}
	
	public void saveAll(){
		this.list.saveAll();
	}

}
