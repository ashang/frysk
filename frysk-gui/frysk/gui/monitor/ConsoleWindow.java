package frysk.gui.monitor;

import org.gnu.gtk.Window;
import org.gnu.gnomevte.Terminal;

public class ConsoleWindow extends Window {
	private Terminal term;
	
	public ConsoleWindow() {
		term = Terminal.terminalAndShell();
		this.add(term);
		this.showAll();
		// term.show();
	}
}
