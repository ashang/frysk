package frysk.gui;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.gnu.glade.GladeXMLException;
import org.gnu.glade.LibGlade;
import org.gnu.gtk.CheckButton;
import org.gnu.gtk.ColorButton;
import org.gnu.gtk.Gtk;
import org.gnu.gtk.HBox;
import org.gnu.gtk.HScale;
import org.gnu.gtk.SizeGroup;
import org.gnu.gtk.SizeGroupMode;
import org.gnu.gtk.VBox;
import org.gnu.gtk.Viewport;
import org.gnu.gtk.event.RangeEvent;
import org.gnu.gtk.event.RangeListener;

import frysk.gui.common.IconManager;
import frysk.gui.common.Messages;

public class DummyDebugResults {

	private static final String BASE_PATH = "frysk/gui/";
    private static final String GLADE_PKG_PATH = "glade/";
	
    private LibGlade glade;
    
	public DummyDebugResults(String[] args, String[] gladep, String[] imaged,
			String[] messaged, String[] testfiled){
		
		Gtk.init(args);
		
		IconManager.setImageDir(imaged);
		IconManager.loadIcons();
		IconManager.useSmallIcons();
			
		Messages.setBundlePaths(messaged);
			
		for(int i = 0; i < gladep.length; i++){
			try {
				System.out.println("Trying " + gladep[i]);
				glade = new LibGlade(gladep[i] + "/debugresultswindow.glade", this);
			} catch (GladeXMLException e) {
				continue;
			} catch (FileNotFoundException e) {
				continue;
			} catch (IOException e) {
				continue;
			}
			
			if(glade != null)
				break;
		}
		
		if(glade == null)
			System.exit(1);		
		
		Viewport vp = (Viewport) this.glade.getWidget("observerSelectViewport");
		
		SizeGroup checkSize = new SizeGroup(SizeGroupMode.HORIZONTAL);
		
		VBox box = new VBox(true, 6);
		for(int i = 0; i < 10; i++){
			HBox box2 = new HBox(false, 12);
			CheckButton button = new CheckButton("Observer "+ i, true);
			checkSize.addWidget(button);
			box2.packStart(button, false, false, 0);
			box2.packStart(new ColorButton(), false, false, 0);
			box.packStart(box2);
		}
		box.showAll();
		vp.add(box);
		
		vp = (Viewport) this.glade.getWidget("timelineViewport");
		final DebugHistory hist = new DebugHistory(0);
		vp.add(hist);
		vp.showAll();
		
		final HScale scale = (HScale) this.glade.getWidget("thresholdBar");
		scale.addListener(new RangeListener() {
			public void rangeEvent(RangeEvent arg0) {
				hist.setThreshold((int)scale.getValue());
			}
		});
		
		Gtk.main();
	}
	
	public static void main(String[] args) {
		DummyDebugResults res = new DummyDebugResults(args, new String[] {
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
		res.toString();
	}
}
