package frysk.gui.srcwin.prefs;

import org.gnu.gtk.HBox;
import org.gnu.gtk.VBox;

public class LNFPreferenceViewer extends VBox implements PreferenceViewer{

    private ColorPreferenceEditor editor;
    private PreferenceList list;
    
    public LNFPreferenceViewer(){
        super(false, 5);
        this.setBorderWidth(10);
        
        this.editor = new ColorPreferenceEditor();
        this.list = new PreferenceList(this, ColorPreference.NAMES);
        
        HBox topBox = new HBox(false, 5);
        topBox.packStart(this.list);
        topBox.packStart(this.editor);
        
        this.packStart(topBox);
    }

    public void showPreferenceEditor(ColorPreference toShow) {
        this.editor.setCurrentPref(toShow);
    }

    public void saveAll() {
        this.list.saveAll();
    }
}
