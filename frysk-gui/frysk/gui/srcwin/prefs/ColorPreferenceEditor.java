/**
 * 
 */
package frysk.gui.srcwin.prefs;


import org.gnu.gtk.Button;
import org.gnu.gtk.CheckButton;
import org.gnu.gtk.ColorButton;
import org.gnu.gtk.HBox;
import org.gnu.gtk.Label;
import org.gnu.gtk.VBox;
import org.gnu.gtk.event.ColorButtonEvent;
import org.gnu.gtk.event.ColorButtonListener;
import org.gnu.gtk.event.ToggleEvent;
import org.gnu.gtk.event.ToggleListener;
import org.gnu.pango.Style;
import org.gnu.pango.Weight;

import frysk.gui.common.prefs.ColorPreference;

/**
 * @author ajocksch
 * 
 */
public class ColorPreferenceEditor extends VBox implements ColorButtonListener,
		ToggleListener {

	private ColorButton button;

	private CheckButton weightButton;

	private CheckButton styleButton;

	private ColorPreference currentPref = null;

	public ColorPreferenceEditor() {
		super(false, 0);

		// Color Button
		this.button = new ColorButton();
		button.setSensitive(false);
		this.button.addListener((ColorButtonListener) this);

		HBox box = new HBox(false, 0);
		box.packStart(new Label("Color: "), false, true, 0);
		box.packStart(this.button, false, true, 0);
		this.packStart(box, false, true, 0);

   		this.weightButton = new CheckButton();
   		this.weightButton.setLabel("Bold");
   		this.weightButton.setSensitive(false);
   		this.weightButton.addListener(this);
   		this.packStart(this.weightButton, false, true, 0);
        this.weightButton.addListener(this);

  		this.styleButton = new CheckButton();
   		this.styleButton.setLabel("Italics");
   		this.styleButton.setSensitive(false);
   		this.styleButton.addListener(this);
   		this.packStart(this.styleButton, false, true, 0);

		this.setBorderWidth(20);
	}

	public void setCurrentPref(ColorPreference newPref) {
		this.currentPref = null;
	
		this.button.setColor(newPref.getCurrentColor());
		this.button.setSensitive(true);
        
        if(newPref instanceof SyntaxPreference){
            SyntaxPreference tmp = (SyntaxPreference) newPref;
    		this.weightButton.setState(tmp.getCurrentWeight().equals(
    				Weight.BOLD));
    		this.weightButton.setSensitive(true);
    		this.styleButton.setState(tmp.getCurrentStyle()
    				.equals(Style.ITALIC));
    		this.styleButton.setSensitive(true);
        }
        else{
            this.weightButton.setState(false);
            this.weightButton.setSensitive(false);
            this.styleButton.setState(true);
            this.styleButton.setSensitive(false);
        }

		this.currentPref = newPref;
		this.showAll();
	}

	public boolean colorButtonEvent(ColorButtonEvent arg0) {
		if(this.currentPref == null)
			return false;
		
		this.currentPref.setCurrentColor(this.button.getColor());

		return false;
	}

	public void toggleEvent(ToggleEvent arg0) {
		if(this.currentPref == null)
			return;
		
        /*
         * If we only have a ColorPreference, the toggle boxes will be
         * disabled, therefore we won't even be recieving these events.
         * The fact that we are means that we must have a SyntaxPreference.
         * Hence we don't need to check it.
         */
		if(((Button) arg0.getSource()).getLabel().equals("Italics"))
			((SyntaxPreference) this.currentPref).toggleItalics();
		else
			((SyntaxPreference) this.currentPref).toggleBold();
	}
}

