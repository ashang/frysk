package frysk.gui.common;

import org.gnu.gdk.Pixbuf;
import org.gnu.gdk.PixbufAnimation;
import org.gnu.gtk.IconFactory;
import org.gnu.gtk.IconSet;
import org.gnu.gtk.IconSize;
import org.gnu.gtk.IconSource;

public class IconManager {

	//	 Image files - search bar
	public static final String FIND_NEXT_PNG = "findNext.png"; //$NON-NLS-1$
	public static final String FIND_PREV_PNG = "findPrev.png"; //$NON-NLS-1$
	public static final String FIND_GO_PNG = "findGo.png"; //$NON-NLS-1$
	public static final String HIGHLIGHT_PNG = "highlight.png"; //$NON-NLS-1$
	// Names of the image files - Toolbar
	private static final String RUN_PNG = "stock-execute.png";
	private static final String STOP_PNG = "stop.png";
	private static final String STEP_PNG = "stepI.png"; //$NON-NLS-1$
	private static final String NEXT_PNG = "nextI.png"; //$NON-NLS-1$
	private static final String FINISH_PNG = "finish.png"; //$NON-NLS-1$
	private static final String CONTINUE_PNG = "continue.png"; //$NON-NLS-1$
	private static final String STEP_AI_PNG = "stepAI.png"; //$NON-NLS-1$
	private static final String NEXT_AI_PNG = "nextAI.png"; //$NON-NLS-1$
	private static final String DOWN_PNG = "down.png"; //$NON-NLS-1$
	private static final String UP_PNG = "up.png"; //$NON-NLS-1$
	private static final String BOTTOM_PNG = "bottom.png"; //$NON-NLS-1$
	
	// Tray Icon prefix
	private static final String TRAY_PREFIX = "fryskTrayIcon";
	
	private static IconFactory[] factories;
	
	private static String[] sizeDirs = new String[] {"16", "24", "32"};
	
	private static String[] IMAGES_DIR;
	
	public static PixbufAnimation anim;
	
	public static Pixbuf windowIcon;
	
	public static void setImageDir(String[] path){
		IMAGES_DIR = path;
	}
	
	public static void loadIcons(){
		factories = new IconFactory[2];
		factories[0] = new IconFactory(); // "small" icons
		factories[1] = new IconFactory(); // "large" icons
		
		
		FryskIconSet[] runSet = new FryskIconSet[] {new FryskIconSet("frysk-run"), new FryskIconSet("frysk-run")};
		FryskIconSet[] stopSet = new FryskIconSet[] {new FryskIconSet("frysk-stop"), new FryskIconSet("frysk-stop")};
		FryskIconSet[] nextSet = new FryskIconSet[] {new FryskIconSet("frysk-next"), new FryskIconSet("frysk-next")};
		FryskIconSet[] nextAISet = new FryskIconSet[] {new FryskIconSet("frysk-nextAI"), new FryskIconSet("frysk-nextAI")};
		FryskIconSet[] stepSet = new FryskIconSet[] {new FryskIconSet("frysk-step"), new FryskIconSet("frysk-step")};
		FryskIconSet[] stepAISet = new FryskIconSet[] {new FryskIconSet("frysk-stepAI"), new FryskIconSet("frysk-stepAI")};
		FryskIconSet[] continueSet = new FryskIconSet[] {new FryskIconSet("frysk-continue"), new FryskIconSet("frysk-continue")};
		FryskIconSet[] finishSet = new FryskIconSet[] {new FryskIconSet("frysk-finish"), new FryskIconSet("frysk-finish")};
		FryskIconSet[] downSet = new FryskIconSet[] {new FryskIconSet("frysk-down"), new FryskIconSet("frysk-down")};
		FryskIconSet[] upSet = new FryskIconSet[] {new FryskIconSet("frysk-up"), new FryskIconSet("frysk-up")};
		FryskIconSet[] bottomSet = new FryskIconSet[] {new FryskIconSet("frysk-bottom"), new FryskIconSet("frysk-bottom")};
		
		for(int i = 0; i < IMAGES_DIR.length; i++){
			try {
				for(int j = 0; j < sizeDirs.length - 1; j++){
					addIconSet(runSet[j], IMAGES_DIR[i], j, RUN_PNG);
					addIconSet(stopSet[j], IMAGES_DIR[i], j, STOP_PNG);					
					addIconSet(nextSet[j], IMAGES_DIR[i], j, NEXT_PNG);
					addIconSet(nextAISet[j], IMAGES_DIR[i], j, NEXT_AI_PNG);
					addIconSet(stepSet[j], IMAGES_DIR[i], j, STEP_PNG);
					addIconSet(stepAISet[j], IMAGES_DIR[i], j, STEP_AI_PNG);
					addIconSet(continueSet[j], IMAGES_DIR[i], j, CONTINUE_PNG);
					addIconSet(finishSet[j], IMAGES_DIR[i], j, FINISH_PNG);
					addIconSet(downSet[j], IMAGES_DIR[i], j, DOWN_PNG);
					addIconSet(upSet[j], IMAGES_DIR[i], j, UP_PNG);
					addIconSet(bottomSet[j], IMAGES_DIR[i], j, BOTTOM_PNG);
					
					// The only other image we need is the highlight image
					IconSet set = new IconSet();
					IconSource source = new IconSource();
					source.setFilename(IMAGES_DIR[i] + "/" + HIGHLIGHT_PNG);
					source.setSize(IconSize.BUTTON);
					set.addSource(source);
					factories[j].addIconSet("frysk-highlight", set);
				}
				
				// Load the tray icons
				for(int k = 1; k <= 24; k++){
					IconSet set = new IconSet();
					IconSource source = new IconSource();
					source.setFilename(IMAGES_DIR[i] + "/icon/" + TRAY_PREFIX +
							(k < 10 ? "0" + k : "" + k) + 
							".png");
					
					if(k == 24){
						windowIcon = new Pixbuf(IMAGES_DIR[i] + "/icon/" + TRAY_PREFIX + k + ".png");
					}
					
					source.setSize(IconSize.BUTTON);
					set.addSource(source);
					factories[0].addIconSet("frysk-tray-" + k, set);
					factories[1].addIconSet("frysk-tray-" + k, set);
				}
				
				anim = new PixbufAnimation(IMAGES_DIR[i] + "/" + "fryskTrayIcon.gif");
				
			} catch (Exception e){
				if(i == IMAGES_DIR.length - 1){
					System.err.println("Error loading images on path " + IMAGES_DIR[i]+"! Exiting");
					e.printStackTrace();
					System.exit(1);
				}
				
				continue;
			}	
			
			break;
		}
		
		factories[0].addDefault();
	}

	public static void useSmallIcons(){
		factories[1].removeDefault();
		factories[0].addDefault();
	}
	
	public static void useLargeIcons(){
		factories[0].removeDefault();
		factories[1].addDefault();
	}
	
	private static void addIconSet(FryskIconSet set, String dir, int index, String iconName)
			throws Exception{
		set.addIcon(new Pixbuf(dir + "/" + sizeDirs[index] + "/" + iconName),
				IconSize.SMALL_TOOLBAR);
		set.addIcon(new Pixbuf(dir + "/" + sizeDirs[index] + "/" + iconName),
				IconSize.MENU);
		set.addIcon(new Pixbuf(dir + "/" + sizeDirs[index+1] + "/" + iconName),
				IconSize.LARGE_TOOLBAR);
		set.addIcon(new Pixbuf(dir + "/" + sizeDirs[index+1] + "/" + iconName),
				IconSize.BUTTON);
		set.addToFactory(factories[index]);
	}
}
