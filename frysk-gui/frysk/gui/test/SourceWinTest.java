package frysk.gui.test;

import org.gnu.gtk.Gtk;

import frysk.gui.srcwin.SourceWindow;


/**
 * Simple driver program for the source window. Instantiates the window and
 * starts the Gtk loop
 * 
 * @author ajocksch
 *
 */

public class SourceWinTest {

	public static void main(String[] args) {
		Gtk.init(args);
		
		SourceWindow s = new SourceWindow();
		
		Gtk.main();
	}

}
