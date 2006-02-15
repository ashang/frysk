package frysk.gui;

import org.gnu.gtk.Gtk;

import frysk.gui.common.IconManager;
import frysk.gui.common.Messages;
import frysk.gui.common.dialogs.DialogManager;
import frysk.gui.srcwin.SourceWindowFactory;
import frysk.proc.DummyProc;
import frysk.proc.DummyTask;
import frysk.proc.Manager;

public class DummySourceWindow {

	private static final String BASE_PATH = "frysk/gui/";
    private static final String GLADE_PKG_PATH = "glade/";
	
	public static void DummySourceWin(String[] args, String[] gladep, String[] imaged,
			String[] messaged, String[] testfiled){
		
		Gtk.init(args);
		
		IconManager.setImageDir(imaged);
		IconManager.loadIcons();
		IconManager.useSmallIcons();
			
		Messages.setBundlePaths(messaged);
			
		SourceWindowFactory.setTestFilesPath(testfiled);
		SourceWindowFactory.setGladePaths(gladep);
		
		DummyProc proc = new DummyProc();
		DummyTask task = new DummyTask(proc);
		
		SourceWindowFactory.createSourceWindow(task);
		
		Gtk.main();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DummySourceWindow.DummySourceWin (args, new String[] {
			     GLADE_PKG_PATH,
			     BASE_PATH + GLADE_PKG_PATH,
			     // Check both relative ...
			     Build.SRCDIR + "/" + BASE_PATH + GLADE_PKG_PATH,
			     // ... and absolute.
			     Build.ABS_SRCDIR + "/" + BASE_PATH + GLADE_PKG_PATH,
			 },
			 new String[] {
			     Build.ABS_SRCDIR + "/" + BASE_PATH + "images/"
			 }, 
			 new String[] {
			     "./common", Build.SRCDIR + "/" + BASE_PATH + "common/",
			     Build.ABS_SRCDIR + "/" + BASE_PATH + "common/"
			 },
			 new String[] {
			     "./srcwin/testfiles", Build.SRCDIR + "/" + BASE_PATH + "srcwin/testfiles",
			     Build.ABS_SRCDIR + "/" + BASE_PATH + "srcwin/testfiles"
			 });
		
	}

}
