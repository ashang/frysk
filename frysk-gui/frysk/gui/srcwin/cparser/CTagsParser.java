/**
 * 
 */
package frysk.gui.srcwin.cparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.gnu.gtk.TextIter;

import frysk.gui.srcwin.SourceBuffer;
import frysk.gui.srcwin.StaticParser;
import frysk.gui.srcwin.Variable;


/**
 * @author ajocksch
 *
 */
public class CTagsParser implements StaticParser {
	
	/* (non-Javadoc)
	 * @see frysk.gui.srcwin.StaticParser#parse(java.lang.String, com.redhat.fedora.frysk.gui.srcwin.SourceBuffer)
	 */
	public void parse(String filename, SourceBuffer buffer) throws IOException {
		String[] command = new String[7];
		command[0] = "ctags";
		command[1] = "--fields=+KSn";
		command[2] = "-uV";
		command[3] = "--c-kinds=+lxp";
		command[4] = "--file-scope=yes";
		command[5] = "-f "+new File(".").getCanonicalPath()+"/tags";
		command[6] = filename;
		
		Runtime run = Runtime.getRuntime();
		
		Process proc = run.exec(command);
		
		try {
			proc.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
		
		BufferedReader reader = null;
		try {
			File f = new File(".");
			reader = new BufferedReader(new FileReader(new File(f.getCanonicalPath()+"/tags")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		String line = reader.readLine();
		
		while(line != null && line.charAt(0) == '!')
			line = reader.readLine();
		
		while(line != null){
			// Get information from tags file
			String[] parts = line.split("\t");
			String name = parts[0];
			String regep = parts[2];
			String type = parts[3];
			int lineNum = Integer.parseInt(parts[4].split(":")[1]);
			
			// Get line of code this appeared on
			TextIter iter1 = buffer.getIter(lineNum-1, 0);
			TextIter iter2 = buffer.getIter(lineNum-1, buffer.getText(iter1, buffer.getEndIter(), true).indexOf("\n"));
			String lineText = buffer.getText(iter1, iter2, false);
			
			int start = lineText.indexOf(name);
			
			if(type.equals("member") || type.equals("local") || type.equals("variable")){
				if(start != 0)
					while(!Character.isWhitespace(lineText.charAt(start-1)))
						start += lineText.substring(start+1).indexOf(name)+1;
				
				if(!type.equals("variable"))
					buffer.addVariable(new Variable(name, lineNum, start, false));
				else
					buffer.addVariable(new Variable(name, lineNum, start, true));
			}
			
			else if(type.equals("function")){
				buffer.addFunction(name, lineNum-1, start, true);
			}
			
			line = reader.readLine();
		}
		
		reader.close();
	}

	private String escapeChars(String s){
		String ret = "";
		
		for(int i = 0; i < s.length(); i++){
			char c = s.charAt(i);
			
			if(c == '{' || c == '}')
				ret += "\\";
			
			ret += c;
		}
		
		return ret;
	}
}
