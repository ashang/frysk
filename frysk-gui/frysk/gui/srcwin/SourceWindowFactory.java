/**
 * 
 */
package frysk.gui.srcwin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigInteger;
import java.util.HashMap;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;

import frysk.gui.srcwin.dom.DOMFrysk;
import frysk.gui.srcwin.dom.DOMSource;
import frysk.gui.srcwin.dom.DOMTestGUIBuilder;
import frysk.proc.Task;
import frysk.Config;

/**
 * @author ajocksch
 *
 */
public class SourceWindowFactory {

	private static String dummyPath;
	
	private static String[] gladePaths;
	
	private static String[] imagePaths;

	private static HashMap map;
	
	public static void setDummyPath(String path){
		dummyPath = path;
	}
	
	public static void clearDummyPath(){
		dummyPath = "";
	}
	
	public static void setGladePaths(String[] paths){
		gladePaths = paths;
	}
	
	public static void setImagePaths(String[] path){
		imagePaths = path;
	}
	
	static{
		map = new HashMap();
		dummyPath = Config.PKGDATADIR + "/samples";
		gladePaths = new String[] {Config.GLADEDIR, 
				Config.ABS_SRCDIR + "/../frysk-gui/frysk/gui/glade"};
		imagePaths = new String[] {Config.PKGDATADIR+"/images",
				Config.ABS_SRCDIR + "/../frysk-gui/frysk/gui/images"};
	}
	
	public static void createSourceWindow(Task task){
		SourceWindow s = null;

		if(map.containsKey(task)){
			// Do something here to revive the existing window
			System.out.println("Window was already open, refreshing");
			s = (SourceWindow) map.get(task);
			s.grabFocus();
		}
		else{
			// Do real stuff here
			System.out.println("Creating new window");
			
			if(!dummyPath.equals("")){
				DOMFrysk dom = DOMTestGUIBuilder.makeTestDOM();
				DOMSource source = dom.getImage("test6").getSource("test3.cpp");
				source.setFileName("test3.cpp");
				source.setFilePath(dummyPath);
				BufferedReader reader = null;
				int line = 1;
				int offset = 0;
				try{
					reader = new BufferedReader(new FileReader(new File(dummyPath + "/test3.cpp")));
					while(reader.ready()){
						String text = reader.readLine()+"\n";
						System.out.print(text);
						source.addLine(line++, text, !text.startsWith("//"), false, false, offset, BigInteger.valueOf(255));
						offset += text.length();
					}
				}
				catch(Exception e){
					
				}
				StackLevel stack1 = new StackLevel(source, 2);
				
				source = dom.getImage("test6").getSource("test4.cpp");
				source.setFileName("test4.cpp");
				source.setFilePath(dummyPath);
				try{
					reader = new BufferedReader(new FileReader(new File(dummyPath + "/test4.cpp")));
					line = 1;
					offset = 0;
					while(reader.ready()){
						String text = reader.readLine()+"\n";
						source.addLine(line++, text, !text.startsWith("//"), false, false, offset, BigInteger.valueOf(255));
						offset += text.length();
					}
				}
				catch (Exception e){
					
				}
				StackLevel stack2 = new StackLevel(source, 2);
				stack1.addNextScope(stack2);
				
				source = dom.getImage("test6").getSource("test5.cpp");
				source.setFileName("test5.cpp");
				source.setFilePath(dummyPath);
				try{
					reader = new BufferedReader(new FileReader(new File(dummyPath + "/test5.cpp")));
					line = 1;
					offset = 0;
					while(reader.ready()){
						String text = reader.readLine()+"\n";
						source.addLine(line++, text, !text.startsWith("//"), false, false, offset, BigInteger.valueOf(255));
						offset += text.length();
					}
				}
				catch (Exception e){
					
				}
				StackLevel stack3 = new StackLevel(source, 2);
				stack2.addNextScope(stack3);
				
				source = dom.getImage("test6").getSource("test6.cpp");
				source.setFileName("test6.cpp");
				source.setFilePath(dummyPath);
				StackLevel stack4 = new StackLevel(source, 10);
				stack3.addNextScope(stack4);
				
				LibGlade glade = null;
				
				for(int i = 0; i < gladePaths.length; i++){
					try{
						glade = new LibGlade(gladePaths[i]+"/"+SourceWindow.GLADE_FILE, null);
					}
					catch (Exception e){
						// If we don't find the glade file, continue looking
						continue;
					}
					
					// If we've found it, break
					break;
				}
				
				// If we don't have a glade file by this point, bail
				if(glade == null){
					System.err.println("Could not file source window glade file in path "+gladePaths[gladePaths.length - 1] +"! Exiting.");
					return;
				}
				
				s = new SourceWindow(
						 glade,
						imagePaths,
						dom, stack1);
				s.setMyTask(task);
				s.addListener(new SourceWinListener());
				
			}
			else{
				
			}
			// Store the reference to the 
			map.put(task, s);
		}
	}
		
	static class SourceWinListener implements LifeCycleListener{

		public void lifeCycleEvent(LifeCycleEvent arg0) {}

		public boolean lifeCycleQuery(LifeCycleEvent arg0) {
			
            /*
             * If the window is closing we want to remove it and it's
             * task from the map, so that we know to create a new 
             * instance next time
             */
			if(arg0.isOfType(LifeCycleEvent.Type.DELETE)){
				if(map.containsValue(arg0.getSource())){
					SourceWindow s = (SourceWindow) arg0.getSource();
                    map.remove(s.getMyTask());
				}
			}
			
			return false;
		}
		
	}
}
