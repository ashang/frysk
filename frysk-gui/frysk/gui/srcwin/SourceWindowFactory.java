// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

package frysk.gui.srcwin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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

import frysk.dom.DOMFrysk;
import frysk.dom.DOMImage;
import frysk.dom.DOMInlineInstance;
import frysk.dom.DOMSource;
import frysk.proc.Task;

/**
 * SourceWindow factory is the interface through which all SourceWindow objects in frysk
 * should be created. It takes care of setting paths to resource files as well as
 * making sure that at most one window is opened per Task.
 * 
 * @author ajocksch
 *
 */
public class SourceWindowFactory {

	private static String[] testFilesPath;
	
	private static String[] gladePaths;

	private static HashMap map;
	
	/**
	 * Sets the path to use to look for the test files to load. If the
	 * path is not set it will look for the information from the task,
	 * otherwise it will load the test files
	 * @param path The directory where the test files are located
	 */
	public static void setTestFilesPath(String[] path){
		testFilesPath = path;

	}
	
	/**
	 * Clears the test file path, so that all future SourceWindows will be
	 * launched using realy information from the Task
	 */
	public static void clearTestFilesPath(){
		testFilesPath = null;
	}
	
	/**
	 * Sets the paths to look in to find the .glade files needed for the gui
	 * @param paths The possible locations of the gui glade files.
	 */
	public static void setGladePaths(String[] paths){
		gladePaths = paths;
	}
	
