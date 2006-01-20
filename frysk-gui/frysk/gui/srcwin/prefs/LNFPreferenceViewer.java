package frysk.gui.srcwin.prefs;

import org.gnu.gtk.CheckButton;
import org.gnu.gtk.HBox;
import org.gnu.gtk.Label;
import org.gnu.gtk.SpinButton;
import org.gnu.gtk.VBox;
import org.gnu.gtk.event.SpinEvent;
import org.gnu.gtk.event.SpinListener;
import org.gnu.gtk.event.ToggleEvent;
import org.gnu.gtk.event.ToggleListener;

public class LNFPreferenceViewer extends VBox implements PreferenceViewer, ToggleListener, SpinListener{

    private ColorPreferenceEditor editor;
    private PreferenceList list;
    
    private CheckButton lineCheck;
    private CheckButton execMarkCheck;
    private CheckButton toolbarCheck;
    
    private SpinButton inlineLevels;
    
    public LNFPreferenceViewer(){
        super(false, 5);
        this.setBorderWidth(10);
        
        this.editor = new ColorPreferenceEditor();
        this.list = new PreferenceList(this, ColorPreference.NAMES);
        
        HBox topBox = new HBox(false, 5);
        topBox.packStart(this.list);
        topBox.packStart(this.editor);
        
        this.packStart(topBox);
        
        this.lineCheck = new CheckButton();
        this.lineCheck.setLabel("Show Line Numbers");
        this.lineCheck.setState(PreferenceManager.getBooleanPreferenceValue(BooleanPreference.LINE_NUMS));
        this.lineCheck.addListener(this);
        this.packStart(this.lineCheck);
        
        this.execMarkCheck = new CheckButton();
        this.execMarkCheck.setLabel("Show Executable Line Markers");
        this.execMarkCheck.setState(PreferenceManager.getBooleanPreferenceValue(BooleanPreference.EXEC_MARKS));
        this.execMarkCheck.addListener(this);
        this.packStart(this.execMarkCheck);
        
        this.toolbarCheck = new CheckButton();
        this.toolbarCheck.setLabel("Show Toolbar");
        this.toolbarCheck.setState(PreferenceManager.getBooleanPreferenceValue(BooleanPreference.TOOLBAR));
        this.toolbarCheck.addListener(this);
        this.packStart(this.toolbarCheck);
        
        HBox inlineBox = new HBox(false, 5);
        inlineBox.packStart(new Label("Maximum of levels of inline code to display"));
        this.inlineLevels = new SpinButton(0, 50, 1);
        this.inlineLevels.setValue(PreferenceManager.getIntPreferenceValue(IntPreference.INLINE_LEVELS));
        this.inlineLevels.addListener(this);
        inlineBox.packStart(this.inlineLevels);
        this.packStart(inlineBox);
        
        this.showAll();
    }

    public void showPreferenceEditor(ColorPreference toShow) {
        this.editor.setCurrentPref(toShow);
    }

    public void saveAll() {
    		PreferenceManager.getBooleanPreference(BooleanPreference.LINE_NUMS).saveValues();
    		PreferenceManager.getBooleanPreference(BooleanPreference.EXEC_MARKS).saveValues();
    		PreferenceManager.getBooleanPreference(BooleanPreference.TOOLBAR).saveValues();
    		PreferenceManager.getIntPreference(IntPreference.INLINE_LEVELS).saveValues();
    		
        this.list.saveAll();
    }

    public void toggleEvent(ToggleEvent arg0) {
        // This is a little inefficient, but just save all of them
        PreferenceManager.getBooleanPreference(BooleanPreference.LINE_NUMS).setCurrentValue(this.lineCheck.getState());
        PreferenceManager.getBooleanPreference(BooleanPreference.EXEC_MARKS).setCurrentValue(this.execMarkCheck.getState());
        PreferenceManager.getBooleanPreference(BooleanPreference.TOOLBAR).setCurrentValue(this.toolbarCheck.getState());
    }

    public void spinEvent(SpinEvent arg0) {
        PreferenceManager.getIntPreference(IntPreference.INLINE_LEVELS).setCurrentValue(this.inlineLevels.getIntValue());
    }
}
