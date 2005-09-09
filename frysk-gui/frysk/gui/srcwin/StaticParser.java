/**
 * 
 */
package frysk.gui.srcwin;

import java.io.IOException;


/**
 * @author ajocksch
 *
 */
public interface StaticParser {
	
	/**
	 * Parses the given file for syntax and other static information and stores the
	 * info in the specified buffer
	 * 
	 * @param filename The file to parse
	 * @param buffer The source buffer to put the resulting data in
	 */
	public void parse(String filename, SourceBuffer buffer) throws IOException;
}
