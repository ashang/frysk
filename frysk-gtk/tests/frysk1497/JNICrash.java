package frysk1497;
import org.gnu.gtk.Gtk;
import org.gnu.gtk.TextBuffer;

public class JNICrash {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gtk.init(args);
		
		TextBuffer b = new TextBuffer();
		b.insertText("This is a test\nThat spans multiple lines");
		b.createMark("TestMark", b.getLineIter(1), true);
		b.deleteText(b.getStartIter(), b.getEndIter());
		b.insertText("This is another test\nThat will hopefully break");
		b.createMark("TestMark", b.getLineIter(1), true);
		
		// We don't want to enter the main loop, either it will die by this point or it won't
	}

}
