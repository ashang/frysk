package frysk.gui.common.prefs;

import org.gnu.gtk.Alignment;
import org.gnu.gtk.CheckButton;
import org.gnu.gtk.ColorButton;
import org.gnu.gtk.HBox;
import org.gnu.gtk.Label;
import org.gnu.gtk.SizeGroup;
import org.gnu.gtk.SizeGroupMode;
import org.gnu.gtk.SpinButton;
import org.gnu.gtk.VBox;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.ColorButtonEvent;
import org.gnu.gtk.event.ColorButtonListener;
import org.gnu.gtk.event.SpinEvent;
import org.gnu.gtk.event.SpinListener;
import org.gnu.gtk.event.ToggleEvent;
import org.gnu.gtk.event.ToggleListener;
import org.gnu.pango.Style;
import org.gnu.pango.Weight;

import frysk.gui.srcwin.prefs.SyntaxPreference;

/**
 * PreferenceEditor allows the user to change the value of a preference.
 *
 */
public class PreferenceEditor extends HBox {

	FryskPreference myPref;
	
	/**
	 * Creates a new Preference editor.
	 * @param pref The preference to edit
	 */
	public PreferenceEditor(FryskPreference pref) {
		super(false, 12);
		
		this.setPreference(pref);
	}
	
	/*
	 * Sets the preference to edit.
	 */
	private void setPreference(FryskPreference pref){
		if(pref instanceof SyntaxPreference)
			setPreference((SyntaxPreference) pref);
		else if(pref instanceof ColorPreference)
			setPreference((ColorPreference) pref);
		else if(pref instanceof IntPreference)
			setPreference((IntPreference) pref);
		else
			setPreference((BooleanPreference) pref);
	}
	
	/*
	 * Provides a spinner box with which the user can edit a 
	 * integer-valued preference.
	 */
	private void setPreference(IntPreference newPref){
		this.myPref = newPref;
		
		final SpinButton button = new SpinButton(newPref.getMinValue(), newPref.getMaxValue(), 1);
		button.setValue(newPref.getCurrentValue());
		button.addListener(new SpinListener() {
			public void spinEvent(SpinEvent arg0) {
				((IntPreference) myPref).setCurrentValue((int) button.getValue());
			}
		});
		
		this.packStart(new Label(newPref.getName()+":"), false, false, 0);
		
		// Push the button to the far right, so that all the buttons will line up
		Alignment align = new Alignment(1.0, 0.0, 0.0, 0.0);
		align.add(button);
		
		this.packStart(align, true, true, 0);
		this.showAll();
	}
	
	/*
	 * Provides a checkbox to edit boolean-valued preferences
	 */
	private void setPreference(BooleanPreference newPref){
		this.myPref = newPref;
		
		final CheckButton button = new CheckButton("", newPref.getCurrentValue());
		button.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent arg0) {
				if(arg0.isOfType(ButtonEvent.Type.CLICK))
					((BooleanPreference) myPref).setCurrentValue(button.getState());
			}
		});
		
		this.packStart(new Label(newPref.getName()+":"), false, false, 0);
		
		// Push the button to the far right.
		Alignment align = new Alignment(1.0, 0.0, 0.0, 0.0);
		align.add(button);
		
		this.packStart(align, true, true, 0);
		this.showAll();
	}
	
	/*
	 * Provides a ColorChooserButton to change the value of color-valued preferences
	 */
	private void setPreference(ColorPreference newPref){
		this.myPref = newPref;
		
		final ColorButton button = new ColorButton(newPref.getCurrentColor());
		button.addListener(new ColorButtonListener() {
			public boolean colorButtonEvent(ColorButtonEvent arg0) {
				((ColorPreference) myPref).setCurrentColor(button.getColor());
				return false;
			}
		});
		
		this.packStart(new Label(newPref.getName()+":"), false, false, 0);
		
		// Push the button all the way to the right
		Alignment align = new Alignment(1.0, 0.0, 0.0, 0.0);
		align.add(button);
		
		this.packStart(align, true, true, 0);
		this.showAll();
	}
	
	/*
	 * Provides a color button as well as bold and italics checkmarks so that
	 * the user can edit the way that syntax highlighting is performed
	 */
	private void setPreference(SyntaxPreference newPref){
		this.myPref = newPref;
		
		SizeGroup sGroup = new SizeGroup(SizeGroupMode.HORIZONTAL);
		
		VBox box = new VBox(false, 6);
		final ColorButton button = new ColorButton(newPref.getCurrentColor());
		button.addListener(new ColorButtonListener() {
			public boolean colorButtonEvent(ColorButtonEvent arg0) {
				((SyntaxPreference) myPref).setCurrentColor(button.getColor());
				return false;
			}
		});
		Label colorLabel = new Label("Color:");
		HBox colorRow = new HBox(false, 6);
		colorRow.packStart(colorLabel, true, true, 0);
		colorRow.packStart(button, false, true, 0);
		sGroup.addWidget(colorRow);
		box.packStart(colorRow);
		
		final CheckButton weight = new CheckButton("Bold", newPref.getCurrentWeight().equals(Weight.BOLD));
		weight.addListener(new ToggleListener() {
			public void toggleEvent(ToggleEvent arg0) {
				((SyntaxPreference) myPref).toggleBold();		
			}
		});
		sGroup.addWidget(weight);
		box.packStart(weight);
		
		final CheckButton style = new CheckButton("Italics", newPref.getCurrentStyle().equals(Style.ITALIC));
		style.addListener(new ToggleListener() {
			public void toggleEvent(ToggleEvent arg0) {
				((SyntaxPreference) myPref).toggleItalics();
			}
		});
		sGroup.addWidget(style);
		box.packStart(style);
		
		Label label = new Label(newPref.getName()+":");
		Alignment align = new Alignment(0.0, 0.0, 0.0, 0.0);
		align.add(label);
		
		this.packStart(align, true, true, 0);
		this.packStart(box, false, false, 0);
		this.showAll();
	}

}
