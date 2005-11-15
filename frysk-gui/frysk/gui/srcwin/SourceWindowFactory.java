/**
 * 
 */
package frysk.gui.srcwin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import frysk.gui.srcwin.dom.DOMFrysk;
import frysk.gui.srcwin.dom.DOMImage;
import frysk.gui.srcwin.dom.DOMSource;
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
	
	/**
	 * Sets the path to use to look for the test files to load. If the
	 * path is not set it will look for the information from the task,
	 * otherwise it will load the test files
	 * @param path The directory where the test files are located
	 */
	public static void setDummyPath(String path){
		dummyPath = path;
	}
	
	/**
	 * Clears the test file path, so that all future SourceWindows will be
	 * launched using realy information from the Task
	 */
	public static void clearDummyPath(){
		dummyPath = "";
	}
	
	/**
	 * Sets the paths to look in to find the .glade files needed for the gui
	 * @param paths The possible locations of the gui glade files.
	 */
	public static void setGladePaths(String[] paths){
		gladePaths = paths;
	}
	
	/**
	 * Sets the paths to look in to find the image files needed for the gui
	 * @param path The possible locations of the image files.
	 */
	public static void setImagePaths(String[] path){
		imagePaths = path;
	}
	
	static{
		map = new HashMap();
		/*
		 * TODO: Make it so that the dummyPath is left empty by default once we can read
		 * the required info from Tasks
		 */ 
		dummyPath = Config.ABS_SRCDIR + "/../frysk-gui/frysk/gui/srcwin/testfiles";
		gladePaths = new String[] {Config.GLADEDIR, 
				Config.ABS_SRCDIR + "/../frysk-gui/frysk/gui/glade"};
		imagePaths = new String[] {Config.PKGDATADIR+"/images",
				Config.ABS_SRCDIR + "/../frysk-gui/frysk/gui/images"};
	}
	
	/**
	 * Creates a new source window using the given task. The SourceWindows correspond
	 * to tasks in a 1-1 relationship, so if you try to launch a SourceWindow for a Task
	 * and an existing window has already been created, that one will be brought to the
	 * forefront rather than creating a new window.
	 * 
	 * @param task The Task to open a SourceWindow for.
	 */
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
				DOMFrysk dom = new DOMFrysk(new Document(new Element("DOM_test")));
                dom.addImage("test", dummyPath, dummyPath);
                DOMImage image = dom.getImage("test");
                for(int i = 3; i <= 6; i++)
                    image.addSource("test"+i+".cpp", dummyPath);
                
				DOMSource source = image.getSource("test3.cpp");
				BufferedReader reader = null;
				int line = 1;
				int offset = 0;
                int[] execLines = new int[] {0,0,0,0,1,0,0,0,1,0};
				try{
					reader = new BufferedReader(new FileReader(new File(dummyPath + "/test3.cpp")));
					while(reader.ready()){
						String text = reader.readLine()+"\n";
						source.addLine(line, text, !text.startsWith("//"), false, false, offset, BigInteger.valueOf(255));
						if(execLines[line-1] == 1)
                            source.getLine(line++).setExecutable(true);
                        else
                            source.getLine(line++).setExecutable(false);
						offset += text.length();
					}
				}
				catch(Exception e){
					
				}
				StackLevel stack1 = new StackLevel(source, 5);
				
				source = image.getSource("test4.cpp");
				try{
					reader = new BufferedReader(new FileReader(new File(dummyPath + "/test4.cpp")));
					line = 1;
					offset = 0;
                    execLines = new int[] {0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0};
					while(reader.ready()){
						String text = reader.readLine()+"\n";
						source.addLine(line, text, !text.startsWith("//"), false, false, offset, BigInteger.valueOf(255));
                        if(execLines[line-1] == 1)
                            source.getLine(line++).setExecutable(true);
                        else
                            source.getLine(line++).setExecutable(false);
						offset += text.length();
					}
				}
				catch (Exception e){
					
				}
				StackLevel stack2 = new StackLevel(source, 3);
				stack1.addNextScope(stack2);
				
				source = image.getSource("test5.cpp");
				try{
					reader = new BufferedReader(new FileReader(new File(dummyPath + "/test5.cpp")));
					line = 1;
					offset = 0;
                    execLines = new int[] {0,0,1,1,0};
					while(reader.ready()){
						String text = reader.readLine()+"\n";
						source.addLine(line, text, !text.startsWith("//"), false, false, offset, BigInteger.valueOf(255));
                        if(execLines[line-1] == 1)
                            source.getLine(line++).setExecutable(true);
                        else
                            source.getLine(line++).setExecutable(false);
						offset += text.length();
					}
				}
				catch (Exception e){
					
				}
				StackLevel stack3 = new StackLevel(source, 3);
				stack2.addNextScope(stack3);
				
				source = image.getSource("test6.cpp");
                try{
                    reader = new BufferedReader(new FileReader(new File(dummyPath + "/test6.cpp")));
                    line = 1;
                    offset = 0;
                    execLines = new int[] {0,0,0,1,1,1,0,1,0,0,0,1,0,1,1,1,1,0};
                    while(reader.ready()){
                        String text = reader.readLine()+"\n";
                        source.addLine(line, text, !text.startsWith("//"), false, false, offset, BigInteger.valueOf(255));
                        if(execLines[line-1] == 1)
                            source.getLine(line).setExecutable(true);
                        else
                            source.getLine(line).setExecutable(false);
                        
                        if(line++ == 15){
                        	String lineText = source.getLine(15).getText();
                        	source.getLine(15).addInlineInst("min", lineText.indexOf("min"), 3);
                        }
                        
                        offset += text.length();
                    }
                }
                catch (Exception e){
                    
                }
                
                String[] funcLines = new String[6];
                for(int i = 0; i < funcLines.length; i++)
                	funcLines[i] = source.getLine(i + 4).getText();
                    
                image.addFunction("min", source.getFileName(),
                		4, 4 + funcLines.length,
                		source.getLine(4).getOffset(), 
                		source.getLine(9).getOffset()+source.getLine(9).getLength());
                
				StackLevel stack4 = new StackLevel(source, 15);
				stack3.addNextScope(stack4);
				
				LibGlade glade = null;
                
                // Look for the right path to load the glade file from
                int i = 0;
				for(; i < gladePaths.length; i++){
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
				
				printDOM(dom);
				
				s = new SourceWindow(
						 glade,
                         gladePaths[i],
						imagePaths,
						dom, stack1);
				s.setMyTask(task);
				s.addListener(new SourceWinListener());
				
			}
			else{
				
			}
			// Store the reference to the source window
			map.put(task, s);
		}
	}
	
	/**
	 * Print out the DOM in XML format
	 */
	public static void printDOM(DOMFrysk dom) {
		try {
			XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
			outputter.output(dom.getDOMFrysk(), System.out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
	/*
	 * The responsability of this class that whever a SourceWindow is closed the
	 * corresponding task is removed from the HashMap. This tells createSourceWindow
	 * to create a new window the next time that task is passed to it.
	 */
	private static class SourceWinListener implements LifeCycleListener{

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