	static{
		map = new HashMap();
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

//		task.requestAddAttachedObserver(new TaskObserver.Attached() {
//			
//			public void deletedFrom(Object observable) {
//				// TODO Auto-generated method stub
//			}
//		
//			public void addFailed(Object observable, Throwable w) {
//				// TODO Auto-generated method stub
//			}
//		
//			public void addedTo(Object observable) {
//				// TODO Auto-generated method stub
//			}
//		
//			public Action updateAttached(Task task) {
//				// TODO Auto-generated method stub
//				System.out.println("Blocking");
//				return Action.BLOCK;
//			}
//		
//		});
		
		if(map.containsKey(task)){
			// Do something here to revive the existing window
			s = (SourceWindow) map.get(task);
			s.grabFocus();
		}
		else{
			// Try to find the correct path to load the test files from
			int index = 0;
			String finalTestPath = "";
			while(index < testFilesPath.length){
				File test = new File(testFilesPath[index] + "/test2.cpp");
				if(test.exists()){
					finalTestPath = testFilesPath[index];
					break;
				}
				
				index++;
			}
			
			if(finalTestPath.equals("")){
				System.err.println("Could not load test files from provided paths!");
				System.exit(1);
			}
			
			if(!testFilesPath.equals("")){
				DOMFrysk dom = new DOMFrysk(new Document(new Element("DOM_test")));
                dom.addImage("test", finalTestPath, finalTestPath);
                DOMImage image = dom.getImage("test");
                for(int i = 2; i <= 6; i++)
                    image.addSource("test"+i+".cpp", finalTestPath);
                
                DOMSource source = image.getSource("test2.cpp");
                BufferedReader reader = null;
				int line = 1;
				int offset = 0;
				int[] execLines = new int[] {0,0,0,0,0,0,0,0,0,1,0,0,0,1,0,0};
					try {
						reader = new BufferedReader(new FileReader(new File(finalTestPath + "/test2.cpp")));
					} catch (FileNotFoundException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
					try {
						while(reader.ready()){
							String text = reader.readLine()+"\n";
							source.addLine(line, text, !text.startsWith("//"), false, offset, BigInteger.valueOf(255));
							if(execLines[line-1] == 1)
						        source.getLine(line).setExecutable(true);
						    else
						        source.getLine(line).setExecutable(false);
							
							if(line++ == 12){
								String lineText = source.getLine(12).getText();
						    	source.getLine(12).addInlineInst("bar", lineText.indexOf("bar"), 3, 9);
						    	DOMInlineInstance instance = source.getLine(12).getInlineInst("bar");
						    	instance.addInlineInst("baz",10,3,22);
						    	instance = instance.getInlineInstance();
						    	instance.addInlineInst("foobar",10,6,4);
							}
							
							offset += text.length();
						}
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				
           
				
				String[] funcLines = new String[6];
                for(int i = 0; i < funcLines.length; i++)
                		funcLines[i] = source.getLine(i + 8).getText();
                    
                image.addFunction("foo", source.getFileName(),
                		8, 8 + funcLines.length,
                		source.getLine(8).getOffset(), 
                		source.getLine(13).getOffset()+source.getLine(13).getLength());
			
                StackLevel stack1 = new StackLevel(image.getFunction("foo"), 12);
                
                
				source = image.getSource("test3.cpp");
				line = 1;
				offset = 0;
                execLines = new int[] {0,0,0,0,1,0,0,0,1,0};
				try{
					reader = new BufferedReader(new FileReader(new File(finalTestPath + "/test3.cpp")));
					while(reader.ready()){
						String text = reader.readLine()+"\n";
						source.addLine(line, text, !text.startsWith("//"), false, offset, BigInteger.valueOf(255));
						if(execLines[line-1] == 1)
                            source.getLine(line++).setExecutable(true);
                        else
                            source.getLine(line++).setExecutable(false);
//						
//						if(line++ == 9){
//							String lineText = source.getLine(9).getText();
//                        	source.getLine(9).addInlineInst("baz", lineText.indexOf("baz"), 3, 22);
//                        	DOMInlineInstance instance = source.getLine(9).getInlineInst("baz");
//                        	instance.addInlineInst("foobar",10,6,4);
//						}
						
						offset += text.length();
					}
				}
				catch(Exception e){
					
				}
				
				funcLines = new String[6];
                for(int i = 0; i < funcLines.length; i++)
                	funcLines[i] = source.getLine(i + 5).getText();
                    
                image.addFunction("bar", source.getFileName(),
                		5, 5 + funcLines.length,
                		source.getLine(5).getOffset(), 
                		source.getLine(10).getOffset()+source.getLine(10).getLength());
				
				source = image.getSource("test4.cpp");
				try{
					reader = new BufferedReader(new FileReader(new File(finalTestPath + "/test4.cpp")));
					line = 1;
					offset = 0;
                    execLines = new int[] {0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0};
					while(reader.ready()){
						String text = reader.readLine()+"\n";
						source.addLine(line, text, !text.startsWith("//"), false, offset, BigInteger.valueOf(255));
                        if(execLines[line-1] == 1)
                            source.getLine(line++).setExecutable(true);
                        else
                            source.getLine(line++).setExecutable(false);
						offset += text.length();
					}
				}
				catch (Exception e){
					
				}
//				StackLevel stack2 = new StackLevel(source, 3);
//				stack1.addNextScope(stack2);
				
				funcLines = new String[21];
                for(int i = 0; i < funcLines.length; i++)
                	funcLines[i] = source.getLine(i + 3).getText();
                    
                image.addFunction("baz", source.getFileName(),
                		3, 3 + funcLines.length,
                		source.getLine(3).getOffset(), 
                		source.getLine(23).getOffset()+source.getLine(23).getLength());
				
				source = image.getSource("test5.cpp");
				try{
					reader = new BufferedReader(new FileReader(new File(finalTestPath + "/test5.cpp")));
					line = 1;
					offset = 0;
                    execLines = new int[] {0,0,1,1,0};
					while(reader.ready()){
						String text = reader.readLine()+"\n";
						source.addLine(line, text, !text.startsWith("//"), false, offset, BigInteger.valueOf(255));
                        if(execLines[line-1] == 1)
                            source.getLine(line++).setExecutable(true);
                        else
                            source.getLine(line++).setExecutable(false);
						offset += text.length();
					}
				}
				catch (Exception e){
					
				}
//				StackLevel stack3 = new StackLevel(source, 3);
//				stack1.addNextScope(stack3);
				
				funcLines = new String[3];
                for(int i = 0; i < funcLines.length; i++)
                	funcLines[i] = source.getLine(i + 3).getText();
                    
                image.addFunction("foobar", source.getFileName(),
                		3, 3 + funcLines.length,
                		source.getLine(3).getOffset(), 
                		source.getLine(5).getOffset()+source.getLine(5).getLength());
				
				source = image.getSource("test6.cpp");
                try{
                    reader = new BufferedReader(new FileReader(new File(finalTestPath + "/test6.cpp")));
                    line = 1;
                    offset = 0;
                    execLines = new int[3000];
                    while(reader.ready()){
                        String text = reader.readLine()+"\n";
                        source.addLine(line, text, !text.startsWith("//"), false, offset, BigInteger.valueOf(255));
                        if(execLines[line-1] == 1)
                            source.getLine(line++).setExecutable(true);
                        else
                            source.getLine(line++).setExecutable(false);
                        
                        offset += text.length();
                    }
                }
                catch (Exception e){
                	// TODO: What to do if the load don't work?
                }
                
                funcLines = new String[6];
                for(int i = 0; i < funcLines.length; i++)
                		funcLines[i] = source.getLine(i + 4).getText();
                    
                image.addFunction("min", source.getFileName(),
                		10, 10 + funcLines.length,
                		source.getLine(10).getOffset(), 
                		source.getLine(10 + funcLines.length).getOffset()+source.getLine(10 + funcLines.length).getLength());
                
				StackLevel stack4 = new StackLevel(image.getFunction("min"), 21);
				stack1.addNextScope(stack4);
				
				LibGlade glade = null;
                
                // Look for the right path to load the glade file from
                int i = 0;
				for(; i < gladePaths.length; i++){
					try{
						glade = new LibGlade(gladePaths[i]+"/"+SourceWindow.GLADE_FILE, null);
					}
					catch (Exception e){
						if (i < gladePaths.length -1 )
							// If we don't find the glade file, look at the next file
							continue;
						else{
							e.printStackTrace();
							System.exit(1);
						}
							
					}
					
					// If we've found it, break
					break;
				}
				
				// If we don't have a glade file by this point, bail
				if(glade == null){
					System.err.println("Could not file source window glade file in path "+gladePaths[gladePaths.length - 1] +"! Exiting.");
					return;
				}
				
//				printDOM(dom);
				
				s = new SourceWindow(
						 glade,
                         gladePaths[i],
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
