package frysk.dom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import lib.dw.DwflLine;
import lib.dw.NoDebugInfoException;

import frysk.dom.cparser.CDTParser;
import frysk.proc.Proc;
import frysk.proc.Task;

public class DOMFactory {

	private static HashMap hashmap = new HashMap();
	
	public static DOMFrysk createDOM(Task task) throws NoDebugInfoException{
		DOMFrysk dom;
		DwflLine line = task.getDwflLineXXX();
		if(line == null)
			throw new NoDebugInfoException("Could not find debug information for task " + task.getName());
		String fullPath = line.getSourceFile();
		String filename = fullPath.substring(fullPath.lastIndexOf("/") + 1);
		String path = fullPath.substring(0, fullPath.lastIndexOf("/"));
		
		System.out.println("Trying to read " + fullPath);
		
		Proc proc = task.getProc();
		
		if(hashmap.containsKey(proc)){
			// retrieve the previously created dom
			dom = (DOMFrysk) hashmap.get(proc);
		}
		else{
			// create a new dom and associate it with the given task
			String taskName = task.getName();
			dom = new DOMFrysk("Task"+taskName.substring(0, taskName.indexOf(" ")));
			dom.addImage(task.getName(), path, path);
		}
		
		DOMSource source = dom.getImage(task.getName()).getSource(filename);
		
		/* If this source file has not previously been incorporated into the
		 * dom, so do now
		 */
		if(source == null){
			DOMImage image = dom.getImage(task.getName());
			source = new DOMSource(filename, path);
			
			// Read the file lines from disk
			// XXX: Remote file access?
			try{
				BufferedReader reader = new BufferedReader(new FileReader(new File(fullPath)));
				int offset = 0;
				int lineNum = 0;
				
				while(reader.ready()){
					String text = reader.readLine();
					// XXX: detect executable lines?
					DOMLine l = new DOMLine(lineNum++, text+"\n", offset, false, false, Long.parseLong("deadbeef", 16));
					source.addLine(l);
					
					offset += text.length() + 1;
				}
			}
			catch (FileNotFoundException e){
				//XXX: bork?
				e.printStackTrace();
			}
			catch (IOException e2){
				//XXX: bork?
				e2.printStackTrace();
			}
			
			image.addSource(source);
			
			// Parse the file and populate the DOM
			StaticParser parser = new CDTParser();
			try{
				parser.parse(source, image);
			}
			catch (IOException e){
				//XXX: bork?
				e.printStackTrace();
			}
		}
		
		hashmap.put(proc, dom);
		
		return dom;
	}
	
}
