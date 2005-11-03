/**
 * 
 */
package frysk.gui.srcwin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigInteger;
import java.util.HashMap;

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
				Config.ABS_SOURCEDIR + "/../frysk-gui/frysk/gui/glade"};
		imagePaths = new String[] {Config.PKGDATADIR+"/images",
				Config.ABS_SOURCEDIR + "/../frysk-gui/frysk/gui/images"};
	}
	
	public static void createSourceWindow(Task task){
		SourceWindow s = null;
		
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
			
			s = new SourceWindow(
					 gladePaths,
					imagePaths,
					dom, stack1);
			
		}
		else{
			if(map.containsKey(task)){
				// Do something here to revive the existing window
				System.out.println("Window was already open, refreshing");
			}
			else{
				// Do real stuff here
				System.out.println("Creating new window");
			}
		}
		
		// Store the reference to the 
		map.put(task, s);
	}
}
