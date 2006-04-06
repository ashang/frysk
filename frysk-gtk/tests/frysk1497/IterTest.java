package frysk1497;

import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.Gtk;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.TreeIter;

public class IterTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gtk.init(args);
		
		DataColumn[] cols = new DataColumn[] {new DataColumnString()};
		
		ListStore store = new ListStore(cols);
		
		for(int i = 0; i < 10; i++){
			TreeIter iter = store.appendRow();
			store.setValue(iter, (DataColumnString) cols[0], ""+i);
		}
		
		TreeIter iter = store.getFirstIter();
		
		while(iter != null)
			iter = iter.getNextIter();
	}

}
