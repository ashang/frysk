package gcc_4_0_15;
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
		
		Gtk.main();
	}

}
